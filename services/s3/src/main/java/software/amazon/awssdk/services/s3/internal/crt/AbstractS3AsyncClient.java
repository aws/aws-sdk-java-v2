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

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
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
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseResponse;
import software.amazon.awssdk.services.s3.paginators.ListMultipartUploadsPublisher;
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsPublisher;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;
import software.amazon.awssdk.services.s3.paginators.ListPartsPublisher;
import software.amazon.awssdk.services.s3.waiters.S3AsyncWaiter;

@SdkInternalApi
public abstract class AbstractS3AsyncClient implements S3AsyncClient {

    private final S3AsyncClient delegate;

    AbstractS3AsyncClient(S3AsyncClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody requestBody) {
        return delegate.putObject(putObjectRequest, requestBody);
    }

    @Override
    public CompletableFuture<PutObjectResponse> putObject(Consumer<PutObjectRequest.Builder> putObjectRequest,
                                                          AsyncRequestBody requestBody) {
        return delegate.putObject(putObjectRequest, requestBody);
    }

    @Override
    public CompletableFuture<ListObjectsV2Response> listObjectsV2(ListObjectsV2Request listObjectsV2Request) {
        return delegate.listObjectsV2(listObjectsV2Request);
    }

    @Override
    public CompletableFuture<CopyObjectResponse> copyObject(CopyObjectRequest copyObjectRequest) {
        return delegate.copyObject(copyObjectRequest);
    }

    @Override
    public CompletableFuture<HeadObjectResponse> headObject(HeadObjectRequest headObjectRequest) {
        return delegate.headObject(headObjectRequest);
    }

    @Override
    public CompletableFuture<AbortMultipartUploadResponse> abortMultipartUpload(
        AbortMultipartUploadRequest abortMultipartUploadRequest) {
        return delegate.abortMultipartUpload(abortMultipartUploadRequest);
    }

    @Override
    public CompletableFuture<CompleteMultipartUploadResponse> completeMultipartUpload(
        CompleteMultipartUploadRequest completeMultipartUploadRequest) {
        return delegate.completeMultipartUpload(completeMultipartUploadRequest);
    }

    @Override
    public CompletableFuture<CreateBucketResponse> createBucket(CreateBucketRequest createBucketRequest) {
        return delegate.createBucket(createBucketRequest);
    }

