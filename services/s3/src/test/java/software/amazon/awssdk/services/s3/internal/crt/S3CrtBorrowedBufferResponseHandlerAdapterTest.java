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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
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
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            new S3CrtBorrowedBufferResponseHandlerAdapter(executeFuture,
                                                          mock(SdkAsyncHttpResponseHandler.class),
                                                          null,
                                                          CompletableFuture.completedFuture(metaRequest),
                                                          capturingHandler(new AtomicReference<>(), true));
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(1));

        assertThat(handler.onResponseBody(crtBuffer, 0, 1)).isZero();

        verify(crtBuffer).close();
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
                capturingHandler(new AtomicReference<>(), false));

        assertThat(handler.onResponseBody(ByteBuffer.allocate(1), 0, 1)).isZero();
        assertThat(executeFuture).isCompletedExceptionally();
    }

    private static final class BlockingBufferHandler implements S3CrtBorrowedBufferStreamHandler {
        private final CountDownLatch bufferEntered = new CountDownLatch(1);
        private final CountDownLatch continueBuffer = new CountDownLatch(1);
        private final AtomicReference<S3CrtBorrowedBuffer> buffer = new AtomicReference<>();

        @Override
        public void onBorrowedStreamStart(Runnable cancellationAction) {
        }

        @Override
        public void onBorrowedBuffer(S3CrtBorrowedBuffer borrowedBuffer) {
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
            public void onBorrowedBuffer(S3CrtBorrowedBuffer buffer) {
                if (failOnNext) {
                    throw new RuntimeException("callback failed");
                }
                delivered.set(buffer);
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
