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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

class S3CrtBorrowedBufferResponseHandlerAdapterTest {


    @Test
    void successfulFinishWithoutBody_shouldStartAndCompleteStream() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, transformer).build();
        S3FinishedResponseContext context = mock(S3FinishedResponseContext.class);
        when(context.getErrorCode()).thenReturn(CRT.AWS_CRT_SUCCESS);

        handler.onFinished(context);

        verify(transformer).onBorrowedStreamStart(any(), any());
        verify(transformer).onBorrowedStreamComplete();
    }

    @Test
    void borrowedCallbacks_whenContiguousFromContentRangeStart_shouldAcceptAllBuffers() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        when(transformer.onBorrowedBuffer(any())).thenReturn(true);
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer).build();
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "bytes 10-14/20")});

        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(3), 10, 13)).isZero();
        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(2), 13, 15)).isZero();

        verify(transformer, times(2)).onBorrowedBuffer(any());
        assertThat(executeFuture).isNotCompletedExceptionally();
    }

    @Test
    void borrowedCallback_whenNotContiguous_shouldReleaseAndFailWithoutDelivering() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        when(transformer.onBorrowedBuffer(any())).thenReturn(true);
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer).build();
        S3BorrowedBuffer outOfOrder = directCrtBuffer(3);

        assertThat(handler.onResponseBodyBorrowed(directCrtBuffer(3), 0, 3)).isZero();
        assertThat(handler.onResponseBodyBorrowed(outOfOrder, 6, 9)).isZero();

        verify(transformer, times(1)).onBorrowedBuffer(any());
        verify(outOfOrder).close();
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 6, but the expected offset was 3");
    }

    @Test
    void borrowedCallback_whenFirstRangeDoesNotMatchContentRange_shouldReleaseAndFailWithoutDelivering() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer).build();
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "bytes 10-14/20")});
        S3BorrowedBuffer mismatched = directCrtBuffer(4);

        assertThat(handler.onResponseBodyBorrowed(mismatched, 11, 15)).isZero();

        verify(transformer, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 11, but the expected offset was 10");
    }

    @Test
    void borrowedCallback_whenNoContentRangeAndFirstRangeIsNonzero_shouldReleaseAndFailWithoutDelivering() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer).build();
        handler.onResponseHeaders(200, new HttpHeader[0]);
        S3BorrowedBuffer mismatched = directCrtBuffer(3);

        assertThat(handler.onResponseBodyBorrowed(mismatched, 10, 13)).isZero();

        verify(transformer, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 10, but the expected offset was 0");
    }

    @Test
    void borrowedCallback_whenContentRangeCannotBeParsedAndFirstRangeIsNonzero_shouldReleaseAndFailWithoutDelivering() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer).build();
        handler.onResponseHeaders(206, new HttpHeader[] {new HttpHeader("Content-Range", "invalid")});
        S3BorrowedBuffer mismatched = directCrtBuffer(3);

        assertThat(handler.onResponseBodyBorrowed(mismatched, 10, 13)).isZero();

        verify(transformer, never()).onBorrowedBuffer(any());
        verify(mismatched).close();
        assertThatThrownBy(executeFuture::join)
            .hasRootCauseMessage("CRT borrowed buffer started at object offset 10, but the expected offset was 0");
    }

    @Test
    void borrowedCallback_whenRejected_shouldRelease() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        when(transformer.onBorrowedBuffer(any())).thenAnswer(invocation -> {
            S3BorrowedBuffer buffer = invocation.getArgument(0);
            buffer.close();
            return false;
        });
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer).build();
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(3));

        assertThat(handler.onResponseBodyBorrowed(crtBuffer, 0, 3)).isZero();

        verify(crtBuffer).close();
    }


    @Test
    void cancellationWhileFirstBufferCallbackBlocked_shouldReportCancellationToStream() throws Exception {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        BlockingBufferHandler transformer = blockingBufferHandler();
        S3CrtBorrowedBufferResponseHandlerAdapter handler = adapterBuilder(executeFuture, transformer.handler).build();
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(1));

        CompletableFuture<Void> callback = CompletableFuture.runAsync(() -> handler.onResponseBodyBorrowed(crtBuffer, 0, 1));
        assertThat(transformer.bufferEntered.await(5, TimeUnit.SECONDS)).isTrue();

        executeFuture.cancel(true);

        verify(transformer.handler).onBorrowedStreamError(any(CancellationException.class));
        transformer.continueBuffer.countDown();
        callback.get(5, TimeUnit.SECONDS);
    }
    @Test
    void creditBeforeMetaRequestIsAvailable_shouldBeSentOnceItIs() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        CompletableFuture<S3MetaRequestWrapper> metaRequestFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        when(transformer.onBorrowedBuffer(any())).thenReturn(true);
        AtomicReference<LongConsumer> readWindowAction = new AtomicReference<>();
        doAnswer(invocation -> {
            readWindowAction.set(invocation.getArgument(1));
            return null;
        }).when(transformer).onBorrowedStreamStart(any(), any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, transformer).metaRequestFuture(metaRequestFuture).build();
        handler.onResponseBodyBorrowed(directCrtBuffer(3), 0, 3);

        readWindowAction.get().accept(3);
        verify(metaRequest, never()).incrementReadWindow(anyLong());

        metaRequestFuture.complete(metaRequest);
        verify(metaRequest).incrementReadWindow(3);
    }

    @Test
    void nativeCreditFailure_shouldFailRequestAndReportToStream() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        RuntimeException failure = new RuntimeException("credit failed");
        doThrow(failure).when(metaRequest).incrementReadWindow(3);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        when(transformer.onBorrowedBuffer(any())).thenReturn(true);
        AtomicReference<LongConsumer> readWindowAction = new AtomicReference<>();
        doAnswer(invocation -> {
            readWindowAction.set(invocation.getArgument(1));
            return null;
        }).when(transformer).onBorrowedStreamStart(any(), any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, transformer)
                .metaRequestFuture(CompletableFuture.completedFuture(metaRequest))
                .build();
        handler.onResponseBodyBorrowed(directCrtBuffer(3), 0, 3);

        readWindowAction.get().accept(3);

        assertThatThrownBy(executeFuture::join).hasCause(failure);
        verify(transformer).onBorrowedStreamError(failure);
        verify(metaRequest, times(1)).incrementReadWindow(3);
    }

    @Test
    void failureBeforeStreamStart_shouldNotNotifyTransformer() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        adapterBuilder(executeFuture, transformer).build();

        executeFuture.completeExceptionally(new RuntimeException("request failed"));

        verify(transformer, never()).onBorrowedStreamError(any());
    }
    @Test
    void failureWhileStreamStarting_shouldBeReportedOnceStarted() throws Exception {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        CountDownLatch streamStartEntered = new CountDownLatch(1);
        CountDownLatch continueStreamStart = new CountDownLatch(1);
        when(transformer.onBorrowedBuffer(any())).thenReturn(true);
        doAnswer(invocation -> {
            streamStartEntered.countDown();
            assertThat(continueStreamStart.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(transformer).onBorrowedStreamStart(any(), any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, transformer).build();

        CompletableFuture<Void> callback =
            CompletableFuture.runAsync(() -> handler.onResponseBodyBorrowed(directCrtBuffer(1), 0, 1));
        assertThat(streamStartEntered.await(5, TimeUnit.SECONDS)).isTrue();
        RuntimeException failure = new RuntimeException("request failed");

        executeFuture.completeExceptionally(failure);
        verify(transformer, never()).onBorrowedStreamError(any());
        continueStreamStart.countDown();
        callback.get(5, TimeUnit.SECONDS);

        verify(transformer).onBorrowedStreamError(failure);
    }
    @Test
    void borrowedCallback_whenBufferCallbackThrows_shouldReleaseAndFailRequest() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, capturingHandler(new AtomicReference<>(), true))
                .metaRequestFuture(CompletableFuture.completedFuture(metaRequest))
                .build();
        S3BorrowedBuffer crtBuffer = mock(S3BorrowedBuffer.class);
        when(crtBuffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(1));

        assertThat(handler.onResponseBodyBorrowed(crtBuffer, 0, 1)).isZero();

        verify(crtBuffer).close();
        assertThat(executeFuture).isCompletedExceptionally();
    }

    @Test
    void heapCallback_shouldFailInsteadOfFallingBack() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, capturingHandler(new AtomicReference<>(), false)).build();

        assertThat(handler.onResponseBody(ByteBuffer.allocate(1), 0, 1)).isZero();
        assertThat(executeFuture).isCompletedExceptionally();
    }

    @Test
    void borrowedCallback_whenStreamStartFails_shouldReleaseBufferAndFailWithoutDelivering() {
        CompletableFuture<Void> executeFuture = new CompletableFuture<>();
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        doThrow(new IllegalStateException("stream start failed")).when(transformer).onBorrowedStreamStart(any(), any());
        S3CrtBorrowedBufferResponseHandlerAdapter handler =
            adapterBuilder(executeFuture, transformer)
                .metaRequestFuture(CompletableFuture.completedFuture(metaRequest))
                .build();
        S3BorrowedBuffer crtBuffer = directCrtBuffer(3);

        assertThat(handler.onResponseBodyBorrowed(crtBuffer, 0, 3)).isZero();

        verify(transformer, never()).onBorrowedBuffer(any());
        verify(crtBuffer).close();
        verify(metaRequest, never()).incrementReadWindow(anyLong());
        assertThatThrownBy(executeFuture::join).hasRootCauseMessage("stream start failed");
    }

    private static S3CrtBorrowedBufferResponseHandlerAdapter.Builder adapterBuilder(
        CompletableFuture<Void> executeFuture, S3CrtBorrowedBufferBlockingResponseTransformer transformer) {
        return S3CrtBorrowedBufferResponseHandlerAdapter.builder()
                                                         .executeFuture(executeFuture)
                                                         .responseHandler(mock(SdkAsyncHttpResponseHandler.class))
                                                         .metaRequestFuture(
                                                             CompletableFuture.completedFuture(mock(S3MetaRequestWrapper.class)))
                                                         .transformer(transformer);
    }

    private static S3BorrowedBuffer directCrtBuffer(int size) {
        S3BorrowedBuffer buffer = mock(S3BorrowedBuffer.class);
        when(buffer.asByteBuffer()).thenReturn(ByteBuffer.allocateDirect(size));
        return buffer;
    }

    private static BlockingBufferHandler blockingBufferHandler() {
        S3CrtBorrowedBufferBlockingResponseTransformer handler = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        BlockingBufferHandler result = new BlockingBufferHandler(handler);
        doAnswer(invocation -> {
            result.bufferEntered.countDown();
            if (!result.continueBuffer.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting to continue buffer callback");
            }
            return true;
        }).when(handler).onBorrowedBuffer(any());
        return result;
    }

    private static S3CrtBorrowedBufferBlockingResponseTransformer capturingHandler(
        AtomicReference<S3BorrowedBuffer> delivered,
        boolean failOnNext) {
        S3CrtBorrowedBufferBlockingResponseTransformer handler = mock(S3CrtBorrowedBufferBlockingResponseTransformer.class);
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
        private final S3CrtBorrowedBufferBlockingResponseTransformer handler;
        private final CountDownLatch bufferEntered = new CountDownLatch(1);
        private final CountDownLatch continueBuffer = new CountDownLatch(1);

        private BlockingBufferHandler(S3CrtBorrowedBufferBlockingResponseTransformer handler) {
            this.handler = handler;
        }
    }
}
