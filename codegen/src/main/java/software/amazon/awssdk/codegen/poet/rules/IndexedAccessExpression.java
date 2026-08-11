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
 * Represents an index access expression. E.g., {@code resourceId[0]}
 */
public final class IndexedAccessExpression implements RuleExpression {
    private final RuleType type;
    private final RuleExpression source;
    private final int index;

    IndexedAccessExpression(Builder builder) {
        this.type = builder.type;
        this.source = Validate.paramNotNull(builder.source, "source");
        this.index = Validate.paramNotNull(builder.index, "index");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.INDEXED_MEMBER_ACCESS;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        if (type == null) {
            buf.append("^UNDEFINED ");
        } else {
            buf.append("^").append(type).append(" ");
        }
        buf.append("(get-indexed ");
        source.appendTo(buf);
        return buf.append(" ")
                  .append(index)
                  .append(")");
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitIndexedAccessExpression(this);
    }

    public RuleExpression source() {
        return source;
    }

    public int index() {
        return index;
    }

    public Builder toBuilder() {
        return new Builder(this);
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

        IndexedAccessExpression that = (IndexedAccessExpression) o;

        if (index != that.index) {
            return false;
        }
        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + source.hashCode();
        result = 31 * result + index;
        return result;
    }

    public static class Builder {
        private RuleType type;
        private RuleExpression source;
        private Integer index;

        private Builder() {
        }

        private Builder(IndexedAccessExpression expr) {
            this.type = expr.type;
            this.source = expr.source;
            this.index = expr.index;
        }

        public Builder source(RuleExpression source) {
            this.source = source;
            return this;
        }

        public Builder index(int index) {
            this.index = index;
            return this;
        }

        public Builder type(RuleType type) {
            this.type = type;
            return this;
        }

        public IndexedAccessExpression build() {
            return new IndexedAccessExpression(this);
        }
    }
}
