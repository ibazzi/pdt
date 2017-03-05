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

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.internal.ui.text.hover.CompletionHoverControlCreator;
import org.eclipse.dltk.ui.PreferenceConstants;
import org.eclipse.dltk.ui.text.ScriptTextTools;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposal;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.internal.core.PHPCoreConstants;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.ast.nodes.Program;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite;
import org.eclipse.php.internal.core.ast.util.Signature;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.ui.editor.SharedASTProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEdit;

public class PHPCompletionProposal extends ScriptCompletionProposal implements IPHPCompletionProposalExtension {

	/**
	 * The control creator.
	 */
	private IInformationControlCreator fCreator;
	private ISourceModule fSourceModule;
	/** The unqualified type name. */
	private final String fUnqualifiedTypeName;
	/** The fully qualified type name. */
	private final String fFullyQualifiedTypeName;

	@Deprecated
	public PHPCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image,
			String displayString, int relevance) {
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance);
		fUnqualifiedTypeName = null;
		fFullyQualifiedTypeName = null;
	}

	@Deprecated
	public PHPCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image,
			String displayString, int relevance, boolean indoc) {
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance, indoc);
		fUnqualifiedTypeName = null;
		fFullyQualifiedTypeName = null;
	}

	public PHPCompletionProposal(String replacementString, ISourceModule sourceModule, int replacementOffset,
			int replacementLength, Image image, StyledString displayString, int relevance,
			String fullyQualifiedTypeName) {
		this(replacementString, sourceModule, replacementOffset, replacementLength, image, displayString, relevance,
				false, fullyQualifiedTypeName);
	}

	public PHPCompletionProposal(String replacementString, ISourceModule sourceModule, int replacementOffset,
			int replacementLength, Image image, StyledString displayString, int relevance, boolean indoc,
			String fullyQualifiedTypeName) {
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance, indoc);
		this.fSourceModule = sourceModule;
		fFullyQualifiedTypeName = fullyQualifiedTypeName;
		fUnqualifiedTypeName = fullyQualifiedTypeName != null ? Signature.getSimpleName(fullyQualifiedTypeName) : null;
	}

	protected boolean isValidPrefix(String prefix) {
		String word = getDisplayString();
		if (word.startsWith("$") && !prefix.startsWith("$")) { //$NON-NLS-1$ //$NON-NLS-2$
			word = word.substring(1);
		}
		boolean result = isPrefix(prefix, word);
		if (!result && ProposalExtraInfo.isClassInNamespace(getExtraInfo())) {
			result = isPrefix(prefix, fUnqualifiedTypeName) || isPrefix(prefix, fFullyQualifiedTypeName);
		}
		return result;
	}

	protected boolean isSmartTrigger(char trigger) {
		return trigger == '$';
	}

	public void apply(IDocument document, char trigger, int offset) {
		try {
			ImportRewrite impRewrite = null;
			if (fSourceModule != null) {
				try {
					Program astRoot = SharedASTProvider.getAST(fSourceModule, SharedASTProvider.WAIT_YES,
							SubMonitor.convert(null, 2));
					impRewrite = ImportRewrite.create(astRoot, true);
				} catch (IOException e) {
					PHPUiPlugin.log(e);
				}
			}

			boolean updateCursorPosition = updateReplacementString(document, trigger, offset, impRewrite);

			if (updateCursorPosition)
				setCursorPosition(getReplacementString().length());

			super.apply(document, trigger, offset);

			if (impRewrite != null) {
				int oldLen = document.getLength();
				impRewrite.rewriteImports(new NullProgressMonitor()).apply(document, TextEdit.UPDATE_REGIONS);
				setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
			}
		} catch (CoreException e) {
			PHPUiPlugin.log(e);
		} catch (BadLocationException e) {
			PHPUiPlugin.log(e);
		}
	}

	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportRewrite impRewrite)
			throws CoreException, BadLocationException {
		// avoid adding imports when inside imports container
		if (impRewrite != null && fFullyQualifiedTypeName != null) {
			String replacementString = getReplacementString();
			String qualifiedType = fFullyQualifiedTypeName;
			if (qualifiedType.indexOf(NamespaceReference.NAMESPACE_DELIMITER) != -1
					&& replacementString.startsWith(qualifiedType)
					&& !replacementString.endsWith(String.valueOf(';'))) {
				IType[] types = impRewrite.getSourceModule().getTypes();
				if (types.length > 0 && types[0].getSourceRange().getOffset() <= offset) {
					// ignore positions above type.
					setReplacementString(impRewrite.addImport(getReplacementString()));
					return true;
				}
			}
		}
		return false;
	}

	public IContextInformation getContextInformation() {
		String displayString = getDisplayString();
		if (displayString.indexOf('(') == -1) {
			return null;
		}
		return super.getContextInformation();
	}

	protected boolean isCamelCaseMatching() {
		return true;
	}

	protected boolean insertCompletion() {
		return Platform.getPreferencesService().getBoolean(PHPCorePlugin.ID,
				PHPCoreConstants.CODEASSIST_INSERT_COMPLETION, true, null);
	}

	protected ScriptTextTools getTextTools() {
		return PHPUiPlugin.getDefault().getTextTools();
	}

	public IInformationControlCreator getInformationControlCreator() {
		if (fCreator == null) {
			fCreator = new CompletionHoverControlCreator(new IInformationControlCreator() {
				public IInformationControl createInformationControl(Shell parent) {
					if (BrowserInformationControl.isAvailable(parent)) {
						return new BrowserInformationControl(parent, PreferenceConstants.APPEARANCE_DOCUMENTATION_FONT,
								true);
					} else {
						return new DefaultInformationControl(parent, true);
					}
				}
			}, true);
		}
		return fCreator;
	}

	public Object getExtraInfo() {
		return ProposalExtraInfo.DEFAULT;
	}
}