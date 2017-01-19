package org.eclipse.php.internal.ui.viewsupport;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.ast.references.TypeReference;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.dltk.ui.viewsupport.ScriptElementLabelComposer;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.php.core.compiler.IPHPModifiers;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.core.codeassist.CodeAssistUtils;
import org.eclipse.php.internal.core.compiler.ast.nodes.PHPDocBlock;
import org.eclipse.php.internal.core.compiler.ast.nodes.PHPDocTag;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;
import org.eclipse.php.internal.ui.PHPUIMessages;

public class PHPElementLabelComposer extends ScriptElementLabelComposer {

	private class InternalClassType {
		private int offset;

		private String typeName;
	}

	private final String MIXED_RETURN_TYPE = "mixed"; //$NON-NLS-1$
	private final String VOID_RETURN_TYPE = "void"; //$NON-NLS-1$
	private final String QUESTION_MARK = "?"; //$NON-NLS-1$

	/**
	 * User-readable string for reference ("&").
	 */
	public final static String REFERENCE_STRING = "&"; //$NON-NLS-1$

	/**
	 * Creates a new java element composer based on the given buffer.
	 * 
	 * @param buffer
	 *            the buffer
	 */
	public PHPElementLabelComposer(FlexibleBuffer buffer) {
		super(buffer);
	}

	/**
	 * Creates a new java element composer based on the given buffer.
	 * 
	 * @param buffer
	 *            the buffer
	 */
	public PHPElementLabelComposer(StyledString buffer) {
		super(buffer);
	}

	/**
	 * Creates a new java element composer based on the given buffer.
	 * 
	 * @param buffer
	 *            the buffer
	 */
	public PHPElementLabelComposer(StringBuffer buffer) {
		super(buffer);
	}

