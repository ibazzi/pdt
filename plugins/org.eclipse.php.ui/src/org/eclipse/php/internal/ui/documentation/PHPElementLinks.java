/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.ui.documentation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.ui.ScriptElementLabels;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.internal.ui.corext.util.Strings;
import org.eclipse.php.internal.ui.viewsupport.PHPElementLabelComposer;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Display;

/**
 * Links inside PHPDoc hovers.
 * 
 * @since 3.4
 */
public class PHPElementLinks {

	/**
	 * A handler is asked to handle links to targets.
	 * 
	 * @see PHPElementLinks#createLocationListener(PHPElementLinks.ILinkHandler)
	 */
	public interface ILinkHandler {

		/**
		 * Handle normal kind of link to given target.
		 * 
		 * @param target
		 *            the target to show
		 */
		void handleInlineLink(IModelElement target);

		/**
		 * Handle link to given target to open its declaration
		 * 
		 * @param target
		 *            the target to show
		 */
		void handleDeclarationLink(IModelElement target);

		/**
		 * Handle link to given URL to open in browser.
		 * 
		 * @param url
		 *            the url to show
		 * @param display
		 *            the current display
		 * @return <code>true</code> if the handler could open the link
		 *         <code>false</code> if the browser should follow the link
		 */
		boolean handleExternalLink(URL url, Display display);

		/**
		 * Informs the handler that the text of the browser was set.
		 */
		void handleTextSet();
	}

	private static final class PHPElementLinkedLabelComposer extends PHPElementLabelComposer {

		private static List<String> primitiveTypes = Arrays.asList(new String[] { "bool", "int", "boolean", "integer",
				"float", "double", "string", "array", "object", "callback", "mixed", "void" });
		private final IModelElement fElement;

		public PHPElementLinkedLabelComposer(IModelElement member, StringBuffer buf) {
			super(buf);
			if (member instanceof IPackageDeclaration) {
				fElement = member.getAncestor(IModelElement.PACKAGE_DECLARATION);
			} else {
				fElement = member;
			}
		}

		@Override
		public String getElementName(IModelElement element) {
			String elementName = element.getElementName();
			return getElementName(element, elementName);
		}

		@Override
		protected String getElementName(String typeName, ISourceModule sourceModule, int offset) {
			if (primitiveTypes.contains(typeName)) {
				return typeName;
			}
			try {
				IType[] types = PHPModelUtils.getTypes(typeName, sourceModule, offset, null);
				if (types.length > 0) {
					return getElementName(types[0]);
				}
			} catch (ModelException e) {
			}
			return super.getElementName(typeName, sourceModule, offset);
		}

		private String getElementName(IModelElement element, String elementName) {
			if (element.equals(fElement)) { // linking to the member itself
											// would be a no-op
				return elementName;
			}
			if (elementName.length() == 0) { // anonymous
				return elementName;
			}
			try {
				String uri = createURI(PHPDOC_SCHEME, element);
				return createHeaderLink(uri, elementName);
			} catch (URISyntaxException e) {
				PHPUiPlugin.log(e);
				return elementName;
			}
		}

	}

	public static final String OPEN_LINK_SCHEME = "eclipse-open"; //$NON-NLS-1$
	public static final String PHPDOC_SCHEME = "eclipse-phpdoc"; //$NON-NLS-1$
	private static final char LINK_BRACKET_REPLACEMENT = '\u2603';

	/**
	 * The link is composed of a number of segments, separated by
	 * LINK_SEPARATOR:
	 * <p>
	 * segments[0]: ""<br>
	 * segments[1]: baseElementHandle<br>
	 * segments[2]: typeName<br>
	 * segments[3]: memberName<br>
	 * segments[4...]: parameterTypeName (optional)
	 */
	private static final char LINK_SEPARATOR = '\u2602';

	private PHPElementLinks() {
		// static only
	}

