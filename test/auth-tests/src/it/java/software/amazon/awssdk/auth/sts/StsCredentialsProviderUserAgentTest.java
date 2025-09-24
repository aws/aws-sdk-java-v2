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

package software.amazon.awssdk.auth.sts;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithSamlCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsGetFederationTokenCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsGetSessionTokenCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsWebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Tests STS credentials provider business metrics emission in User-Agent headers.
 * 
 * Tests the following business metrics:
 * - CREDENTIALS_STS_ASSUME_ROLE("i") - StsAssumeRoleCredentialsProvider
 * - CREDENTIALS_STS_ASSUME_ROLE_SAML("j") - StsAssumeRoleWithSamlCredentialsProvider  
 * - CREDENTIALS_STS_ASSUME_ROLE_WEB_ID("k") - StsAssumeRoleWithWebIdentityCredentialsProvider
 * - CREDENTIALS_STS_FEDERATION_TOKEN("l") - StsGetFederationTokenCredentialsProvider
 * - CREDENTIALS_STS_SESSION_TOKEN("m") - StsGetSessionTokenCredentialsProvider
 * - CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN("h") - WebIdentityTokenFileCredentialsProvider
 */
class StsCredentialsProviderUserAgentTest {

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setup() {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());
    }

    @ParameterizedTest
    @MethodSource("stsCredentialsProviders")
    void stsCredentialsProvider_emitsCorrectBusinessMetrics(AwsCredentialsProvider provider, 
                                                           String expected, 
                                                           String providerName) throws Exception {
        StsClient stsClient = StsClient.builder()
                                       .credentialsProvider(provider)
                                       .httpClient(mockHttpClient)
                                       .build();

        stsClient.getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);

        stsClient.close();
    }

    private static Stream<Arguments> stsCredentialsProviders() throws Exception {
        return Stream.of(
            Arguments.of(createAssumeRoleProvider(), "m/D,i", "StsAssumeRoleCredentialsProvider"),
            Arguments.of(createAssumeRoleWithSamlProvider(), "m/D,j", "StsAssumeRoleWithSamlCredentialsProvider"),
            Arguments.of(createAssumeRoleWithWebIdentityProvider(), "m/D,k", "StsAssumeRoleWithWebIdentityCredentialsProvider"),
            Arguments.of(createFederationTokenProvider(), "m/D,l", "StsGetFederationTokenCredentialsProvider"),
            Arguments.of(createSessionTokenProvider(), "m/D,m", "StsGetSessionTokenCredentialsProvider"),
            Arguments.of(createWebIdentityTokenFileProvider(), "m/D,k,h", "StsWebIdentityTokenFileCredentialsProvider")
        );
    }

    private static AwsCredentialsProvider createAssumeRoleProvider() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(createStsResponse("AssumeRole"));
        
        StsClient stsClient = StsClient.builder()
                .httpClient(mockHttpClient)
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();
        
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(r -> r.roleArn("arn:aws:iam::123456789012:role/TestRole")
                                      .roleSessionName("test-session"))
                .build();
    }

    private static AwsCredentialsProvider createAssumeRoleWithSamlProvider() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(createStsResponse("AssumeRoleWithSAML"));
        
        StsClient stsClient = StsClient.builder()
                .httpClient(mockHttpClient)
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();
        
        String samlAssertion = "PHNhbWw6QXNzZXJ0aW9uPjwvc2FtbDpBc3NlcnRpb24+";
        
        return StsAssumeRoleWithSamlCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(r -> r.roleArn("arn:aws:iam::123456789012:role/TestRole")
                                      .principalArn("arn:aws:iam::123456789012:saml-provider/TestProvider")
                                      .samlAssertion(samlAssertion))
                .build();
    }

    private static AwsCredentialsProvider createAssumeRoleWithWebIdentityProvider() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(createStsResponse("AssumeRoleWithWebIdentity"));
        
        StsClient stsClient = StsClient.builder()
                .httpClient(mockHttpClient)
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();
        
        String webIdentityToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        return StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(r -> r.roleArn("arn:aws:iam::123456789012:role/TestRole")
                                      .webIdentityToken(webIdentityToken)
                                      .roleSessionName("test-session"))
                .build();
    }

    private static AwsCredentialsProvider createFederationTokenProvider() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(createStsResponse("GetFederationToken"));
        
        StsClient stsClient = StsClient.builder()
                .httpClient(mockHttpClient)
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();
        
        return StsGetFederationTokenCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(r -> r.name("test-user"))
                .build();
    }

    private static AwsCredentialsProvider createSessionTokenProvider() {
        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(createStsResponse("GetSessionToken"));
        
        StsClient stsClient = StsClient.builder()
                .httpClient(mockHttpClient)
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();
        
        return StsGetSessionTokenCredentialsProvider.builder()
                .stsClient(stsClient)
                .build();
    }

    private static AwsCredentialsProvider createWebIdentityTokenFileProvider() throws Exception {
        // Create temporary token file
        Path tempTokenFile = Files.createTempFile("test-token", ".jwt");
        Files.write(tempTokenFile, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c".getBytes());

        System.setProperty(SdkSystemSetting.AWS_ROLE_ARN.property(), "arn:aws:iam::123456789012:role/TestRole");
        System.setProperty(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.property(), tempTokenFile.toString());
        System.setProperty(SdkSystemSetting.AWS_ROLE_SESSION_NAME.property(), "test-session");

        MockSyncHttpClient mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(createStsResponse("AssumeRoleWithWebIdentity"));
        
        StsClient stsClient = StsClient.builder()
                .httpClient(mockHttpClient)
                .region(software.amazon.awssdk.regions.Region.US_EAST_1)
                .build();
        
        return StsWebIdentityTokenFileCredentialsProvider.builder()
                .stsClient(stsClient)
                .build();
    }

    private static HttpExecuteResponse mockStsResponse() {
        String getCallerIdentityResponseBody = "<GetCallerIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">\n" +
                                               "  <GetCallerIdentityResult>\n" +
                                               "    <Arn>arn:aws:sts::123456789012:assumed-role/TestRole/test-session</Arn>\n" +
                                               "    <UserId>AROATEST:test-session</UserId>\n" +
                                               "    <Account>123456789012</Account>\n" +
                                               "  </GetCallerIdentityResult>\n" +
                                               "</GetCallerIdentityResponse>";

        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(getCallerIdentityResponseBody)))
                                  .build();
    }

    private static HttpExecuteResponse createStsResponse(String operation) {
        String responseBody;
        
        switch (operation) {
            case "AssumeRole":
                responseBody = "<AssumeRoleResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">\n"
                             + "  <AssumeRoleResult>\n"
                             + "    <Credentials>\n"
                             + "      <AccessKeyId>AKIATEST</AccessKeyId>\n"
                             + "      <SecretAccessKey>test-secret</SecretAccessKey>\n"
                             + "      <SessionToken>test-session-token</SessionToken>\n"
                             + "      <Expiration>2025-09-24T00:00:00Z</Expiration>\n"
                             + "    </Credentials>\n"
                             + "    <AssumedRoleUser>\n"
                             + "      <Arn>arn:aws:sts::123456789012:assumed-role/TestRole/test-session</Arn>\n"
                             + "      <AssumedRoleId>AROATEST:test-session</AssumedRoleId>\n"
                             + "    </AssumedRoleUser>\n"
                             + "  </AssumeRoleResult>\n"
                             + "</AssumeRoleResponse>";
                break;
                
            case "AssumeRoleWithSAML":
                responseBody = "<AssumeRoleWithSAMLResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">\n"
                             + "  <AssumeRoleWithSAMLResult>\n"
                             + "    <Credentials>\n"
                             + "      <AccessKeyId>AKIATEST</AccessKeyId>\n"
                             + "      <SecretAccessKey>test-secret</SecretAccessKey>\n"
                             + "      <SessionToken>test-session-token</SessionToken>\n"
                             + "      <Expiration>2025-09-24T00:00:00Z</Expiration>\n"
                             + "    </Credentials>\n"
                             + "    <AssumedRoleUser>\n"
                             + "      <Arn>arn:aws:sts::123456789012:assumed-role/TestRole/test-session</Arn>\n"
                             + "      <AssumedRoleId>AROATEST:test-session</AssumedRoleId>\n"
                             + "    </AssumedRoleUser>\n"
                             + "  </AssumeRoleWithSAMLResult>\n"
                             + "</AssumeRoleWithSAMLResponse>";
                break;
                
            case "AssumeRoleWithWebIdentity":
                responseBody = "<AssumeRoleWithWebIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">\n"
                             + "  <AssumeRoleWithWebIdentityResult>\n"
                             + "    <Credentials>\n"
                             + "      <AccessKeyId>AKIATEST</AccessKeyId>\n"
                             + "      <SecretAccessKey>test-secret</SecretAccessKey>\n"
                             + "      <SessionToken>test-session-token</SessionToken>\n"
                             + "      <Expiration>2025-09-24T00:00:00Z</Expiration>\n"
                             + "    </Credentials>\n"
                             + "    <AssumedRoleUser>\n"
                             + "      <Arn>arn:aws:sts::123456789012:assumed-role/TestRole/test-session</Arn>\n"
                             + "      <AssumedRoleId>AROATEST:test-session</AssumedRoleId>\n"
                             + "    </AssumedRoleUser>\n"
                             + "  </AssumeRoleWithWebIdentityResult>\n"
                             + "</AssumeRoleWithWebIdentityResponse>";
                break;
                
            case "GetFederationToken":
                responseBody = "<GetFederationTokenResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">\n"
                             + "  <GetFederationTokenResult>\n"
                             + "    <Credentials>\n"
                             + "      <AccessKeyId>AKIATEST</AccessKeyId>\n"
                             + "      <SecretAccessKey>test-secret</SecretAccessKey>\n"
                             + "      <SessionToken>test-session-token</SessionToken>\n"
                             + "      <Expiration>2025-09-24T00:00:00Z</Expiration>\n"
                             + "    </Credentials>\n"
                             + "    <FederatedUser>\n"
                             + "      <Arn>arn:aws:sts::123456789012:federated-user/test-user</Arn>\n"
                             + "      <FederatedUserId>123456789012:test-user</FederatedUserId>\n"
                             + "    </FederatedUser>\n"
                             + "  </GetFederationTokenResult>\n"
                             + "</GetFederationTokenResponse>";
                break;
                
            case "GetSessionToken":
                responseBody = "<GetSessionTokenResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">\n"
                             + "  <GetSessionTokenResult>\n"
                             + "    <Credentials>\n"
                             + "      <AccessKeyId>AKIATEST</AccessKeyId>\n"
                             + "      <SecretAccessKey>test-secret</SecretAccessKey>\n"
                             + "      <SessionToken>test-session-token</SessionToken>\n"
                             + "      <Expiration>2025-09-24T00:00:00Z</Expiration>\n"
                             + "    </Credentials>\n"
                             + "  </GetSessionTokenResult>\n"
                             + "</GetSessionTokenResponse>";
                break;
                
            default:
                throw new IllegalArgumentException("Unknown STS operation: " + operation);
        }
        
        return HttpExecuteResponse.builder()
                .response(SdkHttpResponse.builder().statusCode(200).build())
                .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                .build();
    }
}
