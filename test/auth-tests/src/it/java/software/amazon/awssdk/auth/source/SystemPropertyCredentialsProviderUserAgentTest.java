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
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
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

/**
 * Test class to verify that SystemPropertyCredentialsProvider correctly includes
 * business metrics in the User-Agent header. This test focuses specifically on the
 * CREDENTIALS_JVM_SYSTEM_PROPERTIES ("f") business metric feature ID.
 */
class SystemPropertyCredentialsProviderUserAgentTest {

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setup() {

        System.setProperty("aws.accessKeyId", "test-access-key");
        System.setProperty("aws.secretAccessKey", "test-secret-key");
        
        // Setup mock HTTP client for STS calls
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
        System.clearProperty("aws.sessionToken");
    }

    private static HttpExecuteResponse mockStsResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("systemPropertyCredentialProviders")
    void userAgentString_containsSystemPropertyBusinessMetric_WhenUsingSystemPropertyCredentials(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> systemPropertyCredentialProviders() {
        return Stream.of(
            Arguments.of(SystemPropertyCredentialsProvider.create(), "m/D,f")
        );
    }

    @ParameterizedTest
    @MethodSource("systemPropertyCredentialProvidersWithSessionToken")
    void userAgentString_containsSystemPropertyBusinessMetric_WhenUsingSystemPropertyCredentialsWithSessionToken(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        System.setProperty("aws.sessionToken", "test-session-token");
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> systemPropertyCredentialProvidersWithSessionToken() {
        return Stream.of(
            Arguments.of(SystemPropertyCredentialsProvider.create(), "m/D,f")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
