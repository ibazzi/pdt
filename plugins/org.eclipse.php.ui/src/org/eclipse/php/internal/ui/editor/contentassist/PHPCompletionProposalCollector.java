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
package org.eclipse.php.internal.ui.editor.contentassist;

import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.ui.text.completion.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.internal.core.codeassist.CompletionFlag;
import org.eclipse.php.internal.core.codeassist.IPHPCompletionRequestor;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.project.PHPNature;
import org.eclipse.php.internal.ui.util.PHPPluginImages;
import org.eclipse.swt.graphics.Image;

public class PHPCompletionProposalCollector extends ScriptCompletionProposalCollector
		implements IPHPCompletionRequestor {

	private static final String DOUBLE_COLON = "::";//$NON-NLS-1$
	private static final String EMPTY_STRING = "";//$NON-NLS-1$
	private IDocument document;
	private boolean explicit;
	private int offset;
	private int flags = CompletionFlag.DEFAULT;

	public PHPCompletionProposalCollector(IDocument document, ISourceModule cu, boolean explicit) {
		super(cu);
		this.document = document;
		this.explicit = explicit;
	}

	protected ScriptCompletionProposal createMagicMethodOverloadCompletionProposal(IMethod method,
			IScriptProject scriptProject, ISourceModule compilationUnit, String name, String[] paramTypes, int start,
			int length, StyledString label, String string) {
		return new PHPMagicMethodOverloadCompletionProposal(method, scriptProject, compilationUnit, name, paramTypes,
				start, length, label, string);
	}

	protected ScriptCompletionProposal createOverrideCompletionProposal(IScriptProject scriptProject,
			ISourceModule compilationUnit, String name, String[] paramTypes, int start, int length, StyledString label,
			String string) {
		return new PHPOverrideCompletionProposal(scriptProject, compilationUnit, name, paramTypes, start, length, label,
				string);
	}

	protected IScriptCompletionProposal createPackageProposal(CompletionProposal proposal) {
		final AbstractScriptCompletionProposal scriptProposal = (AbstractScriptCompletionProposal) super.createPackageProposal(
				proposal);
		final IModelElement modelElement = proposal.getModelElement();
		if (modelElement != null) {
			scriptProposal.setProposalInfo(new ProposalInfo(modelElement.getScriptProject(), proposal.getName()));
		}
		return scriptProposal;
	}

	protected IScriptCompletionProposal createKeywordProposal(CompletionProposal proposal) {
		AbstractScriptCompletionProposal scriptProposal = (AbstractScriptCompletionProposal) super.createKeywordProposal(
				proposal);
		final IModelElement modelElement = proposal.getModelElement();
		if (modelElement != null && modelElement.getElementType() == IModelElement.SOURCE_MODULE) {
			scriptProposal.setImage(PHPPluginImages.get(PHPPluginImages.IMG_OBJS_PHP_FILE));
		}
		return scriptProposal;
	}

	protected IScriptCompletionProposal createScriptCompletionProposal(CompletionProposal proposal) {
		if (proposal.getKind() == CompletionProposal.METHOD_DECLARATION) {
			return createMethodDeclarationProposal(proposal);
		}
		return super.createScriptCompletionProposal(proposal);
	}

	protected char[] getVarTrigger() {
		// variable proposal will be inserted automatically if one of these
		// characters
		// is being typed in showing proposal time:
		return null;
	}

	public IDocument getDocument() {
		return document;
	}

	public boolean isExplicit() {
		return explicit;
	}

	private IScriptCompletionProposal createMethodDeclarationProposal(CompletionProposal proposal) {
		if (getSourceModule() == null || getScriptProject() == null) {
			return null;
		}

		String name = proposal.getName();

		String[] paramTypes = CharOperation.NO_STRINGS;
		String completion = proposal.getCompletion();

		int start = proposal.getReplaceStart();
		int length = getLength(proposal);
		StyledString label = ((PHPCompletionProposalLabelProvider) getLabelProvider())
				.createStyledOverrideMethodProposalLabel(proposal);
		ScriptCompletionProposal scriptProposal = null;
		if (ProposalExtraInfo.isMagicMethodOverload(proposal.getExtraInfo())) {
			scriptProposal = createMagicMethodOverloadCompletionProposal((IMethod) proposal.getModelElement(),
					getScriptProject(), getSourceModule(), name, paramTypes, start, length, label, completion);
		} else {
			scriptProposal = createOverrideCompletionProposal(getScriptProject(), getSourceModule(), name, paramTypes,
					start, length, label, completion);
		}
		if (scriptProposal == null)
			return null;
		scriptProposal.setImage(getImage(getLabelProvider().createMethodImageDescriptor(proposal)));

		ProposalInfo info = new MethodProposalInfo(getScriptProject(), proposal);
		scriptProposal.setProposalInfo(info);

		scriptProposal.setRelevance(computeRelevance(proposal));
		return scriptProposal;
	}

	protected IScriptCompletionProposal createMethodReferenceProposal(final CompletionProposal proposal) {
		if (getSourceModule() == null || getSourceModule().getScriptProject() == null) {
			return null;
		}

		String completion = proposal.getCompletion();
		int replaceStart = proposal.getReplaceStart();
		int length = getLength(proposal);

		StyledString displayString = ((PHPCompletionProposalLabelProvider) getLabelProvider())
				.createStyledMethodProposalLabel(proposal);

		AbstractScriptCompletionProposal scriptProposal = null;
		if (ProposalExtraInfo.isNotInsertUse(proposal.getExtraInfo())) {
			Image image = getImage(
					((PHPCompletionProposalLabelProvider) getLabelProvider()).createMethodImageDescriptor(proposal));

			String fullName = proposal.getModelElement().getElementName();
			scriptProposal = new PHPTypeCompletionProposal(completion, getSourceModule(), replaceStart, length, image,
					displayString, 0, fullName) {
				public String getReplacementString() {
					IMethod method = (IMethod) proposal.getModelElement();
					if (ProposalExtraInfo.isNoInsert(proposal.getExtraInfo())) {
						return method.getElementName();
					}
					setReplacementString(method.getFullyQualifiedName("\\")); //$NON-NLS-1$
					return super.getReplacementString();
				}

				@Override
				public Object getExtraInfo() {
					return proposal.getExtraInfo();
				}

			};
		} else {
			scriptProposal = ParameterGuessingProposal.createProposal(proposal, getInvocationContext(), false);
		}

		scriptProposal.setImage(getImage(getLabelProvider().createMethodImageDescriptor(proposal)));

		ProposalInfo info = new MethodProposalInfo(getSourceModule().getScriptProject(), proposal);
		scriptProposal.setProposalInfo(info);

		scriptProposal.setRelevance(computeRelevance(proposal));
		return scriptProposal;
	}

	protected IScriptCompletionProposal createTypeProposal(final CompletionProposal typeProposal) {
		Image image = getImage(
				((PHPCompletionProposalLabelProvider) getLabelProvider()).createTypeImageDescriptor(typeProposal));
		AbstractScriptCompletionProposal scriptProposal = new LazyPHPTypeCompletionProposal(typeProposal,
				getInvocationContext());
		typeProposal.setRelevance(scriptProposal.getRelevance());
		scriptProposal.setImage(image);
		scriptProposal.setRelevance(computeRelevance(typeProposal));
		scriptProposal.setProposalInfo(new TypeProposalInfo(getSourceModule().getScriptProject(), typeProposal));
		return scriptProposal;

	}

	protected IScriptCompletionProposal createFieldProposal(final CompletionProposal proposal) {
		return super.createFieldProposal(proposal);
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	protected String getNatureId() {
		return PHPNature.ID;
	}

	@Override
	public int computeRelevance(CompletionProposal proposal) {
		// if (ProposalExtraInfo.STUB.equals(proposal.getExtraInfo())) {
		// return Integer.MAX_VALUE;
		// }
		if (proposal.getModelElement() instanceof IMethod && ProposalExtraInfo.isMagicMethod(proposal.getExtraInfo())) {
			return -1;
		}
		if (proposal.getModelElement() instanceof IField && ProposalExtraInfo.isMagicMethod(proposal.getExtraInfo())) {
			return Integer.MAX_VALUE;
		}
		return super.computeRelevance(proposal);
	}

	public boolean filter(int flag) {
		return (flags & flag) != 0;
	}

	public void addFlag(int flag) {
		flags |= flag;
	}
}
