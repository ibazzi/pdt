/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.ui.editor.contentassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.core.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.internal.core.PHPCoreConstants;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.ast.nodes.*;
import org.eclipse.php.internal.core.ast.rewrite.*;
import org.eclipse.php.internal.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.corext.dom.NodeFinder;
import org.eclipse.php.internal.core.format.FormatterUtils;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.internal.ui.corext.codemanipulation.StubUtility;
import org.eclipse.php.internal.ui.text.correction.ASTResolving;
import org.eclipse.text.edits.MalformedTreeException;

public class PHPOverrideCompletionProposal extends PHPTypeCompletionProposal implements ICompletionProposalExtension4 {

	private String fMethodName;

	public PHPOverrideCompletionProposal(IScriptProject jproject, ISourceModule cu, String methodName,
			String[] paramTypes, int start, int length, String displayName, String completionProposal) {
		this(jproject, cu, methodName, paramTypes, start, length, new StyledString(displayName), completionProposal);
	}

	/**
	 * @since 5.5
	 */
	public PHPOverrideCompletionProposal(IScriptProject jproject, ISourceModule cu, String methodName,
			String[] paramTypes, int start, int length, StyledString displayName, String completionProposal) {
		super(completionProposal, cu, start, length, null, displayName, 0, null);
		Assert.isNotNull(jproject);
		Assert.isNotNull(methodName);
		Assert.isNotNull(paramTypes);
		Assert.isNotNull(cu);

		fMethodName = methodName;

		StringBuffer buffer = new StringBuffer();
		buffer.append(completionProposal);

		setReplacementString(buffer.toString());
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return fMethodName;
	}

	protected MethodDeclaration getMethodDeclaration(AST ast, ASTRewrite rewrite, ImportRewrite imports,
			ITypeBinding declaringType) throws CoreException {
		IMethodBinding methodToOverride = Bindings.findMethodInHierarchy(declaringType, fMethodName);
		if (methodToOverride == null && declaringType.isInterface()) {
			methodToOverride = Bindings.findMethodInType(ast.resolveWellKnownType("java.lang.Object"), //$NON-NLS-1$
					fMethodName);
		}

		if (methodToOverride != null) {
			return StubUtility.createImplementationStub(fSourceModule, rewrite, imports,
					(IMethod) methodToOverride.getPHPElement(), declaringType.isInterface());
		}
		return null;
	}

	protected Program getRecoveredAST(IDocument document, int offset, Document recoveredDocument) {
		try {
			// Program ast = SharedASTProvider.getAST(fSourceModule,
			// SharedASTProvider.WAIT_YES, null);
			// if (ast != null) {
			// recoveredDocument.set(document.get());
			// return ast;
			// }

			char[] content = document.get().toCharArray();

			// clear prefix to avoid compile errors
			int index = offset - 1;
			while (index >= 0 && Character.isJavaIdentifierPart(content[index])) {
				content[index] = ' ';
				index--;
			}

			recoveredDocument.set(new String(content));

			final ASTParser parser = ASTParser.newParser(fSourceModule);
			parser.setSource(content);
			return (Program) parser.createAST(new NullProgressMonitor());
		} catch (Exception e) {
			PHPUiPlugin.log(e);
		}
		return null;
	}

