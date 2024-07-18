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

import java.util.List;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.rules.endpoints.BuiltInParameter;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

/**
 * Visitor that rewrites some expressions in preparation for codegen and also renaming locals assignments to use idiomatic java
 * names. This visitor in particular rewrites variable references to the equivalent to {@code getAttr(params, NAME)} or {@code
 * getAttr(locals, NAME)}, depending on whether the reference is an endpoint params variable or a locally assigned one.
 */
public final class PrepareForCodegenVisitor extends RewriteRuleExpressionVisitor {
    private final SymbolTable symbolTable;
    private final SymbolTable.Builder renames;

    public PrepareForCodegenVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.renames = SymbolTable.builder();
    }

    public SymbolTable symbolTable() {
        return renames.build();
    }

    @Override
    public RuleExpression visitBooleanNotExpression(BooleanNotExpression e) {
        e = (BooleanNotExpression) super.visitBooleanNotExpression(e);
        RuleExpression arg = e.expression();
        if (arg instanceof FunctionCallExpression) {
            FunctionCallExpression functionCall = (FunctionCallExpression) arg;
            if ("isSet".equals(functionCall.name())) {
                return functionCall.toBuilder()
                                   .name("isNotSet")
                                   .build();
            }
        }
        return e;
    }

    @Override
    public RuleExpression visitFunctionCallExpression(FunctionCallExpression e) {
        e = (FunctionCallExpression) super.visitFunctionCallExpression(e);
        String fn = e.name();
        switch (fn) {
            case "booleanEquals":
                return simplifyBooleanEquals(e);
            case "stringEquals":
                return simplifyStringEquals(e);
            case "not":
                return simplifyNotExpression(e);
            default:
                return e;
        }
    }

    @Override
    public RuleExpression visitVariableReferenceExpression(VariableReferenceExpression e) {
        String name = e.variableName();
        if (symbolTable.isLocal(name)) {
            RuleType type = symbolTable.localType(name);
            String newName = javaName(name);
            renames.putLocal(newName, type);
            return MemberAccessExpression
                .builder()
                .type(e.type())
                .source(VariableReferenceExpression.builder().variableName("locals").build())
                .name(newName)
                .build();
        }
        if (symbolTable.isParam(name)) {
            RuleType type = symbolTable.paramType(name);
            BuiltInParameter builtInParamType = symbolTable.builtInParamType(name);

            String newName = javaName(name);
            renames.putParam(newName, type, builtInParamType);
            return MemberAccessExpression
                .builder()
                .type(e.type())
                .source(VariableReferenceExpression.builder().variableName("params").build())
                .name(newName)
                .build();
        }
        return e;
    }

    @Override
    public RuleExpression visitIndexedAccessExpression(IndexedAccessExpression e) {
        e = (IndexedAccessExpression) super.visitIndexedAccessExpression(e);
        return FunctionCallExpression
            .builder()
            .name("listAccess")
            .type(e.type())
            .addArgument(e.source())
            .addArgument(new LiteralIntegerExpression(e.index()))
            .build();
    }

    @Override
    public RuleExpression visitLetExpression(LetExpression e) {
        LetExpression.Builder builder = LetExpression.builder();
        e.bindings().forEach((k, v) -> {
            String newName = javaName(k);
            RuleExpression value = v.accept(this);
            builder.putBinding(newName, value);
            renames.putLocal(newName, value.type());
        });
        return builder.build();
    }

    /**
     * Transforms the following expressions:
     * <ul>
     *     <li>{@code booleanEquals(left, TRUE)} transforms to {@code left}</li>
     *     <li>{@code booleanEquals(TRUE, right)} transforms to {@code right}</li>
     *     <li>{@code booleanEquals(left, FALSE)} transforms to {@code (not left)}</li>
     *     <li>{@code booleanEquals(FALSE, right)} transforms to {@code (not right)}</li>
     * </ul>
     */
    private RuleExpression simplifyBooleanEquals(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        RuleExpression left = args.get(0).accept(this);
        RuleExpression right = args.get(1).accept(this);
        if (left.kind() == RuleExpression.RuleExpressionKind.BOOLEAN_VALUE) {
            LiteralBooleanExpression leftAsBoolean = (LiteralBooleanExpression) left;
            if (leftAsBoolean.value()) {
                return right;
            }
            return BooleanNotExpression
                .builder()
                .expression(right)
                .build();
        }
        if (right.kind() == RuleExpression.RuleExpressionKind.BOOLEAN_VALUE) {
            LiteralBooleanExpression rightAsBoolean = (LiteralBooleanExpression) right;
            if (rightAsBoolean.value()) {
                return left;
            }
            return BooleanNotExpression
                .builder()
                .expression(left)
                .build();
        }
        return MethodCallExpression.builder()
                                   .name("equals")
                                   .source(left)
                                   .addArgument(right)
                                   .build();
    }

    /**
     * Transforms the following expression
     * <ul>
     *     <li>{@code stringEquals(left, right)} to {@code left.equals(right)} when left is a String constant</li>
     *     <li>{@code stringEquals(left, right)} to {@code right.equals(left)} when right is a String constant</li>
     * </ul>
     */
    private RuleExpression simplifyStringEquals(FunctionCallExpression e) {
        List<RuleExpression> args = e.arguments();
        RuleExpression left = args.get(0).accept(this);
        RuleExpression right = args.get(1).accept(this);
        if (right.kind() == RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return MethodCallExpression.builder()
                                       .name("equals")
                                       .source(right)
                                       .addArgument(left)
                                       .build();

        }
        if (left.kind() == RuleExpression.RuleExpressionKind.STRING_VALUE) {
            return MethodCallExpression.builder()
                                       .name("equals")
                                       .source(left)
                                       .addArgument(right)
                                       .build();
        }
        return e;
    }

    /**
     * Transforms the following expression
     * <ul>
     *     <li>{@code not(isSet(getAttr(source, name)))} to {@code isNotSet(getAttr(source, name))} which can be later
     *     transformed into {@code getAttr(source, name) == null}</li>
     * </ul>
     */
    private RuleExpression simplifyNotExpression(FunctionCallExpression e) {
        RuleExpression arg = e.arguments().get(0);
        if (arg instanceof FunctionCallExpression) {
            FunctionCallExpression inner = (FunctionCallExpression) arg;
            if ("isSet".equals(inner.name())) {
                return inner.toBuilder()
                            .name("isNotSet")
                            .build();
            }
        }
        return e;
    }

    private String javaName(String name) {
        return Utils.unCapitalize(CodegenNamingUtils.pascalCase(name));
    }
}
