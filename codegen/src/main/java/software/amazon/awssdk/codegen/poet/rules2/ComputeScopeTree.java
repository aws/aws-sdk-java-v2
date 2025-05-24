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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes all the symbols, locals and params, used by each of the rules, either directly or transitively.
 */
public final class ComputeScopeTree extends WalkRuleExpressionVisitor {
    private final SymbolTable symbolTable;
    private final Deque<ScopeBuilder> scopes = new ArrayDeque<>();
    private final Map<String, Scope> scopesByName = new HashMap<>();
    private Scope result;

    public ComputeScopeTree(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * Returns the root scope.
     */
    public Scope result() {
        return result;
    }

    /**
     * Returns the mapping between rule id and scope.
     */
    public Map<String, Scope> scopesByName() {
        return scopesByName;
    }

    @Override
    public Void visitRuleSetExpression(RuleSetExpression node) {
        ScopeBuilder scopeBuilder = new ScopeBuilder();
        scopeBuilder.ruleId(node.ruleId());
        scopes.push(scopeBuilder);
        super.visitRuleSetExpression(node);
        result = scopes.pop().build();
        scopesByName.put(result.ruleId(), result);
        if (!scopes.isEmpty()) {
            scopes.peekFirst().addChild(result);
        }
        return null;
    }

    @Override
    public Void visitVariableReferenceExpression(VariableReferenceExpression e) {
        String variableName = e.variableName();
        ScopeBuilder current = scopes.peekFirst();
        if (symbolTable.isLocal(variableName)) {
            current.usesLocal(variableName);
        } else if (symbolTable.isParam(variableName)) {
            current.usesParam(variableName);
        }
        return null;
    }

    @Override
    public Void visitLetExpression(LetExpression e) {
        ScopeBuilder scopeBuilder = scopes.peekFirst();
        for (String binding : e.bindings().keySet()) {
            scopeBuilder.defines(binding);
        }
        return super.visitLetExpression(e);
    }

    public static class Scope {
        private final String ruleId;
        private final Set<String> defines;
        private final Set<String> usesLocals;
        private final Set<String> usesParams;
        private final List<Scope> children;

        public Scope(ScopeBuilder builder) {
            this.ruleId = Objects.requireNonNull(builder.ruleId, "ruleId cannot be null");
            this.defines = Collections.unmodifiableSet(new LinkedHashSet<>(builder.defines));
            this.usesLocals = Collections.unmodifiableSet(new LinkedHashSet<>(builder.usesLocals));
            this.usesParams = Collections.unmodifiableSet(new LinkedHashSet<>(builder.usesParams));
            this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
        }

        public String ruleId() {
            return ruleId;
        }

        public Set<String> defines() {
            return defines;
        }

        public Set<String> usesLocals() {
            return usesLocals;
        }

        public Set<String> usesParams() {
            return usesParams;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            appendTo(0, builder);
            return builder.toString();
        }

        public void appendTo(int level, StringBuilder sb) {
            String prefix = levelValue(level);
            sb.append(prefix).append("=========================================\n");
            sb.append(prefix).append("rule ").append(ruleId).append("\n");
            sb.append(prefix).append("defines ").append(defines).append("\n");
            sb.append(prefix).append("uses ").append(usesLocals).append("\n");
            for (Scope child : children) {
                child.appendTo(level + 1, sb);
            }
        }

        private String levelValue(int level) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < level; i++) {
                result.append("  ");
            }
            return result.toString();
        }
    }

    public static class ScopeBuilder {
        private String ruleId;
        private final Set<String> defines = new LinkedHashSet<>();
        private final Set<String> usesLocals = new LinkedHashSet<>();
        private final Set<String> usesParams = new LinkedHashSet<>();
        private final List<Scope> children = new ArrayList<>();

        public ScopeBuilder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public ScopeBuilder defines(String define) {
            defines.add(define);
            return this;
        }

        public ScopeBuilder usesLocal(String use) {
            usesLocals.add(use);
            return this;
        }

        public ScopeBuilder usesParam(String use) {
            usesParams.add(use);
            return this;
        }

        public ScopeBuilder addChild(Scope child) {
            children.add(child);
            for (String local : child.usesLocals) {
                if (!child.defines.contains(local)) {
                    usesLocals.add(local);
                }
            }
            for (String param : child.usesParams) {
                usesParams.add(param);
            }
            return this;
        }

        public Scope build() {
            return new Scope(this);
        }
    }
}