	/*
	 * @see
	 * JavaTypeCompletionProposal#updateReplacementString(IDocument,char,int,
	 * ImportRewrite)
	 */
	@Override
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite importRewrite)
			throws CoreException, BadLocationException {
		Document recoveredDocument = new Document();
		Program unit = getRecoveredAST(document, offset, recoveredDocument);
		if (importRewrite == null) {
			importRewrite = ImportRewrite.create(unit, true);
		}

		ITypeBinding declaringType = null;
		ASTNode node = NodeFinder.perform(unit, offset, 1);
		node = ASTResolving.findParentType(node);
		ListRewrite rewriter = null;
		ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
		if (node instanceof AnonymousClassDeclaration) {
			declaringType = ((AnonymousClassDeclaration) node).resolveTypeBinding();
			rewriter = rewrite.getListRewrite(((AnonymousClassDeclaration) node).getBody(), Block.STATEMENTS_PROPERTY);
		} else if (node instanceof TypeDeclaration) {
			TypeDeclaration declaration = (TypeDeclaration) node;
			declaringType = declaration.resolveTypeBinding();
			rewriter = rewrite.getListRewrite(((TypeDeclaration) node).getBody(), Block.STATEMENTS_PROPERTY);
		}
		if (declaringType != null && rewriter != null) {
			MethodDeclaration stub = getMethodDeclaration(node.getAST(), rewrite, importRewrite, declaringType);
			if (stub != null) {
				int indentWidth = FormatterUtils.getFormatterCommonPrferences().getIndentationSize(document);
				int tabWidth = FormatterUtils.getFormatterCommonPrferences().getTabSize(document);

				rewriter.insertFirst(stub, null);

				ITrackedNodePosition position = rewrite.track(stub);
				try {
					rewrite.rewriteAST(recoveredDocument, fSourceModule.getScriptProject().getOptions(true))
							.apply(recoveredDocument);

					String generatedCode = recoveredDocument.get(position.getStartPosition(), position.getLength());
					int generatedIndent = IndentManipulation.measureIndentUnits(
							getIndentAt(recoveredDocument, position.getStartPosition(), tabWidth, indentWidth),
							tabWidth, indentWidth);

					String indent = getIndentAt(document, getReplacementOffset(), tabWidth, indentWidth);
					setReplacementString(IndentManipulation.changeIndent(generatedCode, generatedIndent, tabWidth,
							indentWidth, indent, TextUtilities.getDefaultLineDelimiter(document)));

					int replacementLength = getReplacementLength();
					if (document.get(getReplacementOffset() + replacementLength, 1).equals(")")) { //$NON-NLS-1$
						setReplacementLength(replacementLength + 1);
					}

				} catch (MalformedTreeException exception) {
					PHPUiPlugin.log(exception);
				} catch (BadLocationException exception) {
					PHPUiPlugin.log(exception);
				}
			}
		}
		return true;
	}

	protected static String getIndentAt(IDocument document, int offset, int tabWidth, int indentWidth) {
		try {
			IRegion region = document.getLineInformationOfOffset(offset);
			return IndentManipulation.extractIndentString(document.get(region.getOffset(), region.getLength()),
					tabWidth, indentWidth);
		} catch (BadLocationException e) {
			return ""; //$NON-NLS-1$
		}
	}

	public boolean isAutoInsertable() {
		return Platform.getPreferencesService().getBoolean(PHPCorePlugin.ID, PHPCoreConstants.CODEASSIST_AUTOINSERT,
				false, null);
	}

	protected void calculateCursorPosition(IDocument document, int offset) {
		try {
			while (Character.isJavaIdentifierPart(document.getChar(offset))
					|| document.getChar(offset) == NamespaceReference.NAMESPACE_SEPARATOR) {
				++offset;
			}
			if (document.getChar(offset) == '(') {
				boolean hasArguments = false;
				IModelElement modelElement = getModelElement();
				if (modelElement.getElementType() == IModelElement.METHOD) {
					IMethod method = (IMethod) modelElement;
					try {
						String[] parameters = method.getParameterNames();
						if (parameters != null && parameters.length > 0) {
							hasArguments = true;
						}
					} catch (ModelException e) {
					}
				}
				if (!hasArguments) {
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=459377
					// Check if we have some parameters inside of parentheses,
					// even if they shouldn't be there.
					// In this case, place cursor after left parenthesis,
					// otherwise place cursor after right parenthesis.
					IRegion line = document.getLineInformationOfOffset(offset);
					int lineEnd = line.getOffset() + line.getLength();
					int pos = offset + 1;
					while (pos < lineEnd) {
						if (Character.isWhitespace(document.getChar(pos))) {
							pos++;
							continue;
						}
						if (document.getChar(pos) == ')') {
							pos++;
							break;
						}
						pos = offset + 1;
						break;
					}
					setCursorPosition(pos - getReplacementOffset());
				} else {
					setCursorPosition(offset - getReplacementOffset() + 1);
				}
			}
		} catch (BadLocationException e) {
		}
	}

}
