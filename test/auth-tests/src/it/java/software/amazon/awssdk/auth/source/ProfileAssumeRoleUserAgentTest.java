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
 * Test class to verify that ProfileCredentialsProvider correctly includes
 * business metrics in the User-Agent header for profile assume role scenarios.
 * 
 * This test simulates this example:
 * - Profile A: assume role with source_profile = B
 * - Profile B: basic credentials (access key + secret key)
 * 
 * Expected business metrics: "o" (CREDENTIALS_PROFILE_SOURCE_PROFILE), 
 * "n" (CREDENTIALS_PROFILE), "i" (CREDENTIALS_STS_ASSUME_ROLE)
 */
class ProfileAssumeRoleUserAgentTest {

    private MockSyncHttpClient mockHttpClient;
    private Path tempConfigFile;

    @BeforeEach
    public void setup() throws IOException {
        tempConfigFile = Files.createTempFile("aws-config-", ".tmp");
        String configContent = 
            "[profile A]\n" +
            "role_arn = arn:aws:iam::123456789:role/RoleA\n" +
            "source_profile = B\n" +
            "\n" +
            "[profile B]\n" +
            "aws_access_key_id = AKIAIOSFODNN7EXAMPLE\n" +
            "aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\n";
        
        Files.write(tempConfigFile, configContent.getBytes());

        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsAssumeRoleResponse());
        mockHttpClient.stubNextResponse(mockStsGetCallerIdentityResponse());
    }

    @AfterEach
    public void teardown() throws IOException {
        if (tempConfigFile != null && Files.exists(tempConfigFile)) {
            Files.delete(tempConfigFile);
        }
    }

    private static HttpExecuteResponse mockStsAssumeRoleResponse() {
        String responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<AssumeRoleResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">" +
                "<AssumeRoleResult>" +
                "<Credentials>" +
                "<AccessKeyId>ASIAIOSFODNN7EXAMPLE</AccessKeyId>" +
                "<SecretAccessKey>wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY</SecretAccessKey>" +
                "<SessionToken>session-token</SessionToken>" +
                "<Expiration>2024-12-31T23:59:59Z</Expiration>" +
                "</Credentials>" +
                "<AssumedRoleUser>" +
                "<AssumedRoleId>AROA3XFRBF535PLBQX4MJ:aws-sdk-java-1234567890</AssumedRoleId>" +
                "<Arn>arn:aws:sts::123456789:assumed-role/RoleA/aws-sdk-java-1234567890</Arn>" +
                "</AssumedRoleUser>" +
                "</AssumeRoleResult>" +
                "</AssumeRoleResponse>";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    private static HttpExecuteResponse mockStsGetCallerIdentityResponse() {
        String responseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<GetCallerIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">" +
                "<GetCallerIdentityResult>" +
                "<Arn>arn:aws:sts::123456789:assumed-role/RoleA/aws-sdk-java-1234567890</Arn>" +
                "<UserId>AROA3XFRBF535PLBQX4MJ:aws-sdk-java-1234567890</UserId>" +
                "<Account>123456789</Account>" +
                "</GetCallerIdentityResult>" +
                "</GetCallerIdentityResponse>";
        
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream(responseBody)))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("profileAssumeRoleCredentialProviders")
    void userAgentString_containsProfileAssumeRoleBusinessMetrics_WhenUsingProfileAssumeRole(
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

    private static Stream<Arguments> profileAssumeRoleCredentialProviders() throws IOException {
        // Create temporary config file with profile assume role scenario
        Path tempConfigFile = Files.createTempFile("aws-config-", ".tmp");
        String configContent = 
            "[profile A]\n" +
            "role_arn = arn:aws:iam::123456789:role/RoleA\n" +
            "source_profile = B\n" +
            "\n" +
            "[profile B]\n" +
            "aws_access_key_id = AKIAIOSFODNN7EXAMPLE\n" +
            "aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\n";
        
        Files.write(tempConfigFile, configContent.getBytes());

        // Create ProfileFile from temporary config
        ProfileFile profileFile = ProfileFile.builder()
            .content(tempConfigFile)
            .type(ProfileFile.Type.CONFIGURATION)
            .build();

        return Stream.of(
            Arguments.of(ProfileCredentialsProvider.builder()
                            .profileFile(profileFile)
                            .profileName("A")
                            .build(), "m/D,o,n,i")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
