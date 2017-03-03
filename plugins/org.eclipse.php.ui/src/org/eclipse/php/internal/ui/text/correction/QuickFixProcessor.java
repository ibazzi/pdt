package org.eclipse.php.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.ui.text.completion.IScriptCompletionProposal;
import org.eclipse.php.internal.core.compiler.ast.parser.PhpProblemIdentifier;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class QuickFixProcessor implements IQuickFixProcessor, IQuickFixProcessorExtension {

	public IScriptCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		if (locations == null || locations.length == 0) {
			return null;
		}

		HashSet handledProblems = new HashSet(locations.length);
		ArrayList resultingCollections = new ArrayList();
		for (int i = 0; i < locations.length; i++) {
			IProblemLocation curr = locations[i];
			IProblemIdentifier id = curr.getProblemIdentifier();
			if (handledProblems.add(id)) {
				process(context, curr, resultingCollections);
			}
		}
		return (IScriptCompletionProposal[]) resultingCollections
				.toArray(new IScriptCompletionProposal[resultingCollections.size()]);
	}

	public boolean hasCorrections(ISourceModule unit, int problemId) {
		return false;
	}

	private void process(IInvocationContext context, IProblemLocation problem, Collection proposals)
			throws CoreException {
		PhpProblemIdentifier id = PhpProblemIdentifier.getProblem(problem.getProblemIdentifier());
		switch (id) {
		case UnusedImport:
		case DuplicateImport:
		case UnnecessaryImport:
			ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, problem, proposals);
			break;
		case ImportNotFound:
			ReorgCorrectionsSubProcessor.removeImportStatementProposals(context, problem, proposals);
			break;
		case ClassExtendFinalClass:
			ModifierCorrectionSubProcessor.addNonAccessibleReferenceProposal(context, problem, proposals, ModifierCorrectionSubProcessor.TO_NON_FINAL, IProposalRelevance.REMOVE_FINAL_MODIFIER);
			break;
		case AbstractMethodInAbstractClass:
		case BodyForAbstractMethod:
			ModifierCorrectionSubProcessor.addAbstractMethodProposals(context, problem, proposals);
			break;
		case AbstractMethodsInConcreteClass:
			ModifierCorrectionSubProcessor.addAbstractTypeProposals(context, problem, proposals);
			break;
		case MethodRequiresBody:
			ModifierCorrectionSubProcessor.addMethodRequiresBodyProposals(context, problem, proposals);
			break;
		case UndefinedType:
			UnresolvedElementsSubProcessor.getTypeProposals(context, problem, proposals);
			break;
		case AbstractMethodMustBeImplemented:
			LocalCorrectionsSubProcessor.addUnimplementedMethodsProposals(context, problem, proposals);
			break;
		case SuperclassMustBeAClass:
			LocalCorrectionsSubProcessor.getInterfaceExtendsClassProposals(context, problem, proposals);
			break;
		default:
			return;
		}
	}

	@Override
	public boolean hasCorrections(ISourceModule unit, IProblemIdentifier identifier) {
		PhpProblemIdentifier problem = PhpProblemIdentifier.getProblem(identifier);
		switch (problem) {
		case AbstractMethodInAbstractClass:
		case AbstractMethodsInConcreteClass:
		case BodyForAbstractMethod:
		case MethodRequiresBody:
		case AbstractMethodMustBeImplemented:
		case ClassExtendFinalClass:
		case DuplicateImport:
		case ImportNotFound:
		case SuperclassMustBeAClass:
		case UndefinedType:
		case UnnecessaryImport:
		case UnusedImport:
			return true;
		default:
			break;
		}
		return false;
	}

}
