/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *     Yannick de Lange <yannick.l.88@gmail.com>
 *******************************************************************************/
package org.eclipse.php.internal.ui.corext.codemanipulation;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.dltk.compiler.problem.DefaultProblem;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.core.index2.search.ISearchEngine.MatchRule;
import org.eclipse.dltk.core.index2.search.ModelAccess;
import org.eclipse.dltk.core.manipulation.SourceModuleChange;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.core.search.TypeNameMatch;
import org.eclipse.dltk.ui.viewsupport.BasicElementLabels;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.php.internal.core.ast.nodes.*;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite;
import org.eclipse.php.internal.core.ast.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.php.internal.core.ast.visitor.ApplyAll;
import org.eclipse.php.internal.core.compiler.ast.nodes.NamespaceReference;
import org.eclipse.php.internal.core.compiler.ast.parser.PhpProblemIdentifier;
import org.eclipse.php.internal.core.search.PHPSearchTypeNameMatch;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.internal.ui.corext.util.Messages;
import org.eclipse.php.internal.ui.corext.util.Strings;
import org.eclipse.php.internal.ui.corext.util.TypeNameMatchCollector;
import org.eclipse.php.internal.ui.text.correction.ASTResolving;
import org.eclipse.php.internal.ui.text.correction.ProblemLocation;
import org.eclipse.php.internal.ui.text.correction.SimilarElementsRequestor;
import org.eclipse.php.ui.editor.SharedASTProvider;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class OrganizeUseStatementsOperation implements IWorkspaceRunnable {
	public static interface IChooseImportQuery {
		/**
		 * Selects imports from a list of choices.
		 * 
		 * @param openChoices
		 *            From each array, a type reference has to be selected
		 * @param ranges
		 *            For each choice the range of the corresponding type
		 *            reference.
		 * @return Returns <code>null</code> to cancel the operation, or the
		 *         selected imports.
		 */
		TypeNameMatch[] chooseImports(TypeNameMatch[][] openChoices, ISourceRange[] ranges);
	}

	private static class UnresolvableImportMatcher {
		static UnresolvableImportMatcher forProgram(Program cu) {
			Collection<UseStatement> unresolvableImports = determineUnresolvableImports(cu);

			Map<String, Set<String>> typeImportsBySimpleName = new HashMap<>();
			for (UseStatement importDeclaration : unresolvableImports) {
				for (UseStatementPart part : importDeclaration.parts()) {
					String qualifiedName = part.getName().getName();

					String simpleName = qualifiedName
							.substring(qualifiedName.lastIndexOf(NamespaceReference.NAMESPACE_SEPARATOR) + 1);

					Map<String, Set<String>> importsBySimpleName = typeImportsBySimpleName;
					Set<String> importsWithSimpleName = importsBySimpleName.get(simpleName);
					if (importsWithSimpleName == null) {
						importsWithSimpleName = new HashSet<>();
						importsBySimpleName.put(simpleName, importsWithSimpleName);
					}

					importsWithSimpleName.add(qualifiedName);
				}

			}

			return new UnresolvableImportMatcher(typeImportsBySimpleName);
		}

		private static Collection<UseStatement> determineUnresolvableImports(Program program) {
			Collection<UseStatement> unresolvableImports = new ArrayList<>(program.getUseStatements().size());
			IProblem[] problems = program.getProblems();
			for (IProblem problem : problems) {
				PhpProblemIdentifier id = PhpProblemIdentifier.getProblem(((DefaultProblem) problem).getID());
				if (id == PhpProblemIdentifier.ImportNotFound) {
					UseStatement problematicImport = getProblematicImport(problem, program);
					if (problematicImport != null) {
						unresolvableImports.add(problematicImport);
					}
				}
			}

			return unresolvableImports;
		}

		private static UseStatement getProblematicImport(IProblem problem, Program cu) {
			ASTNode coveringNode = new ProblemLocation(problem).getCoveringNode(cu);
			if (coveringNode != null && coveringNode instanceof UseStatement) {
				return (UseStatement) coveringNode;
			}
			return null;
		}

		private final Map<String, Set<String>> fTypeImportsBySimpleName;

		private UnresolvableImportMatcher(Map<String, Set<String>> typeImportsBySimpleName) {
			fTypeImportsBySimpleName = typeImportsBySimpleName;
		}

		private Set<String> matchImports(String simpleName) {
			Map<String, Set<String>> importsBySimpleName = fTypeImportsBySimpleName;

			Set<String> matchingSingleImports = importsBySimpleName.get(simpleName);
			if (matchingSingleImports != null) {
				return Collections.unmodifiableSet(matchingSingleImports);
			}

			Set<String> matchingOnDemandImports = importsBySimpleName.get("*"); //$NON-NLS-1$
			if (matchingOnDemandImports != null) {
				return Collections.unmodifiableSet(matchingOnDemandImports);
			}

			return Collections.emptySet();
		}

		Set<String> matchTypeImports(String simpleName) {
			return matchImports(simpleName);
		}
	}

	private static class TypeReferenceProcessor {

		private static class UnresolvedTypeData {
			final Identifier ref;
			final int typeKinds;
			final List<TypeNameMatch> foundInfos;

			public UnresolvedTypeData(Identifier ref) {
				this.ref = ref;
				this.typeKinds = ASTResolving.getPossibleTypeKinds(ref);
				this.foundInfos = new ArrayList<>(3);
			}

			public void addInfo(TypeNameMatch info) {
				for (int i = this.foundInfos.size() - 1; i >= 0; i--) {
					TypeNameMatch curr = this.foundInfos.get(i);
					if (curr.getTypeContainerName().equals(info.getTypeContainerName())) {
						return; // not added. already contains type with same
								// name
					}
				}
				foundInfos.add(info);
			}
		}

		private Set<String> fOldSingleImports;

		private ImportRewrite fImpStructure;

		private boolean fDoIgnoreLowerCaseNames;

		private final UnresolvableImportMatcher fUnresolvableImportMatcher;

		// private IPackageFragment fCurrPackage;
		//
		// private ScopeAnalyzer fAnalyzer;
		private boolean fAllowDefaultPackageImports;

		private Map<String, UnresolvedTypeData> fUnresolvedTypes;
		private Set<String> fImportsAdded;
		private TypeNameMatch[][] fOpenChoices;
		private SourceRange[] fSourceRanges;

		public TypeReferenceProcessor(Set<String> oldSingleImports, Program root, ImportRewrite impStructure,
				boolean ignoreLowerCaseNames, UnresolvableImportMatcher unresolvableImportMatcher) {
			fOldSingleImports = oldSingleImports;
			fImpStructure = impStructure;
			fDoIgnoreLowerCaseNames = ignoreLowerCaseNames;
			fUnresolvableImportMatcher = unresolvableImportMatcher;

			// fAnalyzer= new ScopeAnalyzer(root);
			//
			// fCurrPackage= (IPackageFragment) cu.getParent()

			fAllowDefaultPackageImports = true;

			fImportsAdded = new HashSet<>();
			fUnresolvedTypes = new HashMap<>();
		}

		/**
		 * Tries to find the given type name and add it to the import structure.
		 * 
		 * @param ref
		 *            the name node
		 */
		public void add(Identifier ref) {
			String typeName = ref.getName();

			if (fImportsAdded.contains(typeName)) {
				return;
			}

			IBinding binding = ref.resolveBinding();
			if (binding != null) {
				if (binding.getKind() != IBinding.TYPE) {
					return;
				}
				ITypeBinding typeBinding = ((ITypeBinding) binding).getTypeDeclaration();
				if (typeBinding != null) {
					String typeBindingName = typeBinding.getName();
					if (typeBindingName != null && typeBindingName.startsWith(NamespaceReference.NAMESPACE_DELIMITER)) {
						typeBindingName = typeBindingName.substring(1);
					}
					fImpStructure.addImport(typeBindingName);
					fImportsAdded.add(typeName);
					return;
				}
			} else {
				if (fDoIgnoreLowerCaseNames && typeName.length() > 0) {
					char ch = typeName.charAt(0);
					if (Strings.isLowerCase(ch) && Character.isLetter(ch)) {
						return;
					}
				}
			}

			fImportsAdded.add(typeName);
			fUnresolvedTypes.put(typeName, new UnresolvedTypeData(ref));
		}

		public boolean process(IProgressMonitor monitor) throws ModelException {
			try {
				int nUnresolved = fUnresolvedTypes.size();
				if (nUnresolved == 0) {
					return false;
				}
				final IScriptProject project = fImpStructure.getProgram().getScriptProject();
				IDLTKSearchScope scope = SearchEngine.createSearchScope(project);
				final ArrayList<TypeNameMatch> typesFound = new ArrayList<>();
				TypeNameMatchCollector collector = new TypeNameMatchCollector(typesFound);
				for (Iterator<String> iter = fUnresolvedTypes.keySet().iterator(); iter.hasNext();) {
					ModelAccess modelAccess = new ModelAccess();
					IType[] types = modelAccess.findTypes(iter.next(), MatchRule.EXACT, 0, 0, scope, monitor);
					for (IType type : types) {
						TypeNameMatch match = new PHPSearchTypeNameMatch(type, type.getFlags());
						collector.acceptTypeNameMatch(match);
					}
				}

				for (int i = 0; i < typesFound.size(); i++) {
					TypeNameMatch curr = typesFound.get(i);
					UnresolvedTypeData data = fUnresolvedTypes.get(curr.getSimpleTypeName());
					if (data != null && isOfKind(curr, data.typeKinds)) {
						if (fAllowDefaultPackageImports || curr.getPackageName().length() > 0) {
							data.addInfo(curr);
						}
					}
				}

				for (Entry<String, UnresolvedTypeData> entry : fUnresolvedTypes.entrySet()) {
					if (entry.getValue().foundInfos.size() == 0) { // No result
																	// found in
																	// search
						Set<String> matchingUnresolvableImports = fUnresolvableImportMatcher
								.matchTypeImports(entry.getKey());
						if (!matchingUnresolvableImports.isEmpty()) {
							// If there are matching unresolvable import(s),
							// rely on them to provide the type.
							for (String string : matchingUnresolvableImports) {
								fImpStructure.addImport(string, UNRESOLVABLE_IMPORT_CONTEXT);
							}
						}
					}
				}

				ArrayList<TypeNameMatch[]> openChoices = new ArrayList<>(nUnresolved);
				ArrayList<SourceRange> sourceRanges = new ArrayList<>(nUnresolved);
				for (Iterator<UnresolvedTypeData> iter = fUnresolvedTypes.values().iterator(); iter.hasNext();) {
					UnresolvedTypeData data = iter.next();
					TypeNameMatch[] openChoice = processTypeInfo(data.foundInfos);
					if (openChoice != null) {
						openChoices.add(openChoice);
						sourceRanges.add(new SourceRange(data.ref.getStart(), data.ref.getLength()));
					}
				}
				if (openChoices.isEmpty()) {
					return false;
				}
				fOpenChoices = openChoices.toArray(new TypeNameMatch[openChoices.size()][]);
				fSourceRanges = sourceRanges.toArray(new SourceRange[sourceRanges.size()]);
				return true;
			} finally {
				monitor.done();
			}
		}

		private TypeNameMatch[] processTypeInfo(List<TypeNameMatch> typeRefsFound) {
			int nFound = typeRefsFound.size();
			if (nFound == 0) {
				// nothing found
				return null;
			} else if (nFound == 1) {
				TypeNameMatch typeRef = typeRefsFound.get(0);
				fImpStructure.addImport(typeRef.getFullyQualifiedName());
				return null;
			} else {
				// multiple found, use old imports to find an entry
				for (int i = 0; i < nFound; i++) {
					TypeNameMatch typeRef = typeRefsFound.get(i);
					String fullName = typeRef.getFullyQualifiedName();
					if (fOldSingleImports.contains(fullName)) {
						// was single-imported
						fImpStructure.addImport(fullName);
						return null;
					}
				}
				// return the open choices
				return typeRefsFound.toArray(new TypeNameMatch[nFound]);
			}
		}

		private boolean isOfKind(TypeNameMatch curr, int typeKinds) {
			int flags = curr.getModifiers();
			if (Flags.isInterface(flags)) {
				return (typeKinds & SimilarElementsRequestor.INTERFACES) != 0;
			}
			return (typeKinds & SimilarElementsRequestor.CLASSES) != 0;
		}

		public TypeNameMatch[][] getChoices() {
			return fOpenChoices;
		}

		public ISourceRange[] getChoicesSourceRanges() {
			return fSourceRanges;
		}
	}

	/**
	 * Used to ensure that unresolvable imports don't get reduced into on-demand
	 * imports.
	 */
	private static ImportRewriteContext UNRESOLVABLE_IMPORT_CONTEXT = new ImportRewriteContext() {
		@Override
		public int findInContext(String qualifier, String name, int kind) {
			return RES_NAME_UNKNOWN;
		}
	};

	private int fNumberOfImportsAdded;
	private int fNumberOfImportsRemoved;

	private boolean fIgnoreLowerCaseNames;

	private IChooseImportQuery fChooseImportQuery;

	private ISourceModule fSourceModule;

	private Program fASTRoot;

	public OrganizeUseStatementsOperation(ISourceModule sourceModule, Program astRoot, boolean ignoreLowerCaseNames,
			IChooseImportQuery chooseImportQuery) {
		fSourceModule = sourceModule;
		fASTRoot = astRoot;

		fIgnoreLowerCaseNames = ignoreLowerCaseNames;
		// fAllowSyntaxErrors= allowSyntaxErrors;
		fChooseImportQuery = chooseImportQuery;

		fNumberOfImportsAdded = 0;
		fNumberOfImportsRemoved = 0;

		// fParsingError= null;
	}

	public TextEdit createTextEdit(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException, IOException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			fNumberOfImportsAdded = 0;
			fNumberOfImportsRemoved = 0;

			monitor.beginTask(Messages.format(CodeGenerationMessages.OrganizeImportsOperation_description,
					BasicElementLabels.getFileName(fSourceModule)), 9);

			Program astRoot = fASTRoot;
			if (astRoot == null) {
				astRoot = SharedASTProvider.getAST(fSourceModule, SharedASTProvider.WAIT_YES,
						SubMonitor.convert(monitor, 2));
				if (monitor.isCanceled())
					throw new OperationCanceledException();
			} else {
				monitor.worked(2);
			}

			ImportRewrite importsRewrite = ImportRewrite.create(astRoot, false);

			Set<String> oldSingleImports = new HashSet<>();
			List<Identifier> typeReferences = new ArrayList<>();

			if (!collectReferences(astRoot, typeReferences, oldSingleImports))
				return null;

			UnresolvableImportMatcher unresolvableImportMatcher = UnresolvableImportMatcher.forProgram(astRoot);

			TypeReferenceProcessor processor = new TypeReferenceProcessor(oldSingleImports, astRoot, importsRewrite,
					fIgnoreLowerCaseNames, unresolvableImportMatcher);

			Iterator<Identifier> refIterator = typeReferences.iterator();
			while (refIterator.hasNext()) {
				Identifier typeRef = refIterator.next();
				processor.add(typeRef);
			}

			boolean hasOpenChoices = processor.process(SubMonitor.convert(monitor, 3));

			if (hasOpenChoices && fChooseImportQuery != null) {
				TypeNameMatch[][] choices = processor.getChoices();
				ISourceRange[] ranges = processor.getChoicesSourceRanges();
				TypeNameMatch[] chosen = fChooseImportQuery.chooseImports(choices, ranges);
				if (chosen == null) {
					// cancel pressed by the user
					throw new OperationCanceledException();
				}
				for (int i = 0; i < chosen.length; i++) {
					TypeNameMatch typeInfo = chosen[i];
					if (typeInfo != null) {
						importsRewrite.addImport(typeInfo.getFullyQualifiedName());
					} else { // Skipped by user
						String typeName = choices[i][0].getSimpleTypeName();
						Set<String> matchingUnresolvableImports = unresolvableImportMatcher.matchTypeImports(typeName);
						if (!matchingUnresolvableImports.isEmpty()) {
							// If there are matching unresolvable import(s),
							// rely on them to provide the type.
							for (String string : matchingUnresolvableImports) {
								importsRewrite.addImport(string, UNRESOLVABLE_IMPORT_CONTEXT);
							}
						}
					}
				}
			}

			TextEdit result = importsRewrite.rewriteImports(SubMonitor.convert(monitor, 3));

			determineImportDifferences(importsRewrite, oldSingleImports);

			return result;
		} finally {
			monitor.done();
		}
	}

	private void determineImportDifferences(ImportRewrite importsStructure, Set<String> oldSingleImports) {
		ArrayList<String> importsAdded = new ArrayList<>();
		importsAdded.addAll(Arrays.asList(importsStructure.getCreatedImports()));

		Object[] content = oldSingleImports.toArray();
		for (int i = 0; i < content.length; i++) {
			String importName = (String) content[i];
			if (importsAdded.remove(importName))
				oldSingleImports.remove(importName);
		}
		fNumberOfImportsAdded = importsAdded.size();
		fNumberOfImportsRemoved = oldSingleImports.size();
	}

	/**
	 * Runs the operation.
	 * 
	 * @param monitor
	 *            the progress monitor
	 * @throws CoreException
	 *             thrown when the operation failed
	 * @throws OperationCanceledException
	 *             Runtime error thrown when operation is canceled.
	 */
	@Override
	public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
			monitor.beginTask(Messages.format(CodeGenerationMessages.OrganizeImportsOperation_description,
					BasicElementLabels.getFileName(fSourceModule)), 10);

			SourceModuleChange cuChange = new SourceModuleChange("1111", fSourceModule);
			cuChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
			TextChange change = cuChange;
			TextEdit edit = createTextEdit(SubMonitor.convert(monitor));
			if (edit != null) {
				change.setEdit(edit);
			}

			change.initializeValidationData(new NullProgressMonitor());
			RefactoringStatus valid = change.isValid(new NullProgressMonitor());
			if (valid.hasFatalError()) {
				IStatus status = new Status(IStatus.ERROR, PHPUiPlugin.ID, IStatus.ERROR,
						valid.getMessageMatchingSeverity(RefactoringStatus.FATAL), null);
				throw new CoreException(status);
			} else {
				IUndoManager manager = RefactoringCore.getUndoManager();
				Change undoChange;
				boolean successful = false;
				try {
					manager.aboutToPerformChange(change);
					undoChange = change.perform(new NullProgressMonitor());
					successful = true;
				} finally {
					manager.changePerformed(change, successful);
				}
				if (undoChange != null) {
					undoChange.initializeValidationData(new NullProgressMonitor());
					manager.addUndo("OrganizeUseStatements", undoChange);
				}
			}
		} catch (MalformedTreeException | IOException e) {
			e.printStackTrace();
		} finally {
			monitor.done();
		}
	}

	private boolean collectReferences(Program astRoot, List<Identifier> typeReferences, Set<String> oldSingleImports) {
		List<UseStatement> imports = astRoot.getUseStatements();
		for (int i = 0; i < imports.size(); i++) {
			UseStatement curr = imports.get(i);
			for (UseStatementPart part : curr.parts()) {
				oldSingleImports.add(part.getName().getName());
			}
		}
		astRoot.accept(new ReferencesCollector(typeReferences));
		return true;
	}

	public int getNumberOfImportsAdded() {
		return fNumberOfImportsAdded;
	}

	public int getNumberOfImportsRemoved() {
		return fNumberOfImportsRemoved;
	}

	/**
	 * @return Returns the scheduling rule for this operation
	 */
	public ISchedulingRule getScheduleRule() {
		return fSourceModule.getResource();
	}

	static class ReferencesCollector extends ApplyAll {
		List<Identifier> fTypeReferences;

		public ReferencesCollector(List<Identifier> typeReferences) {
			fTypeReferences = typeReferences;
		}

		@Override
		protected boolean apply(ASTNode node) {
			return true;
		}

		@Override
		public boolean visit(UseStatement statement) {
			return false;
		}

		@Override
		public boolean visit(FunctionName functionName) {
			return false;
		}

		@Override
		public boolean visit(Variable variable) {
			return false;
		}

		@Override
		public boolean visit(Identifier identifier) {
			ASTNode parent = identifier.getParent();
			if (parent instanceof NamespaceName && parent.getParent() instanceof NamespaceDeclaration)
				return false;
			if (parent instanceof ClassDeclaration || parent instanceof FunctionDeclaration)
				return false;
			fTypeReferences.add(identifier);
			return false;
		}

	}
}
