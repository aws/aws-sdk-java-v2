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

package software.amazon.awssdk.codegen.poet;

import java.io.File;

import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.IntermediateModelBuilder;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

/**
 * A static set of service models that can be used for testing purposes.
 */
public class ClientTestModels {
    private ClientTestModels() {}

    public static IntermediateModel awsJsonServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/json/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/json/customization.config").getFile());
        File paginatorsModel = new File(ClientTestModels.class.getResource("client/c2j/json/paginators.json").getFile());
        C2jModels models = C2jModels.builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .paginatorsModel(getPaginatorsModel(paginatorsModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel awsQueryCompatibleJsonServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query-to-json-errorcode/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/query-to-json-errorcode/customization.config").getFile());
        File paginatorsModel = new File(ClientTestModels.class.getResource("client/c2j/query-to-json-errorcode/paginators.json").getFile());
        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .paginatorsModel(getPaginatorsModel(paginatorsModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel bearerAuthServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/json-bearer-auth/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/json-bearer-auth/customization.config").getFile());
        File paginatorsModel = new File(ClientTestModels.class.getResource("client/c2j/json-bearer-auth/paginators.json").getFile());
        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .paginatorsModel(getPaginatorsModel(paginatorsModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel restJsonServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/rest-json/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/rest-json/customization.config").getFile());
        File paginatorsModel = new File(ClientTestModels.class.getResource("client/c2j/rest-json/paginators.json").getFile());
        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .paginatorsModel(getPaginatorsModel(paginatorsModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel queryServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/query/customization.config").getFile());
        File waitersModel = new File(ClientTestModels.class.getResource("client/c2j/query/waiters-2.json").getFile());
        File endpointRuleSetModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-rule-set.json").getFile());
        File endpointTestsModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-tests.json").getFile());

        C2jModels models = C2jModels
                .builder()
                .serviceModel(getServiceModel(serviceModel))
                .customizationConfig(getCustomizationConfig(customizationModel))
                .waitersModel(getWaiters(waitersModel))
            .endpointRuleSetModel(getEndpointRuleSet(endpointRuleSetModel))
            .endpointTestSuiteModel(getEndpointTestSuite(endpointTestsModel))
                .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel stringArrayServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/stringarray/service-2.json").getFile());
        File endpointRuleSetModel =
            new File(ClientTestModels.class.getResource("client/c2j/stringarray/endpoint-rule-set.json").getFile());
        File endpointTestsModel =
            new File(ClientTestModels.class.getResource("client/c2j/stringarray/endpoint-tests.json").getFile());

        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(CustomizationConfig.create())
            .endpointRuleSetModel(getEndpointRuleSet(endpointRuleSetModel))
            .endpointTestSuiteModel(getEndpointTestSuite(endpointTestsModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel queryServiceModelsWithOverrideKnowProperties() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/query/customization-endpoint-know-prop.config").getFile());
        File waitersModel = new File(ClientTestModels.class.getResource("client/c2j/query/waiters-2.json").getFile());
        File endpointRuleSetModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-rule-set.json").getFile());
        File endpointTestsModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-tests.json").getFile());

        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .waitersModel(getWaiters(waitersModel))
            .endpointRuleSetModel(getEndpointRuleSet(endpointRuleSetModel))
            .endpointTestSuiteModel(getEndpointTestSuite(endpointTestsModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel queryServiceModelsEndpointAuthParamsWithAllowList() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/customization-endpoint-auth-params-with-allowed.config")
                                           .getFile());
        File waitersModel = new File(ClientTestModels.class.getResource("client/c2j/query/waiters-2.json").getFile());
        File endpointRuleSetModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-rule-set.json").getFile());
        File endpointTestsModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-tests.json").getFile());

        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .waitersModel(getWaiters(waitersModel))
            .endpointRuleSetModel(getEndpointRuleSet(endpointRuleSetModel))
            .endpointTestSuiteModel(getEndpointTestSuite(endpointTestsModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel queryServiceModelsEndpointAuthParamsWithoutAllowList() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/customization-endpoint-auth-params-without-allowed"
                                                        + ".config")
                                           .getFile());
        File waitersModel = new File(ClientTestModels.class.getResource("client/c2j/query/waiters-2.json").getFile());
        File endpointRuleSetModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-rule-set.json").getFile());
        File endpointTestsModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-tests.json").getFile());

        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .waitersModel(getWaiters(waitersModel))
            .endpointRuleSetModel(getEndpointRuleSet(endpointRuleSetModel))
            .endpointTestSuiteModel(getEndpointTestSuite(endpointTestsModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel queryServiceModelWithSpecialCustomization(String specialCustomization) {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/query/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/"+  specialCustomization).getFile());


        File waitersModel = new File(ClientTestModels.class.getResource("client/c2j/query/waiters-2.json").getFile());
        File endpointRuleSetModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-rule-set.json").getFile());
        File endpointTestsModel =
            new File(ClientTestModels.class.getResource("client/c2j/query/endpoint-tests.json").getFile());

        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .waitersModel(getWaiters(waitersModel))
            .endpointRuleSetModel(getEndpointRuleSet(endpointRuleSetModel))
            .endpointTestSuiteModel(getEndpointTestSuite(endpointTestsModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel granularAuthProvidersServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/fine-grained-auth/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/fine-grained-auth/customization.config")
                                           .getFile());

        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel granularAuthWithLegacyTraitServiceModels() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/fine-grained-auth-legacy-trait/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/fine-grained-auth-legacy-trait/customization.config")
                                           .getFile());
        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel allOperationsWithAuthSameValueServiceModels() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/all-ops-with-auth-same-value/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/all-ops-with-auth-same-value/customization.config")
                                           .getFile());
        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel allOperationsWithAuthDifferentValueServiceModels() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/all-ops-with-auth-different-value/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/all-ops-with-auth-different-value/customization.config")
                                           .getFile());
        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel operationWithNoAuth() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/ops-with-no-auth/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/ops-with-no-auth/customization.config")
                                           .getFile());
        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel serviceWithNoAuth() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/service-with-no-auth/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/service-with-no-auth/customization.config")
                                           .getFile());
        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel serviceMiniS3() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/mini-s3/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/mini-s3/customization.config")
                                           .getFile());
        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel xmlServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/xml/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/xml/customization.config").getFile());


        C2jModels models = C2jModels
            .builder()
            .serviceModel(getServiceModel(serviceModel))
            .customizationConfig(getCustomizationConfig(customizationModel))
            .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel endpointDiscoveryModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/endpointdiscovery/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/endpointdiscovery/customization.config").getFile());

        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel customContentTypeModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/customservicemetadata/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/customservicemetadata/customization.config").getFile());

        C2jModels models = C2jModels.builder()
                .serviceModel(getServiceModel(serviceModel))
                .customizationConfig(getCustomizationConfig(customizationModel))
                .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel customPackageModels() {
        File serviceModel =
            new File(ClientTestModels.class.getResource("client/c2j/custompackage/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/custompackage/customization.config").getFile());

        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel composedClientJsonServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/rest-json/service-2.json").getFile());
        File customizationModel =
            new File(ClientTestModels.class.getResource("client/c2j/composedclient/customization.config").getFile());
        CustomizationConfig customizationConfig = getCustomizationConfig(customizationModel);
        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(customizationConfig)
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel internalConfigModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/internalconfig/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/internalconfig/customization.config").getFile());

        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel rpcv2ServiceModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/rpcv2/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/rpcv2/customization.config").getFile());
        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
    }

    public static IntermediateModel batchManagerModels() {
        File serviceModel = new File(ClientTestModels.class.getResource("client/c2j/batchmanager/service-2.json").getFile());
        File customizationModel = new File(ClientTestModels.class.getResource("client/c2j/batchmanager/customization.config").getFile());

        C2jModels models = C2jModels.builder()
                                    .serviceModel(getServiceModel(serviceModel))
                                    .customizationConfig(getCustomizationConfig(customizationModel))
                                    .build();

        return new IntermediateModelBuilder(models).build();
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

    private static EndpointRuleSetModel getEndpointRuleSet(File file) {
        return ModelLoaderUtils.loadModel(EndpointRuleSetModel.class, file);
    }

    private static EndpointTestSuiteModel getEndpointTestSuite(File file) {
        return ModelLoaderUtils.loadModel(EndpointTestSuiteModel.class, file);
    }

    private static Paginators getPaginatorsModel(File file) {
        return ModelLoaderUtils.loadModel(Paginators.class, file);
    }
}
