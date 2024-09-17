/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.v2migration;

import static software.amazon.awssdk.v2migration.internal.utils.NamingConversionUtils.getV2Equivalent;
import static software.amazon.awssdk.v2migration.internal.utils.NamingConversionUtils.getV2ModelPackageWildCardEquivalent;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isCustomSdk;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV1ClientClass;
import static software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils.isV1ModelClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.java.tree.TypedTree;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * Recipe to change AWS SDK for Java v1 generated types such as SDK client and request and response POJOs to v2 equivalents.
 *
 * <p>
 * This class contains source imported from https://github.com/openrewrite/rewrite/, licensed under the Apache License 2.0,
 * available at the time of the fork (03/27/2024) here: https://github.com/openrewrite/rewrite/blob/main/LICENSE
 * <p>
 * All original source licensed under the Apache License 2.0 by openrewrite. All modifications are licensed under the Apache
 * License 2.0 by Amazon Web Services.
 */
@SdkInternalApi
public class ChangeSdkType extends Recipe {
    private static final Logger log = Logger.loggerFor(ChangeSdkType.class);
    private static final String V1_SERVICE_MODEL_WILD_CARD_CLASS_PATTERN =
        "com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.model\\.\\*";
    private static final String V1_SERVICE_WILD_CARD_CLASS_PATTERN = "com\\.amazonaws\\.services\\.[a-zA-Z0-9]+\\.\\*";

    @Override
    public String getDisplayName() {
        return "Change AWS SDK for Java v1 types to v2 equivalents";
    }

    @Override
    public String getDescription() {
        return "Change AWS SDK for Java v1 types to v2 equivalents.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeTypeVisitor();
    }

    private static class ChangeTypeVisitor extends JavaVisitor<ExecutionContext> {

        private J.Identifier importAlias;

        private final Map<JavaType, JavaType> oldNameToChangedType = new IdentityHashMap<>();
        private final Set<String> topLevelClassnames = new HashSet<>();
        private final List<String> wildcardImports = new ArrayList<>();

        private Map<String, Pair<JavaType.Class, JavaType>> oldTypeToNewType = new HashMap<>();

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
            if (cd.getType() != null) {
                topLevelClassnames.add(getTopLevelClassName(cd.getType()).getFullyQualifiedName());
            }
            return cd;
        }

        @Override
        public J visitImport(J.Import anImport, ExecutionContext ctx) {
            JavaType.FullyQualified fullyQualified =
                Optional.ofNullable(anImport.getQualid())
                        .map(J.FieldAccess::getType)
                        .map(TypeUtils::asFullyQualified)
                        .orElse(null);

            if (fullyQualified == null) {
                String fullName = anImport.getTypeName();
                if (isWildcard(fullName)) {
                    maybeAddImport(getV2ModelPackageWildCardEquivalent(fullName));
                    wildcardImports.add(fullName);
                }
                return anImport;
            }

            String currentFqcn = fullyQualified.getFullyQualifiedName();

            if (isV1Class(fullyQualified)) {
                storeV1ClassMetadata(currentFqcn);
                if (anImport.getAlias() != null) {
                    importAlias = anImport.getAlias();
                }
            }

            return anImport;
        }

        private static boolean isWildcard(String fullName) {
            return fullName.matches(V1_SERVICE_MODEL_WILD_CARD_CLASS_PATTERN) ||
                   fullName.matches(V1_SERVICE_WILD_CARD_CLASS_PATTERN);
        }

        private static boolean isV1Class(JavaType.FullyQualified fullyQualified) {
            String fullyQualifiedName = fullyQualified.getFullyQualifiedName();

            if (!isV1ModelClass(fullyQualified) && !isV1ClientClass(fullyQualified)) {
                return false;
            }

            if (isCustomSdk(fullyQualifiedName)) {
                log.info(() -> String.format("Skipping transformation for %s because it is a custom SDK", fullyQualifiedName));
                return false;
            }
            return true;
        }

        @Override
        public JavaType visitType(JavaType javaType, ExecutionContext ctx) {
            if (javaType == null || javaType instanceof JavaType.Unknown) {
                return javaType;
            }

            return updateType(javaType);
        }

