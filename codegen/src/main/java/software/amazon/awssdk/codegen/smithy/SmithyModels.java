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

package software.amazon.awssdk.codegen.smithy;

import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.smithy.model.Model;

/**
 * Inputs consumed by {@link SmithyIntermediateModelBuilder}. Smithy counterpart to
 * {@code C2jModels}.
 *
 * <p>Paginators and waiters are not carried yet.
 */
public final class SmithyModels {

    private final Model model;
    private final CustomizationConfig customizationConfig;

    private SmithyModels(Model model, CustomizationConfig customizationConfig) {
        this.model = model;
        this.customizationConfig = customizationConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Model model() {
        return model;
    }

    public CustomizationConfig customizationConfig() {
        return customizationConfig;
    }

    public static final class Builder implements SdkBuilder<Builder, SmithyModels> {

        private Model model;
        private CustomizationConfig customizationConfig;

        private Builder() {
        }

        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        public Builder customizationConfig(CustomizationConfig customizationConfig) {
            this.customizationConfig = customizationConfig;
            return this;
        }

        @Override
        public SmithyModels build() {
            CustomizationConfig config = customizationConfig != null ? customizationConfig : CustomizationConfig.create();
            return new SmithyModels(model, config);
        }
    }
}
