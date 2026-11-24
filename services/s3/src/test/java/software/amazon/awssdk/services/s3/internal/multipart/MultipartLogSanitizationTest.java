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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.s3ResumeToken;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.stubSuccessfulCompleteMultipartCall;
import static software.amazon.awssdk.services.s3.internal.multipart.utils.MultipartUploadTestUtils.stubSuccessfulUploadPartCalls;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.PAUSE_OBSERVABLE;
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.RESUME_TOKEN;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AnnotationDirective;
import software.amazon.awssdk.services.s3.model.AnnotationEntry;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAnnotationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectAnnotationsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectAnnotationsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.multipart.PauseObservable;
import software.amazon.awssdk.services.s3.multipart.S3ResumeToken;
import software.amazon.awssdk.services.s3.paginators.ListObjectAnnotationsPublisher;
import software.amazon.awssdk.services.s3.paginators.ListPartsPublisher;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.CompletableFutureUtils;

class MultipartLogSanitizationTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final long PART_SIZE = 8 * 1024;
    private static final long MPU_CONTENT_SIZE = 25 * 1024;
    private static final String MULTIPART_LOGGER_PREFIX = "software.amazon.awssdk.services.s3.internal.multipart";
    private static final String COPY_LOGGER = "software.amazon.awssdk.services.s3.S3AsyncClient";
    private static final String PAYLOAD = "aBc\r\n2026-08-29 03:14:15,123 INFO forged.Logger - forged";
    private static final String ESCAPED_PAYLOAD = "aBc\\r\\n2026-08-29 03:14:15,123 INFO forged.Logger - forged";

    private S3AsyncClient s3AsyncClient;

    @BeforeEach
    void setUp() {
        s3AsyncClient = mock(S3AsyncClient.class);
    }

    @Test
    void resumeUpload_whenUploadIdContainsLineBreaks_logsEscapedUploadId() {
        S3ResumeToken resumeToken = s3ResumeToken(1, PART_SIZE, MPU_CONTENT_SIZE, PAYLOAD);
        PutObjectRequest putObjectRequest =
            PutObjectRequest.builder()
                            .bucket(BUCKET)
                            .key(KEY)
                            .contentLength(MPU_CONTENT_SIZE)
                            .overrideConfiguration(o -> o.putExecutionAttribute(RESUME_TOKEN, resumeToken)
                                                         .putExecutionAttribute(PAUSE_OBSERVABLE, new PauseObservable()))
                            .build();
        ListPartsPublisher mockPublisher = mock(ListPartsPublisher.class);
        when(s3AsyncClient.listPartsPaginator(any(ListPartsRequest.class))).thenReturn(mockPublisher);
        when(mockPublisher.parts()).thenReturn(SdkPublisher.fromIterable(
            Collections.singletonList(Part.builder().partNumber(1).build())));
        stubSuccessfulUploadPartCalls(s3AsyncClient);
        stubSuccessfulCompleteMultipartCall(BUCKET, KEY, s3AsyncClient);

        List<LogEvent> events;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            uploadObjectHelper().uploadObject(putObjectRequest, requestBody(MPU_CONTENT_SIZE)).join();
            events = logCaptor.loggedEvents();
        }

        assertThat(messages(events, MULTIPART_LOGGER_PREFIX))
            .contains("Resuming a paused multipart upload, uploadId: " + ESCAPED_PAYLOAD
                      + ", completedPartCount: 1, remainingPartCount: 3, partSize: " + PART_SIZE)
            .anySatisfy(message -> assertThat(message).startsWith("Sending uploadPartRequest:")
                                                      .contains("uploadId: " + ESCAPED_PAYLOAD));
        assertNoRawLineBreaks(events, MULTIPART_LOGGER_PREFIX);

        ArgumentCaptor<ListPartsRequest> listPartsCaptor = ArgumentCaptor.forClass(ListPartsRequest.class);
        verify(s3AsyncClient).listPartsPaginator(listPartsCaptor.capture());
        assertThat(listPartsCaptor.getValue().uploadId()).isEqualTo(PAYLOAD);
        ArgumentCaptor<UploadPartRequest> uploadPartCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        verify(s3AsyncClient, atLeastOnce()).uploadPart(uploadPartCaptor.capture(), any(AsyncRequestBody.class));
        assertThat(uploadPartCaptor.getAllValues()).isNotEmpty()
                                                   .allSatisfy(r -> assertThat(r.uploadId()).isEqualTo(PAYLOAD));
    }

    @Test
    void cleanUpParts_whenUploadIdAndFailureContainLineBreaks_logsEscapedValues() {
        when(s3AsyncClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(new RuntimeException("failure\nFORGED")));

        List<LogEvent> events;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            genericMultipartHelper().cleanUpParts(PAYLOAD, AbortMultipartUploadRequest.builder()
                                                                                      .bucket(BUCKET)
                                                                                      .key(KEY));
            events = logCaptor.loggedEvents();
        }

        assertThat(messages(events, MULTIPART_LOGGER_PREFIX))
            .contains("Aborting multipart upload: " + ESCAPED_PAYLOAD)
            .anySatisfy(message -> assertThat(message)
                .startsWith("Failed to abort previous multipart upload (id: " + ESCAPED_PAYLOAD + ")"));
        assertNoRawLineBreaks(events, MULTIPART_LOGGER_PREFIX);
        List<Throwable> throwables = events.stream()
                                           .map(LogEvent::getThrown)
                                           .filter(t -> t != null)
                                           .collect(Collectors.toList());
        assertThat(throwables).hasSize(1);
        assertThat(throwables.get(0).toString()).contains("failure\\nFORGED").doesNotContain("\n");

        ArgumentCaptor<AbortMultipartUploadRequest> abortCaptor =
            ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3AsyncClient).abortMultipartUpload(abortCaptor.capture());
        assertThat(abortCaptor.getValue().uploadId()).isEqualTo(PAYLOAD);
    }

    @Test
    void completeMultipartUpload_whenUploadIdContainsLineBreaks_logsEscapedUploadId() {
        stubSuccessfulCompleteMultipartCall(BUCKET, KEY, s3AsyncClient);
        CompletedPart[] parts = {CompletedPart.builder().partNumber(1).build()};

        List<LogEvent> events;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            genericMultipartHelper().completeMultipartUpload(PutObjectRequest.builder().bucket(BUCKET).key(KEY).build(),
                                                             PAYLOAD, parts, MPU_CONTENT_SIZE).join();
            events = logCaptor.loggedEvents();
        }

        assertThat(messages(events, MULTIPART_LOGGER_PREFIX))
            .contains("Sending completeMultipartUploadRequest, uploadId: " + ESCAPED_PAYLOAD);
        assertNoRawLineBreaks(events, MULTIPART_LOGGER_PREFIX);
        ArgumentCaptor<CompleteMultipartUploadRequest> captor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(s3AsyncClient).completeMultipartUpload(captor.capture());
        assertThat(captor.getValue().uploadId()).isEqualTo(PAYLOAD);
    }

    @Test
    void unknownContentLengthOnError_whenFailureContainsLineBreaks_logsEscapedThrowable() {
        UnknownContentLengthAsyncRequestBodySubscriber subscriber =
            new UnknownContentLengthAsyncRequestBodySubscriber(PART_SIZE,
                                                               PutObjectRequest.builder().bucket("bucket").key("key").build(),
                                                               new CompletableFuture<>(),
                                                               mock(MultipartUploadHelper.class),
                                                               genericMultipartHelper(),
                                                               1);

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            subscriber.onError(new IOException("body\r\nfailed"));

            assertThat(logCaptor.loggedEvents()).anySatisfy(event -> {
                assertThat(event.getMessage().getFormattedMessage()).contains("Received onError()");
                assertThat(event.getThrown()).hasToString("java.io.IOException: body\\r\\nfailed");
            });
        }
    }

    @Test
    void downloadObject_whenRangeContainsLineBreaks_logsEscapedRange() {
        when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(GetObjectResponse.builder().build()));
        GetObjectRequest request = GetObjectRequest.builder().bucket(BUCKET).key(KEY).range(PAYLOAD).build();

        List<LogEvent> events;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            new DownloadObjectHelper(s3AsyncClient, PART_SIZE, 1)
                .downloadObject(request, AsyncResponseTransformer.toBytes());
            events = logCaptor.loggedEvents();
        }

        assertThat(messages(events, MULTIPART_LOGGER_PREFIX))
            .contains("Using single part download because getObjectRequest range is included in the request. range = "
                      + ESCAPED_PAYLOAD);
        assertNoRawLineBreaks(events, MULTIPART_LOGGER_PREFIX);
        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3AsyncClient).getObject(captor.capture(), any(AsyncResponseTransformer.class));
        assertThat(captor.getValue().range()).isEqualTo(PAYLOAD);
    }

    @Test
    void copyObject_whenAnnotationNameContainsLineBreaks_logsEscapedName() {
        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                            .contentLength(4 * PART_SIZE)
                                                                            .versionId("src-version")
                                                                            .eTag("\"src-etag\"")
                                                                            .build()));
        when(s3AsyncClient.listObjectAnnotationsPaginator(any(Consumer.class)))
            .thenAnswer(invocation -> {
                Consumer<ListObjectAnnotationsRequest.Builder> consumer = invocation.getArgument(0);
                return new ListObjectAnnotationsPublisher(s3AsyncClient, ListObjectAnnotationsRequest.builder()
                                                                                                     .applyMutation(consumer)
                                                                                                     .build());
            });
        when(s3AsyncClient.listObjectAnnotations(any(ListObjectAnnotationsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(ListObjectAnnotationsResponse.builder()
                                                                                       .annotations(AnnotationEntry.builder()
                                                                                                                   .annotationName(PAYLOAD)
                                                                                                                   .build())
                                                                                       .build()));
        when(s3AsyncClient.getObjectAnnotation(any(Consumer.class), any(AsyncResponseTransformer.class)))
            .thenReturn(CompletableFuture.completedFuture(
                ResponseBytes.fromByteArray(GetObjectAnnotationResponse.builder().build(), "content".getBytes())));
        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder()
                                                                                       .uploadId("multipartId")
                                                                                       .build()));
        when(s3AsyncClient.uploadPartCopy(any(UploadPartCopyRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(UploadPartCopyResponse.builder()
                                                                              .copyPartResult(CopyPartResult.builder()
                                                                                                            .checksumCRC32("crc")
                                                                                                            .build())
                                                                              .build()));
        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder()
                                                                                         .bucket(BUCKET)
                                                                                         .key(KEY)
                                                                                         .versionId("dest-version")
                                                                                         .eTag("\"dest-etag\"")
                                                                                         .build()));
        S3Exception preconditionFailed = (S3Exception) S3Exception.builder()
                                                                  .statusCode(412)
                                                                  .message("Precondition Failed")
                                                                  .build();
        when(s3AsyncClient.putObjectAnnotation(any(Consumer.class), any(AsyncRequestBody.class)))
            .thenReturn(CompletableFutureUtils.failedFuture(preconditionFailed));
        CopyObjectRequest request = CopyObjectRequest.builder()
                                                     .sourceBucket(BUCKET)
                                                     .sourceKey(KEY)
                                                     .destinationBucket("destination")
                                                     .destinationKey("destinationKey")
                                                     .annotationDirective(AnnotationDirective.COPY)
                                                     .build();

        List<LogEvent> events;
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            CompletableFuture<CopyObjectResponse> future =
                new CopyObjectHelper(s3AsyncClient, PART_SIZE, PART_SIZE * 2).copyObject(request);
            assertThatThrownBy(future::join).hasCauseInstanceOf(SdkClientException.class);
            events = logCaptor.loggedEvents();
        }

        assertThat(messages(events, COPY_LOGGER))
            .anySatisfy(message -> assertThat(message).startsWith("Annotation copy partial failure.")
                                                      .contains("FAILED (1): [" + ESCAPED_PAYLOAD + "]"));
        assertNoRawLineBreaks(events, COPY_LOGGER);
    }

    private UploadObjectHelper uploadObjectHelper() {
        return new UploadObjectHelper(s3AsyncClient,
                                      new MultipartConfigurationResolver(
                                          MultipartConfiguration.builder()
                                                                .minimumPartSizeInBytes(PART_SIZE)
                                                                .thresholdInBytes(PART_SIZE * 2)
                                                                .build()));
    }

    private GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper() {
        return new GenericMultipartHelper<>(s3AsyncClient,
                                            SdkPojoConversionUtils::toAbortMultipartUploadRequest,
                                            SdkPojoConversionUtils::toPutObjectResponse);
    }

    private static AsyncRequestBody requestBody(long contentLength) {
        return AsyncRequestBody.fromBytes(new byte[Math.toIntExact(contentLength)]);
    }

    private static void assertNoRawLineBreaks(List<LogEvent> events, String loggerPrefix) {
        assertThat(messages(events, loggerPrefix)).isNotEmpty()
                                                  .allSatisfy(message -> assertThat(message).doesNotContain("\r", "\n"));
    }

    private static List<String> messages(List<LogEvent> events, String loggerPrefix) {
        return events.stream()
                     .filter(event -> event.getLoggerName() != null && event.getLoggerName().startsWith(loggerPrefix))
                     .map(event -> event.getMessage().getFormattedMessage())
                     .collect(Collectors.toList());
    }
}
