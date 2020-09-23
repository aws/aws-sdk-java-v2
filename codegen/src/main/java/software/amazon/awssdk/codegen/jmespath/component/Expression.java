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

package software.amazon.awssdk.codegen.jmespath.component;

import software.amazon.awssdk.codegen.jmespath.parser.JmesPathVisitor;
import software.amazon.awssdk.utils.Validate;

/**
 * An expression is any statement that can be executed in isolation from other parts of a JMESPath string. Every valid JMESPath
 * string is an expression, usually made up of other expressions.
 *
 * Examples: https://jmespath.org/examples.html
 */
public class Expression {
    private SubExpression subExpression;
    private IndexExpression indexExpression;
    private ComparatorExpression comparatorExpression;
    private OrExpression orExpression;
    private String identifier;
    private AndExpression andExpression;
    private NotExpression notExpression;
    private ParenExpression parenExpression;
    private WildcardExpression wildcardExpression;
    private MultiSelectList multiSelectList;
    private MultiSelectHash multiSelectHash;
    private Literal literal;
    private FunctionExpression functionExpression;
    private PipeExpression pipeExpression;
    private String rawString;
    private CurrentNode currentNode;

    public static Expression subExpression(SubExpression subExpression) {
        Validate.notNull(subExpression, "subExpression");
        Expression expression = new Expression();
        expression.subExpression = subExpression;
        return expression;
    }

    public static Expression indexExpression(IndexExpression indexExpression) {
        Validate.notNull(indexExpression, "indexExpression");
        Expression expression = new Expression();
        expression.indexExpression = indexExpression;
        return expression;
    }

    public static Expression comparatorExpression(ComparatorExpression comparatorExpression) {
        Validate.notNull(comparatorExpression, "comparatorExpression");
        Expression expression = new Expression();
        expression.comparatorExpression = comparatorExpression;
        return expression;
    }

    public static Expression orExpression(OrExpression orExpression) {
        Validate.notNull(orExpression, "orExpression");
        Expression expression = new Expression();
        expression.orExpression = orExpression;
        return expression;
    }

    public static Expression identifier(String identifier) {
        Validate.notNull(identifier, "identifier");
        Expression expression = new Expression();
        expression.identifier = identifier;
        return expression;
    }

    public static Expression andExpression(AndExpression andExpression) {
        Validate.notNull(andExpression, "andExpression");
        Expression expression = new Expression();
        expression.andExpression = andExpression;
        return expression;
    }

    public static Expression notExpression(NotExpression notExpression) {
        Validate.notNull(notExpression, "notExpression");
        Expression expression = new Expression();
        expression.notExpression = notExpression;
        return expression;
    }

    public static Expression parenExpression(ParenExpression parenExpression) {
        Validate.notNull(parenExpression, "parenExpression");
        Expression expression = new Expression();
        expression.parenExpression = parenExpression;
        return expression;
    }

    public static Expression wildcardExpression(WildcardExpression wildcardExpression) {
        Validate.notNull(wildcardExpression, "wildcardExpression");
        Expression expression = new Expression();
        expression.wildcardExpression = wildcardExpression;
        return expression;
    }

    public static Expression multiSelectList(MultiSelectList multiSelectList) {
        Validate.notNull(multiSelectList, "multiSelectList");
        Expression expression = new Expression();
        expression.multiSelectList = multiSelectList;
        return expression;
    }

    public static Expression multiSelectHash(MultiSelectHash multiSelectHash) {
        Validate.notNull(multiSelectHash, "multiSelectHash");
        Expression expression = new Expression();
        expression.multiSelectHash = multiSelectHash;
        return expression;
    }

    public static Expression literal(Literal literal) {
        Validate.notNull(literal, "literal");
        Expression expression = new Expression();
        expression.literal = literal;
        return expression;
    }

    public static Expression functionExpression(FunctionExpression functionExpression) {
        Validate.notNull(functionExpression, "functionExpression");
        Expression expression = new Expression();
        expression.functionExpression = functionExpression;
        return expression;
    }

    public static Expression pipeExpression(PipeExpression pipeExpression) {
        Validate.notNull(pipeExpression, "pipeExpression");
        Expression expression = new Expression();
        expression.pipeExpression = pipeExpression;
        return expression;
    }

    public static Expression rawString(String rawString) {
        Validate.notNull(rawString, "rawString");
        Expression expression = new Expression();
        expression.rawString = rawString;
        return expression;
    }

    public static Expression currentNode(CurrentNode currentNode) {
        Validate.notNull(currentNode, "currentNode");
        Expression expression = new Expression();
        expression.currentNode = currentNode;
        return expression;
    }

    public boolean isSubExpression() {
        return subExpression != null;
    }

    public boolean isIndexExpression() {
        return indexExpression != null;
    }

    public boolean isComparatorExpression() {
        return comparatorExpression != null;
    }

