/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.debug.core.phpIni;

import java.io.File;

import org.eclipse.core.resources.IProject;

/**
 * This interface is used for modifying INI file.
 * 
 * @author ibazzi
 */
public interface IPHPINIModifier {

	void modify(File phpIniPath, IProject project);

}
