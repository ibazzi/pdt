package org.eclipse.php.internal.ui.search;

import org.eclipse.dltk.internal.ui.editor.ExternalStorageEditorInput;
import org.eclipse.php.internal.core.documentModel.dom.ElementImplForPhp;
import org.eclipse.search.ui.ISearchPageScoreComputer;

public class PHPSearchPageScoreComputer implements ISearchPageScoreComputer {

	public int computeScore(String id, Object element) {
		if (!PHPSearchPage.ID.equals(id))
			// Can't decide
			return ISearchPageScoreComputer.UNKNOWN;

		if (element instanceof ElementImplForPhp
				|| element instanceof ExternalStorageEditorInput)
			return 90;

		return ISearchPageScoreComputer.LOWEST;
	}
}