    @Override
    public CompletableFuture<CreateMultipartUploadResponse> createMultipartUpload(
        CreateMultipartUploadRequest createMultipartUploadRequest) {
        return delegate.createMultipartUpload(createMultipartUploadRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketResponse> deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        return delegate.deleteBucket(deleteBucketRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketAnalyticsConfigurationResponse> deleteBucketAnalyticsConfiguration(
        DeleteBucketAnalyticsConfigurationRequest deleteBucketAnalyticsConfigurationRequest) {
        return delegate.deleteBucketAnalyticsConfiguration(deleteBucketAnalyticsConfigurationRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketCorsResponse> deleteBucketCors(DeleteBucketCorsRequest deleteBucketCorsRequest) {
        return delegate.deleteBucketCors(deleteBucketCorsRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketEncryptionResponse> deleteBucketEncryption(
        DeleteBucketEncryptionRequest deleteBucketEncryptionRequest) {
        return delegate.deleteBucketEncryption(deleteBucketEncryptionRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketIntelligentTieringConfigurationResponse> deleteBucketIntelligentTieringConfiguration(
        DeleteBucketIntelligentTieringConfigurationRequest deleteBucketIntelligentTieringConfigurationRequest) {
        return delegate.deleteBucketIntelligentTieringConfiguration(deleteBucketIntelligentTieringConfigurationRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketInventoryConfigurationResponse> deleteBucketInventoryConfiguration(
        DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest) {
        return delegate.deleteBucketInventoryConfiguration(deleteBucketInventoryConfigurationRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketLifecycleResponse> deleteBucketLifecycle(
        DeleteBucketLifecycleRequest deleteBucketLifecycleRequest) {
        return delegate.deleteBucketLifecycle(deleteBucketLifecycleRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketMetricsConfigurationResponse> deleteBucketMetricsConfiguration(
        DeleteBucketMetricsConfigurationRequest deleteBucketMetricsConfigurationRequest) {
        return delegate.deleteBucketMetricsConfiguration(deleteBucketMetricsConfigurationRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketOwnershipControlsResponse> deleteBucketOwnershipControls(
        DeleteBucketOwnershipControlsRequest deleteBucketOwnershipControlsRequest) {
        return delegate.deleteBucketOwnershipControls(deleteBucketOwnershipControlsRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketPolicyResponse> deleteBucketPolicy(DeleteBucketPolicyRequest deleteBucketPolicyRequest) {
        return delegate.deleteBucketPolicy(deleteBucketPolicyRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketReplicationResponse> deleteBucketReplication(
        DeleteBucketReplicationRequest deleteBucketReplicationRequest) {
        return delegate.deleteBucketReplication(deleteBucketReplicationRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketTaggingResponse> deleteBucketTagging(
        DeleteBucketTaggingRequest deleteBucketTaggingRequest) {
        return delegate.deleteBucketTagging(deleteBucketTaggingRequest);
    }

    @Override
    public CompletableFuture<DeleteBucketWebsiteResponse> deleteBucketWebsite(
        DeleteBucketWebsiteRequest deleteBucketWebsiteRequest) {
        return delegate.deleteBucketWebsite(deleteBucketWebsiteRequest);
    }

    @Override
    public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest deleteObjectRequest) {
        return delegate.deleteObject(deleteObjectRequest);
    }

    @Override
    public CompletableFuture<DeleteObjectTaggingResponse> deleteObjectTagging(
        DeleteObjectTaggingRequest deleteObjectTaggingRequest) {
        return delegate.deleteObjectTagging(deleteObjectTaggingRequest);
    }

    @Override
    public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        return delegate.deleteObjects(deleteObjectsRequest);
    }

    @Override
    public CompletableFuture<DeletePublicAccessBlockResponse> deletePublicAccessBlock(
        DeletePublicAccessBlockRequest deletePublicAccessBlockRequest) {
        return delegate.deletePublicAccessBlock(deletePublicAccessBlockRequest);
    }

    @Override
    public CompletableFuture<GetBucketAccelerateConfigurationResponse> getBucketAccelerateConfiguration(
        GetBucketAccelerateConfigurationRequest getBucketAccelerateConfigurationRequest) {
        return delegate.getBucketAccelerateConfiguration(getBucketAccelerateConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketAclResponse> getBucketAcl(GetBucketAclRequest getBucketAclRequest) {
        return delegate.getBucketAcl(getBucketAclRequest);
    }

    @Override
    public CompletableFuture<GetBucketAnalyticsConfigurationResponse> getBucketAnalyticsConfiguration(
        GetBucketAnalyticsConfigurationRequest getBucketAnalyticsConfigurationRequest) {
        return delegate.getBucketAnalyticsConfiguration(getBucketAnalyticsConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketCorsResponse> getBucketCors(GetBucketCorsRequest getBucketCorsRequest) {
        return delegate.getBucketCors(getBucketCorsRequest);
    }

    @Override
    public CompletableFuture<GetBucketEncryptionResponse> getBucketEncryption(
        GetBucketEncryptionRequest getBucketEncryptionRequest) {
        return delegate.getBucketEncryption(getBucketEncryptionRequest);
    }

    @Override
    public CompletableFuture<GetBucketIntelligentTieringConfigurationResponse> getBucketIntelligentTieringConfiguration(
        GetBucketIntelligentTieringConfigurationRequest getBucketIntelligentTieringConfigurationRequest) {
        return delegate.getBucketIntelligentTieringConfiguration(getBucketIntelligentTieringConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketInventoryConfigurationResponse> getBucketInventoryConfiguration(
        GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest) {
        return delegate.getBucketInventoryConfiguration(getBucketInventoryConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketLifecycleConfigurationResponse> getBucketLifecycleConfiguration(
        GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
        return delegate.getBucketLifecycleConfiguration(getBucketLifecycleConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketLocationResponse> getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        return delegate.getBucketLocation(getBucketLocationRequest);
    }

    @Override
    public CompletableFuture<GetBucketLoggingResponse> getBucketLogging(GetBucketLoggingRequest getBucketLoggingRequest) {
        return delegate.getBucketLogging(getBucketLoggingRequest);
    }

    @Override
    public CompletableFuture<GetBucketMetricsConfigurationResponse> getBucketMetricsConfiguration(
        GetBucketMetricsConfigurationRequest getBucketMetricsConfigurationRequest) {
        return delegate.getBucketMetricsConfiguration(getBucketMetricsConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketNotificationConfigurationResponse> getBucketNotificationConfiguration(
        GetBucketNotificationConfigurationRequest getBucketNotificationConfigurationRequest) {
        return delegate.getBucketNotificationConfiguration(getBucketNotificationConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetBucketOwnershipControlsResponse> getBucketOwnershipControls(
        GetBucketOwnershipControlsRequest getBucketOwnershipControlsRequest) {
        return delegate.getBucketOwnershipControls(getBucketOwnershipControlsRequest);
    }

    @Override
    public CompletableFuture<GetBucketPolicyResponse> getBucketPolicy(GetBucketPolicyRequest getBucketPolicyRequest) {
        return delegate.getBucketPolicy(getBucketPolicyRequest);
    }

    @Override
    public CompletableFuture<GetBucketPolicyStatusResponse> getBucketPolicyStatus(
        GetBucketPolicyStatusRequest getBucketPolicyStatusRequest) {
        return delegate.getBucketPolicyStatus(getBucketPolicyStatusRequest);
    }

    @Override
    public CompletableFuture<GetBucketReplicationResponse> getBucketReplication(
        GetBucketReplicationRequest getBucketReplicationRequest) {
        return delegate.getBucketReplication(getBucketReplicationRequest);
    }

    @Override
    public CompletableFuture<GetBucketRequestPaymentResponse> getBucketRequestPayment(
        GetBucketRequestPaymentRequest getBucketRequestPaymentRequest) {
        return delegate.getBucketRequestPayment(getBucketRequestPaymentRequest);
    }

    @Override
    public CompletableFuture<GetBucketTaggingResponse> getBucketTagging(GetBucketTaggingRequest getBucketTaggingRequest) {
        return delegate.getBucketTagging(getBucketTaggingRequest);
    }

    @Override
    public CompletableFuture<GetBucketVersioningResponse> getBucketVersioning(
        GetBucketVersioningRequest getBucketVersioningRequest) {
        return delegate.getBucketVersioning(getBucketVersioningRequest);
    }

    @Override
    public CompletableFuture<GetBucketWebsiteResponse> getBucketWebsite(GetBucketWebsiteRequest getBucketWebsiteRequest) {
        return delegate.getBucketWebsite(getBucketWebsiteRequest);
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        GetObjectRequest getObjectRequest,
        AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {
        return delegate.getObject(getObjectRequest, asyncResponseTransformer);
    }

    @Override
    public CompletableFuture<GetObjectResponse> getObject(GetObjectRequest getObjectRequest, Path destinationPath) {
        return delegate.getObject(getObjectRequest, destinationPath);
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObject(
        Consumer<GetObjectRequest.Builder> getObjectRequest,
        AsyncResponseTransformer<GetObjectResponse, ReturnT> asyncResponseTransformer) {
        return delegate.getObject(getObjectRequest, asyncResponseTransformer);
    }

    @Override
    public CompletableFuture<GetObjectAclResponse> getObjectAcl(GetObjectAclRequest getObjectAclRequest) {
        return delegate.getObjectAcl(getObjectAclRequest);
    }

    @Override
    public CompletableFuture<GetObjectAttributesResponse> getObjectAttributes(
        GetObjectAttributesRequest getObjectAttributesRequest) {
        return delegate.getObjectAttributes(getObjectAttributesRequest);
    }

    @Override
    public CompletableFuture<GetObjectLegalHoldResponse> getObjectLegalHold(GetObjectLegalHoldRequest getObjectLegalHoldRequest) {
        return delegate.getObjectLegalHold(getObjectLegalHoldRequest);
    }

    @Override
    public CompletableFuture<GetObjectLockConfigurationResponse> getObjectLockConfiguration(
        GetObjectLockConfigurationRequest getObjectLockConfigurationRequest) {
        return delegate.getObjectLockConfiguration(getObjectLockConfigurationRequest);
    }

    @Override
    public CompletableFuture<GetObjectRetentionResponse> getObjectRetention(GetObjectRetentionRequest getObjectRetentionRequest) {
        return delegate.getObjectRetention(getObjectRetentionRequest);
    }

    @Override
    public CompletableFuture<GetObjectTaggingResponse> getObjectTagging(GetObjectTaggingRequest getObjectTaggingRequest) {
        return delegate.getObjectTagging(getObjectTaggingRequest);
    }

    @Override
    public <ReturnT> CompletableFuture<ReturnT> getObjectTorrent(
        GetObjectTorrentRequest getObjectTorrentRequest,
        AsyncResponseTransformer<GetObjectTorrentResponse, ReturnT> asyncResponseTransformer) {
        return delegate.getObjectTorrent(getObjectTorrentRequest, asyncResponseTransformer);
    }

    @Override
    public CompletableFuture<GetObjectTorrentResponse> getObjectTorrent(GetObjectTorrentRequest getObjectTorrentRequest,
                                                                        Path destinationPath) {
        return delegate.getObjectTorrent(getObjectTorrentRequest, destinationPath);
    }

    @Override
    public CompletableFuture<GetPublicAccessBlockResponse> getPublicAccessBlock(
        GetPublicAccessBlockRequest getPublicAccessBlockRequest) {
        return delegate.getPublicAccessBlock(getPublicAccessBlockRequest);
    }

    @Override
    public CompletableFuture<HeadBucketResponse> headBucket(HeadBucketRequest headBucketRequest) {
        return delegate.headBucket(headBucketRequest);
    }

    @Override
    public CompletableFuture<ListBucketAnalyticsConfigurationsResponse> listBucketAnalyticsConfigurations(
        ListBucketAnalyticsConfigurationsRequest listBucketAnalyticsConfigurationsRequest) {
        return delegate.listBucketAnalyticsConfigurations(listBucketAnalyticsConfigurationsRequest);
    }

    @Override
    public CompletableFuture<ListBucketIntelligentTieringConfigurationsResponse> listBucketIntelligentTieringConfigurations(
        ListBucketIntelligentTieringConfigurationsRequest listBucketIntelligentTieringConfigurationsRequest) {
        return delegate.listBucketIntelligentTieringConfigurations(listBucketIntelligentTieringConfigurationsRequest);
    }

    @Override
    public CompletableFuture<ListBucketInventoryConfigurationsResponse> listBucketInventoryConfigurations(
        ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest) {
        return delegate.listBucketInventoryConfigurations(listBucketInventoryConfigurationsRequest);
    }

    @Override
    public CompletableFuture<ListBucketMetricsConfigurationsResponse> listBucketMetricsConfigurations(
        ListBucketMetricsConfigurationsRequest listBucketMetricsConfigurationsRequest) {
        return delegate.listBucketMetricsConfigurations(listBucketMetricsConfigurationsRequest);
    }

    @Override
    public CompletableFuture<ListBucketsResponse> listBuckets(ListBucketsRequest listBucketsRequest) {
        return delegate.listBuckets(listBucketsRequest);
    }

    @Override
    public CompletableFuture<ListBucketsResponse> listBuckets() {
        return delegate.listBuckets();
    }

    @Override
    public CompletableFuture<ListMultipartUploadsResponse> listMultipartUploads(
        ListMultipartUploadsRequest listMultipartUploadsRequest) {
        return delegate.listMultipartUploads(listMultipartUploadsRequest);
    }

    @Override
    public ListMultipartUploadsPublisher listMultipartUploadsPaginator(ListMultipartUploadsRequest listMultipartUploadsRequest) {
        return delegate.listMultipartUploadsPaginator(listMultipartUploadsRequest);
    }

    @Override
    public CompletableFuture<ListObjectVersionsResponse> listObjectVersions(ListObjectVersionsRequest listObjectVersionsRequest) {
        return delegate.listObjectVersions(listObjectVersionsRequest);
    }

    @Override
    public ListObjectVersionsPublisher listObjectVersionsPaginator(ListObjectVersionsRequest listObjectVersionsRequest) {
        return delegate.listObjectVersionsPaginator(listObjectVersionsRequest);
    }

    @Override
    public CompletableFuture<ListObjectsResponse> listObjects(ListObjectsRequest listObjectsRequest) {
        return delegate.listObjects(listObjectsRequest);
    }

    @Override
    public ListObjectsV2Publisher listObjectsV2Paginator(ListObjectsV2Request listObjectsV2Request) {
        return delegate.listObjectsV2Paginator(listObjectsV2Request);
    }

    @Override
    public CompletableFuture<ListPartsResponse> listParts(ListPartsRequest listPartsRequest) {
        return delegate.listParts(listPartsRequest);
    }

    @Override
    public ListPartsPublisher listPartsPaginator(ListPartsRequest listPartsRequest) {
        return delegate.listPartsPaginator(listPartsRequest);
    }

    @Override
    public CompletableFuture<PutBucketAccelerateConfigurationResponse> putBucketAccelerateConfiguration(
        PutBucketAccelerateConfigurationRequest putBucketAccelerateConfigurationRequest) {
        return delegate.putBucketAccelerateConfiguration(putBucketAccelerateConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketAclResponse> putBucketAcl(PutBucketAclRequest putBucketAclRequest) {
        return delegate.putBucketAcl(putBucketAclRequest);
    }

    @Override
    public CompletableFuture<PutBucketAnalyticsConfigurationResponse> putBucketAnalyticsConfiguration(
        PutBucketAnalyticsConfigurationRequest putBucketAnalyticsConfigurationRequest) {
        return delegate.putBucketAnalyticsConfiguration(putBucketAnalyticsConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketCorsResponse> putBucketCors(PutBucketCorsRequest putBucketCorsRequest) {
        return delegate.putBucketCors(putBucketCorsRequest);
    }

    @Override
    public CompletableFuture<PutBucketEncryptionResponse> putBucketEncryption(
        PutBucketEncryptionRequest putBucketEncryptionRequest) {
        return delegate.putBucketEncryption(putBucketEncryptionRequest);
    }

    @Override
    public CompletableFuture<PutBucketIntelligentTieringConfigurationResponse> putBucketIntelligentTieringConfiguration(
        PutBucketIntelligentTieringConfigurationRequest putBucketIntelligentTieringConfigurationRequest) {
        return delegate.putBucketIntelligentTieringConfiguration(putBucketIntelligentTieringConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketInventoryConfigurationResponse> putBucketInventoryConfiguration(
        PutBucketInventoryConfigurationRequest putBucketInventoryConfigurationRequest) {
        return delegate.putBucketInventoryConfiguration(putBucketInventoryConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketLifecycleConfigurationResponse> putBucketLifecycleConfiguration(
        PutBucketLifecycleConfigurationRequest putBucketLifecycleConfigurationRequest) {
        return delegate.putBucketLifecycleConfiguration(putBucketLifecycleConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketLoggingResponse> putBucketLogging(PutBucketLoggingRequest putBucketLoggingRequest) {
        return delegate.putBucketLogging(putBucketLoggingRequest);
    }

    @Override
    public CompletableFuture<PutBucketMetricsConfigurationResponse> putBucketMetricsConfiguration(
        PutBucketMetricsConfigurationRequest putBucketMetricsConfigurationRequest) {
        return delegate.putBucketMetricsConfiguration(putBucketMetricsConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketNotificationConfigurationResponse> putBucketNotificationConfiguration(
        PutBucketNotificationConfigurationRequest putBucketNotificationConfigurationRequest) {
        return delegate.putBucketNotificationConfiguration(putBucketNotificationConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutBucketOwnershipControlsResponse> putBucketOwnershipControls(
        PutBucketOwnershipControlsRequest putBucketOwnershipControlsRequest) {
        return delegate.putBucketOwnershipControls(putBucketOwnershipControlsRequest);
    }

    @Override
    public CompletableFuture<PutBucketPolicyResponse> putBucketPolicy(PutBucketPolicyRequest putBucketPolicyRequest) {
        return delegate.putBucketPolicy(putBucketPolicyRequest);
    }

    @Override
    public CompletableFuture<PutBucketReplicationResponse> putBucketReplication(
        PutBucketReplicationRequest putBucketReplicationRequest) {
        return delegate.putBucketReplication(putBucketReplicationRequest);
    }

    @Override
    public CompletableFuture<PutBucketRequestPaymentResponse> putBucketRequestPayment(
        PutBucketRequestPaymentRequest putBucketRequestPaymentRequest) {
        return delegate.putBucketRequestPayment(putBucketRequestPaymentRequest);
    }

    @Override
    public CompletableFuture<PutBucketTaggingResponse> putBucketTagging(PutBucketTaggingRequest putBucketTaggingRequest) {
        return delegate.putBucketTagging(putBucketTaggingRequest);
    }

    @Override
    public CompletableFuture<PutBucketVersioningResponse> putBucketVersioning(
        PutBucketVersioningRequest putBucketVersioningRequest) {
        return delegate.putBucketVersioning(putBucketVersioningRequest);
    }

    @Override
    public CompletableFuture<PutBucketWebsiteResponse> putBucketWebsite(PutBucketWebsiteRequest putBucketWebsiteRequest) {
        return delegate.putBucketWebsite(putBucketWebsiteRequest);
    }

    @Override
    public CompletableFuture<PutObjectAclResponse> putObjectAcl(PutObjectAclRequest putObjectAclRequest) {
        return delegate.putObjectAcl(putObjectAclRequest);
    }

    @Override
    public CompletableFuture<PutObjectLegalHoldResponse> putObjectLegalHold(PutObjectLegalHoldRequest putObjectLegalHoldRequest) {
        return delegate.putObjectLegalHold(putObjectLegalHoldRequest);
    }

    @Override
    public CompletableFuture<PutObjectLockConfigurationResponse> putObjectLockConfiguration(
        PutObjectLockConfigurationRequest putObjectLockConfigurationRequest) {
        return delegate.putObjectLockConfiguration(putObjectLockConfigurationRequest);
    }

    @Override
    public CompletableFuture<PutObjectRetentionResponse> putObjectRetention(PutObjectRetentionRequest putObjectRetentionRequest) {
        return delegate.putObjectRetention(putObjectRetentionRequest);
    }

    @Override
    public CompletableFuture<PutObjectTaggingResponse> putObjectTagging(PutObjectTaggingRequest putObjectTaggingRequest) {
        return delegate.putObjectTagging(putObjectTaggingRequest);
    }

    @Override
    public CompletableFuture<PutPublicAccessBlockResponse> putPublicAccessBlock(
        PutPublicAccessBlockRequest putPublicAccessBlockRequest) {
        return delegate.putPublicAccessBlock(putPublicAccessBlockRequest);
    }

    @Override
    public CompletableFuture<RestoreObjectResponse> restoreObject(RestoreObjectRequest restoreObjectRequest) {
        return delegate.restoreObject(restoreObjectRequest);
    }

    @Override
    public CompletableFuture<Void> selectObjectContent(SelectObjectContentRequest selectObjectContentRequest,
                                                       SelectObjectContentResponseHandler asyncResponseHandler) {
        return delegate.selectObjectContent(selectObjectContentRequest, asyncResponseHandler);
    }

    @Override
    public CompletableFuture<UploadPartResponse> uploadPart(UploadPartRequest uploadPartRequest, AsyncRequestBody requestBody) {
        return delegate.uploadPart(uploadPartRequest, requestBody);
    }

    @Override
    public CompletableFuture<UploadPartResponse> uploadPart(UploadPartRequest uploadPartRequest, Path sourcePath) {
        return delegate.uploadPart(uploadPartRequest, sourcePath);
    }

    @Override
    public CompletableFuture<UploadPartCopyResponse> uploadPartCopy(UploadPartCopyRequest uploadPartCopyRequest) {
        return delegate.uploadPartCopy(uploadPartCopyRequest);
    }

    @Override
    public CompletableFuture<WriteGetObjectResponseResponse> writeGetObjectResponse(
        WriteGetObjectResponseRequest writeGetObjectResponseRequest, AsyncRequestBody requestBody) {
        return delegate.writeGetObjectResponse(writeGetObjectResponseRequest, requestBody);
    }

    @Override
    public CompletableFuture<WriteGetObjectResponseResponse> writeGetObjectResponse(
        WriteGetObjectResponseRequest writeGetObjectResponseRequest, Path sourcePath) {
        return delegate.writeGetObjectResponse(writeGetObjectResponseRequest, sourcePath);
    }

    @Override
    public S3Utilities utilities() {
        return delegate.utilities();
    }

    @Override
    public S3AsyncWaiter waiter() {
        return delegate.waiter();
    }

}
