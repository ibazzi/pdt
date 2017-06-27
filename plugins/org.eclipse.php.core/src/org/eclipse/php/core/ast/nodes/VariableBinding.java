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
/**
 * 
 */
package org.eclipse.php.core.ast.nodes;

import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.internal.core.ExternalProjectFragment;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.typeinference.FakeField;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;

/**
 * A variable binding represents either a field of a class or interface, or a
 * local variable declaration (including formal parameters, local variables, and
 * exception variables) or a global variable.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * FIXME - need implementation
 * 
 * @author shalom
 */
public class VariableBinding implements IVariableBinding {

	private static final int VALID_MODIFIERS = Modifiers.AccPublic | Modifiers.AccProtected | Modifiers.AccPrivate
			| Modifiers.AccDefault | Modifiers.AccConstant | Modifiers.AccStatic | Modifiers.AccGlobal;

	private final BindingResolver resolver;
	private final IMember modelElement;
	private boolean isFakeField;
	private boolean isSuperGlobal;
	private Boolean isInternal = null;

	private ITypeBinding declaringClassTypeBinding;
	private ITypeBinding type;
	private int id;

	private Variable variable;

	public Variable getVariable() {
		return variable;
	}

	/**
	 * 
	 */
	public VariableBinding(BindingResolver resolver, IMember modelElement) {
		this.resolver = resolver;
		this.modelElement = modelElement;
		this.isFakeField = modelElement instanceof FakeField;
	}

	public VariableBinding(DefaultBindingResolver resolver, IMember modelElement, Variable variable, int id) {
		this(resolver, modelElement, variable, id, false);
	}

	public VariableBinding(DefaultBindingResolver resolver, IMember modelElement, Variable variable, int id,
			boolean isSuperGlobal) {
		this.resolver = resolver;
		this.modelElement = modelElement;
		this.isFakeField = modelElement instanceof FakeField;
		this.variable = variable;
		this.id = id;
		this.isSuperGlobal = isSuperGlobal;
	}

	/**
	 * Returns this binding's constant value if it has one. Some variables may
	 * have a value computed at compile-time. If the variable has no
	 * compile-time computed value, the result is <code>null</code>.
	 * 
	 * @return the constant value, or <code>null</code> if none
	 * @since 3.0
	 */
	public Object getConstantValue() {
		// TODO ?
		return null;
	}

