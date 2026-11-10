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

package software.amazon.awssdk.codegen.smithy.service;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;

/**
 * Service-specific customization on the Smithy path, for differences from C2J that a validated smithy-build transform
 * cannot express. {@code SmithyIntermediateModelBuilder} runs the customizations whose {@link #sdkIds()} include the
 * service's sdkId: {@link #preprocess} on the model smithy-build validated, before translation, and
 * {@link #postprocess} on the intermediate model, before unused shapes are removed. Neither result is validated by
 * Smithy, so each customization needs its own tests.
 */
@SdkInternalApi
public interface SmithyServiceCustomization {

    /**
     * The sdkIds of the services this customization applies to.
     */
    java.util.Set<String> sdkIds();

    /**
     * True when this customization applies a {@code customization.config} setting itself, so the
     * {@code smithy-build.json} generator must not convert it. Keys are {@code endpointParameters},
     * {@code customOperationContextParams} and {@code shapeModifiers.<shape>.inject.<member>}. For
     * {@code shapeModifiers.<shape>.modify.<member>.required}, the generator still renames the member, and the
     * customization drops {@code required} from it as C2J does.
     */
    default boolean appliesConfig(String key) {
        return false;
    }

    default Model preprocess(Model model, CustomizationConfig config) {
        return model;
    }

    default void postprocess(IntermediateModel model) {
    }
}
