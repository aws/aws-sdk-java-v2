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

package software.amazon.awssdk.codegen.smithy.customization;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

public interface SmithyCustomizationProcessor {

    /**
     * Apply customization by transforming the Smithy model before the
     * IntermediateModel is built. Returns a new Model since Smithy models
     * are immutable.
     *
     * @param model   the current Smithy model
     * @param service the service shape within the model
     * @return the potentially modified model
     */
    Model preprocess(Model model, ServiceShape service);

    /**
     * Apply customization after the IntermediateModel is built.
     * The IntermediateModel is mutable, same as the C2J path.
     */
    void postprocess(IntermediateModel intermediateModel);
}
