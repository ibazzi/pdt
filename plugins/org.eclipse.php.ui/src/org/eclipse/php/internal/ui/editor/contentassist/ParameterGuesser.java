package org.eclipse.php.internal.ui.editor.contentassist;

import java.util.*;

import org.eclipse.dltk.core.*;
import org.eclipse.dltk.ui.ScriptElementImageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.typeinference.PHPSimpleTypes;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.internal.ui.text.template.contentassist.PositionBasedCompletionProposal;
import org.eclipse.php.internal.ui.util.PHPElementImageDescriptor;
import org.eclipse.php.internal.ui.util.PHPPluginImages;
import org.eclipse.php.internal.ui.util.StringMatcher;
import org.eclipse.swt.graphics.Image;

/**
 * This class triggers a code-completion that will track all local and member
 * variables for later use as a parameter guessing proposal.
 */
public class ParameterGuesser {

	private final static class Variable {

		/**
		 * Variable type. Used to choose the best guess based on scope (Local
		 * beats instance beats inherited).
		 */
		public static final int LOCAL = 0;
		public static final int FIELD = 1;
		public static final int INHERITED_FIELD = 2;
		public static final int METHOD = 3;
		public static final int INHERITED_METHOD = 4;
		public static final int LITERALS = 5;

		public final String qualifiedTypeName;
		public final String name;
		public final String displayName;
		public final int variableType;
		public final int positionScore;

		public final boolean isAutoboxingMatch;

		public final char[] triggerChars;
		public final ImageDescriptor descriptor;

		public boolean alreadyMatched;

		public Variable(String qualifiedTypeName, String name, int variableType, boolean isAutoboxMatch,
				int positionScore, char[] triggerChars, ImageDescriptor descriptor) {
			this(qualifiedTypeName, name, name, variableType, isAutoboxMatch, positionScore, triggerChars, descriptor);
		}

		public Variable(String qualifiedTypeName, String name, String displayName, int variableType,
				boolean isAutoboxMatch, int positionScore, char[] triggerChars, ImageDescriptor descriptor) {
			this.qualifiedTypeName = qualifiedTypeName;
			this.name = name;
			this.displayName = displayName;
			this.variableType = variableType;
			this.positionScore = positionScore;
			this.triggerChars = triggerChars;
			this.descriptor = descriptor;
			this.isAutoboxingMatch = isAutoboxMatch;
			this.alreadyMatched = false;
		}

		/*
		 * @see Object#toString()
		 */
		@Override
		public String toString() {

			StringBuffer buffer = new StringBuffer();
			buffer.append(qualifiedTypeName);
			buffer.append(' ');
			buffer.append(name);
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(variableType);
			buffer.append(')');

			return buffer.toString();
		}
	}

	private static final char[] NO_TRIGGERS = new char[0];

	private final Set<String> fAlreadyMatchedNames;
	private final IModelElement fEnclosingElement;

	/**
	 * Creates a parameter guesser
	 * 
	 * @param enclosingElement
	 *            the enclosing Java element
	 */
	public ParameterGuesser(IModelElement enclosingElement) {
		fEnclosingElement = enclosingElement;
		fAlreadyMatchedNames = new HashSet<>();
	}

	private List<Variable> evaluateVisibleMatches(String expectedType, IModelElement[] suggestions)
			throws ModelException {
		IType currentType = null;
		if (fEnclosingElement != null) {
			currentType = (IType) fEnclosingElement.getAncestor(IModelElement.TYPE);
		}

		ArrayList<Variable> res = new ArrayList<>();
		for (int i = 0; i < suggestions.length; i++) {
			Variable variable = createVariable(suggestions[i], currentType, expectedType, i);
			if (variable != null) {
				if (fAlreadyMatchedNames.contains(variable.name)) {
					variable.alreadyMatched = true;
				}
				res.add(variable);
			}
		}

		// add 'this'
		if (currentType != null && !(fEnclosingElement instanceof IMethod
				&& Flags.isStatic(((IMethod) fEnclosingElement).getFlags()))) {
			String fullyQualifiedName = currentType.getFullyQualifiedName(NamespaceReference.NAMESPACE_DELIMITER);
			if (fullyQualifiedName.equals(expectedType)) {
				ImageDescriptor desc = new PHPElementImageDescriptor(PHPPluginImages.DESC_FIELD_PUBLIC,
						PHPElementImageDescriptor.FINAL | PHPElementImageDescriptor.STATIC,
						PHPElementImageDescriptor.SMALL_SIZE);
				res.add(new Variable(fullyQualifiedName, "$this", Variable.LITERALS, false, res.size(), //$NON-NLS-1$
						new char[] { '.' }, desc));
			}
		}

		if (!PHPSimpleTypes.isSimpleType(expectedType)) {
			// add 'null'
			res.add(new Variable(expectedType, "null", Variable.LITERALS, false, res.size(), NO_TRIGGERS, null)); //$NON-NLS-1$
		} else {
			if (expectedType.equals("bool")) {
				// add 'true', 'false'
				res.add(new Variable(expectedType, "true", Variable.LITERALS, false, res.size(), NO_TRIGGERS, null)); //$NON-NLS-1$
				res.add(new Variable(expectedType, "false", Variable.LITERALS, false, res.size(), NO_TRIGGERS, //$NON-NLS-1$
						null));
			} else {
				// add 0
				res.add(new Variable(expectedType, "0", Variable.LITERALS, false, res.size(), NO_TRIGGERS, null)); //$NON-NLS-1$
			}
		}
		return res;
	}

