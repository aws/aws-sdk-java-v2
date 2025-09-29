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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Test class to verify Profile credentials provider business metrics.
 */
class ProfileCredentialProviderUserAgentTest {

    private MockSyncHttpClient mockHttpClient;
    private Path tempConfigFile;

    @BeforeEach
    public void setup() {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());
    }

    @AfterEach
    public void teardown() throws IOException {
        if (tempConfigFile != null && Files.exists(tempConfigFile)) {
            Files.delete(tempConfigFile);
        }
    }

    private static HttpExecuteResponse mockStsResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    // Basic profile credentials - Expected Feature ID: "n"
    @Test
    void basicProfileCredentials_containsFeatureIdN() throws Exception {
        String configContent =
            "[profile A]\n" +
            "aws_access_key_id = abc123\n" +
            "aws_secret_access_key = def456\n";

        tempConfigFile = Files.createTempFile("aws-config-basic-", ".tmp");
        Files.write(tempConfigFile, configContent.getBytes());

        ProfileFile profileFile = ProfileFile.builder()
                                             .content(tempConfigFile)
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();

        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder()
                                                                                   .profileFile(profileFile)
                                                                                   .profileName("A")
                                                                                   .build();

        StsClient stsClient = StsClient.builder()
                                       .credentialsProvider(credentialsProvider)
                                       .httpClient(mockHttpClient)
                                       .build();

        stsClient.getCallerIdentity();

        assertThat(mockHttpClient.getRequests()).hasSize(1);

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        String userAgent = userAgentHeaders.get(0);

        assertThat(userAgent).contains("m/D,n");

        credentialsProvider.close();
        stsClient.close();
    }

    //Profile with credential_process - Expected Feature IDs: "v,w"
    @Test
    void profileWithCredentialProcess_containsFeatureIdVW() throws Exception {
        String configContent =
            "[profile A]\n" +
            "credential_process = echo '{\"Version\": 1, \"AccessKeyId\": \"abc123\", \"SecretAccessKey\": \"def456\"}'\n";

        tempConfigFile = Files.createTempFile("aws-config-process-", ".tmp");
        Files.write(tempConfigFile, configContent.getBytes());

        ProfileFile profileFile = ProfileFile.builder()
                                             .content(tempConfigFile)
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();

        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder()
                                                                                   .profileFile(profileFile)
                                                                                   .profileName("A")
                                                                                   .build();

        StsClient stsClient = StsClient.builder()
                                       .credentialsProvider(credentialsProvider)
                                       .httpClient(mockHttpClient)
                                       .build();

        stsClient.getCallerIdentity();

        assertThat(mockHttpClient.getRequests()).hasSize(1);

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        String userAgent = userAgentHeaders.get(0);

        assertThat(userAgent).contains("m/D,v,w");

        credentialsProvider.close();
        stsClient.close();
    }

}
