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
 * Visitor for all the expressions defined in the endpoints rule sets.
 *
 * @param <T> The type returned by the visitor.
 */
public interface RuleExpressionVisitor<T> {

    // Literals
    T visitLiteralBooleanExpression(LiteralBooleanExpression e);

    T visitLiteralIntegerExpression(LiteralIntegerExpression e);

    T visitLiteralStringExpression(LiteralStringExpression e);

    // Function call
    T visitFunctionCallExpression(FunctionCallExpression e);

    T visitMethodCallExpression(MethodCallExpression e);

    // Access
    T visitVariableReferenceExpression(VariableReferenceExpression e);

    T visitMemberAccessExpression(MemberAccessExpression e);

    T visitIndexedAccessExpression(IndexedAccessExpression e);

    T visitStringConcatExpression(StringConcatExpression e);

    // Let expression/statement
    T visitLetExpression(LetExpression e);

    // Boolean
    T visitBooleanAndExpression(BooleanAndExpression e);

    T visitBooleanNotExpression(BooleanNotExpression e);

    // Rule Set
    T visitRuleSetExpression(RuleSetExpression e);

    T visitEndpointExpression(EndpointExpression e);

    T visitErrorExpression(ErrorExpression e);

    // Collections
    T visitPropertiesExpression(PropertiesExpression e);

    T visitHeadersExpression(HeadersExpression e);

    T visitListExpression(ListExpression e);
}
