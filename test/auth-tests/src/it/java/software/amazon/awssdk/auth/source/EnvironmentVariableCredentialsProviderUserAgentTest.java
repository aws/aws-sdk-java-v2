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

package software.amazon.awssdk.auth.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Test class to verify that EnvironmentVariableCredentialsProvider correctly includes
 * business metrics in the User-Agent header. This test focuses specifically on the
 * CREDENTIALS_ENV_VARS ("g") business metric feature ID.
 */
class EnvironmentVariableCredentialsProviderUserAgentTest {

    private MockSyncHttpClient mockHttpClient;
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @BeforeEach
    public void setup() {
        
        // Configure environment variable credentials
        System.setProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property(), "test-access-key");
        System.setProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property(), "test-secret-key");
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_ACCESS_KEY_ID.environmentVariable(), "akid2");
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.environmentVariable(), "skid2");

        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property());
        System.clearProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property());
        System.clearProperty(SdkSystemSetting.AWS_SESSION_TOKEN.property());
    }

    private static HttpExecuteResponse mockStsResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("environmentVariableCredentialProviders")
    void userAgentString_containsEnvironmentVariableBusinessMetric_WhenUsingEnvironmentVariableCredentials(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

            stsClient(provider, mockHttpClient).getCallerIdentity();

            SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
            assertThat(lastRequest).isNotNull();

            List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
            assertThat(userAgentHeaders).isNotNull().hasSize(1);
            assertThat(userAgentHeaders.get(0)).contains(expected);

    }

    private static Stream<Arguments> environmentVariableCredentialProviders() {
        return Stream.of(
            Arguments.of(EnvironmentVariableCredentialsProvider.create(), "m/D,g")
        );
    }

    @ParameterizedTest
    @MethodSource("environmentVariableCredentialProvidersWithSessionToken")
    void userAgentString_containsEnvironmentVariableBusinessMetric_WhenUsingEnvironmentVariableCredentialsWithSessionToken(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        System.setProperty(SdkSystemSetting.AWS_SESSION_TOKEN.property(), "test-session-token");

            stsClient(provider, mockHttpClient).getCallerIdentity();

            SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
            assertThat(lastRequest).isNotNull();

            List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
            assertThat(userAgentHeaders).isNotNull().hasSize(1);
            assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> environmentVariableCredentialProvidersWithSessionToken() {
        return Stream.of(
            Arguments.of(EnvironmentVariableCredentialsProvider.create(), "m/D,g")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
