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

import java.util.Arrays;
import java.util.Collections;
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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClientBuilder;

/**
 * Functional tests verifying custom User-Agent header preservation.
 *
 * <p>Tests ensure that User-Agent headers provided via
 * {@link software.amazon.awssdk.core.client.config.ClientOverrideConfiguration.Builder#putHeader(String, String)} are preserved
 * and not overwritten by SDK's default User-Agent generation logic.
 */
class CustomUserAgentHeaderTest {

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String SDK_USER_AGENT_PREFIX = "aws-sdk-java";
    private static final String TEST_API_NAME = "TestApiName";
    private static final String TEST_API_VERSION = "1.0";
    private static final String INTERCEPTOR_STOP_MESSAGE = "stop";

    private CapturingInterceptor interceptor;

    private static Stream<Arguments> customUserAgentValues() {
        return Stream.of(
            Arguments.of("CustomUserAgentHeaderValue"),
            Arguments.of("MyApplication/1.0.0"),
            Arguments.of("CustomClient/2.0 (Linux; x86_64)")
        );
    }

    private static Stream<Arguments> customUserAgentListValues() {
        return Stream.of(
            Arguments.of(Arrays.asList("Agent1")),
            Arguments.of(Arrays.asList("Agent1", "Agent2")),
            Arguments.of(Arrays.asList("CustomClient/1.0", "MyApp/2.0"))
        );
    }

    @BeforeEach
    void setUp() {
        interceptor = new CapturingInterceptor();
    }

    // ========== Default Behavior Tests ==========

    @Test
    void executeRequest_withoutCustomUserAgent_shouldAddSdkDefaultUserAgent() {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();
        executeRequestExpectingInterception(client);

        assertUserAgentContains(SDK_USER_AGENT_PREFIX);
    }

    // ========== Custom User-Agent Preservation Tests ==========

    @ParameterizedTest(name = "Custom User-Agent ''{0}'' should be preserved without SDK prefix")
    @MethodSource("customUserAgentValues")
    void executeRequest_withCustomUserAgent_shouldPreserveAndNotOverwrite(String customUserAgent) {
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent(customUserAgent);
        executeRequestExpectingInterception(client);

        String userAgent = getCapturedUserAgent();
        assertThat(userAgent)
            .isEqualTo(customUserAgent)
            .doesNotContain(SDK_USER_AGENT_PREFIX);
    }

    @ParameterizedTest(name = "Custom User-Agent list {0} should be preserved")
    @MethodSource("customUserAgentListValues")
    void executeRequest_withCustomUserAgentList_shouldPreserveAllValues(List<String> customUserAgentList) {
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgentList(customUserAgentList);
        executeRequestExpectingInterception(client);

        List<String> userAgentList = getCapturedUserAgentList();
        assertThat(userAgentList).isEqualTo(customUserAgentList);
    }

    // ========== Request-Level User-Agent Tests ==========

