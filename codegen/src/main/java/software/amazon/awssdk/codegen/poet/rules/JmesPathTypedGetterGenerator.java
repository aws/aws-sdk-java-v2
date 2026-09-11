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

package software.amazon.awssdk.codegen.poet.rules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import software.amazon.awssdk.codegen.jmespath.component.AndExpression;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifier;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithContents;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithQuestionMark;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithoutContents;
import software.amazon.awssdk.codegen.jmespath.component.ComparatorExpression;
import software.amazon.awssdk.codegen.jmespath.component.CurrentNode;
import software.amazon.awssdk.codegen.jmespath.component.Expression;
import software.amazon.awssdk.codegen.jmespath.component.ExpressionType;
import software.amazon.awssdk.codegen.jmespath.component.FunctionArg;
import software.amazon.awssdk.codegen.jmespath.component.FunctionExpression;
import software.amazon.awssdk.codegen.jmespath.component.IndexExpression;
import software.amazon.awssdk.codegen.jmespath.component.Literal;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectHash;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectList;
import software.amazon.awssdk.codegen.jmespath.component.NotExpression;
import software.amazon.awssdk.codegen.jmespath.component.OrExpression;
import software.amazon.awssdk.codegen.jmespath.component.ParenExpression;
import software.amazon.awssdk.codegen.jmespath.component.PipeExpression;
import software.amazon.awssdk.codegen.jmespath.component.SliceExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpressionRight;
import software.amazon.awssdk.codegen.jmespath.component.WildcardExpression;
import software.amazon.awssdk.codegen.jmespath.parser.JmesPathParser;
import software.amazon.awssdk.codegen.jmespath.parser.JmesPathVisitor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.model.TypeProvider;

/**
 * Generates direct getter bindings for a validated subset of endpoint operation-context JMESPath expressions.
 * Expressions outside the subset throw {@link UnsupportedOperationException} before code is emitted.
 */
final class JmesPathTypedGetterGenerator {

    private static final TypeName STRING_TYPE = ClassName.get(String.class);
    private static final TypeName BOOLEAN_TYPE = ClassName.get(Boolean.class);

    private final IntermediateModel model;
    private final TypeProvider typeProvider;

    JmesPathTypedGetterGenerator(IntermediateModel model) {
        this.model = model;
        this.typeProvider = new TypeProvider(model);
    }

    NameAllocator newNameAllocator() {
        NameAllocator names = new NameAllocator();
        names.newName("params");
        names.newName("request");
        names.newName("input");
        return names;
    }

    CodeBlock lower(ShapeModel inputShape, String jmesPath, String terminalType, String setterName) {
        return lower(inputShape, jmesPath, terminalType, setterName, newNameAllocator());
    }

    CodeBlock lower(ShapeModel inputShape, String jmesPath, String terminalType, String setterName, NameAllocator names) {
        String normalizedTerminalType = terminalType == null ? "" : terminalType.toLowerCase(Locale.US);
        if (!"string".equals(normalizedTerminalType)
            && !"boolean".equals(normalizedTerminalType)
            && !"stringarray".equals(normalizedTerminalType)) {
            throw new UnsupportedOperationException("Unsupported endpoint parameter type for " + setterName);
        }

        List<Step> steps = new StepCollector().collect(JmesPathParser.parse(jmesPath));
        boolean listTerminal = "stringarray".equals(normalizedTerminalType);

        if (steps.size() == 1 && steps.get(0) instanceof KeysStep) {
            if (!listTerminal) {
                throw new UnsupportedOperationException("keys() result can only bind a stringarray parameter");
            }
            List<Step> argument = ((KeysStep) steps.get(0)).arg;
            validateKeys(inputShape, argument);
            return emitKeys(inputShape, argument, setterName, names);
        }
        if (steps.stream().anyMatch(s -> s instanceof KeysStep)) {
            throw new UnsupportedOperationException("keys() is only supported as the top-level expression");
        }

        int projectionIndex = indexOfProjection(steps);
        if (projectionIndex < 0) {
            if (listTerminal) {
                throw new UnsupportedOperationException("List parameter requires a projection or keys()");
            }
            List<String> fields = fieldNames(steps);
            MemberModel leaf = resolvePath(inputShape, fields);
            validateScalarTerminal(leaf, normalizedTerminalType);
            return emitScalarPath(inputShape, fields, setterName, names);
        }

        if (!listTerminal) {
            throw new UnsupportedOperationException("Projection result can only bind a stringarray parameter");
        }
        validateProjection(inputShape, steps, projectionIndex);
        return emitProjection(inputShape, steps, projectionIndex, setterName, names);
    }

