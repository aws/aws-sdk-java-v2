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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.codegen.internal.Constant;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.codegen.utils.ProtocolUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Constructs the {@link Metadata} for the intermediate model from a Smithy service. The Smithy
 * counterpart to C2J's {@code AddMetadata}.
 *
 * <p>{@code serviceAbbreviation} has no Smithy equivalent and is left null, so
 * {@link Metadata#getDescriptiveServiceName()} falls back to the service full name. Javadoc difference
 * only.
 */
final class AddSmithyMetadata {
    private static final String AWS_PACKAGE_PREFIX = "software.amazon.awssdk.services";
    private static final String HTTP = "http";
    private static final String EVENT_STREAM_HTTP = "eventStreamHttp";
    private static final String H2 = "h2";

    private AddSmithyMetadata() {
    }

    static Metadata constructMetadata(Model model,
                                      ServiceShape service,
                                      ServiceIndex serviceIndex,
                                      NamingStrategy namingStrategy,
                                      CustomizationConfig customizationConfig) {
        Metadata metadata = new Metadata();

        String serviceName = namingStrategy.getServiceName();
        configurePackageName(metadata, namingStrategy, customizationConfig);

        String protocol = ProtocolUtils.resolveProtocol(serviceIndex, service);

        metadata.withApiVersion(service.getVersion())
                .withAsyncClient(String.format(Constant.ASYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withAsyncInterface(String.format(Constant.ASYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withAsyncBuilder(String.format(Constant.ASYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withAsyncBuilderInterface(String.format(Constant.ASYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilderInterface(String.format(Constant.BASE_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseBuilder(String.format(Constant.BASE_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withDocumentation(documentation(service))
                .withServiceAbbreviation(null)
                .withBatchmanagerPackageName(namingStrategy.getBatchManagerPackageName(serviceName))
                .withPresignedUrlPackageName(namingStrategy.getPresignedUrlPackageName(serviceName))
                .withServiceFullName(serviceFullName(service))
                .withServiceName(serviceName)
                .withSyncClient(String.format(Constant.SYNC_CLIENT_CLASS_NAME_PATTERN, serviceName))
                .withSyncInterface(String.format(Constant.SYNC_CLIENT_INTERFACE_NAME_PATTERN, serviceName))
                .withSyncBuilder(String.format(Constant.SYNC_BUILDER_CLASS_NAME_PATTERN, serviceName))
                .withSyncBuilderInterface(String.format(Constant.SYNC_BUILDER_INTERFACE_NAME_PATTERN, serviceName))
                .withBaseExceptionName(String.format(Constant.BASE_EXCEPTION_NAME_PATTERN, serviceName))
                .withBaseRequestName(String.format(Constant.BASE_REQUEST_NAME_PATTERN, serviceName))
                .withBaseResponseName(String.format(Constant.BASE_RESPONSE_NAME_PATTERN, serviceName))
                .withProtocol(Protocol.fromValue(protocol))
                .withEndpointPrefix(endpointPrefix(service))
                .withSigningName(namingStrategy.getSigningName())
                .withAuthType(authType(service))
                .withUid(uid(service))
                .withServiceId(serviceId(service))
                .withSupportsH2(supportsH2(serviceIndex, service))
                .withAwsQueryCompatible(awsQueryCompatible(service))
                .withAuth(auth(service));

        metadata.withJsonVersion(jsonVersion(metadata, service));

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

    private static Optional<Pair<String, String>> splitCustomRootPackageName(String rootPackageName) {
        if (rootPackageName == null) {
            return Optional.empty();
        }
        int i = rootPackageName.lastIndexOf('.');
        return Optional.of(Pair.of(rootPackageName.substring(0, i), rootPackageName.substring(i + 1)));
    }

    /**
     * Smithy has no {@code jsonVersion} field; the version is encoded in the protocol trait. So
     * awsJson1_0 is the only case that differs from C2J's {@code 1.1} default for JSON protocols.
     */
    private static String jsonVersion(Metadata metadata, ServiceShape service) {
        if (!metadata.isJsonProtocol()) {
            return null;
        }
        if (service.hasTrait(AwsJson1_0Trait.class)) {
            return "1.0";
        }
        return "1.1";
    }

    /**
     * C2J derives this from {@code protocolSettings.containsKey("h2")}. Smithy carries the equivalent
     * on the protocol trait: {@code http} lists supported HTTP versions, {@code eventStreamHttp}
     * those required for event streams.
     *
     * <p>Read off the trait node rather than via {@code AwsProtocolTrait} because
     * {@code Rpcv2CborTrait} inherits these members from a different base class, in a jar that is
     * only test-scoped here.
     */
    private static boolean supportsH2(ServiceIndex serviceIndex, ServiceShape service) {
        for (Trait protocolTrait : serviceIndex.getProtocols(service).values()) {
            Optional<ObjectNode> node = protocolTrait.toNode().asObjectNode();
            if (!node.isPresent()) {
                continue;
            }
            if (listsH2(node.get(), HTTP) || listsH2(node.get(), EVENT_STREAM_HTTP)) {
                return true;
            }
        }
        return false;
    }

    private static boolean listsH2(ObjectNode protocolTrait, String member) {
        return protocolTrait.getArrayMember(member)
                            .map(versions -> versions.getElementsAs(StringNode.class).stream()
                                                     .anyMatch(v -> H2.equals(v.getValue())))
                            .orElse(false);
    }

    /**
     * C2J carries an always-empty map as a marker; the Smithy equivalent is an annotation trait,
     * which carries no value at all.
     */
    private static Map<String, String> awsQueryCompatible(ServiceShape service) {
        return service.hasTrait(AwsQueryCompatibleTrait.class) ? new LinkedHashMap<>() : null;
    }

    private static String documentation(ServiceShape service) {
        return service.getTrait(DocumentationTrait.class).map(DocumentationTrait::getValue).orElse(null);
    }

    private static String serviceFullName(ServiceShape service) {
        return service.getTrait(TitleTrait.class).map(TitleTrait::getValue).orElse(null);
    }

    private static String serviceId(ServiceShape service) {
        return service.getTrait(ServiceTrait.class)
                      .map(ServiceTrait::getSdkId)
                      .orElseThrow(() -> new IllegalStateException(
                          "Service is missing @aws.api#service trait: " + service.getId()));
    }

    /**
     * C2J's {@code uid} builds the generated API-doc cross-links; Smithy's {@code docId} is declared
     * for that purpose. {@code resolveDocId} falls back to sdkId-plus-version, which models override
     * explicitly when that does not match.
     */
    private static String uid(ServiceShape service) {
        return service.getTrait(ServiceTrait.class)
                      .map(trait -> trait.resolveDocId(service))
                      .orElse(null);
    }

    private static String endpointPrefix(ServiceShape service) {
        return service.getTrait(ServiceTrait.class)
                      .map(ServiceTrait::getEndpointPrefix)
                      .orElse(null);
    }

    /**
     * The legacy single {@code authType}; C2J derives it from the service {@code signatureVersion}.
     */
    private static AuthType authType(ServiceShape service) {
        if (service.hasTrait(SigV4Trait.class)) {
            return AuthType.V4;
        }
        return null;
    }

    /**
     * C2J's {@code metadata.auth} is the service's effective auth, so the fallback matters: reading
     * only {@code @auth} would leave this empty for services that never declare it, such as EC2.
     */
    private static List<AuthType> auth(ServiceShape service) {
        List<AuthType> auth = new ArrayList<>();
        if (service.hasTrait(AuthTrait.class)) {
            for (ShapeId schemeId : service.expectTrait(AuthTrait.class).getValues()) {
                auth.add(AuthType.fromValue(schemeId.toString()));
            }
            return auth;
        }
        if (service.hasTrait(SigV4Trait.class)) {
            auth.add(AuthType.V4);
        } else if (service.hasTrait(HttpBearerAuthTrait.class)) {
            auth.add(AuthType.BEARER);
        }
        return auth;
    }
}
