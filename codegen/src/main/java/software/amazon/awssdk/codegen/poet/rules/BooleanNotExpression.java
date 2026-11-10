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

import java.util.Objects;
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a {@code not} function call as an expression.
 */
public final class BooleanNotExpression implements RuleExpression {
    private final RuleType type;
    private final RuleExpression expression;

    BooleanNotExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.BOOLEAN;
        this.expression = Validate.paramNotNull(builder.expression, "expression");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.BOOLEAN_NOT;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("(not ");
        expression.appendTo(buf);
        return buf.append(')');
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitBooleanNotExpression(this);
    }

    public RuleExpression expression() {
        return expression;
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

        BooleanNotExpression that = (BooleanNotExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + expression.hashCode();
        return result;
    }

    public static class Builder {
        private RuleExpression expression;

        public Builder expression(RuleExpression expression) {
            this.expression = expression;
            return this;
        }

        public BooleanNotExpression build() {
            return new BooleanNotExpression(this);
        }
    }
}