    private ShapeModel targetShape(MemberModel member) {
        if (member.getShape() != null) {
            return member.getShape();
        }
        return model.getShapes().get(member.getC2jShape());
    }

    private CodeBlock emitKeys(ShapeModel inputShape, List<Step> argSteps, String setterName, NameAllocator names) {
        List<String> fields = fieldNames(argSteps);
        CodeBlock.Builder b = CodeBlock.builder();

        ScalarRef ref = scalarChain(b, names, inputShape, fields);
        TypeName mapType = typeProvider.returnType(ref.lastMember);
        String mapVar = names.newName(baseName(ref.lastMember));
        String resultVar = names.newName(setterName);
        TypeName resultType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        b.addStatement("$T $N = $L", mapType, mapVar, ref.finalExpression);
        b.addStatement("$T $N = new $T<>()", resultType, resultVar, ClassName.get(ArrayList.class));
        String keyVar = names.newName("key");
        b.beginControlFlow("if ($N != null)", mapVar);
        b.beginControlFlow("for ($T $N : new $T<>($N).keySet())", String.class, keyVar, HashMap.class, mapVar);
        b.beginControlFlow("if ($N != null)", keyVar);
        b.addStatement("$N.add($N)", resultVar, keyVar);
        b.endControlFlow();
        b.endControlFlow();
        b.endControlFlow();
        b.addStatement("params.$N($N)", setterName, resultVar);
        return b.build();
    }

    private CodeBlock emitScalarPath(ShapeModel inputShape, List<String> fields, String setterName,
                                     NameAllocator names) {
        CodeBlock.Builder b = CodeBlock.builder();
        ScalarRef ref = scalarChain(b, names, inputShape, fields);
        b.addStatement("params.$N($L)", setterName, ref.finalExpression);
        return b.build();
    }

    private CodeBlock emitProjection(ShapeModel inputShape, List<Step> steps, int projectionIndex,
                                     String setterName, NameAllocator names) {
        List<String> prefix = fieldNames(steps.subList(0, projectionIndex));
        List<Step> rest = new ArrayList<>(steps.subList(projectionIndex + 1, steps.size()));
        boolean flatten = !rest.isEmpty() && rest.get(rest.size() - 1) instanceof FlattenStep;
        if (flatten) {
            rest.remove(rest.size() - 1);
        }

        CodeBlock.Builder b = CodeBlock.builder();
        TypeName listElementType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        String resultVar = names.newName(setterName);
        // walkToLast guards every prefix field but the last, so a single-field prefix always reaches the loop. When it
        // can be skipped, seed the immutable empty list that stringValues() returns for a null prefix.
        boolean prefixGuarded = prefix.size() > 1;
        if (prefixGuarded) {
            b.addStatement("$T $N = $T.emptyList()", listElementType, resultVar, ClassName.get(Collections.class));
        } else {
            b.addStatement("$T $N = new $T<>()", listElementType, resultVar, ClassName.get(ArrayList.class));
        }

        Walk walk = walkToLast(b, names, inputShape, prefix);
        if (prefixGuarded) {
            b.addStatement("$N = new $T<>()", resultVar, ClassName.get(ArrayList.class));
        }
        MemberModel listMember = walk.lastMember;
        MemberModel elementMember = listMember.getListModel().getListMemberModel();
        ShapeModel elementShape = targetShape(elementMember);
        TypeName elementType = typeProvider.returnType(elementMember);
        String elementVar = names.newName(elementBaseName(elementMember));

        b.beginControlFlow("for ($T $N : $L)", elementType, elementVar, accessLast(walk, listMember));
        b.beginControlFlow("if ($N != null)", elementVar);
        if (rest.size() == 1 && rest.get(0) instanceof MultiSelectStep) {
            for (List<Step> branch : ((MultiSelectStep) rest.get(0)).branches) {
                emitLeafAppend(b, names, elementVar, elementShape, fieldNames(branch), resultVar);
            }
        } else {
            emitLeafAppend(b, names, elementVar, elementShape, fieldNames(rest), resultVar);
        }
        b.endControlFlow();
        b.endControlFlow();
        for (int i = 0; i < walk.openedGuards; i++) {
            b.endControlFlow();
        }
        b.addStatement("params.$N($N)", setterName, resultVar);
        return b.build();
    }

