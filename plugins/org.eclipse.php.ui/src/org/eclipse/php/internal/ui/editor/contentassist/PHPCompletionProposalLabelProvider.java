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

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.internal.core.ArchiveProjectFragment;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.ScriptElementImageDescriptor;
import org.eclipse.dltk.ui.ScriptElementImageProvider;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.ui.text.completion.CompletionProposalLabelProvider;
import org.eclipse.dltk.ui.text.completion.ICompletionProposalLabelProviderExtension;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalCollector;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.core.compiler.IPHPModifiers;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.core.codeassist.AliasField;
import org.eclipse.php.internal.core.codeassist.AliasMethod;
import org.eclipse.php.internal.core.codeassist.AliasType;
import org.eclipse.php.internal.core.language.PHPMagicMethods;
import org.eclipse.php.internal.core.typeinference.FakeConstructor;
import org.eclipse.php.internal.core.typeinference.FakeMethod;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;
import org.eclipse.php.internal.ui.Logger;
import org.eclipse.php.internal.ui.preferences.PreferenceConstants;
import org.eclipse.php.internal.ui.text.PHPTextMessages;
import org.eclipse.php.internal.ui.util.PHPPluginImages;
import org.eclipse.php.internal.ui.viewsupport.PHPElementLabelComposer;

