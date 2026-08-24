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

package software.amazon.awssdk.codegen.naming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.ShareModelConfig;
import software.amazon.awssdk.codegen.model.config.customization.UnderscoresInNameBehavior;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Verifies {@link DefaultSmithyNamingStrategy} sources service-level metadata
 * from Smithy traits ({@code @aws.api#service}, {@code @aws.auth#sigv4}) in a
 * way that produces the same names as {@link DefaultNamingStrategy} does from
 * a C2J {@link ServiceMetadata}. Non-model-sourced logic (reserved-name checks,
 * enum-suffix rules, string transforms) is shared with {@link DefaultNamingStrategy}
 * and covered by {@link DefaultNamingStrategyTest}.
 */
class DefaultSmithyNamingStrategyTest {

    private static final String SHARING_SERVICE = "dynamodbstreams";

    private static DefaultNamingStrategy c2j(String serviceId, String signingName) {
        return c2j(serviceId, signingName, CustomizationConfig.create());
    }

    private static DefaultNamingStrategy c2j(String serviceId, String signingName, CustomizationConfig customization) {
        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setServiceId(serviceId);
        metadata.setSigningName(signingName);
        metadata.setEndpointPrefix(signingName);
        ServiceModel model = new ServiceModel();
        model.setMetadata(metadata);
        return new DefaultNamingStrategy(model, customization);
    }

    private static DefaultSmithyNamingStrategy smithy(String serviceId, String signingName) {
        return smithy(serviceId, signingName, CustomizationConfig.create());
    }

    private static DefaultSmithyNamingStrategy smithy(String serviceId, String signingName,
                                                      CustomizationConfig customization) {
        String idl =
            "$version: \"2\"\n"
            + "namespace com.example\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + "@service(sdkId: \"" + serviceId + "\", arnNamespace: \"" + signingName + "\")\n"
            + "@sigv4(name: \"" + signingName + "\")\n"
            + "service Widgets {\n"
            + "    version: \"2020-01-01\"\n"
            + "}\n";
        Model model = Model.assembler(DefaultSmithyNamingStrategyTest.class.getClassLoader())
                           .discoverModels(DefaultSmithyNamingStrategyTest.class.getClassLoader())
                           .addUnparsedModel("widgets.smithy", idl)
                           .assemble()
                           .unwrap();
        ServiceShape service = model.expectShape(ShapeId.from("com.example#Widgets"), ServiceShape.class);
        return new DefaultSmithyNamingStrategy(model, service, customization);
    }

    private static NamingPair pair(String serviceId, String signingName) {
        return pair(serviceId, signingName, CustomizationConfig.create());
    }

    private static NamingPair pair(String serviceId, String signingName, CustomizationConfig customization) {
        return new NamingPair(c2j(serviceId, signingName, customization),
                              smithy(serviceId, signingName, customization));
    }

    private static CustomizationConfig shareModelsWith(String shareModelWith, String packageName) {
        ShareModelConfig shareModelConfig = new ShareModelConfig();
        shareModelConfig.setShareModelWith(shareModelWith);
        shareModelConfig.setPackageName(packageName);
        CustomizationConfig customization = CustomizationConfig.create();
        customization.setShareModelConfig(shareModelConfig);
        return customization;
    }

    private static IntermediateModel modelWithAsyncBuilderInterface(String name) {
        Metadata metadata = new Metadata();
        metadata.setAsyncBuilderInterface(name);
        IntermediateModel model = new IntermediateModel();
        model.setMetadata(metadata);
        return model;
    }

    private static final class NamingPair {
        final NamingStrategy c2j;
        final NamingStrategy smithy;

        NamingPair(NamingStrategy c2j, NamingStrategy smithy) {
            this.c2j = c2j;
            this.smithy = smithy;
        }
    }

    /**
     * Every service-level accessor derives its value from either
     * {@code @aws.api#service.sdkId} or {@code @aws.auth#sigv4.name}. If either
     * trait is read wrong, this parameterized test flags it.
     */
    static Stream<Arguments> serviceLevelNamers() {
        return Stream.of(
            Arguments.of("getServiceName",
                         (Function<NamingStrategy, String>) NamingStrategy::getServiceName),
            Arguments.of("getServiceNameForEnvironmentVariables",
                         (Function<NamingStrategy, String>) NamingStrategy::getServiceNameForEnvironmentVariables),
            Arguments.of("getServiceNameForProfileFile",
                         (Function<NamingStrategy, String>) NamingStrategy::getServiceNameForProfileFile),
            Arguments.of("getSigningName",
                         (Function<NamingStrategy, String>) NamingStrategy::getSigningName),
            Arguments.of("getSigningNameForEnvironmentVariables",
                         (Function<NamingStrategy, String>) NamingStrategy::getSigningNameForEnvironmentVariables),
            Arguments.of("getSigningNameForSystemProperties",
                         (Function<NamingStrategy, String>) NamingStrategy::getSigningNameForSystemProperties)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serviceLevelNamers")
    void serviceLevelName_matchesC2j(String label, Function<NamingStrategy, String> extract) {
        NamingPair p = pair("DynamoDB", "dynamodb");
        assertThat(extract.apply(p.smithy)).isEqualTo(extract.apply(p.c2j));
    }

    @Test
    void getServiceName_returnsPascalCasedSdkId() {
        assertThat(pair("DynamoDB", "dynamodb").smithy.getServiceName()).isEqualTo("DynamoDb");
    }

    @Test
    void getServiceName_stripsServiceSuffixForGrandfatheredIds() {
        // "Directory Service" is in SdkServiceIdValidator's PREEXISTING_SERVICE_IDS
        // exemption list, so it bypasses the "must not end with service" check and
        // still exercises the trailing-"service" strip in the naming strategy.
        assertThat(pair("Directory Service", "ds").smithy.getServiceName()).isEqualTo("Directory");
    }

    /**
     * Lists all twelve package-name methods so that one forgetting the {@code shareModelConfig}
     * redirect is caught, even though only two services in the SDK share models.
     */
    static Stream<Arguments> packageNamers() {
        return Stream.of(
            Arguments.of("getClientPackageName",
                         (Function<NamingStrategy, String>) s -> s.getClientPackageName(SHARING_SERVICE)),
            Arguments.of("getModelPackageName",
                         (Function<NamingStrategy, String>) s -> s.getModelPackageName(SHARING_SERVICE)),
            Arguments.of("getTransformPackageName",
                         (Function<NamingStrategy, String>) s -> s.getTransformPackageName(SHARING_SERVICE)),
            Arguments.of("getRequestTransformPackageName",
                         (Function<NamingStrategy, String>) s -> s.getRequestTransformPackageName(SHARING_SERVICE)),
            Arguments.of("getPaginatorsPackageName",
                         (Function<NamingStrategy, String>) s -> s.getPaginatorsPackageName(SHARING_SERVICE)),
            Arguments.of("getWaitersPackageName",
                         (Function<NamingStrategy, String>) s -> s.getWaitersPackageName(SHARING_SERVICE)),
            Arguments.of("getEndpointRulesPackageName",
                         (Function<NamingStrategy, String>) s -> s.getEndpointRulesPackageName(SHARING_SERVICE)),
            Arguments.of("getPresignedUrlPackageName",
                         (Function<NamingStrategy, String>) s -> s.getPresignedUrlPackageName(SHARING_SERVICE)),
            Arguments.of("getAuthSchemePackageName",
                         (Function<NamingStrategy, String>) s -> s.getAuthSchemePackageName(SHARING_SERVICE)),
            Arguments.of("getJmesPathPackageName",
                         (Function<NamingStrategy, String>) s -> s.getJmesPathPackageName(SHARING_SERVICE)),
            Arguments.of("getBatchManagerPackageName",
                         (Function<NamingStrategy, String>) s -> s.getBatchManagerPackageName(SHARING_SERVICE)),
            Arguments.of("getSmokeTestPackageName",
                         (Function<NamingStrategy, String>) s -> s.getSmokeTestPackageName(SHARING_SERVICE))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packageNamers")
    void packageName_whenSharingModels_matchesC2j(String label, Function<NamingStrategy, String> extract) {
        NamingPair p = pair("DynamoDB Streams", "dynamodb", shareModelsWith("dynamodb", "streams"));
        assertThat(extract.apply(p.smithy)).isEqualTo(extract.apply(p.c2j));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packageNamers")
    void packageName_whenNotSharingModels_matchesC2j(String label, Function<NamingStrategy, String> extract) {
        NamingPair p = pair("DynamoDB Streams", "dynamodb");
        assertThat(extract.apply(p.smithy)).isEqualTo(extract.apply(p.c2j));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packageNamers")
    void packageName_whenSharingModelsWithoutPackageName_matchesC2j(String label,
                                                                   Function<NamingStrategy, String> extract) {
        NamingPair p = pair("DynamoDB Streams", "dynamodb", shareModelsWith("dynamodb", null));
        assertThat(extract.apply(p.smithy)).isEqualTo(extract.apply(p.c2j));
    }

    /**
     * Pins both mechanisms to concrete values, so a change breaking both strategies in the same way is
     * still caught. The other assertions in this class are differential and would not be.
     */
    @Test
    void packageName_whenSharingModels_nestsAllButModelAndTransform() {
        NamingStrategy strategy = smithy("DynamoDB Streams", "dynamodb", shareModelsWith("dynamodb", "streams"));

        assertThat(strategy.getClientPackageName(SHARING_SERVICE)).isEqualTo("dynamodb.streams");
        assertThat(strategy.getRequestTransformPackageName(SHARING_SERVICE)).isEqualTo("dynamodb.streams.transform");
        assertThat(strategy.getModelPackageName(SHARING_SERVICE)).isEqualTo("dynamodb.model");
        assertThat(strategy.getTransformPackageName(SHARING_SERVICE)).isEqualTo("dynamodb.transform");
    }

    @Test
    void validateCustomerVisibleNaming_underscoreWithNoBehaviorSet_throws() {
        NamingStrategy strategy = smithy("DynamoDB", "dynamodb");

        assertThatThrownBy(() -> strategy.validateCustomerVisibleNaming(modelWithAsyncBuilderInterface("foo_bar")))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateCustomerVisibleNaming_underscoreWithAllowBehavior_passes() {
        CustomizationConfig customization =
            CustomizationConfig.create().withUnderscoresInShapeNameBehavior(UnderscoresInNameBehavior.ALLOW);
        NamingStrategy strategy = smithy("DynamoDB", "dynamodb", customization);

        strategy.validateCustomerVisibleNaming(modelWithAsyncBuilderInterface("foo_bar"));
    }

    @Test
    void validateCustomerVisibleNaming_underscoreOnAllowlist_passes() {
        CustomizationConfig customization = CustomizationConfig.create();
        customization.setAllowedUnderscoreNames(Arrays.asList("checksumXXHASH3_64", "foo_bar"));
        NamingStrategy strategy = smithy("DynamoDB", "dynamodb", customization);

        strategy.validateCustomerVisibleNaming(modelWithAsyncBuilderInterface("foo_bar"));
    }

    @Test
    void validateCustomerVisibleNaming_underscoreOffAllowlist_throws() {
        CustomizationConfig customization = CustomizationConfig.create();
        customization.setAllowedUnderscoreNames(Arrays.asList("checksumXXHASH3_64", "foo_bar"));
        NamingStrategy strategy = smithy("DynamoDB", "dynamodb", customization);

        assertThatThrownBy(() -> strategy.validateCustomerVisibleNaming(modelWithAsyncBuilderInterface("fizz_buzz")))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateCustomerVisibleNaming_nameIsNotALegalJavaIdentifier_throws() {
        NamingStrategy strategy = smithy("DynamoDB", "dynamodb");

        assertThatThrownBy(() -> strategy.validateCustomerVisibleNaming(modelWithAsyncBuilderInterface("foo-bar")))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void validateCustomerVisibleNaming_idiomaticName_passes() {
        NamingStrategy strategy = smithy("DynamoDB", "dynamodb");

        strategy.validateCustomerVisibleNaming(modelWithAsyncBuilderInterface("DynamoDbAsyncClientBuilder"));
    }
}