    /**
     * Emit a null-guarded walk of {@code fields} starting from {@code startVar} (of {@code startShape}), appending the non-null
     * scalar leaf to {@code resultVar}. Opens and closes its own guard blocks (balanced).
     */
    private void emitLeafAppend(CodeBlock.Builder b, NameAllocator names, String startVar, ShapeModel startShape,
                                List<String> fields, String resultVar) {
        ShapeModel current = startShape;
        String parentVar = startVar;
        int opened = 0;
        for (int i = 0; i < fields.size() - 1; i++) {
            MemberModel m = resolve(current, fields.get(i));
            if (targetShape(m) == null) {
                throw new UnsupportedOperationException("Field path traverses a non-structure member: " + fields.get(i));
            }
            String var = names.newName(baseName(m));
            b.addStatement("$T $N = $N.$N()", typeProvider.returnType(m), var, parentVar, m.getFluentGetterMethodName());
            b.beginControlFlow("if ($N != null)", var);
            opened++;
            parentVar = var;
            current = targetShape(m);
        }
        MemberModel leaf = resolve(current, fields.get(fields.size() - 1));
        String leafVar = names.newName(baseName(leaf));
        b.addStatement("$T $N = $N.$N()", typeProvider.returnType(leaf), leafVar, parentVar, leaf.getFluentGetterMethodName());
        b.beginControlFlow("if ($N != null)", leafVar);
        b.addStatement("$N.add($N)", resultVar, leafVar);
        b.endControlFlow();
        for (int i = 0; i < opened; i++) {
            b.endControlFlow();
        }
    }

    private void validateKeys(ShapeModel inputShape, List<Step> argument) {
        MemberModel mapMember = resolvePath(inputShape, fieldNames(argument));
        if (!mapMember.isMap()) {
            throw new UnsupportedOperationException("keys() argument must resolve to a map");
        }
        requireType(mapMember.getMapModel().getKeyModel(), STRING_TYPE, "keys() map key");
    }

