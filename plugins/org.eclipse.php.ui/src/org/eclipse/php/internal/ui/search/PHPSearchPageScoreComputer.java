package org.eclipse.php.internal.ui.search;

import org.eclipse.dltk.internal.ui.editor.ExternalStorageEditorInput;
import org.eclipse.php.internal.core.documentModel.dom.ElementImplForPHP;
import org.eclipse.search.ui.ISearchPageScoreComputer;

public class PHPSearchPageScoreComputer implements ISearchPageScoreComputer {

	@Override
	public int computeScore(String id, Object element) {
		if (!PHPSearchPage.ID.equals(id))
			// Can't decide
			return ISearchPageScoreComputer.UNKNOWN;

		if (element instanceof ElementImplForPHP || element instanceof ExternalStorageEditorInput)
			return 90;

		return ISearchPageScoreComputer.LOWEST;
	}
}
