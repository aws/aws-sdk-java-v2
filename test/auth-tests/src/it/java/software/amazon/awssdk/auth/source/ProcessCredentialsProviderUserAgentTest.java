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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Test class to verify that ProcessCredentialsProvider correctly includes
 * business metrics in the User-Agent header. This test focuses specifically on the
 * CREDENTIALS_PROCESS ("w") business metric feature ID.
 */
class ProcessCredentialsProviderUserAgentTest {

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setup() {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());
    }

    private static HttpExecuteResponse mockStsResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("processCredentialProviders")
    void userAgentString_containsProcessBusinessMetric_WhenUsingProcessCredentials(
        IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> processCredentialProviders() {
        String mockCommand = createMockCredentialsCommand(false);
        List<String> mockCommandList = createMockCredentialsCommandList(false);

        return Stream.of(
            Arguments.of(ProcessCredentialsProvider.builder()
                                                   .command(mockCommand)
                                                   .build(), "m/D,w"),

            Arguments.of(ProcessCredentialsProvider.builder()
                                                   .command(mockCommandList)
                                                   .build(), "m/D,w")
        );
    }

    @ParameterizedTest
    @MethodSource("processCredentialProvidersWithSessionToken")
    void userAgentString_containsProcessBusinessMetric_WhenUsingProcessCredentialsWithSessionToken(
        IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> processCredentialProvidersWithSessionToken() {
        String mockCommand = createMockCredentialsCommand(true);

        return Stream.of(
            Arguments.of(ProcessCredentialsProvider.builder()
                                                   .command(mockCommand)
                                                   .build(), "m/D,w")
        );
    }

    private static String createMockCredentialsCommand(boolean includeSessionToken) {
        String credentialsJson = createCredentialsJson(includeSessionToken);

        return "echo '" + credentialsJson + "'";
    }

    private static List<String> createMockCredentialsCommandList(boolean includeSessionToken) {
        String credentialsJson = createCredentialsJson(includeSessionToken);

        // Use echo command as a list
        return Arrays.asList("echo", credentialsJson);
    }

    private static String createCredentialsJson(boolean includeSessionToken) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"Version\": 1,");
        json.append("\"AccessKeyId\": \"test-access-key\",");
        json.append("\"SecretAccessKey\": \"test-secret-key\"");

        if (includeSessionToken) {
            json.append(",\"SessionToken\": \"test-session-token\"");
        }

        // Add expiration time (1 hour from now)
        String expiration = DateUtils.formatIso8601Date(Instant.now().plus(1, ChronoUnit.HOURS));
        json.append(",\"Expiration\": \"").append(expiration).append("\"");

        json.append("}");
        return json.toString();
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}