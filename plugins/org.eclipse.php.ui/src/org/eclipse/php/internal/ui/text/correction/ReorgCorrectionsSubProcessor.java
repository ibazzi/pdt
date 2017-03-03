package org.eclipse.php.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.jface.text.IDocument;
import org.eclipse.php.internal.ui.Logger;
import org.eclipse.php.internal.ui.text.correction.proposals.AbstractCorrectionProposal;
import org.eclipse.php.internal.ui.text.correction.proposals.RemoveUnusedUseStatementProposal;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ReorgCorrectionsSubProcessor {

	private final static String ORGANIZE_USE_STATEMENTS_ID = "org.eclipse.php.ui.editor.organize.use.statements"; //$NON-NLS-1$

	public static void removeImportStatementProposals(IInvocationContext context, IProblemLocation problem,
			Collection proposals) {
		RemoveUnusedUseStatementProposal proposal = new RemoveUnusedUseStatementProposal(context, problem, 5);
		proposals.add(new RunCommandProposal());
		proposals.add(proposal);
	}

	static private class RunCommandProposal extends AbstractCorrectionProposal {

		public RunCommandProposal() {
			super(CorrectionMessages.ReorgCorrectionsSubProcessor_organizeimports_description, 10,
					DLTKPluginImages.get(DLTKPluginImages.IMG_CORRECTION_CHANGE), ORGANIZE_USE_STATEMENTS_ID);
		}

		@Override
		public void apply(IDocument document) {
			ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
			try {
				service.getCommand(getCommandId()).executeWithChecks(new ExecutionEvent());
			} catch (Exception e) {
				Logger.logException(e);
			}
		}

		@Override
		public String getAdditionalProposalInfo() {
			return null;
		}
	}

}
