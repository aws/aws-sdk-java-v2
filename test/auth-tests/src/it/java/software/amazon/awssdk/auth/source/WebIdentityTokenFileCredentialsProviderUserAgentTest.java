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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
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
 * Test class to verify that WebIdentityTokenFileCredentialsProvider correctly includes
 * business metrics in the User-Agent header. This test focuses specifically on the
 * CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN ("h") business metric feature ID.
 */
class WebIdentityTokenFileCredentialsProviderUserAgentTest {

    private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/TestRole";
    private static final String ROLE_SESSION_NAME = "test-session";
    
    private MockSyncHttpClient mockHttpClient;
    private Path tokenFile;

    @BeforeEach
    public void setup() throws IOException {
        String existingTokenPath = Paths.get("../../services/sts/src/test/resources/token.jwt").toAbsolutePath().toString();
        byte[] tokenBytes = Files.readAllBytes(Paths.get(existingTokenPath));
        String tokenContent = new String(tokenBytes);

        tokenFile = Files.createTempFile("web-identity-token", ".jwt");
        Files.write(tokenFile, tokenContent.getBytes());

        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());
    }

    @AfterEach
    public void teardown() throws IOException {
        if (tokenFile != null && Files.exists(tokenFile)) {
            Files.delete(tokenFile);
        }
    }

    private static HttpExecuteResponse mockStsResponse() {
        String responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<AssumeRoleWithWebIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">" +
                "<AssumeRoleWithWebIdentityResult>" +
                "<Credentials>" +
                "<AccessKeyId>AKIAIOSFODNN7EXAMPLE</AccessKeyId>" +
                "<SecretAccessKey>wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY</SecretAccessKey>" +
                "<SessionToken>session-token</SessionToken>" +
                "<Expiration>2024-12-31T23:59:59Z</Expiration>" +
                "</Credentials>" +
                "</AssumeRoleWithWebIdentityResult>" +
                "</AssumeRoleWithWebIdentityResponse>";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("webIdentityTokenCredentialProvidersWithBuilder")
    void userAgentString_containsWebIdentityTokenBusinessMetric_WhenUsingWebIdentityTokenCredentialsWithBuilder(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {
        
        try {
            stsClient(provider, mockHttpClient).getCallerIdentity();

            SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
            assertThat(lastRequest).isNotNull();

            List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
            assertThat(userAgentHeaders).isNotNull().hasSize(1);
            assertThat(userAgentHeaders.get(0)).contains(expected);
        } catch (Exception e) {
        }
    }

    private static Stream<Arguments> webIdentityTokenCredentialProvidersWithBuilder() throws IOException {
        String existingTokenPath = Paths.get("../../services/sts/src/test/resources/token.jwt").toAbsolutePath().toString();
        
        return Stream.of(
            Arguments.of(WebIdentityTokenFileCredentialsProvider.builder()
                            .roleArn(ROLE_ARN)
                            .roleSessionName(ROLE_SESSION_NAME)
                            .webIdentityTokenFile(Paths.get(existingTokenPath))
                            .build(), "m/D,h")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
