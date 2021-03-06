package org.eclipse.php.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.viewsupport.BasicElementLabels;
import org.eclipse.php.core.ast.nodes.ASTNode;
import org.eclipse.php.core.ast.nodes.Comment;
import org.eclipse.php.core.ast.nodes.Identifier;
import org.eclipse.php.core.ast.nodes.Program;
import org.eclipse.php.internal.core.ast.rewrite.ASTRewrite;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;
import org.eclipse.php.internal.ui.text.correction.proposals.ASTRewriteCorrectionProposal;
import org.eclipse.php.internal.ui.text.correction.proposals.AddImportCorrectionProposal;
import org.eclipse.php.internal.ui.text.correction.proposals.CUCorrectionProposal;
import org.eclipse.php.internal.ui.util.Messages;
import org.eclipse.php.ui.text.correction.IInvocationContext;
import org.eclipse.php.ui.text.correction.IProblemLocation;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class UnresolvedElementsSubProcessor {

	public static void getTypeProposals(IInvocationContext context, IProblemLocation problem, Collection proposals)
			throws CoreException {
		ISourceModule cu = context.getCompilationUnit();

		ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}

		while (selectedNode.getLocationInParent() == Identifier.NAME_PROPERTY) {
			selectedNode = selectedNode.getParent();
		}

		Identifier node = null;
		if (selectedNode instanceof Identifier) {
			node = ((Identifier) selectedNode);
		} else if (selectedNode instanceof Comment) {
			int start = problem.getOffset();
			int end = problem.getOffset() + problem.getLength();
			String nodeName = cu.getSource().substring(start, end);
			node = new Identifier(start, end, context.getASTRoot().getAST(), nodeName);
			node.setParent(selectedNode, Comment.COMMENT_TYPE_PROPERTY);
			selectedNode = node;
		} else {
			return;
		}

		int kind = evauateTypeKind(selectedNode);

		addSimilarTypeProposals(kind, cu, node, 3, proposals);
	}

	private static void addSimilarTypeProposals(int kind, ISourceModule cu, Identifier node, int relevance,
			Collection proposals) throws CoreException {
		SimilarElement[] elements = SimilarElementsRequestor.findSimilarElement(cu, node, kind);

		// add all similar elements
		for (int i = 0; i < elements.length; i++) {
			SimilarElement elem = elements[i];
			if ((elem.getKind() & SimilarElementsRequestor.ALL_TYPES) != 0) {
				String fullName = elem.getName();
				proposals.add(createTypeRefChangeProposal(cu, fullName, node, relevance, elements.length));
			}
		}
	}

	private static CUCorrectionProposal createTypeRefChangeProposal(ISourceModule cu, String fullName, Identifier node,
			int relevance, int maxProposals) {
		ImportRewrite importRewrite = null;
		String simpleName = PHPModelUtils.extractElementName(fullName);
		String packName = PHPModelUtils.extractNameSpaceName(fullName);
		if (packName == null) {
			packName = "";
		}
		if (packName.endsWith(SimilarElementsRequestor.ENCLOSING_TYPE_SEPARATOR)) {
			packName = packName.substring(0, packName.length() - 1);
		}
		if (packName.equals("")) {
			packName = "global namespace";
		}
		// variables
		importRewrite = ImportRewrite.create((Program) node.getRoot(), true);
		simpleName = PHPModelUtils.extractElementName(importRewrite.addImport(fullName));

		if (simpleName == null)
			return null;

		if (!isLikelyTypeName(simpleName)) {
			relevance -= 2;
		}

		ASTRewriteCorrectionProposal proposal;
		if (importRewrite != null && simpleName.equals(((Identifier) node).getName())) {
			String[] arg = { simpleName, packName };
			String label = Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_importtype_description,
					arg);
			Image image = DLTKPluginImages.get(DLTKPluginImages.IMG_OBJS_IMPDECL);
			int boost = QualifiedTypeNameHistory.getBoost(fullName, 0, maxProposals);
			proposal = new AddImportCorrectionProposal(label, cu, relevance + 100 + boost, image, packName, simpleName,
					(Identifier) node);
		} else {
			String label;
			if (packName.length() == 0) {
				label = Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetype_nopack_description,
						BasicElementLabels.getJavaElementName(simpleName));
			} else {
				String[] arg = { BasicElementLabels.getJavaElementName(simpleName),
						BasicElementLabels.getJavaElementName(packName) };
				label = Messages.format(CorrectionMessages.UnresolvedElementsSubProcessor_changetype_description, arg);
			}
			ASTRewrite rewrite = ASTRewrite.create(node.getAST());
			rewrite.replace(node, rewrite.createStringPlaceholder(simpleName, ASTNode.IDENTIFIER), null);
			Image image = DLTKPluginImages.get(DLTKPluginImages.IMG_CORRECTION_CHANGE);
			proposal = new ASTRewriteCorrectionProposal(label, cu, rewrite, relevance, image);
		}
		if (importRewrite != null) {
			proposal.setImportRewrite(importRewrite);
		}

		return proposal;
	}

	private static boolean isLikelyTypeName(String name) {
		return name.length() > 0 && Character.isUpperCase(name.charAt(0));
	}

	private static int evauateTypeKind(ASTNode node) {
		int kind = ASTResolving.getPossibleTypeKinds(node);
		return kind;
	}

}
