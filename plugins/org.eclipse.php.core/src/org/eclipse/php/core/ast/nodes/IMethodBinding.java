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
package org.eclipse.php.core.ast.nodes;

/**
 * A method binding represents a method or constructor of a class or interface.
 * Method bindings usually correspond directly to method or constructor
 * declarations found in the source code.
 * 
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see ITypeBinding#getDeclaredMethods()
 * @since 2.0
 */
public interface IMethodBinding extends IFunctionBinding {

	/**
	 * Returns whether this binding is for a constructor or a method.
	 * 
	 * @return <code>true</code> if this is the binding for a constructor, and
	 *         <code>false</code> if this is the binding for a method
	 */
	public boolean isConstructor();

	/**
	 * Returns the type binding representing the class or interface that
	 * declares this method or constructor.
	 * 
	 * @return the binding of the class or interface that declares this method
	 *         or constructor
	 */
	public ITypeBinding getDeclaringClass();

	/**
	 * Returns whether this method overrides the given method, as specified in
	 * section 8.4.8.1 of <em>The Java Language Specification, Third
	 * Edition</em> (JLS3).
	 * 
	 * @param method
	 *            the method that is possibly overriden
	 * @return <code>true</code> if this method overrides the given method, and
	 *         <code>false</code> otherwise
	 * @since 3.1
	 */
	public boolean overrides(IMethodBinding method);

	/**
	 * Returns the binding for the method declaration corresponding to this
	 * method binding.
	 * <ul>
	 * <li>For parameterized methods ({@link #isParameterizedMethod()}) and raw
	 * methods ({@link #isRawMethod()}), this method returns the binding for the
	 * corresponding generic method.</li>
	 * <li>For references to the method {@link Object#getClass()
	 * Object.getClass()}, returns the binding for the method declaration which
	 * is declared to return <code>Class&lt;?&gt;</code> or
	 * <code>Class&lt;? extends Object&gt;</code>. In the reference binding, the
	 * return type becomes
	 * <code>Class&lt;? extends </code><em>R</em><code>&gt;</code>, where
	 * <em>R</em> is the erasure of the static type of the receiver of the
	 * method invocation.</li>
	 * <li>For references to a signature polymorphic method from class
	 * MethodHandle, returns the declaration of the method. In the reference
	 * binding, the parameter types and the return type are determined by the
	 * concrete invocation context.</li>
	 * <li>For lambda methods, returns the (possibly parameterized) single
	 * abstract method of the functional type.</li>
	 * <li>For other method bindings, this returns the same binding.</li>
	 * </ul>
	 *
	 * @return the method binding
	 */
	public IMethodBinding getMethodDeclaration();
}
