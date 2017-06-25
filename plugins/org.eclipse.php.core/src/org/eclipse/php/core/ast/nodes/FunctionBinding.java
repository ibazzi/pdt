/*******************************************************************************
 * Copyright (c) 2009, 2015, 2016 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.internal.core.ExternalProjectFragment;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;

/**
 * A PHP function binding. This class is also the base class for the
 * {@link MethodBinding} implementation.
 * 
 * @author shalom
 */
public class FunctionBinding implements IFunctionBinding {

	protected static final int VALID_MODIFIERS = Modifiers.AccPublic | Modifiers.AccProtected | Modifiers.AccPrivate
			| Modifiers.AccDefault | Modifiers.AccStatic | Modifiers.AccFinal | Modifiers.AccAbstract;
	protected BindingResolver resolver;
	private ITypeBinding[] parameterTypes;
	private ITypeBinding[] returnType;
	protected IMethod modelElement;
	private boolean isInternalEvaluated;
	private boolean isInternal;

	/**
	 * Constructs a new FunctionBinding.
	 * 
	 * @param resolver
	 *            A {@link BindingResolver}.
	 * @param modelElement
	 *            An {@link IMethod}.
	 */
	public FunctionBinding(BindingResolver resolver, IMethod modelElement) {
		this.resolver = resolver;
		this.modelElement = modelElement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IFunctionBinding#
	 * getExceptionTypes ()
	 */
	public ITypeBinding[] getExceptionTypes() {
		// Get an array of PHPDocFields
		// ArrayList<ITypeBinding> exceptions = new ArrayList<ITypeBinding>();
		// PHPDocBlock docBlock = PHPModelUtils.getDocBlock(modelElement);
		// for (PHPDocTag tag : docBlock.getTags(TagKind.THROWS)) {
		// List<TypeReference> references = tag.getTypeReferences();
		// TODO - create ITypeBinding array from this TypeReference
		// array
		// }
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IFunctionBinding#getName()
	 */
	public String getName() {
		return modelElement.getElementName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IFunctionBinding#
	 * getParameterTypes ()
	 */
	public ITypeBinding[] getParameterTypes() {
		if (this.parameterTypes == null) {
			try {
				IModelElement[] elements = modelElement.getChildren();
				List<ITypeBinding> typeBindings = new ArrayList<>();
				if (elements != null) {
					for (IModelElement element : elements) {
						if (element instanceof IField) {
							typeBindings.add(this.resolver.getFieldTypeBinding((IField) element));
						}
					}
				}
				this.parameterTypes = typeBindings.toArray(new ITypeBinding[0]);
			} catch (ModelException e) {
				PHPCorePlugin.log(e);
			}
		}
		return this.parameterTypes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.core.ast.nodes.IFunctionBinding#getReturnType()
	 */
	public ITypeBinding[] getReturnType() {
		if (this.returnType == null) {
			this.returnType = resolver.getMethodReturnTypeBinding(modelElement);
		}
		return this.returnType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IFunctionBinding#isVarargs()
	 */
	public boolean isVarargs() {
		// TODO
		return false;
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
		return IBinding.METHOD;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.core.ast.nodes.IBinding#getModifiers()
	 */
	public int getModifiers() {
		try {
			return modelElement.getFlags() & VALID_MODIFIERS;
		} catch (ModelException e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
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
	public boolean isInternal() {
		if (!isInternalEvaluated && modelElement != null) {
			IModelElement element = modelElement.getAncestor(IModelElement.PROJECT_FRAGMENT);
			if (element instanceof ExternalProjectFragment && ((ExternalProjectFragment) element).isExternal()) {
				isInternal = true;
			}
		}
		return isInternal;
	}
}