        private void addImport(JavaType.FullyQualified owningClass) {
            if (importAlias != null) {
                maybeAddImport(owningClass.getPackageName(), owningClass.getClassName(), null, importAlias.getSimpleName(), true);
            }

            maybeAddImport(owningClass.getPackageName(), owningClass.getClassName(), null, null, true);
        }

        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            J currentTree = super.postVisit(tree, ctx);
            if (currentTree instanceof J.ArrayType) {
                J.ArrayType arrayType = (J.ArrayType) currentTree;
                JavaType type = updateType(arrayType.getType());
                currentTree = arrayType.withType(type);

            } else if (currentTree instanceof J.MethodDeclaration) {

                J.MethodDeclaration method = (J.MethodDeclaration) currentTree;
                JavaType.Method mt = updateType(method.getMethodType());
                currentTree = method.withMethodType(mt)
                                    .withName(method.getName().withType(mt));

            } else if (currentTree instanceof J.MethodInvocation) {

                J.MethodInvocation method = (J.MethodInvocation) currentTree;
                JavaType.Method mt = updateType(method.getMethodType());
                currentTree = method.withMethodType(mt)
                                    .withName(method.getName().withType(mt));

            } else if (currentTree instanceof J.NewClass) {

                J.NewClass n = (J.NewClass) currentTree;
                currentTree = n.withConstructorType(updateType(n.getConstructorType()));

            } else if (tree instanceof TypedTree) {

                currentTree = ((TypedTree) tree).withType(updateType(((TypedTree) tree).getType()));

            } else if (tree instanceof JavaSourceFile) {
                currentTree = postVisitSourceFile((JavaSourceFile) tree, ctx, currentTree);
            }

