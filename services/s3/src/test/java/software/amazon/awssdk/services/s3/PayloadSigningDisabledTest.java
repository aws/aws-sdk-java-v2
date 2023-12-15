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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Ensure that payload signing is disabled for S3 operations.
 */
public class PayloadSigningDisabledTest {
    private static final AwsCredentialsProvider CREDENTIALS = () -> AwsBasicCredentials.create("akid", "skid");
    private static final ClientOverrideConfiguration ENABLE_PAYLOAD_SIGNING_CONFIG =
        ClientOverrideConfiguration.builder()
                                   .putExecutionAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true)
                                   .build();

    @Test
    public void syncPayloadSigningIsDisabled() {
        try (MockSyncHttpClient httpClient = new MockSyncHttpClient();
             S3Client s3 = S3Client.builder()
                                   .region(Region.US_WEST_2)
                                   .credentialsProvider(CREDENTIALS)
                                   .httpClient(httpClient)
                                   .build()) {
            httpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                           .response(SdkHttpResponse.builder().statusCode(200).build())
                                                           .build());

            s3.createBucket(r -> r.bucket("foo"));

            assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-content-sha256"))
                .hasValue("UNSIGNED-PAYLOAD");
        }
    }

    @Test
    public void asyncPayloadSigningIsDisabled() {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient();
             S3AsyncClient s3 = S3AsyncClient.builder()
                                             .region(Region.US_WEST_2)
                                             .credentialsProvider(CREDENTIALS)
                                             .httpClient(httpClient)
                                             .build()) {
            httpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                           .response(SdkHttpResponse.builder().statusCode(200).build())
                                                           .build());

            s3.createBucket(r -> r.bucket("foo")).join();

            assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-content-sha256"))
                .hasValue("UNSIGNED-PAYLOAD");
        }
    }

    @Test
    public void syncPayloadSigningCanBeEnabled() {
        try (MockSyncHttpClient httpClient = new MockSyncHttpClient();
             S3Client s3 = S3Client.builder()
                                   .region(Region.US_WEST_2)
                                   .credentialsProvider(CREDENTIALS)
                                   .httpClient(httpClient)
                                   .overrideConfiguration(ENABLE_PAYLOAD_SIGNING_CONFIG)
                                   .build()) {
            httpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                           .response(SdkHttpResponse.builder().statusCode(200).build())
                                                           .build());

            s3.createBucket(r -> r.bucket("foo"));

            assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-content-sha256"))
                .hasValue("a40ef303139635de59992f34c1c7da763f89200f2d55b71016f7c156527d63a0");
        }
    }

    @Test
    public void asyncPayloadSigningCanBeEnabled() {
        try (MockAsyncHttpClient httpClient = new MockAsyncHttpClient();
             S3AsyncClient s3 = S3AsyncClient.builder()
                                             .region(Region.US_WEST_2)
                                             .credentialsProvider(CREDENTIALS)
                                             .httpClient(httpClient)
                                             .overrideConfiguration(ENABLE_PAYLOAD_SIGNING_CONFIG)
                                             .build()) {
            httpClient.stubNextResponse(HttpExecuteResponse.builder()
                                                           .response(SdkHttpResponse.builder().statusCode(200).build())
                                                           .build());

            s3.createBucket(r -> r.bucket("foo")).join();

            assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-content-sha256"))
                .hasValue("a40ef303139635de59992f34c1c7da763f89200f2d55b71016f7c156527d63a0");
        }
    }
}
