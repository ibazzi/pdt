/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.debug.ui.hovers;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.dltk.core.IField;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.internal.ui.text.hover.AbstractScriptEditorTextHover;
import org.eclipse.jface.text.*;
import org.eclipse.php.core.ast.nodes.*;
import org.eclipse.php.core.ast.visitor.AbstractVisitor;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.corext.dom.NodeFinder;
import org.eclipse.php.internal.debug.core.zend.debugger.Expression;
import org.eclipse.php.internal.debug.core.zend.debugger.ExpressionsUtil;
import org.eclipse.php.internal.debug.core.zend.model.PHPDebugTarget;
import org.eclipse.php.internal.debug.core.zend.model.PHPStackFrame;
import org.eclipse.php.internal.debug.core.zend.model.PHPVariable;
import org.eclipse.php.internal.debug.ui.PHPDebugUIPlugin;
import org.eclipse.php.ui.editor.SharedASTProvider;
import org.eclipse.php.ui.editor.hover.IHoverMessageDecorator;
import org.eclipse.php.ui.editor.hover.IPHPTextHover;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

public class PHPDebugTextHover extends AbstractScriptEditorTextHover implements IPHPTextHover, ITextHoverExtension2 {

	private ExpressionsUtil expressionsUtil;

	public PHPDebugTextHover() {
	}

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return getHoverInfo2(textViewer, hoverRegion).toString();
	}

	public IHoverMessageDecorator getMessageDecorator() {
		return null;
	}

	/**
	 * Returns the stack frame in which to search for variables, or
	 * <code>null</code> if none.
	 * 
	 * @return the stack frame in which to search for variables, or
	 *         <code>null</code> if none
	 */
	protected PHPStackFrame getFrame() {
		IAdaptable adaptable = DebugUITools.getDebugContext();
		if (adaptable != null) {
			return adaptable.getAdapter(PHPStackFrame.class);
		}
		return null;
	}

	public IInformationControlCreator getHoverControlCreator() {
		return new ExpressionInformationControlCreator();
	}

	// Returns the php debug target that is in contex.
	// In case that
	protected PHPDebugTarget getDebugTarget() {
		IAdaptable adaptable = DebugUITools.getDebugContext();
		if (adaptable instanceof PHPStackFrame) {
			PHPStackFrame stackFrame = (PHPStackFrame) adaptable;
			IEditorInput ei = getEditor().getEditorInput();
			if (ei instanceof FileEditorInput) {
				FileEditorInput fi = (FileEditorInput) ei;

				// Check for the file path within the project
				String fileInDebug = stackFrame.getSourceName();
				String fileInProject = fi.getFile().getProjectRelativePath().toString();
				if (fileInDebug != null
						&& (fileInDebug.endsWith('/' + fileInProject) || fileInDebug.equals(fileInProject))) {
					PHPDebugTarget debugTarget = (PHPDebugTarget) stackFrame.getDebugTarget();
					return debugTarget;
				}
			} else {
				// File on the include Path
				PHPDebugTarget debugTarget = (PHPDebugTarget) stackFrame.getDebugTarget();
				return debugTarget;
			}
		}
		return null;
	}

	@Override
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		PHPStackFrame frame = getFrame();
		if (frame == null)
			return null;

		expressionsUtil = ExpressionsUtil.getInstance(getDebugTarget().getExpressionManager());

		IVariable variable = null;
		try {
			ISourceModule sourceModule = getEditorInputModelElement();
			ASTNode root = SharedASTProvider.getAST(sourceModule, SharedASTProvider.WAIT_NO, null);
			if (root == null) {
				ASTParser parser = ASTParser.newParser(sourceModule);
				root = parser.createAST(null);
			}
			ASTNode node = NodeFinder.perform(root, hoverRegion.getOffset(), hoverRegion.getLength());

			// local variables
			if (node instanceof Variable || (node instanceof Identifier && node.getParent() instanceof Variable
					&& !((Variable) node.getParent()).isDollared())) {
				String variableName = null;
				// ${a}
				if (node instanceof Identifier && node.getParent() instanceof Variable
						&& !((Variable) node.getParent()).isDollared()) {
					variableName = "$" + ((Identifier) node).getName();
				} else {
					IDocument document = textViewer.getDocument();
					if (document != null) {
						Variable var = (Variable) node;
						// $a
						if (var.isDollared()) {
							variableName = document.get(hoverRegion.getOffset(), hoverRegion.getLength());
							// $$a
						} else if (var instanceof ReflectionVariable) {
							variableName = document.get(((ReflectionVariable) var).getName().getStart(),
									((ReflectionVariable) node).getName().getLength());
						}
					}
				}
				variable = frame.findVariable(variableName);
			} else if (node instanceof Scalar) {
				Scalar scalar = (Scalar) node;
				if (node.getParent() instanceof ArrayAccess) {
					ArrayAccess access = (ArrayAccess) node.getParent();
					Expression expression = expressionsUtil.buildExpression(computeExpression(access.getName()));
					Expression[] children = expression.getValue().getOriChildren();
					if (children != null && children.length > 0) {
						for (Expression child : children) {
							String name = child.getLastName();
							if (scalar.getScalarType() == Scalar.TYPE_STRING) {
								name = "\"" + name + "\"";
							}
							if (name.equals(scalar.getStringValue())) {
								variable = new PHPVariable(getDebugTarget(), child);
							}
						}
					}
				} else if (!(scalar.getParent() instanceof Include) && scalar.getScalarType() == Scalar.TYPE_STRING) {
					if (!(scalar.getStringValue().startsWith("\"") && scalar.getStringValue().endsWith("\""))) {
						if (!scalar.getStringValue().trim().equals("")) {
							Expression constant = expressionsUtil.fetchConstant(scalar.getStringValue());
							variable = new PHPVariable(getDebugTarget(), constant);
						}
					}
				}
			} else if (node.getParent() instanceof Variable && node.getParent().getParent() instanceof FieldAccess) {
				String nodeName = ((Identifier) node).getName();
				String expression = computeExpression(((FieldAccess) node.getParent().getParent()).getDispatcher());
				variable = fetchClassMember(expression, nodeName);
			} else if (node.getParent() instanceof StaticConstantAccess) {
				String nodeName = ((Identifier) node).getName();
				StaticConstantAccess staticAccess = (StaticConstantAccess) node.getParent();
				String className = resolveTypeName((Identifier) staticAccess.getClassName());
				if (className != null) {
					if (nodeName.equals("class")) {
						variable = new PHPVariable(getDebugTarget(), expressionsUtil.fetchClassContext(className));
					} else {
						variable = fetchClassConstant(className, nodeName);
					}
				}
			} else if (node.getParent() instanceof StaticFieldAccess) {
				Variable var = (Variable) node;
				String nodeName = ((Identifier) var.getName()).getName();
				StaticFieldAccess staticAccess = (StaticFieldAccess) node.getParent();
				Identifier identifier = null;
				if (staticAccess.getClassName() instanceof Identifier) {
					identifier = (Identifier) staticAccess.getClassName();
				} else if (staticAccess.getClassName() instanceof VariableBase) {
					identifier = (Identifier) var.getName();
				}
				variable = fetchStaticMember(identifier, nodeName);
			} else if (node.getParent() instanceof ConstantDeclaration) {
				String nodeName = ((Identifier) node).getName();
				IField field = (IField) sourceModule.getElementAt(node.getStart());
				if (field.getParent() instanceof IType) {
					IType type = (IType) field.getParent();
					String typeName = type.getFullyQualifiedName(NamespaceReference.NAMESPACE_DELIMITER);
					if (!PHPFlags.isNamespace(type.getFlags())) {
						variable = fetchClassConstant(typeName, nodeName);
					} else {
						Expression constant = expressionsUtil.fetchConstant(typeName + "\\" + nodeName);
						variable = new PHPVariable(getDebugTarget(), constant);
					}
				}
			} else if (node.getParent() instanceof SingleFieldDeclaration) {
				IField field = (IField) sourceModule.getElementAt(node.getStart());
				String typeName = "";
				boolean isAnonymous = false;
				if (field.getParent() instanceof IType) {
					IType type = (IType) field.getParent();
					typeName = type.getFullyQualifiedName(NamespaceReference.NAMESPACE_DELIMITER);
					isAnonymous = PHPFlags.isAnonymous(type.getFlags());
				}
				Variable var = (Variable) node;
				String nodeName = ((Identifier) var.getName()).getName();
				if (!PHPFlags.isStatic(field.getFlags())) {
					Expression e = expressionsUtil.buildExpression("$this");
					if (isAnonymous || typeName.equals(e.getValue().getValue().toString())) {
						variable = fetchClassMember(e, nodeName);
					}
				} else {
					variable = fetchStaticMember(typeName, nodeName);
				}
			}
		} catch (Exception e) {
			PHPDebugUIPlugin.log(e);
		}
		return variable;
	}

	private String computeExpression(VariableBase node) {
		final StringBuffer dispatcher = new StringBuffer();
		node.accept(new AbstractVisitor() {
			private boolean isFirstVariable = true;

			public boolean visit(Identifier identifier) {
				if (identifier.getParent() instanceof Identifier) {
					String typeName = resolveTypeName((Identifier) identifier.getParent());
					if (typeName != null)
						dispatcher.append(typeName);
				} else if (identifier.getParent() instanceof Variable) {
					Variable variable = (Variable) identifier.getParent();
					if (variable.isDollared()) {
						if (!isFirstVariable) {
							dispatcher.append("::");
						}
						dispatcher.append("$");
					} else {
						if (!isFirstVariable) {
							dispatcher.append("->");
						}
					}
					dispatcher.append(((Identifier) variable.getName()).getName());
				}

				isFirstVariable = false;
				return false;
			};
		});
		return dispatcher.toString();
	}

	private PHPVariable fetchClassMember(String expression, String fieldName) {
		return fetchClassMember(expressionsUtil.buildExpression(expression), fieldName);
	}

	private PHPVariable fetchClassMember(Expression expression, String fieldName) {
		if (expression.getValue().getOriChildren() == null)
			return null;
		for (Expression child : expression.getValue().getOriChildren()) {
			if (child.getLastName().endsWith(fieldName)) {
				return new PHPVariable(getDebugTarget(), child);
			}
		}
		return null;
	}

	private PHPVariable fetchStaticMember(String className, String fieldName) {
		Expression[] staticMembers = expressionsUtil.fetchStaticMembers(className);
		for (Expression child : staticMembers) {
			if (child.getLastName().endsWith(fieldName)) {
				return new PHPVariable(getDebugTarget(), child);
			}
		}
		return null;
	}

	private PHPVariable fetchStaticMember(Identifier type, String fieldName) {
		String className = resolveTypeName(type);
		if (className != null) {
			return fetchStaticMember(className, fieldName);
		}
		return null;
	}

	private PHPVariable fetchClassConstant(String className, String constantName) {
		Expression[] constants = expressionsUtil.fetchClassConstants(className);
		for (Expression child : constants) {
			if (child.getLastName().equals(constantName)) {
				return new PHPVariable(getDebugTarget(), child);
			}
		}
		return null;
	}

	private String resolveTypeName(Identifier type) {
		ITypeBinding typeBinding = type.resolveTypeBinding();
		String className = null;
		if (typeBinding != null) {
			className = typeBinding.getName();
			if (className.startsWith(NamespaceReference.NAMESPACE_DELIMITER)) {
				className = className.substring(1);
			}
		} else {
			className = type.getName();
		}
		return className;
	}

}
