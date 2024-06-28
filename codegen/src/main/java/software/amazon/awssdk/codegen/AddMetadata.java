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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.naming.DefaultNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Constructs the metadata that is required for generating the java client from the service meta data.
 */
final class AddMetadata {
    private static final String AWS_PACKAGE_PREFIX = "software.amazon.awssdk.services";

    private AddMetadata() {
    }


    public static Metadata constructMetadata(ServiceModel serviceModel,
                                             CustomizationConfig customizationConfig) {
        Metadata metadata = new Metadata();

        NamingStrategy namingStrategy = new DefaultNamingStrategy(serviceModel, customizationConfig);
        ServiceMetadata serviceMetadata = serviceModel.getMetadata();
        String serviceName = namingStrategy.getServiceName();

        configurePackageName(metadata, namingStrategy, customizationConfig);

        metadata.withApiVersion(serviceMetadata.getApiVersion())
                .withAsyncClient(String.format(Constant.ASYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withAsyncInterface(String.format(Constant.ASYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withAsyncBuilder(String.format(Constant.ASYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withAsyncBuilderInterface(String.format(Constant.ASYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilderInterface(String.format(Constant.BASE_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilder(String.format(Constant.BASE_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withDocumentation(serviceModel.getDocumentation())
                .withServiceAbbreviation(serviceMetadata.getServiceAbbreviation())
                .withBatchmanagerPackageName(namingStrategy.getBatchManagerPackageName(serviceName))
                .withServiceFullName(serviceMetadata.getServiceFullName())
                .withServiceName(serviceName)
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
                .withUid(serviceMetadata.getUid())
                .withServiceId(serviceMetadata.getServiceId())
                .withSupportsH2(supportsH2(serviceMetadata))
                .withJsonVersion(getJsonVersion(metadata, serviceMetadata))
                .withAwsQueryCompatible(serviceMetadata.getAwsQueryCompatible())
                .withAuth(getAuthFromServiceMetadata(serviceMetadata, customizationConfig.useMultiAuth()));

        return metadata;
    }

    private static void configurePackageName(Metadata metadata,
                                             NamingStrategy namingStrategy,
                                             CustomizationConfig customizationConfig) {
        String packageName = customizationConfig.getRootPackageName();

        Optional<Pair<String, String>> packageNamePair = splitCustomRootPackageName(packageName);
        String rootPackageWithoutServiceId = packageNamePair.map(pkg -> StringUtils.lowerCase(pkg.left()))
                                                            .orElse(AWS_PACKAGE_PREFIX);

        String service = packageNamePair.map(pkg -> StringUtils.lowerCase(pkg.right()))
                                        .orElse(namingStrategy.getServiceName());

        metadata.withRootPackageName(rootPackageWithoutServiceId)
                .withClientPackageName(namingStrategy.getClientPackageName(service))
                .withModelPackageName(namingStrategy.getModelPackageName(service))
                .withTransformPackageName(namingStrategy.getTransformPackageName(service))
                .withRequestTransformPackageName(namingStrategy.getRequestTransformPackageName(service))
                .withPaginatorsPackageName(namingStrategy.getPaginatorsPackageName(service))
                .withWaitersPackageName(namingStrategy.getWaitersPackageName(service))
                .withEndpointRulesPackageName(namingStrategy.getEndpointRulesPackageName(service))
                .withAuthSchemePackageName(namingStrategy.getAuthSchemePackageName(service))
                .withJmesPathPackageName(namingStrategy.getJmesPathPackageName(service));
    }

    /**
     * Split the root package to [prefix].[suffix] pair. For example: "software.amazon.awssdk.services.s3" will be split into
     * "software.amazon.awssdk.services" and "s3".
     */
    public static Optional<Pair<String, String>> splitCustomRootPackageName(String rootPackageName) {
        if (rootPackageName == null) {
            return Optional.empty();
        }

        int i = rootPackageName.lastIndexOf('.');
        return Optional.of(Pair.of(rootPackageName.substring(0, i), rootPackageName.substring(i + 1, rootPackageName.length())));
    }

    private static boolean supportsH2(ServiceMetadata serviceMetadata) {
        return serviceMetadata.getProtocolSettings() != null && serviceMetadata.getProtocolSettings().containsKey("h2");
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
     * Converts service metadata into a list of AuthTypes. If useMultiAuth is enabled, then
     * {@code metadata.auth} will be used in the conversion if present. Otherwise, use
     * {@code metadata.signatureVersion}.
     */
    private static List<AuthType> getAuthFromServiceMetadata(ServiceMetadata serviceMetadata,
                                                             boolean useMultiAuth) {
        if (useMultiAuth) {
            List<String> serviceAuth = serviceMetadata.getAuth();
            if (serviceAuth != null) {
                return serviceAuth.stream().map(AuthType::fromValue).collect(Collectors.toList());
            }
        }
        return Collections.singletonList(AuthType.fromValue(serviceMetadata.getSignatureVersion()));
    }
}
