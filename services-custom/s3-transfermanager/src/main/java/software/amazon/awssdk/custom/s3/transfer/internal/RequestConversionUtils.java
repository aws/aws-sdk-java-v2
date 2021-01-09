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

package software.amazon.awssdk.custom.s3.transfer.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Utility class for converting between similar S3 requests. E.g. GetObjectRequest -> HeadObjectRequest.
 */
//TODO: This can be replaced by code generation, which will also be more reliable
@SdkInternalApi
final class RequestConversionUtils {
    private RequestConversionUtils() {
    }

    /**
     * Convert {@link GetObjectRequest} to a {@link HeadObjectRequest}.
     */
    static HeadObjectRequest toHeadObjectRequest(GetObjectRequest getObjectRequest) {
        return HeadObjectRequest.builder()
                                .bucket(getObjectRequest.bucket())
                                .ifMatch(getObjectRequest.ifMatch())
                                .ifModifiedSince(getObjectRequest.ifModifiedSince())
                                .ifNoneMatch(getObjectRequest.ifNoneMatch())
                                .ifUnmodifiedSince(getObjectRequest.ifUnmodifiedSince())
                                .key(getObjectRequest.key())
                                .expectedBucketOwner(getObjectRequest.expectedBucketOwner())
                                .partNumber(getObjectRequest.partNumber())
                                .range(getObjectRequest.range())
                                .requestPayer(getObjectRequest.requestPayerAsString())
                                .sseCustomerAlgorithm(getObjectRequest.sseCustomerAlgorithm())
                                .sseCustomerKey(getObjectRequest.sseCustomerKey())
                                .sseCustomerKeyMD5(getObjectRequest.sseCustomerKeyMD5())
                                .versionId(getObjectRequest.versionId())
                                .build();
    }


    static CreateMultipartUploadRequest toCreateMultipartUploadRequest(PutObjectRequest putObjectRequest) {

        return CreateMultipartUploadRequest.builder()
                                           .bucket(putObjectRequest.bucket())
                                           .key(putObjectRequest.key())
                                           .sseCustomerAlgorithm(putObjectRequest.sseCustomerAlgorithm())
                                           .sseCustomerKey(putObjectRequest.sseCustomerKey())
                                           .sseCustomerKeyMD5(putObjectRequest.sseCustomerKeyMD5())
                                           .requestPayer(putObjectRequest.requestPayer())
                                           .acl(putObjectRequest.acl())
                                           .cacheControl(putObjectRequest.cacheControl())
                                           .metadata(putObjectRequest.metadata())
                                           .contentDisposition(putObjectRequest.contentDisposition())
                                           .contentEncoding(putObjectRequest.contentEncoding())
                                           .contentType(putObjectRequest.contentType())
                                           .contentLanguage(putObjectRequest.contentLanguage())
                                           .grantFullControl(putObjectRequest.grantFullControl())
                                           .expires(putObjectRequest.expires())
                                           .grantRead(putObjectRequest.grantRead())
                                           .grantFullControl(putObjectRequest.grantFullControl())
                                           .grantReadACP(putObjectRequest.grantReadACP())
                                           .grantWriteACP(putObjectRequest.grantWriteACP())
                                           //TODO filter out headers
                                           //.overrideConfiguration(putObjectRequest.overrideConfiguration())
                                           .build();
    }

    static UploadPartRequest toUploadPartRequest(PutObjectRequest putObjectRequest,
                                                 long size,
                                                 String uploadId,
                                                 Integer partNumber) {

        return UploadPartRequest.builder()
                                .bucket(putObjectRequest.bucket())
                                .key(putObjectRequest.key())
                                .contentLength(size)
                                .sseCustomerAlgorithm(putObjectRequest.sseCustomerAlgorithm())
                                .sseCustomerKey(putObjectRequest.sseCustomerKey())
                                .sseCustomerKeyMD5(putObjectRequest.sseCustomerKeyMD5())
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .requestPayer(putObjectRequest.requestPayer())
                                //.overrideConfiguration(putObjectRequest.overrideConfiguration())
                                .build();
    }

    static CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(PutObjectRequest putObjectRequest,
                                                                           String uploadId,
                                                                           CompletedPart... parts) {
        return CompleteMultipartUploadRequest.builder()
                                             .bucket(putObjectRequest.bucket())
                                             .key(putObjectRequest.key())
                                             .multipartUpload(CompletedMultipartUpload.builder()
                                                                                      .parts(parts)
                                                                                      .build())
                                             .requestPayer(putObjectRequest.requestPayer())
                                             .uploadId(uploadId)
                                             .build();

    }

    static AbortMultipartUploadRequest toAbortPartRequest(PutObjectRequest putObjectRequest,
                                                          String uploadId) {
        return AbortMultipartUploadRequest.builder()
                                          .bucket(putObjectRequest.bucket())
                                          .key(putObjectRequest.key())
                                          .uploadId(uploadId)
                                          .requestPayer(putObjectRequest.requestPayer())
                                          .build();

    }
}
