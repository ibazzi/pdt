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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.core.ast.nodes.AST;
import org.eclipse.php.core.ast.nodes.ITypeBinding;
import org.eclipse.php.core.ast.nodes.MethodDeclaration;
import org.eclipse.php.internal.core.ast.rewrite.ASTRewrite;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite;
import org.eclipse.php.internal.ui.corext.codemanipulation.StubUtility;

public class PHPMagicMethodOverloadCompletionProposal extends PHPOverrideCompletionProposal {

	private IMethod fMagicMethod;

	public PHPMagicMethodOverloadCompletionProposal(IMethod magicMethod, IScriptProject jproject, ISourceModule cu,
			String methodName, String[] paramTypes, int start, int length, String displayName,
			String completionProposal) {
		super(jproject, cu, methodName, paramTypes, start, length, displayName, completionProposal);
		this.fMagicMethod = magicMethod;
	}

	public PHPMagicMethodOverloadCompletionProposal(IMethod magicMethod, IScriptProject jproject, ISourceModule cu,
			String methodName, String[] paramTypes, int start, int length, StyledString displayName,
			String completionProposal) {
		super(jproject, cu, methodName, paramTypes, start, length, displayName, completionProposal);
		this.fMagicMethod = magicMethod;
	}

	@Override
	protected MethodDeclaration getMethodDeclaration(AST ast, ASTRewrite rewrite, ImportRewrite imports,
			ITypeBinding declaringType) throws CoreException {
		return StubUtility.createMagicMethodStub(fSourceModule, rewrite, fMagicMethod, declaringType.isInterface());
	}

}