    private void validateProjection(ShapeModel inputShape, List<Step> steps, int projectionIndex) {
        List<String> prefix = fieldNames(steps.subList(0, projectionIndex));
        MemberModel listMember = resolvePath(inputShape, prefix);
        if (!listMember.isList()) {
            throw new UnsupportedOperationException("Projection [*] must apply to a list member");
        }

        List<Step> rest = new ArrayList<>(steps.subList(projectionIndex + 1, steps.size()));
        boolean flatten = !rest.isEmpty() && rest.get(rest.size() - 1) instanceof FlattenStep;
        if (flatten) {
            rest.remove(rest.size() - 1);
        }
        if (rest.isEmpty()) {
            throw new UnsupportedOperationException("Projection must select a scalar field");
        }

        MemberModel elementMember = listMember.getListModel().getListMemberModel();
        ShapeModel elementShape = targetShape(elementMember);
        if (rest.size() == 1 && rest.get(0) instanceof MultiSelectStep) {
            if (!flatten) {
                throw new UnsupportedOperationException("multi-select-list in a projection must be followed by []");
            }
            for (List<Step> branch : ((MultiSelectStep) rest.get(0)).branches) {
                MemberModel leaf = resolvePath(elementShape, fieldNames(branch));
                requireType(leaf, STRING_TYPE, "multi-select-list leaf");
            }
            return;
        }
        if (flatten) {
            throw new UnsupportedOperationException("Flatten is only supported after a multi-select-list");
        }
        MemberModel leaf = resolvePath(elementShape, fieldNames(rest));
        requireType(leaf, STRING_TYPE, "projection leaf");
    }

    private void validateScalarTerminal(MemberModel leaf, String terminalType) {
        if ("string".equals(terminalType)) {
            requireType(leaf, STRING_TYPE, "string terminal");
        } else if ("boolean".equals(terminalType)) {
            requireType(leaf, BOOLEAN_TYPE, "boolean terminal");
        } else {
            throw new UnsupportedOperationException("Unsupported scalar endpoint parameter type: " + terminalType);
        }
    }

    private void requireType(MemberModel member, TypeName expected, String description) {
        if (!expected.equals(typeProvider.returnType(member))) {
            throw new UnsupportedOperationException(description + " has incompatible type");
        }
    }

    private MemberModel resolvePath(ShapeModel startShape, List<String> fields) {
        if (fields.isEmpty()) {
            throw new UnsupportedOperationException("Field path must not be empty");
        }
        ShapeModel current = startShape;
        MemberModel member = null;
        for (int i = 0; i < fields.size(); i++) {
            member = resolve(current, fields.get(i));
            if (hasRuntimeDefault(member)) {
                throw new UnsupportedOperationException("Field uses an SdkField runtime default: " + fields.get(i));
            }
            if (i < fields.size() - 1) {
                current = targetShape(member);
                if (current == null) {
                    throw new UnsupportedOperationException("Field path traverses a non-structure member: " + fields.get(i));
                }
            }
        }
        return member;
    }

    private boolean hasRuntimeDefault(MemberModel member) {
        if (member.isIdempotencyToken() || model.getCustomizationConfig() == null) {
            return member.isIdempotencyToken();
        }
        java.util.Map<String, String> defaults =
            model.getCustomizationConfig().getModelMarshallerDefaultValueSupplier();
        return defaults != null && defaults.containsKey(member.getC2jName());
    }

    /**
     * A resolved scalar field path: the terminal member and a null-safe Java expression yielding its value.
     */
    private static final class ScalarRef {
        private final MemberModel lastMember;
        private final String finalExpression;

        private ScalarRef(MemberModel lastMember, String finalExpression) {
            this.lastMember = lastMember;
            this.finalExpression = finalExpression;
        }
    }

    /**
     * Walk a scalar field path, emitting a null-guarded local per intermediate hop as a ternary (never an {@code if} block, so
     * the setter is still invoked with {@code null} when the path breaks, matching the reflective runtime). Returns the final
     * member and a null-safe expression yielding its value.
     */
    private ScalarRef scalarChain(CodeBlock.Builder b, NameAllocator names, ShapeModel inputShape, List<String> fields) {
        ShapeModel current = inputShape;
        String prevVar = null;
        for (int i = 0; i < fields.size() - 1; i++) {
            MemberModel m = resolve(current, fields.get(i));
            if (targetShape(m) == null) {
                throw new UnsupportedOperationException("Field path traverses a non-structure member: " + fields.get(i));
            }
            String var = names.newName(baseName(m));
            if (prevVar == null) {
                b.addStatement("$T $N = request.$N()", typeProvider.returnType(m), var, m.getFluentGetterMethodName());
            } else {
                b.addStatement("$T $N = $N == null ? null : $N.$N()", typeProvider.returnType(m), var, prevVar, prevVar,
                               m.getFluentGetterMethodName());
            }
            prevVar = var;
            current = targetShape(m);
        }
        MemberModel last = resolve(current, fields.get(fields.size() - 1));
        String finalExpression;
        if (prevVar == null) {
            finalExpression = CodeBlock.of("request.$N()", last.getFluentGetterMethodName()).toString();
        } else {
            finalExpression = CodeBlock.of("$N == null ? null : $N.$N()", prevVar, prevVar,
                                           last.getFluentGetterMethodName()).toString();
        }
        return new ScalarRef(last, finalExpression);
    }

