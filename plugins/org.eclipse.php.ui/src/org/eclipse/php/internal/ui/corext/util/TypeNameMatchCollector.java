package org.eclipse.php.internal.ui.corext.util;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.search.TypeNameMatch;
import org.eclipse.dltk.core.search.TypeNameMatchRequestor;
import org.eclipse.dltk.internal.corext.util.TypeFilter;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.php.internal.core.project.PHPNature;

public class TypeNameMatchCollector extends TypeNameMatchRequestor {

	private final TypeFilter filter = new TypeFilter(DLTKUILanguageManager.getLanguageToolkit(PHPNature.ID));

	private final Collection<TypeNameMatch> fCollection;

	public TypeNameMatchCollector(Collection<TypeNameMatch> collection) {
		Assert.isNotNull(collection);
		fCollection = collection;
	}

	private boolean inScope(TypeNameMatch match) {
		return !filter.isFiltered(match);
	}

	@Override
	public void acceptTypeNameMatch(TypeNameMatch match) {
		if (inScope(match)) {
			fCollection.add(match);
		}
	}

}
