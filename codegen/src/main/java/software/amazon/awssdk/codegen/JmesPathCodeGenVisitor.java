/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen;

import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import software.amazon.awssdk.jmespath.Comparator;
import software.amazon.awssdk.jmespath.InvalidTypeException;
import software.amazon.awssdk.jmespath.JmesPathAndExpression;
import software.amazon.awssdk.jmespath.JmesPathField;
import software.amazon.awssdk.jmespath.JmesPathFilter;
import software.amazon.awssdk.jmespath.JmesPathFlatten;
import software.amazon.awssdk.jmespath.JmesPathFunction;
import software.amazon.awssdk.jmespath.JmesPathIdentity;
import software.amazon.awssdk.jmespath.JmesPathLiteral;
import software.amazon.awssdk.jmespath.JmesPathMultiSelectList;
import software.amazon.awssdk.jmespath.JmesPathNotExpression;
import software.amazon.awssdk.jmespath.JmesPathProjection;
import software.amazon.awssdk.jmespath.JmesPathSubExpression;
import software.amazon.awssdk.jmespath.JmesPathValueProjection;
import software.amazon.awssdk.jmespath.JmesPathVisitor;

public class JmesPathCodeGenVisitor implements JmesPathVisitor<Void, String> {

    /**
     * Generates the code for a new JmesPathSubExpression.
     *
     * @param subExpression JmesPath subexpression type
     * @param aVoid         void
     * @return String that represents a call to
     *     the new subexpression
     */
    @Override
    public String visit(final JmesPathSubExpression subExpression, final Void aVoid)
            throws InvalidTypeException {
        final String prefix = "new JmesPathSubExpression( ";
        return subExpression.getExpressions().stream()
                            .map(a -> a.accept(this, aVoid))
                            .collect(Collectors.joining(",", prefix, ")"));
    }

    /**
     * Generates the code for a new JmesPathField.
     *
     * @param fieldNode JmesPath field type
     * @param aVoid     void
     * @return String that represents a call to
     *     the new fieldNode
     */
    @Override
    public String visit(final JmesPathField fieldNode, final Void aVoid) {
        return "new JmesPathField( \"" + fieldNode.getValue() + "\")";
    }

    /**
     * Generates the code for a new JmesPathProjection.
     *
     * @param jmesPathProjection JmesPath projection type
     * @param aVoid              void
     * @return String that represents a call to
     *     the new list projection
     */
    @Override
    public String visit(final JmesPathProjection jmesPathProjection,
                        final Void aVoid) throws InvalidTypeException {
        final String param1 = jmesPathProjection.getLhsExpr().accept(this, aVoid);
        final String param2 = jmesPathProjection.getProjectionExpr().accept(this, aVoid);
        return "new JmesPathProjection( " + param1 + ", " + param2 + ")";
    }

    /**
     * Generates the code for a new JmesPathFlatten.
     *
     * @param flatten JmesPath flatten type
     * @param aVoid   void
     * @return String that represents a call to
     *     the new flatten projection
     */
    @Override
    public String visit(final JmesPathFlatten flatten, final Void aVoid)
            throws InvalidTypeException {
        return "new JmesPathFlatten( " + flatten.getFlattenExpr()
                                                .accept(this, aVoid) + ")";
    }

    /**
     * Generates the code for a new JmesPathIdentity.
     *
     * @param jmesPathIdentity JmesPath identity type
     * @param aVoid            void
     * @return String that represents a call to
     *     the new identity expression
     */
    @Override
    public String visit(final JmesPathIdentity jmesPathIdentity,
                        final Void aVoid) {
        return "new JmesPathIdentity()";
    }

    /**
     * Generates the code for a new JmesPathValueProjection.
     *
     * @param valueProjection JmesPath value projection type
     * @param aVoid           void
     * @return String that represents a call to
     *     the new value projection
     */
    @Override
    public String visit(final JmesPathValueProjection valueProjection,
                        final Void aVoid) throws InvalidTypeException {
        final String param1 = valueProjection.getLhsExpr().accept(this, aVoid);
        final String param2 = valueProjection.getRhsExpr().accept(this, aVoid);
        return "new JmesPathValueProjection( " + param1 + ", " + param2 + ")";
    }