public class PHPCompletionProposalLabelProvider extends CompletionProposalLabelProvider
		implements ICompletionProposalLabelProviderExtension {

	@Override
	protected StyledString createMethodProposalLabel(CompletionProposal methodProposal) {
		return createStyledMethodProposalLabel(methodProposal);
	}

	@Override
	protected StyledString createOverrideMethodProposalLabel(CompletionProposal methodProposal) {
		return createStyledOverrideMethodProposalLabel(methodProposal);
	}

	protected StyledString createStyledOverrideMethodProposalLabel(CompletionProposal methodProposal) {
		StyledString nameBuffer = new StyledString();
		IMethod method = (IMethod) methodProposal.getModelElement();

		if (method instanceof FakeConstructor && method.getParent() instanceof AliasType) {
			AliasType aliasType = (AliasType) method.getParent();
			nameBuffer.append(aliasType.getAlias());
		} else if (method instanceof AliasMethod) {
			AliasMethod aliasMethod = (AliasMethod) method;
			nameBuffer.append(aliasMethod.getAlias());
		} else {
			nameBuffer.append(method.getElementName());
		}
		// parameters
		nameBuffer.append('(');
		appendParameterList(nameBuffer, methodProposal);
		nameBuffer.append(')'); // $NON-NLS-1$

		appendMethodType(nameBuffer, methodProposal);

		if (method.getParent() != null) {
			nameBuffer.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			if (method instanceof FakeMethod && PHPMagicMethods.isMagicMethod(method.getElementName())) {
				nameBuffer.append(PHPTextMessages.ResultCollector_overloadingmagicmethod,
						StyledString.QUALIFIER_STYLER);
			} else {
				nameBuffer.append(Messages.format(PHPTextMessages.ResultCollector_overridingmethod,
						method.getParent().getElementName()), StyledString.QUALIFIER_STYLER);
			}
		}

		return nameBuffer;
	}

	protected void appendMethodType(StyledString nameBuffer, CompletionProposal methodProposal) {
		if (showMethodReturnType()) {
			IMethod method = (IMethod) methodProposal.getModelElement();
			if (method instanceof AliasMethod) {
				method = (IMethod) ((AliasMethod) method).getMethod();
			}
			if (method == null) {
				return;
			}
			try {
				if (method.isConstructor() || !method.exists()) {
					return;
				}
				nameBuffer.append(getReturnTypeSeparator());
				if (PHPFlags.isNullable(method.getFlags())) {
					nameBuffer.append("?"); //$NON-NLS-1$
				}
				String type = PHPModelUtils.extractElementName(method.getType());
				if (type == null) {
					if ((method.getFlags() & IPHPModifiers.AccReturn) != 0) {
						type = "mixed"; //$NON-NLS-1$
					} else {
						type = "void"; //$NON-NLS-1$
					}
				}
				nameBuffer.append(type);
			} catch (ModelException e) {
				Logger.logException(e);
			}
		}
	}

	protected void appendFieldType(StyledString nameBuffer, CompletionProposal methodProposal) {
		IField element = (IField) methodProposal.getModelElement();
		if (element instanceof AliasField) {
			element = (IField) ((AliasField) element).getField();
		}
		if (element == null) {
			return;
		}
		try {
			if (!element.exists()) {
				return;
			}
			String type = element.getType();
			if (type != null) {
				nameBuffer.append(getReturnTypeSeparator());
				nameBuffer.append(type);
			}
			if (!(element.getParent() instanceof IMethod)) {
				appendQualifier(nameBuffer, element.getParent());
			}
		} catch (ModelException e) {
			Logger.logException(e);
		}
	}

	private boolean showMethodReturnType() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_METHOD_RETURNTYPE);
	}

	@Override
	public StyledString createTypeProposalLabel(CompletionProposal typeProposal) {
		return createStyledTypeProposalLabel(typeProposal);
	}

	protected String createLabelWithTypeAndDeclaration(CompletionProposal proposal) {
		StringBuilder nameBuffer = new StringBuilder();

		nameBuffer.append(proposal.getName());
		IField field = (IField) proposal.getModelElement();
		if (field.getParent() != null) {

			nameBuffer.append(" - "); //$NON-NLS-1$
			IModelElement parent = field.getParent();
			nameBuffer.append(parent.getElementName());
		}

		return nameBuffer.toString();
	}

	@Override
	public ImageDescriptor createImageDescriptor(CompletionProposal proposal) {
		if (proposal.getModelElement() instanceof ArchiveProjectFragment) {
			return DLTKPluginImages.DESC_OBJS_JAR;
		}
		return super.createImageDescriptor(proposal);
	}

	@Override
	public ImageDescriptor createTypeImageDescriptor(CompletionProposal proposal) {
		if (PHPFlags.isTrait(proposal.getFlags())) {
			return decorateImageDescriptor(PHPPluginImages.DESC_OBJS_TRAIT, proposal);
		} else {
			return super.createTypeImageDescriptor(proposal);
		}
	}

	@Override
	public StyledString createFieldProposalLabel(CompletionProposal proposal) {
		return createStyledFieldProposalLabel(proposal);
	}

	@Override
	public ImageDescriptor createFieldImageDescriptor(CompletionProposal proposal) {
		return super.createFieldImageDescriptor(proposal);
	}

	@Override
	public ImageDescriptor createMethodImageDescriptor(CompletionProposal proposal) {
		return super.createMethodImageDescriptor(proposal);
	}

	@Override
	public StyledString createStyledFieldProposalLabel(CompletionProposal proposal) {
		StyledString buffer = new StyledString();
		if (proposal.getModelElement() instanceof AliasField) {
			AliasField aliasField = (AliasField) proposal.getModelElement();
			buffer.append(aliasField.getAlias());
		} else {
			buffer.append(proposal.getName());
		}
		IModelElement element = proposal.getModelElement();
		if (element != null && element.getElementType() == IModelElement.FIELD) {
			appendFieldType(buffer, proposal);
		}

		return buffer;
	}

	@Override
	public StyledString createStyledLabel(CompletionProposal proposal) {
		switch (proposal.getKind()) {
		case CompletionProposal.METHOD_NAME_REFERENCE:
		case CompletionProposal.METHOD_REF:
		case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
			return createStyledMethodProposalLabel(proposal);
		case CompletionProposal.METHOD_DECLARATION:
			return createStyledOverrideMethodProposalLabel(proposal);
		case CompletionProposal.TYPE_REF:
			return createStyledTypeProposalLabel(proposal);
		case CompletionProposal.FIELD_REF:
			return createStyledFieldProposalLabel(proposal);
		case CompletionProposal.LOCAL_VARIABLE_REF:
		case CompletionProposal.VARIABLE_DECLARATION:
			return createStyledSimpleLabelWithType(proposal);
		case CompletionProposal.KEYWORD:
			return createStyledKeywordLabel(proposal);
		case CompletionProposal.PACKAGE_REF:
		case CompletionProposal.LABEL_REF:
			return createStyledSimpleLabel(proposal);
		default:
			Assert.isTrue(false);
			return null;
		}
	}

	@Override
	protected ImageDescriptor decorateImageDescriptor(ImageDescriptor descriptor, CompletionProposal proposal) {
		if (proposal.isConstructor()) {
			int adornmentFlags = ScriptElementImageProvider.computeAdornmentFlags(proposal.getModelElement(),
					ScriptElementImageProvider.SMALL_ICONS | ScriptElementImageProvider.OVERLAY_ICONS);
			if (adornmentFlags == 0) {
				return descriptor;
			}
			adornmentFlags ^= ScriptElementImageDescriptor.CONSTRUCTOR;
			return new ScriptElementImageDescriptor(descriptor, adornmentFlags, ScriptElementImageProvider.SMALL_SIZE);
		} else {
			return super.decorateImageDescriptor(descriptor, proposal);
		}
	}

	protected StyledString createStyledMethodProposalLabel(CompletionProposal methodProposal) {
		StyledString nameBuffer = new StyledString();
		boolean isAlias = methodProposal.getModelElement() instanceof AliasMethod;

		// method name
		if (isAlias) {
			AliasMethod aliasMethod = (AliasMethod) methodProposal.getModelElement();
			nameBuffer.append(aliasMethod.getAlias());
		} else {
			nameBuffer.append(methodProposal.getName());
		}

		// parameters
		nameBuffer.append('(');
		appendParameterList(nameBuffer, methodProposal);
		nameBuffer.append(')');

		appendMethodType(nameBuffer, methodProposal);
		if (isAlias) {
			return nameBuffer;
		}

		IModelElement method = methodProposal.getModelElement();
		appendQualifier(nameBuffer, method.getParent());

		return nameBuffer;
	}

	@Override
	protected StyledString appendParameterList(StyledString buffer, CompletionProposal methodProposal) {
		IMethod method = (IMethod) methodProposal.getModelElement();
		if (method instanceof AliasMethod) {
			method = (IMethod) ((AliasMethod) method).getMethod();
		}
		IParameter[] parameters = null;
		boolean isVariadic = false;
		try {
			if (method != null) {
				parameters = method.getParameters();
				if (PHPFlags.isVariadic(method.getFlags())) {
					isVariadic = true;
				}
			}
		} catch (ModelException e) {
			Logger.logException(e);
		}

		if (parameters != null) {
			Integer paramLimit = (Integer) methodProposal
					.getAttribute(ScriptCompletionProposalCollector.ATTR_PARAM_LIMIT);
			if (paramLimit == null) {
				paramLimit = parameters.length;
			}
			return appendParameterSignature(buffer, parameters, isVariadic, paramLimit);
		}
		return buffer;
	}

	protected StyledString appendParameterSignature(StyledString buffer, IParameter[] parameters, boolean isVariadic,
			int paramLimit) {
		if (parameters != null) {
			for (int i = 0; i < paramLimit; i++) {
				if (i > 0) {
					buffer.append(',');
					buffer.append(' ');
				}
				if (parameters[i].getType() != null) {
					buffer.append(PHPModelUtils.extractElementName(parameters[i].getType()));
					buffer.append(' ');
				}
				if (parameters != null && i < parameters.length && PHPFlags.isReference(parameters[i].getFlags())) {
					buffer.append(PHPElementLabelComposer.REFERENCE_STRING);
				}
				if (isVariadic && i + 1 == parameters.length) {
					buffer.append(ScriptElementLabels.ELLIPSIS_STRING);
				}
				buffer.append(parameters[i].getName());
			}
		}
		return buffer;
	}

	@Override
	public String createLabel(CompletionProposal proposal) {
		return createStyledLabel(proposal).toString();
	}

	@Override
	public StyledString createStyledKeywordLabel(CompletionProposal proposal) {
		return new StyledString(proposal.getName());
	}

	@Override
	public StyledString createStyledSimpleLabel(CompletionProposal proposal) {
		return new StyledString(proposal.getName());
	}

	@Override
	public StyledString createStyledTypeProposalLabel(CompletionProposal typeProposal) {
		StyledString nameBuffer = new StyledString();

		IType type = (IType) typeProposal.getModelElement();
		if (type instanceof AliasType) {
			AliasType aliasType = (AliasType) type;
			nameBuffer.append(aliasType.getAlias());
		} else {
			nameBuffer.append(typeProposal.getName());
		}

		boolean isNamespace = false;
		try {
			isNamespace = PHPFlags.isNamespace(type.getFlags());
		} catch (ModelException e) {
			Logger.logException(e);
		}
		if (!isNamespace) {
			appendQualifier(nameBuffer, type.getParent());
		}

		return nameBuffer;
	}

	@Override
	public StyledString createStyledSimpleLabelWithType(CompletionProposal proposal) {
		StyledString buffer = new StyledString(proposal.getName());

		IModelElement element = proposal.getModelElement();
		if (element != null && element.getElementType() == IModelElement.LOCAL_VARIABLE && element.exists()) {
			final ILocalVariable var = (ILocalVariable) element;
			String type = var.getType();
			if (type != null) {
				buffer.append(getReturnTypeSeparator(), StyledString.DECORATIONS_STYLER);
				buffer.append(type, StyledString.QUALIFIER_STYLER);
			}
		}
		return buffer;
	}

	@Override
	protected StyledString createSimpleLabelWithType(CompletionProposal proposal) {
		return createStyledSimpleLabelWithType(proposal);
	}

	protected void appendQualifier(StyledString buffer, IModelElement modelElement) {
		if (modelElement == null) {
			return;
		}
		buffer.append(" - ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
		buffer.append(modelElement.getElementName(), StyledString.QUALIFIER_STYLER);
	}

}
