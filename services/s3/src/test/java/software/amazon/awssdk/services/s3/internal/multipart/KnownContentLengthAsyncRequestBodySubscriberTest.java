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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
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
    }

    @Test
    void pause_withOngoingCompleteMpuFuture_shouldReturnTokenAndCancelFuture() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture = new CompletableFuture<>();
        int numExistingParts = 2;
        S3ResumeToken resumeToken = configureSubscriberAndPause(numExistingParts, completeMpuFuture);

        verifyResumeToken(resumeToken, numExistingParts);
        assertThat(completeMpuFuture).isCancelled();
    }

    @Test
    void pause_withCompletedCompleteMpuFuture_shouldReturnNullToken() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture =
            CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder().build());
        int numExistingParts = 2;
        S3ResumeToken resumeToken = configureSubscriberAndPause(numExistingParts, completeMpuFuture);

        assertThat(resumeToken).isNull();
    }

    @Test
    void pause_withUninitiatedCompleteMpuFuture_shouldReturnToken() {
        CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture = null;
        int numExistingParts = 2;
        S3ResumeToken resumeToken = configureSubscriberAndPause(numExistingParts, completeMpuFuture);

        verifyResumeToken(resumeToken, numExistingParts);
    }

    private S3ResumeToken configureSubscriberAndPause(int numExistingParts,
                                                      CompletableFuture<CompleteMultipartUploadResponse> completeMpuFuture) {
        Map<Integer, CompletedPart> existingParts = existingParts(numExistingParts);
        KnownContentLengthAsyncRequestBodySubscriber subscriber = subscriber(putObjectRequest, asyncRequestBody, existingParts,
                                                                             new CompletableFuture<>());

        when(multipartUploadHelper.completeMultipartUpload(any(CompletableFuture.class), any(String.class),
                                                           any(CompletedPart[].class), any(PutObjectRequest.class),
                                                           any(Long.class)))
            .thenReturn(completeMpuFuture);
        subscriber.onComplete();
        return subscriber.pause();
    }

    private KnownContentLengthAsyncRequestBodySubscriber subscriber(PutObjectRequest putObjectRequest,
                                                                    AsyncRequestBody asyncRequestBody,
                                                                    Map<Integer, CompletedPart> existingParts,
                                                                    CompletableFuture<PutObjectResponse> returnFuture) {

        MpuRequestContext mpuRequestContext = MpuRequestContext.builder()
                                                               .request(Pair.of(putObjectRequest, asyncRequestBody))
                                                               .contentLength(MPU_CONTENT_SIZE)
                                                               .partSize(PART_SIZE)
                                                               .uploadId(UPLOAD_ID)
                                                               .existingParts(existingParts)
                                                               .numPartsCompleted((long) existingParts.size())
                                                               .build();

        return new KnownContentLengthAsyncRequestBodySubscriber(mpuRequestContext, returnFuture, multipartUploadHelper);
    }

    private Map<Integer, CompletedPart> existingParts(int numExistingParts) {
        Map<Integer, CompletedPart> existingParts = new ConcurrentHashMap<>();
        for (int i = 1; i <= numExistingParts; i++) {
            existingParts.put(i, CompletedPart.builder().partNumber(i).build());
        }
        return existingParts;
    }

    private void verifyResumeToken(S3ResumeToken s3ResumeToken, int numExistingParts) {
        assertThat(s3ResumeToken).isNotNull();
        assertThat(s3ResumeToken.uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(s3ResumeToken.partSize()).isEqualTo(PART_SIZE);
        assertThat(s3ResumeToken.totalNumParts()).isEqualTo(TOTAL_NUM_PARTS);
        assertThat(s3ResumeToken.numPartsCompleted()).isEqualTo(numExistingParts);
    }
}
