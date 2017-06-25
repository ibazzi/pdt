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
package org.eclipse.php.core.ast.nodes;

import org.eclipse.php.core.ast.visitor.AbstractVisitor;

;

/**
 * Utilities used for Ast nodes
 * 
 * @author Eden
 *
 */
public class ASTNodes {

	public static ASTNode getParent(ASTNode node, Class parentClass) {
		if (node == null)
			return null;

		do {
			node = node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return node;
	}

	public static ASTNode getParent(ASTNode node, int nodeType) {
		if (node == null)
			return null;

		do {
			node = node.getParent();
		} while (node != null && node.getType() != nodeType);
		return node;
	}

	/**
	 * @param node
	 * @return whether the given node is the only statement of a control
	 *         statement
	 */
	public static boolean isControlStatement(ASTNode node) {
		assert node != null;
		int type = node.getType();

		return (type == ASTNode.IF_STATEMENT || type == ASTNode.FOR_STATEMENT || type == ASTNode.FOR_EACH_STATEMENT
				|| type == ASTNode.WHILE_STATEMENT || type == ASTNode.DO_STATEMENT);
	}

	/**
	 * Aggregates the strings for a given node
	 * 
	 * @param node
	 * @return the aggregated strings for a given node
	 */
	public static String getScalars(ASTNode node) {
		final StringBuilder builder = new StringBuilder();
		node.accept(new AbstractVisitor() {

			@Override
			public boolean visit(Scalar scalar) {
				builder.append(scalar.getStringValue());
				return true;
			}

		});

		return builder.toString();
	}

	/**
	 * Tells if a variable is in the form of <code>${var}</code> or
	 * <code>${var[0]}</code> inside a back-quoted string, a double-quoted
	 * string or a heredoc section
	 * 
	 * @param variable
	 * @return true if the variable is in the form of <code>${var}</code> or
	 *         <code>${var[0]}</code> inside a back-quoted string, a
	 *         double-quoted string or a heredoc section, false otherwise
	 */
	public static boolean isQuotedDollaredCurlied(Variable variable) {
		if (variable.isDollared() || variable.getParent() == null) {
			return false;
		}

		ASTNode enclosing = null;

		if (variable.getParent().getType() == ASTNode.ARRAY_ACCESS) {
			enclosing = variable.getParent().getParent();
			if (enclosing != null && enclosing.getType() == ASTNode.REFLECTION_VARIABLE) {
				enclosing = enclosing.getParent();
			} else {
				enclosing = null;
			}
		} else if (variable.getParent().getType() == ASTNode.REFLECTION_VARIABLE) {
			enclosing = variable.getParent().getParent();
		}

		if (enclosing == null) {
			return false;
		}

		return enclosing.getType() == ASTNode.QUOTE || enclosing.getType() == ASTNode.BACK_TICK_EXPRESSION;
	}

	/**
	 * For {@link Name} or {@link Type} nodes, returns the topmost {@link Type}
	 * node that shares the same type binding as the given node.
	 *
	 * @param node
	 *            an ASTNode
	 * @return the normalized {@link Type} node or the original node
	 */
	public static ASTNode getNormalizedNode(ASTNode node) {
		ASTNode current = node;
		// normalize name
		if (NamespaceName.NAME_PROPERTY.equals(current.getLocationInParent())) {
			current = current.getParent();
		}
		// // normalize type
		// if (QualifiedType.NAME_PROPERTY.equals(current.getLocationInParent())
		// || SimpleType.NAME_PROPERTY.equals(current.getLocationInParent())
		// ||
		// NameQualifiedType.NAME_PROPERTY.equals(current.getLocationInParent()))
		// {
		// current = current.getParent();
		// }
		// // normalize parameterized types
		// if
		// (ParameterizedType.TYPE_PROPERTY.equals(current.getLocationInParent()))
		// {
		// current = current.getParent();
		// }
		return current;
	}
}