    @ParameterizedTest(name = "Request-level User-Agent ''{0}'' should be preserved")
    @MethodSource("customUserAgentValues")
    void executeRequest_withRequestLevelCustomUserAgent_shouldPreserveAndNotOverwrite(String customUserAgent) {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();

        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o.putHeader(USER_AGENT_HEADER, customUserAgent))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);

        String userAgent = getCapturedUserAgent();
        assertThat(userAgent)
            .isEqualTo(customUserAgent)
            .doesNotContain(SDK_USER_AGENT_PREFIX);
    }

    @ParameterizedTest(name = "Request-level User-Agent list {0} should be preserved")
    @MethodSource("customUserAgentListValues")
    void executeRequest_withRequestLevelCustomUserAgentList_shouldPreserveAllValues(List<String> customUserAgentList) {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();

        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o.putHeader(USER_AGENT_HEADER, customUserAgentList))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);

        List<String> userAgentList = getCapturedUserAgentList();
        assertThat(userAgentList).isEqualTo(customUserAgentList);
    }

    @Test
    void executeRequest_withRequestLevelCustomUserAgentAndApiName_shouldNotAppendApiName() {
        String customUserAgent = "CustomUserAgentHeaderValue";
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();

        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o
                .addApiName(api -> api.name(TEST_API_NAME).version(TEST_API_VERSION))
                .putHeader(USER_AGENT_HEADER, customUserAgent))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);

        String userAgent = getCapturedUserAgent();
        assertThat(userAgent)
            .isEqualTo(customUserAgent)
            .doesNotContain(TEST_API_NAME);
    }

    @ParameterizedTest(name = "Request-level User-Agent list {0} with API name should not append API name")
    @MethodSource("customUserAgentListValues")
    void executeRequest_withRequestLevelCustomUserAgentListAndApiName_shouldNotAppendApiName(List<String> customUserAgentList) {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();

        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o
                .addApiName(api -> api.name(TEST_API_NAME).version(TEST_API_VERSION))
                .putHeader(USER_AGENT_HEADER, customUserAgentList))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);

        List<String> userAgentList = getCapturedUserAgentList();
        assertThat(userAgentList).isEqualTo(customUserAgentList);
        assertThat(String.join(" ", userAgentList)).doesNotContain(TEST_API_NAME);
    }

    // ========== Header via Interceptors ==========

    @Test
    void executeRequest_withInterceptorAddingUserAgent_shouldAddSdkDefaultUserAgent() {
        RestJsonEndpointProvidersClient client =
            defaultClientBuilder().overrideConfiguration(o -> o
                .addExecutionInterceptor(interceptor)
                .addExecutionInterceptor(new ExecutionInterceptor() {
                    @Override
                    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                                            ExecutionAttributes executionAttributes) {
                        return context.httpRequest().toBuilder()
                                      .putHeader(USER_AGENT_HEADER, "custom-agent")
                                      .build();
                    }
                })).build();

        executeRequestExpectingInterception(client);
        assertUserAgentContains(SDK_USER_AGENT_PREFIX);
    }

    // ========== API Name Handling Tests ==========

    @Test
    void executeRequest_withCustomUserAgentAndApiName_shouldNotAppendApiName() {
        String customUserAgent = "CustomUserAgentHeaderValue";
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent(customUserAgent);
        executeRequestWithApiName(client);

        String userAgent = getCapturedUserAgent();
        assertThat(userAgent)
            .isEqualTo(customUserAgent)
            .doesNotContain(TEST_API_NAME);
    }

    @Test
    void executeRequest_withoutCustomUserAgentAndWithApiName_shouldAppendApiName() {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();
        executeRequestWithApiName(client);

        assertUserAgentContains(TEST_API_NAME + "/" + TEST_API_VERSION);
    }

    // ========== Edge Case Tests ==========

    @Test
    void buildClient_withNullListUserAgent_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> clientWithCustomUserAgentList(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("values must not be null");
    }

    @Test
    void executeRequest_withEmptyListUserAgent_shouldResultInSdkUserAgentHeader() {
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgentList(Collections.emptyList());
        executeRequestExpectingInterception(client);

        List<String> userAgentList = getCapturedUserAgentList();
        assertThat(userAgentList).isNull();
    }

    @Test
    void executeRequest_withEmptyCustomUserAgent_shouldStoreSdkUserAgent() {
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent("");
        executeRequestExpectingInterception(client);

        assertUserAgentContains("");
    }

    @Test
    void executeRequest_withNullStringUserAgent_shouldStoreAsSdkUserAgent() {
        RestJsonEndpointProvidersClient client = clientWithCustomUserAgent(null);
        executeRequestExpectingInterception(client);

        List<String> userAgentList = getCapturedUserAgentList();
        assertThat(userAgentList)
            .hasSize(1)
            .allSatisfy(ua -> {
                assertThat(ua).isNull();
            });
    }

    @Test
    void executeRequest_withRequestLevelEmptyCustomUserAgent_shouldStoreEmptyUserAgent() {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();

        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o.putHeader(USER_AGENT_HEADER, ""))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);

        assertUserAgentContains("");
    }

    @Test
    void executeRequest_withRequestLevelEmptyListUserAgent_shouldResultInNoUserAgent() {
        RestJsonEndpointProvidersClient client = defaultClientBuilder().build();

        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o.putHeader(USER_AGENT_HEADER, Collections.emptyList()))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);

        List<String> userAgentList = getCapturedUserAgentList();
        assertThat(userAgentList).isNull();
    }

    // ========== Helper Methods ==========

    private void assertUserAgentContains(String expected) {
        assertThat(getCapturedUserAgent()).contains(expected);
    }

    private void executeRequestExpectingInterception(RestJsonEndpointProvidersClient client) {
        assertThatThrownBy(() -> client.allTypes(r -> {}))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);
    }

    private void executeRequestWithApiName(RestJsonEndpointProvidersClient client) {
        assertThatThrownBy(() -> client.allTypes(r -> r
            .overrideConfiguration(o -> o.addApiName(api -> api
                .name(TEST_API_NAME)
                .version(TEST_API_VERSION)))))
            .hasMessageContaining(INTERCEPTOR_STOP_MESSAGE);
    }

    private String getCapturedUserAgent() {
        Map<String, List<String>> headers = interceptor.context.httpRequest().headers();
        assertThat(headers).containsKey(USER_AGENT_HEADER);
        return headers.get(USER_AGENT_HEADER).get(0);
    }

    private List<String> getCapturedUserAgentList() {
        Map<String, List<String>> headers = interceptor.context.httpRequest().headers();
        return headers.get(USER_AGENT_HEADER);
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

    private RestJsonEndpointProvidersClient clientWithCustomUserAgentList(List<String> customUserAgentList) {
        return RestJsonEndpointProvidersClient.builder()
                                              .region(Region.US_WEST_2)
                                              .credentialsProvider(StaticCredentialsProvider.create(
                                                  AwsBasicCredentials.create("akid", "skid")))
                                              .overrideConfiguration(c -> c
                                                  .addExecutionInterceptor(interceptor)
                                                  .putHeader(USER_AGENT_HEADER, customUserAgentList))
                                              .build();
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            throw new RuntimeException(INTERCEPTOR_STOP_MESSAGE);
        }
    }
}
