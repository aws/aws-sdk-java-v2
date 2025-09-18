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
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Test class to verify that ProfileCredentialsProvider correctly handles
 * business metrics in the User-Agent header when credential_source succeeds
 * but assume role fails and falls back to the next provider in the chain.
 * 
 * This test simulates this example:
 * - Profile A: assume role with credential_source = Ec2InstanceMetadata
 * - IMDS credentials provider succeeds (emits "p", "0")
 * - Assume role service call fails
 * - Falls back to EnvironmentVariableCredentialsProvider (succeeds)
 * 
 * Expected behavior: When assume role fails, "p" and "0" are removed from
 * business metrics, and the chain continues to the next provider.
 * Final business metrics should only contain the successful provider's metrics.
 */
class ProfileCredentialSourceStsFailureUserAgentTest {

    private MockSyncHttpClient mockHttpClient;
    private Path tempConfigFile;

    @BeforeEach
    public void setup() throws IOException {
        // Create temporary config file with profile credential source scenario
        tempConfigFile = Files.createTempFile("aws-config-", ".tmp");
        String configContent = 
            "[profile A]\n" +
            "role_arn = arn:aws:iam::123456789:role/RoleA\n" +
            "credential_source = Ec2InstanceMetadata\n";
        
        Files.write(tempConfigFile, configContent.getBytes());

        mockHttpClient = new MockSyncHttpClient();
        // Mock successful IMDS responses
        mockHttpClient.stubNextResponse(mockImdsTokenResponse());
        mockHttpClient.stubNextResponse(mockImdsCredentialsResponse());
        // Mock STS AssumeRole failure response (403 Forbidden)
        mockHttpClient.stubNextResponse(mockStsAssumeRoleFailureResponse());
        // Mock successful GetCallerIdentity response for fallback provider
        mockHttpClient.stubNextResponse(mockStsGetCallerIdentityResponse());
    }

    @AfterEach
    public void teardown() throws IOException {
        if (tempConfigFile != null && Files.exists(tempConfigFile)) {
            Files.delete(tempConfigFile);
        }
    }

    private static HttpExecuteResponse mockImdsTokenResponse() {
        String responseBody = "AQAAANpEq2k-c8BtmxvWBHyQLjKJEc6DEBhQ3oP5wVxVSKWHhH_SqA==";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    private static HttpExecuteResponse mockImdsCredentialsResponse() {
        String responseBody = "{\n" +
                "  \"Code\" : \"Success\",\n" +
                "  \"LastUpdated\" : \"2024-01-01T00:00:00Z\",\n" +
                "  \"Type\" : \"AWS-HMAC\",\n" +
                "  \"AccessKeyId\" : \"AKIAIOSFODNN7EXAMPLE\",\n" +
                "  \"SecretAccessKey\" : \"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\",\n" +
                "  \"Token\" : \"token\",\n" +
                "  \"Expiration\" : \"2024-12-31T23:59:59Z\"\n" +
                "}";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    private static HttpExecuteResponse mockStsAssumeRoleFailureResponse() {
        String responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<ErrorResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">" +
                "<Error>" +
                "<Type>Sender</Type>" +
                "<Code>AccessDenied</Code>" +
                "<Message>User: arn:aws:iam::123456789:user/testuser is not authorized to perform: sts:AssumeRole on resource: arn:aws:iam::123456789:role/RoleA</Message>" +
                "</Error>" +
                "<RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>" +
                "</ErrorResponse>";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(403).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    private static HttpExecuteResponse mockStsGetCallerIdentityResponse() {
        String responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<GetCallerIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">" +
                "<GetCallerIdentityResult>" +
                "<Arn>arn:aws:iam::123456789:user/testuser</Arn>" +
                "<UserId>AIDACKCEVSQ6C2EXAMPLE</UserId>" +
                "<Account>123456789</Account>" +
                "</GetCallerIdentityResult>" +
                "</GetCallerIdentityResponse>";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("profileCredentialSourceStsFailureProviders")
    void userAgentString_containsOnlySuccessfulProviderBusinessMetrics_WhenCredentialSourceSucceedsButAssumeRoleFails(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        System.setProperty("aws.accessKeyId", "AKIAIOSFODNN7EXAMPLE");
        System.setProperty("aws.secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        
        try {
            stsClient(provider, mockHttpClient).getCallerIdentity();

            SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
            assertThat(lastRequest).isNotNull();

            List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
            assertThat(userAgentHeaders).isNotNull().hasSize(1);
            String userAgent = userAgentHeaders.get(0);

            assertThat(userAgent).contains(expected);
            
            // Verify that failed credential source metrics are NOT present
            // The "p" (CREDENTIALS_PROFILE_CREDENTIAL_SOURCE) and "0" (CREDENTIALS_IMDS) 
            // should be removed when assume role fails
            assertThat(userAgent).doesNotContain("p");
            assertThat(userAgent).doesNotContain("0");
            
        } catch (Exception e) {
        } finally {
            System.clearProperty("aws.accessKeyId");
            System.clearProperty("aws.secretAccessKey");
        }
    }

    private static Stream<Arguments> profileCredentialSourceStsFailureProviders() throws IOException {
        // Create temporary config file with profile credential source scenario
        Path tempConfigFile = Files.createTempFile("aws-config-", ".tmp");
        String configContent = 
            "[profile A]\n" +
            "role_arn = arn:aws:iam::123456789:role/RoleA\n" +
            "credential_source = Ec2InstanceMetadata\n";
        
        Files.write(tempConfigFile, configContent.getBytes());

        // Create ProfileFile from temporary config
        ProfileFile profileFile = ProfileFile.builder()
            .content(tempConfigFile)
            .type(ProfileFile.Type.CONFIGURATION)
            .build();

        // Create a credentials provider chain that includes:
        // 1. ProfileCredentialsProvider (IMDS succeeds, assume role fails)
        // 2. EnvironmentVariableCredentialsProvider (will succeed)
        ProfileCredentialsProvider profileProvider = ProfileCredentialsProvider.builder()
            .profileFile(profileFile)
            .profileName("A")
            .build();
        
        EnvironmentVariableCredentialsProvider envProvider = EnvironmentVariableCredentialsProvider.create();
        
        AwsCredentialsProviderChain chainProvider = AwsCredentialsProviderChain.of(
            profileProvider,
            envProvider
        );

        return Stream.of(
            // Expected: Only environment variable provider business metrics (g = CREDENTIALS_ENV_VARS)
            // The failed credential source metrics (p, 0) should be removed
            Arguments.of(chainProvider, "m/D,g")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