	public Variable createVariable(IModelElement element, IType enclosingType, String expectedType, int positionScore)
			throws ModelException {
		int variableType;
		int elementType = element.getElementType();
		String elementName = element.getElementName();
		String displayName = elementName;

		String type = null;
		String prefix = "";
		switch (elementType) {
		case IModelElement.FIELD: {
			IField field = (IField) element;
			boolean isStatic = Flags.isStatic(field.getFlags());
			if (field.getDeclaringType() == null) {
				variableType = Variable.LOCAL;
				type = field.getType();
			} else if (field.getDeclaringType().equals(enclosingType)) {
				variableType = Variable.FIELD;
				if (isStatic) {
					prefix = "self::";
				} else {
					prefix = "$this->";
				}
			} else {
				variableType = Variable.INHERITED_FIELD;
				prefix = "parent::";
			}
			type = field.getType();
			break;
		}
		case IModelElement.METHOD: {
			IMethod method = (IMethod) element;
			if (isMethodToSuggest(method)) {
				boolean isStatic = Flags.isStatic(method.getFlags());
				if (!PHPFlags.isNamespace(method.getDeclaringType().getFlags()) && method.getDeclaringType() != null
						&& method.getDeclaringType().equals(enclosingType)) {
					variableType = Variable.METHOD;
					if (isStatic) {
						prefix = "self::";
					} else {
						prefix = "$this->";
					}
				} else if (!PHPFlags.isNamespace(method.getDeclaringType().getFlags())) {
					variableType = Variable.INHERITED_METHOD;
					prefix = "parent::";
				} else {
					variableType = Variable.INHERITED_METHOD;
				}
				type = method.getType();
				displayName = elementName + "()"; //$NON-NLS-1$
			} else {
				return null;
			}
			break;
		}
		default:
			return null;
		}
		elementName = prefix + displayName;
		return new Variable(type, elementName, displayName, variableType, false, positionScore, NO_TRIGGERS,
				getImageDescriptor(element));
	}

	private ImageDescriptor getImageDescriptor(IModelElement elem) {
		ScriptElementImageProvider imageProvider = new ScriptElementImageProvider();
		ImageDescriptor desc = imageProvider.getBaseImageDescriptor(elem, ScriptElementImageProvider.OVERLAY_ICONS);
		imageProvider.dispose();
		return desc;
	}

	private boolean isMethodToSuggest(IMethod method) {
		try {
			String methodName = method.getElementName();
			return method.getParameters().length == 0 && !"void".equals(method.getType())
					&& (methodName.startsWith("get") || methodName.startsWith("is")); //$NON-NLS-1$//$NON-NLS-2$
		} catch (ModelException e) {
			return false;
		}
	}

