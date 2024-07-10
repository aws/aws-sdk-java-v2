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
 * Represents a rule set expression.
 */
public final class RuleSetExpression implements RuleExpression {
    private final RuleType type;
    private final List<RuleExpression> conditions;
    private final List<RuleSetExpression> children;
    private final ErrorExpression error;
    private final EndpointExpression endpoint;
    private final String ruleId;

    RuleSetExpression(Builder builder) {
        this.type = RuleRuntimeTypeMirror.VOID;
        if (builder.conditions.isEmpty()) {
            this.conditions = Collections.emptyList();
        } else {
            this.conditions = Collections.unmodifiableList(new ArrayList<>(builder.conditions));
        }
        if (!builder.children.isEmpty()) {
            Validate.isNull(builder.error, "RuleSet with children cannot have error result");
            Validate.isNull(builder.endpoint, "RuleSet with children cannot have endpoint result");
            this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
            this.error = null;
            this.endpoint = null;
        } else if (builder.endpoint != null) {
            Validate.isNull(builder.error, "RuleSet with endpoint cannot have error result");
            this.children = null;
            this.error = null;
            this.endpoint = builder.endpoint;
        } else if (builder.error != null) {
            Validate.isNull(builder.endpoint, "RuleSet with error cannot have endpoint result");
            this.children = null;
            this.error = builder.error;
            this.endpoint = null;
        } else {
            throw new IllegalArgumentException("At exactly one of error, endpoint or tree, none given");
        }
        this.ruleId = builder.ruleId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.RULE_SET;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("{:type ");
        if (isEndpoint()) {
            buf.append(":endpoint");
            appendConditions(buf, conditions);
            buf.append(", :endpoint ");
            endpoint.appendTo(buf);
        } else if (isError()) {
            buf.append(":error");
            appendConditions(buf, conditions);
            buf.append(", :error ");
            error.appendTo(buf);
        } else if (isTree()) {
            buf.append(":tree");
            appendConditions(buf, conditions);
            buf.append(", :tree [");
            boolean isFirst = true;
            for (RuleSetExpression expr : children) {
                if (!isFirst) {
                    buf.append(", ");
                }
                expr.appendTo(buf);
                isFirst = false;
            }
            buf.append("]");
        } else {
            buf.append("UNKNOWN");
        }
        buf.append("}");
        return buf;
    }

    static void appendConditions(StringBuilder buf, List<RuleExpression> conditions) {
        buf.append(", :conditions [");
        boolean isFirst = true;
        for (RuleExpression expr : conditions) {
            if (!isFirst) {
                buf.append(", ");
            }
            expr.appendTo(buf);
            isFirst = false;
        }
        buf.append("]");
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitRuleSetExpression(this);
    }

    public List<RuleExpression> conditions() {
        return conditions;
    }

    public List<RuleSetExpression> children() {
        return children;
    }

    public ErrorExpression error() {
        return error;
    }

    public String ruleId() {
        return ruleId;
    }

    public EndpointExpression endpoint() {
        return endpoint;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public boolean isEndpoint() {
        return endpoint != null;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isTree() {
        return endpoint == null && error == null;
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

        RuleSetExpression that = (RuleSetExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        if (!Objects.equals(conditions, that.conditions)) {
            return false;
        }
        if (!Objects.equals(children, that.children)) {
            return false;
        }
        if (!Objects.equals(error, that.error)) {
            return false;
        }
        if (!Objects.equals(endpoint, that.endpoint)) {
            return false;
        }
        return Objects.equals(ruleId, that.ruleId);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (conditions != null ? conditions.hashCode() : 0);
        result = 31 * result + (children != null ? children.hashCode() : 0);
        result = 31 * result + (error != null ? error.hashCode() : 0);
        result = 31 * result + (endpoint != null ? endpoint.hashCode() : 0);
        result = 31 * result + (ruleId != null ? ruleId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public static class Builder {
        private final List<RuleExpression> conditions = new ArrayList<>();
        private final List<RuleSetExpression> children = new ArrayList<>();
        private ErrorExpression error;
        private EndpointExpression endpoint;
        private String ruleId;

        public Builder() {
        }

        public Builder(RuleSetExpression expr) {
            this.conditions.addAll(expr.conditions);
            if (expr.children != null) {
                this.children.addAll(expr.children);
            }
            this.error = expr.error;
            this.endpoint = expr.endpoint;
            this.ruleId = expr.ruleId;
        }

        public Builder conditions(List<RuleExpression> conditions) {
            this.conditions.clear();
            this.conditions.addAll(conditions);
            return this;
        }

        public Builder addCondition(RuleExpression condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder clearConditions() {
            this.conditions.clear();
            return this;
        }

        public Builder addChildren(RuleSetExpression expr) {
            children.add(expr);
            return this;
        }

        public Builder clearChildren() {
            this.children.clear();
            return this;
        }

        public Builder childern(List<RuleSetExpression> children) {
            this.children.clear();
            this.children.addAll(children);
            return this;
        }

        public Builder error(ErrorExpression error) {
            this.error = error;
            return this;
        }

        public Builder endpoint(EndpointExpression endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public RuleSetExpression build() {
            return new RuleSetExpression(this);
        }
    }
}