	/**
	 * Returns the type binding representing the class or interface that
	 * declares this field.
	 * <p>
	 * The declaring class of a field is the class or interface of which it is a
	 * member. Local variables have no declaring class. The field length of an
	 * array type has no declaring class.
	 * </p>
	 * 
	 * @return the binding of the class or interface that declares this field,
	 *         or <code>null</code> if none
	 */
	public ITypeBinding getDeclaringClass() {
		if (declaringClassTypeBinding == null) {
			IType parent = modelElement.getDeclaringType();
			if (parent != null) {
				declaringClassTypeBinding = resolver.getTypeBinding(parent);
			}
		}
		return declaringClassTypeBinding;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IVariableBinding#
	 * getDeclaringFunction ()
	 */
	public IFunctionBinding getDeclaringFunction() {
		if (!isField()) {
			ASTNode node = this.resolver.findDeclaringNode(this);
			while (true) {
				if (node == null) {
					// if (this.binding instanceof LocalVariableBinding) {
					// LocalVariableBinding localVariableBinding =
					// (LocalVariableBinding) this.binding;
					// org.eclipse.jdt.internal.compiler.lookup.MethodBinding
					// enclosingMethod =
					// localVariableBinding.getEnclosingMethod();
					// if (enclosingMethod != null)
					// return this.resolver.getMethodBinding(enclosingMethod);
					// }
					return null;
				}
				switch (node.getType()) {
				case ASTNode.METHOD_DECLARATION:
					MethodDeclaration methodDeclaration = (MethodDeclaration) node;
					return methodDeclaration.resolveMethodBinding();
				case ASTNode.FUNCTION_DECLARATION:
					FunctionDeclaration functionDeclaration = (FunctionDeclaration) node;
					return functionDeclaration.resolveFunctionBinding();
				case ASTNode.LAMBDA_FUNCTION_DECLARATION:
					LambdaFunctionDeclaration lambdaExpression = (LambdaFunctionDeclaration) node;
					return lambdaExpression.resolveFunctionBinding();
				default:
					node = node.getParent();
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IVariableBinding#getName()
	 */
	public String getName() {
		return modelElement.getElementName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IVariableBinding#getType()
	 */
	public ITypeBinding getType() {
		if (this.type == null && variable != null) {
			if (variable.getParent() instanceof Assignment) {
				this.type = this.resolver.resolveExpressionType(((Assignment) variable.getParent()).getRightHandSide());
			} else {
				this.type = this.resolver.resolveExpressionType(variable);
			}
		}
		return this.type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.core.ast.nodes.IVariableBinding#getVariableId()
	 */
	public int getVariableId() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IVariableBinding#isField()
	 */
	public boolean isField() {
		if (IModelElement.FIELD == modelElement.getElementType() && !isFakeField) {
			ITypeBinding declaraingClass = getDeclaringClass();
			if (declaraingClass != null && declaraingClass.isClass()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IVariableBinding#isGlobal()
	 */
	public boolean isGlobal() {
		if (IModelElement.FIELD == modelElement.getElementType()) {
			if (variable.getLocationInParent() == GlobalStatement.VARIABLES_PROPERTY) {
				return true;
			}
			return !isFakeField && getDeclaringFunction() == null;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IVariableBinding#isLocal()
	 */
	public boolean isLocal() {
		if (IModelElement.FIELD == modelElement.getElementType()) {
			if (variable != null && variable.getLocationInParent() == GlobalStatement.VARIABLES_PROPERTY) {
				return false;
			}
			return !isFakeField && getDeclaringFunction() != null;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.core.ast.nodes.IVariableBinding#isParameter()
	 */
	public boolean isParameter() {
		boolean isParameter = false;
		if (variable != null) {
			ASTNode decl = variable.getProgramRoot().findDeclaringNode(this);
			if (decl != null) {
				if (decl instanceof Variable || decl instanceof Reference) {
					isParameter = decl.getLocationInParent() == FormalParameter.PARAMETER_NAME_PROPERTY;
				} else if (decl instanceof Identifier) {
					isParameter = decl.getParent().getLocationInParent() == FormalParameter.PARAMETER_TYPE_PROPERTY;
				}
			}
		}
		return isParameter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IBinding#getKey()
	 */
	public String getKey() {
		return modelElement.getHandleIdentifier();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IBinding#getKind()
	 */
	public int getKind() {
		return IBinding.VARIABLE;
	}

	/**
	 * Returns the modifiers for this binding.
	 * 
	 * @return the bit-wise or of <code>Modifiers</code> constants
	 * @see Modifiers
	 */
	public int getModifiers() {
		if (modelElement != null) {
			try {
				return modelElement.getFlags() & VALID_MODIFIERS;
			} catch (ModelException e) {
				PHPCorePlugin.log(e);
			}
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IBinding#getPHPElement()
	 */
	public IModelElement getPHPElement() {
		return modelElement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IBinding#isDeprecated()
	 */
	public boolean isDeprecated() {
		return PHPModelUtils.isDeprecated(modelElement);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IVariableBinding) {
			return this.modelElement == ((IVariableBinding) obj).getPHPElement();
		}

		return false;
	}

	@Override
	public IVariableBinding getVariableDeclaration() {
		if (isField()) {
			return this.resolver.getVariableBinding((IField) modelElement);
		}
		return this;
	}

	@Override
	public boolean isSuperGlobal() {
		return isSuperGlobal;
	}

	@Override
	public boolean isInternal() {
		if (isInternal == null) {
			isInternal = false;
			if (isConstant() && modelElement != null) {
				IModelElement element = modelElement.getAncestor(IModelElement.PROJECT_FRAGMENT);
				if (element instanceof ExternalProjectFragment && ((ExternalProjectFragment) element).isExternal()) {
					isInternal = true;
				}
			}
		}
		return isInternal;
	}

	@Override
	public boolean isConstant() {
		return PHPFlags.isConstant(getModifiers());
	}
}