    /**
     * Result of walking a field path: the resolved terminal member, the code expression for its parent object, and how many
     * guard control-flow blocks were opened (which the caller must close).
     */
    private static final class Walk {
        private final MemberModel lastMember;
        private final String parentExpr;
        private final int openedGuards;

        private Walk(MemberModel lastMember, String parentExpr, int openedGuards) {
            this.lastMember = lastMember;
            this.parentExpr = parentExpr;
            this.openedGuards = openedGuards;
        }
    }

    /**
     * Walk all-but-last fields as null-guarded locals rooted at {@code request}, returning the parent expression from which the
     * last field is accessed. Guards are opened with {@code beginControlFlow} and must be closed by the caller.
     */
    private Walk walkToLast(CodeBlock.Builder b, NameAllocator names, ShapeModel inputShape, List<String> fields) {
        ShapeModel current = inputShape;
        String parentExpr = "request";
        int opened = 0;
        for (int i = 0; i < fields.size() - 1; i++) {
            MemberModel m = resolve(current, fields.get(i));
            if (targetShape(m) == null) {
                throw new UnsupportedOperationException("Field path traverses a non-structure member: " + fields.get(i));
            }
            String var = names.newName(baseName(m));
            b.addStatement("$T $N = $L.$N()", typeProvider.returnType(m), var, parentExpr, m.getFluentGetterMethodName());
            b.beginControlFlow("if ($N != null)", var);
            opened++;
            parentExpr = var;
            current = targetShape(m);
        }
        MemberModel last = resolve(current, fields.get(fields.size() - 1));
        return new Walk(last, parentExpr, opened);
    }

    private String accessLast(Walk walk, MemberModel lastMember) {
        return CodeBlock.of("$L.$N()", walk.parentExpr, lastMember.getFluentGetterMethodName()).toString();
    }

    private MemberModel resolve(ShapeModel shape, String c2jName) {
        if (shape == null) {
            throw new UnsupportedOperationException("Cannot resolve '" + c2jName + "' against a non-structure");
        }
        MemberModel member = shape.getMemberByC2jName(c2jName);
        if (member == null) {
            throw new UnsupportedOperationException("No member '" + c2jName + "' on shape " + shape.getShapeName());
        }
        return member;
    }

    private String baseName(MemberModel m) {
        return m.getVariable().getVariableName();
    }

