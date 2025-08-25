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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ChecksumType;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectResult;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * Request conversion utility method for POJO classes associated with multipart feature.
 */
@SdkInternalApi
public final class SdkPojoConversionUtils {

    protected static final Set<String> PUT_OBJECT_REQUEST_TO_UPLOAD_PART_FIELDS_TO_IGNORE =
        new HashSet<>(Arrays.asList("ChecksumSHA1", "ChecksumSHA256", "ContentMD5", "ChecksumCRC32C", "ChecksumCRC32",
                                    "ChecksumCRC64NVME", "ContentLength"));

    private static final Set<String> PUT_OBJECT_TO_UPLOAD_PART_ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
        "ACL",
        "Bucket",
        "CacheControl",
        "ContentDisposition",
        "ContentEncoding",
        "ContentLanguage",
        "ContentLength",
        "ContentMD5",
        "ContentType",
        "ChecksumAlgorithm",
        "ChecksumCRC32",
        "ChecksumCRC32C",
        "ChecksumCRC64NVME",
        "ChecksumSHA1",
        "ChecksumSHA256",
        "Expires",
        "IfMatch",
        "IfNoneMatch",
        "GrantFullControl",
        "GrantRead",
        "GrantReadACP",
        "GrantWriteACP",
        "Key",
        "WriteOffsetBytes",
        "Metadata",
        "ServerSideEncryption",
        "StorageClass",
        "WebsiteRedirectLocation",
        "SSECustomerAlgorithm",
        "SSECustomerKey",
        "SSECustomerKeyMD5",
        "SSEKMSKeyId",
        "SSEKMSEncryptionContext",
        "BucketKeyEnabled",
        "RequestPayer",
        "Tagging",
        "ObjectLockMode",
        "ObjectLockRetainUntilDate",
        "ObjectLockLegalHoldStatus",
        "ExpectedBucketOwner"
    ));

    private static final Set<String> COPY_OBJECT_TO_COPY_OBJECT_ALLOWED_FIELDS = new HashSet<>(Arrays.asList(
        "ACL",
        "CacheControl",
        "ChecksumAlgorithm",
        "ContentDisposition",
        "ContentEncoding",
        "ContentLanguage",
        "ContentType",
        "CopySource",
        "CopySourceIfMatch",
        "CopySourceIfModifiedSince",
        "CopySourceIfNoneMatch",
        "CopySourceIfUnmodifiedSince",
        "CopySourceSSECustomerAlgorithm",
        "CopySourceSSECustomerKey",
        "CopySourceSSECustomerKeyMD5",
        "DestinationBucket",
        "DestinationKey",
        "Expires",
        "GrantFullControl",
        "GrantRead",
        "GrantReadACP",
        "GrantWriteACP",
        "Metadata",
        "MetadataDirective",
        "TaggingDirective",
        "ServerSideEncryption",
        "SourceBucket",
        "SourceKey",
        "SourceVersionId",
        "StorageClass",
        "WebsiteRedirectLocation",
        "SSECustomerAlgorithm",
        "SSECustomerKey",
        "SSECustomerKeyMD5",
        "SSEKMSKeyId",
        "SSEKMSEncryptionContext",
        "BucketKeyEnabled",
        "RequestPayer",
        "Tagging",
        "ObjectLockMode",
        "ObjectLockRetainUntilDate",
        "ObjectLockLegalHoldStatus",
        "ExpectedBucketOwner",
        "ExpectedSourceBucketOwner"
    ));


    private SdkPojoConversionUtils() {
    }

    public static UploadPartRequest toUploadPartRequest(PutObjectRequest putObjectRequest, int partNumber, String uploadId) {

        UploadPartRequest.Builder builder = UploadPartRequest.builder();
        validateRequestFields(putObjectRequest, builder, PUT_OBJECT_TO_UPLOAD_PART_ALLOWED_FIELDS);
        setSdkFields(builder, putObjectRequest, PUT_OBJECT_REQUEST_TO_UPLOAD_PART_FIELDS_TO_IGNORE);
        return builder.uploadId(uploadId).partNumber(partNumber).build();
    }

    public static CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(PutObjectRequest putObjectRequest,
                                                                                  String uploadId, CompletedPart[] parts,
                                                                                  long contentLength) {
        CompleteMultipartUploadRequest.Builder builder = CompleteMultipartUploadRequest.builder();
        validateRequestFields(putObjectRequest, builder, PUT_OBJECT_TO_UPLOAD_PART_ALLOWED_FIELDS);
        setSdkFields(builder, putObjectRequest);

        builder.mpuObjectSize(contentLength);

        if (S3ChecksumUtils.checksumValueSpecified(putObjectRequest)) {
            builder.checksumType(ChecksumType.FULL_OBJECT);
        }

        return builder.uploadId(uploadId).multipartUpload(c -> c.parts(parts)).build();
    }

    public static CreateMultipartUploadRequest toCreateMultipartUploadRequest(PutObjectRequest putObjectRequest) {

        CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder();
        validateRequestFields(putObjectRequest, builder, PUT_OBJECT_TO_UPLOAD_PART_ALLOWED_FIELDS);
        setSdkFields(builder, putObjectRequest);

        if (S3ChecksumUtils.checksumValueSpecified(putObjectRequest)) {
            builder.checksumType(ChecksumType.FULL_OBJECT);
        }

        return builder.build();
    }

    public static HeadObjectRequest toHeadObjectRequest(CopyObjectRequest copyObjectRequest) {

        // We can't set SdkFields directly because the fields in CopyObjectRequest do not match 100% with the ones in
        // HeadObjectRequest
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
        CompletedPart.Builder builder = CompletedPart.builder();

        setSdkFields(builder, copyPartResult);
        return builder.partNumber(partNumber).build();
    }

    public static CompletedPart toCompletedPart(UploadPartResponse partResponse, int partNumber) {
        CompletedPart.Builder builder = CompletedPart.builder();
        setSdkFields(builder, partResponse);
        return builder.partNumber(partNumber).build();
    }

    public static CompletedPart toCompletedPart(Part part) {
        CompletedPart.Builder builder = CompletedPart.builder();
        setSdkFields(builder, part);
        return builder.build();
    }

    public static ListPartsRequest toListPartsRequest(String uploadId, PutObjectRequest putObjectRequest) {
        ListPartsRequest.Builder builder = ListPartsRequest.builder();
        validateRequestFields(putObjectRequest, builder, PUT_OBJECT_TO_UPLOAD_PART_ALLOWED_FIELDS);
        setSdkFields(builder, putObjectRequest);
        return builder.uploadId(uploadId).build();
    }

    public static CreateMultipartUploadRequest toCreateMultipartUploadRequest(CopyObjectRequest copyObjectRequest) {
        CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder();
        validateRequestFields(copyObjectRequest, builder, COPY_OBJECT_TO_COPY_OBJECT_ALLOWED_FIELDS);
        setSdkFields(builder, copyObjectRequest);
        builder.bucket(copyObjectRequest.destinationBucket());
        builder.key(copyObjectRequest.destinationKey());
        return builder.build();
    }

    public static CopyObjectResponse toCopyObjectResponse(CompleteMultipartUploadResponse response) {
        CopyObjectResponse.Builder builder = CopyObjectResponse.builder();

        setSdkFields(builder, response);

        builder.responseMetadata(response.responseMetadata());
        builder.sdkHttpResponse(response.sdkHttpResponse());

        return builder.copyObjectResult(toCopyObjectResult(response))
                      .build();
    }

    private static CopyObjectResult toCopyObjectResult(CompleteMultipartUploadResponse response) {
        CopyObjectResult.Builder builder = CopyObjectResult.builder();

        setSdkFields(builder, response);
        return builder.build();
    }

    public static AbortMultipartUploadRequest.Builder toAbortMultipartUploadRequest(CopyObjectRequest copyObjectRequest) {
        AbortMultipartUploadRequest.Builder builder = AbortMultipartUploadRequest.builder();
        validateRequestFields(copyObjectRequest, builder, COPY_OBJECT_TO_COPY_OBJECT_ALLOWED_FIELDS);
        setSdkFields(builder, copyObjectRequest);
        builder.bucket(copyObjectRequest.destinationBucket());
        builder.key(copyObjectRequest.destinationKey());
        return builder;
    }

    public static AbortMultipartUploadRequest.Builder toAbortMultipartUploadRequest(PutObjectRequest putObjectRequest) {
        AbortMultipartUploadRequest.Builder builder = AbortMultipartUploadRequest.builder();
        validateRequestFields(putObjectRequest, builder, PUT_OBJECT_TO_UPLOAD_PART_ALLOWED_FIELDS);
        setSdkFields(builder, putObjectRequest);
        return builder;
    }

    public static UploadPartCopyRequest toUploadPartCopyRequest(CopyObjectRequest copyObjectRequest,
                                                                int partNumber,
                                                                String uploadId,
                                                                String range) {
        UploadPartCopyRequest.Builder builder = UploadPartCopyRequest.builder();
        setSdkFields(builder, copyObjectRequest);
        return builder.copySourceRange(range)
                      .partNumber(partNumber)
                      .uploadId(uploadId)
                      .bucket(copyObjectRequest.destinationBucket())
                      .key(copyObjectRequest.destinationKey())
                      .build();
    }

    public static PutObjectResponse toPutObjectResponse(CompleteMultipartUploadResponse response) {

        PutObjectResponse.Builder builder = PutObjectResponse.builder();

        setSdkFields(builder, response);

        builder.responseMetadata(response.responseMetadata());
        builder.sdkHttpResponse(response.sdkHttpResponse());

        return builder.build();
    }

    private static Map<String, Object> retrieveSdkFields(SdkPojo sourceObject, List<SdkField<?>> sdkFields) {
        return sdkFields.stream().collect(
            HashMap::new,
            (map, field) -> map.put(field.memberName(),
                                    field.getValueOrDefault(sourceObject)),
            Map::putAll);
    }

    private static void setSdkFields(SdkPojo targetBuilder, SdkPojo sourceObject) {
        setSdkFields(targetBuilder, sourceObject, new HashSet<>());
    }

    private static void setSdkFields(SdkPojo targetBuilder, SdkPojo sourceObject, Set<String> fieldsToIgnore) {
        Map<String, Object> sourceFields = retrieveSdkFields(sourceObject, sourceObject.sdkFields());
        List<SdkField<?>> targetSdkFields = targetBuilder.sdkFields();

        for (SdkField<?> field : targetSdkFields) {
            if (fieldsToIgnore.contains(field.memberName())) {
                continue;
            }
            field.set(targetBuilder, sourceFields.getOrDefault(field.memberName(), null));
        }
    }

    private static void validateRequestFields(SdkPojo sourceObject, SdkPojo targetBuilder, Set<String> allowedFields) {
        Set<String> invalidFields = new HashSet<>();

        for (SdkField<?> sourceField : sourceObject.sdkFields()) {
            String fieldName = sourceField.memberName();
            Object sourceValue = sourceField.getValueOrDefault(sourceObject);

            if (!allowedFields.contains(fieldName)) {
                SdkField<?> targetField = targetBuilder.sdkFields()
                                                       .stream()
                                                       .filter(field -> field.memberName().equals(fieldName))
                                                       .findFirst()
                                                       .orElse(null);
                if (targetField != null && !targetField.getValueOrDefault(targetBuilder).equals(sourceValue)) {
                    invalidFields.add(fieldName);
                }
            }
        }

        if (!invalidFields.isEmpty()) {
            throw SdkClientException.create(
                String.format("The following fields are not allowed: %s",
                              String.join(", ", invalidFields))
            );
        }
    }
}
