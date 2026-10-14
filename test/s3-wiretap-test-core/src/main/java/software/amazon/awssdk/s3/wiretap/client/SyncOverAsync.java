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

package software.amazon.awssdk.s3.wiretap.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketMetadataConfigurationRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketMetadataConfigurationResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketMetadataTableConfigurationRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketMetadataTableConfigurationResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;
import software.amazon.awssdk.services.s3.model.CreateSessionResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetadataConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetadataConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetadataTableConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetadataTableConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketReplicationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketMetadataConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetadataConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketMetadataTableConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetadataTableConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketOwnershipControlsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketOwnershipControlsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusResponse;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesResponse;
import software.amazon.awssdk.services.s3.model.GetObjectLegalHoldRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLegalHoldResponse;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketIntelligentTieringConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketIntelligentTieringConfigurationsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListDirectoryBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListDirectoryBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketAclRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAclResponse;
import software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.PutBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketLoggingRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLoggingResponse;
import software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationResponse;
import software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentResponse;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.PutObjectAclRequest;
import software.amazon.awssdk.services.s3.model.PutObjectAclResponse;
import software.amazon.awssdk.services.s3.model.PutObjectLegalHoldRequest;
import software.amazon.awssdk.services.s3.model.PutObjectLegalHoldResponse;
import software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRetentionResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.RenameObjectRequest;
import software.amazon.awssdk.services.s3.model.RenameObjectResponse;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectResponse;
import software.amazon.awssdk.services.s3.model.UpdateBucketMetadataInventoryTableConfigurationRequest;
import software.amazon.awssdk.services.s3.model.UpdateBucketMetadataInventoryTableConfigurationResponse;
import software.amazon.awssdk.services.s3.model.UpdateBucketMetadataJournalTableConfigurationRequest;
import software.amazon.awssdk.services.s3.model.UpdateBucketMetadataJournalTableConfigurationResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseResponse;
import software.amazon.awssdk.utils.IoUtils;

public final class SyncOverAsync implements S3Client {
    private final S3AsyncClient delegate;

    public SyncOverAsync(S3AsyncClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
        return unwrap(() -> delegate.putObject(request, toAsyncBody(body)).join());
    }

    @Override
    public <T> T getObject(GetObjectRequest request,
                           ResponseTransformer<GetObjectResponse, T> transformer) {
        ResponseBytes<GetObjectResponse> resp =
                unwrap(() -> delegate.getObject(request, AsyncResponseTransformer.toBytes()).join());
        return applyTransformer(resp, transformer);
    }

    @Override
    public UploadPartResponse uploadPart(UploadPartRequest request, RequestBody body) {
        return unwrap(() -> delegate.uploadPart(request, toAsyncBody(body)).join());
    }

    @Override
    public <T> T getObjectTorrent(GetObjectTorrentRequest request,
                                  ResponseTransformer<GetObjectTorrentResponse, T> transformer) {
        ResponseBytes<GetObjectTorrentResponse> resp =
                unwrap(() -> delegate.getObjectTorrent(request, AsyncResponseTransformer.toBytes()).join());
        return applyTransformer(resp, transformer);
    }

    @Override
    public WriteGetObjectResponseResponse writeGetObjectResponse(
            WriteGetObjectResponseRequest request, RequestBody body) {
        return unwrap(() -> delegate.writeGetObjectResponse(request, toAsyncBody(body)).join());
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectRequest request) {
        return unwrap(() -> delegate.copyObject(request).join());
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
        return unwrap(() -> delegate.deleteObject(request).join());
    }

    @Override
    public DeleteObjectsResponse deleteObjects(DeleteObjectsRequest request) {
        return unwrap(() -> delegate.deleteObjects(request).join());
    }

    @Override
    public DeleteObjectTaggingResponse deleteObjectTagging(DeleteObjectTaggingRequest request) {
        return unwrap(() -> delegate.deleteObjectTagging(request).join());
    }

