package org.eclipse.php.internal.ui.search;

import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.search.IDLTKSearchConstants;
import org.eclipse.dltk.internal.corext.util.Messages;
import org.eclipse.dltk.internal.ui.search.DLTKSearchQuery;
import org.eclipse.dltk.internal.ui.search.SearchMessages;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.ui.search.ElementQuerySpecification;
import org.eclipse.dltk.ui.search.PatternQuerySpecification;
import org.eclipse.dltk.ui.search.QuerySpecification;

public class PHPSearchQuery extends DLTKSearchQuery {

	private final QuerySpecification fPatternData;

	public PHPSearchQuery(QuerySpecification data) {
		super(data);
		fPatternData = data;
	}

	@Override
	public String getResultLabel(int nMatches) {
		if (nMatches == 1) {
			String[] args = { getSearchPatternDescription(), fPatternData.getScopeDescription() };
			switch (fPatternData.getLimitTo()) {
			// case IDLTKSearchConstants.IMPLEMENTORS:
			// return
			// Messages.format(SearchMessages.DLTKSearchOperation_singularImplementorsPostfix,
			// args);
			case IDLTKSearchConstants.DECLARATIONS:
				return Messages.format(SearchMessages.DLTKSearchOperation_singularDeclarationsPostfix, args);
			case IDLTKSearchConstants.REFERENCES:
				return Messages.format(SearchMessages.DLTKSearchOperation_singularReferencesPostfix, args);
			case IDLTKSearchConstants.ALL_OCCURRENCES:
				return Messages.format(SearchMessages.DLTKSearchOperation_singularOccurrencesPostfix, args);
			// case IDLTKSearchConstants.READ_ACCESSES:
			// return
			// Messages.format(SearchMessages.DLTKSearchOperation_singularReadReferencesPostfix,
			// args);
			// case IDLTKSearchConstants.WRITE_ACCESSES:
			// return
			// Messages.format(SearchMessages.DLTKSearchOperation_singularWriteReferencesPostfix,
			// args);
			default:
				return Messages.format(SearchMessages.DLTKSearchOperation_singularOccurrencesPostfix, args);
			}
		} else {
			Object[] args = { getSearchPatternDescription(), new Integer(nMatches),
					fPatternData.getScopeDescription() };
			switch (fPatternData.getLimitTo()) {
			// case IDLTKSearchConstants.IMPLEMENTORS:
			// return
			// Messages.format(SearchMessages.DLTKSearchOperation_pluralImplementorsPostfix,
			// args);
			case IDLTKSearchConstants.DECLARATIONS:
				return Messages.format(SearchMessages.DLTKSearchOperation_pluralDeclarationsPostfix, args);
			case IDLTKSearchConstants.REFERENCES:
				return Messages.format(SearchMessages.DLTKSearchOperation_pluralReferencesPostfix, args);
			case IDLTKSearchConstants.ALL_OCCURRENCES:
				return Messages.format(SearchMessages.DLTKSearchOperation_pluralOccurrencesPostfix, args);
			// case IDLTKSearchConstants.READ_ACCESSES:
			// return
			// Messages.format(SearchMessages.DLTKSearchOperation_pluralReadReferencesPostfix,
			// args);
			// case IDLTKSearchConstants.WRITE_ACCESSES:
			// return
			// Messages.format(SearchMessages.DLTKSearchOperation_pluralWriteReferencesPostfix,
			// args);
			default:
				return Messages.format(SearchMessages.DLTKSearchOperation_pluralOccurrencesPostfix, args);
			}
		}
	}

	private String getSearchPatternDescription() {
		if (fPatternData instanceof ElementQuerySpecification) {
			IModelElement element = ((ElementQuerySpecification) fPatternData).getElement();
			long flags = (ScriptElementLabels.ALL_DEFAULT | ScriptElementLabels.ALL_FULLY_QUALIFIED
					| ScriptElementLabels.USE_RESOLVED | ScriptElementLabels.M_PARAMETER_TYPES)
					& ~ScriptElementLabels.M_APP_RETURNTYPE & ~ScriptElementLabels.M_PARAMETER_NAMES;
			return ScriptElementLabels.getDefault().getElementLabel(element, flags);
		}
		return ((PatternQuerySpecification) fPatternData).getPattern();
	}

}
