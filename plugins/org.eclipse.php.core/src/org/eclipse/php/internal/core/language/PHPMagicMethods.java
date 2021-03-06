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
package org.eclipse.php.internal.core.language;

import java.util.*;

import org.eclipse.dltk.core.Flags;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IParameter;
import org.eclipse.dltk.internal.core.MethodParameterInfo;
import org.eclipse.dltk.internal.core.ModelElement;
import org.eclipse.php.core.PHPVersion;
import org.eclipse.php.internal.core.typeinference.FakeMethod;

public class PHPMagicMethods {

	private static final Map<String, String[]> MAGIC_METHODS_PARAMETERS;
	private static final String[] MAGIC_METHODS = { "__get", "__set", "__call", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"__sleep", "__wakeup", }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String[] MAGIC_METHODS_PHP5 = { "__isset", "__unset", //$NON-NLS-1$ //$NON-NLS-2$
			"__toString", "__set_state", "__clone", "__autoload", }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final String[] MAGIC_METHODS_PHP5_3 = { "__callstatic", //$NON-NLS-1$
			"__invoke", }; //$NON-NLS-1$
	private static final String[] MAGIC_METHODS_PHP5_6 = { "__debugInfo" }; //$NON-NLS-1$

	static {
		MAGIC_METHODS_PARAMETERS = new HashMap<>();
		MAGIC_METHODS_PARAMETERS.put("__get", new String[] { "$name" });
		MAGIC_METHODS_PARAMETERS.put("__set", new String[] { "$name", "$value" });
		MAGIC_METHODS_PARAMETERS.put("__call", new String[] { "$name", "$arguments" });
		MAGIC_METHODS_PARAMETERS.put("__sleep", new String[] {});
		MAGIC_METHODS_PARAMETERS.put("__wakeup", new String[] {});
		MAGIC_METHODS_PARAMETERS.put("__isset", new String[] { "$name" });
		MAGIC_METHODS_PARAMETERS.put("__unset", new String[] { "$name" });
		MAGIC_METHODS_PARAMETERS.put("__toString", new String[] {});
		MAGIC_METHODS_PARAMETERS.put("__set_state", new String[] { "$properties" });
		MAGIC_METHODS_PARAMETERS.put("__clone", new String[] {});
		MAGIC_METHODS_PARAMETERS.put("__autoload", new String[] { "$class" });
		MAGIC_METHODS_PARAMETERS.put("__callstatic", new String[] { "$name", "$arguments" });
		MAGIC_METHODS_PARAMETERS.put("__invoke", new String[] {});
		MAGIC_METHODS_PARAMETERS.put("__debugInfo", new String[] {});
	}

	public static String[] getMethods(PHPVersion phpVersion) {
		List<String> methods = new LinkedList<String>();
		methods.addAll(Arrays.asList(MAGIC_METHODS));
		methods.addAll(Arrays.asList(MAGIC_METHODS_PHP5));
		if (phpVersion.isGreaterThan(PHPVersion.PHP5)) {
			methods.addAll(Arrays.asList(MAGIC_METHODS_PHP5_3));
		}
		if (phpVersion.isGreaterThan(PHPVersion.PHP5_5)) {
			methods.addAll(Arrays.asList(MAGIC_METHODS_PHP5_6));
		}
		return (String[]) methods.toArray(new String[methods.size()]);
	}

	public static IMethod createMethod(ModelElement parent, String name) {
		int flags = Flags.AccPublic;
		if (name.equals("__callstatic")) {
			flags |= Flags.AccStatic;
		}
		FakeMethod method = new FakeMethod(parent, name, flags);
		String[] strParameters = MAGIC_METHODS_PARAMETERS.get(name);
		if (strParameters != null && strParameters.length > 0) {
			IParameter[] parameters = new IParameter[strParameters.length];
			for (int i = 0; i < strParameters.length; i++) {
				parameters[i] = new MethodParameterInfo(strParameters[i]);
			}
			method.setParameters(parameters);
		}

		return method;
	}

	public static boolean isMagicMethod(String name) {
		return MAGIC_METHODS_PARAMETERS.containsKey(name);
	}
}
