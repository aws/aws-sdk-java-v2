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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Pair;

public class UnknownContentLengthAsyncRequestBodySubscriberTest {

    private static final long PART_SIZE = 8 * 1024;
    private static final String UPLOAD_ID = "1234";

    private MultipartUploadHelper multipartUploadHelper;
    private GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;
    private PutObjectRequest putObjectRequest;
    private CompletableFuture<PutObjectResponse> returnFuture;
    private Subscription subscription;

    @BeforeEach
    public void beforeEach() {
        multipartUploadHelper = mock(MultipartUploadHelper.class);
        genericMultipartHelper = mock(GenericMultipartHelper.class);
        putObjectRequest = PutObjectRequest.builder()
                .bucket("bucket")
                .key("key")
                .build();
        returnFuture = new CompletableFuture<>();
        subscription = mock(Subscription.class);
    }

    @Test
    void validatePart_withMissingContentLength_shouldFailRequest() {
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(50);
        subscriber.onSubscribe(subscription);

        // First onNext with valid body (held as firstRequestBody)
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));

        // Second onNext triggers CreateMultipartUpload path
        stubSuccessfulCreateMultipartCall();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(CompletedPart.builder().build()));

        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));

        // Third onNext with missing content length
        subscriber.onNext(createMockAsyncRequestBodyWithEmptyContentLength());

        verifyFailRequestsElegantly("Content length is missing on the AsyncRequestBody");
    }

    @Test
    void validatePart_withPartSizeExceedingLimit_shouldFailRequest() {
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(50);
        subscriber.onSubscribe(subscription);

        // First onNext with valid body
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));

        // Second onNext with oversized body triggers failure
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE + 1));

        verifyFailRequestsElegantly("Content length must not be greater than part size");
    }

    @Test
    void onNext_withNullBody_shouldThrowNullPointerException() {
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(50);
        subscriber.onSubscribe(subscription);

        assertThatThrownBy(() -> subscriber.onNext(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("MUST NOT be null");

        verify(multipartUploadHelper).failRequestsElegantly(
                any(), any(NullPointerException.class), any(), eq(returnFuture), eq(putObjectRequest));
    }

    @Test
    void maxInFlightParts_shouldLimitConcurrentUploads() {
        int maxInFlight = 4;
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(maxInFlight);
        Subscription mockSubscription = mock(Subscription.class);
        subscriber.onSubscribe(mockSubscription);

        // onSubscribe requests 1
        verify(mockSubscription, times(1)).request(1);

        // First onNext: holds the first body, requests 1 more to decide single vs multipart
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        verify(mockSubscription, times(2)).request(1);

        // Second onNext: triggers CreateMultipartUpload, sends parts 1 and 2,
        // then bootstraps pipeline with request(maxInFlight - 2) = request(2)
        stubSuccessfulCreateMultipartCall();

        CompletableFuture<CompletedPart> pendingFuture1 = new CompletableFuture<>();
        CompletableFuture<CompletedPart> pendingFuture2 = new CompletableFuture<>();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(any(), any(), any(), any(), any()))
                .thenReturn(pendingFuture1)
                .thenReturn(pendingFuture2);

        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));

        // After sending 2 parts, bootstraps with request(maxInFlight - 2) = request(2)
        verify(mockSubscription, times(1)).request(2);

        // Complete part 1 — inFlight drops to 1, which is < 4, so request(1) is called
        pendingFuture1.complete(CompletedPart.builder().partNumber(1).build());
        verify(mockSubscription, times(3)).request(1);
    }

    @Test
    void onComplete_withSinglePart_shouldUploadInOneChunk() {
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(50);
        subscriber.onSubscribe(subscription);

        // Only one onNext — single part, no multipart needed
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        subscriber.onComplete();

        verify(multipartUploadHelper).uploadInOneChunk(eq(putObjectRequest), any(), eq(returnFuture));
    }

    @Test
    void onComplete_withNoParts_shouldUploadEmptyBody() {
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(50);
        subscriber.onSubscribe(subscription);

        // No onNext at all — empty stream
        subscriber.onComplete();

        verify(multipartUploadHelper).uploadInOneChunk(eq(putObjectRequest), any(), eq(returnFuture));
    }

    /**
     * Regression test for the "Expected: N, Actual: N-1" part-count failure seen with low maxInFlightParts.
     *
     * <p>In the upload completion callback, {@code subscription.request(1)} is invoked between
     * {@code asyncRequestBodyInFlight.decrementAndGet()} and {@code completeMultipartUploadIfFinish}.
     * The upstream SimplePublisher delivers queued signals synchronously on the requesting thread, so that
     * request can deliver the final AsyncRequestBody (starting a new upload) followed by onComplete()
     * (setting isDone) before the callback finishes. If completion is then decided using the stale
     * decrement snapshot of 0, CompleteMultipartUpload is initiated while the final part is still in
     * flight, and the upload fails with "The number of UploadParts requests is not equal to the expected
     * number of parts" (or, without the part-count validation, would complete with a missing part).
     *
     * <p>This test scripts that exact delivery order with a Subscription that mimics SimplePublisher's
     * synchronous, demand-gated delivery.
     */
    @Test
    void lastBodyAndOnCompleteDeliveredInsideCompletionCallback_shouldCompleteWithAllParts() {
        int maxInFlight = 2;
        int numParts = 5;
        UnknownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(maxInFlight);

        stubSuccessfulCreateMultipartCall();
        when(genericMultipartHelper.determinePartCount(anyLong(), anyLong()))
            .thenAnswer(invocation -> (int) Math.ceil(invocation.getArgument(0, Long.class)
                                                      / (double) invocation.getArgument(1, Long.class)));
        UploadPartRecorder recorder = new UploadPartRecorder();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(any(), any(), any(), any(), any()))
            .thenAnswer(recorder);

        ScriptedSubscription subscription = new ScriptedSubscription(subscriber);
        subscriber.onSubscribe(subscription);

        subscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // held as firstRequestBody
        subscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // triggers MPU; parts 1, 2 start
        recorder.completePart(1);                                                  // frees a slot
        subscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // part 3 starts
        recorder.completePart(2);
        subscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // part 4 starts

        // Part 3 completes while the final chunk is not buffered yet: its request(1) delivers nothing.
        recorder.completePart(3);

        // The producer now queues the final body and the stream-complete signal. They will be delivered
        // synchronously inside the next request(1), which happens in part 4's completion callback,
        // between its decrementAndGet() (returning the stale 0) and completeMultipartUploadIfFinish.
        subscription.enqueueBodyQuietly(createMockAsyncRequestBody(PART_SIZE));
        subscription.enqueueStreamCompleteQuietly();
        recorder.completePart(4);

        // With the stale-snapshot bug, completion was initiated here with only 4 parts:
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
        verify(multipartUploadHelper, never()).completeMultipartUpload(any(), any(), any(), any(), anyLong());

        // Part 5's upload finishes; only now should CompleteMultipartUpload be sent, with all 5 parts.
        recorder.completePart(5);

        ArgumentCaptor<CompletedPart[]> partsCaptor = ArgumentCaptor.forClass(CompletedPart[].class);
        verify(multipartUploadHelper).completeMultipartUpload(eq(returnFuture), eq(UPLOAD_ID), partsCaptor.capture(),
                                                              eq(putObjectRequest), eq(numParts * PART_SIZE));
        assertThat(partsCaptor.getValue()).hasSize(numParts).doesNotContainNull();
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    /**
     * Records the consumer and pending future of each UploadPart request so the test can complete parts
     * the same way MultipartUploadHelper does: consumer.accept(completedPart) followed by future completion.
     */
    private static final class UploadPartRecorder implements org.mockito.stubbing.Answer<CompletableFuture<CompletedPart>> {
        private final Map<Integer, Consumer<CompletedPart>> consumers = new HashMap<>();
        private final Map<Integer, CompletableFuture<CompletedPart>> futures = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public CompletableFuture<CompletedPart> answer(org.mockito.invocation.InvocationOnMock invocation) {
            Consumer<CompletedPart> consumer = invocation.getArgument(1, Consumer.class);
            Pair<UploadPartRequest, AsyncRequestBody> pair = invocation.getArgument(3, Pair.class);
            int partNumber = pair.left().partNumber();
            CompletableFuture<CompletedPart> future = new CompletableFuture<>();
            consumers.put(partNumber, consumer);
            futures.put(partNumber, future);
            return future;
        }

        void completePart(int partNumber) {
            CompletableFuture<CompletedPart> future = futures.get(partNumber);
            assertThat(future).withFailMessage("UploadPart request for part %d was never sent", partNumber).isNotNull();
            CompletedPart part = CompletedPart.builder().partNumber(partNumber).eTag("etag-" + partNumber).build();
            consumers.get(partNumber).accept(part);
            future.complete(part);
        }
    }

    /**
     * A Subscription that mimics {@link software.amazon.awssdk.utils.async.SimplePublisher}: queued signals
     * are delivered synchronously on the thread that calls request(), onNext delivery is gated on demand,
     * and onComplete is delivered once the queue drains, without needing demand.
     */
    private static final class ScriptedSubscription implements Subscription {
        private final UnknownContentLengthAsyncRequestBodySubscriber subscriber;
        private final Deque<CloseableAsyncRequestBody> queuedBodies = new ArrayDeque<>();
        private long demand;
        private boolean streamComplete;
        private boolean onCompleteDelivered;
        private boolean draining;

        ScriptedSubscription(UnknownContentLengthAsyncRequestBodySubscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            demand += n;
            drain();
        }

        @Override
        public void cancel() {
        }

        void enqueueBodyAndDeliver(CloseableAsyncRequestBody body) {
            queuedBodies.add(body);
            drain();
        }

        void enqueueBodyQuietly(CloseableAsyncRequestBody body) {
            queuedBodies.add(body);
        }

        void enqueueStreamCompleteQuietly() {
            streamComplete = true;
        }

        private void drain() {
            if (draining) {
                return; // mirrors SimplePublisher's processingQueue flag: no re-entrant delivery
            }
            draining = true;
            try {
                while (demand > 0 && !queuedBodies.isEmpty()) {
                    demand--;
                    subscriber.onNext(queuedBodies.poll());
                }
                if (streamComplete && queuedBodies.isEmpty() && !onCompleteDelivered) {
                    onCompleteDelivered = true;
                    subscriber.onComplete();
                }
            } finally {
                draining = false;
            }
        }
    }

    private UnknownContentLengthAsyncRequestBodySubscriber createSubscriber(int maxInFlightParts) {
        return new UnknownContentLengthAsyncRequestBodySubscriber(
                PART_SIZE, putObjectRequest, returnFuture,
                multipartUploadHelper, genericMultipartHelper, maxInFlightParts);
    }

    private void stubSuccessfulCreateMultipartCall() {
        when(multipartUploadHelper.createMultipartUpload(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse.builder()
                                .uploadId(UPLOAD_ID)
                                .build()));
    }

    private CloseableAsyncRequestBody createMockAsyncRequestBody(long contentLength) {
        CloseableAsyncRequestBody mockBody = mock(CloseableAsyncRequestBody.class);
        when(mockBody.contentLength()).thenReturn(Optional.of(contentLength));
        return mockBody;
    }

    private CloseableAsyncRequestBody createMockAsyncRequestBodyWithEmptyContentLength() {
        CloseableAsyncRequestBody mockBody = mock(CloseableAsyncRequestBody.class);
        when(mockBody.contentLength()).thenReturn(Optional.empty());
        return mockBody;
    }

    private void verifyFailRequestsElegantly(String expectedErrorMessage) {
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(multipartUploadHelper).failRequestsElegantly(
                any(), exceptionCaptor.capture(), any(), eq(returnFuture), eq(putObjectRequest));

        Throwable exception = exceptionCaptor.getValue();
        assertThat(exception).isInstanceOf(SdkClientException.class);
        assertThat(exception.getMessage()).contains(expectedErrorMessage);
    }
}
