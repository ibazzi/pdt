package org.eclipse.php.internal.core.compiler.ast.visitor;

import java.text.MessageFormat;
import java.util.*;

import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.references.TypeReference;
import org.eclipse.dltk.ast.statements.Statement;
import org.eclipse.dltk.compiler.problem.*;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.ISourceLineTracker;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.compiler.ast.nodes.*;
import org.eclipse.php.internal.core.compiler.ast.parser.Messages;
import org.eclipse.php.internal.core.compiler.ast.parser.PhpProblemIdentifier;
import org.eclipse.php.internal.core.typeinference.PHPModelUtils;

public class ValidatorVisitor extends PHPASTVisitor {

	private static final String[] ALL_TYPE = new String[] { "array", "mixed", "void", "integer", "int", "string",
			"float", "double", "bool", "boolean", "resource", "null", "NULL", "object" };
	private static final List<String> BUINDIN_SKIP = new ArrayList<String>();
	private static final String NAMESPACE_SEPARATOR = new String(new char[] { NamespaceReference.NAMESPACE_SEPARATOR });

	static {
		BUINDIN_SKIP.add("parent");
		BUINDIN_SKIP.add("self");
		BUINDIN_SKIP.add("static");
		BUINDIN_SKIP.addAll(Arrays.asList(ALL_TYPE));
	}

	private Map<String, UsePartInfo> usePartInfo = new LinkedHashMap<String, UsePartInfo>();
	private Map<String, Boolean> elementExists = new HashMap<String, Boolean>();
	private NamespaceDeclaration currentNamespace;
	private ISourceModule sourceModule;
	private IBuildContext context;

	public ValidatorVisitor(IBuildContext context) {
		this.context = context;
		this.sourceModule = context.getSourceModule();
	}

	public boolean visit(NamespaceDeclaration s) throws Exception {
		currentNamespace = s;
		return true;
	}

	@Override
	public boolean endvisit(NamespaceDeclaration s) throws Exception {
		Collection<UsePartInfo> useInfos = usePartInfo.values();
		for (UsePartInfo useInfo : useInfos) {
			FullyQualifiedReference m = useInfo.getUsePart().getNamespace();
			String name = m.getFullyQualifiedName();
			if (useInfo.getRefCount() == 0) {
				reportProblem(m, Messages.UnusedImport, PhpProblemIdentifier.UnusedImport, name,
						ProblemSeverities.Warning);
			}
		}
		usePartInfo.clear();
		elementExists.clear();
		return super.endvisit(s);
	}

	public boolean visit(PHPMethodDeclaration s) throws Exception {
		if (s.getPHPDoc() != null)
			s.getPHPDoc().traverse(this);
		return visitGeneral(s);
	}

	@Override
	public boolean visit(PHPFieldDeclaration s) throws Exception {
		if (s.getPHPDoc() != null) {
			s.getPHPDoc().traverse(this);
		}
		return super.visit(s);
	}

	public boolean visit(PHPCallExpression node) throws Exception {
		if (node.getReceiver() != null) {
			node.getReceiver().traverse(this);
		}
		if (node.getArgs() != null) {
			node.getArgs().traverse(this);
		}
		return false;
	}

	public boolean visit(FullyQualifiedReference node) throws Exception {
		return visit((TypeReference) node);
	}

	public boolean visit(TypeReference node) throws Exception {
		return visit(node, ProblemSeverities.Error);
	}

	private boolean visit(TypeReference node, ProblemSeverity severity) throws Exception {
		TypeReferenceInfo tri = new TypeReferenceInfo(node, false);
		String nodeName = tri.getTypeName();
		if (BUINDIN_SKIP.contains(nodeName)) {
			return true;
		}
		String key = "";
		if (tri.isGlobal()) {
			key = node.getName();
		} else {
			key = new Path(nodeName).segment(0);
		}
		UsePartInfo info = usePartInfo.get(key);

		boolean isFound = false;
		if (info != null) {
			if (!nodeName.equals(info.getFullyQualifiedName()))
				info.refer();
			isFound = findElement(info.getTypeReferenceInfo());
		} else {
			isFound = findElement(tri);
		}
		if (!isFound) {
			reportProblem(node, Messages.UndefinedType, PhpProblemIdentifier.UndefinedType, node.getName(), severity);
		}
		return false;
	}

