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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.ui.text.completion.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.internal.core.codeassist.AliasField;
import org.eclipse.php.internal.core.codeassist.CompletionFlag;
import org.eclipse.php.internal.core.codeassist.IPHPCompletionRequestor;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.project.PHPNature;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.internal.ui.util.PHPPluginImages;
import org.eclipse.swt.graphics.Image;

public class PHPCompletionProposalCollector extends ScriptCompletionProposalCollector
		implements IPHPCompletionRequestor {

	private static final String DOUBLE_COLON = "::";//$NON-NLS-1$
	private static final String EMPTY_STRING = "";//$NON-NLS-1$
	private final Set<String> fSuggestedMethodNames = new HashSet<String>();
	private IDocument document;
	private boolean explicit;
	private int offset;
	private int flags = CompletionFlag.DEFAULT;

	public PHPCompletionProposalCollector(IDocument document, ISourceModule cu, boolean explicit) {
		super(cu);
		this.document = document;
		this.explicit = explicit;
	}

	@Override
	protected void processUnprocessedProposal(CompletionProposal proposal) {
		if (proposal.getKind() == CompletionProposal.POTENTIAL_METHOD_DECLARATION) {
			acceptPotentialMethodDeclaration(proposal);
		} else {
			super.processUnprocessedProposal(proposal);
		}
	}

	private void acceptPotentialMethodDeclaration(CompletionProposal proposal) {
		ISourceModule sourceModule = getSourceModule();
		if (sourceModule == null)
			return;
		try {
			IModelElement element = sourceModule.getElementAt(proposal.getCompletionLocation());
			if (element != null) {
				IType type = (IType) element.getAncestor(IModelElement.TYPE);
				if (type != null) {

					List<IScriptCompletionProposal> scriptProposals = new ArrayList<IScriptCompletionProposal>();
					String prefix = String.valueOf(proposal.getName());
					int completionStart = proposal.getReplaceStart();
					int completionEnd = proposal.getReplaceEnd();
					int relevance = computeRelevance(proposal);
					GetterSetterCompletionProposal.evaluateProposals(type, prefix, completionStart,
							completionEnd - completionStart, relevance + 1, fSuggestedMethodNames, scriptProposals);
					PHPMethodDeclarationCompletionProposal.evaluateProposals(type, prefix, completionStart,
							completionEnd - completionStart, relevance, fSuggestedMethodNames, scriptProposals);
					for (IScriptCompletionProposal p : scriptProposals) {
						this.addProposal(p, proposal);
					}
				}
			}
		} catch (CoreException e) {
			PHPUiPlugin.log(e);
		}
	}

	protected ScriptCompletionProposal createMagicMethodOverloadCompletionProposal(IMethod method,
			IScriptProject scriptProject, ISourceModule compilationUnit, String name, String[] paramTypes, int start,
			int length, StyledString label, String string) {
		return new PHPMagicMethodOverloadCompletionProposal(method, scriptProject, compilationUnit, name, paramTypes,
				start, length, label, string);
	}

	@Override
	protected ScriptCompletionProposal createOverrideCompletionProposal(IScriptProject scriptProject,
			ISourceModule compilationUnit, String name, String[] paramTypes, int start, int length, StyledString label,
			String string) {
		return new PHPOverrideCompletionProposal(scriptProject, compilationUnit, name, paramTypes, start, length, label,
				string);
	}

	@Override
	protected IScriptCompletionProposal createPackageProposal(CompletionProposal proposal) {
		final AbstractScriptCompletionProposal scriptProposal = (AbstractScriptCompletionProposal) super.createPackageProposal(
				proposal);
		final IModelElement modelElement = proposal.getModelElement();
		if (modelElement != null) {
			scriptProposal.setProposalInfo(new ProposalInfo(modelElement.getScriptProject(), proposal.getName()));
		}
		return scriptProposal;
	}

	@Override
	protected IScriptCompletionProposal createKeywordProposal(CompletionProposal proposal) {
		AbstractScriptCompletionProposal scriptProposal = (AbstractScriptCompletionProposal) super.createKeywordProposal(
				proposal);
		final IModelElement modelElement = proposal.getModelElement();
		if (modelElement != null && modelElement.getElementType() == IModelElement.SOURCE_MODULE) {
			scriptProposal.setImage(PHPPluginImages.get(PHPPluginImages.IMG_OBJS_PHP_FILE));
		}
		return scriptProposal;
	}

	@Override
	protected IScriptCompletionProposal createScriptCompletionProposal(CompletionProposal proposal) {
		if (proposal.getKind() == CompletionProposal.METHOD_DECLARATION) {
			return createMethodDeclarationProposal(proposal);
		}
		return super.createScriptCompletionProposal(proposal);
	}

	@Override
	protected char[] getVarTrigger() {
		// variable proposal will be inserted automatically if one of these
		// characters
		// is being typed in showing proposal time:
		return null;
	}

	@Override
	public IDocument getDocument() {
		return document;
	}

	@Override
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
		fSuggestedMethodNames.add(name);
		return scriptProposal;
	}

	@Override
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
				@Override
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

	@Override
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

	@Override
	protected IScriptCompletionProposal createFieldProposal(final CompletionProposal proposal) {
		String completion = String.valueOf(proposal.getCompletion());
		int start = proposal.getReplaceStart();
		int length = getLength(proposal);
		StyledString displayString = ((PHPCompletionProposalLabelProvider) getLabelProvider())
				.createStyledFieldProposalLabel(proposal);
		Image image = getImage(
				((PHPCompletionProposalLabelProvider) getLabelProvider()).createFieldImageDescriptor(proposal));

		ScriptCompletionProposal scriptProposal = new ScriptCompletionProposal(completion, start, length, image,
				displayString, 0, false) {
			private boolean fReplacementStringComputed = false;

			@Override
			public String getReplacementString() {
				if (!fReplacementStringComputed) {
					String replacementString = computeReplacementString();
					if (ProposalExtraInfo.isAddQuote(proposal.getExtraInfo())) {
						replacementString = "'" + replacementString + "'"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					setReplacementString(replacementString);
				}
				fReplacementStringComputed = true;
				return super.getReplacementString();
			}

			private String computeReplacementString() {
				IField field = (IField) proposal.getModelElement();
				if (field instanceof AliasField) {
					AliasField aliasField = (AliasField) field;
					return aliasField.getAlias();
				}
				if (ProposalExtraInfo.isFullName(proposal.getExtraInfo())) {
					return field.getFullyQualifiedName("\\"); //$NON-NLS-1$
				}
				return super.getReplacementString();
			}

			@Override
			protected boolean isValidPrefix(String prefix) {
				String word = getDisplayString();
				if (word.startsWith("$") && !prefix.startsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
					word = word.substring(1);
				}
				return isPrefix(prefix, word);
			}

			@Override
			protected boolean isSmartTrigger(char trigger) {
				return trigger == '$';
			}
		};
		if (getSourceModule().getScriptProject() != null) {
			scriptProposal.setProposalInfo(new FieldProposalInfo(getSourceModule().getScriptProject(), proposal));
		}
		scriptProposal.setRelevance(computeRelevance(proposal));
		scriptProposal.setTriggerCharacters(getVarTrigger());
		return scriptProposal;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
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

	@Override
	public boolean filter(int flag) {
		return (flags & flag) != 0;
	}

	@Override
	public void addFlag(int flag) {
		flags |= flag;
	}
}
