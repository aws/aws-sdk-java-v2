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

package software.amazon.awssdk.services.useragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;
import software.amazon.awssdk.utils.StringUtils;

class AppIdUserAgentTest {
    private CapturingInterceptor interceptor;

    private static final String USER_AGENT_HEADER_NAME = "User-Agent";

    @BeforeEach
    public void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty(SdkSystemSetting.AWS_SDK_UA_APP_ID.property());
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void resolveAppIdFromEnvironment(String description, String clientAppId, String systemProperty, String expected) {
        if (!StringUtils.isEmpty(systemProperty)) {
            System.setProperty(SdkSystemSetting.AWS_SDK_UA_APP_ID.property(), systemProperty);
        }

        RestJsonEndpointProvidersClientBuilder clientBuilder = syncClientBuilder();

        if (!StringUtils.isEmpty(clientAppId)) {
            ClientOverrideConfiguration config = clientBuilder.overrideConfiguration().toBuilder().appId(clientAppId).build();
            clientBuilder.overrideConfiguration(config);
        }

        assertThatThrownBy(() -> clientBuilder.build().allTypes(r -> {}))
            .hasMessageContaining("stop");

        Map<String, List<String>> headers = interceptor.context.httpRequest().headers();
        assertThat(headers).containsKey(USER_AGENT_HEADER_NAME);
        String userAgent = headers.get(USER_AGENT_HEADER_NAME).get(0);

        if (expected != null) {
            assertThat(userAgent).contains("app/" + expected);
        } else {
            assertThat(userAgent).doesNotContain("app/");
        }
    }

    private static Stream<Arguments> inputValues() {
        return Stream.of(
            Arguments.of("Without appId input, nothing is added to user agent", null, null, null),
            Arguments.of("Values resolved from environment are propagated to user agent", null,
                         "SystemPropertyAppId", "SystemPropertyAppId"),
            Arguments.of("Client value is propagated to user agent", "ClientAppId", null, "ClientAppId"),
            Arguments.of("Client value takes precedence over environment values", "ClientAppId", "SystemPropertyAppId",
                         "ClientAppId")
        );
    }

    private RestJsonEndpointProvidersClientBuilder syncClientBuilder() {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(
                                                  StaticCredentialsProvider.create(
                                                      AwsBasicCredentials.create("akid", "skid")))
                                              .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("stop");
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }
    }
}
