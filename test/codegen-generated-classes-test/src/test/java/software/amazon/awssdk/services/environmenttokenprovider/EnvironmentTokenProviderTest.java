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

package software.amazon.awssdk.services.environmenttokenprovider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.environmenttokenprovider.auth.scheme.EnvironmentTokenProviderAuthSchemeProvider;
import software.amazon.awssdk.services.environmenttokenprovider.model.OneOperationRequest;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class EnvironmentTokenProviderTest {
    private static final String ENV_NAME = "AWS_BEARER_TOKEN_ENVIRONMENT_TOKEN";
    private static final String SYSTEM_PROPERTY_NAME = "aws.bearerTokenEnvironmentToken";
    public static final String ENV_TOKEN = "env-test-token";
    public static final String SYSTEM_TEST_TOKEN = "system-test-token";

    private MockSyncHttpClient mockHttpClient;
    private MockAsyncHttpClient mockAsyncHttpClient;
    private String systemPropertyBeforeTest;

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @BeforeEach
    void setUp() {
        mockHttpClient = new MockSyncHttpClient();
        mockAsyncHttpClient = new MockAsyncHttpClient();
        systemPropertyBeforeTest = System.getProperty(SYSTEM_PROPERTY_NAME);
    }

    @AfterEach
    void tearDown() {
        mockHttpClient.reset();
        mockAsyncHttpClient.reset();
        environmentVariableHelper.reset();
        if (systemPropertyBeforeTest != null) {
            System.setProperty(SYSTEM_PROPERTY_NAME, systemPropertyBeforeTest);
        } else {
            System.clearProperty(SYSTEM_PROPERTY_NAME);
        }
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void testAsyncClient(TestCase testCase) {
        setupSystemAndEnv(testCase);

        mockAsyncHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderAsyncClientBuilder clientBuilder = EnvironmentTokenProviderAsyncClient
            .builder()
            .httpClient(mockAsyncHttpClient);

        if (testCase.authSchemeProvider != null) {
            clientBuilder.authSchemeProvider(testCase.authSchemeProvider);
        }

        EnvironmentTokenProviderAsyncClient client = clientBuilder.build();

        if (testCase.operationToken == null) {
            client.oneOperation(b -> {} ).join();
        } else {
            client.oneOperation(requestWithOperationToken(testCase)).join();
        }

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();

        verifyRequest(testCase, loggedRequest);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void testSyncClient(TestCase testCase) {
        setupSystemAndEnv(testCase);

        mockHttpClient.stubNextResponse(mockResponse());

        EnvironmentTokenProviderClientBuilder clientBuilder = EnvironmentTokenProviderClient
            .builder()
            .httpClient(mockHttpClient);

        if (testCase.authSchemeProvider != null) {
            clientBuilder.authSchemeProvider(testCase.authSchemeProvider);
        }

        EnvironmentTokenProviderClient client = clientBuilder.build();

        if (testCase.operationToken == null) {
            client.oneOperation(b -> {} );
        } else {
            client.oneOperation(requestWithOperationToken(testCase));
        }


        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();

        verifyRequest(testCase, loggedRequest);
    }

    private static void verifyRequest(TestCase testCase, SdkHttpFullRequest loggedRequest) {
        if (testCase.expectBearerAuth) {
            assertThat(loggedRequest.firstMatchingHeader("Authorization").get())
                .startsWith("Bearer");
        } else {
            assertThat(loggedRequest.firstMatchingHeader("Authorization")
                                    .get()).startsWith("AWS4-HMAC-SHA256");
        }

        if (testCase.expectBusinessMetricSet) {
            assertThat(loggedRequest.firstMatchingHeader("User-Agent").get())
                .matches(".*m\\/[A-Za-z0-9,]+" + BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS);
        } else {
            assertThat(loggedRequest.firstMatchingHeader("User-Agent").get())
                .doesNotMatch(".*m\\/[A-Za-z0-9,]+" + BusinessMetricFeatureId.BEARER_SERVICE_ENV_VARS);
        }
    }

    static Stream<TestCase> testCases() {
        return Stream.of(
            TestCase.builder()
                .description("Does not use bearer auth when ENV token is unset")
                .expectBearerAuth(false)
                .build(),

            TestCase.builder()
                .description("Uses bearer auth when ENV token is set")
                .envVar(ENV_NAME, ENV_TOKEN)
                .expectBearerAuth(true)
                .expectedBearerToken(ENV_TOKEN)
                .expectBusinessMetricSet(true)
                .build(),

            TestCase.builder()
                .description("Uses bearer auth when system property token is set")
                .envVar(ENV_NAME, "some-other-token")
                .systemProperty(SYSTEM_TEST_TOKEN)
                .expectBearerAuth(true)
                .expectedBearerToken(SYSTEM_TEST_TOKEN)
                .expectBusinessMetricSet(true)
                .build(),

            TestCase.builder()
                    .description("Uses bearer auth from environment over auth scheme preference")
                    .envVar(ENV_NAME, ENV_TOKEN)
                    .envVar(
                        SdkSystemSetting.AWS_AUTH_SCHEME_PREFERENCE.environmentVariable(),
                        "sigv4")
                    .expectBearerAuth(true)
                    .expectedBearerToken(ENV_TOKEN)
                    .expectBusinessMetricSet(true)
                    .build(),

            TestCase.builder()
                    .description("Doesn't use bearer when AuthSchemeProvider is manually configured on the client")
                    .envVar(ENV_NAME, ENV_TOKEN)
                    .authSchemeProvider(EnvironmentTokenProviderAuthSchemeProvider.defaultProvider())
                    .expectBearerAuth(false)
                    .expectBusinessMetricSet(false)
                    .build(),

            TestCase.builder()
                    .description("Business metric is not set when the token is overridden on the operation")
                    .envVar(ENV_NAME, ENV_TOKEN)
                    .operationToken("operation-token")
                    .expectBearerAuth(true)
                    .expectedBearerToken("operation-token")
                    .expectBusinessMetricSet(false)
                    .build()
        );
    }

    private static OneOperationRequest requestWithOperationToken(TestCase testCase) {
        return OneOperationRequest.builder()
                                  .overrideConfiguration(c -> c.tokenIdentityProvider(
                                      StaticTokenProvider.create(() -> testCase.operationToken)))
                                  .build();
    }

    private void setupSystemAndEnv(TestCase testCase) {
        testCase.envVars.forEach(environmentVariableHelper::set);
        if (testCase.systemProperty != null) {
            System.setProperty(SYSTEM_PROPERTY_NAME, testCase.systemProperty);
        }
    }

    private HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .build();
    }

    static final class TestCase {
        final String description;
        final Map<String, String> envVars;
        final String systemProperty;
        final EnvironmentTokenProviderAuthSchemeProvider authSchemeProvider;
        final String operationToken;
        final boolean expectBearerAuth;
        final String expectedBearerToken;
        final boolean expectBusinessMetricSet;

        private TestCase(Builder builder) {
            this.description = builder.description;
            this.envVars = builder.envVars;
            this.systemProperty = builder.systemProperty;
            this.authSchemeProvider = builder.authSchemeProvider;
            this.operationToken = builder.operationToken;
            this.expectBearerAuth = builder.expectBearerAuth;
            this.expectedBearerToken = builder.expectedBearerToken;
            this.expectBusinessMetricSet = builder.expectBusinessMetricSet;
        }

        @Override
        public String toString() {
            return description;
        }

        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String description;
            private Map<String, String> envVars = new HashMap<>();
            private String systemProperty;
            private EnvironmentTokenProviderAuthSchemeProvider authSchemeProvider;
            private String operationToken;
            private boolean expectBearerAuth;
            private String expectedBearerToken;
            private boolean expectBusinessMetricSet;

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder envVar(String key, String value) {
                this.envVars.put(key, value);
                return this;
            }

            public Builder systemProperty(String systemProperty) {
                this.systemProperty = systemProperty;
                return this;
            }

            public Builder authSchemeProvider(EnvironmentTokenProviderAuthSchemeProvider authSchemeProvider) {
                this.authSchemeProvider = authSchemeProvider;
                return this;
            }

            public Builder operationToken(String operationToken) {
                this.operationToken = operationToken;
                return this;
            }

            public Builder expectBearerAuth(boolean expectBearerAuth) {
                this.expectBearerAuth = expectBearerAuth;
                return this;
            }

            public Builder expectedBearerToken(String expectedBearerToken) {
                this.expectedBearerToken = expectedBearerToken;
                return this;
            }

            public Builder expectBusinessMetricSet(boolean expectBusinessMetricSet) {
                this.expectBusinessMetricSet = expectBusinessMetricSet;
                return this;
            }

            public TestCase build() {
                return new TestCase(this);
            }
        }
    }
}