            return currentTree;
        }

        private J postVisitSourceFile(JavaSourceFile tree, ExecutionContext ctx, J currentTree) {
            JavaSourceFile sourceFile = tree;

            for (Pair<JavaType.Class, JavaType> oldToNew : oldTypeToNewType.values()) {
                JavaType.Class originalType = oldToNew.left();
                JavaType targetType = oldToNew.right();
                if (targetType instanceof JavaType.FullyQualified) {
                    for (J.Import anImport : sourceFile.getImports()) {
                        if (anImport.isStatic()) {
                            continue;
                        }

                        JavaType maybeType = anImport.getQualid().getType();
                        if (maybeType instanceof JavaType.FullyQualified) {
                            JavaType.FullyQualified type = (JavaType.FullyQualified) maybeType;
                            String fullyQualifiedName = originalType.getFullyQualifiedName();
                            if (fullyQualifiedName.equals(type.getFullyQualifiedName())) {
                                sourceFile = (JavaSourceFile) new RemoveImport<ExecutionContext>(fullyQualifiedName)
                                    .visit(sourceFile, ctx, getCursor().getParentOrThrow());
                            } else if (originalType.getOwningClass() != null &&
                                       originalType.getOwningClass().getFullyQualifiedName()
                                                   .equals(type.getFullyQualifiedName())) {
                                sourceFile =
                                    (JavaSourceFile) new RemoveImport<ExecutionContext>(
                                        originalType.getOwningClass().getFullyQualifiedName())
                                        .visit(sourceFile, ctx, getCursor().getParentOrThrow());
                            }
                        }
                    }
                }

                JavaType.FullyQualified fullyQualifiedTarget = TypeUtils.asFullyQualified(targetType);
                if (fullyQualifiedTarget != null) {
                    JavaType.FullyQualified owningClass = fullyQualifiedTarget.getOwningClass();
                    if (!topLevelClassnames.contains(getTopLevelClassName(fullyQualifiedTarget).getFullyQualifiedName())) {
                        if (owningClass != null) {
                            addImport(owningClass);
                        }
                        addImport(fullyQualifiedTarget);
                    }
                }

                if (sourceFile != null) {
                    sourceFile = sourceFile.withImports(
                        ListUtils.map(sourceFile.getImports(), i -> visitAndCast(i, ctx,
                                                                                 super::visitImport)));
                }

                currentTree = sourceFile;
            }

            return removeWildcardImports(ctx, currentTree, sourceFile);
        }

        private J removeWildcardImports(ExecutionContext ctx, J currentTree, JavaSourceFile sourceFile) {
            for (String fqcn : wildcardImports) {
                sourceFile = (JavaSourceFile) new RemoveImport<ExecutionContext>(fqcn)
                    .visit(sourceFile, ctx, getCursor().getParentOrThrow());

                if (sourceFile != null) {
                    sourceFile = sourceFile.withImports(
                        ListUtils.map(sourceFile.getImports(), i -> visitAndCast(i, ctx,
                                                                                 super::visitImport)));
                }

                currentTree = sourceFile;
            }
            return currentTree;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            for (Pair<JavaType.Class, JavaType> entry : oldTypeToNewType.values()) {
                JavaType.Class originalType = entry.left();
                JavaType targetType = entry.right();
                if (fieldAccess.isFullyQualifiedClassReference(originalType.getFullyQualifiedName())) {
                    if (targetType instanceof JavaType.FullyQualified) {
                        return updateOuterClassTypes(
                            TypeTree.build(((JavaType.FullyQualified) targetType).getFullyQualifiedName())
                                    .withPrefix(fieldAccess.getPrefix()), targetType);
                    }
                } else {
                    StringBuilder maybeClass = new StringBuilder();
                    for (Expression target = fieldAccess; target != null; ) {
                        if (target instanceof J.FieldAccess) {
                            J.FieldAccess fa = (J.FieldAccess) target;
                            maybeClass.insert(0, fa.getSimpleName()).insert(0, '.');
                            target = fa.getTarget();
                        } else if (target instanceof J.Identifier) {
                            maybeClass.insert(0, ((J.Identifier) target).getSimpleName());
                            target = null;
                        } else {
                            maybeClass = new StringBuilder("__NOT_IT__");
                            break;
                        }
                    }
                    JavaType.Class oldType = JavaType.ShallowClass.build(originalType.getFullyQualifiedName());
                    if (maybeClass.toString().equals(oldType.getClassName())) {
                        maybeRemoveImport(oldType.getOwningClass());
                        Expression e = updateOuterClassTypes(TypeTree.build(((JavaType.FullyQualified) targetType).getClassName())
                                                                     .withPrefix(fieldAccess.getPrefix()), targetType);
                        // If a FieldAccess like Map.Entry has been replaced with an Identifier, ensure that identifier has the
                        // correct type
                        if (e instanceof J.Identifier && e.getType() == null) {
                            J.Identifier i = (J.Identifier) e;
                            e = i.withType(targetType);
                        }
                        return e;
                    }
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {

            JavaType currentType = ident.getType();
            if (!(currentType instanceof JavaType.FullyQualified)) {
                return visitAndCast(ident, ctx, super::visitIdentifier);
            }

            JavaType.FullyQualified original = TypeUtils.asFullyQualified(currentType);

            if (original != null && TypeUtils.isOfClassType(ident.getType(), original.getFullyQualifiedName())) {
                String fullyQualifiedName = original.getFullyQualifiedName();

                if (isV1Class(original)) {
                    storeV1ClassMetadata(fullyQualifiedName);
                    JavaType.Class originalType = oldTypeToNewType.get(fullyQualifiedName).left();
                    String className = originalType.getClassName();

                    if (ident.getSimpleName().equals(className)) {
                        JavaType targetType = oldTypeToNewType.get(fullyQualifiedName).right();
                        ident = ident.withSimpleName(((JavaType.FullyQualified) targetType).getClassName());
                        ident = ident.withType(updateType(currentType));
                    }
                }
            }

            return visitAndCast(ident, ctx, super::visitIdentifier);
        }

        private void storeV1ClassMetadata(String currentFqcn) {
            JavaType.ShallowClass originalType = JavaType.ShallowClass.build(currentFqcn);
            String v2Equivalent = getV2Equivalent(currentFqcn);

            JavaType targetType = JavaType.buildType(v2Equivalent);

            oldTypeToNewType.put(currentFqcn, Pair.of(originalType, targetType));
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (method.getMethodType() == null) {
                return method;
            }

            JavaType.FullyQualified declaringType = method.getMethodType().getDeclaringType();
            if (isV1Class(declaringType)) {
                String fullyQualifiedName = declaringType.getFullyQualifiedName();
                storeV1ClassMetadata(fullyQualifiedName);

                Pair<JavaType.Class, JavaType> oldTypeToNewTypePair = oldTypeToNewType.get(fullyQualifiedName);
                JavaType.Class originalType = oldTypeToNewTypePair.left();
                JavaType targetType = oldTypeToNewTypePair.right();
                if (method.getMethodType().hasFlags(Flag.Static)) {
                    if (method.getMethodType().getDeclaringType().isAssignableFrom(originalType)) {
                        JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);

                        for (J.Import anImport : cu.getImports()) {
                            if (anImport.isStatic() && anImport.getQualid().getTarget().getType() != null) {
                                JavaType.FullyQualified fqn =
                                    TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType());
                                if (fqn != null && TypeUtils.isOfClassType(fqn, originalType.getFullyQualifiedName()) &&
                                    method.getSimpleName().equals(anImport.getQualid().getSimpleName())) {
                                    JavaType.FullyQualified targetFqn = (JavaType.FullyQualified) targetType;

                                    addImport(targetFqn);
                                    maybeAddImport(targetFqn.getFullyQualifiedName(), method.getName().getSimpleName());
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            return super.visitMethodInvocation(method, ctx);
        }

        private Expression updateOuterClassTypes(Expression typeTree, JavaType targetType) {
            if (!(typeTree instanceof J.FieldAccess)) {
                return typeTree;
            }
            JavaType.FullyQualified type = (JavaType.FullyQualified) targetType;

            if (type.getOwningClass() == null) {
                // just a performance shortcut when this isn't an inner class
                typeTree.withType(updateType(targetType));
            }

            Stack<Expression> typeStack = new Stack<>();
            typeStack.push(typeTree);

            Stack<JavaType.FullyQualified> attrStack = new Stack<>();
            attrStack.push(type);

            for (Expression t = ((J.FieldAccess) typeTree).getTarget(); ; ) {
                typeStack.push(t);
                if (t instanceof J.FieldAccess) {
                    if (Character.isUpperCase(((J.FieldAccess) t).getSimpleName().charAt(0))) {
                        if (attrStack.peek().getOwningClass() != null) {
                            attrStack.push(attrStack.peek().getOwningClass());
                        }
                    }
                    t = ((J.FieldAccess) t).getTarget();
                } else if (t instanceof J.Identifier) {
                    if (Character.isUpperCase(((J.Identifier) t).getSimpleName().charAt(0))) {
                        if (attrStack.peek().getOwningClass() != null) {
                            attrStack.push(attrStack.peek().getOwningClass());
                        }
                    }
                    break;
                }
            }

            Expression attributed = null;
            for (Expression e = typeStack.pop(); ; e = typeStack.pop()) {
                if (e instanceof J.Identifier) {
                    if (attrStack.size() == typeStack.size() + 1) {
                        attributed = ((J.Identifier) e).withType(attrStack.pop());
                    } else {
                        attributed = e;
                    }
                } else if (e instanceof J.FieldAccess) {
                    if (attrStack.size() == typeStack.size() + 1) {
                        attributed = ((J.FieldAccess) e).withTarget(attributed)
                                                        .withType(attrStack.pop());
                    } else {
                        attributed = ((J.FieldAccess) e).withTarget(attributed);
                    }
                }
                if (typeStack.isEmpty()) {
                    break;
                }
            }

            assert attributed != null;
            return attributed;
        }

        private JavaType updateType(JavaType currentType) {
            JavaType type = oldNameToChangedType.get(currentType);
            if (type != null) {
                return type;
            }

            if (currentType instanceof JavaType.FullyQualified) {
                return updateFullyQualifiedType(currentType);
            }

            if (currentType instanceof JavaType.Variable) {
                return updateVariableType(currentType);
            }

            if (currentType instanceof JavaType.Array) {
                return updateArrayType(currentType);
            }

            return currentType;
        }

        private JavaType.Array updateArrayType(JavaType currentType) {
            JavaType.Array array = (JavaType.Array) currentType;
            array = array.withElemType(updateType(array.getElemType()));
            oldNameToChangedType.put(currentType, array);
            oldNameToChangedType.put(array, array);
            return array;
        }

        private JavaType.Variable updateVariableType(JavaType currentType) {
            JavaType.Variable variable = (JavaType.Variable) currentType;
            variable = variable.withOwner(updateType(variable.getOwner()));
            variable = variable.withType(updateType(variable.getType()));
            oldNameToChangedType.put(currentType, variable);
            oldNameToChangedType.put(variable, variable);
            return variable;
        }

        private JavaType updateFullyQualifiedType(JavaType currentType) {
            JavaType.FullyQualified original = TypeUtils.asFullyQualified(currentType);
            if (original == null) {
                return currentType;
            }

            String fullyQualifiedName = original.getFullyQualifiedName();
            if (!oldTypeToNewType.keySet().contains(fullyQualifiedName)) {
                return currentType;
            }

            Pair<JavaType.Class, JavaType> oldToNewPair = oldTypeToNewType.get(fullyQualifiedName);
            JavaType targetType = oldToNewPair.right();

            if (original.getKind() == JavaType.FullyQualified.Kind.Enum) {
                JavaType.FullyQualified targetTypeEnum = TypeUtils.asFullyQualified(targetType);
                if (targetTypeEnum != null) {
                    JavaType enumType = JavaType.ShallowClass.build(targetTypeEnum.getFullyQualifiedName())
                                                             .withKind(JavaType.FullyQualified.Kind.Enum);
                    oldNameToChangedType.put(currentType, enumType);
                    return enumType;
                }
            }

            oldNameToChangedType.put(currentType, targetType);
            return targetType;
        }

        private JavaType.Method updateType(JavaType.Method oldMethodType) {
            if (oldMethodType != null) {
                JavaType.Method method = (JavaType.Method) oldNameToChangedType.get(oldMethodType);
                if (method != null) {
                    return method;
                }

                method = oldMethodType;
                method = method.withDeclaringType((JavaType.FullyQualified) updateType(method.getDeclaringType()))
                               .withReturnType(updateType(method.getReturnType()))
                               .withParameterTypes(ListUtils.map(method.getParameterTypes(), this::updateType));
                oldNameToChangedType.put(oldMethodType, method);
                oldNameToChangedType.put(method, method);
                return method;
            }
            return null;
        }
    }

    private static JavaType.FullyQualified getTopLevelClassName(JavaType.FullyQualified classType) {
        while (true) {
            if (classType.getOwningClass() == null) {
                return classType;
            }
            classType = classType.getOwningClass();
        }
    }
}
