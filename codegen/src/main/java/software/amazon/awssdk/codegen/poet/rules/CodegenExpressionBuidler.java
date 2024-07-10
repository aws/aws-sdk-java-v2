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
import java.util.Map;

public final class CodegenExpressionBuidler {
    private final RuleSetExpression root;
    private final SymbolTable symbolTable;

    public CodegenExpressionBuidler(RuleSetExpression root, SymbolTable symbolTable) {
        this.root = root;
        this.symbolTable = symbolTable;
    }

    public static CodegenExpressionBuidler from(RuleSetExpression root, RuleRuntimeTypeMirror typeMirror, SymbolTable table) {
        AssignTypesVisitor assignTypesVisitor = new AssignTypesVisitor(typeMirror, table);
        root = (RuleSetExpression) root.accept(assignTypesVisitor);
        List<String> errors = assignTypesVisitor.errors();
        if (!errors.isEmpty()) {
            throw new RuntimeException("Errors found while parsing the endpoint rules: " + String.join(", ", errors));
        }
        table = assignTypesVisitor.symbolTable();
        root = assignIdentifier(root);
        PrepareForCodegenVisitor prepareForCodegenVisitor = new PrepareForCodegenVisitor(table);
        root = (RuleSetExpression) root.accept(prepareForCodegenVisitor);
        table = prepareForCodegenVisitor.symbolTable();
        return new CodegenExpressionBuidler(root, table);
    }

    private static RuleSetExpression assignIdentifier(RuleSetExpression root) {
        AssignIdentifierVisitor assignIdentifierVisitor = new AssignIdentifierVisitor();
        return (RuleSetExpression) root.accept(assignIdentifierVisitor);
    }

    public RuleSetExpression root() {
        return root;
    }

    public boolean isParam(String name) {
        return symbolTable.isParam(name);
    }

    public boolean isLocal(String name) {
        return symbolTable.isLocal(name);
    }

    public Map<String, RuleType> locals() {
        return symbolTable.locals();
    }

    public Map<String, RuleType> params() {
        return symbolTable.params();
    }

    public SymbolTable symbolTable() {
        return symbolTable;
    }
}
