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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;

/**
 * Functional tests to verify that custom User-Agent headers provided via
 * {@link software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.Builder#putHeader(String, String)}
 * are preserved and not overwritten by the SDK's default User-Agent generation logic.
 */
class CustomUserAgentHeaderTest {

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String SDK_USER_AGENT_PREFIX = "aws-sdk-java";
    private static final String TEST_API_NAME = "TestApiName";
    private static final String TEST_API_VERSION = "1.0";

    private CapturingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new CapturingInterceptor();
    }

    @Test
    void execute_withoutCustomUserAgent_shouldAddSdkDefaultUserAgent() {

        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();
        executeRequestExpectingInterception(client);
        String userAgent = getCapturedUserAgent();
        assertThat(userAgent).contains(SDK_USER_AGENT_PREFIX);
    }

    @Test
    void execute_withEmptyCustomUserAgent_shouldPreserveEmptyValue() {

        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent("");
        executeRequestExpectingInterception(client);
        String userAgent = getCapturedUserAgent();
        assertThat(userAgent).isEmpty();
    }

    @ParameterizedTest(name = "{index}: userAgent={0}")
    @MethodSource("customUserAgentValues")
    void execute_withCustomUserAgent_shouldPreserveAndNotOverwrite(String customUserAgent) {

        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent(customUserAgent);
        executeRequestExpectingInterception(client);

        String userAgent = getCapturedUserAgent();
        assertThat(userAgent)
            .isEqualTo(customUserAgent)
            .doesNotContain(SDK_USER_AGENT_PREFIX);
    }

    private static Stream<Arguments> customUserAgentValues() {
        return Stream.of(
            Arguments.of("CustomUserAgentHeaderValue"),
            Arguments.of("MyApplication/1.0.0"),
            Arguments.of("CustomClient/2.0 (Linux; x86_64)")
        );
    }

    @Test
    void execute_withCustomUserAgentAndApiName_shouldNotAppendApiName() {

        String customUserAgent = "CustomUserAgentHeaderValue";
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent(customUserAgent);
        executeRequestWithApiName(client);
        String userAgent = getCapturedUserAgent();
        assertThat(userAgent)
            .isEqualTo(customUserAgent)
            .doesNotContain(TEST_API_NAME);
    }

    @Test
    void execute_withoutCustomUserAgentAndWithApiName_shouldAppendApiName() {

        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();
        executeRequestWithApiName(client);
        String userAgent = getCapturedUserAgent();
        assertThat(userAgent).contains(TEST_API_NAME + "/" + TEST_API_VERSION);
    }

    private RestJsonEndpointProvidersClientBuilder defaultClientBuilder() {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(StaticCredentialsProvider.create(
                                                  AwsBasicCredentials.create("akid", "skid")))
                                              .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor));
    }

    private RestJsonEndpointProvidersClient clientWithCustomUserAgent(String customUserAgent) {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(StaticCredentialsProvider.create(
                                                  AwsBasicCredentials.create("akid", "skid")))
                                              .overrideConfiguration(c -> c
                                                  .addExecutionInterceptor(interceptor)
                                                  .putHeader(USER_AGENT_HEADER, customUserAgent))
                                              .build();
    }

    private void executeRequestExpectingInterception(RestJsonEndpointProvidersClient client) {
        assertThatThrownBy(() -> client.allTypes(r -> {}))
            .hasMessageContaining("stop");
    }

    private void executeRequestWithApiName(RestJsonEndpointProvidersClient client) {
        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o.addApiName(api -> api
                .name(TEST_API_NAME)
                .version(TEST_API_VERSION)))))
            .hasMessageContaining("stop");
    }

    private String getCapturedUserAgent() {
        Map<String, List<String>> headers = interceptor.context.httpRequest().headers();
        assertThat(headers).containsKey(USER_AGENT_HEADER);
        return headers.get(USER_AGENT_HEADER).get(0);
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            throw new RuntimeException("stop");
        }
    }
}
