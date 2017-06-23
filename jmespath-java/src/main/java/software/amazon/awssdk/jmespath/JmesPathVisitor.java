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

package software.amazon.awssdk.jmespath;

/**
 * Visitor interface for delegating the Jmespath expression to its
 * corresponding type for evaluation
 *
 * @param <InputT>  Input type for the visitor
 *                 CodeGen visitor: Void
 *                 Evaluation visitor: JsonNode
 * @param <OutputT> Output type for the visitor
 *                 CodeGen visitor: String
 *                 Evaluation visitor: JsonNode
 */
public interface JmesPathVisitor<InputT, OutputT> {

    OutputT visit(JmesPathSubExpression subExpression, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathField fieldNode, InputT input);

    OutputT visit(JmesPathProjection jmesPathProjection, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathFlatten flatten, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathIdentity jmesPathIdentity, InputT input);

    OutputT visit(JmesPathValueProjection valueProjection, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathFunction function, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathLiteral literal, InputT input);

    OutputT visit(JmesPathFilter filter, InputT input) throws InvalidTypeException;

    OutputT visit(Comparator op, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathNotExpression expression, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathAndExpression expression, InputT input) throws InvalidTypeException;

    OutputT visit(JmesPathMultiSelectList multiSelectList, InputT input) throws InvalidTypeException;
}
