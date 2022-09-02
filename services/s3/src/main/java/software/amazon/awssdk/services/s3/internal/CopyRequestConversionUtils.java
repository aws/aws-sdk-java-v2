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

package software.amazon.awssdk.services.s3.internal;

import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

public final class CopyRequestConversionUtils {

    private CopyRequestConversionUtils() {
    }

    public static HeadObjectRequest toHeadObjectRequest(CopyObjectRequest copyObjectRequest) {
        return HeadObjectRequest.builder()
                                .bucket(copyObjectRequest.sourceBucket())
                                .key(copyObjectRequest.sourceKey())
                                .versionId(copyObjectRequest.sourceVersionId())
                                .build();
    }

    public static CompletedPart toCompletedPart(CopyPartResult copyPartResult, int partNumber) {
        return CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(copyPartResult.eTag())
                            .checksumCRC32C(copyPartResult.checksumCRC32C())
                            .checksumCRC32(copyPartResult.checksumCRC32())
                            .checksumSHA1(copyPartResult.checksumSHA1())
                            .checksumSHA256(copyPartResult.checksumSHA256())
                            .eTag(copyPartResult.eTag())
                            .build();
    }

    public static CreateMultipartUploadRequest toCreateMultipartUploadRequest(CopyObjectRequest copyObjectRequest) {
        return CreateMultipartUploadRequest.builder()
                                           .bucket(copyObjectRequest.destinationBucket())
                                           .key(copyObjectRequest.destinationKey())
                                           .build();
    }

    public static CopyObjectResponse toCopyObjectResponse(CompleteMultipartUploadResponse response) {
        CopyObjectResponse.Builder builder = CopyObjectResponse.builder()
                                                               .versionId(response.versionId())
                                                               .copyObjectResult(CopyObjectResult.builder()
                                                                                                 .checksumCRC32(response.checksumCRC32())
                                                                                                 .checksumSHA1(response.checksumSHA1())
                                                                                                 .checksumSHA256(response.checksumSHA256())
                                                                                                 .checksumCRC32C(response.checksumCRC32C())
                                                                                                 .eTag(response.eTag())
                                                                                                 .build())
                                                               .expiration(response.expiration())
                                                               .requestCharged(response.requestCharged());
        return (CopyObjectResponse) builder.responseMetadata(response.responseMetadata())
                                           .sdkHttpResponse(response.sdkHttpResponse())
                                           .build();
    }

    public static AbortMultipartUploadRequest toAbortMultipartUploadRequest(CopyObjectRequest copyObjectRequest,
                                                                            String uploadId) {
        return AbortMultipartUploadRequest.builder()
                                          .uploadId(uploadId)
                                          .bucket(copyObjectRequest.destinationBucket())
                                          .key(copyObjectRequest.destinationKey())
                                          .requestPayer(copyObjectRequest.requestPayer())
                                          .expectedBucketOwner(copyObjectRequest.expectedBucketOwner())
                                          .build();
    }

}
