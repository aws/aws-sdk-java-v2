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

package software.amazon.awssdk.codegen.poet.rules2;

import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

/**
 * Visitor that renames locals assignments to use idiomatic java names. This visitor also rewrites variable references to
 * the equivalent to {@code getAttr(params, NAME)}, to call the getter method in the params.
 */
public final class RenameForCodegenVisitor extends RewriteRuleExpressionVisitor {
    private final SymbolTable symbolTable;
    private final SymbolTable.Builder renames;

    public RenameForCodegenVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.renames = SymbolTable.builder();
    }

    /**
     * Returns the new symbol table with the renamed symbols.
     */
    public SymbolTable symbolTable() {
        String regionParamName = symbolTable.regionParamName();
        if (regionParamName != null) {
            renames.regionParamName(javaName(regionParamName));
        }
        return renames.build();
    }

    @Override
    public RuleExpression visitVariableReferenceExpression(VariableReferenceExpression e) {
        String name = e.variableName();
        if (symbolTable.isLocal(name)) {
            RuleType type = symbolTable.localType(name);
            String newName = javaName(name);
            renames.putLocal(newName, type);
            return VariableReferenceExpression
                .builder()
                .variableName(newName)
                .build();
        }
        if (symbolTable.isParam(name)) {
            RuleType type = symbolTable.paramType(name);
            String newName = javaName(name);
            renames.putParam(newName, type);
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

    private String javaName(String name) {
        return Utils.unCapitalize(CodegenNamingUtils.pascalCase(name));
    }
}
