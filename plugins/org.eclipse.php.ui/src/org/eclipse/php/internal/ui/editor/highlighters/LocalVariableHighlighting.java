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
package org.eclipse.php.internal.ui.editor.highlighters;

import org.eclipse.php.core.ast.nodes.*;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;

public class LocalVariableHighlighting extends AbstractSemanticHighlighting {

	protected class LocalVariableApply extends AbstractSemanticApply {

		@Override
		public boolean visit(Variable variable) {
			ASTNode parent = variable.getParent();
			boolean isLocal = false;
			while (parent != null) {
				if (parent instanceof FunctionDeclaration || parent instanceof LambdaFunctionDeclaration) {
					isLocal = true;
					break;
				}
				parent = parent.getParent();
			}
			if (isLocal && !(variable.getParent() instanceof FormalParameter) && variable.isDollared()
					&& !((Identifier) variable.getName()).getName().equals("this")) {
				highlight(variable);
			}
			return false;
		}
	}

	@Override
	public AbstractSemanticApply getSemanticApply() {
		return new LocalVariableApply();
	}

	@Override
	protected void initDefaultPreferences() {
		getStyle().setEnabledByDefault(true).setDefaultTextColor(106, 62, 62);
	}

	@Override
	public String getDisplayName() {
		return Messages.LocalVariableHighlighting_0;
	}
}