	public boolean visit(AnonymousClassDeclaration s) throws Exception {
		IModelElement element = sourceModule.getElementAt(s.start());
		TypeReference superClassNode = s.getSuperClass();
		if (superClassNode == null && s.getInterfaceList().size() > 0) {
			superClassNode = s.getInterfaceList().get(0);
		}
		if (superClassNode != null && element != null) {
			checkUnimplementedMethods(s, superClassNode);
			checkSuperclass((FullyQualifiedReference) superClassNode, element.getElementName());
		}
		return super.visit(s);
	}

	public boolean visit(ClassDeclaration s) throws Exception {
		TypeReference superClass = s.getSuperClass();
		checkUnimplementedMethods(s, s.getRef());
		checkSuperclass((FullyQualifiedReference) superClass, s.getName());
		return true;
	}

	public boolean visit(ClassInstanceCreation s) throws Exception {
		if (s.getClassName() instanceof FullyQualifiedReference) {
			FullyQualifiedReference ref = (FullyQualifiedReference) s.getClassName();
			if (ref != null) {
				IType[] types = PHPModelUtils.getTypes(ref.getFullyQualifiedName(), sourceModule, ref.sourceStart(),
						null);
				for (IType type : types) {
					if (PHPFlags.isInterface(type.getFlags()) || PHPFlags.isAbstract(type.getFlags())) {
						reportProblem(ref, Messages.CannotInstantiateType, PhpProblemIdentifier.CannotInstantiateType,
								type.getElementName(), ProblemSeverities.Error);
						break;
					}
				}
			}
		}
		return visitGeneral(s);
	}

	public boolean visit(InterfaceDeclaration s) throws Exception {
		if (s.getSuperClasses() == null)
			return true;
		for (ASTNode node : s.getSuperClasses().getChilds()) {
			FullyQualifiedReference superClass = (FullyQualifiedReference) node;
			IType[] types = PHPModelUtils.getTypes(superClass.getFullyQualifiedName(), sourceModule,
					superClass.sourceStart(), null);
			for (IType type : types) {
				if (!PHPFlags.isInterface(type.getFlags())) {
					reportProblem(superClass, Messages.SuperInterfaceMustBeAnInterface,
							PhpProblemIdentifier.SuperInterfaceMustBeAnInterface,
							new String[] { superClass.getName(), s.getName() }, ProblemSeverities.Error);
				}
			}
		}
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration s) throws Exception {
		checkDuplicateDeclaration(s);
		return super.visit(s);
	}

	public boolean visit(UsePart part) throws Exception {
		UsePartInfo info = new UsePartInfo(part);
		TypeReferenceInfo tri = info.getTypeReferenceInfo();
		String name = tri.getTypeName();
		String currentNamespaceName;
		if (currentNamespace == null) {
			currentNamespaceName = "";
		} else {
			currentNamespaceName = currentNamespace.getName();
		}
		if (!findElement(tri)) {
			reportProblem(tri.getTypeReference(), Messages.ImportNotFound, PhpProblemIdentifier.ImportNotFound, name,
					ProblemSeverities.Error);
		} else if (usePartInfo.get(info.getRealName()) != null) {
			reportProblem(tri.getTypeReference(), Messages.DuplicateImport, PhpProblemIdentifier.DuplicateImport,
					new String[] { name, info.getRealName() }, ProblemSeverities.Error);
		} else if (info.getNamespaceName().equals(currentNamespaceName)) {
			reportProblem(tri.getTypeReference(), Messages.UnnecessaryImport, PhpProblemIdentifier.UnnecessaryImport,
					new String[] { name }, ProblemSeverities.Warning);
		} else {
			usePartInfo.put(info.getRealName(), info);
		}
		return false;
	}