    /**
     * Generates the code for a new JmesPathLiteral.
     *
     * @param literal JmesPath literal type
     * @param aVoid   void
     * @return String that represents a call to
     *     the new literal expression
     */
    @Override
    public String visit(final JmesPathLiteral literal, final Void aVoid) {
        return "new JmesPathLiteral(\"" + StringEscapeUtils
                .escapeJava(literal.getValue().toString()) + "\")";

    }

    /**
     * Generates the code for a new JmesPathFilter.
     *
     * @param filter JmesPath filter type
     * @param aVoid  void
     * @return String that represents a call to
     *     the new filter expression
     */
    @Override
    public String visit(final JmesPathFilter filter,
                        final Void aVoid) throws InvalidTypeException {
        return "new JmesPathFilter( " + filter.getLhsExpr().accept(this, aVoid)
               + ", " + filter.getRhsExpr().accept(this, aVoid) + ", "
               + filter.getComparator().accept(this, aVoid) + ")";
    }

    /**
     * Generates the code for a new JmesPathFunction.
     *
     * @param function JmesPath function type
     * @param aVoid    void
     * @return String that represents a call to
     *     the new function expression
     */
    @Override
    public String visit(final JmesPathFunction function,
                        final Void aVoid) throws InvalidTypeException {
        final String prefix = "new " + function.getClass()
                                               .getSimpleName() + "( ";
        return function.getExpressions().stream()
                       .map(a -> a.accept(this, aVoid))
                       .collect(Collectors.joining(",", prefix, ")"));
    }

    /**
     * Generates the code for a new Comparator.
     *
     * @param op    JmesPath comparison operator type
     * @param aVoid void
     * @return String that represents a call to
     *     the new comparator expression
     */
    @Override
    public String visit(final Comparator op, final Void aVoid)
            throws InvalidTypeException {
        String lhs = op.getLhsExpr().accept(this, aVoid);
        String rhs = op.getRhsExpr().accept(this, aVoid);

        return String.format("new %s(%s, %s)", op.getClass()
                                                 .getSimpleName(), lhs, rhs);
    }

    /**
     * Generates the code for a new JmesPathNotExpression.
     *
     * @param notExpression JmesPath not-expression type
     * @param aVoid         void
     * @return String that represents a call to
     *     the new not-expression
     */
    @Override
    public String visit(final JmesPathNotExpression notExpression,
                        final Void aVoid) throws InvalidTypeException {
        return "new JmesPathNotExpression( " + notExpression.getExpr()
                                                            .accept(this, aVoid) + " )";
    }

    /**
     * Generates the code for a new JmesPathAndExpression.
     *
     * @param andExpression JmesPath and-expression type
     * @param aVoid         void
     * @return String that represents a call to
     *     the new and-expression
     */
    @Override
    public String visit(final JmesPathAndExpression andExpression,
                        final Void aVoid) throws InvalidTypeException {
        final String param1 = andExpression.getLhsExpr().accept(this, aVoid);
        final String param2 = andExpression.getRhsExpr().accept(this, aVoid);
        return "new JmesPathAndExpression( " + param1 + ", " + param2 + " )";
    }

    /**
     * Generates the code for a new JmesPathMultiSelectList.
     *
     * @param multiSelectList JmesPath multiSelectList type
     * @param aVoid           void
     * @return String that represents a call to
     *     the new multiSelectList
     */
    @Override
    public String visit(final JmesPathMultiSelectList multiSelectList,
                        final Void aVoid) throws InvalidTypeException {
        final String prefix = "new JmesPathMultiSelectList( ";
        return multiSelectList.getExpressions().stream()
                              .map(a -> a.accept(this, aVoid))
                              .collect(Collectors.joining(",", prefix, ")"));
    }
}