    public boolean isOrExpression() {
        return orExpression != null;
    }

    public boolean isIdentifier() {
        return identifier != null;
    }

    public boolean isAndExpression() {
        return andExpression != null;
    }

    public boolean isNotExpression() {
        return notExpression != null;
    }

    public boolean isParenExpression() {
        return parenExpression != null;
    }

    public boolean isWildcardExpression() {
        return wildcardExpression != null;
    }

    public boolean isMultiSelectList() {
        return multiSelectList != null;
    }

    public boolean isMultiSelectHash() {
        return multiSelectHash != null;
    }

    public boolean isLiteral() {
        return literal != null;
    }

    public boolean isFunctionExpression() {
        return functionExpression != null;
    }

    public boolean isPipeExpression() {
        return pipeExpression != null;
    }

    public boolean isRawString() {
        return rawString != null;
    }

    public boolean isCurrentNode() {
        return currentNode != null;
    }

    public SubExpression asSubExpression() {
        Validate.validState(isSubExpression(), "Not a SubExpression");
        return subExpression;
    }

    public IndexExpression asIndexExpression() {
        Validate.validState(isIndexExpression(), "Not a IndexExpression");
        return indexExpression;
    }

    public ComparatorExpression asComparatorExpression() {
        Validate.validState(isComparatorExpression(), "Not a ComparatorExpression");
        return comparatorExpression;
    }

    public OrExpression asOrExpression() {
        Validate.validState(isOrExpression(), "Not a OrExpression");
        return orExpression;
    }

    public String asIdentifier() {
        Validate.validState(isIdentifier(), "Not a Identifier");
        return identifier;
    }

    public AndExpression asAndExpression() {
        Validate.validState(isAndExpression(), "Not a AndExpression");
        return andExpression;
    }

    public NotExpression asNotExpression() {
        Validate.validState(isNotExpression(), "Not a NotExpression");
        return notExpression;
    }

    public ParenExpression asParenExpression() {
        Validate.validState(isParenExpression(), "Not a ParenExpression");
        return parenExpression;
    }

    public WildcardExpression asWildcardExpression() {
        Validate.validState(isWildcardExpression(), "Not a WildcardExpression");
        return wildcardExpression;
    }

    public MultiSelectList asMultiSelectList() {
        Validate.validState(isMultiSelectList(), "Not a MultiSelectList");
        return multiSelectList;
    }

    public MultiSelectHash asMultiSelectHash() {
        Validate.validState(isMultiSelectHash(), "Not a MultiSelectHash");
        return multiSelectHash;
    }

    public Literal asLiteral() {
        Validate.validState(isLiteral(), "Not a Literal");
        return literal;
    }

    public FunctionExpression asFunctionExpression() {
        Validate.validState(isFunctionExpression(), "Not a FunctionExpression");
        return functionExpression;
    }

    public PipeExpression asPipeExpression() {
        Validate.validState(isPipeExpression(), "Not a PipeExpression");
        return pipeExpression;
    }

    public String asRawString() {
        Validate.validState(isRawString(), "Not a RawString");
        return rawString;
    }

    public CurrentNode asCurrentNode() {
        Validate.validState(isCurrentNode(), "Not a CurrentNode");
        return currentNode;
    }

    public void visit(JmesPathVisitor visitor) {
        if (isSubExpression()) {
            visitor.visitSubExpression(asSubExpression());
        } else if (isIndexExpression()) {
            visitor.visitIndexExpression(asIndexExpression());
        } else if (isComparatorExpression()) {
            visitor.visitComparatorExpression(asComparatorExpression());
        } else if (isOrExpression()) {
            visitor.visitOrExpression(asOrExpression());
        } else if (isIdentifier()) {
            visitor.visitIdentifier(asIdentifier());
        } else if (isAndExpression()) {
            visitor.visitAndExpression(asAndExpression());
        } else if (isNotExpression()) {
            visitor.visitNotExpression(asNotExpression());
        } else if (isParenExpression()) {
            visitor.visitParenExpression(asParenExpression());
        } else if (isWildcardExpression()) {
            visitor.visitWildcardExpression(asWildcardExpression());
        } else if (isMultiSelectList()) {
            visitor.visitMultiSelectList(asMultiSelectList());
        } else if (isMultiSelectHash()) {
            visitor.visitMultiSelectHash(asMultiSelectHash());
        } else if (isLiteral()) {
            visitor.visitLiteral(asLiteral());
        } else if (isFunctionExpression()) {
            visitor.visitFunctionExpression(asFunctionExpression());
        } else if (isPipeExpression()) {
            visitor.visitPipeExpression(asPipeExpression());
        } else if (isRawString()) {
            visitor.visitRawString(asRawString());
        } else if (isCurrentNode()) {
            visitor.visitCurrentNode(asCurrentNode());
        } else {
            throw new IllegalStateException();
        }
    }
}
