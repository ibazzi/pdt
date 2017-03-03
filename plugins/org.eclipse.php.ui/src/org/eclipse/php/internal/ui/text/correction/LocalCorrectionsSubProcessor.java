package org.eclipse.php.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.php.internal.core.ast.nodes.*;
import org.eclipse.php.internal.core.ast.rewrite.ASTRewrite;
import org.eclipse.php.internal.core.ast.rewrite.ListRewrite;
import org.eclipse.php.internal.ui.text.correction.proposals.ASTRewriteCorrectionProposal;
import org.eclipse.php.internal.ui.text.correction.proposals.UnimplementedMethodsCorrectionProposal;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class LocalCorrectionsSubProcessor {

	public static void addUnimplementedMethodsProposals(IInvocationContext context, IProblemLocation problem,
			Collection proposals) {
		ISourceModule cu = context.getCompilationUnit();
		ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		ASTNode typeNode = null;
		boolean hasProposal = false;
		if (selectedNode.getType() == ASTNode.IDENTIFIER) {
			typeNode = selectedNode;
			while ((typeNode = typeNode.getParent()) != null) {
				if (typeNode instanceof ClassDeclaration) {
					if (((ClassDeclaration) typeNode).resolveTypeBinding() != null) {
						hasProposal = true;
					}
					break;
				} else if (typeNode instanceof AnonymousClassDeclaration) {
					hasProposal = true;
					break;
				}
			}
		}
		if (hasProposal) {
			UnimplementedMethodsCorrectionProposal proposal = new UnimplementedMethodsCorrectionProposal(cu, typeNode,
					IProposalRelevance.ADD_UNIMPLEMENTED_METHODS);
			proposals.add(proposal);
		}
		if (typeNode instanceof ClassDeclaration) {
			ClassDeclaration typeDeclaration = (ClassDeclaration) typeNode;
			ModifierCorrectionSubProcessor.addMakeTypeAbstractProposal(context, typeDeclaration, proposals, cu);
		}
	}

	public static void getInterfaceExtendsClassProposals(IInvocationContext context, IProblemLocation problem,
			Collection<ICommandAccess> proposals) {
		ASTNode selectedNode = problem.getCoveringNode(context.getASTRoot());
		if (selectedNode == null) {
			return;
		}
		while (selectedNode.getParent() instanceof NamespaceName) {
			selectedNode = selectedNode.getParent();
		}

		StructuralPropertyDescriptor locationInParent = selectedNode.getLocationInParent();
		if (locationInParent != ClassDeclaration.SUPER_CLASS_PROPERTY) {
			return;
		}

		ClassDeclaration typeDecl = (ClassDeclaration) selectedNode.getParent();
		{
			ASTRewrite rewrite = ASTRewrite.create(context.getASTRoot().getAST());
			ASTNode placeHolder = rewrite.createMoveTarget(selectedNode);
			ListRewrite interfaces = rewrite.getListRewrite(typeDecl, ClassDeclaration.INTERFACES_PROPERTY);
			interfaces.insertFirst(placeHolder, null);

			String label = CorrectionMessages.LocalCorrectionsSubProcessor_extendstoimplements_description;
			Image image = DLTKPluginImages.get(DLTKPluginImages.IMG_CORRECTION_CHANGE);
			ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
					context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_EXTENDS_TO_IMPLEMENTS, image);
			proposals.add(proposal);
		}
		{
//			ASTRewrite rewrite = ASTRewrite.create(context.getASTRoot().getAST());
//
//			rewrite.set(typeDecl, ClassDeclaration.INTERFACES_PROPERTY, Boolean.TRUE, null);
//
//			String typeName = typeDecl.getName().getName();
//			String label = Messages.format(CorrectionMessages.LocalCorrectionsSubProcessor_classtointerface_description,
//					BasicElementLabels.getJavaElementName(typeName));
//			Image image = DLTKPluginImages.get(DLTKPluginImages.IMG_CORRECTION_CHANGE);
//			ASTRewriteCorrectionProposal proposal = new ASTRewriteCorrectionProposal(label,
//					context.getCompilationUnit(), rewrite, IProposalRelevance.CHANGE_CLASS_TO_INTERFACE, image);
//			proposals.add(proposal);
		}
	}
}
