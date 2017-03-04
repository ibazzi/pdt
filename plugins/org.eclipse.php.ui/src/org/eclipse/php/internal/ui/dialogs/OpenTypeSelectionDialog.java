package org.eclipse.php.internal.ui.dialogs;

import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.ui.dialogs.TypeSelectionExtension;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.swt.widgets.Shell;

/**
 * A type selection dialog used for opening types.
 */
public class OpenTypeSelectionDialog extends FilteredTypesSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.php.internal.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IDLTKSearchScope scope,
			int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IDLTKSearchScope scope,
			int elementKinds, TypeSelectionExtension extension) {
		super(parent, multi, context, scope, elementKinds, extension);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PHPUiPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = PHPUiPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}
}
