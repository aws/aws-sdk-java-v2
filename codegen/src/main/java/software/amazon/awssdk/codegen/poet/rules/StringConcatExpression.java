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

public final class StringConcatExpression implements RuleExpression {
    private final RuleType type;
    private final List<RuleExpression> expressions;

    StringConcatExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.STRING;
        this.expressions = Collections.unmodifiableList(new ArrayList<>(builder.expressions));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.STRING_CONCAT;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("(concat");
        for (RuleExpression expr : expressions) {
            buf.append(" ");
            expr.appendTo(buf);
        }
        return buf.append(")");
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitStringConcatExpression(this);
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

        StringConcatExpression that = (StringConcatExpression) o;

        if (!type.equals(that.type)) {
            return false;
        }
        return expressions.equals(that.expressions);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + expressions.hashCode();
        return result;
    }

    public static class Builder {
        private final List<RuleExpression> expressions = new ArrayList<>();

        public Builder addExpression(RuleExpression expr) {
            this.expressions.add(expr);
            return this;
        }

        public StringConcatExpression build() {
            return new StringConcatExpression(this);
        }
    }
}
