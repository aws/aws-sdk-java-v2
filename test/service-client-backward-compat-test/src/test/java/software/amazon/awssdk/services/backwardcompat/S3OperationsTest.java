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

package software.amazon.awssdk.services.backwardcompat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Ensure that S3 put/get operations, flexible checksums, and request-level credential overrides work correctly.
 */
public class S3OperationsTest {
    private static final StaticCredentialsProvider REQUEST_CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("request-akid", "request-skid"));

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

    // Put/Get operations

    @Test
    public void putObject_sync_completesSuccessfully() {
        s3.putObject(r -> r.bucket("bucket").key("key"), RequestBody.fromString("foo"));
        assertThat(httpClient.getLastRequest()).isNotNull();
        assertThat(httpClient.getLastRequest().method().name()).isEqualTo("PUT");
    }

    @Test
    public void putObject_async_completesSuccessfully() {
        s3Async.putObject(r -> r.bucket("bucket").key("key"), AsyncRequestBody.fromString("foo")).join();
        assertThat(asyncHttpClient.getLastRequest()).isNotNull();
        assertThat(asyncHttpClient.getLastRequest().method().name()).isEqualTo("PUT");
    }

    @Test
    public void getObject_sync_completesSuccessfully() {
        s3.getObject(r -> r.bucket("bucket").key("key"), ResponseTransformer.toBytes());
        assertThat(httpClient.getLastRequest()).isNotNull();
        assertThat(httpClient.getLastRequest().method().name()).isEqualTo("GET");
    }

    @Test
    public void getObject_async_completesSuccessfully() {
        s3Async.getObject(r -> r.bucket("bucket").key("key"), AsyncResponseTransformer.toBytes()).join();
        assertThat(asyncHttpClient.getLastRequest()).isNotNull();
        assertThat(asyncHttpClient.getLastRequest().method().name()).isEqualTo("GET");
    }

    // Flexible checksums

    @Test
    public void putObject_withChecksumAlgorithm_sync_sendsChecksumHeader() {
        s3.putObject(r -> r.bucket("bucket").key("key").checksumAlgorithm(ChecksumAlgorithm.CRC32),
                     RequestBody.fromString("foo"));
        assertThat(httpClient.getLastRequest()).isNotNull();
        // Checksum algorithm is sent as x-amz-sdk-checksum-algorithm, actual value as trailing header
        assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-sdk-checksum-algorithm"))
            .isPresent()
            .hasValue("CRC32");
        assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-trailer"))
            .isPresent()
            .hasValue("x-amz-checksum-crc32");
    }

    @Test
    public void putObject_withChecksumAlgorithm_async_sendsChecksumHeader() {
        s3Async.putObject(r -> r.bucket("bucket").key("key").checksumAlgorithm(ChecksumAlgorithm.CRC32),
                          AsyncRequestBody.fromString("foo")).join();
        assertThat(asyncHttpClient.getLastRequest()).isNotNull();
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("x-amz-sdk-checksum-algorithm"))
            .isPresent()
            .hasValue("CRC32");
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("x-amz-trailer"))
            .isPresent()
            .hasValue("x-amz-checksum-crc32");
    }

    @Test
    public void getObject_withChecksumMode_sync_sendsChecksumHeader() {
        s3.getObject(r -> r.bucket("bucket").key("key").checksumMode(ChecksumMode.ENABLED),
                     ResponseTransformer.toBytes());
        assertThat(httpClient.getLastRequest()).isNotNull();
        assertThat(httpClient.getLastRequest().firstMatchingHeader("x-amz-checksum-mode"))
            .isPresent()
            .hasValue("ENABLED");
    }

    @Test
    public void getObject_withChecksumMode_async_sendsChecksumHeader() {
        s3Async.getObject(r -> r.bucket("bucket").key("key").checksumMode(ChecksumMode.ENABLED),
                          AsyncResponseTransformer.toBytes()).join();
        assertThat(asyncHttpClient.getLastRequest()).isNotNull();
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("x-amz-checksum-mode"))
            .isPresent()
            .hasValue("ENABLED");
    }

    // Request-level credential overrides

    @Test
    public void requestLevelCredentials_overrideClientCredentials_sync() {
        s3.putObject(r -> r.bucket("bucket")
                           .key("key")
                           .overrideConfiguration(c -> c.credentialsProvider(REQUEST_CREDENTIALS)),
                     RequestBody.fromString("foo"));
        // Request should have Authorization header since we overrode anonymous with real credentials
        assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void requestLevelCredentials_overrideClientCredentials_async() {
        s3Async.putObject(r -> r.bucket("bucket")
                                .key("key")
                                .overrideConfiguration(c -> c.credentialsProvider(REQUEST_CREDENTIALS)),
                          AsyncRequestBody.fromString("foo"))
               .join();
        // Request should have Authorization header since we overrode anonymous with real credentials
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    public void withoutRequestLevelCredentials_usesClientAnonymous_sync() {
        s3.putObject(r -> r.bucket("bucket").key("key"), RequestBody.fromString("foo"));
        // No override — should be anonymous (no Authorization header)
        assertThat(httpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }

    @Test
    public void withoutRequestLevelCredentials_usesClientAnonymous_async() {
        s3Async.putObject(r -> r.bucket("bucket").key("key"), AsyncRequestBody.fromString("foo")).join();
        // No override — should be anonymous (no Authorization header)
        assertThat(asyncHttpClient.getLastRequest().firstMatchingHeader("Authorization")).isEmpty();
    }
}
