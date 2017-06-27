/*******************************************************************************
 * Copyright (c) 2017 Alex Xu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Alex Xu - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.core.typeinference.context;

import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.ti.IContext;
import org.eclipse.dltk.ti.ISourceModuleContext;
import org.eclipse.dltk.ti.types.IEvaluatedType;
import org.eclipse.php.core.compiler.ast.nodes.LambdaFunctionDeclaration;
import org.eclipse.php.internal.core.typeinference.IModelAccessCache;

/**
 * This is a PHP lambda function context.
 * 
 */
public class LambdaFunctionContext
		implements IContext, INamespaceContext, IArgumentsContext, ISourceModuleContext, IModelCacheContext {

	private final ISourceModule sourceModule;
	private final ModuleDeclaration rootNode;
	private final LambdaFunctionDeclaration methodNode;
	private final String[] argNames;
	private final IEvaluatedType[] argTypes;
	private String namespaceName;
	private IContext parentContext;
	private IModelAccessCache cache;

	public LambdaFunctionContext(IContext parent, ISourceModule sourceModule, ModuleDeclaration rootNode,
			LambdaFunctionDeclaration methodNode, String[] argNames, IEvaluatedType[] argTypes) {
		this.sourceModule = sourceModule;
		this.rootNode = rootNode;
		this.methodNode = methodNode;
		this.argNames = argNames;
		this.argTypes = argTypes;
		this.parentContext = parent;
		if (parent instanceof INamespaceContext) {
			namespaceName = ((INamespaceContext) parent).getNamespace();
		}
	}

	public IEvaluatedType getArgumentType(String name) {
		for (int i = 0; i < argNames.length; i++) {
			String argName = argNames[i];
			if (name.equals(argName)) {
				if (i < argTypes.length) {
					return argTypes[i];
				} else {
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Returns namespace where the method was declared or <code>null</code> if
	 * this is a global scope method/function
	 */
	public String getNamespace() {
		return namespaceName;
	}

	/**
	 * Returns root AST node of the file where the method is declared
	 */
	public ModuleDeclaration getRootNode() {
		return rootNode;
	}

	/**
	 * Returns the file {@link ISourceModule} where the method is declared
	 */
	public ISourceModule getSourceModule() {
		return sourceModule;
	}

	/**
	 * Returns AST node of the method declaration
	 */
	public LambdaFunctionDeclaration getMethodNode() {
		return methodNode;
	}

	public IContext getParentContext() {
		return parentContext;
	}

	public String getLangNature() {
		if (sourceModule != null) {
			IDLTKLanguageToolkit languageToolkit = DLTKLanguageManager.getLanguageToolkit(sourceModule);
			if (languageToolkit != null) {
				return languageToolkit.getNatureId();
			}
		}
		return null;
	}

	public IModelAccessCache getCache() {
		return cache;
	}

	public void setCache(IModelAccessCache cache) {
		this.cache = cache;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodNode == null) ? 0 : methodNode.hashCode());
		result = prime * result + ((namespaceName == null) ? 0 : namespaceName.hashCode());
		result = prime * result + ((sourceModule == null) ? 0 : sourceModule.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LambdaFunctionContext other = (LambdaFunctionContext) obj;
		if (methodNode == null) {
			if (other.methodNode != null)
				return false;
		} else if (!methodNode.equals(other.methodNode))
			return false;
		if (namespaceName == null) {
			if (other.namespaceName != null)
				return false;
		} else if (!namespaceName.equals(other.namespaceName))
			return false;
		if (sourceModule == null) {
			if (other.sourceModule != null)
				return false;
		} else if (!sourceModule.equals(other.sourceModule))
			return false;
		return true;
	}

}
