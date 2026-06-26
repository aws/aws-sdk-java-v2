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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
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
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.TaggingDirective;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.AnnotationDirective;
import software.amazon.awssdk.services.s3.model.AnnotationEntry;
import software.amazon.awssdk.services.s3.model.GetObjectAnnotationResponse;
import software.amazon.awssdk.services.s3.model.ListObjectAnnotationsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectAnnotationsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectAnnotationResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.paginators.ListObjectAnnotationsPublisher;
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
        when(s3AsyncClient.listObjectAnnotationsPaginator(any(Consumer.class)))
            .thenAnswer(invocation -> {
                Consumer<ListObjectAnnotationsRequest.Builder> consumer = invocation.getArgument(0);
                ListObjectAnnotationsRequest request = ListObjectAnnotationsRequest.builder()
                                                                                   .applyMutation(consumer)
                                                                                   .build();
                return new ListObjectAnnotationsPublisher(s3AsyncClient, request);
            });
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

        assertThatThrownBy(future::join).hasCauseInstanceOf(NoSuchBucketException.class)
                                        .hasStackTraceContaining("Failed to retrieve metadata")
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

        assertThatThrownBy(future::join).hasStackTraceContaining("Failed to send multipart copy requests").hasCause(exception);

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

        assertThatThrownBy(future::join).hasStackTraceContaining("Failed to send multipart copy requests").hasCause(exception);

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
    public void multiPartCopy_overrideConfigSetInOriginalRequest_includedInCompleteMultipart() {
        AwsRequestOverrideConfiguration overrideConfig = AwsRequestOverrideConfiguration.builder()
                                                                                        .putHeader("x-custom", "value")
                                                                                        .build();
        CopyObjectRequest copyRequest = copyRequestBuilder().overrideConfiguration(overrideConfig).build();

        stubSuccessfulHeadObjectCall(3 * PART_SIZE_BYTES);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyRequest).join();

        ArgumentCaptor<CompleteMultipartUploadRequest> completeMultipartCaptor =
            ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);

        verify(s3AsyncClient).completeMultipartUpload(completeMultipartCaptor.capture());

        CompleteMultipartUploadRequest completeRequest = completeMultipartCaptor.getValue();

        assertThat(completeRequest.overrideConfiguration()).isPresent();
        assertThat(completeRequest.overrideConfiguration().get()).isEqualTo(overrideConfig);
    }

    @Test
    public void multiPartCopy_sseCHeadersSetInOriginalRequest_includedInCompleteMultipart() {
        String customerAlgorithm = "algorithm";
        String customerKey = "key";
        String customerKeyMd5 = "keyMd5";

        CopyObjectRequest copyRequest = copyRequestBuilder()
            .sseCustomerAlgorithm(customerAlgorithm)
            .sseCustomerKey(customerKey)
            .sseCustomerKeyMD5(customerKeyMd5)
            .build();

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

    @ParameterizedTest
    @MethodSource("metadataDirectiveCopyProvider")
    void multiPartCopy_metadataDirectiveCopyOrUnset_shouldForwardSourceMetadata(MetadataDirective directive) {
        CopyObjectRequest.Builder requestBuilder = copyRequestBuilder();
        if (directive != null) {
            requestBuilder.metadataDirective(directive);
        }
        CopyObjectRequest copyObjectRequest = requestBuilder.build();

        stubSuccessfulHeadObjectCallWithMetadata(4000L);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyObjectRequest).join();

        ArgumentCaptor<CreateMultipartUploadRequest> captor = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(s3AsyncClient).createMultipartUpload(captor.capture());

        CreateMultipartUploadRequest actualRequest = captor.getValue();
        assertThat(actualRequest.metadata()).containsEntry("customKey", "customValue");
        assertThat(actualRequest.contentType()).isEqualTo("application/zip");
        assertThat(actualRequest.cacheControl()).isEqualTo("max-age=86400");
        assertThat(actualRequest.contentDisposition()).isEqualTo("attachment");
        assertThat(actualRequest.contentEncoding()).isEqualTo("gzip");
        assertThat(actualRequest.contentLanguage()).isEqualTo("en-US");
        assertThat(actualRequest.expires()).isEqualTo(Instant.ofEpochSecond(1700000000L));
    }

    @Test
    void multiPartCopy_metadataDirectiveReplace_shouldNotForwardSourceMetadata() {
        CopyObjectRequest copyObjectRequest = copyRequestBuilder()
            .metadataDirective(MetadataDirective.REPLACE)
            .metadata(Collections.singletonMap("newKey", "newValue"))
            .contentType("text/plain")
            .build();

        stubSuccessfulHeadObjectCallWithMetadata(4000L);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyObjectRequest).join();

        ArgumentCaptor<CreateMultipartUploadRequest> captor = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(s3AsyncClient).createMultipartUpload(captor.capture());

        CreateMultipartUploadRequest actualRequest = captor.getValue();
        assertThat(actualRequest.metadata()).containsEntry("newKey", "newValue");
        assertThat(actualRequest.metadata()).doesNotContainKey("customKey");
        assertThat(actualRequest.contentType()).isEqualTo("text/plain");
    }

    @Test
    void multiPartCopy_metadataDirectiveCopyWithCustomerMetadata_sourceMetadataShouldWin() {
        CopyObjectRequest copyObjectRequest = copyRequestBuilder()
            .metadataDirective(MetadataDirective.COPY)
            .metadata(Collections.singletonMap("ignored", "value"))
            .contentType("text/html")
            .build();

        stubSuccessfulHeadObjectCallWithMetadata(4000L);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyObjectRequest).join();

        ArgumentCaptor<CreateMultipartUploadRequest> captor = ArgumentCaptor.forClass(CreateMultipartUploadRequest.class);
        verify(s3AsyncClient).createMultipartUpload(captor.capture());

        CreateMultipartUploadRequest actualRequest = captor.getValue();
        // Source metadata wins when directive is COPY, matching S3 CopyObject API behavior
        assertThat(actualRequest.metadata()).containsEntry("customKey", "customValue");
        assertThat(actualRequest.metadata()).doesNotContainKey("ignored");
        assertThat(actualRequest.contentType()).isEqualTo("application/zip");
    }

    @Test
    void multiPartCopy_shouldPinSourceVersionIdAndETag() {
        String sourceVersionId = "version-abc-123";
        String sourceETag = "\"etag-xyz-456\"";

        stubSuccessfulHeadObjectCall(4000L, sourceVersionId, sourceETag);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyObjectRequest()).join();

        ArgumentCaptor<UploadPartCopyRequest> captor = ArgumentCaptor.forClass(UploadPartCopyRequest.class);
        verify(s3AsyncClient, times(4)).uploadPartCopy(captor.capture());

        List<UploadPartCopyRequest> partRequests = captor.getAllValues();
        assertThat(partRequests).allSatisfy(r -> {
            assertThat(r.sourceVersionId()).isEqualTo(sourceVersionId);
            assertThat(r.copySourceIfMatch()).isEqualTo(sourceETag);
        });
    }

    @Test
    void multiPartCopy_noVersionIdFromHead_shouldUseCustomerProvidedVersionId() {
        String customerVersionId = "customer-version-id";
        String sourceETag = "\"etag-from-head\"";

        stubSuccessfulHeadObjectCall(4000L, null, sourceETag);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        CopyObjectRequest request = copyRequestBuilder()
            .sourceVersionId(customerVersionId)
            .build();

        copyHelper.copyObject(request).join();

        ArgumentCaptor<UploadPartCopyRequest> captor = ArgumentCaptor.forClass(UploadPartCopyRequest.class);
        verify(s3AsyncClient, times(4)).uploadPartCopy(captor.capture());

        List<UploadPartCopyRequest> partRequests = captor.getAllValues();
        assertThat(partRequests).allSatisfy(r -> {
            assertThat(r.sourceVersionId()).isEqualTo(customerVersionId);
            assertThat(r.copySourceIfMatch()).isEqualTo(sourceETag);
        });
    }

    @Test
    void multiPartCopy_taggingDirectiveCopy_shouldFetchAndApplyTagsPostCompletion() {
        stubSuccessfulHeadObjectCall(4000L, "version-123", "\"etag\"");

        List<Tag> sourceTags = Arrays.asList(
            Tag.builder().key("env").value("prod").build(),
            Tag.builder().key("team").value("sdk").build()
        );
        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetObjectTaggingResponse.builder().tagSet(sourceTags).build()));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCallWithVersion("dest-v", "\"dest-e\"");

        when(s3AsyncClient.putObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectTaggingResponse.builder().build()));

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient).getObjectTagging(any(Consumer.class));
        verify(s3AsyncClient).putObjectTagging(any(Consumer.class));
    }

    @Test
    void multiPartCopy_noTaggingDirective_shouldNotCallGetObjectTagging() {
        stubSuccessfulHeadObjectCall(4000L);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyObjectRequest()).join();

        verify(s3AsyncClient, never()).getObjectTagging(any(Consumer.class));
    }

    @Test
    void multiPartCopy_taggingDirectiveCopy_getObjectTaggingFails_shouldFail() {
        stubSuccessfulHeadObjectCall(4000L, null, "\"etag\"");

        SdkClientException exception = SdkClientException.create("Access Denied");
        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(exception));

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        assertThatThrownBy(future::join).hasCauseInstanceOf(SdkClientException.class)
                                        .hasStackTraceContaining("Failed to read source object properties");

        verify(s3AsyncClient, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void multiPartCopy_annotationDirectiveCopy_shouldFetchAndWriteAnnotations() {
        String sourceVersionId = "src-version";
        byte[] annotationBody = "annotation-content".getBytes();

        stubSuccessfulHeadObjectCall(4000L, sourceVersionId, "\"src-etag\"");

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                ListObjectAnnotationsResponse.builder()
                                             .annotations(AnnotationEntry.builder().annotationName("anno1").build())
                                             .build()));

        ResponseBytes<GetObjectAnnotationResponse> responseBytes =
            ResponseBytes.fromByteArray(GetObjectAnnotationResponse.builder().build(), annotationBody);
        when(s3AsyncClient.getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(responseBytes));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCallWithVersion("dest-version", "\"dest-etag\"");

        when(s3AsyncClient.putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectAnnotationResponse.builder().build()));

        CopyObjectRequest request = copyRequestBuilder()
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        ArgumentCaptor<ListObjectAnnotationsRequest> listCaptor = ArgumentCaptor.forClass(ListObjectAnnotationsRequest.class);
        verify(s3AsyncClient).listObjectAnnotations(listCaptor.capture());
        assertThat(listCaptor.getValue().versionId()).isEqualTo(sourceVersionId);

        verify(s3AsyncClient).getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class));
        verify(s3AsyncClient).putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class));
    }

    @Test
    void multiPartCopy_noAnnotationDirective_shouldNotCallAnnotationApis() {
        stubSuccessfulHeadObjectCall(4000L);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        copyHelper.copyObject(copyObjectRequest()).join();

        verify(s3AsyncClient, never()).listObjectAnnotationsPaginator(any(Consumer.class));
        verify(s3AsyncClient, never()).getObjectAnnotation(any(Consumer.class),
                                                           any(AsyncResponseTransformer.class));
        verify(s3AsyncClient, never()).putObjectAnnotation(any(Consumer.class),
                                                           any(AsyncRequestBody.class));
    }

    @Test
    void multiPartCopy_annotationWriteFails_shouldReportPartialFailure() {
        byte[] annotationBody = "content".getBytes();

        stubSuccessfulHeadObjectCall(4000L, "src-version", "\"src-etag\"");

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                ListObjectAnnotationsResponse.builder()
                                             .annotations(
                                                 AnnotationEntry.builder().annotationName("good").build(),
                                                 AnnotationEntry.builder().annotationName("bad").build())
                                             .build()));

        ResponseBytes<GetObjectAnnotationResponse> responseBytes =
            ResponseBytes.fromByteArray(GetObjectAnnotationResponse.builder().build(), annotationBody);
        when(s3AsyncClient.getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(responseBytes));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCallWithVersion("dest-v", "\"dest-e\"");

        S3Exception preconditionFailed = (S3Exception) S3Exception.builder()
                                                                  .statusCode(412).message("Precondition Failed").build();

        when(s3AsyncClient.putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectAnnotationResponse.builder().build()))
            .thenReturn(CompletableFutureUtils.failedFuture(preconditionFailed));

        CopyObjectRequest request = copyRequestBuilder()
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(SdkClientException.class)
            .hasMessageContaining("Failed to copy 1 of 2 annotations to destination object");
    }

    @Test
    void multiPartCopy_annotationPagination_shouldFetchAllPages() {
        byte[] body = "data".getBytes();

        stubSuccessfulHeadObjectCall(4000L, "src-version", "\"src-etag\"");

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                ListObjectAnnotationsResponse.builder()
                                             .annotations(AnnotationEntry.builder().annotationName("anno1").build())
                                             .nextContinuationToken("token-abc")
                                             .build()))
            .thenReturn(CompletableFuture.completedFuture(
                ListObjectAnnotationsResponse.builder()
                                             .annotations(AnnotationEntry.builder().annotationName("anno2").build())
                                             .build()));

        ResponseBytes<GetObjectAnnotationResponse> responseBytes =
            ResponseBytes.fromByteArray(GetObjectAnnotationResponse.builder().build(), body);
        when(s3AsyncClient.getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(responseBytes));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCallWithVersion("dest-v", "\"dest-e\"");

        when(s3AsyncClient.putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectAnnotationResponse.builder().build()));

        CopyObjectRequest request = copyRequestBuilder()
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient, times(2)).listObjectAnnotations(any(ListObjectAnnotationsRequest.class));
        verify(s3AsyncClient, times(2)).getObjectAnnotation(any(Consumer.class),
                                                            any(AsyncResponseTransformer.class));
        verify(s3AsyncClient, times(2)).putObjectAnnotation(any(Consumer.class),
                                                            any(AsyncRequestBody.class));
    }

    @Test
    void singlePartCopy_withDirectives_shouldNotFetchTagsOrAnnotations() {
        stubSuccessfulHeadObjectCall(500L, null, "\"etag\"");

        when(s3AsyncClient.copyObject(any(CopyObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CopyObjectResponse.builder().build()));

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient, never()).getObjectTagging(any(Consumer.class));
        verify(s3AsyncClient, never()).listObjectAnnotationsPaginator(any(Consumer.class));
        verify(s3AsyncClient).copyObject(any(CopyObjectRequest.class));
    }

    @Test
    void multiPartCopy_bothDirectivesCopy_shouldFetchTagsAndAnnotations() {
        byte[] body = "data".getBytes();

        stubSuccessfulHeadObjectCall(4000L, "src-version", "\"src-etag\"");

        List<Tag> tags = Arrays.asList(Tag.builder().key("k").value("v").build());
        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetObjectTaggingResponse.builder().tagSet(tags).build()));

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                ListObjectAnnotationsResponse.builder()
                                             .annotations(AnnotationEntry.builder().annotationName("a1").build())
                                             .build()));

        ResponseBytes<GetObjectAnnotationResponse> responseBytes =
            ResponseBytes.fromByteArray(GetObjectAnnotationResponse.builder().build(), body);
        when(s3AsyncClient.getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(responseBytes));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCallWithVersion("dest-v", "\"dest-e\"");

        when(s3AsyncClient.putObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectTaggingResponse.builder().build()));
        when(s3AsyncClient.putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFuture.completedFuture(PutObjectAnnotationResponse.builder().build()));

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient).getObjectTagging(any(Consumer.class));
        verify(s3AsyncClient).listObjectAnnotations(any(ListObjectAnnotationsRequest.class));
        verify(s3AsyncClient).getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class));
        verify(s3AsyncClient).putObjectTagging(any(Consumer.class));
        verify(s3AsyncClient).putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class));
    }

    @Test
    void multiPartCopy_listObjectAnnotationsFails403_shouldFail() {
        stubSuccessfulHeadObjectCall(4000L, null, "\"etag\"");

        S3Exception accessDenied = (S3Exception) S3Exception.builder()
                                                            .statusCode(403).message("Access Denied").build();
        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(accessDenied));

        CopyObjectRequest request = copyRequestBuilder()
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        assertThatThrownBy(future::join).hasStackTraceContaining("Failed to read source object properties");

        verify(s3AsyncClient, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void multiPartCopy_annotationDirectiveExclude_shouldNotCopyAnnotations() {
        stubSuccessfulHeadObjectCall(4000L);
        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        CopyObjectRequest request = copyRequestBuilder()
            .annotationDirective(AnnotationDirective.EXCLUDE)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient, never()).listObjectAnnotationsPaginator(any(Consumer.class));
    }

    @Test
    void multiPartCopy_sourceHasEmptyAnnotations_shouldSkipPhase3() {
        stubSuccessfulHeadObjectCall(4000L, "v1", "\"etag\"");

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                ListObjectAnnotationsResponse.builder().annotations(Collections.emptyList()).build()));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        CopyObjectRequest request = copyRequestBuilder()
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient, never()).getObjectAnnotation(any(Consumer.class),
                                                           any(AsyncResponseTransformer.class));
        verify(s3AsyncClient, never()).putObjectAnnotation(any(Consumer.class),
                                                           any(AsyncRequestBody.class));
    }

    @Test
    void multiPartCopy_sourceHasEmptyTags_shouldNotCallPutObjectTagging() {
        stubSuccessfulHeadObjectCall(4000L, null, "\"etag\"");

        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetObjectTaggingResponse.builder().tagSet(Collections.emptyList()).build()));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCall();

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .build();

        copyHelper.copyObject(request).join();

        verify(s3AsyncClient, never()).putObjectTagging(any(Consumer.class));
    }

    @Test
    void multiPartCopy_cancelReturnFuture_shouldNotProceedToCreateMpu() {
        stubSuccessfulHeadObjectCall(4000L, null, "\"etag\"");

        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(new CompletableFuture<>());

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(new CompletableFuture<>());

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        verify(s3AsyncClient).getObjectTagging(any(Consumer.class));

        future.cancel(true);

        assertThat(future).isCancelled();
        verify(s3AsyncClient, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void multiPartCopy_taggingFetchFails_shouldCancelAnnotationFetchAndNotProceed() {
        stubSuccessfulHeadObjectCall(4000L, null, "\"etag\"");

        SdkClientException exception = SdkClientException.create("Access Denied");
        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(exception));

        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(new CompletableFuture<>());

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        assertThatThrownBy(future::join)
            .hasStackTraceContaining("Failed to read source object properties");

        verify(s3AsyncClient, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void multiPartCopy_annotationFetchFails_shouldCancelTaggingFetchAndNotProceed() {
        stubSuccessfulHeadObjectCall(4000L, null, "\"etag\"");

        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(new CompletableFuture<>());

        S3Exception accessDenied = (S3Exception) S3Exception.builder()
                                                            .statusCode(403).message("Access Denied").build();
        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(accessDenied));

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .annotationDirective(AnnotationDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        assertThatThrownBy(future::join)
            .hasStackTraceContaining("Failed to read source object properties");

        verify(s3AsyncClient, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    void multiPartCopy_putObjectTaggingFails_shouldFailCopy() {
        stubSuccessfulHeadObjectCall(4000L, "version-123", "\"etag\"");

        List<Tag> sourceTags = Arrays.asList(Tag.builder().key("k").value("v").build());
        when(s3AsyncClient.getObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFuture.completedFuture(
                GetObjectTaggingResponse.builder().tagSet(sourceTags).build()));

        stubSuccessfulCreateMulipartCall();
        stubSuccessfulUploadPartCopyCalls();
        stubSuccessfulCompleteMultipartCallWithVersion("dest-v", "\"dest-e\"");

        S3Exception accessDenied = (S3Exception) S3Exception.builder()
                                                            .statusCode(403).message("Access Denied").build();
        when(s3AsyncClient.putObjectTagging(any(Consumer.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(accessDenied));

        CopyObjectRequest request = copyRequestBuilder()
            .taggingDirective(TaggingDirective.COPY)
            .build();

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(request);

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(S3Exception.class)
            .hasMessageContaining("Access Denied");
    }

    @Test
    void multiPartCopy_cancelReturnFuture_shouldCancelCreateMpuFuture() {
        stubSuccessfulHeadObjectCall(4000L);

        CompletableFuture<CreateMultipartUploadResponse> createMpuFuture = new CompletableFuture<>();
        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(createMpuFuture);

        CompletableFuture<CopyObjectResponse> future = copyHelper.copyObject(copyObjectRequest());
        future.cancel(true);

        assertThat(createMpuFuture).isCompletedExceptionally();
    }

    private static CopyObjectRequest.Builder copyRequestBuilder() {
        return CopyObjectRequest.builder()
                                .sourceBucket(SOURCE_BUCKET)
                                .sourceKey(SOURCE_KEY)
                                .destinationBucket(DESTINATION_BUCKET)
                                .destinationKey(DESTINATION_KEY);
    }

    private static CopyObjectRequest copyObjectRequest() {
        return copyRequestBuilder().build();
    }

    private void stubSuccessfulHeadObjectCall(long contentLength) {
        stubSuccessfulHeadObjectCall(contentLength, null, null);
    }

    private void stubSuccessfulHeadObjectCall(long contentLength, String versionId, String eTag) {
        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                            .contentLength(contentLength)
                                                                            .versionId(versionId)
                                                                            .eTag(eTag)
                                                                            .build()));
    }

    private void stubSuccessfulHeadObjectCallWithMetadata(long contentLength) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("customKey", "customValue");

        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                            .contentLength(contentLength)
                                                                            .metadata(metadata)
                                                                            .contentType("application/zip")
                                                                            .cacheControl("max-age=86400")
                                                                            .contentDisposition("attachment")
                                                                            .contentEncoding("gzip")
                                                                            .contentLanguage("en-US")
                                                                            .expires(Instant.ofEpochSecond(1700000000L))
                                                                            .build()));
    }

    private void stubSuccessfulCreateMulipartCall() {
        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder()
                                                                                       .uploadId(MULTIPART_ID)
                                                                                       .build()));
    }

    private void stubSuccessfulUploadPartCopyCalls() {
        when(s3AsyncClient.uploadPartCopy(any(UploadPartCopyRequest.class)))
            .thenAnswer(new Answer<CompletableFuture<UploadPartCopyResponse>>() {
                int numberOfCalls = 0;

                @Override
                public CompletableFuture<UploadPartCopyResponse> answer(InvocationOnMock invocationOnMock) {
                    numberOfCalls++;
                    return CompletableFuture.completedFuture(UploadPartCopyResponse.builder()
                                                                .copyPartResult(CopyPartResult.builder()
                                                                                              .checksumCRC32("crc" + numberOfCalls)
                                                                                              .build())
                                                                .build());
                }
            });
    }

    private void stubSuccessfulCompleteMultipartCall() {
        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder()
                                                                                         .bucket(DESTINATION_BUCKET)
                                                                                         .key(DESTINATION_KEY)
                                                                                         .build()));
    }

    private void stubSuccessfulCompleteMultipartCallWithVersion(String versionId, String eTag) {
        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder()
                                                                                         .bucket(DESTINATION_BUCKET)
                                                                                         .key(DESTINATION_KEY)
                                                                                         .versionId(versionId)
                                                                                         .eTag(eTag)
                                                                                         .build()));
    }

    private static Stream<Arguments> metadataDirectiveCopyProvider() {
        return Stream.of(
            Arguments.of(MetadataDirective.COPY),
            Arguments.of((MetadataDirective) null)
        );
    }
}