	public boolean visit(PHPDocTag phpDocTag) throws Exception {
		for (TypeReference simpleReference : phpDocTag.getTypeReferences()) {
			TypeReference typeReference = (TypeReference) simpleReference;
			String typeName = typeReference.getName();
			if (typeName.endsWith("[]")) {
				typeName = typeName.substring(0, typeName.length() - 2);
				typeReference.setName(typeName);
				typeReference.setEnd(typeReference.end() - 2);
			}
			visit(typeReference, ProblemSeverities.Warning);
		}
		return false;
	}

	private void checkDuplicateDeclaration(TypeDeclaration node) {
		String name = node.getName();
		if (usePartInfo.containsKey(name)) {
			reportProblem(node.getRef(), Messages.DuplicateDeclaration, PhpProblemIdentifier.DuplicateDeclaration, name,
					ProblemSeverities.Error);
		}
	}

	private void checkUnimplementedMethods(Statement statement, ASTNode classNode) throws ModelException {
		IModelElement element = sourceModule.getElementAt(statement.start());
		if (!(element instanceof IType))
			return;
		IType type = (IType) element;
		if (!PHPFlags.isAbstract(type.getFlags())) {
			IMethod[] methods = PHPModelUtils.getUnimplementedMethods(type, null);
			for (IMethod method : methods) {
				if (method.getParent().getElementName().equals(type.getElementName())) {
					continue;
				}
				StringBuffer methodName = new StringBuffer(method.getParent().getElementName()).append("::");
				PHPModelUtils.getMethodLabel(method, methodName);
				reportProblem(classNode, Messages.AbstractMethodMustBeImplemented,
						PhpProblemIdentifier.AbstractMethodMustBeImplemented,
						new String[] { type.getElementName(), methodName.toString() }, ProblemSeverities.Error);
			}
		}
	}

	private void checkSuperclass(FullyQualifiedReference superClass, String className) throws ModelException {
		if (superClass != null) {
			IType[] types = PHPModelUtils.getTypes(superClass.getFullyQualifiedName(), sourceModule,
					superClass.sourceStart(), null);
			for (IType type : types) {
				if (PHPFlags.isInterface(type.getFlags())) {
					reportProblem(superClass, Messages.SuperclassMustBeAClass,
							PhpProblemIdentifier.SuperclassMustBeAClass,
							new String[] { superClass.getName(), className }, ProblemSeverities.Error);
				}
				if (PHPFlags.isFinal(type.getFlags())) {
					reportProblem(superClass, Messages.ClassExtendFinalClass,
							PhpProblemIdentifier.ClassExtendFinalClass,
							new String[] { className, type.getElementName() }, ProblemSeverities.Error);
				}
			}
		}
	}

	private boolean findElement(TypeReferenceInfo info) {
		String name = info.getFullyQualifiedName();
		if (elementExists.containsKey(name)) {
			return elementExists.get(name);
		}

		boolean isFound = false;
		try {
			TypeReference type = info.getTypeReference();
			IModelElement[] types = PHPModelUtils.getTypes(name, context.getSourceModule(), type.start(), null);
			if (types.length == 0 && info.isUseStatement()) {
				types = sourceModule.codeSelect(type.start(), type.end() - type.start());
			}
			if (types.length > 0) {
				isFound = true;
			}
		} catch (ModelException e) {
			PHPCorePlugin.log(e);
		}
		elementExists.put(name, isFound);
		return isFound;
	}

	private void reportProblem(ASTNode s, String message, IProblemIdentifier id, String[] stringArguments,
			ProblemSeverity severity) {
		message = MessageFormat.format(message, (Object[]) stringArguments);
		reportProblem(s, message, id, severity);
	}

	private void reportProblem(ASTNode s, String message, IProblemIdentifier id, ProblemSeverity severity) {
		int start = 0, end = 0, line = 1;
		ISourceLineTracker tracker = context.getLineTracker();
		if (s != null) {
			start = s.sourceStart();
			end = s.sourceEnd();
			line = tracker.getLineNumberOfOffset(start);
		} else {
			start = end = context.getLineTracker().getLineOffset(line);
		}
		IProblem problem = new DefaultProblem(context.getFile().getName(), message, id, null, severity, start, end,
				line, 0);
		context.getProblemReporter().reportProblem(problem);
	}

