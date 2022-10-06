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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;

/**
 * Request conversion utility method for POJO classes associated with {@link S3CrtAsyncClient#copyObject(CopyObjectRequest)}
 */
@SdkInternalApi
public final class CopyRequestConversionUtils {

    private CopyRequestConversionUtils() {
    }

    public static HeadObjectRequest toHeadObjectRequest(CopyObjectRequest copyObjectRequest) {
        return HeadObjectRequest.builder()
                                .bucket(copyObjectRequest.sourceBucket())
                                .key(copyObjectRequest.sourceKey())
                                .versionId(copyObjectRequest.sourceVersionId())
                                .ifMatch(copyObjectRequest.copySourceIfMatch())
                                .ifModifiedSince(copyObjectRequest.copySourceIfModifiedSince())
                                .ifNoneMatch(copyObjectRequest.copySourceIfNoneMatch())
                                .ifUnmodifiedSince(copyObjectRequest.copySourceIfUnmodifiedSince())
                                .expectedBucketOwner(copyObjectRequest.expectedSourceBucketOwner())
                                .sseCustomerAlgorithm(copyObjectRequest.copySourceSSECustomerAlgorithm())
                                .sseCustomerKey(copyObjectRequest.copySourceSSECustomerKey())
                                .sseCustomerKeyMD5(copyObjectRequest.copySourceSSECustomerKeyMD5())
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
                                           .contentEncoding(copyObjectRequest.contentEncoding())
                                           .checksumAlgorithm(copyObjectRequest.checksumAlgorithmAsString())
                                           .tagging(copyObjectRequest.tagging())
                                           .contentType(copyObjectRequest.contentType())
                                           .contentLanguage(copyObjectRequest.contentLanguage())
                                           .contentDisposition(copyObjectRequest.contentDisposition())
                                           .cacheControl(copyObjectRequest.cacheControl())
                                           .expires(copyObjectRequest.expires())
                                           .key(copyObjectRequest.destinationKey())
                                           .websiteRedirectLocation(copyObjectRequest.websiteRedirectLocation())
                                           .expectedBucketOwner(copyObjectRequest.expectedBucketOwner())
                                           .requestPayer(copyObjectRequest.requestPayerAsString())
                                           .acl(copyObjectRequest.aclAsString())
                                           .grantRead(copyObjectRequest.grantRead())
                                           .grantReadACP(copyObjectRequest.grantReadACP())
                                           .grantWriteACP(copyObjectRequest.grantWriteACP())
                                           .grantFullControl(copyObjectRequest.grantFullControl())
                                           .storageClass(copyObjectRequest.storageClassAsString())
                                           .ssekmsKeyId(copyObjectRequest.ssekmsKeyId())
                                           .sseCustomerKey(copyObjectRequest.sseCustomerKey())
                                           .sseCustomerAlgorithm(copyObjectRequest.sseCustomerAlgorithm())
                                           .sseCustomerKeyMD5(copyObjectRequest.sseCustomerKeyMD5())
                                           .ssekmsEncryptionContext(copyObjectRequest.ssekmsEncryptionContext())
                                           .serverSideEncryption(copyObjectRequest.serverSideEncryptionAsString())
                                           .bucketKeyEnabled(copyObjectRequest.bucketKeyEnabled())
                                           .objectLockMode(copyObjectRequest.objectLockModeAsString())
                                           .objectLockLegalHoldStatus(copyObjectRequest.objectLockLegalHoldStatusAsString())
                                           .objectLockRetainUntilDate(copyObjectRequest.objectLockRetainUntilDate())
                                           .metadata(copyObjectRequest.metadata())
                                           .build();
    }

    public static CopyObjectResponse toCopyObjectResponse(CompleteMultipartUploadResponse response) {
        CopyObjectResponse.Builder builder = CopyObjectResponse.builder()
                                                               .versionId(response.versionId())
                                                               .copyObjectResult(b -> b.checksumCRC32(response.checksumCRC32())
                                                                                       .checksumSHA1(response.checksumSHA1())
                                                                                       .checksumSHA256(response.checksumSHA256())
                                                                                       .checksumCRC32C(response.checksumCRC32C())
                                                                                       .eTag(response.eTag())
                                                                                       .build())
                                                               .expiration(response.expiration())
                                                               .bucketKeyEnabled(response.bucketKeyEnabled())
                                                               .serverSideEncryption(response.serverSideEncryption())
                                                               .ssekmsKeyId(response.ssekmsKeyId())
                                                               .serverSideEncryption(response.serverSideEncryptionAsString())
                                                               .requestCharged(response.requestChargedAsString());
        if (response.responseMetadata() != null) {
            builder.responseMetadata(response.responseMetadata());
        }

        if (response.sdkHttpResponse() != null) {
            builder.sdkHttpResponse(response.sdkHttpResponse());
        }

        return builder.build();
    }

    public static AbortMultipartUploadRequest toAbortMultipartUploadRequest(CopyObjectRequest copyObjectRequest,
                                                                            String uploadId) {
        return AbortMultipartUploadRequest.builder()
                                          .uploadId(uploadId)
                                          .bucket(copyObjectRequest.destinationBucket())
                                          .key(copyObjectRequest.destinationKey())
                                          .requestPayer(copyObjectRequest.requestPayerAsString())
                                          .expectedBucketOwner(copyObjectRequest.expectedBucketOwner())
                                          .build();
    }

    public static UploadPartCopyRequest toUploadPartCopyRequest(CopyObjectRequest copyObjectRequest,
                                                                int partNumber,
                                                                String uploadId,
                                                                String range) {

        return UploadPartCopyRequest.builder()
                                    .sourceBucket(copyObjectRequest.sourceBucket())
                                    .sourceKey(copyObjectRequest.sourceKey())
                                    .sourceVersionId(copyObjectRequest.sourceVersionId())
                                    .uploadId(uploadId)
                                    .partNumber(partNumber)
                                    .destinationBucket(copyObjectRequest.destinationBucket())
                                    .destinationKey(copyObjectRequest.destinationKey())
                                    .copySourceIfMatch(copyObjectRequest.copySourceIfMatch())
                                    .copySourceIfNoneMatch(copyObjectRequest.copySourceIfNoneMatch())
                                    .copySourceIfUnmodifiedSince(copyObjectRequest.copySourceIfUnmodifiedSince())
                                    .copySourceRange(range)
                                    .copySourceSSECustomerAlgorithm(copyObjectRequest.copySourceSSECustomerAlgorithm())
                                    .copySourceSSECustomerKeyMD5(copyObjectRequest.copySourceSSECustomerKeyMD5())
                                    .copySourceSSECustomerKey(copyObjectRequest.copySourceSSECustomerKey())
                                    .copySourceIfModifiedSince(copyObjectRequest.copySourceIfModifiedSince())
                                    .expectedBucketOwner(copyObjectRequest.expectedBucketOwner())
                                    .expectedSourceBucketOwner(copyObjectRequest.expectedSourceBucketOwner())
                                    .requestPayer(copyObjectRequest.requestPayerAsString())
                                    .sseCustomerKey(copyObjectRequest.sseCustomerKey())
                                    .sseCustomerAlgorithm(copyObjectRequest.sseCustomerAlgorithm())
                                    .sseCustomerKeyMD5(copyObjectRequest.sseCustomerKeyMD5())
                                    .build();
    }

}