	/**
	 * Returns the matches for the type and name argument, ordered by match
	 * quality.
	 * 
	 * @param expectedType
	 *            - the qualified type of the parameter we are trying to match
	 * @param paramName
	 *            - the name of the parameter (used to find similarly named
	 *            matches)
	 * @param pos
	 *            the position
	 * @param suggestions
	 *            the suggestions or <code>null</code>
	 * @param fillBestGuess
	 *            <code>true</code> if the best guess should be filled in
	 * @param isLastParameter
	 *            <code>true</code> iff this proposal is for the last parameter
	 *            of a method
	 * @return returns the name of the best match, or <code>null</code> if no
	 *         match found
	 * @throws ModelException
	 *             if it fails
	 */
	public ICompletionProposal[] parameterProposals(String expectedType, String paramName, Position pos,
			IModelElement[] suggestions, boolean fillBestGuess, boolean isLastParameter) throws ModelException {
		List<Variable> typeMatches = evaluateVisibleMatches(expectedType, suggestions);
		orderMatches(typeMatches, paramName);

		boolean hasVarWithParamName = false;
		ICompletionProposal[] ret = new ICompletionProposal[typeMatches.size()];
		int i = 0;
		int replacementLength = 0;
		for (Iterator<Variable> it = typeMatches.iterator(); it.hasNext();) {
			Variable v = it.next();
			if (i == 0) {
				fAlreadyMatchedNames.add(v.name);
				replacementLength = v.name.length();
			}

			String displayString = v.displayName;
			hasVarWithParamName |= displayString.equals(paramName);

			final char[] triggers;
			if (isLastParameter) {
				triggers = v.triggerChars;
			} else {
				triggers = new char[v.triggerChars.length + 1];
				System.arraycopy(v.triggerChars, 0, triggers, 0, v.triggerChars.length);
				triggers[triggers.length - 1] = ',';
			}

			ret[i++] = new PositionBasedCompletionProposal(v.name, pos, replacementLength, getImage(v.descriptor),
					displayString, null, null, triggers);
		}
		if (!fillBestGuess && !hasVarWithParamName) {
			// insert a proposal with the argument name
			ICompletionProposal[] extended = new ICompletionProposal[ret.length + 1];
			System.arraycopy(ret, 0, extended, 1, ret.length);
			extended[0] = new PositionBasedCompletionProposal(paramName, pos, replacementLength, null, paramName, null,
					null, isLastParameter ? null : new char[] { ',' });
			return extended;
		}
		return ret;
	}

	private static class MatchComparator implements Comparator<Variable> {

		private String fParamName;

		MatchComparator(String paramName) {
			fParamName = paramName;
		}

		@Override
		public int compare(Variable one, Variable two) {
			return score(two) - score(one);
		}

		/**
		 * The four order criteria as described below - put already used into
		 * bit 10, all others into bits 0-9, 11-20, 21-30; 31 is sign - always 0
		 * 
		 * @param v
		 *            the variable
		 * @return the score for <code>v</code>
		 */
		private int score(Variable v) {
			int variableScore = 100 - v.variableType; // since these are
														// increasing with
														// distance
			int subStringScore = getLongestCommonSubstring(v.name, fParamName).length();
			// substring scores under 60% are not considered
			// this prevents marginal matches like a - ba and false - isBool
			// that will
			// destroy the sort order
			int shorter = Math.min(v.name.length(), fParamName.length());
			if (subStringScore < 0.6 * shorter)
				subStringScore = 0;

			int positionScore = v.positionScore; // since ???
			int matchedScore = v.alreadyMatched ? 0 : 1;
			int autoboxingScore = v.isAutoboxingMatch ? 0 : 1;

			int score = autoboxingScore << 30 | variableScore << 21 | subStringScore << 11 | matchedScore << 10
					| positionScore;
			return score;
		}

	}

	/**
	 * Determine the best match of all possible type matches. The input into
	 * this method is all possible completions that match the type of the
	 * argument. The purpose of this method is to choose among them based on the
	 * following simple rules:
	 *
	 * 1) Local Variables > Instance/Class Variables > Inherited Instance/Class
	 * Variables
	 *
	 * 2) A longer case insensitive substring match will prevail
	 *
	 * 3) Variables that have not been used already during this completion will
	 * prevail over those that have already been used (this avoids the same
	 * String/int/char from being passed in for multiple arguments)
	 *
	 * 4) A better source position score will prevail (the declaration point of
	 * the variable, or "how close to the point of completion?"
	 *
	 * @param typeMatches
	 *            the list of type matches
	 * @param paramName
	 *            the parameter name
	 */
	private static void orderMatches(List<Variable> typeMatches, String paramName) {
		if (typeMatches != null)
			Collections.sort(typeMatches, new MatchComparator(paramName));
	}

	/**
	 * Returns the longest common substring of two strings.
	 *
	 * @param first
	 *            the first string
	 * @param second
	 *            the second string
	 * @return the longest common substring
	 */
	private static String getLongestCommonSubstring(String first, String second) {

		String shorter = (first.length() <= second.length()) ? first : second;
		String longer = shorter == first ? second : first;

		int minLength = shorter.length();

		StringBuffer pattern = new StringBuffer(shorter.length() + 2);
		String longestCommonSubstring = ""; //$NON-NLS-1$

		for (int i = 0; i < minLength; i++) {
			for (int j = i + 1; j <= minLength; j++) {
				if (j - i < longestCommonSubstring.length())
					continue;

				String substring = shorter.substring(i, j);
				pattern.setLength(0);
				pattern.append('*');
				pattern.append(substring);
				pattern.append('*');

				StringMatcher matcher = new StringMatcher(pattern.toString(), true, false);
				if (matcher.match(longer))
					longestCommonSubstring = substring;
			}
		}

		return longestCommonSubstring;
	}

	private Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : PHPUiPlugin.getImageDescriptorRegistry().get(descriptor);
	}

}
