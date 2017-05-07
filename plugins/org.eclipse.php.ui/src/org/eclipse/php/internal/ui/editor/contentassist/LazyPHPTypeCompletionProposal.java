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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.php.core.ast.nodes.ASTParser;
import org.eclipse.php.core.ast.nodes.NamespaceDeclaration;
import org.eclipse.php.core.ast.nodes.Program;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.php.internal.core.codeassist.AliasType;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.ui.editor.SharedASTProvider;
import org.eclipse.text.edits.TextEdit;

/**
 * If passed source module is not null, the replacement string will be seen as a
 * qualified type name.
 */
public class LazyPHPTypeCompletionProposal extends LazyScriptCompletionProposal {
	/** Triggers for types. Do not modify. */
	protected static final char[] TYPE_TRIGGERS = new char[] { '\\', '\t', '[', '(', ' ' };
	/** Triggers for types in phpdoc. Do not modify. */
	protected static final char[] PHPDOC_TYPE_TRIGGERS = new char[] { '#', '}', ' ', '\\' };

	protected static final String NAMESPACE_DELIMITER = NamespaceReference.NAMESPACE_DELIMITER;

	/** The source module, or <code>null</code> if none is available. */
	protected final ISourceModule fSourceModule;

	private String fQualifiedName;
	private String fSimpleName;
	private String fAlias;
	private ImportRewrite fImportRewrite;
	private ImportRewriteContext fImportContext;
	private boolean fSortStringComputed;

	public LazyPHPTypeCompletionProposal(CompletionProposal proposal, ScriptContentAssistInvocationContext context) {
		super(proposal, context);
		fSourceModule = context.getSourceModule();
		fQualifiedName = null;
		initAlias();
	}

	void setImportRewrite(ImportRewrite importRewrite) {
		fImportRewrite = importRewrite;
	}

	public final String getQualifiedTypeName() {
		if (fQualifiedName == null) {
			fQualifiedName = ((IType) getModelElement()).getFullyQualifiedName(NAMESPACE_DELIMITER);
		}
		return fQualifiedName;
	}

