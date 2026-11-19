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
 * Represents an endpoint expression.
 */
public final class EndpointExpression implements RuleExpression {
    private final RuleType type;
    private final RuleExpression url;
    private final PropertiesExpression properties;
    private final HeadersExpression headers;

    EndpointExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.url = Validate.paramNotNull(builder.url, "url");
        this.properties = Validate.paramNotNull(builder.properties, "properties");
        this.headers = Validate.paramNotNull(builder.headers, "headers");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.ENDPOINT;
    }

    public RuleExpression url() {
        return url;
    }

    public PropertiesExpression properties() {
        return properties;
    }

    public HeadersExpression headers() {
        return headers;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("{:type :endpoint");
        buf.append(", :url ");
        url.appendTo(buf);
        buf.append(", :properties ");
        properties.appendTo(buf);
        buf.append(", :headers ");
        headers.appendTo(buf);
        buf.append("}");
        return buf;
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitEndpointExpression(this);
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

        EndpointExpression that = (EndpointExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        if (!url.equals(that.url)) {
            return false;
        }
        if (!properties.equals(that.properties)) {
            return false;
        }
        return headers.equals(that.headers);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + url.hashCode();
        result = 31 * result + properties.hashCode();
        result = 31 * result + headers.hashCode();
        return result;
    }

    public static class Builder {
        private RuleExpression url;
        private PropertiesExpression properties = PropertiesExpression.empty();
        private HeadersExpression headers = HeadersExpression.empty();

        public Builder url(RuleExpression url) {
            this.url = url;
            return this;
        }

        public Builder properties(PropertiesExpression properties) {
            this.properties = properties;
            return this;
        }

        public Builder headers(HeadersExpression headers) {
            this.headers = headers;
            return this;
        }

        public EndpointExpression build() {
            return new EndpointExpression(this);
        }
    }
}
