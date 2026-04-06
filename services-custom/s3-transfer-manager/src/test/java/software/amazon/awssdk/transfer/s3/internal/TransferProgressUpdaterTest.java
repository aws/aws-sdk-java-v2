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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.transfer.s3.CaptureTransferListener;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.transfer.s3.model.CompletedObjectTransfer;
import software.amazon.awssdk.transfer.s3.model.TransferObjectRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
import software.amazon.awssdk.utils.async.SimplePublisher;

class TransferProgressUpdaterTest {
    private static final long OBJ_SIZE = 16 * MB;
    private static File sourceFile;
    private CaptureTransferListener captureTransferListener;

    private static Stream<Arguments> contentLength() {
        return Stream.of(
            Arguments.of(OBJ_SIZE, "Total bytes equals transferred, future complete through subscriberOnNext()"),
            Arguments.of(OBJ_SIZE / 2, "Total bytes less than transferred, future complete through subscriberOnNext()"),
            Arguments.of(OBJ_SIZE * 2, "Total bytes more than transferred, future complete through subscriberOnComplete()"));
    }

    private static CompletableFuture<CompletedObjectTransfer> completedObjectResponse(long millis) {
        return CompletableFuture.supplyAsync(() -> {
            quietSleep(millis);
            return new CompletedObjectTransfer() {
                @Override
                public SdkResponse response() {
                    return PutObjectResponse.builder().eTag("ABCD").build();
                }
            };
        });
    }

    private static void quietSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void initiate() {
        captureTransferListener = new CaptureTransferListener();
    }

    @ParameterizedTest(name = "{index} - {1}, total bytes = {0}")
    @MethodSource("contentLength")
    void registerCompletion_differentTransferredByteRatios_alwaysCompletesOnce(Long givenContentLength, String description)
        throws Exception {
        TransferObjectRequest transferRequest = Mockito.mock(TransferObjectRequest.class);
        CaptureTransferListener mockListener = Mockito.mock(CaptureTransferListener.class);
        when(transferRequest.transferListeners()).thenReturn(Arrays.asList(LoggingTransferListener.create(), mockListener,
                                                                           captureTransferListener));

        sourceFile = new RandomTempFile(OBJ_SIZE);
        AsyncRequestBody requestBody = AsyncRequestBody.fromFile(sourceFile);
        TransferProgressUpdater transferProgressUpdater = new TransferProgressUpdater(transferRequest, givenContentLength);
        AsyncRequestBody asyncRequestBody = transferProgressUpdater.wrapRequestBody(requestBody, false);

        CompletableFuture<CompletedObjectTransfer> completionFuture = completedObjectResponse(10);
        transferProgressUpdater.registerCompletion(completionFuture);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);
        asyncRequestBody.subscribe(subscriber);

        captureTransferListener.getCompletionFuture().get(5, TimeUnit.SECONDS);

