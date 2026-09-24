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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.utils.Validate;

/**
 * Synthetic expression used to group and codegen assignment "conditions" from a rule set. E.g., {@code "assign": "arnType"}
 */
public final class LetExpression implements RuleExpression {
    private final RuleType type;
    private final Map<String, RuleExpression> bindings;

    LetExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.bindings = Collections.unmodifiableMap(new LinkedHashMap<>(builder.bindings));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.LET;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("(let [");
        boolean isFirst = true;
        for (Map.Entry<String, RuleExpression> kvp : bindings.entrySet()) {
            if (!isFirst) {
                buf.append(' ');
            }
            buf.append(kvp.getKey()).append(' ');
            kvp.getValue().appendTo(buf);
            isFirst = false;
        }
        buf.append("] ⋯ )");
        return buf;
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitLetExpression(this);
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public Map<String, RuleExpression> bindings() {
        return bindings;
    }

    @Override
    public RuleType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LetExpression that = (LetExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return bindings.equals(that.bindings);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + bindings.hashCode();
        return result;
    }

    public static class Builder {
        private final Map<String, RuleExpression> bindings = new LinkedHashMap<>();

        public Builder putBinding(String name, RuleExpression value) {
            bindings.put(Validate.paramNotNull(name, "name"), Validate.paramNotNull(value, "value"));
            return this;
        }

        public LetExpression build() {
            return new LetExpression(this);
        }
    }
}
