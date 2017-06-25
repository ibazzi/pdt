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
package org.eclipse.php.internal.ui.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.php.core.ast.nodes.*;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.ui.editor.highlighters.Messages;
import org.eclipse.php.internal.ui.preferences.PreferenceConstants;
import org.eclipse.swt.graphics.RGB;

/**
 * Semantic highlightings
 *
 */
public class SemanticHighlightings {

	/**
	 * A named preference part that controls the highlighting of constant
	 * fields.
	 */
	public static final String CONSTANT = "constant"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of static fields.
	 */
	public static final String STATIC_FIELD = "staticField"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of fields.
	 */
	public static final String FIELD = "field"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of super global
	 * variables.
	 */
	public static final String SUPER_GLOBAL = "superGlobal"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of method
	 * declarations.
	 */
	public static final String METHOD_DECLARATION = "methodDeclarationName"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of static method
	 * invocations.
	 */
	public static final String STATIC_METHOD_INVOCATION = "staticMethodInvocation"; //$NON-NLS-1$

	/**
	 * <<<<<<< HEAD A named preference part that controls the highlighting of
	 * inherited method invocations.
	 */
	public static final String INHERITED_METHOD_INVOCATION = "inheritedMethodInvocation"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of annotation
	 * element references.
	 *
	 * @since 3.1
	 */
	public static final String ANNOTATION_ELEMENT_REFERENCE = "annotationElementReference"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of abstract method
	 * invocations.
	 */
	public static final String ABSTRACT_METHOD_INVOCATION = "abstractMethodInvocation"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of local
	 * variables.
	 */
	public static final String LOCAL_VARIABLE_DECLARATION = "localVariableDeclaration"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of local
	 * variables.
	 */
	public static final String LOCAL_VARIABLE = "localVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of parameter
	 * variables.
	 */
	public static final String PARAMETER_VARIABLE = "parameterVariable"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of deprecated
	 * members.
	 */
	public static final String DEPRECATED_MEMBER = "deprecatedMember"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of functions
	 * (invocations and declarations).
	 */
	public static final String FUNCTION = "function"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of methods
	 * (invocations and declarations).
	 *
	 */
	public static final String METHOD = "method"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of classes.
	 *
	 */
	public static final String CLASS = "class"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of internal
	 * classes.
	 *
	 */
	public static final String INTERNAL_CLASS = "internalClass"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of internal
	 * constant.
	 *
	 */
	public static final String INTERNAL_CONSTANT = "internalConstant"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of internal
	 * function.
	 *
	 */
	public static final String INTERNAL_FUNCTION = "internalFunction"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of interfaces.
	 *
	 */
	public static final String INTERFACE = "interface"; //$NON-NLS-1$

	/**
	 * A named preference part that controls the highlighting of numbers.
	 *
	 */
	public static final String NUMBER = "number"; //$NON-NLS-1$

	/**
	 * Semantic highlightings
	 */
	private static SemanticHighlighting[] fgSemanticHighlightings;

