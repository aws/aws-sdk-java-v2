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
 * Represents a member access expression. E.g. {@code {bucketArn#region}}
 */
public final class MemberAccessExpression implements RuleExpression {
    private final RuleType type;
    private final RuleExpression source;
    private final String name;

    MemberAccessExpression(Builder builder) {
        this.type = builder.type;
        this.source = Validate.paramNotNull(builder.source, "source");
        this.name = Validate.paramNotNull(builder.name, "name");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.MEMBER_ACCESS;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("(get-member ");
        source.appendTo(buf);
        buf.append(" ");
        buf.append(name);
        return buf.append(")");
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitMemberAccessExpression(this);
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public RuleExpression source() {
        return source;
    }

    public String name() {
        return name;
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

        MemberAccessExpression that = (MemberAccessExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        if (!source.equals(that.source)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + source.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public static class Builder {
        private RuleType type;
        private RuleExpression source;
        private String name;

        private Builder() {
        }

        private Builder(MemberAccessExpression expr) {
            this.type = expr.type;
            this.source = expr.source;
            this.name = expr.name;
        }

        public Builder source(RuleExpression source) {
            this.source = source;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(RuleType type) {
            this.type = type;
            return this;
        }

        public MemberAccessExpression build() {
            return new MemberAccessExpression(this);
        }
    }
}
