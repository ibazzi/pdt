/*******************************************************************************
 * Copyright (c) 2006 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   William Candillon {wcandillon@gmail.com} - Initial implementation
 *******************************************************************************/
package org.eclipse.php.internal.ui.editor.highlighters;

import org.eclipse.php.internal.core.ast.nodes.ASTNode;
import org.eclipse.php.internal.core.ast.nodes.Expression;
import org.eclipse.php.internal.core.ast.nodes.FunctionInvocation;
import org.eclipse.php.internal.core.ast.nodes.Variable;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;

public class StaticMethodHighlighting extends AbstractSemanticHighlighting {

	protected class StaticMethodApply extends AbstractSemanticApply {

		@Override
		public boolean visit(FunctionInvocation functionInvocation) {
			final Expression functionName = functionInvocation.getFunctionName().getName();
			final int invocationParent = functionInvocation.getParent().getType();
			if ((functionName.getType() == ASTNode.IDENTIFIER || (functionName.getType() == ASTNode.VARIABLE
					&& ((Variable) functionName).getName().getType() == ASTNode.IDENTIFIER))
					&& invocationParent == ASTNode.STATIC_METHOD_INVOCATION) {
				highlight(functionName);
			}
			return true;
		}
	}

	@Override
	public AbstractSemanticApply getSemanticApply() {
		return new StaticMethodApply();
	}

	@Override
	public int getPriority() {
		return 110;
	}

	@Override
	protected void initDefaultPreferences() {
		getStyle().setItalicByDefault(true);
	}

	public String getDisplayName() {
		return Messages.StaticMethodHighlighting_0;
	}
}