    @Override
    public GetObjectAclResponse getObjectAcl(GetObjectAclRequest request) {
        return unwrap(() -> delegate.getObjectAcl(request).join());
    }

    @Override
    public GetObjectAttributesResponse getObjectAttributes(GetObjectAttributesRequest request) {
        return unwrap(() -> delegate.getObjectAttributes(request).join());
    }

    @Override
    public GetObjectLegalHoldResponse getObjectLegalHold(GetObjectLegalHoldRequest request) {
        return unwrap(() -> delegate.getObjectLegalHold(request).join());
    }

    @Override
    public GetObjectLockConfigurationResponse getObjectLockConfiguration(
            GetObjectLockConfigurationRequest request) {
        return unwrap(() -> delegate.getObjectLockConfiguration(request).join());
    }

    @Override
    public GetObjectRetentionResponse getObjectRetention(GetObjectRetentionRequest request) {
        return unwrap(() -> delegate.getObjectRetention(request).join());
    }

    @Override
    public GetObjectTaggingResponse getObjectTagging(GetObjectTaggingRequest request) {
        return unwrap(() -> delegate.getObjectTagging(request).join());
    }

    @Override
    public HeadObjectResponse headObject(HeadObjectRequest request) {
        return unwrap(() -> delegate.headObject(request).join());
    }

    @Override
    public PutObjectAclResponse putObjectAcl(PutObjectAclRequest request) {
        return unwrap(() -> delegate.putObjectAcl(request).join());
    }

    @Override
    public PutObjectLegalHoldResponse putObjectLegalHold(PutObjectLegalHoldRequest request) {
        return unwrap(() -> delegate.putObjectLegalHold(request).join());
    }

    @Override
    public PutObjectLockConfigurationResponse putObjectLockConfiguration(
            PutObjectLockConfigurationRequest request) {
        return unwrap(() -> delegate.putObjectLockConfiguration(request).join());
    }

    @Override
    public PutObjectRetentionResponse putObjectRetention(PutObjectRetentionRequest request) {
        return unwrap(() -> delegate.putObjectRetention(request).join());
    }

    @Override
    public PutObjectTaggingResponse putObjectTagging(PutObjectTaggingRequest request) {
        return unwrap(() -> delegate.putObjectTagging(request).join());
    }

    @Override
    public RenameObjectResponse renameObject(RenameObjectRequest request) {
        return unwrap(() -> delegate.renameObject(request).join());
    }

    @Override
    public RestoreObjectResponse restoreObject(RestoreObjectRequest request) {
        return unwrap(() -> delegate.restoreObject(request).join());
    }

    @Override
    public AbortMultipartUploadResponse abortMultipartUpload(AbortMultipartUploadRequest request) {
        return unwrap(() -> delegate.abortMultipartUpload(request).join());
    }

    @Override
    public CompleteMultipartUploadResponse completeMultipartUpload(
            CompleteMultipartUploadRequest request) {
        return unwrap(() -> delegate.completeMultipartUpload(request).join());
    }

    @Override
    public CreateMultipartUploadResponse createMultipartUpload(
            CreateMultipartUploadRequest request) {
        return unwrap(() -> delegate.createMultipartUpload(request).join());
    }

    @Override
    public UploadPartCopyResponse uploadPartCopy(UploadPartCopyRequest request) {
        return unwrap(() -> delegate.uploadPartCopy(request).join());
    }

    @Override
    public ListMultipartUploadsResponse listMultipartUploads(
            ListMultipartUploadsRequest request) {
        return unwrap(() -> delegate.listMultipartUploads(request).join());
    }

    @Override
    public ListPartsResponse listParts(ListPartsRequest request) {
        return unwrap(() -> delegate.listParts(request).join());
    }

    @Override
    public CreateBucketResponse createBucket(CreateBucketRequest request) {
        return unwrap(() -> delegate.createBucket(request).join());
    }

    @Override
    public CreateSessionResponse createSession(CreateSessionRequest request) {
        return unwrap(() -> delegate.createSession(request).join());
    }

