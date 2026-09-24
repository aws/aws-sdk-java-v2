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
 * Represents a properties expression, part of an endpoint expression.
 */
public final class PropertiesExpression implements RuleExpression {
    private static final PropertiesExpression EMPTY = new PropertiesExpression(Collections.emptyMap());
    private final RuleType type;

    private final Map<String, RuleExpression> properties;

    PropertiesExpression(Map<String, RuleExpression> properties) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(Validate.paramNotNull(properties, "properties")));
    }

    PropertiesExpression(Builder builder) {
        this(builder.properties);
    }

    public static PropertiesExpression empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.PROPERTIES;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("{:type :properties");
        properties.forEach((k, v) -> {
            buf.append(", :").append(k).append(" ");
            v.appendTo(buf);
        });
        buf.append("}");
        return buf;
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitPropertiesExpression(this);
    }

    public Map<String, RuleExpression> properties() {
        return properties;
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

        PropertiesExpression that = (PropertiesExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + properties.hashCode();
        return result;
    }

    public static class Builder {
        private final Map<String, RuleExpression> properties = new LinkedHashMap<>();

        public Builder putProperty(String name, RuleExpression expr) {
            this.properties.put(Validate.paramNotNull(name, "name"),
                                Validate.paramNotNull(expr, "expr"));
            return this;
        }

        public PropertiesExpression build() {
            if (properties.isEmpty()) {
                return EMPTY;
            }
            return new PropertiesExpression(this);
        }
    }
}
