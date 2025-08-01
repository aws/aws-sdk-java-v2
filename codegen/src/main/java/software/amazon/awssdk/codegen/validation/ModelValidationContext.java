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

package software.amazon.awssdk.codegen.validation;

import java.util.Optional;
import software.amazon.awssdk.codegen.model.config.customization.ShareModelConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Context object for {@link ModelValidator}s. This object contains all the information available to the validations in order
 * for them to perform their tasks.
 */
public final class ModelValidationContext {
    private final IntermediateModel intermediateModel;
    private final IntermediateModel shareModelsTarget;

    private ModelValidationContext(Builder builder) {
        this.intermediateModel = builder.intermediateModel;
        this.shareModelsTarget = builder.shareModelsTarget;
    }

    /**
     * The service model for which code is being generated.
     */
    public IntermediateModel intermediateModel() {
        return intermediateModel;
    }

    /**
     * The model of the service that the currently generating service shares models with. In other words, this is the service
     * model for the service defined in {@link ShareModelConfig#getShareModelWith()}.
     */
    public Optional<IntermediateModel> shareModelsTarget() {
        return Optional.ofNullable(shareModelsTarget);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IntermediateModel intermediateModel;
        private IntermediateModel shareModelsTarget;

        /**
         * The service model for which code is being generated.
         */
        public Builder intermediateModel(IntermediateModel intermediateModel) {
            this.intermediateModel = intermediateModel;
            return this;
        }

        /**
         * The model of the service that the currently generating service shares models with. In other words, this is the service
         * model for the service defined in {@link ShareModelConfig#getShareModelWith()}.
         */
        public Builder shareModelsTarget(IntermediateModel shareModelsTarget) {
            this.shareModelsTarget = shareModelsTarget;
            return this;
        }

        public ModelValidationContext build() {
            return new ModelValidationContext(this);
        }
    }
}
