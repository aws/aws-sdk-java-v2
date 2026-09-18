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
import java.util.Map;
import software.amazon.awssdk.codegen.internal.Utils;
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
 * Expressions outside the subset throw {@link UnsupportedLoweringException} before code is emitted.
 */
final class JmesPathTypedGetterGenerator {

    /**
     * Signals that an expression cannot be lowered and the caller should fall back to the reflective runtime. A
     * dedicated type so that the fallback catch cannot mask an unexpected exception from a genuine codegen bug.
     */
    static final class UnsupportedLoweringException extends RuntimeException {
        UnsupportedLoweringException(String message) {
            super(message);
        }
    }

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
            throw new UnsupportedLoweringException("Unsupported endpoint parameter type for " + setterName);
        }

        List<Step> steps = new StepCollector().collect(JmesPathParser.parse(jmesPath));
        boolean listTerminal = "stringarray".equals(normalizedTerminalType);

        if (steps.size() == 1 && steps.get(0) instanceof KeysStep) {
            if (!listTerminal) {
                throw new UnsupportedLoweringException("keys() result can only bind a stringarray parameter");
            }
            List<Step> argument = ((KeysStep) steps.get(0)).arg;
            validateKeys(inputShape, argument);
            return emitKeys(inputShape, argument, setterName, names);
        }
        if (steps.stream().anyMatch(s -> s instanceof KeysStep)) {
            throw new UnsupportedLoweringException("keys() is only supported as the top-level expression");
        }

        int projectionIndex = indexOfProjection(steps);
        if (projectionIndex < 0) {
            if (listTerminal) {
                throw new UnsupportedLoweringException("List parameter requires a projection or keys()");
            }
            List<String> fields = fieldNames(steps);
            MemberModel leaf = resolvePath(inputShape, fields);
            validateScalarTerminal(leaf, normalizedTerminalType);
            return emitScalarPath(inputShape, fields, setterName, names);
        }

        if (!listTerminal) {
            throw new UnsupportedLoweringException("Projection result can only bind a stringarray parameter");
        }
        ProjectionParts parts = projectionParts(steps, projectionIndex);
        validateProjection(inputShape, parts);
        return emitProjection(inputShape, parts, setterName, names);
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

        TypeName resultType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        String resultVar = names.newName(setterName);
        // If the prefix is guarded, the loop may never run: seed the immutable empty list that the reflective
        // runtime's stringValues() returns for a null prefix.
        boolean prefixGuarded = fields.size() > 1;
        if (prefixGuarded) {
            b.addStatement("$T $N = $T.emptyList()", resultType, resultVar, ClassName.get(Collections.class));
        }

        Walk walk = walkToLast(b, names, inputShape, fields);
        String mapVar = names.newName(baseName(walk.lastMember));
        b.addStatement("$T $N = $L", typeProvider.returnType(walk.lastMember), mapVar,
                       accessLast(walk, walk.lastMember));
        if (prefixGuarded) {
            b.addStatement("$N = new $T<>($N.size())", resultVar, ClassName.get(ArrayList.class), mapVar);
        } else {
            b.addStatement("$T $N = new $T<>($N.size())", resultType, resultVar, ClassName.get(ArrayList.class), mapVar);
        }
        String keyVar = names.newName("key");
        b.beginControlFlow("for ($T $N : new $T<>($N).keySet())", String.class, keyVar, HashMap.class, mapVar);
        b.beginControlFlow("if ($N != null)", keyVar);
        b.addStatement("$N.add($N)", resultVar, keyVar);
        b.endControlFlow();
        b.endControlFlow();
        for (int i = 0; i < walk.openedGuards; i++) {
            b.endControlFlow();
        }
        b.addStatement("params.$N($N)", setterName, resultVar);
        return b.build();
    }

    private CodeBlock emitScalarPath(ShapeModel inputShape, List<String> fields, String setterName,
                                     NameAllocator names) {
        CodeBlock.Builder b = CodeBlock.builder();
        CodeBlock value = scalarChain(b, names, inputShape, fields);
        b.addStatement("params.$N($L)", setterName, value);
        return b.build();
    }

    private CodeBlock emitProjection(ShapeModel inputShape, ProjectionParts parts,
                                     String setterName, NameAllocator names) {
        CodeBlock.Builder b = CodeBlock.builder();
        TypeName listElementType = ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class));
        String resultVar = names.newName(setterName);
        // walkToLast guards every prefix field but the last, so a single-field prefix always reaches the loop. If the
        // prefix is guarded, seed the immutable empty list that the reflective runtime's stringValues() returns for a
        // null prefix.
        boolean prefixGuarded = parts.prefix.size() > 1;
        if (prefixGuarded) {
            b.addStatement("$T $N = $T.emptyList()", listElementType, resultVar, ClassName.get(Collections.class));
        }

        Walk walk = walkToLast(b, names, inputShape, parts.prefix);
        MemberModel listMember = walk.lastMember;
        String listVar = names.newName(baseName(listMember));
        b.addStatement("$T $N = $L", typeProvider.returnType(listMember), listVar, accessLast(walk, listMember));
        // The source size is exact for a single-leaf projection and a lower bound for a multiselect.
        if (prefixGuarded) {
            b.addStatement("$N = new $T<>($N.size())", resultVar, ClassName.get(ArrayList.class), listVar);
        } else {
            b.addStatement("$T $N = new $T<>($N.size())", listElementType, resultVar, ClassName.get(ArrayList.class),
                           listVar);
        }
        MemberModel elementMember = listMember.getListModel().getListMemberModel();
        ShapeModel elementShape = targetShape(elementMember);
        TypeName elementType = typeProvider.returnType(elementMember);
        String elementVar = names.newName(elementBaseName(elementMember));

        b.beginControlFlow("for ($T $N : $N)", elementType, elementVar, listVar);
        b.beginControlFlow("if ($N != null)", elementVar);
        if (parts.isMultiSelect()) {
            for (List<Step> branch : parts.multiSelect().branches) {
                emitLeafAppend(b, names, elementVar, elementShape, fieldNames(branch), resultVar);
            }
        } else {
            emitLeafAppend(b, names, elementVar, elementShape, fieldNames(parts.rest), resultVar);
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
                throw new UnsupportedLoweringException("Field path traverses a non-structure member: " + fields.get(i));
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
            throw new UnsupportedLoweringException("keys() argument must resolve to a map");
        }
        requireType(mapMember.getMapModel().getKeyModel(), STRING_TYPE, "keys() map key");
    }

    private void validateProjection(ShapeModel inputShape, ProjectionParts parts) {
        MemberModel listMember = resolvePath(inputShape, parts.prefix);
        if (!listMember.isList()) {
            throw new UnsupportedLoweringException("Projection [*] must apply to a list member");
        }
        if (parts.rest.isEmpty()) {
            throw new UnsupportedLoweringException("Projection must select a scalar field");
        }

        MemberModel elementMember = listMember.getListModel().getListMemberModel();
        ShapeModel elementShape = targetShape(elementMember);
        if (parts.isMultiSelect()) {
            if (!parts.flatten) {
                throw new UnsupportedLoweringException("multi-select-list in a projection must be followed by []");
            }
            for (List<Step> branch : parts.multiSelect().branches) {
                MemberModel leaf = resolvePath(elementShape, fieldNames(branch));
                requireType(leaf, STRING_TYPE, "multi-select-list leaf");
            }
            return;
        }
        if (parts.flatten) {
            throw new UnsupportedLoweringException("Flatten is only supported after a multi-select-list");
        }
        MemberModel leaf = resolvePath(elementShape, fieldNames(parts.rest));
        requireType(leaf, STRING_TYPE, "projection leaf");
    }

    private void validateScalarTerminal(MemberModel leaf, String terminalType) {
        if ("string".equals(terminalType)) {
            requireType(leaf, STRING_TYPE, "string terminal");
        } else if ("boolean".equals(terminalType)) {
            requireType(leaf, BOOLEAN_TYPE, "boolean terminal");
        } else {
            throw new UnsupportedLoweringException("Unsupported scalar endpoint parameter type: " + terminalType);
        }
    }

    private void requireType(MemberModel member, TypeName expected, String description) {
        if (!expected.equals(typeProvider.returnType(member))) {
            throw new UnsupportedLoweringException(description + " has incompatible type");
        }
    }

    private MemberModel resolvePath(ShapeModel startShape, List<String> fields) {
        if (fields.isEmpty()) {
            throw new UnsupportedLoweringException("Field path must not be empty");
        }
        ShapeModel current = startShape;
        MemberModel member = null;
        for (int i = 0; i < fields.size(); i++) {
            member = resolve(current, fields.get(i));
            if (hasRuntimeDefault(member)) {
                throw new UnsupportedLoweringException("Field uses an SdkField runtime default: " + fields.get(i));
            }
            if (i < fields.size() - 1) {
                current = targetShape(member);
                if (current == null) {
                    throw new UnsupportedLoweringException("Field path traverses a non-structure member: " + fields.get(i));
                }
            }
        }
        return member;
    }

    private boolean hasRuntimeDefault(MemberModel member) {
        if (member.isIdempotencyToken()) {
            return true;
        }
        if (model.getCustomizationConfig() == null) {
            return false;
        }
        Map<String, String> defaults = model.getCustomizationConfig().getModelMarshallerDefaultValueSupplier();
        return defaults != null && defaults.containsKey(member.getC2jName());
    }

    /**
     * Walk a scalar field path, emitting a null-guarded local per intermediate hop as a ternary (never an {@code if} block, so
     * the setter is still invoked with {@code null} when the path breaks, matching the reflective runtime). Returns a null-safe
     * expression yielding the final member's value.
     */
    private CodeBlock scalarChain(CodeBlock.Builder b, NameAllocator names, ShapeModel inputShape, List<String> fields) {
        ShapeModel current = inputShape;
        String prevVar = null;
        for (int i = 0; i < fields.size() - 1; i++) {
            MemberModel m = resolve(current, fields.get(i));
            if (targetShape(m) == null) {
                throw new UnsupportedLoweringException("Field path traverses a non-structure member: " + fields.get(i));
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
        if (prevVar == null) {
            return CodeBlock.of("request.$N()", last.getFluentGetterMethodName());
        }
        return CodeBlock.of("$N == null ? null : $N.$N()", prevVar, prevVar, last.getFluentGetterMethodName());
    }

    /**
     * Result of walking a field path: the resolved terminal member, the code expression for its parent object, and how many
     * guard control-flow blocks were opened (which the caller must close).
     */
    private static final class Walk {
        private final MemberModel lastMember;
        private final CodeBlock parentExpr;
        private final int openedGuards;

        private Walk(MemberModel lastMember, CodeBlock parentExpr, int openedGuards) {
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
        CodeBlock parentExpr = CodeBlock.of("request");
        int opened = 0;
        for (int i = 0; i < fields.size() - 1; i++) {
            MemberModel m = resolve(current, fields.get(i));
            if (targetShape(m) == null) {
                throw new UnsupportedLoweringException("Field path traverses a non-structure member: " + fields.get(i));
            }
            String var = names.newName(baseName(m));
            b.addStatement("$T $N = $L.$N()", typeProvider.returnType(m), var, parentExpr, m.getFluentGetterMethodName());
            b.beginControlFlow("if ($N != null)", var);
            opened++;
            parentExpr = CodeBlock.of("$N", var);
            current = targetShape(m);
        }
        MemberModel last = resolve(current, fields.get(fields.size() - 1));
        return new Walk(last, parentExpr, opened);
    }

    /**
     * Expression reading the walked path's final member from its parent. Safe to consume without a null guard only
     * for collection members: generated list and map getters return auto-construct empties, never null.
     */
    private CodeBlock accessLast(Walk walk, MemberModel lastMember) {
        return CodeBlock.of("$L.$N()", walk.parentExpr, lastMember.getFluentGetterMethodName());
    }

    private MemberModel resolve(ShapeModel shape, String c2jName) {
        if (shape == null) {
            throw new UnsupportedLoweringException("Cannot resolve '" + c2jName + "' against a non-structure");
        }
        MemberModel member = shape.getMemberByC2jName(c2jName);
        if (member == null) {
            throw new UnsupportedLoweringException("No member '" + c2jName + "' on shape " + shape.getShapeName());
        }
        return member;
    }

    private String baseName(MemberModel m) {
        return m.getVariable().getVariableName();
    }

    private String elementBaseName(MemberModel elementMember) {
        ShapeModel shape = targetShape(elementMember);
        if (shape != null && shape.getShapeName() != null) {
            return Utils.unCapitalize(shape.getShapeName());
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
                throw new UnsupportedLoweringException("Expected a plain field path");
            }
            names.add(((FieldStep) s).c2jName);
        }
        return names;
    }

    /**
     * The structural decomposition of a projection expression, computed once so that validation and emission cannot
     * disagree about the shape they are processing.
     */
    private static final class ProjectionParts {
        /** Field path before the projection. */
        private final List<String> prefix;
        /** Steps applied to each projected element, with a trailing flatten already removed. */
        private final List<Step> rest;
        private final boolean flatten;

        private ProjectionParts(List<String> prefix, List<Step> rest, boolean flatten) {
            this.prefix = prefix;
            this.rest = rest;
            this.flatten = flatten;
        }

        private boolean isMultiSelect() {
            return rest.size() == 1 && rest.get(0) instanceof MultiSelectStep;
        }

        private MultiSelectStep multiSelect() {
            return (MultiSelectStep) rest.get(0);
        }
    }

    private static ProjectionParts projectionParts(List<Step> steps, int projectionIndex) {
        List<String> prefix = fieldNames(steps.subList(0, projectionIndex));
        List<Step> rest = new ArrayList<>(steps.subList(projectionIndex + 1, steps.size()));
        boolean flatten = !rest.isEmpty() && rest.get(rest.size() - 1) instanceof FlattenStep;
        if (flatten) {
            rest.remove(rest.size() - 1);
        }
        return new ProjectionParts(prefix, rest, flatten);
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
     * Flattens the parsed JMESPath AST into a linear {@link Step} list. Any node type outside the supported subset
     * throws {@link UnsupportedLoweringException}.
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
                throw new UnsupportedLoweringException("Unsupported bracket specifier");
            }
        }

        @Override
        public void visitBracketSpecifierWithoutContents(BracketSpecifierWithoutContents input) {
            steps.add(new FlattenStep());
        }

        @Override
        public void visitBracketSpecifierWithQuestionMark(BracketSpecifierWithQuestionMark input) {
            throw new UnsupportedLoweringException("Filter expressions are not supported");
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
                throw new UnsupportedLoweringException("Unsupported function: " + input.function());
            }
            List<FunctionArg> args = input.functionArgs();
            if (args.size() != 1 || !args.get(0).isExpression()) {
                throw new UnsupportedLoweringException("keys() requires a single expression argument");
            }
            steps.add(new KeysStep(new StepCollector().collect(args.get(0).asExpression())));
        }

        @Override
        public void visitIdentifier(String input) {
            steps.add(new FieldStep(input));
        }

        @Override
        public void visitSliceExpression(SliceExpression input) {
            throw new UnsupportedLoweringException("slice expression is not supported");
        }

        @Override
        public void visitComparatorExpression(ComparatorExpression input) {
            throw new UnsupportedLoweringException("comparator expression is not supported");
        }

        @Override
        public void visitOrExpression(OrExpression input) {
            throw new UnsupportedLoweringException("or expression is not supported");
        }

        @Override
        public void visitAndExpression(AndExpression input) {
            throw new UnsupportedLoweringException("and expression is not supported");
        }

        @Override
        public void visitNotExpression(NotExpression input) {
            throw new UnsupportedLoweringException("not expression is not supported");
        }

        @Override
        public void visitParenExpression(ParenExpression input) {
            throw new UnsupportedLoweringException("paren expression is not supported");
        }

        @Override
        public void visitMultiSelectHash(MultiSelectHash input) {
            throw new UnsupportedLoweringException("multi select hash is not supported");
        }

        @Override
        public void visitExpressionType(ExpressionType input) {
            throw new UnsupportedLoweringException("expression type is not supported");
        }

        @Override
        public void visitPipeExpression(PipeExpression input) {
            throw new UnsupportedLoweringException("pipe expression is not supported");
        }

        @Override
        public void visitCurrentNode(CurrentNode input) {
            throw new UnsupportedLoweringException("current node is not supported");
        }

        @Override
        public void visitRawString(String input) {
            throw new UnsupportedLoweringException("raw string is not supported");
        }

        @Override
        public void visitLiteral(Literal input) {
            throw new UnsupportedLoweringException("literal is not supported");
        }

        @Override
        public void visitNumber(int input) {
            throw new UnsupportedLoweringException("number is not supported");
        }
    }
}
