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
package org.eclipse.php.internal.core.codeassist.strategies;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.internal.core.SourceType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.php.core.codeassist.ICompletionContext;
import org.eclipse.php.core.codeassist.IElementFilter;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.PHPVersion;
import org.eclipse.php.internal.core.codeassist.ICompletionReporter;
import org.eclipse.php.internal.core.codeassist.ProposalExtraInfo;
import org.eclipse.php.internal.core.codeassist.contexts.AbstractCompletionContext;
import org.eclipse.php.internal.core.codeassist.contexts.ClassMemberContext;
import org.eclipse.php.internal.core.codeassist.contexts.ClassMemberContext.Trigger;
import org.eclipse.php.internal.core.codeassist.contexts.ClassStatementContext;
import org.eclipse.php.internal.core.codeassist.contexts.GlobalMethodStatementContext;
import org.eclipse.php.internal.core.language.PHPMagicMethods;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;

/**
 * This strategy completes class methods
 * 
 * @author michael
 */
public class ClassMethodsStrategy extends ClassMembersStrategy {

	private ISourceRange fReplaceRange = null;
	private boolean fExactName;
	private String fPrefix;
	private String fSuffix;
	private Set<String> fMagicMethods = new HashSet<String>();
	private ICompletionReporter fReporter;

	public ClassMethodsStrategy(ICompletionContext context, IElementFilter elementFilter) {
		super(context, elementFilter);
	}

	public ClassMethodsStrategy(ICompletionContext context) {
		super(context);
	}

	public void apply(ICompletionReporter reporter) throws BadLocationException {
		fReporter = reporter;
		AbstractCompletionContext concreteContext = (AbstractCompletionContext) getContext();
		CompletionRequestor requestor = concreteContext.getCompletionRequestor();
		fPrefix = concreteContext.getPrefix().isEmpty() ? concreteContext.getPreviousWord()
				: concreteContext.getPrefix();
		fSuffix = getSuffix(concreteContext);
		if (fSuffix.equals("")) { //$NON-NLS-1$
			fReplaceRange = getReplacementRange(concreteContext);
		} else {
			fReplaceRange = getReplacementRangeWithBraces(concreteContext);
		}

		PHPVersion phpVersion = concreteContext.getPhpVersion();
		fMagicMethods.addAll(Arrays.asList(PHPMagicMethods.getMethods(phpVersion)));

		fExactName = requestor.isContextInformationMode();
		// for methodName(|),we need set exactName to true
		if (!fExactName && concreteContext.getOffset() - 1 >= 0
				&& concreteContext.getDocument().getChar(concreteContext.getOffset() - 1) == '(') {
			fExactName = true;
		}
		List<IMethod> result = new LinkedList<IMethod>();

		if (concreteContext instanceof ClassMemberContext) {
			processContext((ClassMemberContext) concreteContext, result);
		} else if (concreteContext instanceof GlobalMethodStatementContext) {
			processContext((GlobalMethodStatementContext) concreteContext, result);
		} else if (concreteContext instanceof ClassStatementContext) {
			processContext((ClassStatementContext) concreteContext, result);
		}
	}

	private void processContext(ClassStatementContext concreteContext, List<IMethod> result) {
		IType type = concreteContext.getEnclosingType();
		if (type == null) {
			return;
		}
		try {
			ITypeHierarchy hierarchy = getCompanion().getSuperTypeHierarchy(type, null);
			IMethod[] methods = PHPModelUtils.getSuperTypeHierarchyMethod(type, hierarchy, fPrefix, fExactName, null);
			for (IMethod method : removeOverriddenElements(Arrays.asList(methods))) {
				int flags = method.getFlags();
				if (!type.getMethod(method.getElementName()).exists()
						&& (PHPFlags.isPublic(flags) || PHPFlags.isProtected(flags)) && !PHPFlags.isFinal(flags)) {
					if (fMagicMethods.contains(method.getElementName())) {
						fMagicMethods.remove(method.getElementName());
					}
					fReporter.reportMethod(method, fSuffix, fReplaceRange, ProposalExtraInfo.METHOD_OVERRIDE, 10);
				}
			}
			for (String magicMethod : fMagicMethods) {
				if (!type.getMethod(magicMethod).exists() && magicMethod.startsWith(fPrefix)) {
					IMethod fakeMethod = PHPMagicMethods.createMethod((SourceType) type, magicMethod);
					fReporter.reportMethod(fakeMethod, fSuffix, fReplaceRange, ProposalExtraInfo.MAGIC_METHOD_OVERLOAD);
				}
			}
		} catch (CoreException e) {
			PHPCorePlugin.log(e);
		}
	}