	public void getTypeLabel(IType type, long flags) {
		if (getFlag(flags, ScriptElementLabels.T_FULLY_QUALIFIED | ScriptElementLabels.T_CONTAINER_QUALIFIED)) {
			IModelElement elem = type.getParent();
			IType declaringType = (elem instanceof IType) ? (IType) elem : null;
			if (declaringType != null) {
				getTypeLabel(declaringType,
						ScriptElementLabels.T_CONTAINER_QUALIFIED | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
				fBuffer.append(getTypeDelimiter(elem));
			}
			int parentType = type.getParent().getElementType();
			if (parentType == IModelElement.METHOD || parentType == IModelElement.FIELD) { // anonymous
				// or
				// local
				getElementLabel(type.getParent(),
						(parentType == IModelElement.METHOD ? ScriptElementLabels.M_FULLY_QUALIFIED
								: ScriptElementLabels.F_FULLY_QUALIFIED)
								| (flags & ScriptElementLabels.QUALIFIER_FLAGS));
				fBuffer.append(getTypeDelimiter(elem));
			}
		}

		String typeName = getElementName(type);
		if (typeName.length() == 0) { // anonymous
			try {
				if (type.getParent() instanceof IField) {
					typeName = '{' + ScriptElementLabels.ELLIPSIS_STRING + '}';
				} else {
					String[] superNames = type.getSuperClasses();
					if (superNames != null) {
						int count = 0;
						typeName += ScriptElementLabels.DECL_STRING;
						for (int i = 0; i < superNames.length; ++i) {

							if (count > 0) {
								typeName += ScriptElementLabels.COMMA_STRING + " "; //$NON-NLS-1$
							}
							typeName += superNames[i];
							count++;
						}
					}
				}
			} catch (ModelException e) {
				// ignore
				typeName = ""; //$NON-NLS-1$
			}
		}

		fBuffer.append(typeName);

		// post qualification
		if (getFlag(flags, ScriptElementLabels.T_POST_QUALIFIED)) {
			int offset = fBuffer.length();
			IModelElement elem = type.getParent();
			IType declaringType = (elem instanceof IType) ? (IType) elem : null;
			if (declaringType != null) {
				fBuffer.append(ScriptElementLabels.CONCAT_STRING);
				getTypeLabel(declaringType,
						ScriptElementLabels.T_FULLY_QUALIFIED | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
				int parentType = type.getParent().getElementType();
				if (parentType == IModelElement.METHOD || parentType == IModelElement.FIELD) { // anonymous
					// or
					// local
					fBuffer.append(getTypeDelimiter(elem));
					getElementLabel(type.getParent(), 0);
				}
			}
			int parentType = type.getParent().getElementType();
			if (parentType == IModelElement.METHOD || parentType == IModelElement.FIELD
					|| parentType == IModelElement.SOURCE_MODULE) { // anonymous
				// or
				// local
				fBuffer.append(ScriptElementLabels.CONCAT_STRING);
				long qualifiedFlag = 0;
				switch (parentType) {
				case IModelElement.METHOD:
					qualifiedFlag = ScriptElementLabels.M_FULLY_QUALIFIED;
					break;
				case IModelElement.FIELD:
					qualifiedFlag = ScriptElementLabels.F_FULLY_QUALIFIED;
					break;
				case IModelElement.SOURCE_MODULE:
					qualifiedFlag = ScriptElementLabels.CU_QUALIFIED;
					break;
				}
				getElementLabel(type.getParent(), qualifiedFlag | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
			}
			if (getFlag(flags, ScriptElementLabels.COLORIZE)) {
				fBuffer.setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
			}
		}
	}

	protected void getMethodLabel(IMethod method, long flags) {
		try {
			InternalClassType classType = getReturnType(method);

			// qualification
			if (getFlag(flags, ScriptElementLabels.M_FULLY_QUALIFIED)) {
				IType type = method.getDeclaringType();
				if (type != null) {
					getTypeLabel(type,
							ScriptElementLabels.T_FULLY_QUALIFIED | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
					fBuffer.append("::");
				}
			}

			fBuffer.append(getElementName(method));

			// parameters
			fBuffer.append('(');
			getMethodParameters(method, flags);
			fBuffer.append(')');

			if (getFlag(flags, ScriptElementLabels.M_APP_RETURNTYPE) && method.exists() && !method.isConstructor()) {
				String typeName = getElementName(classType.typeName, method.getSourceModule(), classType.offset);
				if (typeName == null) {
					if ((method.getFlags() & IPHPModifiers.AccReturn) != 0) {
						typeName = MIXED_RETURN_TYPE;
					} else {
						typeName = VOID_RETURN_TYPE;
					}
				}
				int offset = fBuffer.length();
				fBuffer.append(ScriptElementLabels.DECL_STRING);
				if (PHPFlags.isNullable(method.getFlags())) {
					fBuffer.append(QUESTION_MARK);
				}
				fBuffer.append(typeName);
				if (getFlag(flags, ScriptElementLabels.COLORIZE) && offset != fBuffer.length()) {
					fBuffer.setStyle(offset, fBuffer.length() - offset, DECORATIONS_STYLE);
				}
			}

			// post qualification
			if (getFlag(flags, ScriptElementLabels.M_POST_QUALIFIED)) {
				IType declaringType = method.getDeclaringType();
				if (declaringType != null) {
					int offset = fBuffer.length();
					fBuffer.append(ScriptElementLabels.CONCAT_STRING);
					getTypeLabel(declaringType,
							ScriptElementLabels.T_FULLY_QUALIFIED | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
					if (getFlag(flags, ScriptElementLabels.COLORIZE)) {
						fBuffer.setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
					}
				}
			}
		} catch (ModelException e) {
			e.printStackTrace();
		}
	}

	protected void getMethodParameters(IMethod method, long flags) throws ModelException {
		if (getFlag(flags, ScriptElementLabels.M_PARAMETER_TYPES | ScriptElementLabels.M_PARAMETER_NAMES)) {
			if (method.exists()) {
				final boolean bNames = getFlag(flags, ScriptElementLabels.M_PARAMETER_NAMES);
				final boolean bTypes = getFlag(flags, ScriptElementLabels.M_PARAMETER_TYPES);
				final boolean bInitializers = getFlag(flags, ScriptElementLabels.M_PARAMETER_INITIALIZERS);
				final IParameter[] params = method.getParameters();
				final boolean isVariadic = PHPFlags.isVariadic(method.getFlags());
				for (int i = 0, nParams = params.length; i < nParams; i++) {
					if (i > 0) {
						fBuffer.append(ScriptElementLabels.COMMA_STRING);
					}
					boolean isLast = i + 1 == nParams;
					if (bTypes) {
						InternalClassType parameterType = getParameterType(method, params[i]);
						if (parameterType.typeName != null) {
							if (PHPFlags.isNullable(params[i].getFlags())) {
								fBuffer.append(QUESTION_MARK);
							}
							fBuffer.append(getElementName(parameterType.typeName, method.getSourceModule(),
									parameterType.offset));
							if (bNames) {
								fBuffer.append(' ');
							} else {
								if (PHPFlags.isReference(params[i].getFlags())) {
									fBuffer.append(REFERENCE_STRING);
								}
								if (isLast && isVariadic) {
									fBuffer.append(ScriptElementLabels.ELLIPSIS_STRING);
								}
							}
						} else if (!bNames) {
							if (PHPFlags.isReference(params[i].getFlags())) {
								fBuffer.append(REFERENCE_STRING);
							}
							if (isLast && isVariadic) {
								fBuffer.append(ScriptElementLabels.ELLIPSIS_STRING);
							}
							fBuffer.append(params[i].getName());
						}
					}
					if (bNames) {
						if (PHPFlags.isReference(params[i].getFlags())) {
							fBuffer.append(REFERENCE_STRING);
						}
						if (isLast && isVariadic) {
							fBuffer.append(ScriptElementLabels.ELLIPSIS_STRING);
						}
						fBuffer.append(params[i].getName());
					}
					if (bInitializers && params[i].getDefaultValue() != null) {
						fBuffer.append("=");
						fBuffer.append(params[i].getDefaultValue());
					}
				}
			}
		} else if (method.getParameters().length > 0) {
			fBuffer.append(ScriptElementLabels.ELLIPSIS_STRING);
		}
	}

	protected void getFieldLabel(IField field, long flags) {
		String typeName = getFieldType(field);
		if (getFlag(flags, ScriptElementLabels.F_PRE_TYPE_SIGNATURE) && field.exists()) {
			if (typeName != null) {
				fBuffer.append(typeName);
				fBuffer.append(' ');
			}
		}

		// qualification
		if (getFlag(flags, ScriptElementLabels.F_FULLY_QUALIFIED)) {
			IType type = field.getDeclaringType();
			if (type != null) {
				getTypeLabel(type,
						ScriptElementLabels.T_FULLY_QUALIFIED | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
				fBuffer.append("::");
			}
		}
		fBuffer.append(field.getElementName());
		if (getFlag(flags, ScriptElementLabels.F_APP_TYPE_SIGNATURE) && field.exists()) {
			if (typeName != null) {
				int offset = fBuffer.length();
				fBuffer.append(ScriptElementLabels.DECL_STRING);
				fBuffer.append(typeName);
				if (getFlag(flags, ScriptElementLabels.COLORIZE) && offset != fBuffer.length()) {
					fBuffer.setStyle(offset, fBuffer.length() - offset, DECORATIONS_STYLE);
				}
			}
		}
		// post qualification
		if (getFlag(flags, ScriptElementLabels.M_POST_QUALIFIED)) {
			IType declaringType = field.getDeclaringType();
			if (declaringType != null) {
				int offset = fBuffer.length();
				fBuffer.append(ScriptElementLabels.CONCAT_STRING);
				getTypeLabel(declaringType,
						ScriptElementLabels.T_FULLY_QUALIFIED | (flags & ScriptElementLabels.QUALIFIER_FLAGS));
				if (getFlag(flags, ScriptElementLabels.COLORIZE) && offset != fBuffer.length()) {
					fBuffer.setStyle(offset, fBuffer.length() - offset, QUALIFIER_STYLE);
				}
			}
		}
	}

	protected String getElementName(String typeName, ISourceModule sourceModule, int offset) {
		return PHPModelUtils.extractElementName(typeName);
	}

	/**
	 * @since 2.0
	 */
	protected void getImportContainerLabel(IModelElement element, long flags) {
		fBuffer.append(PHPUIMessages.PHPOutlineContentProvider_useStatementsNode);
	}

	private InternalClassType getParameterType(IMethod method, IParameter parameter) {
		InternalClassType type = new InternalClassType();
		try {
			if (parameter.getType() != null) {
				type.typeName = parameter.getType();
				type.offset = method.getSourceRange().getOffset();
				return type;
			} else {
				PHPDocBlock[] blocks = getPHPDocBlock(method);
				for (PHPDocBlock block : blocks) {
					PHPDocTag[] tags = block.getTags(PHPDocTag.TagKind.PARAM);
					for (PHPDocTag tag : tags) {
						if (tag.getVariableReference().getName().equals(parameter.getName())) {
							type.typeName = tag.getSingleTypeReference().getName();
							type.offset = tag.getSingleTypeReference().start();
							return type;
						}
					}
				}
			}
		} catch (Exception e) {
		}
		return type;
	}

	private InternalClassType getReturnType(IMethod method) {
		InternalClassType classType = new InternalClassType();
		try {
			classType.typeName = method.getType();
			PHPDocBlock[] blocks = getPHPDocBlock(method);
			for (PHPDocBlock block : blocks) {
				PHPDocTag[] tags = block.getTags(PHPDocTag.TagKind.RETURN);
				for (PHPDocTag tag : tags) {
					List<TypeReference> refs = tag.getTypeReferences();
					if (refs.size() == 1) {
						classType.typeName = refs.get(0).getName();
						classType.offset = refs.get(0).start();
						return classType;
					}
				}
			}
		} catch (ModelException e) {
		}
		return classType;
	}

	private String getFieldType(IField field) {
		IType[] types = null;
		String typeName = null;
		int offset = 0;
		try {
			if (field.getType() == null) {
				PHPDocBlock[] blocks = getPHPDocBlock(field);
				for (PHPDocBlock block : blocks) {
					PHPDocTag[] tags = block.getTags(PHPDocTag.TagKind.VAR);
					if (tags.length > 0) {
						typeName = tags[0].getTypeReferences().get(0).getName();
						offset = tags[0].getTypeReferences().get(0).start();
					}
					break;
				}
			} else {
				typeName = field.getType();
				offset = field.getSourceRange().getOffset();
			}
			if (typeName == null) {
				types = CodeAssistUtils.getVariableType(field.getSourceModule(), field.getElementName(),
						field.getSourceRange().getOffset());
				if (types.length > 0) {
					typeName = types[0].getElementName();
					offset = field.getSourceRange().getOffset();
				}
			}
			if (typeName != null) {
				return getElementName(typeName, field.getSourceModule(), offset);
			}
		} catch (ModelException e) {
		}
		return typeName;
	}

	private PHPDocBlock[] getPHPDocBlock(IMember member) {
		try {
			IType parent = member.getDeclaringType();
			if (parent == null || member.getSourceModule() instanceof IExternalSourceModule) {
				return new PHPDocBlock[0];
			}
			if (member instanceof IMethod && parent.getSuperClasses() != null && parent.getSuperClasses().length > 0) {
				return PHPModelUtils.getTypeHierarchyMethodDoc(member.getDeclaringType(), member.getElementName(), true,
						null);
			}
			PHPDocBlock block = null;
			if (member instanceof IType) {
				block = PHPModelUtils.getDocBlock((IType) member);
			} else if (member instanceof IMethod) {
				block = PHPModelUtils.getDocBlock((IMethod) member);
			} else if (member instanceof IField) {
				block = PHPModelUtils.getDocBlock((IField) member);
			}
			if (block != null) {
				return new PHPDocBlock[] { block };
			}
		} catch (CoreException e) {
		}
		return new PHPDocBlock[0];
	}

}
