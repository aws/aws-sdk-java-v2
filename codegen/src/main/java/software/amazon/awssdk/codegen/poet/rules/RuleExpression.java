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
 * Represents an expression within an endpoint rules set, either explicit or synthetically created for codegen.
 */
public interface RuleExpression {

    RuleExpressionKind kind();

    RuleType type();

    StringBuilder appendTo(StringBuilder buf);

    <T> T accept(RuleExpressionVisitor<T> visitor);

    default RuleExpression simplify() {
        return this;
    }

    enum RuleExpressionKind {
        INTEGER_VALUE,
        STRING_VALUE,
        BOOLEAN_VALUE,
        VARIABLE_REFERENCE,
        FUNCTION_CALL,
        MEMBER_ACCESS,
        INDEXED_MEMBER_ACCESS,
        BOOLEAN_NOT,
        BOOLEAN_AND,
        LET,
        RULE_SET,
        ENDPOINT,
        ERROR,
        PROPERTIES,
        HEADERS,
        LIST,
        STRING_CONCAT,
        METHOD_CALL,
    }
}
