package org.eclipse.php.internal.ui.text;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 *
 * @since 5.0
 */
final public class PHPTextMessages extends NLS {

	private static final String BUNDLE_NAME = PHPTextMessages.class.getName();

	private PHPTextMessages() {
		// Do not instantiate
	}

	public static String ResultCollector_anonymous_type;
	public static String ResultCollector_overridingmethod;
	public static String ResultCollector_overloadingmagicmethod;
	public static String GetterSetterCompletionProposal_getter_label;
	public static String GetterSetterCompletionProposal_setter_label;
	public static String MethodCompletionProposal_method_label;
	public static String MethodCompletionProposal_constructor_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, PHPTextMessages.class);
	}
}