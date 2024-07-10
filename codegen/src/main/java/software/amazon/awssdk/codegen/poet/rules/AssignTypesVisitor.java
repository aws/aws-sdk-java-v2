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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.utils.ToString;

/**
 * Assigns types to each expression and validates that the assigned types match the expected ones.
 */
public final class AssignTypesVisitor extends RewriteRuleExpressionVisitor {
    private final RuleRuntimeTypeMirror typeMirror;
    private final SymbolTable.Builder tableBuilder;
    private final List<String> errors;

    public AssignTypesVisitor(RuleRuntimeTypeMirror typeMirror, SymbolTable table) {
        this.typeMirror = typeMirror;
        this.tableBuilder = table.toBuilder();
        this.errors = new ArrayList<>();
    }

    public Map<String, RuleType> params() {
        return tableBuilder.build().params();
    }

    public SymbolTable symbolTable() {
        return tableBuilder.build();
    }

    @Override
    public String toString() {
        return ToString.builder("AssignTypesVisitor")
                       .add("typeMirror", typeMirror)
                       .add("tableBuilder", tableBuilder)
                       .add("errors", errors)
                       .build();
    }

    public List<String> errors() {
        return errors;
    }

    @Override
    public RuleExpression visitVariableReferenceExpression(VariableReferenceExpression e) {
        String variableName = e.variableName();
        RuleType type = tableBuilder.local(variableName);
        if (type == null) {
            type = tableBuilder.param(variableName);
        }
        if (type == null) {
            errors.add(String.format("Undefined variable `%s`", variableName));
            return e;
        }
        return e.toBuilder()
                .type(type)
                .build();
    }

    @Override
    public RuleExpression visitFunctionCallExpression(FunctionCallExpression e) {
        FunctionCallExpression expr = (FunctionCallExpression) super.visitFunctionCallExpression(e);
        String name = expr.name();
        RuleFunctionMirror function = typeMirror.resolveFunction(name);
        if (function == null) {
            if ("isSet".equals(name)) {
                if (expr.arguments().size() != 1) {
                    addError("Function `isSet` expects one argument, got `%s`", expr.arguments());
                } else {
                    expr = expr.toBuilder().type(RuleRuntimeTypeMirror.BOOLEAN).build();
                }
            } else {
                addError("Function `%s` not found", name);
            }
            return expr;
        }

        List<RuleType> args = expr.arguments().stream().map(RuleExpression::type).collect(Collectors.toList());
        if (!function.matches(args)) {
            addError("Arguments for function `%s` doesn't match, expected: `%s`, got: `%s`",
                     name,
                     function.arguments().values(), args);
        }
        return expr.toBuilder().type(function.returns()).build();
    }

    @Override
    public RuleExpression visitBooleanAndExpression(BooleanAndExpression e) {
        e = (BooleanAndExpression) super.visitBooleanAndExpression(e);
        BooleanAndExpression.Builder builder = BooleanAndExpression.builder();
        for (RuleExpression child : e.expressions()) {
            RuleType childType = child.type();
            if (RuleRuntimeTypeMirror.BOOLEAN.equals(childType)) {
                builder.addExpression(child);
            } else if (childType == null) {
                addError("Type for expression `%s` is undefined", child);
            } else {
                // If the inner expression is not boolean then we treat the expression as "isSet", i.e.,
                // whether the values is not null.
                builder.addExpression(FunctionCallExpression.builder()
                                                            .type(RuleRuntimeTypeMirror.BOOLEAN)
                                                            .name("isSet")
                                                            .addArgument(child)
                                                            .build());
            }
        }
        return builder.build();
    }


    @Override
    public RuleExpression visitLetExpression(LetExpression e) {
        LetExpression expr = (LetExpression) super.visitLetExpression(e);
        expr.bindings().forEach((k, v) -> {
            RuleType type = v.type();
            if (type == null) {
                addError("Cannot find type for variable `%s`, expression: `%s`", k, v);
            } else {
                putLocal(k, type);
            }
        });
        return expr;
    }

    @Override
    public RuleExpression visitIndexedAccessExpression(IndexedAccessExpression e) {
        IndexedAccessExpression expr = (IndexedAccessExpression) super.visitIndexedAccessExpression(e);
        RuleType sourceType = expr.source().type();
        if (sourceType == null) {
            addError("Cannot find type for expression `%s`", expr.source());
        } else if (!sourceType.isList()) {
            addError("Expected list type for indexed access expression, got instead `%s`", sourceType);
        } else {
            RuleType type = sourceType.ruleTypeParam();
            expr = expr.toBuilder().type(type).build();
        }
        return expr;
    }

    @Override
    public RuleExpression visitMemberAccessExpression(MemberAccessExpression e) {
        MemberAccessExpression expr = (MemberAccessExpression) super.visitMemberAccessExpression(e);
        RuleType type = expr.source().type();
        if (type == null) {
            addError("Cannot find type for expression `%s`", expr.source());
        } else {
            String name = expr.name();
            RuleType memberType = type.property(name);
            if (memberType == null) {
                addError("Cannot find a member with name `%s` for type `%s`", name, type);
            } else {
                expr = expr.toBuilder().type(memberType).build();
            }
        }
        return expr;
    }

    private void addError(String fmt, Object... args) {
        this.errors.add(String.format(fmt, args));
    }

    private void putLocal(String k, RuleType type) {
        RuleType prev = tableBuilder.local(k);
        if (prev == null) {
            tableBuilder.putLocal(k, type);
        } else {
            if (!prev.equals(type)) {
                // We have the same name in a different scope assigned to a different type. For now, let's just
                // error out. If we were to support this we would need to rename as we currently assume that each
                // name has a unique type.
                addError("Local variable `%s` changed type, prev `%s`, new `%s`", k, prev, type);
            }
        }
    }
}
