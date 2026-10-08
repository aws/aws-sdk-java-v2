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

public final class VariableReferenceExpression implements RuleExpression {
    private final RuleType type;
    private final String variableName;

    public VariableReferenceExpression(String variableName) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.variableName = Validate.paramNotNull(variableName, "variableName");
    }

    VariableReferenceExpression(Builder builder) {
        this.type = builder.type;
        this.variableName = Validate.paramNotNull(builder.variableName, "variableName");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.VARIABLE_REFERENCE;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        return buf.append(variableName);
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitVariableReferenceExpression(this);
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public String variableName() {
        return variableName;
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

        VariableReferenceExpression that = (VariableReferenceExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return variableName.equals(that.variableName);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + variableName.hashCode();
        return result;
    }

    public static class Builder {
        private RuleType type;
        private String variableName;

        private Builder() {
        }

        private Builder(VariableReferenceExpression expr) {
            this.variableName = expr.variableName;
            this.type = expr.type;
        }

        public Builder type(RuleType type) {
            this.type = type;
            return this;
        }

        public Builder variableName(String name) {
            this.variableName = name;
            return this;
        }

        public VariableReferenceExpression build() {
            return new VariableReferenceExpression(this);
        }
    }
}
