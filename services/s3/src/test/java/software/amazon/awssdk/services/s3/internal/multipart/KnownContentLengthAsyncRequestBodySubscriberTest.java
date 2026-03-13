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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
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
        subscriber.onNext(createMockAsyncRequestBody(PART_SIZE + 1));
        verifyFailRequestsElegantly("Content length must not be greater than part size");
    }

    @Test
    void validateLastPartSize_withIncorrectSize_shouldFailRequest() {
        long expectedLastPartSize = MPU_CONTENT_SIZE % PART_SIZE;
        long incorrectLastPartSize = expectedLastPartSize + 1;

        KnownContentLengthAsyncRequestBodySubscriber lastPartSubscriber = createSubscriber(createDefaultMpuRequestContext());
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

        KnownContentLengthAsyncRequestBodySubscriber lastPartSubscriber = createSubscriber(createDefaultMpuRequestContext());
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
    void pause_withOngoingCompleteMpuFuture_shouldReturnTokenAndCancelFuture() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture = new CompletableFuture<>();
        int numExistingParts = 2;

        S3ResumeToken resumeToken = testPauseScenario(numExistingParts, completeMpuFuture);

        verifyResumeToken(resumeToken, numExistingParts);
        assertThat(completeMpuFuture).isCancelled();
    }

    @Test
    void pause_withCompletedCompleteMpuFuture_shouldReturnNullToken() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture =
            CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder().build());
        int numExistingParts = 2;

        S3ResumeToken resumeToken = testPauseScenario(numExistingParts, completeMpuFuture);

        assertThat(resumeToken).isNull();
    }

    @Test
    void pause_withUninitiatedCompleteMpuFuture_shouldReturnToken() {
        int numExistingParts = 2;

        S3ResumeToken resumeToken = testPauseScenario(numExistingParts, null);

        verifyResumeToken(resumeToken, numExistingParts);
    }

    private S3ResumeToken testPauseScenario(int numExistingParts,
                                           CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture) {
        KnownContentLengthAsyncRequestBodySubscriber subscriber =
            createSubscriber(createMpuRequestContextWithExistingParts(numExistingParts));

        when(multipartUploadHelper.completeMultipartUpload(any(CompletableFuture.class), any(String.class),
                                                         any(CompletedPart[].class), any(PutObjectRequest.class),
                                                         any(Long.class)))
            .thenReturn(completeMpuFuture);

        simulateOnNextForAllParts(subscriber);
        subscriber.onComplete();
        assertThat(returnFuture).isNotCompletedExceptionally();
        return subscriber.pause();
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

    private KnownContentLengthAsyncRequestBodySubscriber createSubscriber(MpuRequestContext mpuRequestContext) {
        return new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext, returnFuture, multipartUploadHelper);
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
        verify(multipartUploadHelper).failRequestsElegantly(any(), exceptionCaptor.capture(), eq(UPLOAD_ID), eq(returnFuture), eq(putObjectRequest));

        Throwable exception = exceptionCaptor.getValue();
        assertThat(exception).isInstanceOf(SdkClientException.class);
        assertThat(exception.getMessage()).contains(expectedErrorMessage);
        verify(subscription).cancel();
    }

    private Map<Integer, CompletedPart> createExistingParts(int numExistingParts) {
        Map<Integer, CompletedPart> existingParts =
            IntStream.range(0, numExistingParts)
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

    private void simulateOnNextForAllParts(KnownContentLengthAsyncRequestBodySubscriber subscriber) {
        subscriber.onSubscribe(subscription);

        for (int i = 0; i < TOTAL_NUM_PARTS; i++) {
            subscriber.onNext(createMockAsyncRequestBody(PART_SIZE));
        }
    }

}
