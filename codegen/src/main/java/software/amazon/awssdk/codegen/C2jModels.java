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

package software.amazon.awssdk.codegen;

import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Container for service models and config files.
 */
public class C2jModels {

    private final ServiceModel serviceModel;
    private final Waiters waitersModel;
    private final CustomizationConfig customizationConfig;
    private final Paginators paginatorsModel;

    private C2jModels(ServiceModel serviceModel,
                      Waiters waitersModel,
                      CustomizationConfig customizationConfig,
                      Paginators paginatorsModel) {
        this.serviceModel = serviceModel;
        this.waitersModel = waitersModel;
        this.customizationConfig = customizationConfig;
        this.paginatorsModel = paginatorsModel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ServiceModel serviceModel() {
        return serviceModel;
    }

    public Waiters waitersModel() {
        return waitersModel;
    }

    public CustomizationConfig customizationConfig() {
        return customizationConfig;
    }

    public Paginators paginatorsModel() {
        return paginatorsModel;
    }

    public static class Builder implements SdkBuilder<Builder, C2jModels> {

        private ServiceModel serviceModel;
        private Waiters waitersModel;
        private CustomizationConfig customizationConfig;
        private Paginators paginatorsModel;

        private Builder() {
        }

        public Builder serviceModel(ServiceModel serviceModel) {
            this.serviceModel = serviceModel;
            return this;
        }

        public Builder waitersModel(Waiters waitersModel) {
            this.waitersModel = waitersModel;
            return this;
        }

        public Builder customizationConfig(CustomizationConfig customizationConfig) {
            this.customizationConfig = customizationConfig;
            return this;
        }

        public Builder paginatorsModel(Paginators paginatorsModel) {
            this.paginatorsModel = paginatorsModel;
            return this;
        }

        public C2jModels build() {
            Waiters waiters = waitersModel != null ? waitersModel : Waiters.none();
            Paginators paginators = paginatorsModel != null ? paginatorsModel : Paginators.none();
            return new C2jModels(serviceModel, waiters, customizationConfig, paginators);
        }
    }
}
