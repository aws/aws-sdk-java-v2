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
 * Interface for accessing JmesPath expression. Different
 * types of JmesPath expressions are supported.
 */
public interface JmesPathExpression {

    /**
     * Abstract method definition to delegate to either
     * the CodeGen visitor or Evaluation visitor based on
     * the type of JmesPath visitor
     *
     * @param visitor  CodeGen visitor or Evaluation visitor
     * @param inputT    Input expression that needs to be evaluated
     * @param <InputT>  Input type for the visitor
     *                 CodeGen visitor: Void
     *                 Evaluation visitor: JsonNode
     * @param <OutputT> Output type for the visitor
     *                 CodeGen visitor: String
     *                 Evaluation visitor: JsonNode
     * @return Corresponding output is returned. Evaluated String
     *     in the case of CodeGen visitor or an evaluated JsonNode in the case of Evaluation visitor
     */
    <InputT, OutputT> OutputT accept(JmesPathVisitor<InputT, OutputT> visitor, InputT inputT) throws InvalidTypeException;
}
