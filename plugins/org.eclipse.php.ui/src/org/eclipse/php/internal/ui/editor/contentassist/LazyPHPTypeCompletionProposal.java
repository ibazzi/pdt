/*******************************************************************************
 * Copyright (c) 2017 Alex Xu and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Alex Xu - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.ui.editor.contentassist;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.internal.corext.util.QualifiedTypeNameHistory;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.text.completion.ICompletionProposalInfo;
import org.eclipse.dltk.ui.text.completion.LazyScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.ScriptContentAssistInvocationContext;
import org.eclipse.dltk.ui.text.completion.TypeProposalInfo;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.php.core.ast.nodes.NamespaceDeclaration;
import org.eclipse.php.core.ast.nodes.Program;
import org.eclipse.php.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.ui.editor.SharedASTProvider;
import org.eclipse.text.edits.TextEdit;

/**
 * If passed compilation unit is not null, the replacement string will be seen
 * as a qualified type name.
 */
public class LazyPHPTypeCompletionProposal extends LazyScriptCompletionProposal {
	/** Triggers for types. Do not modify. */
	protected static final char[] TYPE_TRIGGERS = new char[] { '\\', '\t', '[', '(', ' ' };
	/** Triggers for types in javadoc. Do not modify. */
	protected static final char[] JDOC_TYPE_TRIGGERS = new char[] { '#', '}', ' ', '.' };

	/** The compilation unit, or <code>null</code> if none is available. */
	protected final ISourceModule fSourceModule;

	private String fQualifiedName;
	private String fSimpleName;
	private ImportRewrite fImportRewrite;
	private ImportRewriteContext fImportContext;

	public LazyPHPTypeCompletionProposal(CompletionProposal proposal, ScriptContentAssistInvocationContext context) {
		super(proposal, context);
		fSourceModule = context.getSourceModule();
		fQualifiedName = null;
	}

	void setImportRewrite(ImportRewrite importRewrite) {
		fImportRewrite = importRewrite;
	}

	public final String getQualifiedTypeName() {
		if (fQualifiedName == null)
			fQualifiedName = ((IType) getModelElement()).getFullyQualifiedName(NamespaceReference.NAMESPACE_DELIMITER);
		return fQualifiedName;
	}

