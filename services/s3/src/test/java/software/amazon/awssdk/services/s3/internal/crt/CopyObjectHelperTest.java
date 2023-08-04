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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.internal.multipart.CopyObjectHelper;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;
class CopyObjectHelperTest {

    private static final String SOURCE_BUCKET = "source";
    private static final String SOURCE_KEY = "sourceKey";
    private static final String DESTINATION_BUCKET = "destination";
    private static final String DESTINATION_KEY = "destinationKey";
    private static final String MULTIPART_ID = "multipartId";

    private static final Long PART_SIZE_BYTES = 1024L;
    private S3AsyncClient s3AsyncClient;
    private CopyObjectHelper copyHelper;

    private static final long PART_SIZE = 1024L;
    private static final long UPLOAD_THRESHOLD = PART_SIZE * 2;

    @BeforeEach
    public void setUp() {
        s3AsyncClient = Mockito.mock(S3AsyncClient.class);
        copyHelper = new CopyObjectHelper(s3AsyncClient, PART_SIZE, UPLOAD_THRESHOLD);
    }

    @Test
    void copyObject_HeadObjectRequestFailed_shouldFail() {
        NoSuchBucketException exception = NoSuchBucketException.builder().build();
        CompletableFuture<HeadObjectResponse> headFuture =
            CompletableFutureUtils.failedFuture(exception);

        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(headFuture);

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest());

