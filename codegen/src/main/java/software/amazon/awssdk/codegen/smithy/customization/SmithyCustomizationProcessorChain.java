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

/**
 * A composite {@link SmithyCustomizationProcessor} that runs a sequence of
 * processors in order, threading the immutable {@link Model} through each
 * preprocess step and re-resolving the {@link ServiceShape} after each
 * transformation.
 */
public final class SmithyCustomizationProcessorChain implements SmithyCustomizationProcessor {

    private final SmithyCustomizationProcessor[] processorChain;

    public SmithyCustomizationProcessorChain(SmithyCustomizationProcessor... processors) {
        this.processorChain = processors == null
                              ? new SmithyCustomizationProcessor[0]
                              : processors.clone();
    }

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        Model current = model;
        for (SmithyCustomizationProcessor processor : processorChain) {
            current = processor.preprocess(current, service);
            // Re-resolve service shape from the updated model since
            // renaming or other transforms may have changed it
            service = current.expectShape(service.getId(), ServiceShape.class);
        }
        return current;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        for (SmithyCustomizationProcessor processor : processorChain) {
            processor.postprocess(intermediateModel);
        }
    }
}
