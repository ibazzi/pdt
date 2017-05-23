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

import org.eclipse.php.core.ast.nodes.IVariableBinding;
import org.eclipse.php.core.ast.nodes.Variable;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;

public class LocalVariableHighlighting extends AbstractSemanticHighlighting {

	protected class LocalVariableApply extends AbstractSemanticApply {

		// private Collection<String> params = new LinkedList<>();
		//
		// @Override
		// public boolean visit(FormalParameter param) {
		// params.add(param.getParameterNameIdentifier().getName());
		// return true;
		// }
		//
		// @Override
		// public boolean visit(Variable variable) {
		// ASTNode parent = variable.getParent();
		// boolean isLocal = false;
		// while (parent != null) {
		// if (parent instanceof FunctionDeclaration || parent instanceof
		// LambdaFunctionDeclaration) {
		// isLocal = true;
		// break;
		// }
		// parent = parent.getParent();
		// }
		// String name = ((Identifier) variable.getName()).getName();
		// if (isLocal && !(variable.getParent() instanceof FormalParameter) &&
		// variable.isDollared()
		// && !("this".equals(name)) && !params.contains(name)) { //$NON-NLS-1$
		// highlight(variable);
		// }
		// return false;
		// }

		@Override
		public boolean visit(Variable variable) {
			IVariableBinding variableBinding = variable.resolveVariableBinding();
			if (variableBinding != null && variableBinding.isLocal() && !variableBinding.isParameter()) {
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