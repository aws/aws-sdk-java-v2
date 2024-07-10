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
 * Represents a literal integer value. E.g., {@code 123}.
 */
public final class LiteralIntegerExpression implements RuleExpression {
    private final RuleType type;
    private final int value;

    public LiteralIntegerExpression(int value) {
        this.type = RuleRuntimeTypeMirror.INTEGER;
        this.value = value;
    }

    @Override
    public RuleExpressionKind kind() {
        return RuleExpressionKind.INTEGER_VALUE;
    }

    @Override
    public StringBuilder appendTo(StringBuilder buf) {
        return buf.append(value);
    }

    @Override
    public <T> T accept(RuleExpressionVisitor<T> visitor) {
        return visitor.visitLiteralIntegerExpression(this);
    }

    public int value() {
        return value;
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

        LiteralIntegerExpression that = (LiteralIntegerExpression) o;

        if (value != that.value) {
            return false;
        }
        return type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + value;
        return result;
    }
}
