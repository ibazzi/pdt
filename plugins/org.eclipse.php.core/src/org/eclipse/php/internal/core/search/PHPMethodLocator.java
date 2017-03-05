/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.php.internal.core.search;

import org.eclipse.dltk.ast.references.TypeReference;
import org.eclipse.dltk.internal.core.search.matching.MatchingNodeSet;
import org.eclipse.dltk.internal.core.search.matching.MethodLocator;
import org.eclipse.dltk.internal.core.search.matching.MethodPattern;
import org.eclipse.php.internal.core.compiler.ast.nodes.ClassInstanceCreation;

public class PHPMethodLocator extends MethodLocator {

	public PHPMethodLocator(MethodPattern pattern) {
		super(pattern);
	}

	public int match(ClassInstanceCreation node, MatchingNodeSet nodeSet) {
		if (!this.pattern.findReferences)
			return IMPOSSIBLE_MATCH;
		if (this.pattern.selector == null)
			return nodeSet.addMatch(node, POSSIBLE_MATCH);

		if (this.pattern.declaringSimpleName != null) {
			if (node.getClassName() instanceof TypeReference) {
				String nodeName = ((TypeReference) node.getClassName()).getName();
				if (nodeName.equals(new String(this.pattern.declaringSimpleName))) {
					return nodeSet.addMatch(node, POSSIBLE_MATCH);
				}
			}
		}

		return IMPOSSIBLE_MATCH;
	}
}
