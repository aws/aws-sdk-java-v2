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

package software.amazon.awssdk.services.codecatalyst;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.auth.token.credentials.ProfileTokenProvider;
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codecatalyst.model.ListSpacesResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class BearerCredentialTest extends AwsIntegrationTestBase {

    /*
    The test is disabled because it requires a codecatalyst profile that requires a human to run:
    `aws sso login --profile codecatalyst` before running this test. But leaving the test code here, so one can manually run it
     locally if needed.
     */
    // @Test
    public void realClientSucceeds() {
        CodeCatalystClient client = CodeCatalystClient.builder()
                                   .tokenProvider(ProfileTokenProvider.create("codecatalyst"))
                                   .build();
        ListSpacesResponse result = client.listSpaces(r -> {});
        assertThat(result.items()).isNotNull();
    }
    @Test
    public void syncClientSendsBearerToken() {
        try (MockSyncHttpClient httpClient = new MockSyncHttpClient();
             CodeCatalystClient codeCatalyst =
                 CodeCatalystClient.builder()
                                   .region(Region.US_WEST_2)
                                   .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                   .httpClient(httpClient)
                                   .build()) {
            httpClient.stubNextResponse200();
            codeCatalyst.listSpaces(r -> {});

            assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization"))
                .hasValue("Bearer foo-token");
        }
    }

    @Test
    public void asyncClientSendsBearerToken() {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient();
             CodeCatalystAsyncClient codeCatalyst =
                 CodeCatalystAsyncClient.builder()
                                        .region(Region.US_WEST_2)
                                        .tokenProvider(StaticTokenProvider.create(() -> "foo-token"))
                                        .httpClient(httpClient)
                                        .build()) {
            httpClient.stubNextResponse200();
            codeCatalyst.listSpaces(r -> {}).join();

            assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization"))
                .hasValue("Bearer foo-token");
        }
    }
}
