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

/**
 * Represents an error expression.
 */
public final class ErrorExpression implements RuleExpression {
    private final RuleType type;
    private final RuleExpression error;

    public ErrorExpression(RuleExpression error) {
        this.type = RuleRuntimeTypeMirror.VOID;
        this.error = error;
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.ERROR;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        buf.append("{:type :error, :message ");
        error.appendTo(buf);
        return buf.append("}");
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitErrorExpression(this);
    }

    public RuleExpression error() {
        return error;
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

        ErrorExpression that = (ErrorExpression) o;

        if (!Objects.equals(type, that.type)) {
            return false;
        }
        return error.equals(that.error);
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + error.hashCode();
        return result;
    }
}
