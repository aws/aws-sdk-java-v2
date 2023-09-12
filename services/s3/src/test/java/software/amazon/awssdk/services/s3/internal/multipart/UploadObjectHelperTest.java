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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.internal.multipart.MpuTestUtils.stubSuccessfulCompleteMultipartCall;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.testutils.RandomTempFile;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class UploadObjectHelperTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final long PART_SIZE = 8 * 1024;

    // Should contain four parts: [8KB, 8KB, 8KB, 1KB]
    private static final long MPU_CONTENT_SIZE = 25 * 1024;
    private static final long THRESHOLD = 10 * 1024;
    private static final String UPLOAD_ID = "1234";

    private static RandomTempFile testFile;
    private UploadObjectHelper uploadHelper;
    private S3AsyncClient s3AsyncClient;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testFile = new RandomTempFile("testfile.dat", MPU_CONTENT_SIZE);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        testFile.delete();
    }

    public static Stream<AsyncRequestBody> asyncRequestBody() {
        return Stream.of(new UnknownContentLengthAsyncRequestBody(AsyncRequestBody.fromFile(testFile)),
                         AsyncRequestBody.fromFile(testFile));
    }

    @BeforeEach
    public void beforeEach() {
        s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        uploadHelper = new UploadObjectHelper(s3AsyncClient,
                                              new MultipartConfigurationResolver(MultipartConfiguration.builder()
                                                                                                       .minimumPartSizeInBytes(PART_SIZE)
                                                                                                       .thresholdInBytes(THRESHOLD)
                                                                                                       .thresholdInBytes(PART_SIZE * 2)
                                                                                                       .build()));
    }

    @ParameterizedTest
    @ValueSource(longs = {THRESHOLD, PART_SIZE, THRESHOLD - 1, PART_SIZE - 1})
    void uploadObject_contentLengthDoesNotExceedThresholdAndPartSize_shouldUploadInOneChunk(long contentLength) {
        PutObjectRequest putObjectRequest = putObjectRequest(contentLength);
        AsyncRequestBody asyncRequestBody = Mockito.mock(AsyncRequestBody.class);

        CompletableFuture<PutObjectResponse> completedFuture =
            CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        when(s3AsyncClient.putObject(putObjectRequest, asyncRequestBody)).thenReturn(completedFuture);
        uploadHelper.uploadObject(putObjectRequest, asyncRequestBody).join();
        Mockito.verify(s3AsyncClient).putObject(putObjectRequest, asyncRequestBody);
    }

    @ParameterizedTest
    @ValueSource(longs = {PART_SIZE, PART_SIZE - 1})
    void uploadObject_unKnownContentLengthDoesNotExceedPartSize_shouldUploadInOneChunk(long contentLength) {
        PutObjectRequest putObjectRequest = putObjectRequest(contentLength);
        AsyncRequestBody asyncRequestBody =
            new UnknownContentLengthAsyncRequestBody(AsyncRequestBody.fromBytes(RandomStringUtils.randomAscii(Math.toIntExact(contentLength))
                                                                                                 .getBytes(StandardCharsets.UTF_8)));

        CompletableFuture<PutObjectResponse> completedFuture =
            CompletableFuture.completedFuture(PutObjectResponse.builder().build());
        when(s3AsyncClient.putObject(putObjectRequest, asyncRequestBody)).thenReturn(completedFuture);
        uploadHelper.uploadObject(putObjectRequest, asyncRequestBody).join();
        Mockito.verify(s3AsyncClient).putObject(putObjectRequest, asyncRequestBody);
    }

    @ParameterizedTest
    @MethodSource("asyncRequestBody")
    void uploadObject_contentLengthExceedThresholdAndPartSize_shouldUseMPU(AsyncRequestBody asyncRequestBody) {
        PutObjectRequest putObjectRequest = putObjectRequest(null);

        MpuTestUtils.stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        stubSuccessfulUploadPartCalls();
        stubSuccessfulCompleteMultipartCall(BUCKET, KEY, s3AsyncClient);

        uploadHelper.uploadObject(putObjectRequest, asyncRequestBody).join();
        ArgumentCaptor<UploadPartRequest> requestArgumentCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        ArgumentCaptor<AsyncRequestBody> requestBodyArgumentCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(s3AsyncClient, times(4)).uploadPart(requestArgumentCaptor.capture(),
                                                   requestBodyArgumentCaptor.capture());

        List<UploadPartRequest> actualRequests = requestArgumentCaptor.getAllValues();
        List<AsyncRequestBody> actualRequestBodies = requestBodyArgumentCaptor.getAllValues();
        assertThat(actualRequestBodies).hasSize(4);
        assertThat(actualRequests).hasSize(4);

        for (int i = 0; i < actualRequests.size(); i++) {
            UploadPartRequest request = actualRequests.get(i);
            AsyncRequestBody requestBody = actualRequestBodies.get(i);
            assertThat(request.partNumber()).isEqualTo( i + 1);
            assertThat(request.bucket()).isEqualTo(BUCKET);
            assertThat(request.key()).isEqualTo(KEY);

            if (i == actualRequests.size() - 1) {
                assertThat(requestBody.contentLength()).hasValue(1024L);
            } else{
                assertThat(requestBody.contentLength()).hasValue(PART_SIZE);
            }
        }
    }

    /**
     * The second part failed, it should cancel ongoing part(first part).
     */
    @ParameterizedTest
    @MethodSource("asyncRequestBody")
    void mpu_onePartFailed_shouldFailOtherPartsAndAbort(AsyncRequestBody asyncRequestBody) {
        PutObjectRequest putObjectRequest = putObjectRequest(MPU_CONTENT_SIZE);

        MpuTestUtils.stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        CompletableFuture<UploadPartResponse> ongoingRequest = new CompletableFuture<>();

        SdkClientException exception = SdkClientException.create("request failed");

        OngoingStubbing<CompletableFuture<UploadPartResponse>> ongoingStubbing =
            when(s3AsyncClient.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class))).thenReturn(ongoingRequest);

        stubFailedUploadPartCalls(ongoingStubbing, exception);

        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));

        CompletableFuture<PutObjectResponse> future = uploadHelper.uploadObject(putObjectRequest,
                                                                                asyncRequestBody);

        assertThatThrownBy(() -> future.get(100, TimeUnit.MILLISECONDS))
            .hasMessageContaining("Failed to send multipart upload requests").hasRootCause(exception);

        verify(s3AsyncClient, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

        ArgumentCaptor<AbortMultipartUploadRequest> argumentCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3AsyncClient).abortMultipartUpload(argumentCaptor.capture());
        AbortMultipartUploadRequest actualRequest = argumentCaptor.getValue();
        assertThat(actualRequest.uploadId()).isEqualTo(UPLOAD_ID);

        try {
            ongoingRequest.get(100, TimeUnit.MILLISECONDS);
            fail("no exception thrown");
        } catch (Exception e) {
            assertThat(e.getCause()).isEqualTo(exception);
        }
    }

    /**
     * This test is not parameterized because for unknown content length, the progress is nondeterministic. For example, we
     * don't know if it has created multipart upload when we cancel the future.
     */
    @Test
    void upload_knownContentLengthCancelResponseFuture_shouldCancelCreateMultipart() {
        PutObjectRequest putObjectRequest = putObjectRequest(null);

        CompletableFuture<CreateMultipartUploadResponse> createMultipartFuture = new CompletableFuture<>();

        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(createMultipartFuture);

        CompletableFuture<PutObjectResponse> future =
            uploadHelper.uploadObject(putObjectRequest, AsyncRequestBody.fromFile(testFile));

        future.cancel(true);

        assertThat(createMultipartFuture).isCancelled();
    }

    @Test
    void upload_knownContentLengthCancelResponseFuture_shouldCancelUploadPart() {
        PutObjectRequest putObjectRequest = putObjectRequest(null);

        CompletableFuture<CreateMultipartUploadResponse> createMultipartFuture = new CompletableFuture<>();

        MpuTestUtils.stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);

        CompletableFuture<UploadPartResponse> ongoingRequest = new CompletableFuture<>();

            when(s3AsyncClient.uploadPart(any(UploadPartRequest.class),
                                          any(AsyncRequestBody.class))).thenReturn(ongoingRequest);

        CompletableFuture<PutObjectResponse> future =
            uploadHelper.uploadObject(putObjectRequest, AsyncRequestBody.fromFile(testFile));

        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));

        future.cancel(true);

        try {
            ongoingRequest.join();
            fail("no exception");
        } catch (Exception exception) {
            assertThat(ongoingRequest).isCancelled();
        }
    }

    @ParameterizedTest
    @MethodSource("asyncRequestBody")
    void uploadObject_createMultipartUploadFailed_shouldFail(AsyncRequestBody asyncRequestBody) {
        PutObjectRequest putObjectRequest = putObjectRequest(null);

        SdkClientException exception = SdkClientException.create("CompleteMultipartUpload failed");

        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            CompletableFutureUtils.failedFuture(exception);

        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(createMultipartUploadFuture);

        CompletableFuture<PutObjectResponse> future = uploadHelper.uploadObject(putObjectRequest,
                                                                                asyncRequestBody);
        assertThatThrownBy(future::join).hasMessageContaining("Failed to initiate multipart upload")
                                        .hasRootCause(exception);
    }

    @ParameterizedTest
    @MethodSource("asyncRequestBody")
    void uploadObject_completeMultipartFailed_shouldFailAndAbort(AsyncRequestBody asyncRequestBody) {
        PutObjectRequest putObjectRequest = putObjectRequest(null);

        MpuTestUtils.stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        stubSuccessfulUploadPartCalls();

        SdkClientException exception = SdkClientException.create("CompleteMultipartUpload failed");

        CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUploadFuture =
            CompletableFutureUtils.failedFuture(exception);

        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(completeMultipartUploadFuture);

        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));

        CompletableFuture<PutObjectResponse> future = uploadHelper.uploadObject(putObjectRequest,
                                                                                asyncRequestBody);
        assertThatThrownBy(future::join).hasMessageContaining("Failed to send multipart requests")
                                        .hasRootCause(exception);
    }

    @ParameterizedTest()
    @ValueSource(booleans = {false, true})
    void uploadObject_requestBodyOnError_shouldFailAndAbort(boolean contentLengthKnown) {
        PutObjectRequest putObjectRequest = putObjectRequest(null);
        Exception exception = new RuntimeException("error");

        Long contentLength = contentLengthKnown ? MPU_CONTENT_SIZE : null;
        ErroneousAsyncRequestBody erroneousAsyncRequestBody =
            new ErroneousAsyncRequestBody(contentLength, exception);
        MpuTestUtils.stubSuccessfulCreateMultipartCall(UPLOAD_ID, s3AsyncClient);
        stubSuccessfulUploadPartCalls();

        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));

        CompletableFuture<PutObjectResponse> future = uploadHelper.uploadObject(putObjectRequest,
                                                                                erroneousAsyncRequestBody);
        assertThatThrownBy(future::join).hasMessageContaining("Failed to send multipart upload requests")
                                        .hasRootCause(exception);
    }

    private static PutObjectRequest putObjectRequest(Long contentLength) {
        return PutObjectRequest.builder()
                               .bucket(BUCKET)
                               .key(KEY)
                               .contentLength(contentLength)
                               .build();
    }

    private void stubSuccessfulUploadPartCalls() {
        when(s3AsyncClient.uploadPart(any(UploadPartRequest.class), any(AsyncRequestBody.class)))
            .thenAnswer(new Answer<CompletableFuture<UploadPartResponse>>() {
                int numberOfCalls = 0;

                @Override
                public CompletableFuture<UploadPartResponse> answer(InvocationOnMock invocationOnMock) {
                    AsyncRequestBody AsyncRequestBody = invocationOnMock.getArgument(1);
                    // Draining the request body
                    AsyncRequestBody.subscribe(b -> {});

                    numberOfCalls++;
                    return CompletableFuture.completedFuture(UploadPartResponse.builder()
                                                                               .checksumCRC32("crc" + numberOfCalls)
                                                                               .build());
                }
            });
    }

    private OngoingStubbing<CompletableFuture<UploadPartResponse>> stubFailedUploadPartCalls(OngoingStubbing<CompletableFuture<UploadPartResponse>> stubbing, Exception exception) {
        return stubbing.thenAnswer(new Answer<CompletableFuture<UploadPartResponse>>() {

                @Override
                public CompletableFuture<UploadPartResponse> answer(InvocationOnMock invocationOnMock) {
                    AsyncRequestBody AsyncRequestBody = invocationOnMock.getArgument(1);
                    // Draining the request body
                    AsyncRequestBody.subscribe(b -> {});

                    return  CompletableFutureUtils.failedFuture(exception);
                }
            });
    }

    private static class UnknownContentLengthAsyncRequestBody implements AsyncRequestBody {
        private final AsyncRequestBody delegate;
        private volatile boolean cancelled;

        public UnknownContentLengthAsyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.delegate = asyncRequestBody;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.empty();
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            delegate.subscribe(s);
        }
    }

    private static class ErroneousAsyncRequestBody implements AsyncRequestBody {
        private volatile boolean isDone;
        private final Long contentLength;
        private final Exception exception;

        private ErroneousAsyncRequestBody(Long contentLength, Exception exception) {
            this.contentLength = contentLength;
            this.exception = exception;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }


        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (isDone) {
                        return;
                    }
                    isDone = true;
                    s.onNext(ByteBuffer.wrap(RandomStringUtils.randomAscii(Math.toIntExact(PART_SIZE)).getBytes(StandardCharsets.UTF_8)));
                    s.onNext(ByteBuffer.wrap(RandomStringUtils.randomAscii(Math.toIntExact(PART_SIZE)).getBytes(StandardCharsets.UTF_8)));
                    s.onError(exception);

                }

                @Override
                public void cancel() {
                }
            });

        }
    }
}
