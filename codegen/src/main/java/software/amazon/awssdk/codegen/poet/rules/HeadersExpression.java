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
 * Represents a list of headers expression.
 */
public final class HeadersExpression implements RuleExpression {
    private static final HeadersExpression EMPTY = new HeadersExpression(Collections.emptyMap());
    private final RuleType type;
    private final Map<String, ListExpression> headers;

    HeadersExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
    }

    HeadersExpression(Map<String, ListExpression> headers) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.headers = headers;
    }

    public static HeadersExpression empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.HEADERS;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("{:type :headers");
        headers.forEach((k, v) -> {
            buf.append(", :").append(k).append(" ");
            v.appendTo(buf);
        });
        buf.append("}");
        return buf;
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitHeadersExpression(this);
    }

    public Map<String, ListExpression> headers() {
        return headers;
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

        HeadersExpression that = (HeadersExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return headers.equals(that.headers);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + headers.hashCode();
        return result;
    }

    public static class Builder {
        private final Map<String, ListExpression> headers = new LinkedHashMap<>();

        public Builder putHeader(String name, ListExpression value) {
            this.headers.put(Validate.paramNotNull(name, "name"),
                             Validate.paramNotNull(value, "value"));
            return this;
        }

        public HeadersExpression build() {
            if (headers.isEmpty()) {
                return EMPTY;
            }
            return new HeadersExpression(this);
        }
    }
}
