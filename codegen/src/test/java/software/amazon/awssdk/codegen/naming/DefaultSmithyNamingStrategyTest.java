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

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
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

    private static DefaultNamingStrategy c2j(String serviceId, String signingName) {
        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setServiceId(serviceId);
        metadata.setSigningName(signingName);
        metadata.setEndpointPrefix(signingName);
        ServiceModel model = new ServiceModel();
        model.setMetadata(metadata);
        return new DefaultNamingStrategy(model, CustomizationConfig.create());
    }

    private static DefaultSmithyNamingStrategy smithy(String serviceId, String signingName) {
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
        return new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
    }

    private static NamingPair pair(String serviceId, String signingName) {
        return new NamingPair(c2j(serviceId, signingName), smithy(serviceId, signingName));
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
}