	private void processContext(GlobalMethodStatementContext concreteContext, List<IMethod> result)
			throws BadLocationException {

		IType type = concreteContext.getEnclosingType();
		if (type == null) {
			return;
		}
		IMethod enclosingMethod = concreteContext.getEnclosingMethod();
		try {
			ITypeHierarchy hierarchy = getCompanion().getSuperTypeHierarchy(type, null);

			IMethod[] methods = PHPModelUtils.getTypeHierarchyMethod(type, hierarchy, fPrefix, fExactName, null);

			boolean inConstructor = enclosingMethod.isConstructor();
			for (IMethod method : removeOverriddenElements(Arrays.asList(methods))) {
				if (PHPFlags.isPrivate(method.getFlags()))
					continue;
				if (concreteContext.isInUseTraitStatement()) {
					// result.add(method);
					fReporter.reportMethod((IMethod) method, "", //$NON-NLS-1$
							fReplaceRange, ProposalExtraInfo.METHOD_ONLY);
				} else if (!PHPModelUtils.isConstructor(method)) {
					result.add(method);
				}
			}
		} catch (CoreException e) {
			PHPCorePlugin.log(e);
		}

		for (IMethod method : result) {
			fReporter.reportMethod((IMethod) method, fSuffix, fReplaceRange, ProposalExtraInfo.INSERT_THIS);
		}
	}

	private void processContext(ClassMemberContext concreteContext, List<IMethod> result) throws BadLocationException {
		boolean isParentCall = isParentCall(concreteContext);
		for (IType type : concreteContext.getLhsTypes()) {
			try {
				ITypeHierarchy hierarchy = getCompanion().getSuperTypeHierarchy(type, null);

				IMethod[] methods = isParentCall
						? PHPModelUtils.getSuperTypeHierarchyMethod(type, hierarchy, fPrefix, fExactName, null)
						: PHPModelUtils.getTypeHierarchyMethod(type, hierarchy, fPrefix, fExactName, null);

				boolean inConstructor = isInConstructor(type, type.getMethods(), concreteContext);
				for (IMethod method : removeOverriddenElements(Arrays.asList(methods))) {

					if (concreteContext.isInUseTraitStatement()) {
						// result.add(method);
						fReporter.reportMethod((IMethod) method, "", //$NON-NLS-1$
								fReplaceRange, ProposalExtraInfo.METHOD_ONLY);
					} else if ((!PHPModelUtils.isConstructor(method)
							|| inConstructor && isSuperConstructor(method, type, concreteContext))
							&& !isFiltered(method, type, concreteContext)) {
						if (fMagicMethods.contains(method.getElementName())) {
							fReporter.reportMethod(method, fSuffix, fReplaceRange, ProposalExtraInfo.MAGIC_METHOD);
						} else {
							result.add(method);
						}
					}
				}
			} catch (CoreException e) {
				PHPCorePlugin.log(e);
			}
		}

		for (IMethod method : result) {
			fReporter.reportMethod((IMethod) method, fSuffix, fReplaceRange);
		}
	}

	private boolean isInConstructor(IType type, IMethod[] methods, ClassMemberContext concreteContext) {
		try {
			for (int i = 0; i < methods.length; i++) {
				IMethod method = methods[i];
				if (PHPModelUtils.isConstructor(method) && method.getDeclaringType().equals(type)) {
					ISourceRange constructorRange = method.getSourceRange();
					if (concreteContext.getOffset() > constructorRange.getOffset() && concreteContext
							.getOffset() < constructorRange.getOffset() + constructorRange.getLength()) {
						return true;
					}
				}
			}
		} catch (ModelException e) {
		}
		return false;
	}

	private boolean isSuperConstructor(IMethod method, IType type, ClassMemberContext context) {
		if (PHPModelUtils.isConstructor(method) && context.getTriggerType() == Trigger.CLASS && isParent(context)
				&& !method.getDeclaringType().equals(type)) {
			return true;
		}
		return false;
	}

	/**
	 * is 'parent' keyword
	 * 
	 * @param context
	 * @return
	 */
	private boolean isParent(ClassMemberContext context) {
		return !isThisCall(context) && isParentCall(context) && isDirectParentCall(context);
	}

	// private IMethod getConstructor(IType type, IMethod[] methods) {
	// for (int i = 0; i < methods.length; i++) {
	// IMethod method = methods[i];
	// if (PHPModelUtils.isConstructor(method)) {
	// return method;
	// }
	// }
	//
	// return null;
	// }

	protected boolean showNonStaticMembers(ClassMemberContext context) {
		return super.showNonStaticMembers(context) || context.getTriggerType() == Trigger.CLASS;
	}

	public String getSuffix(AbstractCompletionContext abstractContext) throws BadLocationException {
		// look for method bracket or end of line
		IDocument document = abstractContext.getDocument();
		int offset = abstractContext.getOffset();
		while (document.getLength() > offset) {
			char ch = document.getChar(offset);
			if (ch == '(') {
				break;
			} else if (ch == '\n') {
				return "()"; //$NON-NLS-1$
			}
			offset++;
		}
		return ""; //$NON-NLS-1$
	}
}