	/**
	 * A named preference that controls the given semantic highlighting's color.
	 *
	 * @param semanticHighlighting
	 *            the semantic highlighting
	 * @return the color preference key
	 */
	public static String getColorPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has
	 * the text attribute bold.
	 *
	 * @param semanticHighlighting
	 *            the semantic highlighting
	 * @return the bold preference key
	 */
	public static String getBoldPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has
	 * the text attribute italic.
	 *
	 * @param semanticHighlighting
	 *            the semantic highlighting
	 * @return the italic preference key
	 */
	public static String getItalicPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has
	 * the text attribute strikethrough.
	 *
	 * @param semanticHighlighting
	 *            the semantic highlighting
	 * @return the strikethrough preference key
	 * @since 3.1
	 */
	public static String getStrikethroughPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
	}

	/**
	 * A named preference that controls if the given semantic highlighting has
	 * the text attribute underline.
	 *
	 * @param semanticHighlighting
	 *            the semantic highlighting
	 * @return the underline preference key
	 * @since 3.1
	 */
	public static String getUnderlinePreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
	}

	/**
	 * A named preference that controls if the given semantic highlighting is
	 * enabled.
	 *
	 * @param semanticHighlighting
	 *            the semantic highlighting
	 * @return the enabled preference key
	 */
	public static String getEnabledPreferenceKey(SemanticHighlighting semanticHighlighting) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + semanticHighlighting.getPreferenceKey()
				+ PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
	}

	/**
	 * @return The semantic highlightings, the order defines the precedence of
	 *         matches, the first match wins.
	 */
	public static SemanticHighlighting[] getSemanticHighlightings() {
		if (fgSemanticHighlightings == null)
			fgSemanticHighlightings = new SemanticHighlighting[] { new DeprecatedMemberHighlighting(),
					new InternalConstantHighlighting(), new SuperGlobalHighlighting(), new ConstantFieldHighlighting(),
					new StaticFieldHighlighting(), new FieldHighlighting(), new InternalFunctionHighlighting(),
					new FunctionHighlighting(), new MethodDeclarationHighlighting(),
					new StaticMethodInvocationHighlighting(),
					// new AbstractMethodInvocationHighlighting(),
					// new AnnotationElementReferenceHighlighting(),
					// new InheritedMethodInvocationHighlighting(),
					new ParameterVariableHighlighting(),
					// new LocalVariableDeclarationHighlighting(),
					new LocalVariableHighlighting(), new MethodHighlighting(), new InternalClassHighlighting(),
					new ClassHighlighting(),
					// new InterfaceHighlighting(),
					new NumberHighlighting() };
		return fgSemanticHighlightings;
	}

	/**
	 * Semantic highlighting for classes.
	 */
	private static final class ClassHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return CLASS;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 80, 50);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.ClassHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (token.getNode().getName().equalsIgnoreCase("self")) { //$NON-NLS-1$
				return false;
			}
			if (binding instanceof ITypeBinding) {
				ITypeBinding typeBinding = (ITypeBinding) binding;
				return typeBinding.isClass() || typeBinding.isTrait();
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for method declarations.
	 */
	private static final class MethodDeclarationHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return METHOD_DECLARATION;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return Messages.MethodDeclarationHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			StructuralPropertyDescriptor location = token.getNode().getLocationInParent();
			return location == FunctionDeclaration.NAME_PROPERTY;
		}
	}

	/**
	 * Semantic highlighting for internal classes.
	 *
	 */
	private static final class InternalClassHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return INTERNAL_CLASS;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return Messages.InternalClassHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (binding instanceof ITypeBinding) {
				ITypeBinding typeBinding = (ITypeBinding) binding;
				return (typeBinding.isClass() || typeBinding.isTrait()) && typeBinding.isInternal();
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for internal constants.
	 *
	 */
	private static final class InternalConstantHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return INTERNAL_CONSTANT;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return Messages.InternalConstantHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			return false;
		}

		@Override
		public boolean consumesLiteral(SemanticToken token) {
			if (token.getLiteral() instanceof Scalar) {
				Scalar scalar = (Scalar) token.getLiteral();
				IBinding binding = scalar.resolveBinding();
				if (binding instanceof IVariableBinding) {
					return ((IVariableBinding) binding).isInternal();
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for internal constants.
	 *
	 */
	private static final class InternalFunctionHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return INTERNAL_FUNCTION;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return Messages.InternalFunctionHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (binding != null && binding.getKind() == IBinding.METHOD && binding instanceof IMethodBinding) {
				return ((IMethodBinding) binding).isInternal();
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for local variables.
	 */
	private static final class LocalVariableHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return LOCAL_VARIABLE;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(106, 62, 62);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.LocalVariableHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE && !((IVariableBinding) binding).isField()) {
				IVariableBinding variableBinding = (IVariableBinding) binding;
				if (variableBinding.isLocal()) {
					token.setHighlightingNode(token.getNode().getParent());
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for parameter variables.
	 */
	private static final class ParameterVariableHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return PARAMETER_VARIABLE;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(106, 62, 62);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public boolean isUnderlineByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.ParameterVariableHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			boolean isParameter = false;
			if (binding != null && binding.getKind() == IBinding.VARIABLE && !((IVariableBinding) binding).isField()) {
				isParameter = ((IVariableBinding) binding).isParameter();
				if (isParameter) {
					token.setHighlightingNode(token.getNode().getParent());
				}
			}
			return isParameter;
		}
	}

	/**
	 * Semantic highlighting for inherited method invocations.
	 */
	private static final class MethodHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return METHOD;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return Messages.MethodHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			;
			IBinding binding = token.getBinding();
			return binding != null && binding.getKind() == IBinding.METHOD;
		}
	}

	/**
	 * Semantic highlighting for function invocation and declaration.
	 */
	private static final class FunctionHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return FUNCTION;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.FunctionHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (binding != null && binding.getKind() == IBinding.METHOD && binding instanceof IMethodBinding) {
				IMethodBinding methodBinding = (IMethodBinding) binding;
				if (methodBinding.getDeclaringClass() == null
						|| PHPFlags.isNamespace(methodBinding.getDeclaringClass().getModifiers())) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for static method invocations.
	 */
	private static final class StaticMethodInvocationHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return STATIC_METHOD_INVOCATION;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.StaticMethodInvocation_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			return binding != null && binding.getKind() == IBinding.METHOD && PHPFlags.isStatic(binding.getModifiers());
		}
	}

	/**
	 * Semantic highlighting for fields.
	 */
	private static final class FieldHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return FIELD;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.FieldHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			boolean isConsumed = binding != null && binding.getKind() == IBinding.VARIABLE
					&& ((IVariableBinding) binding).isField();
			if (isConsumed) {
				ASTNode node = token.getNode();
				if (node.getParent() instanceof Variable) {
					token.setHighlightingNode(node.getParent());
				}
			}
			return isConsumed;
		}
	}

	/**
	 * Semantic highlighting for static fields.
	 */
	private static final class StaticFieldHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return STATIC_FIELD;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.StaticFieldHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			boolean isConsumed = binding != null && binding.getKind() == IBinding.VARIABLE
					&& ((IVariableBinding) binding).isField() && PHPFlags.isStatic(binding.getModifiers());
			if (isConsumed) {
				ASTNode node = token.getNode();
				if (node.getParent() instanceof Variable) {
					token.setHighlightingNode(node.getParent());
				}
			}
			return isConsumed;
		}
	}

	/**
	 * Semantic highlighting for constant fields.
	 */
	private static final class ConstantFieldHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return CONSTANT;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 192);
		}

		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		@Override
		public boolean isItalicByDefault() {
			return true;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.ConstantHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			return binding != null && binding.getKind() == IBinding.VARIABLE && ((IVariableBinding) binding).isField()
					&& PHPFlags.isConstant(binding.getModifiers());
		}

		@Override
		public boolean consumesLiteral(SemanticToken token) {
			if (token.getLiteral() instanceof Scalar) {
				Scalar scalar = (Scalar) token.getLiteral();
				IBinding binding = scalar.resolveBinding();
				if (binding instanceof IVariableBinding) {
					return ((IVariableBinding) binding).isConstant();
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for fields.
	 */
	private static final class SuperGlobalHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return SUPER_GLOBAL;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(127, 0, 85);
		}

		@Override
		public boolean isBoldByDefault() {
			return true;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		@Override
		public String getDisplayName() {
			return Messages.SuperGlobalHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (binding != null && binding.getKind() == IBinding.VARIABLE
					&& ((IVariableBinding) binding).isSuperGlobal()) {
				ASTNode node = token.getNode();
				if (node.getParent() instanceof Variable) {
					token.setHighlightingNode(node.getParent());
				}
				return true;
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for numbers.
	 *
	 * @since 3.4
	 */
	private static final class NumberHighlighting extends SemanticHighlighting {

		@Override
		public String getPreferenceKey() {
			return NUMBER;
		}

		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(42, 0, 255);
		}

		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		@Override
		public boolean isEnabledByDefault() {
			return false;
		}

		@Override
		public String getDisplayName() {
			return Messages.NumbersHighlighting_0;
		}

		@Override
		public boolean consumes(SemanticToken token) {
			return false;
		}

		@Override
		public boolean consumesLiteral(SemanticToken token) {
			Expression expr = token.getLiteral();
			if (expr != null && expr.getType() == ASTNode.SCALAR) {
				if (((Scalar) expr).getScalarType() == Scalar.TYPE_INT
						|| ((Scalar) expr).getScalarType() == Scalar.TYPE_REAL) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Semantic highlighting for deprecated members.
	 */
	static final class DeprecatedMemberHighlighting extends SemanticHighlighting {

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#
		 * getPreferenceKey()
		 */
		@Override
		public String getPreferenceKey() {
			return DEPRECATED_MEMBER;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#
		 * getDefaultTextColor()
		 */
		@Override
		public RGB getDefaultDefaultTextColor() {
			return new RGB(0, 0, 0);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#
		 * getDefaultTextStyleBold()
		 */
		@Override
		public boolean isBoldByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#
		 * isItalicByDefault()
		 */
		@Override
		public boolean isItalicByDefault() {
			return false;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#
		 * isStrikethroughByDefault()
		 * 
		 * @since 3.1
		 */
		@Override
		public boolean isStrikethroughByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#
		 * isEnabledByDefault()
		 */
		@Override
		public boolean isEnabledByDefault() {
			return true;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.javaeditor.ISemanticHighlighting#
		 * getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.DeprecatedHighlighting_0;
		}

		/*
		 * @see
		 * org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting#consumes(
		 * org.eclipse.jdt.internal.ui.javaeditor.SemanticToken)
		 */
		@Override
		public boolean consumes(SemanticToken token) {
			IBinding binding = token.getBinding();
			if (binding != null) {
				if (binding.isDeprecated())
					return true;
				if (binding instanceof IMethodBinding) {
					IMethodBinding methodBinding = (IMethodBinding) binding;
					ITypeBinding declaringClass = methodBinding.getDeclaringClass();
					if (declaringClass == null)
						return false;
					if (declaringClass.isAnonymous()) {
						ITypeBinding[] interfaces = declaringClass.getInterfaces();
						if (interfaces.length > 0) {
							return interfaces[0].isDeprecated();
						} else if (declaringClass.getSuperclass() != null) {
							return declaringClass.getSuperclass().isDeprecated();
						}
						return false;
					}
					return declaringClass.isDeprecated() && !(token.getNode().getParent() instanceof MethodDeclaration);
				} else if (binding instanceof IVariableBinding) {
					IVariableBinding variableBinding = (IVariableBinding) binding;
					ITypeBinding declaringClass = variableBinding.getDeclaringClass();
					return declaringClass != null && declaringClass.isDeprecated();
					// && !(token.getNode().getParent() instanceof
					// VariableDeclaration);
				}
			}
			return false;
		}
	}

	/**
	 * Initialize default preferences in the given preference store.
	 *
	 * @param store
	 *            The preference store
	 */
	public static void initDefaults(IPreferenceStore store) {
		SemanticHighlighting[] semanticHighlightings = getSemanticHighlightings();
		for (int i = 0, n = semanticHighlightings.length; i < n; i++) {
			SemanticHighlighting semanticHighlighting = semanticHighlightings[i];
			setDefaultAndFireEvent(store, SemanticHighlightings.getColorPreferenceKey(semanticHighlighting),
					semanticHighlighting.getDefaultTextColor());
			store.setDefault(SemanticHighlightings.getBoldPreferenceKey(semanticHighlighting),
					semanticHighlighting.isBoldByDefault());
			store.setDefault(SemanticHighlightings.getItalicPreferenceKey(semanticHighlighting),
					semanticHighlighting.isItalicByDefault());
			store.setDefault(SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlighting),
					semanticHighlighting.isStrikethroughByDefault());
			store.setDefault(SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlighting),
					semanticHighlighting.isUnderlineByDefault());
			store.setDefault(SemanticHighlightings.getEnabledPreferenceKey(semanticHighlighting),
					semanticHighlighting.isEnabledByDefault());
		}
	}

	/**
	 * Tests whether <code>event</code> in <code>store</code> affects the
	 * enablement of semantic highlighting.
	 *
	 * @param store
	 *            the preference store where <code>event</code> was observed
	 * @param event
	 *            the property change under examination
	 * @return <code>true</code> if <code>event</code> changed semantic
	 *         highlighting enablement, <code>false</code> if it did not
	 * @since 3.1
	 */
	public static boolean affectsEnablement(IPreferenceStore store, PropertyChangeEvent event) {
		String relevantKey = null;
		SemanticHighlighting[] highlightings = getSemanticHighlightings();
		for (int i = 0; i < highlightings.length; i++) {
			if (event.getProperty().equals(getEnabledPreferenceKey(highlightings[i]))) {
				relevantKey = event.getProperty();
				break;
			}
		}
		if (relevantKey == null)
			return false;

		for (int i = 0; i < highlightings.length; i++) {
			String key = getEnabledPreferenceKey(highlightings[i]);
			if (key.equals(relevantKey))
				continue;
			if (store.getBoolean(key))
				return false; // another is still enabled or was enabled before
		}

		// all others are disabled, so toggling relevantKey affects the
		// enablement
		return true;
	}

	/**
	 * Tests whether semantic highlighting is currently enabled.
	 *
	 * @param store
	 *            the preference store to consult
	 * @return <code>true</code> if semantic highlighting is enabled,
	 *         <code>false</code> if it is not
	 * @since 3.1
	 */
	public static boolean isEnabled(IPreferenceStore store) {
		SemanticHighlighting[] highlightings = getSemanticHighlightings();
		boolean enable = false;
		for (int i = 0; i < highlightings.length; i++) {
			String enabledKey = getEnabledPreferenceKey(highlightings[i]);
			if (store.getBoolean(enabledKey)) {
				enable = true;
				break;
			}
		}

		return enable;
	}

	/**
	 * Sets the default value and fires a property change event if necessary.
	 *
	 * @param store
	 *            the preference store
	 * @param key
	 *            the preference key
	 * @param newValue
	 *            the new value
	 * @since 3.3
	 */
	private static void setDefaultAndFireEvent(IPreferenceStore store, String key, RGB newValue) {
		RGB oldValue = null;
		if (store.isDefault(key))
			oldValue = PreferenceConverter.getDefaultColor(store, key);

		PreferenceConverter.setDefault(store, key, newValue);

		if (oldValue != null && !oldValue.equals(newValue))
			store.firePropertyChangeEvent(key, oldValue, newValue);
	}

	/**
	 * Do not instantiate
	 */
	private SemanticHighlightings() {
	}
}
