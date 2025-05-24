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

import java.util.List;
import java.util.Map;

public final class CodegenExpressionBuidler {
    private final RuleSetExpression root;
    private final SymbolTable symbolTable;
    private final Map<String, ComputeScopeTree.Scope> scopesByName;

    public CodegenExpressionBuidler(
        RuleSetExpression root,
        SymbolTable symbolTable,
        Map<String, ComputeScopeTree.Scope> scopesByName
    ) {
        this.root = root;
        this.symbolTable = symbolTable;
        this.scopesByName = scopesByName;
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

        RenameForCodegenVisitor renameForCodegenVisitor = new RenameForCodegenVisitor(table);
        root = (RuleSetExpression) root.accept(renameForCodegenVisitor);
        table = renameForCodegenVisitor.symbolTable();

        ComputeScopeTree computeScopeTree = new ComputeScopeTree(table);
        root.accept(computeScopeTree);

        PrepareForCodegenVisitor prepareForCodegenVisitor = new PrepareForCodegenVisitor();
        RuleSetExpression newRoot = (RuleSetExpression) root.accept(prepareForCodegenVisitor);
        return new CodegenExpressionBuidler(newRoot, table, computeScopeTree.scopesByName());
    }

    private static RuleSetExpression assignIdentifier(RuleSetExpression root) {
        AssignIdentifierVisitor assignIdentifierVisitor = new AssignIdentifierVisitor();
        return (RuleSetExpression) root.accept(assignIdentifierVisitor);
    }

    public RuleSetExpression root() {
        return root;
    }

    public String regionParamName() {
        return symbolTable.regionParamName();
    }

    public SymbolTable symbolTable() {
        return symbolTable;
    }

    public Map<String, ComputeScopeTree.Scope> scopesByName() {
        return scopesByName;
    }
}
