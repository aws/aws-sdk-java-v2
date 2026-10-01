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
 * Synthetic expression to represent direct method calls for codegen.
 */
public final class MethodCallExpression implements RuleExpression {
    private final RuleType type;
    private final RuleExpression source;
    private final String name;
    private final List<RuleExpression> arguments;

    MethodCallExpression(Builder builder) {
        this.type = builder.type;
        this.source = Validate.paramNotNull(builder.source, "source");
        this.name = Validate.paramNotNull(builder.name, "name");
        this.arguments = Collections.unmodifiableList(new ArrayList<>(builder.arguments));
    }

    public static Builder builder() {
        return new Builder();
    }

    public RuleExpression source() {
        return source;
    }

    public String name() {
        return name;
    }

    public List<RuleExpression> arguments() {
        return arguments;
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.METHOD_CALL;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("(.").append(name).append(" ");
        source.appendTo(buf);
        for (RuleExpression expr : arguments) {
            buf.append(" ");
            expr.appendTo(buf);
        }
        buf.append(")");
        return buf;
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitMethodCallExpression(this);
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

        MethodCallExpression that = (MethodCallExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        if (!source.equals(that.source)) {
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
        result = 31 * result + source.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + arguments.hashCode();
        return result;
    }

    public static class Builder {
        private final List<RuleExpression> arguments = new ArrayList<>();
        private RuleType type;
        private RuleExpression source;
        private String name;

        public Builder() {
        }

        public Builder(MethodCallExpression expr) {
            this.type = expr.type;
            this.name = expr.name;
            this.source = expr.source;
            this.arguments.addAll(expr.arguments);
        }

        public Builder type(RuleType type) {
            this.type = type;
            return this;
        }

        public Builder source(RuleExpression source) {
            this.source = source;
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

        public MethodCallExpression build() {
            return new MethodCallExpression(this);
        }
    }
}
