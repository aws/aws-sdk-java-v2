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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Ensure that we can make anonymous requests using S3.
 */
public class S3AnonymousTest {
    private MockSyncHttpClient httpClient;
    private MockAsyncHttpClient asyncHttpClient;
    private S3Client s3;
    private S3AsyncClient s3Async;

    @BeforeEach
    public void setup() {
        this.httpClient = new MockSyncHttpClient();
        this.httpClient.stubNextResponse200();

        this.asyncHttpClient = new MockAsyncHttpClient();
        this.asyncHttpClient.stubNextResponse200();

        this.s3 = S3Client.builder()
                          .region(Region.US_WEST_2)
                          .credentialsProvider(AnonymousCredentialsProvider.create())
                          .httpClient(httpClient)
                          .build();

        this.s3Async = S3AsyncClient.builder()
                                    .region(Region.US_WEST_2)
                                    .credentialsProvider(AnonymousCredentialsProvider.create())
                                    .httpClient(asyncHttpClient)
                                    .build();
    }

    @AfterEach
    public void teardown() {
        httpClient.close();
        asyncHttpClient.close();
        s3.close();
        s3Async.close();
    }

    @Test
    public void nonStreamingOperations_canBeAnonymous() {
        s3.listBuckets();
        assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }

    @Test
    public void nonStreamingOperations_async_canBeAnonymous() {
        s3Async.listBuckets().join();
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }

    @Test
    public void streamingWriteOperations_canBeAnonymous() {
        s3.putObject(r -> r.bucket("bucket").key("key"), RequestBody.fromString("foo"));
        assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }

    @Test
    public void streamingWriteOperations_async_canBeAnonymous() {
        s3Async.putObject(r -> r.bucket("bucket").key("key"), AsyncRequestBody.fromString("foo")).join();
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }

    @Test
    public void streamingReadOperations_canBeAnonymous() {
        s3.getObject(r -> r.bucket("bucket").key("key"), ResponseTransformer.toBytes());
        assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }

    @Test
    public void streamingReadOperations_async_canBeAnonymous() {
        s3Async.getObject(r -> r.bucket("bucket").key("key"), AsyncResponseTransformer.toBytes()).join();
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }
}
