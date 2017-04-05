/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.dltk.core.IImportDeclaration;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.internal.corext.util.Strings;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.ui.viewsupport.ScriptElementLabelComposer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.internal.ui.viewsupport.PHPElementLabelComposer;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class PHPElementLabels extends ScriptElementLabels {

	@Override
	protected ScriptElementLabelComposer getScriptElementLabelComposer(StringBuffer buf) {
		return new PHPElementLabelComposer(buf);
	}

	@Override
	protected ScriptElementLabelComposer getScriptElementLabelComposer(StyledString buf) {
		return new PHPElementLabelComposer(buf);
	}

	@Override
	public StyledString getStyledTextLabel(Object obj, long flags) {
		if (obj instanceof IModelElement) {
			return getStyledElementLabel((IModelElement) obj, flags);

		} else if (obj instanceof IAdaptable) {
			IWorkbenchAdapter wbadapter = (IWorkbenchAdapter) ((IAdaptable) obj).getAdapter(IWorkbenchAdapter.class);
			if (wbadapter != null) {
				return Strings.markLTR(new StyledString(wbadapter.getLabel(obj)));
			}
		}
		return new StyledString();
	}

	@Override
	protected void getImportDeclarationLabel(IModelElement element, long flags, StringBuffer buf) {
		super.getImportDeclarationLabel(element, flags, buf);
		IImportDeclaration declaration = (IImportDeclaration) element;
		if (declaration.getAlias() != null) {
			buf.append(" as "); //$NON-NLS-1$
			buf.append(declaration.getAlias());
		}
	}
}
