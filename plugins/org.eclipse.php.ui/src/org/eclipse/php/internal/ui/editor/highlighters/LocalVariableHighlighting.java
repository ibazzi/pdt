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

import org.eclipse.php.internal.core.ast.nodes.*;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;

public class LocalVariableHighlighting extends AbstractSemanticHighlighting {

	protected class LocalVariableApply extends AbstractSemanticApply {

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

	public String getDisplayName() {
		return Messages.LocalVariableHighlighting_0;
	}
}