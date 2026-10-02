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
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.internal.crossregion.utils.CrossRegionUtils.AMZ_BUCKET_REGION_HEADER;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.crt.S3CrtRetryConfiguration;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@WireMockTest
@Timeout(20)
class S3CrtBorrowedBufferCrossRegionWireMockTest {
    private static final int PART_SIZE = 256 * 1024;
    private static final String RANGE = "bytes=0-" + (PART_SIZE - 1);
    private static final Region INITIAL_REGION = Region.US_EAST_1;
    private static final Region REDIRECT_REGION = Region.EU_WEST_1;
    private static final byte[] CONTENT = payload(PART_SIZE);

    @Test
    void redirectWithRegion_shouldUseAttemptBoundStreamAndReusePool(WireMockRuntimeInfo wireMock) throws Exception {
        int port = wireMock.getHttpPort();
        byte[] errorBody = errorXml("PermanentRedirect");
        stubFor(get(anyUrl()).withHeader("Host", equalTo("localhost:" + port))
                             .withHeader("Range", equalTo(RANGE))
                             .willReturn(aResponse().withStatus(301)
                                                        .withHeader("x-amz-bucket-region", REDIRECT_REGION.id())
                                                        .withHeader("Content-Type", "application/xml")
                                                        .withHeader("Content-Length", Integer.toString(errorBody.length))
                                                        .withBody(errorBody)));
        stubSuccessfulGet("127.0.0.1", port);

        try (S3AsyncClient client = newClient(wireMock, null)) {
            for (int request = 0; request < 2; request++) {
                try (ResponseInputStream<GetObjectResponse> stream = getObject(client, wireMock, "key")) {
                    assertThat(stream.response().sdkHttpResponse().statusCode()).isEqualTo(200);
                    assertThat(readAll(stream)).containsExactly(CONTENT);
                    assertThat(stream.read()).isEqualTo(-1);
                }
            }
        }

        List<LoggedRequest> requests = findAll(getRequestedFor(anyUrl()));
        assertThat(requests).hasSize(3);
        assertThat(requests).extracting(request -> request.getHeader("Host"))
                            .containsExactly("localhost:" + port, "127.0.0.1:" + port, "127.0.0.1:" + port);
        verify(0, headRequestedFor(anyUrl()));
    }

    @Test
    void redirectWithoutRegionHeader_shouldDiscoverRegionViaHeadBucketAndStream(WireMockRuntimeInfo wireMock)
        throws Exception {
        int port = wireMock.getHttpPort();
        byte[] errorBody = errorXml("PermanentRedirect");
        // The redirect carries no x-amz-bucket-region, so the cross-region client cannot learn the region from the
        // failure itself and has to fall back to HeadBucket. HeadBucket discovers the region only from a redirect
        // response, so the stub answers it the way S3 does: a 301 whose headers carry the bucket's real region.
        stubFor(get(anyUrl()).withHeader("Host", equalTo("localhost:" + port))
                             .withHeader("Range", equalTo(RANGE))
                             .willReturn(aResponse().withStatus(301)
                                                        .withHeader("Content-Type", "application/xml")
                                                        .withHeader("Content-Length", Integer.toString(errorBody.length))
                                                        .withBody(errorBody)));
        stubFor(head(anyUrl()).withHeader("Host", equalTo("localhost:" + port))
                              .willReturn(aResponse().withStatus(301)
                                                         .withHeader(AMZ_BUCKET_REGION_HEADER, REDIRECT_REGION.id())
                                                         .withHeader("Content-Length", "0")));
        stubSuccessfulGet("127.0.0.1", port);

        // A single-slot pool means the second download can only complete if the first one released every buffer it
        // borrowed, including the buffers of the attempt that was redirected away.
        try (S3AsyncClient client = newClient(wireMock, null)) {
            for (int request = 0; request < 2; request++) {
                try (ResponseInputStream<GetObjectResponse> stream = getObject(client, wireMock, "key")) {
                    assertThat(stream.response().sdkHttpResponse().statusCode()).isEqualTo(200);
                    assertThat(readAll(stream)).containsExactly(CONTENT);
                    assertThat(stream.read()).isEqualTo(-1);
                }
            }
        }

        // The redirected GET, then the region lookup, then the GET that is retried against the resolved region. The
        // second download reuses the cached region, so it goes straight to the redirect endpoint.
        assertThat(findAll(anyRequestedFor(anyUrl())))
            .extracting(request -> request.getMethod().getName() + " " + request.getHeader("Host"))
            .containsExactly("GET localhost:" + port,
                             "HEAD localhost:" + port,
                             "GET 127.0.0.1:" + port,
                             "GET 127.0.0.1:" + port);
    }

