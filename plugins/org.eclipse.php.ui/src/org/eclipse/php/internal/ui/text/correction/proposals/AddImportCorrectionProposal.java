package org.eclipse.php.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.jface.text.IDocument;
import org.eclipse.php.internal.core.ast.nodes.Identifier;
import org.eclipse.php.internal.core.ast.rewrite.ASTRewrite;
import org.eclipse.php.internal.ui.text.correction.SimilarElementsRequestor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;

@SuppressWarnings({ "restriction" })
public class AddImportCorrectionProposal extends ASTRewriteCorrectionProposal {

	private final String fTypeName;
	private final String fQualifierName;

	public AddImportCorrectionProposal(String name, ISourceModule cu, int relevance, Image image, String qualifierName,
			String typeName, Identifier node) {
		super(name, cu, ASTRewrite.create(node.getAST()), relevance, image);
		fTypeName = typeName;
		fQualifierName = qualifierName;
	}

	public String getQualifiedTypeName() {
		return fQualifierName + SimilarElementsRequestor.ENCLOSING_TYPE_SEPARATOR + fTypeName;
	}

	protected void performChange(IEditorPart activeEditor, IDocument document) throws CoreException {
		super.performChange(activeEditor, document);
		rememberSelection();
	}

	private void rememberSelection() {
		QualifiedTypeNameHistory.remember(getQualifiedTypeName());
	}

}