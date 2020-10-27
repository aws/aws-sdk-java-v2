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

package software.amazon.awssdk.codegen.jmespath.parser;

import software.amazon.awssdk.codegen.jmespath.component.AndExpression;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifier;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithContents;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithQuestionMark;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithoutContents;
import software.amazon.awssdk.codegen.jmespath.component.ComparatorExpression;
import software.amazon.awssdk.codegen.jmespath.component.CurrentNode;
import software.amazon.awssdk.codegen.jmespath.component.Expression;
import software.amazon.awssdk.codegen.jmespath.component.ExpressionType;
import software.amazon.awssdk.codegen.jmespath.component.FunctionExpression;
import software.amazon.awssdk.codegen.jmespath.component.IndexExpression;
import software.amazon.awssdk.codegen.jmespath.component.Literal;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectHash;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectList;
import software.amazon.awssdk.codegen.jmespath.component.NotExpression;
import software.amazon.awssdk.codegen.jmespath.component.OrExpression;
import software.amazon.awssdk.codegen.jmespath.component.ParenExpression;
import software.amazon.awssdk.codegen.jmespath.component.PipeExpression;
import software.amazon.awssdk.codegen.jmespath.component.SliceExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpressionRight;
import software.amazon.awssdk.codegen.jmespath.component.WildcardExpression;

/**
 * A visitor across all of the JMESPath expression types. This can be passed to any 'union' type visitors, like
 * {@link Expression#visit(JmesPathVisitor)}.
 */
public interface JmesPathVisitor {
    void visitExpression(Expression input);

    void visitSubExpression(SubExpression input);

    void visitSubExpressionRight(SubExpressionRight input);

    void visitIndexExpression(IndexExpression input);

    void visitBracketSpecifier(BracketSpecifier input);

    void visitBracketSpecifierWithContents(BracketSpecifierWithContents input);

    void visitSliceExpression(SliceExpression input);

    void visitBracketSpecifierWithoutContents(BracketSpecifierWithoutContents input);

    void visitBracketSpecifierWithQuestionMark(BracketSpecifierWithQuestionMark input);

    void visitComparatorExpression(ComparatorExpression input);

    void visitOrExpression(OrExpression input);

    void visitIdentifier(String input);

    void visitAndExpression(AndExpression input);

    void visitNotExpression(NotExpression input);

    void visitParenExpression(ParenExpression input);

    void visitWildcardExpression(WildcardExpression input);

    void visitMultiSelectList(MultiSelectList input);

    void visitMultiSelectHash(MultiSelectHash input);

    void visitExpressionType(ExpressionType asExpressionType);

    void visitLiteral(Literal input);

    void visitFunctionExpression(FunctionExpression input);

    void visitPipeExpression(PipeExpression input);

    void visitRawString(String input);

    void visitCurrentNode(CurrentNode input);

    void visitNumber(int input);
}
