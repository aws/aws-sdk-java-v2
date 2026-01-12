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
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.stubSuccessfulCompleteMultipartCall;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.stubSuccessfulCreateMultipartCall;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.stubSuccessfulPutObjectCall;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.stubSuccessfulUploadPartCalls;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.StringInputStream;

public class UploadWithUnknownContentLengthHelperTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String UPLOAD_ID = "1234";
    private static final long MPU_CONTENT_SIZE = 1005 * 1024;
    private static final long PART_SIZE = 8 * 1024;
    private static final int NUM_TOTAL_PARTS = 126;

    private UploadWithUnknownContentLengthHelper helper;
    private S3AsyncClient s3AsyncClient;
    private static RandomTempFile testFile;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testFile = new RandomTempFile("testfile.dat", MPU_CONTENT_SIZE);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        testFile.delete();
    }

    @BeforeEach
    public void beforeEach() {
        s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        helper = new UploadWithUnknownContentLengthHelper(s3AsyncClient, PART_SIZE, PART_SIZE, PART_SIZE * 4);
    }

    @Test
    void upload_blockingInputStream_shouldInOrder() throws FileNotFoundException {
        stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        stubSuccessfulUploadPartCalls(s3AsyncClient);
        stubSuccessfulCompleteMultipartCall(BUCKET, KEY, s3AsyncClient);

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(null);
        CompletableFuture<PutObjectResponse> future = helper.uploadObject(createPutObjectRequest(), body);
        body.writeInputStream(new FileInputStream(testFile));
        future.join();

        ArgumentCaptor<UploadPartRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(s3AsyncClient, times(NUM_TOTAL_PARTS)).uploadPart(requestArgumentCaptor.capture(),
                requestBodyArgumentCaptor.capture());

        List<UploadPartRequest> actualRequests = requestArgumentCaptor.getAllValues();
        List<AsyncRequestBody> actualRequestBodies = requestBodyArgumentCaptor.getAllValues();
        assertThat(actualRequestBodies).hasSize(NUM_TOTAL_PARTS);
        assertThat(actualRequests).hasSize(NUM_TOTAL_PARTS);

        verifyUploadPartRequests(actualRequests, actualRequestBodies);
        verifyCompleteMultipartUploadRequest();
    }

    @Test
    void uploadObject_withMissingContentLength_shouldFailRequest() {
        CloseableAsyncRequestBody asyncRequestBody = createMockAsyncRequestBodyWithEmptyContentLength();
        CompletableFuture<PutObjectResponse> future = setupAndTriggerUploadFailure(asyncRequestBody);
        verifyFailureWithMessage(future, "Content length is missing on the AsyncRequestBody for part number");
    }

    @Test
    void uploadObject_emptyBody_shouldSucceed() {
        stubSuccessfulPutObjectCall(s3AsyncClient);

        BlockingInputStreamAsyncRequestBody body = AsyncRequestBody.forBlockingInputStream(null);
        CompletableFuture<PutObjectResponse> future = helper.uploadObject(createPutObjectRequest(), body);
        body.writeInputStream(new StringInputStream(""));
        future.join();

        ArgumentCaptor<PutObjectRequest> requestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(s3AsyncClient, times(1)).putObject(requestArgumentCaptor.capture(),
                                                                 requestBodyArgumentCaptor.capture());

        List<PutObjectRequest> actualRequests = requestArgumentCaptor.getAllValues();
        List<AsyncRequestBody> actualRequestBodies = requestBodyArgumentCaptor.getAllValues();
        assertThat(actualRequestBodies).hasSize(1);
        assertThat(actualRequests).hasSize(1);

        PutObjectRequest putObjectRequest = actualRequests.get(0);
        assertThat(putObjectRequest.bucket()).isEqualTo(BUCKET);
        assertThat(putObjectRequest.key()).isEqualTo(KEY);
        assertThat(actualRequestBodies.get(0).contentLength()).hasValue(0L);
    }

    @Test
    void uploadObject_withPartSizeExceedingLimit_shouldFailRequest() {
        CloseableAsyncRequestBody asyncRequestBody = createMockAsyncRequestBody(PART_SIZE + 1);
        CompletableFuture<PutObjectResponse> future = setupAndTriggerUploadFailure(asyncRequestBody);
        verifyFailureWithMessage(future, "Content length must not be greater than part size");
    }

    @Test
    void uploadObject_nullAsyncRequestBody_shouldFailRequest() {
        CloseableAsyncRequestBody asyncRequestBody = createMockAsyncRequestBody(PART_SIZE);
        SdkPublisher<CloseableAsyncRequestBody> mockPublisher = mock(SdkPublisher.class);
        when(asyncRequestBody.splitCloseable(any(Consumer.class))).thenReturn(mockPublisher);

        ArgumentCaptor<Subscriber<CloseableAsyncRequestBody>> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        CompletableFuture<PutObjectResponse> future = helper.uploadObject(createPutObjectRequest(), asyncRequestBody);

        verify(mockPublisher).subscribe(subscriberCaptor.capture());
        Subscriber<CloseableAsyncRequestBody> subscriber = subscriberCaptor.getValue();

        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        assertThatThrownBy(() -> subscriber.onNext(null)).isInstanceOf(NullPointerException.class).hasMessageContaining(
            "asyncRequestBody");

        assertThat(future).isCompletedExceptionally();
        future.exceptionally(throwable -> {
            assertThat(throwable.getCause()).isInstanceOf(NullPointerException.class);
            assertThat(throwable.getCause().getMessage()).contains("MUST NOT be null");
            return null;
        }).join();
    }

    private PutObjectRequest createPutObjectRequest() {
        return PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(KEY)
                .build();
    }

    private List<CompletedPart> createCompletedParts(int totalNumParts) {
        return IntStream.range(1, totalNumParts + 1)
                .mapToObj(i -> CompletedPart.builder().partNumber(i).build())
                .collect(Collectors.toList());
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

    private CompletableFuture<PutObjectResponse> setupAndTriggerUploadFailure(CloseableAsyncRequestBody asyncRequestBody) {
        SdkPublisher<CloseableAsyncRequestBody> mockPublisher = mock(SdkPublisher.class);
        when(asyncRequestBody.splitCloseable(any(Consumer.class))).thenReturn(mockPublisher);

        ArgumentCaptor<Subscriber<CloseableAsyncRequestBody>> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        CompletableFuture<PutObjectResponse> future = helper.uploadObject(createPutObjectRequest(), asyncRequestBody);

        verify(mockPublisher).subscribe(subscriberCaptor.capture());
        Subscriber<CloseableAsyncRequestBody> subscriber = subscriberCaptor.getValue();

        Subscription subscription = mock(Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(asyncRequestBody);

        stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        subscriber.onNext(asyncRequestBody);

        return future;
    }

    private void verifyFailureWithMessage(CompletableFuture<PutObjectResponse> future, String expectedErrorMessage) {
        assertThat(future).isCompletedExceptionally();
        future.exceptionally(throwable -> {
            assertThat(throwable).isInstanceOf(SdkClientException.class);
            assertThat(throwable.getMessage()).contains(expectedErrorMessage);
            return null;
        }).join();
    }

    private void verifyUploadPartRequests(List<UploadPartRequest> actualRequests,
            List<AsyncRequestBody> actualRequestBodies) {
        for (int i = 0; i < actualRequests.size(); i++) {
            UploadPartRequest request = actualRequests.get(i);
            AsyncRequestBody requestBody = actualRequestBodies.get(i);
            assertThat(request.partNumber()).isEqualTo(i + 1);
            assertThat(request.bucket()).isEqualTo(BUCKET);
            assertThat(request.key()).isEqualTo(KEY);

            if (i == actualRequests.size() - 1) {
                assertThat(requestBody.contentLength()).hasValue(5120L);
            } else {
                assertThat(requestBody.contentLength()).hasValue(PART_SIZE);
            }
        }
    }

    private void verifyCompleteMultipartUploadRequest() {
        ArgumentCaptor<CompleteMultipartUploadRequest> completeMpuArgumentCaptor = ArgumentCaptor
                .forClass(CompleteMultipartUploadRequest.class);
        verify(s3AsyncClient).completeMultipartUpload(completeMpuArgumentCaptor.capture());

        CompleteMultipartUploadRequest actualRequest = completeMpuArgumentCaptor.getValue();
        assertThat(actualRequest.multipartUpload().parts()).isEqualTo(createCompletedParts(NUM_TOTAL_PARTS));
    }
}
