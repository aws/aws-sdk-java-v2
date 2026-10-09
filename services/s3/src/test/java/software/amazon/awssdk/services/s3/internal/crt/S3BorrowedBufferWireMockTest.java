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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NavigableMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpProxyEnvironmentVariableSetting;
import software.amazon.awssdk.crt.http.HttpProxyEnvironmentVariableSetting.HttpProxyEnvironmentVariableType;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.crt.s3.S3ClientOptions;
import software.amazon.awssdk.crt.s3.S3DirectBufferPoolOptions;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestOptions;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;

@WireMockTest
@Timeout(15)
class S3BorrowedBufferWireMockTest {
    private static final String PATH = "/object";
    private static final int PART_SIZE = 256 * 1024;
    private static final int PART_COUNT = 3;
    private static final byte[] EXPECTED_BODY = payload(PART_SIZE * PART_COUNT);
    private static final long TIMEOUT_SECONDS = 10;

    @Test
    void getObject_borrowedCallbackReleasesAndCreditsReadWindow(WireMockRuntimeInfo wireMock) throws Exception {
        stubGetObject();

        URI endpoint = URI.create(wireMock.getHttpBaseUrl());
        NavigableMap<Long, byte[]> bodyChunks = new ConcurrentSkipListMap<>();
        AtomicInteger borrowedCallbackCount = new AtomicInteger();
        AtomicInteger byteBufferCallbackCount = new AtomicInteger();
        AtomicLong creditedBytes = new AtomicLong();
        AtomicLong pendingCredit = new AtomicLong();
        AtomicLong receivedBytes = new AtomicLong();
        AtomicBoolean allBuffersDirect = new AtomicBoolean(true);
        AtomicBoolean allBuffersClosed = new AtomicBoolean(true);
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CompletableFuture<S3FinishedResponseContext> finished = new CompletableFuture<>();

        EventLoopGroup eventLoopGroup = new EventLoopGroup(1);
        HostResolver hostResolver = null;
        ClientBootstrap clientBootstrap = null;
        S3Client s3Client = null;
        S3MetaRequest metaRequest = null;
        try {
            hostResolver = new HostResolver(eventLoopGroup);
            clientBootstrap = new ClientBootstrap(eventLoopGroup, hostResolver);

            HttpProxyEnvironmentVariableSetting proxyEnvironment = new HttpProxyEnvironmentVariableSetting();
            proxyEnvironment.setEnvironmentVariableType(HttpProxyEnvironmentVariableType.DISABLED);

            S3ClientOptions clientOptions = new S3ClientOptions()
                .withRegion("us-east-1")
                .withClientBootstrap(clientBootstrap)
                .withPartSize(PART_SIZE)
                .withDirectBufferPoolOptions(S3DirectBufferPoolOptions.fixed(PART_SIZE))
                .withReadBackpressureEnabled(true)
                .withInitialReadWindowSize(PART_SIZE)
                .withProxyEnvironmentVariableSetting(proxyEnvironment);
            s3Client = new S3Client(clientOptions);

            S3MetaRequestResponseHandler responseHandler = new S3MetaRequestResponseHandler() {
                @Override
                public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
                    byteBufferCallbackCount.incrementAndGet();
                    return bodyBytesIn.remaining();
                }

                @Override
                public int onResponseBody(S3BorrowedBuffer buffer, long objectRangeStart, long objectRangeEnd) {
                    borrowedCallbackCount.incrementAndGet();
                    callbackThread.compareAndSet(null, Thread.currentThread());
                    ByteBuffer view = buffer.asByteBuffer().duplicate();
                    int byteCount = view.remaining();
                    byte[] copy = new byte[byteCount];
                    view.get(copy);
                    bodyChunks.put(objectRangeStart, copy);
                    allBuffersDirect.compareAndSet(true, view.isDirect());

                    buffer.close();
                    allBuffersClosed.compareAndSet(true, isClosed(buffer));
                    pendingCredit.addAndGet(byteCount);
                    receivedBytes.addAndGet(byteCount);
                    return 0;
                }

                @Override
                public void onFinished(S3FinishedResponseContext context) {
                    finished.complete(context);
                }
            };

            HttpRequest request = new HttpRequest(
                "GET",
                PATH,
                new HttpHeader[] {new HttpHeader("Host", endpoint.getAuthority())},
                null);
            S3MetaRequestOptions requestOptions = new S3MetaRequestOptions()
                .withMetaRequestType(S3MetaRequestOptions.MetaRequestType.GET_OBJECT)
                .withHttpRequest(request)
                .withEndpoint(endpoint)
                .withObjectSizeHint((long) EXPECTED_BODY.length)
                .withResponseHandler(responseHandler);

            metaRequest = s3Client.makeMetaRequest(requestOptions);

            long stalledAtBytes = awaitStall(receivedBytes, finished);
            assertThat(stalledAtBytes).isLessThanOrEqualTo(PART_SIZE);
            assertThat(finished).isNotDone();
            long completionDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            while (!finished.isDone() || pendingCredit.get() > 0) {
                if (System.nanoTime() >= completionDeadline) {
                    throw new AssertionError("Request did not finish after replenishing the read window");
                }
                long increment = pendingCredit.getAndSet(0);
                if (increment > 0) {
                    metaRequest.incrementReadWindow(increment);
                    creditedBytes.addAndGet(increment);
                }
                if (!finished.isDone() || pendingCredit.get() > 0) {
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            }

            S3FinishedResponseContext context = finished.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(context.getErrorCode())
                .withFailMessage("CRT error code %s, HTTP status %s, cause %s",
                                 context.getErrorCode(), context.getResponseStatus(), context.getCause())
                .isZero();
            assertThat(byteBufferCallbackCount).hasValue(0);
            assertThat(borrowedCallbackCount.get())
                .as("borrowed callback thread: %s", callbackThread.get())
                .isGreaterThanOrEqualTo(PART_COUNT);
            assertThat(allBuffersDirect).isTrue();
            assertThat(allBuffersClosed).isTrue();
            assertThat(creditedBytes).hasValue(EXPECTED_BODY.length);
            assertThat(flatten(bodyChunks)).containsExactly(EXPECTED_BODY);
        } finally {
            closeResources(metaRequest, s3Client, clientBootstrap, hostResolver, eventLoopGroup);
        }
    }

