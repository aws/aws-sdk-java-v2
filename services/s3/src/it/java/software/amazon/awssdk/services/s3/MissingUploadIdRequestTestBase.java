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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Base classes to test S3Client and S3AsyncClient upload multiple part functions.
 */
public abstract class MissingUploadIdRequestTestBase extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(AsyncMissingUploadIdRequestIntegrationTest.class);
    private static final String CONTENT = RandomStringUtils.randomAscii(1000);

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void completeMultipartUpload_requestMissingUploadId_throwsException() throws Exception {
        String key = "completeMultipartUpload_requestMissingUploadId_throwsException";

        // 1. Initiate multipartUpload request
        String uploadId = initiateMultipartUpload(key);

        int partCount = 1;
        Collection<String> contentsToUpload = new ArrayList<>();

        // 2. Upload each part
        List<UploadPartResponse> uploadPartResponses = uploadParts(key, uploadId, partCount, contentsToUpload);

        Collection<CompletedPart> completedParts = new ArrayList<>();

        for (int i = 0; i < uploadPartResponses.size(); i++) {
            int partNumber = i + 1;
            UploadPartResponse response = uploadPartResponses.get(i);
            completedParts.add(CompletedPart.builder().eTag(response.eTag()).partNumber(partNumber).build());
        }

        // 3. Complete multipart upload
        Callable<CompleteMultipartUploadResponse> completeMultipartUploadRequestCallable =
            completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                                                                  .bucket(BUCKET)
                                                                  .key(key)
                                                                  .uploadId(null)
                                                                  .multipartUpload(CompletedMultipartUpload.builder()
                                                                                                           .parts(completedParts)
                                                                                                           .build()).build());

        assertThatThrownBy(completeMultipartUploadRequestCallable::call).isInstanceOf(expectedException());
    }

    @Test
    public void abortMultipartUpload_requestMissingUploadId_throwsException() throws Exception {
        String key = "abortMultipartUpload_requestMissingUploadId_throwsException";

        // 1. Initiate multipartUpload request
        String uploadId = initiateMultipartUpload(key);
        int partCount = 3;

        // 2. Upload each part
        Collection<String> contentsToUpload = new ArrayList<>();
        uploadParts(key, uploadId, partCount, contentsToUpload);

        // 3. abort the multipart upload
        Callable<AbortMultipartUploadResponse> abortMultipartUploadRequestCallable =
            abortMultipartUploadResponseCallable(AbortMultipartUploadRequest.builder()
                                                                            .bucket(BUCKET)
                                                                            .key(key)
                                                                            .uploadId(null)
                                                                            .build());

        assertThatThrownBy(abortMultipartUploadRequestCallable::call).isInstanceOf(expectedException());
    }

    @Test
    public void uploadPart_requestMissingUploadId_throwsException() throws Exception {
        String key = "uploadPart_requestMissingUploadId_throwsException";

        // 1. Initiate multipartUpload request
        initiateMultipartUpload(key);

        int partCount = 1;
        Collection<String> contentsToUpload = new ArrayList<>();

        // 2. Upload each part
        assertThatThrownBy(() -> uploadParts(key, null, partCount, contentsToUpload))
            .isInstanceOf(expectedException());
    }

    @Test
    public void listParts_requestMissingUploadId_throwsException() throws Exception {
        String key = "listParts_requestMissingUploadId_throwsException";

        // 1. Initiate multipartUpload request
        String uploadId = initiateMultipartUpload(key);

        int partCount = 1;
        Collection<String> contentsToUpload = new ArrayList<>();

        // 2. Upload each part
        List<UploadPartResponse> uploadPartResponses = uploadParts(key, uploadId, partCount, contentsToUpload);

        Collection<CompletedPart> completedParts = new ArrayList<>();

        for (int i = 0; i < uploadPartResponses.size(); i++) {
            int partNumber = i + 1;
            UploadPartResponse response = uploadPartResponses.get(i);
            completedParts.add(CompletedPart.builder().eTag(response.eTag()).partNumber(partNumber).build());
        }

        // 3. List uploaded parts
        Callable<ListPartsResponse> listPartsRequestCallable = listParts(ListPartsRequest.builder()
                                                                                         .bucket(BUCKET)
                                                                                         .key(key)
                                                                                         .uploadId(null)
                                                                                         .build());

        assertThatThrownBy(listPartsRequestCallable::call).isInstanceOf(expectedException());
    }

    private List<UploadPartResponse> uploadParts(String key, String uploadId, int partCount, Collection<String> contentsToUpload)
        throws Exception {
        List<UploadPartResponse> uploadPartResponses = new ArrayList<>();

        for (int i = 0; i < partCount; i++) {
            int partNumber = i + 1;
            contentsToUpload.add(CONTENT);
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder().bucket(BUCKET).key(key)
                                                                   .uploadId(uploadId)
                                                                   .partNumber(partNumber)
                                                                   .build();

            uploadPartResponses.add(uploadPart(uploadPartRequest, CONTENT).call());
        }
        return uploadPartResponses;
    }

    private String initiateMultipartUpload(String key) throws Exception {
        return createMultipartUpload(BUCKET, key).call().uploadId();
    }


    public abstract Callable<CreateMultipartUploadResponse> createMultipartUpload(String bucket, String key);

    public abstract Callable<UploadPartResponse> uploadPart(UploadPartRequest request, String requestBody);

    public abstract Callable<ListMultipartUploadsResponse> listMultipartUploads(String bucket);

    public abstract Callable<ListPartsResponse> listParts(ListPartsRequest request);

    public abstract Callable<CompleteMultipartUploadResponse> completeMultipartUpload(CompleteMultipartUploadRequest request);

    public abstract Callable<AbortMultipartUploadResponse> abortMultipartUploadResponseCallable(AbortMultipartUploadRequest request);

    public abstract Class<? extends Exception> expectedException();
}
