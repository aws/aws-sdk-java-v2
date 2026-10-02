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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.crt.S3CrtDirectBufferPoolConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WireMockTest
@Timeout(15)
class S3CrtBorrowedBufferClientWireMockTest {
    private static final int PART_SIZE = 256 * 1024;
    private static final int PART_COUNT = 3;
    private static final byte[] CONTENT = payload(PART_SIZE * PART_COUNT);
    private static final String E_TAG = "\"borrowed-etag\"";

    @Test
    void multipartDownload_withOneSlotPool_shouldStreamAllParts(WireMockRuntimeInfo wireMock) throws Exception {
        stubMultipartObject(CONTENT);

        try (S3AsyncClient client = newClient(wireMock);
             ResponseInputStream<GetObjectResponse> stream = getObject(client)) {
            assertThat(readAll(stream)).containsExactly(CONTENT);
        }
    }

    @Test
    void getObjectWithPartNumber_shouldUseResolvedContentRangeStart(WireMockRuntimeInfo wireMock) throws Exception {
        int start = PART_SIZE;
        int end = 2 * PART_SIZE - 1;
        byte[] part = Arrays.copyOfRange(CONTENT, start, end + 1);
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(206)
                                                   .withHeader("Content-Length", Integer.toString(part.length))
                                                   .withHeader("Content-Range",
                                                               "bytes " + start + "-" + end + "/" + CONTENT.length)
                                                   .withHeader("ETag", E_TAG)
                                                   .withBody(part)));

        try (S3AsyncClient client = newClient(wireMock);
             ResponseInputStream<GetObjectResponse> stream =
                 client.getObject(r -> r.bucket("bucket").key("key").partNumber(2),
                                  S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers())
                       .get(10, TimeUnit.SECONDS)) {
            assertThat(stream.response().contentRange()).isEqualTo("bytes " + start + "-" + end + "/" + CONTENT.length);
            assertThat(readAll(stream)).containsExactly(part);
        }
    }

    @Test
    void borrowedDownload_shouldRecordResponseBytes(WireMockRuntimeInfo wireMock) throws Exception {
        stubMultipartObject(CONTENT);
        AtomicLong responseBytesRead = new AtomicLong();
        ExecutionInterceptor metricCaptor = new ExecutionInterceptor() {
            @Override
            public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
                responseBytesRead.set(executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESPONSE_BYTES_READ).get());
            }
        };

        try (S3AsyncClient client = newClient(wireMock, null, metricCaptor);
             ResponseInputStream<GetObjectResponse> stream = getObject(client)) {
            assertThat(readAll(stream)).containsExactly(CONTENT);
        }

        assertThat(responseBytesRead).hasValue(PART_SIZE);
    }

    @Test
    void closeMidDownload_shouldReleasePoolForNextRequest(WireMockRuntimeInfo wireMock) throws Exception {
        stubMultipartObject(CONTENT);

        try (S3AsyncClient client = newClient(wireMock)) {
            ResponseInputStream<GetObjectResponse> first = getObject(client);
            assertThat(first.read()).isEqualTo(CONTENT[0] & 0xff);
            first.close();

            try (ResponseInputStream<GetObjectResponse> second = getObject(client)) {
                assertThat(readAll(second)).containsExactly(CONTENT);
            }
        }
    }

    @Test
    void clientFutureCancelledBeforeStreamPublished_shouldReleasePoolForNextRequest(
        WireMockRuntimeInfo wireMock) throws Exception {
        stubMultipartObject(CONTENT);
        HoldingExecutor completionExecutor = new HoldingExecutor();
        S3AsyncClient client = newClient(wireMock, completionExecutor);
        try {
            CompletableFuture<ResponseInputStream<GetObjectResponse>> first = getObjectFuture(client);
            completionExecutor.awaitTask();
            assertThat(first).isNotDone();

            first.cancel(true);
            completionExecutor.release();

            assertThat(first).isCancelled();
            try (ResponseInputStream<GetObjectResponse> second = getObject(client)) {
                assertThat(readAll(second)).containsExactly(CONTENT);
            }
        } finally {
            completionExecutor.release();
            client.close();
        }
    }

    @Test
    void afterExecutionFailureAfterStreamPublication_shouldReleasePoolForNextRequest(WireMockRuntimeInfo wireMock)
        throws Exception {
        stubMultipartObject(CONTENT);
        AtomicInteger afterExecutionCalls = new AtomicInteger();
        ExecutionInterceptor failFirstAfterExecution = new ExecutionInterceptor() {
            @Override
            public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
                if (afterExecutionCalls.getAndIncrement() == 0) {
                    throw new IllegalStateException("after execution failed");
                }
            }
        };

        try (S3AsyncClient client = newClient(wireMock, null, failFirstAfterExecution)) {
            assertThatThrownBy(() -> getObject(client)).hasRootCauseMessage("after execution failed");

            try (ResponseInputStream<GetObjectResponse> stream = getObject(client)) {
                assertThat(readAll(stream)).containsExactly(CONTENT);
            }
        }
    }

    @Test
    void getObject_whenServiceReturns404_shouldFailWithoutCreatingStream(WireMockRuntimeInfo wireMock) {
        byte[] errorBody = ("<Error><Code>NoSuchKey</Code><Message>missing</Message>"
                            + "<RequestId>request-id</RequestId></Error>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(404)
                                                    .withHeader("x-amz-request-id", "request-id")
                                                    .withHeader("Content-Length", "0")));
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(404)
                                                   .withHeader("Content-Type", "application/xml")
                                                   .withHeader("Content-Length", Integer.toString(errorBody.length))
                                                   .withHeader("x-amz-request-id", "request-id")
                                                   .withBody(errorBody)));

        try (S3AsyncClient client = newClient(wireMock)) {
            assertThatThrownBy(() -> getObject(client))
                .satisfies(throwable -> {
                    Throwable rootCause = throwable;
                    while (rootCause.getCause() != null) {
                        rootCause = rootCause.getCause();
                    }
                    assertThat(rootCause).isInstanceOf(S3Exception.class);
                    assertThat(((S3Exception) rootCause).statusCode()).isEqualTo(404);
                });
        }
    }

    @Test
    void failureAfterFirstPart_shouldReturnAcceptedBytesThenThrow(WireMockRuntimeInfo wireMock) throws Exception {
        stubHeadObject(CONTENT.length);
        stubSuccessfulRange(0, CONTENT);
        stubFor(get(anyUrl()).atPriority(10)
                             .willReturn(aResponse().withStatus(404)
                                                        .withHeader("x-amz-request-id", "request-id")
                                                        .withHeader("Content-Length", "0")));

        try (S3AsyncClient client = newClient(wireMock);
             ResponseInputStream<GetObjectResponse> stream = getObject(client)) {
            byte[] firstPart = new byte[PART_SIZE];
            readExactly(stream, firstPart);
            assertThat(firstPart).containsExactly(Arrays.copyOfRange(CONTENT, 0, PART_SIZE));
            assertThatThrownBy(stream::read).isInstanceOf(IOException.class);
        }
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock) {
        return newClient(wireMock, null);
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock, Executor completionExecutor) {
        return newClient(wireMock, completionExecutor, null);
    }

    private static S3AsyncClient newClient(WireMockRuntimeInfo wireMock,
                                           Executor completionExecutor,
                                           ExecutionInterceptor executionInterceptor) {
        DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder builder =
            (DefaultS3CrtAsyncClient.DefaultS3CrtClientBuilder) S3AsyncClient.crtBuilder()
                         .region(Region.US_EAST_1)
                         .endpointOverride(URI.create("http://localhost:" + wireMock.getHttpPort()))
                         .credentialsProvider(StaticCredentialsProvider.create(
                             AwsBasicCredentials.create("key", "secret")))
                         .minimumPartSizeInBytes((long) PART_SIZE)
                         .initialReadBufferSizeInBytes((long) PART_SIZE)
                         .directBufferPoolConfiguration(S3CrtDirectBufferPoolConfiguration.fixed(PART_SIZE));
        if (completionExecutor != null) {
            builder.futureCompletionExecutor(completionExecutor);
        }
        if (executionInterceptor != null) {
            builder.addExecutionInterceptor(executionInterceptor);
        }
        return builder.build();
    }

    private static CompletableFuture<ResponseInputStream<GetObjectResponse>> getObjectFuture(S3AsyncClient client) {
        return client.getObject(r -> r.bucket("bucket").key("key"),
                                S3AsyncResponseTransformer.toBlockingInputStreamWithBorrowedBuffers());
    }

    private static ResponseInputStream<GetObjectResponse> getObject(S3AsyncClient client) throws Exception {
        return getObjectFuture(client).get(10, TimeUnit.SECONDS);
    }

    private static void stubMultipartObject(byte[] content) {
        stubHeadObject(content.length);
        for (int part = 0; part < PART_COUNT; part++) {
            stubSuccessfulRange(part, content);
        }
    }

    private static void stubHeadObject(int contentLength) {
        stubFor(head(anyUrl()).willReturn(aResponse().withStatus(200)
                                                    .withHeader("Content-Length", Integer.toString(contentLength))
                                                    .withHeader("ETag", E_TAG)));
    }

    private static void stubSuccessfulRange(int part, byte[] content) {
        int start = part * PART_SIZE;
        int end = Math.min(start + PART_SIZE, content.length) - 1;
        byte[] body = Arrays.copyOfRange(content, start, end + 1);
        stubFor(get(anyUrl()).atPriority(1)
                             .withHeader("Range", equalTo("bytes=" + start + "-" + end))
                             .willReturn(aResponse().withStatus(206)
                                                        .withHeader("Content-Length", Integer.toString(body.length))
                                                        .withHeader("Content-Range",
                                                                    "bytes " + start + "-" + end + "/" + content.length)
                                                        .withHeader("ETag", E_TAG)
                                                        .withBody(body)));
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

    private static void readExactly(InputStream stream, byte[] destination) throws IOException {
        int offset = 0;
        while (offset < destination.length) {
            int read = stream.read(destination, offset, destination.length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of stream");
            }
            offset += read;
        }
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

    private static byte[] payload(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }
}
