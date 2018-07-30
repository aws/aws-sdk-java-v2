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

package software.amazon.awssdk.codegen;

import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.BasicCodeGenConfig;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.naming.DefaultNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;

/**
 * Constructs the metadata that is required for generating the java client from the service meta data.
 */
final class AddMetadata {
    private static final String AWS_PACKAGE_PREFIX = "software.amazon.awssdk.services";

    private AddMetadata() {
    }


    public static Metadata constructMetadata(ServiceModel serviceModel,
                                             BasicCodeGenConfig codeGenConfig,
                                             CustomizationConfig customizationConfig) {

        final Metadata metadata = new Metadata();

        final NamingStrategy namingStrategy = new DefaultNamingStrategy(serviceModel, customizationConfig);
        final ServiceMetadata serviceMetadata = serviceModel.getMetadata();

        final String serviceName;
        final String rootPackageName;

        // API Gateway uses additional codegen.config settings
        if (serviceMetadata.getProtocol().equals(Protocol.API_GATEWAY.getValue())) {
            // TODO: The meaning of root package name has changed a bit since this code was written. Specifically, the root for
            // AWS no longer includes the service name. This changed the behavior of the API gateway generation, but we're not
            // keeping it up to date at this time. Just be aware this has happened when updating the API gateway code.
            serviceName = codeGenConfig.getInterfaceName();
            rootPackageName = codeGenConfig.getPackageName();

            metadata.withDefaultEndpoint(codeGenConfig.getEndpoint())
                    .withDefaultEndpointWithoutHttpProtocol(
                            Utils.getDefaultEndpointWithoutHttpProtocol(codeGenConfig.getEndpoint()))
                    .withDefaultRegion(codeGenConfig.getDefaultRegion());
        } else {
            serviceName = namingStrategy.getServiceName();
            rootPackageName = AWS_PACKAGE_PREFIX;
        }

        metadata.withApiVersion(serviceMetadata.getApiVersion())
                .withAsyncClient(String.format(Constant.ASYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withAsyncInterface(String.format(Constant.ASYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withAsyncBuilder(String.format(Constant.ASYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withAsyncBuilderInterface(String.format(Constant.ASYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilderInterface(String.format(Constant.BASE_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilder(String.format(Constant.BASE_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withDocumentation(serviceModel.getDocumentation())
                .withRootPackageName(rootPackageName)
                .withClientPackageName(namingStrategy.getClientPackageName(serviceName))
                .withModelPackageName(namingStrategy.getModelPackageName(serviceName))
                .withTransformPackageName(namingStrategy.getTransformPackageName(serviceName))
                .withRequestTransformPackageName(namingStrategy.getRequestTransformPackageName(serviceName))
                .withPaginatorsPackageName(namingStrategy.getPaginatorsPackageName(serviceName))
                .withSmokeTestsPackageName(namingStrategy.getSmokeTestPackageName(serviceName))
                .withServiceAbbreviation(serviceMetadata.getServiceAbbreviation())
                .withServiceFullName(serviceMetadata.getServiceFullName())
                .withSyncClient(String.format(Constant.SYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withSyncInterface(String.format(Constant.SYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withSyncBuilder(String.format(Constant.SYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withSyncBuilderInterface(String.format(Constant.SYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseExceptionName(String.format(Constant.BASE_EXCEPTION_NAME_PATTERN, serviceName))
                .withBaseRequestName(String.format(Constant.BASE_REQUEST_NAME_PATTERN, serviceName))
                .withBaseResponseName(String.format(Constant.BASE_RESPONSE_NAME_PATTERN, serviceName))
                .withProtocol(Protocol.fromValue(serviceMetadata.getProtocol()))
                .withJsonVersion(serviceMetadata.getJsonVersion())
                .withEndpointPrefix(serviceMetadata.getEndpointPrefix())
                .withSigningName(serviceMetadata.getSigningName())
                .withAuthType(AuthType.fromValue(serviceMetadata.getSignatureVersion()))
                .withRequiresApiKey(requiresApiKey(serviceModel))
                .withUid(serviceMetadata.getUid());

        final String jsonVersion = getJsonVersion(metadata, serviceMetadata);
        metadata.setJsonVersion(jsonVersion);

        // TODO: iterate through all the operations and check whether any of
        // them accept stream input
        metadata.setHasApiWithStreamInput(false);
        return metadata;
    }

    private static String getJsonVersion(Metadata metadata, ServiceMetadata serviceMetadata) {
        // TODO this should be defaulted in the C2J build tool
        if (serviceMetadata.getJsonVersion() == null && metadata.isJsonProtocol()) {
            return "1.1";
        } else {
            return serviceMetadata.getJsonVersion();
        }
    }

    /**
     * If any operation requires an API key we generate a setter on the builder.
     *
     * @return True if any operation requires an API key. False otherwise.
     */
    private static boolean requiresApiKey(ServiceModel serviceModel) {
        return serviceModel.getOperations().values().stream()
                           .anyMatch(Operation::requiresApiKey);
    }
}