	protected final String getSimpleTypeName() {
		if (fSimpleName == null) {
			fSimpleName = fProposal.getModelElement().getElementName();
		}
		if (fAlias != null) {
			return fAlias;
		}
		return fSimpleName;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#
	 * computeReplacementString()
	 */
	@Override
	protected String computeReplacementString() {
		if (fAlias != null) {
			return fAlias;
		}
		boolean isNamespace = false;
		try {
			isNamespace = PHPFlags.isNamespace(((IType) getModelElement()).getFlags());
		} catch (ModelException e) {
		}

		String replacement = super.computeReplacementString();

		/* No import rewriting ever from within the import section. */
		if (isImportCompletion())
			return replacement;

		String qualifiedTypeName = getQualifiedTypeName();

		// for namespace in use statement context
		if (ProposalExtraInfo.isClassInNamespace(fProposal.getExtraInfo())) {
			return qualifiedTypeName;
		}

		if (!isNamespace && isGlobalNamespace(fSourceModule) && qualifiedTypeName.indexOf(NAMESPACE_DELIMITER) == -1) {
			return getSimpleTypeName();
		}

		// If the user types in the qualification, don't force import rewriting
		// on him - insert the qualified name.
		IDocument document = fInvocationContext.getDocument();
		if (document != null) {
			String prefix = getPrefix(document, getReplacementOffset() + getReplacementLength());
			// always consider namespace qualified
			if (isNamespace && !prefix.startsWith(NAMESPACE_DELIMITER)) {
				prefix = NAMESPACE_DELIMITER + prefix;
			}
			if (prefix.startsWith(NAMESPACE_DELIMITER)) {
				// match up to the last dot in order to make higher level
				// matching still work (camel case...)
				int dotIndex = prefix.lastIndexOf(NAMESPACE_DELIMITER);
				if (qualifiedTypeName.toLowerCase().startsWith(prefix.substring(1, dotIndex + 1).toLowerCase())) {
					if (!qualifiedTypeName.startsWith(NAMESPACE_DELIMITER)) {
						qualifiedTypeName = NAMESPACE_DELIMITER + qualifiedTypeName;
					}
					return qualifiedTypeName;
				}
			}
		}

		/* Add imports if the preference is on. */
		if (fImportRewrite == null)
			fImportRewrite = createImportRewrite();
		if (fImportRewrite != null) {
			NamespaceDeclaration namespace = fImportRewrite.getProgram()
					.getNamespaceDeclaration(getReplacementOffset());
			return fImportRewrite.addImport(namespace, qualifiedTypeName, fAlias, fImportContext);
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
		if (!ProposalExtraInfo.isClassInNamespace(fProposal.getExtraInfo())) {
			return false;
		}
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

	private void initAlias() {
		fAlias = null;
		IType type = (IType) getModelElement();
		if (type instanceof AliasType) {
			fAlias = ((AliasType) type).getAlias();
		}
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

	private Program getASTRoot(ISourceModule sourceModule) {
		Program root = null;
		try {
			IProgressMonitor monitor = new NullProgressMonitor();
			root = SharedASTProvider.getAST(sourceModule, SharedASTProvider.WAIT_NO, monitor);
			if (root == null) {
				ASTParser parser = ASTParser.newParser(sourceModule);
				root = parser.createAST(monitor);
			}
		} catch (Exception e) {
			PHPUiPlugin.log(e);
		}
		return root;
	}

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
		// IPreferenceStore preferenceStore=
		// JavaPlugin.getDefault().getPreferenceStore();
		// return
		// preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
	}

	@Override
	protected boolean isValidPrefix(String prefix) {
		boolean isPrefix = isPrefix(prefix, getSimpleTypeName());
		if (!isPrefix && prefix.indexOf(NAMESPACE_DELIMITER) != -1) {
			if (prefix.startsWith(NAMESPACE_DELIMITER)) {
				prefix = prefix.substring(1);
			}
			isPrefix = isPrefix(prefix, getQualifiedTypeName());
		}
		return isPrefix;
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		String prefix = getPrefix(document, completionOffset);

		String completion;
		// return the qualified name if the prefix is already qualified
		if (prefix.indexOf(NAMESPACE_DELIMITER) != -1)
			completion = getQualifiedTypeName();
		else
			completion = getSimpleTypeName();

		if (isCamelCaseMatching()) {
			completion = getCamelCaseCompound(prefix, completion);
		}

		if (prefix.startsWith(NAMESPACE_DELIMITER)) {
			completion = NAMESPACE_DELIMITER + completion;
		}

		return completion;
	}

	@Override
	protected char[] computeTriggerCharacters() {
		return isInDoc() ? PHPDOC_TYPE_TRIGGERS : TYPE_TRIGGERS;
	}

	@Override
	public final String getSortString() {
		if (!fSortStringComputed)
			setSortString(computeSortString());
		return super.getSortString();
	}

	@Override
	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		int start = super.getPrefixCompletionStart(document, completionOffset);
		String prefix = getPrefix(document, completionOffset);
		if (prefix.startsWith(NAMESPACE_DELIMITER)) {
			start++;
		}
		return start;
	}

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

	protected String computeSortString() {
		// try fast sort string to avoid display string creation
		return getSimpleTypeName() + Character.MIN_VALUE + getQualifiedTypeName();
	}

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

	@Override
	public IModelElement getModelElement() {
		IModelElement element = super.getModelElement();
		while (!(element instanceof IType)) {
			element = element.getParent();
		}
		return element;
	}

	private boolean isGlobalNamespace(ISourceModule sourceModule) {
		return PHPModelUtils.getCurrentNamespace(sourceModule, getReplacementOffset()) == null;
	}

}
