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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

class S3CrtBorrowedBufferResponseHandlerAdapterTest {

    @Test
    void borrowedCallback_whenConsumed_shouldReleaseBeforeReturningCredit() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        SdkAsyncHttpResponseHandler responseHandler = mock(SdkAsyncHttpResponseHandler.class);
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        CompletableFuture<S3MetaRequestWrapper> metaRequestFuture = CompletableFuture.completedFuture(metaRequest);
        AtomicReference<S3CrtBorrowedBuffer> delivered = new AtomicReference<>();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          responseHandler,
                                                          null,
                                                          metaRequestFuture,
                                                          new AtomicLong(),
                                                          capturingHandler(delivered, false));
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(3));

        assertThat(handler.onResponseBody(crtBuffer, 0, 3)).isZero();
        assertThat(delivered.get()).isNotNull();
        verify(crtBuffer, never()).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());

        delivered.get().consumed();

        InOrder releaseThenCredit = inOrder(crtBuffer, metaRequest);
        releaseThenCredit.verify(crtBuffer).close();
        releaseThenCredit.verify(metaRequest).incrementReadWindow(3);
    }

    @Test
    void borrowedCallback_whenAccepted_shouldCountResponseBytes() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          responseBytesRead,
                                                          capturingHandler(new AtomicReference<>(), false));
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(3));

        assertThat(handler.onResponseBody(crtBuffer, 0, 3)).isZero();

        assertThat(responseBytesRead).hasValue(3);
    }

    @Test
    void borrowedCallback_whenAccepted_shouldCountBeforeStartingStream() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        AtomicLong responseBytesRead = new AtomicLong();
        AtomicLong bytesObservedAtStreamStart = new AtomicLong(-1);
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(streamHandler.onBorrowedBuffer(any())).thenReturn(true);
        doAnswer(invocation -> {
            bytesObservedAtStreamStart.set(responseBytesRead.get());
            return null;
        }).when(streamHandler).onBorrowedStreamStart(any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          responseBytesRead,
                                                          streamHandler);

        assertThat(handler.onResponseBody(directCrtBuffer(3), 0, 3)).isZero();

        assertThat(bytesObservedAtStreamStart).hasValue(3);
    }

    @Test
    void borrowedCallbacks_whenContiguousFromZero_shouldAcceptAllBuffers() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(streamHandler.onBorrowedBuffer(any())).thenReturn(true);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          responseBytesRead,
                                                          streamHandler);

        assertThat(handler.onResponseBody(directCrtBuffer(3), 0, 3)).isZero();
        assertThat(handler.onResponseBody(directCrtBuffer(2), 3, 5)).isZero();

        verify(streamHandler, times(2)).onBorrowedBuffer(any());
        assertThat(responseBytesRead).hasValue(5);
        assertThat(executeFuture).isNotCompletedExceptionally();
    }

    @Test
    void borrowedCallback_whenContentRangeHasNonzeroStart_shouldUseHeaderStart() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(streamHandler.onBorrowedBuffer(any())).thenReturn(true);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          responseBytesRead,
                                                          streamHandler);
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "bytes 10-14/20")});

        assertThat(handler.onResponseBody(directCrtBuffer(5), 10, 15)).isZero();
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "bytes 20-21/30")});
        assertThat(handler.onResponseBody(directCrtBuffer(2), 15, 17)).isZero();

        verify(streamHandler, times(2)).onBorrowedBuffer(any());
        assertThat(responseBytesRead).hasValue(7);
        assertThat(executeFuture).isNotCompletedExceptionally();
    }

    @Test
    void borrowedCallback_whenFirstRangeDoesNotMatchContentRange_shouldRejectBeforeEnqueue() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          responseBytesRead,
                                                          streamHandler);
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "bytes 10-14/20")});
        S3BorrowedBuffer mismatched = directCrtBuffer(4);

        assertThat(handler.onResponseBody(mismatched, 11, 15)).isZero();

        verify(streamHandler, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(responseBytesRead).hasValue(0);
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 11, but the expected offset was 10");
    }

    @Test
    void borrowedCallback_whenContentRangeCannotBeParsed_shouldUseFirstCallbackStart() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(streamHandler.onBorrowedBuffer(any())).thenReturn(true);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          new AtomicLong(),
                                                          streamHandler);
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "invalid")});

        assertThat(handler.onResponseBody(directCrtBuffer(3), 10, 13)).isZero();

        verify(streamHandler).onBorrowedBuffer(any());
        assertThat(executeFuture).isNotCompletedExceptionally();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, Long.MAX_VALUE})
    void borrowedCallback_whenFallbackOffsetIsInvalid_shouldRejectBeforeEnqueue(long objectRangeStart) {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          responseBytesRead,
                                                          streamHandler);
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "invalid")});
        S3BorrowedBuffer mismatched = directCrtBuffer(1);

        assertThat(handler.onResponseBody(mismatched, objectRangeStart, objectRangeStart)).isZero();

        verify(streamHandler, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(responseBytesRead).hasValue(0);
        assertThat(executeFuture).isCompletedExceptionally();
    }

    @ParameterizedTest
    @ValueSource(longs = {6, 4, 0})
    void borrowedCallback_whenNextRangeIsNotContiguous_shouldReleaseAndFailWithoutCountingOrCredit(
        long actualStart) {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(streamHandler.onBorrowedBuffer(any())).thenReturn(true);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          responseBytesRead,
                                                          streamHandler);
        S3BorrowedBuffer first = directCrtBuffer(5);
        S3BorrowedBuffer mismatched = directCrtBuffer(2);
        handler.onResponseBody(first, 0, 5);

        assertThat(handler.onResponseBody(mismatched, actualStart, actualStart + 2)).isZero();

        verify(streamHandler).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(responseBytesRead).hasValue(5);
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset " + actualStart
                                 + ", but the expected offset was 5");
    }

    @Test
    void borrowedCallback_whenRangeLengthDoesNotMatchBuffer_shouldReleaseAndFailWithoutCountingOrCredit() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          responseBytesRead,
                                                          streamHandler);
        S3BorrowedBuffer mismatched = directCrtBuffer(3);

        assertThat(handler.onResponseBody(mismatched, 0, 4)).isZero();

        verify(streamHandler, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(responseBytesRead).hasValue(0);
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer ended at object offset 4, but the expected offset was 3");
    }

    @Test
    void borrowedCallback_whenRejected_shouldReleaseWithoutCountingResponseBytes() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(streamHandler.onBorrowedBuffer(any())).thenAnswer(invocation -> {
            S3CrtBorrowedBuffer buffer = invocation.getArgument(0);
            buffer.discard();
            return false;
        });
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          responseBytesRead,
                                                          streamHandler);
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(3));

        assertThat(handler.onResponseBody(crtBuffer, 0, 3)).isZero();

        verify(crtBuffer).close();
        assertThat(responseBytesRead).hasValue(0);
    }

    @Test
    void borrowedCallback_consumedBeforeMetaRequestPublished_shouldCreditAfterPublication() throws Exception {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        CompletableFuture<S3MetaRequestWrapper> metaRequestFuture = new CompletableFuture<>();
        AtomicReference<S3CrtBorrowedBuffer> delivered = new AtomicReference<>();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          metaRequestFuture,
                                                          new AtomicLong(),
                                                          capturingHandler(delivered, false));
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(3));
        handler.onResponseBody(crtBuffer, 0, 3);

        CompletableFuture.runAsync(delivered.get()::consumed).get(5, TimeUnit.SECONDS);

        verify(crtBuffer).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());

        metaRequestFuture.complete(metaRequest);

        verify(metaRequest).incrementReadWindow(3);
    }

    @Test
    void cancellationWhileFirstBufferCallbackBlocked_shouldReleaseWithoutCredit() throws Exception {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        BlockingBufferHandler streamHandler = new BlockingBufferHandler();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          new AtomicLong(),
                                                          streamHandler);
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(1));

        CompletableFuture<Void> callback = CompletableFuture.runAsync(() -> handler.onResponseBody(crtBuffer, 0, 1));
        assertThat(streamHandler.bufferEntered.await(5, TimeUnit.SECONDS)).isTrue();

        executeFuture.cancel(true);

        verify(crtBuffer).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        streamHandler.continueBuffer.countDown();
        callback.get(5, TimeUnit.SECONDS);
    }

    @Test
    void timeoutBeforeStreamStart_shouldAbortPreparedHandlerWithCause() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                      mock(SdkAsyncHttpResponseHandler.class),
                                                      null,
                                                      CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                      new AtomicLong(),
                                                      streamHandler);
        ApiCallTimeoutException timeout = ApiCallTimeoutException.create(1);

        executeFuture.completeExceptionally(timeout);

        verify(streamHandler).onBorrowedStreamAbort(timeout);
        verify(streamHandler, never()).onBorrowedStreamAbort();
    }

    @Test
    void borrowedCallback_whenBufferCallbackThrows_shouldReleaseAndFailRequest() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        AtomicLong responseBytesRead = new AtomicLong();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          responseBytesRead,
                                                          capturingHandler(new AtomicReference<>(), true));
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(1));

        assertThat(handler.onResponseBody(crtBuffer, 0, 1)).isZero();

        verify(crtBuffer).close();
        assertThat(responseBytesRead).hasValue(0);
        assertThat(executeFuture).isCompletedExceptionally();
    }

    @Test
    void heapCallback_shouldFailInsteadOfFallingBack() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(
                executeFuture,
                mock(SdkAsyncHttpResponseHandler.class),
                null,
                CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                new AtomicLong(),
                capturingHandler(new AtomicReference<>(), false));

        assertThat(handler.onResponseBody(ByteBuffer.allocate(1), 0, 1)).isZero();
        assertThat(executeFuture).isCompletedExceptionally();
    }

    @Test
    void borrowedCallback_whenStreamStartFailsAfterHandoff_shouldNotReleaseTheHandedOffBuffer() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        AtomicReference<S3CrtBorrowedBuffer> delivered = new AtomicReference<>();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        // The stream takes the buffer, and only then does starting the stream fail.
        when(streamHandler.onBorrowedBuffer(any())).thenAnswer(invocation -> {
            delivered.set(invocation.getArgument(0));
            return true;
        });
        doAnswer(invocation -> {
            throw new IllegalStateException("stream start failed");
        }).when(streamHandler).onBorrowedStreamStart(any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(
                                                              mock(S3MetaRequestWrapper.class)),
                                                          new AtomicLong(),
                                                          streamHandler);
        S3BorrowedBuffer crtBuffer = directCrtBuffer(3);

        assertThat(handler.onResponseBody(crtBuffer, 0, 3)).isZero();

        // The buffer is queued in the stream, where a reader can still reach it. Releasing it here too would hand the
        // pooled memory back while it is still readable, so the release has to be left to the stream's own cleanup.
        assertThat(delivered.get()).isNotNull();
        verify(crtBuffer, never()).close();
        assertThat(executeFuture).isCompletedExceptionally();
    }

    private static S3BorrowedBuffer directCrtBuffer(int size) {
        S3BorrowedBuffer buffer = mock(S3BorrowedBuffer.class);
        when(buffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(size));
        return buffer;
    }

    private static final class BlockingBufferHandler implements S3CrtBorrowedBufferStreamHandler {
        private final CountDownLatch bufferEntered = new CountDownLatch(1);
        private final CountDownLatch continueBuffer = new CountDownLatch(1);
        private final AtomicReference<S3CrtBorrowedBuffer> buffer = new AtomicReference<>();

        @Override
        public void onBorrowedStreamStart(Runnable cancellationAction) {
        }

        @Override
        public boolean onBorrowedBuffer(S3CrtBorrowedBuffer borrowedBuffer) {
            buffer.set(borrowedBuffer);
            bufferEntered.countDown();
            try {
                if (!continueBuffer.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to continue buffer callback");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return true;
        }

        @Override
        public void onBorrowedStreamComplete() {
        }

        @Override
        public void onBorrowedStreamError(Throwable error) {
        }

        @Override
        public void onBorrowedStreamAbort() {
            discardBuffer();
        }

        @Override
        public void onBorrowedStreamAbort(Throwable error) {
            discardBuffer();
        }

        private void discardBuffer() {
            S3CrtBorrowedBuffer borrowedBuffer = buffer.getAndSet(null);
            if (borrowedBuffer != null) {
                borrowedBuffer.discard();
            }
        }
    }

    private static S3CrtBorrowedBufferStreamHandler capturingHandler(
        AtomicReference<S3CrtBorrowedBuffer> delivered,
        boolean failOnNext) {
        return new S3CrtBorrowedBufferStreamHandler() {
            @Override
            public void onBorrowedStreamStart(Runnable cancellationAction) {
            }

            @Override
            public boolean onBorrowedBuffer(S3CrtBorrowedBuffer buffer) {
                if (failOnNext) {
                    throw new RuntimeException("callback failed");
                }
                delivered.set(buffer);
                return true;
            }

            @Override
            public void onBorrowedStreamComplete() {
            }

            @Override
            public void onBorrowedStreamError(Throwable error) {
            }

            @Override
            public void onBorrowedStreamAbort() {
            }

            @Override
            public void onBorrowedStreamAbort(Throwable error) {
            }
        };
    }
}