    @Test
    void getObject_borrowedBufferCanBeConsumedAfterCallbackReturns(WireMockRuntimeInfo wireMock) throws Exception {
        stubGetObject();

        URI endpoint = URI.create(wireMock.getHttpBaseUrl());
        BlockingQueue<BorrowedChunk> borrowedChunks = new LinkedBlockingQueue<>();
        NavigableMap<Long, byte[]> bodyChunks = new ConcurrentSkipListMap<>();
        AtomicInteger borrowedCallbackCount = new AtomicInteger();
        AtomicInteger byteBufferCallbackCount = new AtomicInteger();
        AtomicLong maxCallbackRangeStart = new AtomicLong(-1);
        AtomicLong creditedBytes = new AtomicLong();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        CompletableFuture<S3FinishedResponseContext> finished = new CompletableFuture<>();

        EventLoopGroup eventLoopGroup = new EventLoopGroup(1);
        HostResolver hostResolver = null;
        ClientBootstrap clientBootstrap = null;
        S3Client s3Client = null;
        S3MetaRequest metaRequest = null;
        BorrowedChunk retainedChunk = null;
        try {
            hostResolver = new HostResolver(eventLoopGroup);
            clientBootstrap = new ClientBootstrap(eventLoopGroup, hostResolver);

            HttpProxyEnvironmentVariableSetting proxyEnvironment = new HttpProxyEnvironmentVariableSetting();
            proxyEnvironment.setEnvironmentVariableType(HttpProxyEnvironmentVariableType.DISABLED);

            S3ClientOptions clientOptions = new S3ClientOptions()
                .withRegion("us-east-1")
                .withClientBootstrap(clientBootstrap)
                .withPartSize(PART_SIZE)
                .withDirectBufferPoolOptions(S3DirectBufferPoolOptions.fixed(PART_SIZE))
                .withReadBackpressureEnabled(true)
                .withInitialReadWindowSize(EXPECTED_BODY.length)
                .withProxyEnvironmentVariableSetting(proxyEnvironment);
            s3Client = new S3Client(clientOptions);

            S3MetaRequestResponseHandler responseHandler = new S3MetaRequestResponseHandler() {
                @Override
                public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
                    byteBufferCallbackCount.incrementAndGet();
                    return bodyBytesIn.remaining();
                }

                @Override
                public int onResponseBody(S3BorrowedBuffer buffer, long objectRangeStart, long objectRangeEnd) {
                    borrowedCallbackCount.incrementAndGet();
                    callbackThread.compareAndSet(null, Thread.currentThread());
                    maxCallbackRangeStart.accumulateAndGet(objectRangeStart, Math::max);
                    borrowedChunks.add(new BorrowedChunk(buffer, buffer.asByteBuffer().duplicate(), objectRangeStart));
                    return 0;
                }

                @Override
                public void onFinished(S3FinishedResponseContext context) {
                    finished.complete(context);
                }
            };

            HttpRequest request = new HttpRequest(
                "GET",
                PATH,
                new HttpHeader[] {new HttpHeader("Host", endpoint.getAuthority())},
                null);
            S3MetaRequestOptions requestOptions = new S3MetaRequestOptions()
                .withMetaRequestType(S3MetaRequestOptions.MetaRequestType.GET_OBJECT)
                .withHttpRequest(request)
                .withEndpoint(endpoint)
                .withObjectSizeHint((long) EXPECTED_BODY.length)
                .withResponseHandler(responseHandler);

            metaRequest = s3Client.makeMetaRequest(requestOptions);
            retainedChunk = borrowedChunks.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(retainedChunk).isNotNull();
            assertThat(retainedChunk.view.isDirect()).isTrue();
            assertThat(callbackThread.get()).isNotSameAs(Thread.currentThread());

            TimeUnit.MILLISECONDS.sleep(250);
            assertThat(maxCallbackRangeStart).hasValueLessThan(PART_SIZE);
            assertThat(finished).isNotDone();

            long consumedBytes = 0;
            while (consumedBytes < EXPECTED_BODY.length) {
                consumedBytes += consume(retainedChunk, bodyChunks, metaRequest, creditedBytes);
                retainedChunk = null;
                if (consumedBytes < EXPECTED_BODY.length) {
                    retainedChunk = borrowedChunks.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    assertThat(retainedChunk).isNotNull();
                }
            }

            S3FinishedResponseContext context = finished.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(context.getErrorCode())
                .withFailMessage("CRT error code %s, HTTP status %s, cause %s",
                                 context.getErrorCode(), context.getResponseStatus(), context.getCause())
                .isZero();
            assertThat(byteBufferCallbackCount).hasValue(0);
            assertThat(borrowedCallbackCount.get()).isGreaterThanOrEqualTo(PART_COUNT);
            assertThat(creditedBytes).hasValue(EXPECTED_BODY.length);
            assertThat(flatten(bodyChunks)).containsExactly(EXPECTED_BODY);
        } finally {
            if (retainedChunk != null) {
                retainedChunk.buffer.close();
            }
            closeQueued(borrowedChunks);
            try {
                closeResources(metaRequest, s3Client, clientBootstrap, hostResolver, eventLoopGroup);
            } finally {
                closeQueued(borrowedChunks);
            }
        }
    }

    private static final class BorrowedChunk {
        private final S3BorrowedBuffer buffer;
        private final ByteBuffer view;
        private final long rangeStart;

        private BorrowedChunk(S3BorrowedBuffer buffer, ByteBuffer view, long rangeStart) {
            this.buffer = buffer;
            this.view = view;
            this.rangeStart = rangeStart;
        }
    }

    private static int consume(BorrowedChunk chunk,
                               NavigableMap<Long, byte[]> bodyChunks,
                               S3MetaRequest metaRequest,
                               AtomicLong creditedBytes) {
        byte[] copy = new byte[chunk.view.remaining()];
        chunk.view.get(copy);
        bodyChunks.put(chunk.rangeStart, copy);
        chunk.buffer.close();
        metaRequest.incrementReadWindow(copy.length);
        creditedBytes.addAndGet(copy.length);
        return copy.length;
    }

    private static void closeQueued(BlockingQueue<BorrowedChunk> chunks) {
        BorrowedChunk chunk;
        while ((chunk = chunks.poll()) != null) {
            chunk.buffer.close();
        }
    }

    private static void stubGetObject() {
        for (int i = 0; i < PART_COUNT; i++) {
            int start = i * PART_SIZE;
            int end = start + PART_SIZE - 1;
            byte[] body = Arrays.copyOfRange(EXPECTED_BODY, start, end + 1);
            stubFor(get(urlEqualTo(PATH))
                        .withHeader("Range", equalTo("bytes=" + start + "-" + end))
                        .willReturn(aResponse().withStatus(206)
                                                   .withHeader("Content-Length", String.valueOf(body.length))
                                                   .withHeader("Content-Range",
                                                               "bytes " + start + "-" + end + "/"
                                                               + EXPECTED_BODY.length)
                                                   .withHeader("ETag", "\"etag\"")
                                                   .withBody(body)));
        }
    }

    private static byte[] flatten(NavigableMap<Long, byte[]> chunks) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        chunks.values().forEach(chunk -> result.write(chunk, 0, chunk.length));
        return result.toByteArray();
    }

    private static byte[] payload(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) (i % 251);
        }
        return result;
    }

    private static long awaitStall(AtomicLong receivedBytes,
                                   CompletableFuture<S3FinishedResponseContext> finished) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        long unchangedSince = System.nanoTime();
        long previous = -1;
        while (System.nanoTime() < deadline) {
            if (finished.isDone()) {
                S3FinishedResponseContext context = finished.getNow(null);
                throw new AssertionError("Request finished before the read window stalled. Error code: "
                                         + context.getErrorCode());
            }
            long current = receivedBytes.get();
            if (current != previous) {
                previous = current;
                unchangedSince = System.nanoTime();
            } else if (current > 0
                       && System.nanoTime() - unchangedSince >= TimeUnit.MILLISECONDS.toNanos(250)) {
                return current;
            }
            TimeUnit.MILLISECONDS.sleep(25);
        }
        throw new AssertionError("Request did not reach a read-window stall");
    }

    private static boolean isClosed(S3BorrowedBuffer buffer) {
        try {
            buffer.asByteBuffer();
            return false;
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static void closeResources(S3MetaRequest metaRequest,
                                       S3Client client,
                                       ClientBootstrap bootstrap,
                                       HostResolver hostResolver,
                                       EventLoopGroup eventLoopGroup) throws Exception {
        try {
            close(metaRequest);
        } finally {
            try {
                close(client);
            } finally {
                try {
                    close(bootstrap);
                } finally {
                    try {
                        if (hostResolver != null) {
                            hostResolver.close();
                        }
                    } finally {
                        eventLoopGroup.close();
                        eventLoopGroup.getShutdownCompleteFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }

    private static void close(S3MetaRequest metaRequest) throws Exception {
        if (metaRequest != null) {
            metaRequest.close();
            metaRequest.getShutdownCompleteFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static void close(S3Client client) throws Exception {
        if (client != null) {
            client.close();
            client.getShutdownCompleteFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static void close(ClientBootstrap bootstrap) throws Exception {
        if (bootstrap != null) {
            bootstrap.close();
            bootstrap.getShutdownCompleteFuture().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }
}