        Mockito.verify(mockListener, never()).transferFailed(ArgumentMatchers.any(TransferListener.Context.TransferFailed.class));
        Mockito.verify(mockListener, times(1)).transferComplete(ArgumentMatchers.any(TransferListener.Context.TransferComplete.class));
    }


    @Test
    void transferFailedWhenSubscriptionErrors() throws Exception {
        Long contentLength = 51L;
        String inputString = RandomStringUtils.randomAlphanumeric(contentLength.intValue());

        TransferObjectRequest transferRequest = Mockito.mock(TransferObjectRequest.class);
        CaptureTransferListener mockListener = Mockito.mock(CaptureTransferListener.class);
        when(transferRequest.transferListeners()).thenReturn(Arrays.asList(LoggingTransferListener.create(),
                                                                           mockListener,
                                                                           captureTransferListener));

        sourceFile = new RandomTempFile(OBJ_SIZE);
        AsyncRequestBody requestFileBody = AsyncRequestBody.fromInputStream(
            new ExceptionThrowingByteArrayInputStream(inputString.getBytes(), 3), contentLength,
            Executors.newSingleThreadExecutor());

        TransferProgressUpdater transferProgressUpdater = new TransferProgressUpdater(transferRequest, contentLength);
        AsyncRequestBody asyncRequestBody = transferProgressUpdater.wrapRequestBody(requestFileBody, false);

        CompletableFuture<CompletedObjectTransfer> future = completedObjectResponse(10);
        transferProgressUpdater.registerCompletion(future);

        AtomicReference<ByteBuffer> publishedBuffer = new AtomicReference<>();
        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(publishedBuffer::set);
        asyncRequestBody.subscribe(subscriber);

        assertThatExceptionOfType(ExecutionException.class).isThrownBy(
            () -> captureTransferListener.getCompletionFuture().get(5, TimeUnit.SECONDS));

        Mockito.verify(mockListener, times(1)).transferFailed(ArgumentMatchers.any(TransferListener.Context.TransferFailed.class));
        Mockito.verify(mockListener, never()).transferComplete(ArgumentMatchers.any(TransferListener.Context.TransferComplete.class));
    }

    @ParameterizedTest
    @ValueSource(longs = {8, 16, 31, 32, 33, 1024, Long.MAX_VALUE})
    void transferProgressUpdater_useContentRangeForTotalBytes(long contentLength) {
        TransferObjectRequest unusedMockTransferRequest = Mockito.mock(TransferObjectRequest.class);
        TransferProgressUpdater transferProgressUpdater = new TransferProgressUpdater(unusedMockTransferRequest, null);
        AsyncResponseTransformer<GetObjectResponse, Void> transformer =
            transferProgressUpdater.wrapResponseTransformerForMultipartDownload(
                new AsyncResponseTransformer<GetObjectResponse, Void>() {
                    @Override
                    public CompletableFuture<Void> prepare() {
                        return new CompletableFuture<>();
                    }

                    @Override
                    public void onResponse(GetObjectResponse response) {
                        // noop, test only
                    }

                    @Override
                    public void onStream(SdkPublisher<ByteBuffer> publisher) {
                        publisher.subscribe(b -> { /* do nothing, test only */ });
                    }

                    @Override
                    public void exceptionOccurred(Throwable error) {
                        // noop, test only
                    }
                }, GetObjectRequest.builder().build());
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder()
                                                .contentRange("bytes 0-127/" + contentLength)
                                                .build());
        TransferProgressSnapshot snapshot = transferProgressUpdater.progress().snapshot();
        assertThat(snapshot.totalBytes()).isPresent();
        assertThat(snapshot.totalBytes().getAsLong()).isEqualTo(contentLength);

        // simulate sending bytes
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        transformer.onStream(SdkPublisher.adapt(publisher));
        assertThat(transferProgressUpdater.progress().snapshot().transferredBytes()).isEqualTo(0L);

        publisher.send(ByteBuffer.wrap(new byte[] {0, 1, 2, 3, 4, 5, 6, 7})).join();
        assertThat(transferProgressUpdater.progress().snapshot().totalBytes().getAsLong()).isEqualTo(contentLength);
        assertThat(transferProgressUpdater.progress().snapshot().transferredBytes()).isEqualTo(8L);
    }

    @ParameterizedTest
    @ValueSource(longs = {8, 16, 31, 32, 33, 1024, Long.MAX_VALUE})
    void transferProgressUpdater_useContentLengthWhenRangeGet(long contentLength) {
        TransferObjectRequest unusedMockTransferRequest = Mockito.mock(TransferObjectRequest.class);
        TransferProgressUpdater transferProgressUpdater = new TransferProgressUpdater(unusedMockTransferRequest, null);
        AsyncResponseTransformer<GetObjectResponse, Void> transformer =
            transferProgressUpdater.wrapResponseTransformerForMultipartDownload(
                new AsyncResponseTransformer<GetObjectResponse, Void>() {
                    @Override
                    public CompletableFuture<Void> prepare() {
                        return new CompletableFuture<>();
                    }

                    @Override
                    public void onResponse(GetObjectResponse response) {
                        // noop, test only
                    }

                    @Override
                    public void onStream(SdkPublisher<ByteBuffer> publisher) {
                        publisher.subscribe(b -> { /* do nothing, test only */ });
                    }

                    @Override
                    public void exceptionOccurred(Throwable error) {
                        // noop, test only
                    }
                }, GetObjectRequest.builder().range("bytes=0-" + contentLength).build());
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder()
                                                .contentLength(contentLength)
                                                .build());
        TransferProgressSnapshot snapshot = transferProgressUpdater.progress().snapshot();
        assertThat(snapshot.totalBytes()).isPresent();
        assertThat(snapshot.totalBytes().getAsLong()).isEqualTo(contentLength);

        // simulate sending bytes
        SimplePublisher<ByteBuffer> publisher = new SimplePublisher<>();
        transformer.onStream(SdkPublisher.adapt(publisher));
        assertThat(transferProgressUpdater.progress().snapshot().transferredBytes()).isEqualTo(0L);

        publisher.send(ByteBuffer.wrap(new byte[] {0, 1, 2, 3, 4, 5, 6, 7})).join();
        assertThat(transferProgressUpdater.progress().snapshot().totalBytes().getAsLong()).isEqualTo(contentLength);
        assertThat(transferProgressUpdater.progress().snapshot().transferredBytes()).isEqualTo(8L);

    }


    @Test
    void wrapRequestBody_disableIncrementalProgress_doesNotReportProgress() {
        byte[] data = new byte[1024];
        Arrays.fill(data, (byte) 'a');
        long contentLength = data.length;

        TransferObjectRequest transferRequest = Mockito.mock(TransferObjectRequest.class);
        when(transferRequest.transferListeners()).thenReturn(Arrays.asList(captureTransferListener));

        TransferProgressUpdater updater = new TransferProgressUpdater(transferRequest, contentLength);
        AsyncRequestBody inMemoryBody = AsyncRequestBody.fromBytes(data);
        AsyncRequestBody wrappedBody = updater.wrapRequestBody(inMemoryBody, true);

        // Use a custom subscriber that captures progress snapshots at each stage
        AtomicReference<Long> bytesAfterOnNext = new AtomicReference<>();
        AtomicReference<Long> bytesAfterOnComplete = new AtomicReference<>();

        wrappedBody.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                bytesAfterOnNext.set(updater.progress().snapshot().transferredBytes());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                bytesAfterOnComplete.set(updater.progress().snapshot().transferredBytes());
            }
        });

        // When disableIncrementalProgress is true, the wrapper should NOT report any progress.
        // Progress is instead reported by the JAVA_PROGRESS_LISTENER after the server responds.
        assertThat(bytesAfterOnNext.get()).isNotNull();
        assertThat(bytesAfterOnNext.get()).isEqualTo(0L);
        assertThat(bytesAfterOnComplete.get()).isEqualTo(0L);
        assertThat(updater.progress().snapshot().transferredBytes()).isEqualTo(0L);
    }

    @Test
    void wrapRequestBody_noSuppression_reportsProgressPerByte() throws Exception {
        long fileSize = 1024;
        sourceFile = new RandomTempFile(fileSize);

        TransferObjectRequest transferRequest = Mockito.mock(TransferObjectRequest.class);
        when(transferRequest.transferListeners()).thenReturn(Arrays.asList(captureTransferListener));

        TransferProgressUpdater updater = new TransferProgressUpdater(transferRequest, fileSize);
        AsyncRequestBody fileBody = AsyncRequestBody.fromFile(sourceFile);
        AsyncRequestBody wrappedBody = updater.wrapRequestBody(fileBody, false);

        CompletableFuture<CompletedObjectTransfer> completionFuture = completedObjectResponse(10);
        updater.registerCompletion(completionFuture);

        // For file-based bodies, progress should be reported incrementally during onNext
        AtomicBoolean progressReportedDuringOnNext = new AtomicBoolean(false);
        CompletableFuture<Void> subscriberDone = new CompletableFuture<>();

        wrappedBody.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                long transferred = updater.progress().snapshot().transferredBytes();
                if (transferred > 0) {
                    progressReportedDuringOnNext.set(true);
                }
            }

            @Override
            public void onError(Throwable t) {
                subscriberDone.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                subscriberDone.complete(null);
            }
        });

        subscriberDone.get(5, TimeUnit.SECONDS);

        // File body should have reported progress during onNext, not deferred
        assertThat(progressReportedDuringOnNext.get()).isTrue();
        assertThat(updater.progress().snapshot().transferredBytes()).isEqualTo(fileSize);
    }

    private static class ExceptionThrowingByteArrayInputStream extends ByteArrayInputStream {
        private final int exceptionPosition;

        public ExceptionThrowingByteArrayInputStream(byte[] buf, int exceptionPosition) {
            super(buf);
            this.exceptionPosition = exceptionPosition;
        }

        @Override
        public int read() {
            return (exceptionPosition == pos + 1) ? exceptionThrowingRead() : super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return (exceptionPosition >= pos && exceptionPosition < (pos + len)) ?
                   exceptionThrowingReadByteArr() : super.read(b, off, len);
        }

        private int exceptionThrowingRead() {
            throw new RuntimeException("Exception occurred at position " + (pos + 1));
        }

        private int exceptionThrowingReadByteArr() {
            throw new RuntimeException("Exception occurred in read(byte[]) at position " + exceptionPosition);
        }
    }

}