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
import software.amazon.awssdk.utils.Validate;

/**
 * Represents a function call expression.
 */
public final class FunctionCallExpression implements RuleExpression {
    private final RuleType type;
    private final String name;
    private final List<RuleExpression> arguments;

    FunctionCallExpression(Builder builder) {
        this.type = builder.type;
        this.name = Validate.paramNotNull(builder.name, "name");
        this.arguments = Collections.unmodifiableList(new ArrayList<>(Validate.paramNotNull(builder.arguments, "arguments")));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.FUNCTION_CALL;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        if (type == null) {
            buf.append("^UNRESOLVED ");
        } else {
            buf.append("^").append(type).append(" ");
        }
        buf.append("(").append(name);
        for (RuleExpression arg : arguments) {
            buf.append(" ");
            arg.appendTo(buf);
        }
        buf.append(")");
        return buf;
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitFunctionCallExpression(this);
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public String name() {
        return name;
    }

    public List<RuleExpression> arguments() {
        return arguments;
    }

    public Builder toBuilder() {
        return new Builder(this);
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

        FunctionCallExpression that = (FunctionCallExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        return arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + name.hashCode();
        result = 31 * result + arguments.hashCode();
        return result;
    }

    public static class Builder {
        private final List<RuleExpression> arguments = new ArrayList<>();
        private RuleType type;
        private String name;

        public Builder() {
        }

        public Builder(FunctionCallExpression expr) {
            this.type = expr.type;
            this.name = expr.name;
            this.arguments.addAll(expr.arguments);
        }

        public Builder type(RuleType type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addArgument(RuleExpression arg) {
            this.arguments.add(arg);
            return this;
        }

        public Builder arguments(List<RuleExpression> args) {
            this.arguments.clear();
            this.arguments.addAll(args);
            return this;
        }

        public FunctionCallExpression build() {
            return new FunctionCallExpression(this);
        }

    }
}