	/**
	 * Creates a location listener which uses the given handler to handle java
	 * element links.
	 * 
	 * The location listener can be attached to a {@link Browser}
	 * 
	 * @param handler
	 *            the handler to use to handle links
	 * @return a new {@link LocationListener}
	 */
	public static LocationListener createLocationListener(final ILinkHandler handler) {
		return new LocationAdapter() {
			public void changing(LocationEvent event) {
				String loc = event.location;

				if ("about:blank".equals(loc)) { //$NON-NLS-1$
					/*
					 * Using the Browser.setText API triggers a location change
					 * to "about:blank". XXX: remove this code once
					 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=130314 is
					 * fixed
					 */
					// input set with setText
					handler.handleTextSet();
					return;
				}

				event.doit = false;

				if (loc.startsWith("about:")) { //$NON-NLS-1$
					// Relative links should be handled via head > base tag.
					// If no base is available, links just won't work.
					return;
				}

				URI uri;
				try {
					uri = new URI(loc);
				} catch (URISyntaxException e) {
					// try it with a file (workaround for
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=237903 ):
					File file = new File(loc);
					if (!file.exists()) {
						PHPUiPlugin.log(e);
						return;
					}
					uri = file.toURI();
					loc = uri.toASCIIString();
				}

				String scheme = uri.getScheme();
				if (PHPElementLinks.PHPDOC_SCHEME.equals(scheme)) {
					IModelElement linkTarget = PHPElementLinks.parseURI(uri);
					if (linkTarget == null)
						return;

					handler.handleInlineLink(linkTarget);
				} else if (PHPElementLinks.OPEN_LINK_SCHEME.equals(scheme)) {
					IModelElement linkTarget = PHPElementLinks.parseURI(uri);
					if (linkTarget == null)
						return;

					handler.handleDeclarationLink(linkTarget);
				} else {
					try {
						if (handler.handleExternalLink(new URL(loc), event.display))
							return;

						event.doit = true;
					} catch (MalformedURLException e) {
						PHPUiPlugin.log(e);
					}
				}
			}
		};
	}

	protected static IModelElement parseURI(URI uri) {
		String ssp = uri.getSchemeSpecificPart();
		String[] segments = ssp.split(String.valueOf(LINK_SEPARATOR), -1);

		// replace '[' manually, since URI confuses it for an IPv6 address as
		// per RFC 2732:
		IModelElement element = DLTKCore.create(segments[1].replace(LINK_BRACKET_REPLACEMENT, '['));

		if ((segments.length > 2) && (element instanceof IMember)) {
			IMember member = (IMember) element;
			String refTypeName = segments[2];

			IType[] types = null;

			try {

				int offset = member.getSourceRange() != null ? member.getSourceRange().getOffset() : 0;
				types = PHPModelUtils.getTypes(refTypeName, member.getSourceModule(), offset,
						new NullProgressMonitor());
			} catch (ModelException e) {
				PHPUiPlugin.log(e);
			}

			if ((types != null) && (types.length > 0)) {
				IType type = types[0]; // take first one
				if (segments.length > 3) {
					String refMemberName = segments[3];
					IMethod method = type.getMethod(refMemberName);
					if (method != null && method.exists()) {
						return method;
					} else {
						// if (refMemberName.startsWith("$")) {
						// refMemberName = refMemberName.substring(0);
						// }
						return type.getField(refMemberName);
					}
				}

				return type;
				// String refParamterTypes = segments[4..];
			}

		}

		return element;
	}

	/**
	 * Creates an {@link URI} with the given scheme for the given element.
	 * 
	 * @param scheme
	 *            the scheme
	 * @param element
	 *            the element
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII}
	 *         string, ready to be used as <code>href</code> attribute in an
	 *         <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException
	 *             if the arguments were invalid
	 */
	public static String createURI(String scheme, IModelElement element) throws URISyntaxException {
		return createURI(scheme, element, null, null, null);
	}

	/**
	 * Creates an {@link URI} with the given scheme based on the given element.
	 * The additional arguments specify a member referenced from the given
	 * element.
	 * 
	 * @param scheme
	 *            a scheme
	 * @param element
	 *            the declaring element
	 * @param refTypeName
	 *            a (possibly qualified) type name, can be <code>null</code>
	 * @param refMemberName
	 *            a member name, can be <code>null</code>
	 * @param refParameterTypes
	 *            a (possibly empty) array of (possibly qualified) parameter
	 *            type names, can be <code>null</code>
	 * @return an {@link URI}, encoded as {@link URI#toASCIIString() ASCII}
	 *         string, ready to be used as <code>href</code> attribute in an
	 *         <code>&lt;a&gt;</code> tag
	 * @throws URISyntaxException
	 *             if the arguments were invalid
	 */
	public static String createURI(String scheme, IModelElement element, String refTypeName, String refMemberName,
			String[] refParameterTypes) throws URISyntaxException {
		/*
		 * We use an opaque URI, not ssp and fragments (to work around Safari
		 * bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=212527 (wrongly
		 * encodes #)).
		 */

		StringBuffer ssp = new StringBuffer(60);
		ssp.append(LINK_SEPARATOR); // make sure first character is not a /
									// (would be hierarchical URI)

		// replace '[' manually, since URI confuses it for an IPv6 address as
		// per RFC 2732:
		ssp.append(element.getHandleIdentifier().replace('[', LINK_BRACKET_REPLACEMENT)); // segments[1]

		if (refTypeName != null) {
			ssp.append(LINK_SEPARATOR);
			ssp.append(refTypeName); // segments[2]

			if (refMemberName != null) {
				ssp.append(LINK_SEPARATOR);
				ssp.append(refMemberName); // segments[3]

				if (refParameterTypes != null) {
					ssp.append(LINK_SEPARATOR);
					for (int i = 0; i < refParameterTypes.length; i++) {
						ssp.append(refParameterTypes[i]); // segments[4|5|..]
						if (i != refParameterTypes.length - 1) {
							ssp.append(LINK_SEPARATOR);
						}
					}
				}
			}
		}
		return new URI(scheme, ssp.toString(), null).toASCIIString();
	}

	/**
	 * Creates a link with the given URI and label text.
	 * 
	 * @param uri
	 *            the URI
	 * @param label
	 *            the label
	 * @return the HTML link
	 * @since 3.6
	 */
	public static String createLink(String uri, String label) {
		return "<a href='" + uri + "'>" + label + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Creates a header link with the given URI and label text.
	 * 
	 * @param uri
	 *            the URI
	 * @param label
	 *            the label
	 * @return the HTML link
	 * @since 3.6
	 */
	public static String createHeaderLink(String uri, String label) {
		return "<a class='header' href='" + uri + "'>" + label + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Returns the label for a Java element with the flags as defined by
	 * {@link JavaElementLabels}. Referenced element names in the label (except
	 * the given element's name) are rendered as header links.
	 * 
	 * @param element
	 *            the element to render
	 * @param flags
	 *            the rendering flags
	 * @return the label of the Java element
	 * @since 3.5
	 */
	public static String getElementLabel(IModelElement element, long flags) {
		return getElementLabel(element, flags, false);
	}

	/**
	 * Returns the label for a Java element with the flags as defined by
	 * {@link JavaElementLabels}. Referenced element names in the label are
	 * rendered as header links. If <code>linkAllNames</code> is
	 * <code>false</code>, don't link the name of the given element
	 * 
	 * @param element
	 *            the element to render
	 * @param flags
	 *            the rendering flags
	 * @param linkAllNames
	 *            if <code>true</code>, link all names; if <code>false</code>,
	 *            link all names except original element's name
	 * @return the label of the Java element
	 * @since 3.6
	 */
	public static String getElementLabel(IModelElement element, long flags, boolean linkAllNames) {
		StringBuffer buf = new StringBuffer();

		if (!Strings.USE_TEXT_PROCESSOR) {
			new PHPElementLinkedLabelComposer(linkAllNames ? null : element, buf).getElementLabel(element, flags);
			return Strings.markLTR(buf.toString());
		} else {
			String label = ScriptElementLabels.getDefault().getElementLabel(element, flags);
			return label.replaceAll("<", "&lt;").replaceAll(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

}
