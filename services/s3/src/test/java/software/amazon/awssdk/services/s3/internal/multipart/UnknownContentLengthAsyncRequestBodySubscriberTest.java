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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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
