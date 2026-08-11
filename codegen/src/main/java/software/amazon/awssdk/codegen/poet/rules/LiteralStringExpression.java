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

/**
 * Represents a literal string value. E.g., {@code "accesspoint"}.
 */
public final class LiteralStringExpression implements RuleExpression {
    private final RuleType type;
    private final String value;

    public LiteralStringExpression(String value) {
        this.type = RuleRuntimeTypeMirror.STRING;
        this.value = value;
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.STRING_VALUE;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        return buf.append('"').append(value).append('"');
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitLiteralStringExpression(this);
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public String value() {
        return value;
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

        LiteralStringExpression that = (LiteralStringExpression) o;

        if (!type.equals(that.type)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
