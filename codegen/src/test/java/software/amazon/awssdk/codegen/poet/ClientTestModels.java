/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * A static set of service models that can be used for testing purposes.
 */
public class ClientTestModels {
    private ClientTestModels() {}

    public static IntermediateModel jsonServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/json/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/json/customization.config").getFile());
        File paginatorsModel = new File(ClientTestModels.class.getResource("client/c2j/json/paginators.json").getFile());
        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .paginatorsModel(getPaginatorsModel(paginatorsModel))
                                    .build();

        try {
            return new IntermediateModelBuilder(models).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static IntermediateModel queryServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/query/customization.config").getFile());
        File waitersModel = new File(ClientTestModels.class.getResource("client/c2j/query/waiters-2.json").getFile());

        C2jModels models = C2jModels
                .builder()
                .serviceModel(getServiceModel(serviceModel))
                .customizationConfig(getCustomizationConfig(customizationModel))
                .waitersModel(getWaiters(waitersModel))
                .build();

        try {
            return new IntermediateModelBuilder(models).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ServiceModel getServiceModel(File file) {
        return ModelLoaderUtils.loadModel(ServiceModel.class, file);
    }

    private static CustomizationConfig getCustomizationConfig(File file) {
        return ModelLoaderUtils.loadModel(CustomizationConfig.class, file);
    }

    private static Waiters getWaiters(File file) {
        return ModelLoaderUtils.loadModel(Waiters.class, file);
    }

    private static Paginators getPaginatorsModel(File file) {
        return ModelLoaderUtils.loadModel(Paginators.class, file);
    }
}