	protected final String getSimpleTypeName() {
		if (fSimpleName == null)
			fSimpleName = fProposal.getModelElement().getElementName();
		return fSimpleName;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeReplacementString()
	 */
	@Override
	protected String computeReplacementString() {
		String replacement = super.computeReplacementString();

		/* No import rewriting ever from within the import section. */
		if (isImportCompletion())
			return replacement;

		String qualifiedTypeName = getQualifiedTypeName();

		if (ProposalExtraInfo.isClassInNamespace(fProposal.getExtraInfo())) {
			if (ProposalExtraInfo.isFullName(fProposal.getExtraInfo()))
				return qualifiedTypeName;
			return getSimpleTypeName();
		}

		if (isGlobalNamespace(fSourceModule)
				&& qualifiedTypeName.indexOf(NamespaceReference.NAMESPACE_DELIMITER) == -1) {
			return qualifiedTypeName;
		}

		/*
		 * If the user types in the qualification, don't force import rewriting
		 * on him - insert the qualified name.
		 */
		IDocument document = fInvocationContext.getDocument();
		if (document != null) {
			String prefix = getPrefix(document, getReplacementOffset() + getReplacementLength());
			int dotIndex = prefix.lastIndexOf(NamespaceReference.NAMESPACE_DELIMITER);
			// match up to the last dot in order to make higher level matching
			// still work (camel case...)
			if (dotIndex != -1
					&& qualifiedTypeName.toLowerCase().startsWith(prefix.substring(0, dotIndex + 1).toLowerCase()))
				return qualifiedTypeName;
		}

		/*
		 * The replacement does not contain a qualification (e.g. an inner type
		 * qualified by its parent) - use the replacement directly.
		 */
		if (replacement.indexOf(NamespaceReference.NAMESPACE_DELIMITER) == -1 && isInDoc()) {
			return getSimpleTypeName(); // don't use the braces added for
										// javadoc link proposals
		}

		/* Add imports if the preference is on. */
		if (fImportRewrite == null)
			fImportRewrite = createImportRewrite();
		if (fImportRewrite != null) {
			NamespaceDeclaration namespace = fImportRewrite.getProgram()
					.getNamespaceDeclaration(getReplacementOffset());
			return fImportRewrite.addImport(namespace, qualifiedTypeName, fImportContext);
		}

		// fall back for the case we don't have an import rewrite (see
		// allowAddingImports)

		/* No imports for implicit imports. */
		// if (fSourceModule != null
		// &&
		// JavaModelUtil.isImplicitImport(Signature.getQualifier(qualifiedTypeName),
		// fSourceModule)) {
		// return Signature.getSimpleName(qualifiedTypeName);
		// }

		/* Default: use the fully qualified type name. */
		return qualifiedTypeName;
	}

	protected final boolean isImportCompletion() {
		String completion = fProposal.getCompletion();
		if (completion.length() == 0)
			return false;

		char last = completion.charAt(completion.length() - 1);
		/*
		 * Proposals end in a semicolon when completing types in normal imports
		 * or when completing static members, in a period when completing types
		 * in static imports.
		 */
		return last == ';' || last == NamespaceReference.NAMESPACE_SEPARATOR;
	}

	private ImportRewrite createImportRewrite() {
		if (fSourceModule != null && allowAddingImports()) {
			Program cu = getASTRoot(fSourceModule);
			if (cu != null) {
				return ImportRewrite.create(cu, true);
			}
		}
		return null;
	}

	private Program getASTRoot(ISourceModule compilationUnit) {
		try {
			return SharedASTProvider.getAST(compilationUnit, SharedASTProvider.WAIT_YES, new NullProgressMonitor());
		} catch (ModelException | IOException e) {
			PHPUiPlugin.log(e);
		}
		return null;
	}

	/*
	 * @see
	 * org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#apply(
	 * org.eclipse.jface.text.IDocument, char, int)
	 */
	@Override
	public void apply(IDocument document, char trigger, int offset) {
		try {
			boolean insertClosingParenthesis = trigger == '(' && autocloseBrackets();
			if (insertClosingParenthesis) {
				StringBuffer replacement = new StringBuffer(getReplacementString());
				updateReplacementWithParentheses(replacement);
				setReplacementString(replacement.toString());
				trigger = '\0';
			}

			super.apply(document, trigger, offset);

			if (fImportRewrite != null && fImportRewrite.hasRecordedChanges()) {
				int oldLen = document.getLength();
				fImportRewrite.rewriteImports(new NullProgressMonitor()).apply(document, TextEdit.UPDATE_REGIONS);
				setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
			}

			if (insertClosingParenthesis)
				setUpLinkedMode(document, ')');

			rememberSelection();
		} catch (CoreException e) {
			PHPUiPlugin.log(e);
		} catch (BadLocationException e) {
			PHPUiPlugin.log(e);
		}
	}

	protected void updateReplacementWithParentheses(StringBuffer replacement) {
		replacement.append(LPAREN);
		setCursorPosition(replacement.length());
		replacement.append(RPAREN);
	}

	/**
	 * Remembers the selection in the content assist history.
	 *
	 * @throws JavaModelException
	 *             if anything goes wrong
	 * @since 3.2
	 */
	protected final void rememberSelection() {
		IType lhs = fInvocationContext.getExpectedType();
		IType rhs = (IType) getModelElement();
		if (lhs != null && rhs != null)
			DLTKUIPlugin.getDefault().getContentAssistHistory().remember(lhs, rhs);

		QualifiedTypeNameHistory.remember(getQualifiedTypeName());
	}

	/**
	 * Returns <code>true</code> if imports may be added. The return value
	 * depends on the context and preferences only and does not take into
	 * account the contents of the compilation unit or the kind of proposal.
	 * Even if <code>true</code> is returned, there may be cases where no
	 * imports are added for the proposal. For example:
	 * <ul>
	 * <li>when completing within the import section</li>
	 * <li>when completing informal javadoc references (e.g. within
	 * <code>&lt;code&gt;</code> tags)</li>
	 * <li>when completing a type that conflicts with an existing import</li>
	 * <li>when completing an implicitly imported type (same package,
	 * <code>java.lang</code> types)</li>
	 * </ul>
	 * <p>
	 * The decision whether a qualified type or the simple type name should be
	 * inserted must take into account these different scenarios.
	 * </p>
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 *
	 * @return <code>true</code> if imports may be added, <code>false</code> if
	 *         not
	 */
	protected boolean allowAddingImports() {
		if (isInDoc()) {
			if (fProposal.getKind() == CompletionProposal.TYPE_REF && fInvocationContext.getCoreContext().isInDoc())
				return false;
		}
		return true;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * isValidPrefix(java.lang.String)
	 */
	@Override
	protected boolean isValidPrefix(String prefix) {
		boolean isPrefix = isPrefix(prefix, getSimpleTypeName());
		if (!isPrefix && prefix.indexOf(NamespaceReference.NAMESPACE_DELIMITER) != -1) {
			isPrefix = isPrefix(prefix, getQualifiedTypeName());
		}
		return isPrefix;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal#
	 * getCompletionText()
	 */
	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		String prefix = getPrefix(document, completionOffset);

		String completion;
		// return the qualified name if the prefix is already qualified
		if (prefix.indexOf(NamespaceReference.NAMESPACE_DELIMITER) != -1)
			completion = getQualifiedTypeName();
		else
			completion = getSimpleTypeName();

		if (isCamelCaseMatching())
			return getCamelCaseCompound(prefix, completion);

		return completion;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeTriggerCharacters()
	 */
	@Override
	protected char[] computeTriggerCharacters() {
		return isInDoc() ? JDOC_TYPE_TRIGGERS : TYPE_TRIGGERS;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeProposalInfo()
	 */
	@Override
	protected ICompletionProposalInfo computeProposalInfo() {
		IScriptProject project;
		if (fSourceModule != null)
			project = fSourceModule.getScriptProject();
		else
			project = fInvocationContext.getProject();
		if (project != null)
			return new TypeProposalInfo(project, fProposal);

		return super.computeProposalInfo();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeSortString()
	 */
	// @Override
	// protected String computeSortString() {
	// // try fast sort string to avoid display string creation
	// return getSimpleTypeName() + Character.MIN_VALUE +
	// getQualifiedTypeName();
	// }

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeRelevance()
	 */
	@Override
	protected int computeRelevance() {
		/*
		 * There are two histories: the RHS history remembers types used for the
		 * current expected type (left hand side), while the type history
		 * remembers recently used types in general).
		 *
		 * The presence of an RHS ranking is a much more precise sign for
		 * relevance as it proves the subtype relationship between the proposed
		 * type and the expected type.
		 *
		 * The "recently used" factor (of either the RHS or general history) is
		 * less important, it should not override other relevance factors such
		 * as if the type is already imported etc.
		 */
		float rhsHistoryRank = fInvocationContext.getHistoryRelevance(getQualifiedTypeName());
		float typeHistoryRank = QualifiedTypeNameHistory.getDefault().getNormalizedPosition(getQualifiedTypeName());

		int recencyBoost = Math.round((rhsHistoryRank + typeHistoryRank) * 5);
		int rhsBoost = rhsHistoryRank > 0.0f ? 50 : 0;
		int baseRelevance = super.computeRelevance();

		return baseRelevance + rhsBoost + recencyBoost;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeContextInformation()
	 * 
	 * @since 3.3
	 */
	@Override
	protected IContextInformation computeContextInformation() {

		// IParameter[] parameters = method.getParameters();
		// char[] signature = fProposal.getSignature();
		// char[][] typeParameters = Signature.getTypeArguments(signature);
		// if (typeParameters.length == 0)
		// return super.computeContextInformation();
		//
		// ProposalContextInformation contextInformation = new
		// ProposalContextInformation(fProposal);
		// if (fContextInformationPosition != 0 &&
		// fProposal.getCompletion().length == 0)
		// contextInformation.setContextInformationPosition(fContextInformationPosition);
		// return contextInformation;
		return null;

	}

	@Override
	public IModelElement getModelElement() {
		IModelElement element = super.getModelElement();
		while (!(element instanceof IType)) {
			element = element.getParent();
		}
		return element;
	}

	private boolean isGlobalNamespace(ISourceModule sourceModule) {
		IType namespace = PHPModelUtils.getCurrentNamespace(sourceModule, getReplacementOffset());
		return namespace == null;
	}

}
