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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.utils.ControlledSubscription;
import software.amazon.awssdk.services.s3.internal.multipart.utils.ManagedUploadPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.Pair;

public class KnownContentLengthAsyncRequestBodySubscriberTest {

    // Should contain four parts: [8KB, 8KB, 8KB, 1KB]
    private static final long MPU_CONTENT_SIZE = 25 * 1024;
    private static final long PART_SIZE = 8 * 1024;
    private static final int TOTAL_NUM_PARTS = 4;
    private static final String UPLOAD_ID = "1234";
    private static RandomTempFile testFile;

    private AsyncRequestBody asyncRequestBody;
    private PutObjectRequest putObjectRequest;
    private S3AsyncClient s3AsyncClient;
    private MultipartUploadHelper multipartUploadHelper;
    private CompletableFuture<PutObjectResponse> returnFuture;
    private KnownContentLengthAsyncRequestBodySubscriber subscriber;
    private Collection<CompletableFuture<CompletedPart>> futures;
    private Subscription subscription;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testFile = new RandomTempFile("testfile.dat", MPU_CONTENT_SIZE);
    }

    @AfterAll
    public static void afterAll() {
        testFile.delete();
    }

    @BeforeEach
    public void beforeEach() {
        s3AsyncClient = mock(S3AsyncClient.class);
        multipartUploadHelper = mock(MultipartUploadHelper.class);
        asyncRequestBody = AsyncRequestBody.fromFile(testFile);
        putObjectRequest = PutObjectRequest.builder().bucket("bucket").key("key").build();

        returnFuture = new CompletableFuture<>();
        futures = new ConcurrentLinkedQueue<>();
        subscription = mock(Subscription.class);

        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(CompletedPart.builder().build()));

        subscriber = createSubscriber(createDefaultMpuRequestContext());
        subscriber.onSubscribe(subscription);
    }

    @Test
    void validatePart_withMissingContentLength_shouldFailRequest() {
        subscriber.onNext(createMockAsyncRequestBodyWithEmptyContentLength());
        verifyFailRequestsElegantly("Content length is missing on the AsyncRequestBody");
    }

    @Test
    void validatePart_withPartSizeExceedingLimit_shouldFailRequest() {
        CloseableAsyncRequestBody oversizedPart = createMockAsyncRequestBody(PART_SIZE + 1);
        subscriber.onNext(oversizedPart);
        verifyFailRequestsElegantly("Content length must not be greater than part size");
        verify(oversizedPart).subscribe(any(CancelledSubscriber.class));
        verify(oversizedPart).close();
    }

    @Test
    void validateLastPartSize_withIncorrectSize_shouldFailRequest() {
        long expectedLastPartSize = MPU_CONTENT_SIZE % PART_SIZE;
        long incorrectLastPartSize = expectedLastPartSize + 1;

        KnownContentLengthAsyncRequestBodySubscriber lastPartSubscriber = createSubscriber(
                createDefaultMpuRequestContext());
        lastPartSubscriber.onSubscribe(subscription);

        for (int i = 0; i < TOTAL_NUM_PARTS - 1; i++) {
            lastPartSubscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        }

        lastPartSubscriber.onNext(createMockAsyncRequestBody(incorrectLastPartSize));

        verifyFailRequestsElegantly("Content length of the last part must be equal to the expected last part size");
    }

    @Test
    void validateTotalPartNum_receivedMoreParts_shouldFail() {
        long expectedLastPartSize = MPU_CONTENT_SIZE % PART_SIZE;

        KnownContentLengthAsyncRequestBodySubscriber lastPartSubscriber = createSubscriber(
                createDefaultMpuRequestContext());
        lastPartSubscriber.onSubscribe(subscription);

        for (int i = 0; i < TOTAL_NUM_PARTS - 1; i++) {
            CloseableAsyncRequestBody regularPart = createMockAsyncRequestBody(PART_SIZE);
            when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));
            lastPartSubscriber.onNext(regularPart);
        }

        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        lastPartSubscriber.onNext(createMockAsyncRequestBody(expectedLastPartSize));
        lastPartSubscriber.onNext(createMockAsyncRequestBody(expectedLastPartSize));

        verifyFailRequestsElegantly("The number of parts divided is not equal to the expected number of parts");
    }

    @Test
    void validateLastPartSize_withCorrectSize_shouldNotFail() {
        long expectedLastPartSize = MPU_CONTENT_SIZE % PART_SIZE;

        KnownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(createDefaultMpuRequestContext());
        subscriber.onSubscribe(subscription);

        for (int i = 0; i < TOTAL_NUM_PARTS - 1; i++) {
            CloseableAsyncRequestBody regularPart = createMockAsyncRequestBody(PART_SIZE);
            when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));
            subscriber.onNext(regularPart);
        }

        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        subscriber.onNext(createMockAsyncRequestBody(expectedLastPartSize));
        subscriber.onComplete();

        assertThat(returnFuture).isNotCompletedExceptionally();
    }

    @Test
    void pause_withOngoingCompleteMpuFuture_shouldReturnNullTokenAndNotCancelFuture() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture = new CompletableFuture<>();
        int numExistingParts = 2;

        S3ResumeToken resumeToken = testPauseScenario(numExistingParts, completeMpuFuture);

        assertThat(resumeToken).isNull();
        assertThat(completeMpuFuture).isNotDone();
    }

    @Test
    void pause_withCompletedCompleteMpuFuture_shouldReturnNullToken() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture = CompletableFuture
                .completedFuture(CompleteMultipartUploadResponse.builder().build());
        int numExistingParts = 2;

        S3ResumeToken resumeToken = testPauseScenario(numExistingParts, completeMpuFuture);

        assertThat(resumeToken).isNull();
    }

    @Test
    void pause_afterCompletionClaimedButBeforeCompleteMpuFutureReturned_shouldReturnNullToken() {
        AtomicReference<KnownContentLengthAsyncRequestBodySubscriber> subscriberRef = new AtomicReference<>();
        AtomicReference<Optional<S3ResumeToken>> tokenFromPauseDuringCompletion = new AtomicReference<>();
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture = new CompletableFuture<>();
        when(multipartUploadHelper.completeMultipartUpload(any(), any(), any(), any(), anyLong()))
            .thenAnswer(invocation -> {
                tokenFromPauseDuringCompletion.set(Optional.ofNullable(subscriberRef.get().pause()));
                return completeMpuFuture;
            });

        KnownContentLengthAsyncRequestBodySubscriber sub = createSubscriber(createDefaultMpuRequestContext());
        subscriberRef.set(sub);
        sub.onSubscribe(subscription);
        deliverAllPartsWithCorrectSizes(sub);
        sub.onComplete();

        assertThat(tokenFromPauseDuringCompletion.get()).isEmpty();
        assertThat(completeMpuFuture).isNotDone();
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    @Test
    void pause_whileLastPartInFlight_shouldCancelItAndNeverCompleteUpload() {
        ManagedUploadPart parts = new ManagedUploadPart();
        List<CompletableFuture<CompletedPart>> sentParts = new ArrayList<>();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenAnswer(registeringPartFutures(parts, sentParts));
        KnownContentLengthAsyncRequestBodySubscriber sub = createSubscriberWithOwnReturnFuture(createDefaultMpuRequestContext());
        sub.onSubscribe(subscription);
        deliverAllPartsWithCorrectSizes(sub);
        parts.completePart(1);
        parts.completePart(2);
        parts.completePart(3);
        sub.onComplete();

        S3ResumeToken resumeToken = sub.pause();

        verifyResumeToken(resumeToken, 3);
        assertThat(sentParts.get(3)).isCancelled();
        assertThat(returnFuture).isCancelled();
        verify(multipartUploadHelper, never()).completeMultipartUpload(any(), any(), any(), any(), anyLong());
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    @Test
    void pause_afterPartPassesAdmissionCheckButBeforeItIsSent_shouldNeverCompleteUpload() {
        ManagedUploadPart parts = new ManagedUploadPart();
        AtomicReference<KnownContentLengthAsyncRequestBodySubscriber> subscriberRef = new AtomicReference<>();
        AtomicReference<S3ResumeToken> tokenRef = new AtomicReference<>();
        List<CompletableFuture<CompletedPart>> sentParts = new ArrayList<>();
        Answer<CompletableFuture<CompletedPart>> sendPart = registeringPartFutures(parts, sentParts);
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                Pair<UploadPartRequest, AsyncRequestBody> request = invocation.getArgument(3);
                if (request.left().partNumber() == TOTAL_NUM_PARTS) {
                    tokenRef.set(subscriberRef.get().pause());
                }
                return sendPart.answer(invocation);
            });
        KnownContentLengthAsyncRequestBodySubscriber sub = createSubscriberWithOwnReturnFuture(createDefaultMpuRequestContext());
        subscriberRef.set(sub);
        sub.onSubscribe(subscription);

        for (int i = 0; i < TOTAL_NUM_PARTS - 1; i++) {
            sub.onNext(createMockAsyncRequestBody(PART_SIZE));
        }
        parts.completePart(1);
        parts.completePart(2);
        parts.completePart(3);
        CloseableAsyncRequestBody lastPart = createMockAsyncRequestBody(MPU_CONTENT_SIZE % PART_SIZE);
        sub.onNext(lastPart);
        sub.onComplete();

        // Part 4 is sent after the pause counted the parts, so the token does not include it.
        verifyResumeToken(tokenRef.get(), 3);
        assertThat(sentParts.get(3)).isCancelled();
        verify(lastPart).close();
        verify(multipartUploadHelper, never()).completeMultipartUpload(any(), any(), any(), any(), anyLong());
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    @Test
    void pause_withExistingPartsFromResumedUpload_shouldCountThemInToken() {
        int numExistingParts = 2;
        KnownContentLengthAsyncRequestBodySubscriber sub =
            createSubscriber(createMpuRequestContextWithExistingParts(numExistingParts));
        sub.onSubscribe(subscription);

        verifyResumeToken(sub.pause(), numExistingParts);
    }

    @Test
    void pause_afterPartFailure_shouldReturnTokenWithoutCancellingReturnFuture() {
        CompletableFuture<CompletedPart> partFuture = new CompletableFuture<>();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenReturn(partFuture);
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        partFuture.completeExceptionally(new RuntimeException("part failed"));

        verifyResumeToken(subscriber.pause(), 0);
        assertThat(returnFuture).isNotCancelled();
        verify(multipartUploadHelper, times(1)).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    @Test
    void pause_afterPartFailure_shouldCountOnlySuccessfulParts() {
        ManagedUploadPart parts = new ManagedUploadPart();
        List<CompletableFuture<CompletedPart>> sentParts = new ArrayList<>();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenAnswer(registeringPartFutures(parts, sentParts));
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        parts.completePart(1);
        sentParts.get(1).completeExceptionally(new RuntimeException("part 2 failed"));

        verifyResumeToken(subscriber.pause(), 1);
    }



    @Test
    void validationFailure_afterPauseWinsDuringOnNext_shouldNotFailOrAbortUpload() {
        AtomicReference<S3ResumeToken> tokenRef = new AtomicReference<>();
        CloseableAsyncRequestBody oversizedPart = mock(CloseableAsyncRequestBody.class);
        when(oversizedPart.contentLength()).thenAnswer(invocation -> {
            if (tokenRef.get() == null) {
                tokenRef.set(subscriber.pause());
            }
            return Optional.of(PART_SIZE + 1);
        });

        subscriber.onNext(oversizedPart);

        verifyResumeToken(tokenRef.get(), 0);
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
        verify(oversizedPart).subscribe(any(CancelledSubscriber.class));
        verify(oversizedPart).close();
    }

    @Test
    void partCountMismatchAtCompletion_shouldFailAndAbortOnlyOnce() {
        doAnswer(invocation -> {
            CompletableFuture<PutObjectResponse> future = invocation.getArgument(3);
            future.completeExceptionally(invocation.getArgument(1));
            return null;
        }).when(multipartUploadHelper).failRequestsElegantly(any(), any(), any(), any(), any());
        for (int i = 0; i < TOTAL_NUM_PARTS - 1; i++) {
            subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        }

        subscriber.onComplete();

        verify(multipartUploadHelper, times(1)).failRequestsElegantly(any(), any(), any(), any(), any());
        verify(multipartUploadHelper, never()).completeMultipartUpload(any(), any(), any(), any(), anyLong());
    }

    @Test
    void pause_afterOnError_shouldReturnTokenWithoutCancellingReturnFuture() {
        subscriber.onError(new RuntimeException("body publisher failed"));

        verifyResumeToken(subscriber.pause(), 0);
        assertThat(returnFuture).isNotCancelled();
        verify(multipartUploadHelper, times(1)).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    @Test
    void onNext_afterPause_shouldDiscardBodyWithoutSendingIt() {
        assertThat(subscriber.pause()).isNotNull();
        CloseableAsyncRequestBody body = createMockAsyncRequestBody(PART_SIZE);

        subscriber.onNext(body);

        verify(body).subscribe(any(CancelledSubscriber.class));
        verify(body).close();
        verify(multipartUploadHelper, never()).sendIndividualUploadPartRequest(any(), any(), any(), any(), any());
    }

    @Test
    void onNext_afterPartFailure_shouldDiscardBodyWithoutSendingIt() {
        CompletableFuture<CompletedPart> partFuture = new CompletableFuture<>();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenReturn(partFuture);
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        partFuture.completeExceptionally(new RuntimeException("part failed"));
        CloseableAsyncRequestBody lateBody = createMockAsyncRequestBody(PART_SIZE);

        subscriber.onNext(lateBody);

        verify(lateBody).subscribe(any(CancelledSubscriber.class));
        verify(lateBody).close();
        verify(multipartUploadHelper, times(1)).sendIndividualUploadPartRequest(any(), any(), any(), any(), any());
    }

    @Test
    void onSubscribe_whenAlreadySubscribed_shouldCancelIncomingSubscription() {
        Subscription secondSubscription = mock(Subscription.class);

        subscriber.onSubscribe(secondSubscription);

        verify(secondSubscription).cancel();
        verify(subscription, never()).cancel();
    }

    @Test
    void onError_afterPause_shouldNotFailOrAbortUpload() {
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenReturn(new CompletableFuture<>());
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));

        S3ResumeToken resumeToken = subscriber.pause();
        subscriber.onError(new RuntimeException("body publisher failed after pause"));

        verifyResumeToken(resumeToken, 0);
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
    }


    @Test
    void returnFutureCancelled_whileCompleteMpuInFlight_shouldAbortUploadAndPauseShouldReturnNullToken() {
        when(multipartUploadHelper.completeMultipartUpload(any(), any(), any(), any(), anyLong()))
            .thenReturn(new CompletableFuture<>());
        CompletableFuture<PutObjectResponse> ownReturnFuture = new CompletableFuture<>();
        KnownContentLengthAsyncRequestBodySubscriber sub =
            new KnownContentLengthAsyncRequestBodySubscriber(createDefaultMpuRequestContext(), ownReturnFuture,
                                                             multipartUploadHelper, 50);
        sub.onSubscribe(subscription);
        deliverAllPartsWithCorrectSizes(sub);
        sub.onComplete();

        ownReturnFuture.cancel(true);

        verify(multipartUploadHelper).failRequestsElegantly(any(), any(), eq(UPLOAD_ID), eq(ownReturnFuture),
                                                            eq(putObjectRequest));
        assertThat(sub.pause()).isNull();
    }

    private S3ResumeToken testPauseScenario(int numExistingParts,
            CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture) {
        ManagedUploadPart parts = new ManagedUploadPart();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
            .thenAnswer(registeringPartFutures(parts, new ArrayList<>()));
        when(multipartUploadHelper.completeMultipartUpload(any(), any(), any(), any(), anyLong()))
            .thenReturn(completeMpuFuture);
        KnownContentLengthAsyncRequestBodySubscriber subscriber = createSubscriber(
                createMpuRequestContextWithExistingParts(numExistingParts));
        subscriber.onSubscribe(subscription);

        deliverAllPartsWithCorrectSizes(subscriber);
        for (int partNum = numExistingParts + 1; partNum <= TOTAL_NUM_PARTS; partNum++) {
            parts.completePart(partNum);
        }
        subscriber.onComplete();

        ArgumentCaptor<CompletedPart[]> partsCaptor = ArgumentCaptor.forClass(CompletedPart[].class);
        verify(multipartUploadHelper).completeMultipartUpload(eq(returnFuture), eq(UPLOAD_ID), partsCaptor.capture(),
                                                              eq(putObjectRequest), eq(MPU_CONTENT_SIZE));
        assertThat(partsCaptor.getValue()).hasSize(TOTAL_NUM_PARTS).doesNotContainNull();
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
        return subscriber.pause();
    }

    @Test
    void maxInFlightPutObjectParts_shouldLimitConcurrentUploads() {
        int maxInFlight = 2;
        long contentSize = 5 * PART_SIZE;
        int totalParts = 5;

        MpuRequestContext context = MpuRequestContext.builder()
                .request(Pair.of(putObjectRequest, asyncRequestBody))
                .contentLength(contentSize)
                .partSize(PART_SIZE)
                .uploadId(UPLOAD_ID)
                .numPartsCompleted(0L)
                .expectedNumParts(totalParts)
                .build();

        // Use non-completing futures to simulate slow uploads so parts stay in-flight
        CompletableFuture<CompletedPart> pendingFuture1 = new CompletableFuture<>();
        CompletableFuture<CompletedPart> pendingFuture2 = new CompletableFuture<>();
        CompletableFuture<CompletedPart> pendingFuture3 = new CompletableFuture<>();

        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                .thenReturn(pendingFuture1)
                .thenReturn(pendingFuture2)
                .thenReturn(pendingFuture3);

        KnownContentLengthAsyncRequestBodySubscriber sub = createSubscriber(context, maxInFlight);
        Subscription mockSubscription = mock(Subscription.class);
        sub.onSubscribe(mockSubscription);

        // onSubscribe requests maxInFlightParts(2) upfront
        verify(mockSubscription, times(1)).request(maxInFlight);

        // First onNext: in-flight goes to 1. Demand driven by completion callbacks.
        sub.onNext(createMockAsyncRequestBody(PART_SIZE));

        // Second onNext: in-flight goes to 2
        sub.onNext(createMockAsyncRequestBody(PART_SIZE));

        // Complete the first part — callback decrements to 1, sees 1 < 2, calls request(1)
        pendingFuture1.complete(CompletedPart.builder().partNumber(1).build());
        verify(mockSubscription, times(1)).request(1);
    }

    /**
     * Regression test for early CompleteMultipartUpload with a missing part under low maxInFlightParts.
     */
    @Test
    void lastBodyAndOnCompleteDeliveredInsideCompletionCallback_shouldCompleteWithAllParts() {
        int maxInFlight = 2;
        int totalParts = 5;
        long contentSize = totalParts * PART_SIZE;

        MpuRequestContext context = MpuRequestContext.builder()
                .request(Pair.of(putObjectRequest, asyncRequestBody))
                .contentLength(contentSize)
                .partSize(PART_SIZE)
                .uploadId(UPLOAD_ID)
                .numPartsCompleted(0L)
                .expectedNumParts(totalParts)
                .build();

        ManagedUploadPart recorder = new ManagedUploadPart();
        when(multipartUploadHelper.sendIndividualUploadPartRequest(eq(UPLOAD_ID), any(), any(), any(), any()))
                .thenAnswer(recorder);

        KnownContentLengthAsyncRequestBodySubscriber sub = createSubscriber(context, maxInFlight);
        ControlledSubscription controlledSubscription = new ControlledSubscription(sub);
        sub.onSubscribe(controlledSubscription);          // requests maxInFlight upfront

        controlledSubscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // part 1 starts
        controlledSubscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // part 2 starts
        recorder.completePart(1);                                                          // frees a slot
        controlledSubscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // part 3 starts
        recorder.completePart(2);
        controlledSubscription.enqueueBodyAndDeliver(createMockAsyncRequestBody(PART_SIZE)); // part 4 starts

        // Part 3 completes while the final chunk is not buffered yet: its request(1) delivers nothing.
        recorder.completePart(3);

        // The producer now queues the final body and the stream-complete signal. They will be delivered
        // synchronously inside the next request(1), which happens in part 4's completion callback,
        // between its decrementAndGet() (returning the stale 0) and completeMultipartUploadIfFinished.
        controlledSubscription.enqueueBodyQuietly(createMockAsyncRequestBody(PART_SIZE));
        controlledSubscription.enqueueStreamCompleteQuietly();
        recorder.completePart(4);

        // verify we haven't called CompleteMultipartUpload yet.
        verify(multipartUploadHelper, never()).completeMultipartUpload(any(), any(), any(), any(), anyLong());
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());

        // Part 5's upload finishes; only now should CompleteMultipartUpload be sent, with all 5 parts.
        recorder.completePart(5);

        ArgumentCaptor<CompletedPart[]> partsCaptor = ArgumentCaptor.forClass(CompletedPart[].class);
        verify(multipartUploadHelper).completeMultipartUpload(eq(returnFuture), eq(UPLOAD_ID), partsCaptor.capture(),
                                                              eq(putObjectRequest), eq(contentSize));
        assertThat(partsCaptor.getValue()).hasSize(totalParts).doesNotContainNull();
        verify(multipartUploadHelper, never()).failRequestsElegantly(any(), any(), any(), any(), any());
    }

    private MpuRequestContext createDefaultMpuRequestContext() {
        return MpuRequestContext.builder()
                .request(Pair.of(putObjectRequest, AsyncRequestBody.fromFile(testFile)))
                .contentLength(MPU_CONTENT_SIZE)
                .partSize(PART_SIZE)
                .uploadId(UPLOAD_ID)
                .numPartsCompleted(0L)
                .expectedNumParts(TOTAL_NUM_PARTS)
                .build();
    }

    private MpuRequestContext createMpuRequestContextWithExistingParts(int numExistingParts) {
        Map<Integer, CompletedPart> existingParts = createExistingParts(numExistingParts);
        return MpuRequestContext.builder()
                .request(Pair.of(putObjectRequest, asyncRequestBody))
                .contentLength(MPU_CONTENT_SIZE)
                .partSize(PART_SIZE)
                .uploadId(UPLOAD_ID)
                .existingParts(existingParts)
                .expectedNumParts(TOTAL_NUM_PARTS)
                .numPartsCompleted((long) existingParts.size())
                .build();
    }

    private KnownContentLengthAsyncRequestBodySubscriber createSubscriberWithOwnReturnFuture(MpuRequestContext mpuRequestContext) {
        // A successful pause cancels the return future, which would also trigger the subscriber created in beforeEach.
        returnFuture = new CompletableFuture<>();
        return createSubscriber(mpuRequestContext);
    }

    private KnownContentLengthAsyncRequestBodySubscriber createSubscriber(MpuRequestContext mpuRequestContext) {
        return new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext, returnFuture, multipartUploadHelper,
                50);
    }

    private KnownContentLengthAsyncRequestBodySubscriber createSubscriber(MpuRequestContext mpuRequestContext,
            int maxInFlightPutObjectParts) {
        return new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext, returnFuture, multipartUploadHelper,
                maxInFlightPutObjectParts);
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
        verify(multipartUploadHelper).failRequestsElegantly(any(), exceptionCaptor.capture(), eq(UPLOAD_ID),
                eq(returnFuture), eq(putObjectRequest));

        Throwable exception = exceptionCaptor.getValue();
        assertThat(exception).isInstanceOf(SdkClientException.class);
        assertThat(exception.getMessage()).contains(expectedErrorMessage);
        verify(subscription).cancel();
    }

    private Map<Integer, CompletedPart> createExistingParts(int numExistingParts) {
        Map<Integer, CompletedPart> existingParts = IntStream.rangeClosed(1, numExistingParts)
                .boxed().collect(Collectors.toMap(Function.identity(),
                        i -> CompletedPart.builder().partNumber(i).build(), (a, b) -> b));
        return existingParts;
    }

    private void verifyResumeToken(S3ResumeToken s3ResumeToken, int numExistingParts) {
        assertThat(s3ResumeToken).isNotNull();
        assertThat(s3ResumeToken.uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(s3ResumeToken.partSize()).isEqualTo(PART_SIZE);
        assertThat(s3ResumeToken.totalNumParts()).isEqualTo(TOTAL_NUM_PARTS);
        assertThat(s3ResumeToken.numPartsCompleted()).isEqualTo(numExistingParts);
    }

    /**
     * Registers each part's future in the subscriber's tracked futures, so that a pause can see and cancel it.
     */
    private static Answer<CompletableFuture<CompletedPart>> registeringPartFutures(
            ManagedUploadPart parts, List<CompletableFuture<CompletedPart>> sentParts) {
        return invocation -> {
            CompletableFuture<CompletedPart> future = parts.answer(invocation);
            Collection<CompletableFuture<CompletedPart>> trackedFutures = invocation.getArgument(2);
            trackedFutures.add(future);
            sentParts.add(future);
            return future;
        };
    }

    private void deliverAllPartsWithCorrectSizes(KnownContentLengthAsyncRequestBodySubscriber subscriber) {
        for (int i = 0; i < TOTAL_NUM_PARTS - 1; i++) {
            subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        }
        subscriber.onNext(createMockAsyncRequestBody(MPU_CONTENT_SIZE % PART_SIZE));
    }
}
