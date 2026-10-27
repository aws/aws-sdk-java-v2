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
import static org.mockito.Mockito.doThrow;
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
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

class S3CrtBorrowedBufferResponseHandlerAdapterTest {


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
        }).when(streamHandler).onBorrowedStreamStart(any(), any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          responseBytesRead,
                                                          streamHandler);

        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(3), 0, 3)).isZero();

        assertThat(bytesObservedAtStreamStart).hasValue(3);
    }

    @Test
    void successfulFinishWithoutBody_shouldStartAndCompleteStream() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          new AtomicLong(),
                                                          streamHandler);
        S3FinishedResponseContext context = mock(S3FinishedResponseContext.class);
        when(context.getErrorCode()).thenReturn(CRT.AWS_CRT_SUCCESS);

        handler.onFinished(context);

        verify(streamHandler).onBorrowedStreamStart(any(), any());
        verify(streamHandler).onBorrowedStreamComplete();
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

        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(3), 0, 3)).isZero();
        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(2), 3, 5)).isZero();

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

        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(5), 10, 15)).isZero();
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "bytes 20-21/30")});
        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(2), 15, 17)).isZero();

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

        assertThat(handler.onResponseBodyBorrowed(mismatched, 11, 15)).isZero();

        verify(streamHandler, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(responseBytesRead).hasValue(0);
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 11, but the expected offset was 10");
    }

    @Test
    void borrowedCallback_whenContentRangeCannotBeParsed_shouldRejectNonzeroStartBeforeEnqueue() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          new AtomicLong(),
                                                          streamHandler);
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "invalid")});
        S3BorrowedBuffer mismatched = directCrtBuffer(3);

        assertThat(handler.onResponseBodyBorrowed(mismatched, 10, 13)).isZero();

        verify(streamHandler, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 10, but the expected offset was 0");
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

        assertThat(handler.onResponseBodyBorrowed(mismatched, objectRangeStart, objectRangeStart)).isZero();

        verify(streamHandler, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(responseBytesRead).hasValue(0);
        if (objectRangeStart < 0) {
            assertThatThrownBy(executeFuture::join)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("CRT borrowed buffer started at object offset -1, but the expected offset was non-negative");
        } else {
            assertThat(executeFuture).isCompletedExceptionally();
        }
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
        handler.onResponseBodyBorrowed(first, 0, 5);

        assertThat(handler.onResponseBodyBorrowed(mismatched, actualStart, actualStart + 2)).isZero();

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

        assertThat(handler.onResponseBodyBorrowed(mismatched, 0, 4)).isZero();

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
            S3CrtBorrowedBufferLease buffer = invocation.getArgument(0);
            buffer.release();
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

        assertThat(handler.onResponseBodyBorrowed(crtBuffer, 0, 3)).isZero();

        verify(crtBuffer).close();
        assertThat(responseBytesRead).hasValue(0);
    }


    @Test
    void cancellationWhileFirstBufferCallbackBlocked_shouldReleaseWithoutCredit() throws Exception {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        BlockingBufferHandler streamHandler = blockingBufferHandler();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          new AtomicLong(),
                                                          streamHandler.handler);
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(1));

        CompletableFuture<Void> callback = CompletableFuture.runAsync(() -> handler.onResponseBodyBorrowed(crtBuffer, 0, 1));
        assertThat(streamHandler.bufferEntered.await(5, TimeUnit.SECONDS)).isTrue();

        executeFuture.cancel(true);

        verify(crtBuffer).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        streamHandler.continueBuffer.countDown();
        callback.get(5, TimeUnit.SECONDS);
    }

    @Test
    void failureBeforeStreamStart_shouldAbortHandlerWithCause() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                      mock(SdkAsyncHttpResponseHandler.class),
                                                      null,
                                                      CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                      new AtomicLong(),
                                                      streamHandler);
        RuntimeException failure = new RuntimeException("request failed");

        executeFuture.completeExceptionally(failure);

        verify(streamHandler).onBorrowedStreamAbort(failure);
        verify(streamHandler, never()).onBorrowedStreamError(any());
    }

    @Test
    void failureAfterStreamInitiatedBeforePublication_shouldReportStreamError() throws Exception {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferStreamHandler streamHandler = mock(S3CrtBorrowedBufferStreamHandler.class);
        CountDownLatch streamStartEntered = new CountDownLatch(1);
        CountDownLatch continueStreamStart = new CountDownLatch(1);
        when(streamHandler.onBorrowedBuffer(any())).thenReturn(true);
        doAnswer(invocation -> {
            streamStartEntered.countDown();
            assertThat(continueStreamStart.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(streamHandler).onBorrowedStreamStart(any(), any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)),
                                                          new AtomicLong(),
                                                          streamHandler);

        CompletableFuture<Void> callback =
            CompletableFuture.runAsync(() -> handler.onResponseBodyBorrowed(directCrtBuffer(1), 0, 1));
        assertThat(streamStartEntered.await(5, TimeUnit.SECONDS)).isTrue();
        RuntimeException failure = new RuntimeException("request failed");

        executeFuture.completeExceptionally(failure);

        verify(streamHandler).onBorrowedStreamError(failure);
        verify(streamHandler, never()).onBorrowedStreamAbort(failure);
        continueStreamStart.countDown();
        callback.get(5, TimeUnit.SECONDS);
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

        assertThat(handler.onResponseBodyBorrowed(crtBuffer, 0, 1)).isZero();

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
    void borrowedCallback_whenStreamStartFailsAfterHandoff_shouldAbortAndReleaseAcceptedBuffer() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        FailingStreamStartHandler streamHandler = failingStreamStartHandler();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          new AtomicLong(),
                                                          streamHandler.handler);
        S3BorrowedBuffer crtBuffer = directCrtBuffer(3);

        assertThat(handler.onResponseBodyBorrowed(crtBuffer, 0, 3)).isZero();

        assertThat(streamHandler.abortError).isInstanceOf(IllegalStateException.class)
                                             .hasMessage("stream start failed");
        verify(crtBuffer).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThat(executeFuture).isCompletedExceptionally();
    }

    private static S3BorrowedBuffer directCrtBuffer(int size) {
        S3BorrowedBuffer buffer = mock(S3BorrowedBuffer.class);
        when(buffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(size));
        return buffer;
    }

    private static BlockingBufferHandler blockingBufferHandler() {
        S3CrtBorrowedBufferStreamHandler handler = mock(S3CrtBorrowedBufferStreamHandler.class);
        BlockingBufferHandler result = new BlockingBufferHandler(handler);
        doAnswer(invocation -> {
            result.buffer.set(invocation.getArgument(0));
            result.bufferEntered.countDown();
            if (!result.continueBuffer.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to continue buffer callback");
            }
            return true;
        }).when(handler).onBorrowedBuffer(any());
        doAnswer(invocation -> {
            result.discardBuffer();
            return null;
        }).when(handler).onBorrowedStreamAbort();
        doAnswer(invocation -> {
            result.discardBuffer();
            return null;
        }).when(handler).onBorrowedStreamAbort(any());
        return result;
    }

    private static FailingStreamStartHandler failingStreamStartHandler() {
        S3CrtBorrowedBufferStreamHandler handler = mock(S3CrtBorrowedBufferStreamHandler.class);
        FailingStreamStartHandler result = new FailingStreamStartHandler(handler);
        doThrow(new IllegalStateException("stream start failed")).when(handler).onBorrowedStreamStart(any(), any());
        doAnswer(invocation -> {
            result.buffer.set(invocation.getArgument(0));
            return true;
        }).when(handler).onBorrowedBuffer(any());
        doAnswer(invocation -> {
            result.release(null);
            return null;
        }).when(handler).onBorrowedStreamAbort();
        doAnswer(invocation -> {
            result.release(invocation.getArgument(0));
            return null;
        }).when(handler).onBorrowedStreamAbort(any());
        return result;
    }

    private static S3CrtBorrowedBufferStreamHandler capturingHandler(
        AtomicReference<S3CrtBorrowedBufferLease> delivered,
        boolean failOnNext) {
        S3CrtBorrowedBufferStreamHandler handler = mock(S3CrtBorrowedBufferStreamHandler.class);
        when(handler.onBorrowedBuffer(any())).thenAnswer(invocation -> {
            if (failOnNext) {
                throw new RuntimeException("callback failed");
            }
            delivered.set(invocation.getArgument(0));
            return true;
        });
        return handler;
    }

    private static final class BlockingBufferHandler {
        private final S3CrtBorrowedBufferStreamHandler handler;
        private final CountDownLatch bufferEntered = new CountDownLatch(1);
        private final CountDownLatch continueBuffer = new CountDownLatch(1);
        private final AtomicReference<S3CrtBorrowedBufferLease> buffer = new AtomicReference<>();

        private BlockingBufferHandler(S3CrtBorrowedBufferStreamHandler handler) {
            this.handler = handler;
        }

        private void discardBuffer() {
            S3CrtBorrowedBufferLease borrowedBuffer = buffer.getAndSet(null);
            if (borrowedBuffer != null) {
                borrowedBuffer.release();
            }
        }
    }

    private static final class FailingStreamStartHandler {
        private final S3CrtBorrowedBufferStreamHandler handler;
        private final AtomicReference<S3CrtBorrowedBufferLease> buffer = new AtomicReference<>();
        private Throwable abortError;

        private FailingStreamStartHandler(S3CrtBorrowedBufferStreamHandler handler) {
            this.handler = handler;
        }

        private void release(Throwable error) {
            abortError = error;
            S3CrtBorrowedBufferLease borrowedBuffer = buffer.getAndSet(null);
            if (borrowedBuffer != null) {
                borrowedBuffer.release();
            }
        }
    }
}
