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

package software.amazon.awssdk.codegen.customization.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * The Smithy path's counterpart to the postprocess half of {@link DefaultCustomizationProcessor}. It runs the
 * customizations that edit the {@link IntermediateModel} rather than the source model, in the same order as the C2J
 * chain. Source-model edits are applied earlier, as smithy-build transforms.
 */
@SdkInternalApi
public final class SmithyIntermediateModelPostprocessor {

    private final List<CodegenCustomizationProcessor> processors;

    private SmithyIntermediateModelPostprocessor(List<CodegenCustomizationProcessor> processors) {
        this.processors = Collections.unmodifiableList(processors);
    }

    public static SmithyIntermediateModelPostprocessor create(CustomizationConfig config) {
        List<CodegenCustomizationProcessor> processors = new ArrayList<>();
        processors.add(new RemoveExceptionMessagePropertyProcessor());
        processors.add(new UseLegacyEventGenerationSchemeProcessor());
        processors.add(new LongPollingOperationProcessor());
        return new SmithyIntermediateModelPostprocessor(processors);
    }

    public void postprocess(IntermediateModel intermediateModel) {
        for (CodegenCustomizationProcessor processor : processors) {
            processor.postprocess(intermediateModel);
        }
    }
}
