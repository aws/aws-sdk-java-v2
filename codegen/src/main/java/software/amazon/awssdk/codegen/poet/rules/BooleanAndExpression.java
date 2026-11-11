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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Synthetic expression used to group and codegen boolean (non-assignment) conditions from a rule set.
 */
public final class BooleanAndExpression implements RuleExpression {
    private final RuleType type;
    private final List<RuleExpression> expressions;

    BooleanAndExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.BOOLEAN;
        this.expressions = Collections.unmodifiableList(new ArrayList<>(builder.expressions));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.BOOLEAN_AND;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("(and");
        for (RuleExpression expr : expressions) {
            buf.append(" ");
            expr.appendTo(buf);
        }
        return buf.append(')');
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitBooleanAndExpression(this);
    }

    @Override
    public RuleExpression simplify() {
        if (expressions.size() == 1) {
            return expressions.get(0);
        }
        return this;
    }

    public List<RuleExpression> expressions() {
        return expressions;
    }

    @Override
    public RuleType type() {
        return type;
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BooleanAndExpression that = (BooleanAndExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return expressions.equals(that.expressions);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + expressions.hashCode();
        return result;
    }

    public static class Builder {
        private final List<RuleExpression> expressions = new ArrayList<>();

        public Builder expressions(List<RuleExpression> expressions) {
            this.expressions.addAll(expressions);
            return this;
        }

        public Builder addExpression(RuleExpression expr) {
            expressions.add(expr);
            return this;
        }

        public BooleanAndExpression build() {
            return new BooleanAndExpression(this);
        }
    }
}