    @Override
    public DeleteBucketResponse deleteBucket(DeleteBucketRequest request) {
        return unwrap(() -> delegate.deleteBucket(request).join());
    }

    @Override
    public HeadBucketResponse headBucket(HeadBucketRequest request) {
        return unwrap(() -> delegate.headBucket(request).join());
    }

    @Override
    public ListBucketsResponse listBuckets(ListBucketsRequest request) {
        return unwrap(() -> delegate.listBuckets(request).join());
    }

    @Override
    public ListDirectoryBucketsResponse listDirectoryBuckets(
            ListDirectoryBucketsRequest request) {
        return unwrap(() -> delegate.listDirectoryBuckets(request).join());
    }

    @Override
    public ListObjectsResponse listObjects(ListObjectsRequest request) {
        return unwrap(() -> delegate.listObjects(request).join());
    }

    @Override
    public ListObjectsV2Response listObjectsV2(ListObjectsV2Request request) {
        return unwrap(() -> delegate.listObjectsV2(request).join());
    }

    @Override
    public ListObjectVersionsResponse listObjectVersions(ListObjectVersionsRequest request) {
        return unwrap(() -> delegate.listObjectVersions(request).join());
    }

    @Override
    public GetBucketAccelerateConfigurationResponse getBucketAccelerateConfiguration(
            GetBucketAccelerateConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketAccelerateConfiguration(request).join());
    }

    @Override
    public PutBucketAccelerateConfigurationResponse putBucketAccelerateConfiguration(
            PutBucketAccelerateConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketAccelerateConfiguration(request).join());
    }

    @Override
    public GetBucketAclResponse getBucketAcl(GetBucketAclRequest request) {
        return unwrap(() -> delegate.getBucketAcl(request).join());
    }

    @Override
    public PutBucketAclResponse putBucketAcl(PutBucketAclRequest request) {
        return unwrap(() -> delegate.putBucketAcl(request).join());
    }

    @Override
    public GetBucketAnalyticsConfigurationResponse getBucketAnalyticsConfiguration(
            GetBucketAnalyticsConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketAnalyticsConfiguration(request).join());
    }

    @Override
    public PutBucketAnalyticsConfigurationResponse putBucketAnalyticsConfiguration(
            PutBucketAnalyticsConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketAnalyticsConfiguration(request).join());
    }

    @Override
    public DeleteBucketAnalyticsConfigurationResponse deleteBucketAnalyticsConfiguration(
            DeleteBucketAnalyticsConfigurationRequest request) {
        return unwrap(() -> delegate.deleteBucketAnalyticsConfiguration(request).join());
    }

    @Override
    public ListBucketAnalyticsConfigurationsResponse listBucketAnalyticsConfigurations(
            ListBucketAnalyticsConfigurationsRequest request) {
        return unwrap(() -> delegate.listBucketAnalyticsConfigurations(request).join());
    }

    @Override
    public GetBucketCorsResponse getBucketCors(GetBucketCorsRequest request) {
        return unwrap(() -> delegate.getBucketCors(request).join());
    }

    @Override
    public PutBucketCorsResponse putBucketCors(PutBucketCorsRequest request) {
        return unwrap(() -> delegate.putBucketCors(request).join());
    }

    @Override
    public DeleteBucketCorsResponse deleteBucketCors(DeleteBucketCorsRequest request) {
        return unwrap(() -> delegate.deleteBucketCors(request).join());
    }

    @Override
    public GetBucketEncryptionResponse getBucketEncryption(
            GetBucketEncryptionRequest request) {
        return unwrap(() -> delegate.getBucketEncryption(request).join());
    }

    @Override
    public PutBucketEncryptionResponse putBucketEncryption(
            PutBucketEncryptionRequest request) {
        return unwrap(() -> delegate.putBucketEncryption(request).join());
    }

    @Override
    public DeleteBucketEncryptionResponse deleteBucketEncryption(
            DeleteBucketEncryptionRequest request) {
        return unwrap(() -> delegate.deleteBucketEncryption(request).join());
    }

    @Override
    public GetBucketIntelligentTieringConfigurationResponse getBucketIntelligentTieringConfiguration(
            GetBucketIntelligentTieringConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketIntelligentTieringConfiguration(request).join());
    }

    @Override
    public PutBucketIntelligentTieringConfigurationResponse putBucketIntelligentTieringConfiguration(
            PutBucketIntelligentTieringConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketIntelligentTieringConfiguration(request).join());
    }

    @Override
    public DeleteBucketIntelligentTieringConfigurationResponse deleteBucketIntelligentTieringConfiguration(
            DeleteBucketIntelligentTieringConfigurationRequest request) {
        return unwrap(() -> delegate.deleteBucketIntelligentTieringConfiguration(request).join());
    }

    @Override
    public ListBucketIntelligentTieringConfigurationsResponse listBucketIntelligentTieringConfigurations(
            ListBucketIntelligentTieringConfigurationsRequest request) {
        return unwrap(() -> delegate.listBucketIntelligentTieringConfigurations(request).join());
    }

    @Override
    public GetBucketInventoryConfigurationResponse getBucketInventoryConfiguration(
            GetBucketInventoryConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketInventoryConfiguration(request).join());
    }

    @Override
    public PutBucketInventoryConfigurationResponse putBucketInventoryConfiguration(
            PutBucketInventoryConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketInventoryConfiguration(request).join());
    }

    @Override
    public DeleteBucketInventoryConfigurationResponse deleteBucketInventoryConfiguration(
            DeleteBucketInventoryConfigurationRequest request) {
        return unwrap(() -> delegate.deleteBucketInventoryConfiguration(request).join());
    }

    @Override
    public ListBucketInventoryConfigurationsResponse listBucketInventoryConfigurations(
            ListBucketInventoryConfigurationsRequest request) {
        return unwrap(() -> delegate.listBucketInventoryConfigurations(request).join());
    }

    @Override
    public GetBucketLifecycleConfigurationResponse getBucketLifecycleConfiguration(
            GetBucketLifecycleConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketLifecycleConfiguration(request).join());
    }

    @Override
    public PutBucketLifecycleConfigurationResponse putBucketLifecycleConfiguration(
            PutBucketLifecycleConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketLifecycleConfiguration(request).join());
    }

    @Override
    public DeleteBucketLifecycleResponse deleteBucketLifecycle(
            DeleteBucketLifecycleRequest request) {
        return unwrap(() -> delegate.deleteBucketLifecycle(request).join());
    }

    @Override
    public GetBucketLocationResponse getBucketLocation(GetBucketLocationRequest request) {
        return unwrap(() -> delegate.getBucketLocation(request).join());
    }

    @Override
    public GetBucketLoggingResponse getBucketLogging(GetBucketLoggingRequest request) {
        return unwrap(() -> delegate.getBucketLogging(request).join());
    }

    @Override
    public PutBucketLoggingResponse putBucketLogging(PutBucketLoggingRequest request) {
        return unwrap(() -> delegate.putBucketLogging(request).join());
    }

    @Override
    public GetBucketMetadataConfigurationResponse getBucketMetadataConfiguration(
            GetBucketMetadataConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketMetadataConfiguration(request).join());
    }

    @Override
    public CreateBucketMetadataConfigurationResponse createBucketMetadataConfiguration(
            CreateBucketMetadataConfigurationRequest request) {
        return unwrap(() -> delegate.createBucketMetadataConfiguration(request).join());
    }

    @Override
    public DeleteBucketMetadataConfigurationResponse deleteBucketMetadataConfiguration(
            DeleteBucketMetadataConfigurationRequest request) {
        return unwrap(() -> delegate.deleteBucketMetadataConfiguration(request).join());
    }

    @Override
    public GetBucketMetadataTableConfigurationResponse getBucketMetadataTableConfiguration(
            GetBucketMetadataTableConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketMetadataTableConfiguration(request).join());
    }

    @Override
    public CreateBucketMetadataTableConfigurationResponse createBucketMetadataTableConfiguration(
            CreateBucketMetadataTableConfigurationRequest request) {
        return unwrap(() -> delegate.createBucketMetadataTableConfiguration(request).join());
    }

    @Override
    public DeleteBucketMetadataTableConfigurationResponse deleteBucketMetadataTableConfiguration(
            DeleteBucketMetadataTableConfigurationRequest request) {
        return unwrap(() -> delegate.deleteBucketMetadataTableConfiguration(request).join());
    }

    @Override
    public UpdateBucketMetadataInventoryTableConfigurationResponse updateBucketMetadataInventoryTableConfiguration(
            UpdateBucketMetadataInventoryTableConfigurationRequest request) {
        return unwrap(() -> delegate.updateBucketMetadataInventoryTableConfiguration(request).join());
    }

    @Override
    public UpdateBucketMetadataJournalTableConfigurationResponse updateBucketMetadataJournalTableConfiguration(
            UpdateBucketMetadataJournalTableConfigurationRequest request) {
        return unwrap(() -> delegate.updateBucketMetadataJournalTableConfiguration(request).join());
    }

    @Override
    public GetBucketMetricsConfigurationResponse getBucketMetricsConfiguration(
            GetBucketMetricsConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketMetricsConfiguration(request).join());
    }

    @Override
    public PutBucketMetricsConfigurationResponse putBucketMetricsConfiguration(
            PutBucketMetricsConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketMetricsConfiguration(request).join());
    }

    @Override
    public DeleteBucketMetricsConfigurationResponse deleteBucketMetricsConfiguration(
            DeleteBucketMetricsConfigurationRequest request) {
        return unwrap(() -> delegate.deleteBucketMetricsConfiguration(request).join());
    }

    @Override
    public ListBucketMetricsConfigurationsResponse listBucketMetricsConfigurations(
            ListBucketMetricsConfigurationsRequest request) {
        return unwrap(() -> delegate.listBucketMetricsConfigurations(request).join());
    }

    @Override
    public GetBucketNotificationConfigurationResponse getBucketNotificationConfiguration(
            GetBucketNotificationConfigurationRequest request) {
        return unwrap(() -> delegate.getBucketNotificationConfiguration(request).join());
    }

    @Override
    public PutBucketNotificationConfigurationResponse putBucketNotificationConfiguration(
            PutBucketNotificationConfigurationRequest request) {
        return unwrap(() -> delegate.putBucketNotificationConfiguration(request).join());
    }

    @Override
    public GetBucketOwnershipControlsResponse getBucketOwnershipControls(
            GetBucketOwnershipControlsRequest request) {
        return unwrap(() -> delegate.getBucketOwnershipControls(request).join());
    }

    @Override
    public PutBucketOwnershipControlsResponse putBucketOwnershipControls(
            PutBucketOwnershipControlsRequest request) {
        return unwrap(() -> delegate.putBucketOwnershipControls(request).join());
    }

    @Override
    public DeleteBucketOwnershipControlsResponse deleteBucketOwnershipControls(
            DeleteBucketOwnershipControlsRequest request) {
        return unwrap(() -> delegate.deleteBucketOwnershipControls(request).join());
    }

    @Override
    public GetBucketPolicyResponse getBucketPolicy(GetBucketPolicyRequest request) {
        return unwrap(() -> delegate.getBucketPolicy(request).join());
    }

    @Override
    public PutBucketPolicyResponse putBucketPolicy(PutBucketPolicyRequest request) {
        return unwrap(() -> delegate.putBucketPolicy(request).join());
    }

    @Override
    public DeleteBucketPolicyResponse deleteBucketPolicy(DeleteBucketPolicyRequest request) {
        return unwrap(() -> delegate.deleteBucketPolicy(request).join());
    }

    @Override
    public GetBucketPolicyStatusResponse getBucketPolicyStatus(
            GetBucketPolicyStatusRequest request) {
        return unwrap(() -> delegate.getBucketPolicyStatus(request).join());
    }

    @Override
    public GetBucketReplicationResponse getBucketReplication(
            GetBucketReplicationRequest request) {
        return unwrap(() -> delegate.getBucketReplication(request).join());
    }

    @Override
    public PutBucketReplicationResponse putBucketReplication(
            PutBucketReplicationRequest request) {
        return unwrap(() -> delegate.putBucketReplication(request).join());
    }

    @Override
    public DeleteBucketReplicationResponse deleteBucketReplication(
            DeleteBucketReplicationRequest request) {
        return unwrap(() -> delegate.deleteBucketReplication(request).join());
    }

    @Override
    public GetBucketRequestPaymentResponse getBucketRequestPayment(
            GetBucketRequestPaymentRequest request) {
        return unwrap(() -> delegate.getBucketRequestPayment(request).join());
    }

    @Override
    public PutBucketRequestPaymentResponse putBucketRequestPayment(
            PutBucketRequestPaymentRequest request) {
        return unwrap(() -> delegate.putBucketRequestPayment(request).join());
    }

    @Override
    public GetBucketTaggingResponse getBucketTagging(GetBucketTaggingRequest request) {
        return unwrap(() -> delegate.getBucketTagging(request).join());
    }

    @Override
    public PutBucketTaggingResponse putBucketTagging(PutBucketTaggingRequest request) {
        return unwrap(() -> delegate.putBucketTagging(request).join());
    }

    @Override
    public DeleteBucketTaggingResponse deleteBucketTagging(
            DeleteBucketTaggingRequest request) {
        return unwrap(() -> delegate.deleteBucketTagging(request).join());
    }

    @Override
    public GetBucketVersioningResponse getBucketVersioning(
            GetBucketVersioningRequest request) {
        return unwrap(() -> delegate.getBucketVersioning(request).join());
    }

    @Override
    public PutBucketVersioningResponse putBucketVersioning(
            PutBucketVersioningRequest request) {
        return unwrap(() -> delegate.putBucketVersioning(request).join());
    }

    @Override
    public GetBucketWebsiteResponse getBucketWebsite(GetBucketWebsiteRequest request) {
        return unwrap(() -> delegate.getBucketWebsite(request).join());
    }

    @Override
    public PutBucketWebsiteResponse putBucketWebsite(PutBucketWebsiteRequest request) {
        return unwrap(() -> delegate.putBucketWebsite(request).join());
    }

    @Override
    public DeleteBucketWebsiteResponse deleteBucketWebsite(
            DeleteBucketWebsiteRequest request) {
        return unwrap(() -> delegate.deleteBucketWebsite(request).join());
    }

    @Override
    public GetPublicAccessBlockResponse getPublicAccessBlock(
            GetPublicAccessBlockRequest request) {
        return unwrap(() -> delegate.getPublicAccessBlock(request).join());
    }

    @Override
    public PutPublicAccessBlockResponse putPublicAccessBlock(
            PutPublicAccessBlockRequest request) {
        return unwrap(() -> delegate.putPublicAccessBlock(request).join());
    }

    @Override
    public DeletePublicAccessBlockResponse deletePublicAccessBlock(
            DeletePublicAccessBlockRequest request) {
        return unwrap(() -> delegate.deletePublicAccessBlock(request).join());
    }

    @Override
    public String serviceName() {
        return delegate.serviceName();
    }

    @Override
    public void close() {
    }

    private static AsyncRequestBody toAsyncBody(RequestBody body) {
        byte[] bytes;
        try {
            bytes = IoUtils.toByteArray(body.contentStreamProvider().newStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return AsyncRequestBody.fromBytes(bytes);
    }

    private static <R, T> T applyTransformer(ResponseBytes<R> resp,
                                              ResponseTransformer<R, T> transformer) {
        try {
            return transformer.transform(resp.response(),
                    AbortableInputStream.create(new ByteArrayInputStream(resp.asByteArray())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T unwrap(Supplier<T> call) {
        try {
            return call.get();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e.getCause());
        }
    }
}