	private void reportProblem(ASTNode s, String message, IProblemIdentifier id, String stringArguments,
			ProblemSeverity severity) {
		reportProblem(s, message, id, new String[] { stringArguments }, severity);
	}

	private class UsePartInfo {
		private UsePart usePart;
		private String realName;
		private int refCount;
		private String fullyQualifiedName;
		private TypeReferenceInfo tri;

		public UsePartInfo(UsePart usePart) {
			this.usePart = usePart;
			tri = new TypeReferenceInfo(usePart.getNamespace(), true);
			if (usePart.getAlias() != null) {
				realName = usePart.getAlias().getName();
			} else {
				realName = usePart.getNamespace().getName();
			}
			if (tri.getNamespaceName() != null) {
				fullyQualifiedName = tri.getNamespaceName();
			}
			fullyQualifiedName += NAMESPACE_SEPARATOR + realName;
			if (!fullyQualifiedName.startsWith(NAMESPACE_SEPARATOR)) {
				fullyQualifiedName = NAMESPACE_SEPARATOR + fullyQualifiedName;
			}
		}

		public UsePart getUsePart() {
			return usePart;
		}

		public int getRefCount() {
			return refCount;
		}

		public void refer() {
			refCount++;
		}

		public String getRealName() {
			return realName;
		}

		public String getFullyQualifiedName() {
			return fullyQualifiedName;
		}

		public String getNamespaceName() {
			return tri.getNamespaceName();
		}

		public TypeReferenceInfo getTypeReferenceInfo() {
			return tri;
		}
	}

	private class TypeReferenceInfo {

		private TypeReference typeReference;
		private boolean isGlobal = false;
		private boolean hasNamespace = false;
		private String namespaceName = "";
		private String typeName;
		private String fullyQualifiedName;
		private boolean isUseStatement;

		public TypeReferenceInfo(TypeReference typeReference, boolean isUseStatement) {
			this.typeReference = typeReference;
			this.isUseStatement = isUseStatement;
			FullyQualifiedReference fullTypeReference = null;
			if (typeReference instanceof FullyQualifiedReference) {
				fullTypeReference = (FullyQualifiedReference) typeReference;
				if (fullTypeReference.getNamespace() != null) {
					hasNamespace = true;
					namespaceName = fullTypeReference.getNamespace().getName();
					if (usePartInfo.get(namespaceName) != null) {
						namespaceName = usePartInfo.get(namespaceName).getFullyQualifiedName();
					}
				}
			}

			if (fullTypeReference != null && hasNamespace) {
				isGlobal = fullTypeReference.getNamespace().isGlobal();
				typeName = fullTypeReference.getFullyQualifiedName();
			} else {
				typeName = typeReference.getName();
			}

			if (fullTypeReference != null && isGlobal) {
				fullyQualifiedName = fullTypeReference.getFullyQualifiedName();
			} else if (hasNamespace) {
				fullyQualifiedName = namespaceName + NAMESPACE_SEPARATOR + typeReference.getName();
			} else {
				fullyQualifiedName = typeName;
			}
			if (!isUseStatement && !fullyQualifiedName.startsWith(NAMESPACE_SEPARATOR)) {
				String key = new Path(fullyQualifiedName).segment(0);
				if (usePartInfo.containsKey(key)) {
					fullyQualifiedName = usePartInfo.get(key).getFullyQualifiedName();
				} else if (currentNamespace != null)
					fullyQualifiedName = currentNamespace.getName() + NAMESPACE_SEPARATOR + fullyQualifiedName;
			}
			if (!fullyQualifiedName.startsWith(NAMESPACE_SEPARATOR))
				fullyQualifiedName = NAMESPACE_SEPARATOR + fullyQualifiedName;
		}

		public boolean isGlobal() {
			return isGlobal;
		}

		public String getTypeName() {
			return typeName;
		}

		public String getFullyQualifiedName() {
			return fullyQualifiedName;
		}

		public String getNamespaceName() {
			return namespaceName;
		}

		public TypeReference getTypeReference() {
			return typeReference;
		}

		public boolean isUseStatement() {
			return isUseStatement;
		}
	}

}
