/*******************************************************************************
 * Copyright (c) 2013, 2016 Zend Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Zend Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.composer.core;

import java.io.InputStream;

/**
 * @author Wojciech Galanciak, 2013
 * 
 */
class ErrorAsyncStreamReader extends AsyncStreamReader {

	public ErrorAsyncStreamReader(InputStream inputStream, StringBuffer buffer,
			ILogDevice logDevice) {
		super(inputStream, buffer, logDevice);
	}

	@Override
	protected void printToDisplayDevice(String line) {
		if (logDevice != null) {
			logDevice.logError(line);
		}
	}

}