        assertThatThrownBy(future::join).hasCauseInstanceOf(SdkClientException.class)
                                        .hasMessageContaining("Failed to retrieve metadata")
                                        .hasRootCause(exception);
    }

    @Test
    void copyObject_HeadObjectRequestThrowsException_shouldFail() {
        RuntimeException exception = new RuntimeException("oops");

        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenThrow(exception);

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest());

        assertThatThrownBy(future::join).hasCause(exception);
    }

    @Test
    void singlePartCopy_happyCase_shouldSucceed() {

        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        stubSuccessfulHeadObjectCall(512L);

        CopyObjectResponse expectedResponse = CopyObjectResponse.builder().build();
        CompletableFuture<CopyObjectResponse> copyFuture =
            CompletableFuture.completedFuture(expectedResponse);

        when(s3AsyncClient.copyObject(copyObjectRequest)).thenReturn(copyFuture);

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest);

        assertThat(future.join()).isEqualTo(expectedResponse);
    }

    @Test
    void copy_doesNotExceedThreshold_shouldUseSingleObjectCopy() {

        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        stubSuccessfulHeadObjectCall(2000L);

        CopyObjectResponse expectedResponse = CopyObjectResponse.builder().build();
        CompletableFuture<CopyObjectResponse> copyFuture =
            CompletableFuture.completedFuture(expectedResponse);

        when(s3AsyncClient.copyObject(copyObjectRequest)).thenReturn(copyFuture);

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest);

        assertThat(future.join()).isEqualTo(expectedResponse);
    }

    @Test
    void multiPartCopy_fourPartsHappyCase_shouldSucceed() {
        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        stubSuccessfulHeadObjectCall(4000L);

        stubSuccessfulCreateMulipartCall();

        stubSuccessfulUploadPartCopyCalls();

        stubSuccessfulCompleteMultipartCall();

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest);

        CopyObjectResponse actualResponse = future.join();
        assertThat(actualResponse.copyObjectResult()).isNotNull();

        ArgumentCaptor<UploadPartCopyRequest> argumentCaptor = ArgumentCaptor.forClass(UploadPartCopyRequest.class);
        verify(s3AsyncClient, times(4)).uploadPartCopy(argumentCaptor.capture());
        List<UploadPartCopyRequest> actualUploadPartCopyRequests = argumentCaptor.getAllValues();
        assertThat(actualUploadPartCopyRequests).allSatisfy(d -> {
            assertThat(d.sourceBucket()).isEqualTo(SOURCE_BUCKET);
            assertThat(d.sourceKey()).isEqualTo(SOURCE_KEY);
            assertThat(d.destinationBucket()).isEqualTo(DESTINATION_BUCKET);
            assertThat(d.destinationKey()).isEqualTo(DESTINATION_KEY);
        });

        assertThat(actualUploadPartCopyRequests.get(0).copySourceRange()).isEqualTo("bytes=0-1023");
        assertThat(actualUploadPartCopyRequests.get(1).copySourceRange()).isEqualTo("bytes=1024-2047");
        assertThat(actualUploadPartCopyRequests.get(2).copySourceRange()).isEqualTo("bytes=2048-3071");
        assertThat(actualUploadPartCopyRequests.get(3).copySourceRange()).isEqualTo("bytes=3072-3999");
    }

    /**
     * Four parts, after the first part failed, the remaining four futures should be cancelled
     */
    @Test
    void multiPartCopy_onePartFailed_shouldFailOtherPartsAndAbort() {
        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        stubSuccessfulHeadObjectCall(4000L);

        stubSuccessfulCreateMulipartCall();

        SdkClientException exception = SdkClientException.create("failed");
        CompletableFuture<UploadPartCopyResponse> uploadPartCopyFuture1 =
            CompletableFutureUtils.failedFuture(exception);

        CompletableFuture<UploadPartCopyResponse> uploadPartCopyFuture2 = new CompletableFuture<>();
        CompletableFuture<UploadPartCopyResponse> uploadPartCopyFuture3 = new CompletableFuture<>();
        CompletableFuture<UploadPartCopyResponse> uploadPartCopyFuture4 = new CompletableFuture<>();

        when(s3AsyncClient.uploadPartCopy(any(UploadPartCopyRequest.class)))
            .thenReturn(uploadPartCopyFuture1, uploadPartCopyFuture2, uploadPartCopyFuture3, uploadPartCopyFuture4);

        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(copyObjectRequest);

        assertThatThrownBy(future::join).hasMessageContaining("Failed to send multipart requests").hasRootCause(exception);

        verify(s3AsyncClient, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

        ArgumentCaptor<AbortMultipartUploadRequest> argumentCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3AsyncClient).abortMultipartUpload(argumentCaptor.capture());
        AbortMultipartUploadRequest actualRequest = argumentCaptor.getValue();
        assertThat(actualRequest.uploadId()).isEqualTo(MULTIPART_ID);

        assertThat(uploadPartCopyFuture2).isCompletedExceptionally();
        assertThat(uploadPartCopyFuture3).isCompletedExceptionally();
        assertThat(uploadPartCopyFuture4).isCompletedExceptionally();
    }

    @Test
    void multiPartCopy_completeMultipartFailed_shouldFailAndAbort() {
        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        stubSuccessfulHeadObjectCall(4000L);

        stubSuccessfulCreateMulipartCall();

        stubSuccessfulUploadPartCopyCalls();

        SdkClientException exception = SdkClientException.create("CompleteMultipartUpload failed");

        CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUploadFuture =
            CompletableFutureUtils.failedFuture(exception);

        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(completeMultipartUploadFuture);

        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AbortMultipartUploadResponse.builder().build()));

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest);

        assertThatThrownBy(future::join).hasMessageContaining("Failed to send multipart requests").hasRootCause(exception);

        ArgumentCaptor<AbortMultipartUploadRequest> argumentCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3AsyncClient).abortMultipartUpload(argumentCaptor.capture());
        AbortMultipartUploadRequest actualRequest = argumentCaptor.getValue();
        assertThat(actualRequest.uploadId()).isEqualTo(MULTIPART_ID);
    }

    @Test
    void multiPartCopy_contentSizeExceeds10000Parts_shouldAdjustPartSize() {
        long contentLength = 1024L * 10_000 * 2; // twice too many parts with configures part size

        stubSuccessfulHeadObjectCall(contentLength);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(copyObjectRequest);

        CopyObjectResponse actualResponse = future.join();
        assertThat(actualResponse.copyObjectResult()).isNotNull();

        ArgumentCaptor<UploadPartCopyRequest> argumentCaptor = ArgumentCaptor.forClass(UploadPartCopyRequest.class);
        verify(s3AsyncClient, times(10_000)).uploadPartCopy(argumentCaptor.capture());
        List<UploadPartCopyRequest> actualUploadPartCopyRequests = argumentCaptor.getAllValues();
        assertThat(actualUploadPartCopyRequests).allSatisfy(d -> {
            assertThat(d.sourceBucket()).isEqualTo(SOURCE_BUCKET);
            assertThat(d.sourceKey()).isEqualTo(SOURCE_KEY);
            assertThat(d.destinationBucket()).isEqualTo(DESTINATION_BUCKET);
            assertThat(d.destinationKey()).isEqualTo(DESTINATION_KEY);
        });

        long expectedPartSize = 2048L;
        for (int i = 0; i < actualUploadPartCopyRequests.size(); i++) {
            int rangeStart = (int) expectedPartSize * i;
            int rangeEnd = (int) (rangeStart + (expectedPartSize - 1));
            assertThat(actualUploadPartCopyRequests.get(i).copySourceRange()).isEqualTo(
                String.format("bytes=%d-%d", rangeStart, rangeEnd));
        }
    }


    @Test
    public void multiPartCopy_sseCHeadersSetInOriginalRequest_includedInCompleteMultipart() {
        String customerAlgorithm = "algorithm";
        String customerKey = "key";
        String customerKeyMd5 = "keyMd5";

        CopyObjectRequest copyRequest = copyObjectRequest().copy(r -> r.sseCustomerAlgorithm(customerAlgorithm)
                                                                       .sseCustomerKey(customerKey)
                                                                       .sseCustomerKeyMD5(customerKeyMd5));

        stubSuccessfulHeadObjectCall(3 * PART_SIZE_BYTES);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyRequest).join();

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);

        verify(s3AsyncClient).completeMultipartUpload(completeMultipartCaptor.capture());

        CompleteMultipartUploadRequest completeRequest = completeMultipartCaptor.getValue();

        assertThat(completeRequest.sseCustomerAlgorithm()).isEqualTo(customerAlgorithm);
        assertThat(completeRequest.sseCustomerKey()).isEqualTo(customerKey);
        assertThat(completeRequest.sseCustomerKeyMD5()).isEqualTo(customerKeyMd5);
    }

    @Test
    void copy_cancelResponseFuture_shouldPropagate() {
        CopyObjectRequest copyObjectRequest = copyObjectRequest();

        CompletableFuture<HeadObjectResponse> headFuture = new CompletableFuture<>();

        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(headFuture);

        CompletableFuture<CopyObjectResponse> future =
            copyHelper.copyObject(copyObjectRequest);

        future.cancel(true);

        assertThat(headFuture).isCancelled();
    }

    private void stubSuccessfulUploadPartCopyCalls() {
        when(s3AsyncClient.uploadPartCopy(any(UploadPartCopyRequest.class)))
            .thenAnswer(new Answer<CompletableFuture<UploadPartCopyResponse>>() {
                int numberOfCalls = 0;

                @Override
                public CompletableFuture<UploadPartCopyResponse> answer(InvocationOnMock invocationOnMock) {
                    numberOfCalls++;
                    return CompletableFuture.completedFuture(UploadPartCopyResponse.builder().copyPartResult(CopyPartResult.builder()
                                                                                                                           .checksumCRC32("crc" + numberOfCalls)
                                                                                                                           .build())
                                                                                   .build());
                }
            });
    }

    private void stubSuccessfulCompleteMultipartCall() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUploadFuture =
            CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder()
                                                                             .bucket(DESTINATION_BUCKET)
                                                                             .key(DESTINATION_KEY)
                                                                             .build());

        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(completeMultipartUploadFuture);
    }

    private void stubSuccessfulHeadObjectCall(long contentLength) {
        CompletableFuture<HeadObjectResponse> headFuture =
            CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                .contentLength(contentLength)
                                                                .build());

        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(headFuture);
    }

    private void stubSuccessfulCreateMulipartCall() {
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder()
                                                                           .uploadId(MULTIPART_ID)
                                                                           .build());

        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(createMultipartUploadFuture);
    }

    private static CopyObjectRequest copyObjectRequest() {
        return CopyObjectRequest.builder()
                                .sourceBucket(SOURCE_BUCKET)
                                .sourceKey(SOURCE_KEY)
                                .destinationBucket(DESTINATION_BUCKET)
                                .destinationKey(DESTINATION_KEY)
                                .build();
    }
}
