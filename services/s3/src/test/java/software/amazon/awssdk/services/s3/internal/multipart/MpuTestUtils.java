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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

public final class MpuTestUtils {

    private MpuTestUtils() {
    }

    public static void stubSuccessfulHeadObjectCall(long contentLength, S3AsyncClient s3AsyncClient) {
        CompletableFuture<HeadObjectResponse> headFuture =
            CompletableFuture.completedFuture(HeadObjectResponse.builder()
                                                                .contentLength(contentLength)
                                                                .build());

        when(s3AsyncClient.headObject(any(HeadObjectRequest.class)))
            .thenReturn(headFuture);
    }

    public static void stubSuccessfulCreateMultipartCall(String mpuId, S3AsyncClient s3AsyncClient) {
        CompletableFuture<CreateMultipartUploadResponse> createMultipartUploadFuture =
            CompletableFuture.completedFuture(CreateMultipartUploadResponse.builder()
                                                                           .uploadId(mpuId)
                                                                           .build());

        when(s3AsyncClient.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(createMultipartUploadFuture);
    }

    public static void stubSuccessfulCompleteMultipartCall(String bucket, String key, S3AsyncClient s3AsyncClient) {
        CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUploadFuture =
            CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder()
                                                                             .bucket(bucket)
                                                                             .key(key)
                                                                             .build());

        when(s3AsyncClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
            .thenReturn(completeMultipartUploadFuture);
    }
}