    private String elementBaseName(MemberModel elementMember) {
        ShapeModel shape = targetShape(elementMember);
        if (shape != null && shape.getShapeName() != null) {
            String name = shape.getShapeName();
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return "item";
    }

    private static int indexOfProjection(List<Step> steps) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i) instanceof ProjectionStep) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> fieldNames(List<Step> steps) {
        List<String> names = new ArrayList<>();
        for (Step s : steps) {
            if (!(s instanceof FieldStep)) {
                throw new UnsupportedOperationException("Expected a plain field path");
            }
            names.add(((FieldStep) s).c2jName);
        }
        return names;
    }

    private interface Step {
    }

    private static final class FieldStep implements Step {
        private final String c2jName;

        private FieldStep(String c2jName) {
            this.c2jName = c2jName;
        }
    }

    private static final class ProjectionStep implements Step {
    }

    private static final class FlattenStep implements Step {
    }

    private static final class MultiSelectStep implements Step {
        private final List<List<Step>> branches;

        private MultiSelectStep(List<List<Step>> branches) {
            this.branches = branches;
        }
    }

    private static final class KeysStep implements Step {
        private final List<Step> arg;

        private KeysStep(List<Step> arg) {
            this.arg = arg;
        }
    }

    /**
     * Flattens the parsed JMESPath AST into a linear {@link Step} list. Any node type outside the supported subset throws
     * {@link UnsupportedOperationException}, which propagates to {@link #lower} and triggers the caller's reflective fallback.
     */
    private static final class StepCollector implements JmesPathVisitor {
        private final List<Step> steps = new ArrayList<>();

        private List<Step> collect(Expression expression) {
            expression.visit(this);
            return steps;
        }

        @Override
        public void visitExpression(Expression input) {
            input.visit(this);
        }

        @Override
        public void visitSubExpression(SubExpression input) {
            input.leftExpression().visit(this);
            visitSubExpressionRight(input.rightSubExpression());
        }

        @Override
        public void visitSubExpressionRight(SubExpressionRight input) {
            input.visit(this);
        }

        @Override
        public void visitIndexExpression(IndexExpression input) {
            input.expression().ifPresent(e -> e.visit(this));
            input.bracketSpecifier().visit(this);
        }

        @Override
        public void visitBracketSpecifier(BracketSpecifier input) {
            input.visit(this);
        }

        @Override
        public void visitBracketSpecifierWithContents(BracketSpecifierWithContents input) {
            if (input.isWildcardExpression()) {
                steps.add(new ProjectionStep());
            } else if (input.isMultiSelectList()) {
                visitMultiSelectList(input.asMultiSelectList());
            } else {
                throw new UnsupportedOperationException("Unsupported bracket specifier");
            }
        }

        @Override
        public void visitBracketSpecifierWithoutContents(BracketSpecifierWithoutContents input) {
            steps.add(new FlattenStep());
        }

        @Override
        public void visitBracketSpecifierWithQuestionMark(BracketSpecifierWithQuestionMark input) {
            throw new UnsupportedOperationException("Filter expressions are not supported");
        }

        @Override
        public void visitWildcardExpression(WildcardExpression input) {
            steps.add(new ProjectionStep());
        }

        @Override
        public void visitMultiSelectList(MultiSelectList input) {
            List<List<Step>> branches = new ArrayList<>();
            for (Expression expression : input.expressions()) {
                branches.add(new StepCollector().collect(expression));
            }
            steps.add(new MultiSelectStep(branches));
        }

        @Override
        public void visitFunctionExpression(FunctionExpression input) {
            if (!"keys".equals(input.function())) {
                throw new UnsupportedOperationException("Unsupported function: " + input.function());
            }
            List<FunctionArg> args = input.functionArgs();
            if (args.size() != 1 || !args.get(0).isExpression()) {
                throw new UnsupportedOperationException("keys() requires a single expression argument");
            }
            steps.add(new KeysStep(new StepCollector().collect(args.get(0).asExpression())));
        }

        @Override
        public void visitIdentifier(String input) {
            steps.add(new FieldStep(input));
        }

        @Override
        public void visitSliceExpression(SliceExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitComparatorExpression(ComparatorExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitOrExpression(OrExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitAndExpression(AndExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitNotExpression(NotExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitParenExpression(ParenExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitMultiSelectHash(MultiSelectHash input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitExpressionType(ExpressionType input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitPipeExpression(PipeExpression input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitCurrentNode(CurrentNode input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitRawString(String input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitLiteral(Literal input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visitNumber(int input) {
            throw new UnsupportedOperationException();
        }
    }
}
