/*********************************************************import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.php.internal.core.ast.nodes.*;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;
import org.eclipse.swt.graphics.RGB;
rg/legal/epl-v10.html
 *
 * Contributors:
 *   William Candillon {wcandillon@gmail.com} - Initial implementation
 *******************************************************************************/
package org.eclipse.php.internal.ui.editor.highlighters;

import org.eclipse.php.core.ast.nodes.IVariableBinding;
import org.eclipse.php.core.ast.nodes.Variable;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;

public class ParameterVariableHighlighting extends AbstractSemanticHighlighting {

	protected class ParameterVariableApply extends AbstractSemanticApply {

		@Override
		public boolean visit(Variable variable) {
			IVariableBinding variableBinding = variable.resolveVariableBinding();
			if (variableBinding != null && variableBinding.isParameter()) {
				highlight(variable);
			}
			return false;
		}
	}

	@Override
	public AbstractSemanticApply getSemanticApply() {
		return new ParameterVariableApply();
	}

	@Override
	protected void initDefaultPreferences() {
		getStyle().setEnabledByDefault(true).setUnderlineByDefault(true);
	}

	@Override
	public String getDisplayName() {
		return Messages.ParameterVariableHighlighting_0;
	}
}