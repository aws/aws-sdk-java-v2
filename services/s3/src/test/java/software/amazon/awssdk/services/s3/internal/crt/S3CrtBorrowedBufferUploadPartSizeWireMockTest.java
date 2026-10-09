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

package software.amazon.awssdk.services.s3.internal.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/** Verifies that direct buffer pools prevent CRT from enlarging multipart upload part sizes. */
@WireMockTest
@Timeout(60)
class S3CrtBorrowedBufferUploadPartSizeWireMockTest {
    private static final int SMALL_PART_SIZE = 256 * 1024;
    private static final int MULTIPART_MINIMUM = 5 * 1024 * 1024;
    private static final byte[] CONTENT = payload(3 * SMALL_PART_SIZE);
    private static final String E_TAG = "\"upload-etag\"";

    @Test
    void multipartUploadBelowMultipartMinimum_withPool_shouldFailFastWithActionableMessage(WireMockRuntimeInfo wireMock) {
        stubUpload();
        AtomicBoolean bodyRequested = new AtomicBoolean(false);
        AsyncRequestBody body = neverProducingBody(CONTENT.length, bodyRequested);

        try (S3AsyncClient client = newClient(wireMock, (long) SMALL_PART_SIZE, 64L * 1024 * 1024)) {
            assertThatThrownBy(() -> client.putObject(r -> r.bucket("bucket")
                                                           .key("key")
                                                           .contentLength((long) CONTENT.length), body)
                                           .get(20, TimeUnit.SECONDS))
                .hasMessageContaining("part size")
                // The caller needs the number to set, not just "too small".
                .hasMessageContaining(Integer.toString(MULTIPART_MINIMUM))
                .hasMessageContaining(Integer.toString(SMALL_PART_SIZE));
        }

        assertThat(bodyRequested).isFalse();
        assertThat(findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void singlePartUpload_withPool_shouldSucceed(WireMockRuntimeInfo wireMock) throws Exception {
        stubUpload();

        try (S3AsyncClient client = newClient(wireMock,
                                               (long) SMALL_PART_SIZE,
                                               64L * 1024 * 1024,
                                               (long) CONTENT.length + 1)) {
            PutObjectResponse response = putContent(client).get(20, TimeUnit.SECONDS);
            assertThat(response).isNotNull();
        }

        assertThat(findAll(anyRequestedFor(anyUrl())))
            .extracting(request -> request.getMethod().getName())
            .containsExactly("PUT");
    }

    @Test
    void uploadBelowMultipartMinimum_withoutPool_shouldSucceed(WireMockRuntimeInfo wireMock) throws Exception {
        stubUpload();

        try (S3AsyncClient client = newClient(wireMock, (long) SMALL_PART_SIZE, 0)) {
            PutObjectResponse response = putContent(client).get(20, TimeUnit.SECONDS);
            assertThat(response).isNotNull();
        }
    }

    @Test
    void uploadNeedingMoreThanMaxParts_withPool_shouldFailBeforeReadingBody(WireMockRuntimeInfo wireMock) {
        stubUpload();
        AtomicBoolean bodyRequested = new AtomicBoolean(false);
        // 10,000 parts at 5 MiB tops out at ~48.8 GiB, so 100 GiB cannot be uploaded at this part size no matter how
        // the parts are arranged. Nothing is ever read from this body, so no real data is needed.
        long declaredLength = 100L * 1024 * 1024 * 1024;
        AsyncRequestBody body = neverProducingBody(declaredLength, bodyRequested);

        try (S3AsyncClient client = newClient(wireMock, (long) MULTIPART_MINIMUM, 64L * 1024 * 1024)) {
            assertThatThrownBy(() -> client.putObject(r -> r.bucket("bucket")
                                                           .key("key")
                                                           .contentLength(declaredLength), body)
                                           .get(20, TimeUnit.SECONDS))
                .hasMessageContaining("part size");
        }

        assertThat(bodyRequested).isFalse();
        assertThat(findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    private static CompletableFuture<PutObjectResponse> putContent(S3AsyncClient client) {
        return client.putObject(r -> r.bucket("bucket").key("key").contentLength((long) CONTENT.length),
                                AsyncRequestBody.fromBytes(CONTENT));
    }

    private static AsyncRequestBody neverProducingBody(long declaredLength, AtomicBoolean bodyRequested) {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(declaredLength);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        bodyRequested.set(true);
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock, long partSize, long poolBytes) {
        return newClient(wireMock, partSize, poolBytes, null);
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock,
                                           long partSize,
                                           long poolBytes,
                                           Long thresholdInBytes) {
        S3CrtAsyncClientBuilderShim builder = new S3CrtAsyncClientBuilderShim(wireMock, partSize, thresholdInBytes);
        return poolBytes > 0 ? builder.withPool(poolBytes) : builder.withoutPool();
    }

    private static final class S3CrtAsyncClientBuilderShim {
        private final WireMockRuntimeInfo wireMock;
        private final long partSize;
        private final Long thresholdInBytes;

        private S3CrtAsyncClientBuilderShim(WireMockRuntimeInfo wireMock, long partSize, Long thresholdInBytes) {
            this.wireMock = wireMock;
            this.partSize = partSize;
            this.thresholdInBytes = thresholdInBytes;
        }

        private software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder base() {
            return S3AsyncClient.crtBuilder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("key", "secret")))
                                .minimumPartSizeInBytes(partSize)
                                .thresholdInBytes(thresholdInBytes)
                                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED);
        }

        private S3AsyncClient withPool(long poolBytes) {
            return base().directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(poolBytes)).build();
        }

        private S3AsyncClient withoutPool() {
            return base().build();
        }
    }

    private static void stubUpload() {
        stubFor(post(anyUrl()).withQueryParam("uploads", equalTo(""))
                              .willReturn(aResponse().withStatus(200)
                                                     .withHeader("Content-Type", "application/xml")
                                                     .withBody("<InitiateMultipartUploadResult>"
                                                               + "<Bucket>bucket</Bucket><Key>key</Key>"
                                                               + "<UploadId>upload-id</UploadId>"
                                                               + "</InitiateMultipartUploadResult>")));
        stubFor(post(anyUrl()).withQueryParam("uploadId", equalTo("upload-id"))
                              .willReturn(aResponse().withStatus(200)
                                                     .withHeader("Content-Type", "application/xml")
                                                     .withBody("<CompleteMultipartUploadResult>"
                                                               + "<Location>http://localhost/bucket/key</Location>"
                                                               + "<Bucket>bucket</Bucket><Key>key</Key>"
                                                               + "<ETag>" + E_TAG + "</ETag>"
                                                               + "</CompleteMultipartUploadResult>")));
        stubFor(put(anyUrl()).willReturn(aResponse().withStatus(200).withHeader("ETag", E_TAG)));
        stubFor(any(anyUrl()).atPriority(10).willReturn(aResponse().withStatus(200).withHeader("ETag", E_TAG)));
    }

    private static byte[] payload(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }
}
