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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.smithy.model.Model;

/**
 * Service-specific edits to the Smithy model and the intermediate model. {@code SmithyIntermediateModelBuilder}
 * runs the customizations whose {@link #sdkIds()} contain the service's sdkId. Smithy does not validate their results,
 * so each customization needs its own tests.
 */
@SdkInternalApi
public interface SmithyServiceCustomization {

    /**
     * The sdkIds of the services this customization applies to.
     */
    Set<String> sdkIds();

    /**
     * Whether this customization applies the given {@code customization.config} setting itself. The
     * {@code smithy-build.json} generator reads this and leaves such settings out of the file it writes.
     * {@code shapeModifiers.<shape>.modify.<member>.required} stands for making a renamed member optional.
     */
    default boolean appliesConfig(String key) {
        return false;
    }

    /**
     * Edits the Smithy model after smithy-build has validated it, before translation.
     */
    default Model preprocess(Model model, CustomizationConfig config) {
        return model;
    }

    /**
     * Edits the intermediate model before unused shapes are removed.
     */
    default void postprocess(IntermediateModel model) {
    }
}