    @Test
    void cancellationDuringFirstAttempt_shouldReleasePoolForNextRequest(WireMockRuntimeInfo wireMock) throws Exception {
        int port = wireMock.getHttpPort();
        stubSuccessfulGet("localhost", port);
        HoldingExecutor completionExecutor = new HoldingExecutor();
        S3AsyncClient client = newClient(wireMock, completionExecutor);
        try {
            CompletableFuture<ResponseInputStream<GetObjectResponse>> first =
                getObjectFuture(client, wireMock, "cancelled");
            completionExecutor.awaitTask();
            assertThat(first).isNotDone();

            assertThat(first.cancel(true)).isTrue();
            completionExecutor.release();
            assertThat(first).isCancelled();

            try (ResponseInputStream<GetObjectResponse> second = getObject(client, wireMock, "after-cancel")) {
                assertThat(readAll(second)).containsExactly(CONTENT);
                assertThat(second.read()).isEqualTo(-1);
            }
        } finally {
            completionExecutor.release();
            client.close();
        }

        verify(2, getRequestedFor(anyUrl()).withHeader("Host", equalTo("localhost:" + port))
                                            .withHeader("Range", equalTo(RANGE)));
        verify(0, headRequestedFor(anyUrl()));
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock, Executor completionExecutor) {
        DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder builder =
            (DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder) S3AsyncClient.crtBuilder()
                         .region(INITIAL_REGION)
                         .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                         .credentialsProvider(StaticCredentialsProvider.create(
                             AwsBasicCredentials.create("key", "secret")))
                         .forcePathStyle(true)
                         .crossRegionAccessEnabled(true)
                         .minimumPartSizeInBytes((long) PART_SIZE)
                         .initialReadBufferSizeInBytes((long) PART_SIZE)
                         .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                         .retryConfiguration(S3CrtRetryConfiguration.builder().numRetries(0).build())
                         .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(PART_SIZE));
        if (completionExecutor != null) {
            builder.futureCompletionExecutor(completionExecutor);
        }
        return builder.build();
    }

    private static ResponseInputStream<GetObjectResponse> getObject(
        S3AsyncClient client,
        WireMockRuntimeInfo wireMock,
        String key) throws Exception {
        return getObjectFuture(client, wireMock, key).get(5, TimeUnit.SECONDS);
    }

    private static CompletableFuture<ResponseInputStream<GetObjectResponse>> getObjectFuture(
        S3AsyncClient client,
        WireMockRuntimeInfo wireMock,
        String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                                                   .bucket("bucket")
                                                   .key(key)
                                                   .overrideConfiguration(
                                                       config -> config.endpointProvider(
                                                           localRegionalEndpointProvider(wireMock.getHttpPort())))
                                                   .build();
        return client.getObject(request, S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers());
    }

    private static S3EndpointProvider localRegionalEndpointProvider(int port) {
        return params -> {
            String host = REDIRECT_REGION.equals(params.region()) ? "127.0.0.1" : "localhost";
            return CompletableFuture.completedFuture(
                Endpoint.builder().url(URI.create("http://" + host + ":" + port)).build());
        };
    }

    private static void stubSuccessfulGet(String host, int port) {
        stubFor(get(anyUrl()).withHeader("Host", equalTo(host + ":" + port))
                             .withHeader("Range", equalTo(RANGE))
                             .willReturn(aResponse().withStatus(206)
                                                        .withHeader("Content-Length", Integer.toString(CONTENT.length))
                                                        .withHeader("Content-Range",
                                                                    "bytes 0-" + (CONTENT.length - 1)
                                                                    + "/" + CONTENT.length)
                                                        .withHeader("ETag", "\"etag\"")
                                                        .withBody(CONTENT)));
    }

    private static byte[] errorXml(String code) {
        return ("<Error><Code>" + code + "</Code><Message>redirect</Message>"
                + "<RequestId>request-id</RequestId></Error>").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            result.write(buffer, 0, read);
        }
        return result.toByteArray();
    }

    private static byte[] payload(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }

    private static final class HoldingExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();
        private final CountDownLatch taskSubmitted = new CountDownLatch(1);
        private boolean released;

        @Override
        public void execute(Runnable command) {
            boolean runNow;
            synchronized (this) {
                runNow = released;
                if (!runNow) {
                    tasks.add(command);
                    taskSubmitted.countDown();
                }
            }
            if (runNow) {
                command.run();
            }
        }

        private void awaitTask() throws InterruptedException {
            assertThat(taskSubmitted.await(5, TimeUnit.SECONDS)).isTrue();
        }

        private void release() {
            List<Runnable> pending;
            synchronized (this) {
                if (released) {
                    return;
                }
                released = true;
                pending = new ArrayList<>(tasks);
                tasks.clear();
            }
            pending.forEach(Runnable::run);
        }
    }
}
