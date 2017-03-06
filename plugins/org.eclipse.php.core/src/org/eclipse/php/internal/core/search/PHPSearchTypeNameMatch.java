package org.eclipse.php.internal.core.search;

import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.internal.core.search.DLTKSearchTypeNameMatch;

/**
 * PHP Search concrete type for a type name match.
 * 
 */
public class PHPSearchTypeNameMatch extends DLTKSearchTypeNameMatch {

	public PHPSearchTypeNameMatch(IType type, int modifiers) {
		super(type, modifiers);
	}

	@Override
	public String getFullyQualifiedName() {
		return getType().getFullyQualifiedName("\\");
	}

	@Override
	public String getTypeContainerName() {
		IType outerType = getType().getDeclaringType();
		if (outerType != null) {
			return outerType.getTypeQualifiedName("\\");
		} else {
			return getType().getScriptFolder().getElementName();
		}
	}
}
