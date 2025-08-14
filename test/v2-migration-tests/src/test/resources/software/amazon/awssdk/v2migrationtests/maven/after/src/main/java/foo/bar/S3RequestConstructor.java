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

package foo.bar;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockRequest;
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
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteResponse;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclResponse;
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
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
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
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.MetricsConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.OwnershipControls;
import software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.ReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreObjectResponse;
import software.amazon.awssdk.services.s3.model.RestoreRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

public class S3RequestConstructor {
    S3Client s3 = S3Client.create();
    String bucketName = "bucketName";
    String objectKey = "objectKey";

    private S3RequestConstructor() {

    }

    public void requestconstructor() {

        s3.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket("name").key("key").uploadId("upload")
            .build());
        s3.completeMultipartUpload(CompleteMultipartUploadRequest.builder().bucket("name").key("key").uploadId("upload").multipartUpload(CompletedMultipartUpload.builder().parts(new ArrayList<>()).build())
            .build());

        s3.copyObject(CopyObjectRequest.builder()
            .build());
        s3.copyObject(CopyObjectRequest.builder().sourceBucket("1").sourceKey("2").destinationBucket("3").destinationKey("4")
            .build());
        s3.copyObject(CopyObjectRequest.builder().sourceBucket("1").sourceKey("2").sourceVersionId("3").destinationBucket("4").destinationKey("5")
            .build());
        s3.uploadPartCopy(UploadPartCopyRequest.builder()
            .build());

        s3.deleteBucket(DeleteBucketRequest.builder().bucket("name")
            .build());

        DeleteBucketAnalyticsConfigurationResponse dbacR1 =
            s3.deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest.builder()
                .build());
        DeleteBucketAnalyticsConfigurationResponse dbacR2 =
            s3.deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest.builder().bucket("1").id("2")
                .build());

        DeleteBucketEncryptionResponse dbeR1 = s3.deleteBucketEncryption(DeleteBucketEncryptionRequest.builder()
            .build());

        DeleteBucketIntelligentTieringConfigurationResponse dbitcR1 =
            s3.deleteBucketIntelligentTieringConfiguration(DeleteBucketIntelligentTieringConfigurationRequest.builder()
                .build());

        s3.deleteBucketIntelligentTieringConfiguration(DeleteBucketIntelligentTieringConfigurationRequest.builder().bucket("1").id("2")
            .build());

        DeleteBucketInventoryConfigurationResponse dbicR1 =
            s3.deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder()
                .build());

        s3.deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder().bucket("1").id("2")
            .build());

        s3.deleteBucketLifecycle(DeleteBucketLifecycleRequest.builder().bucket("name")
            .build());

        s3.deleteBucketMetricsConfiguration(DeleteBucketMetricsConfigurationRequest.builder()
            .build());
        s3.deleteBucketMetricsConfiguration(DeleteBucketMetricsConfigurationRequest.builder().bucket("1").id("2")
            .build());

        s3.deleteBucketOwnershipControls(DeleteBucketOwnershipControlsRequest.builder()
            .build());

        s3.deleteBucketPolicy(DeleteBucketPolicyRequest.builder().bucket("name")
            .build());

        s3.deleteBucketTagging(DeleteBucketTaggingRequest.builder().bucket(bucketName)
            .build());

        s3.deleteBucketWebsite(DeleteBucketWebsiteRequest.builder().bucket(bucketName)
            .build());

        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey)
            .build());

        s3.deleteObjects(DeleteObjectsRequest.builder().bucket(bucketName)
            .build());

        s3.deleteObjectTagging(DeleteObjectTaggingRequest.builder().bucket(bucketName).key(objectKey)
            .build());

        s3.deletePublicAccessBlock(DeletePublicAccessBlockRequest.builder()
            .build());

        GetBucketAccelerateConfigurationResponse bucketAccelerateConfiguration =
            s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder().bucket(bucketName)
                .build());

        GetBucketAclResponse bucketAcl = s3.getBucketAcl(GetBucketAclRequest.builder().bucket(bucketName)
            .build());

        GetBucketAnalyticsConfigurationResponse analyticsConfig = s3.getBucketAnalyticsConfiguration(
            GetBucketAnalyticsConfigurationRequest.builder()
                .build());

        GetBucketAnalyticsConfigurationResponse analyticsConfig2 = s3.getBucketAnalyticsConfiguration(
            GetBucketAnalyticsConfigurationRequest.builder().bucket("1").id("2")
                .build());

        GetBucketCorsResponse bucketCors =
            s3.getBucketCors(GetBucketCorsRequest.builder().bucket(bucketName)
                .build());

        GetBucketEncryptionResponse encryptionResult = s3.getBucketEncryption(
            GetBucketEncryptionRequest.builder()
                .build());

        GetBucketIntelligentTieringConfigurationResponse intelligentTieringConfig = s3.getBucketIntelligentTieringConfiguration(
            GetBucketIntelligentTieringConfigurationRequest.builder()
                .build());

        GetBucketIntelligentTieringConfigurationResponse intelligentTieringConfig2 = s3.getBucketIntelligentTieringConfiguration(
            GetBucketIntelligentTieringConfigurationRequest.builder().bucket("1").id("2")
                .build());

        GetBucketInventoryConfigurationResponse inventoryConfig = s3.getBucketInventoryConfiguration(
            GetBucketInventoryConfigurationRequest.builder()
                .build());

        GetBucketInventoryConfigurationResponse inventoryConfig2 = s3.getBucketInventoryConfiguration(
            GetBucketInventoryConfigurationRequest.builder().bucket("1").id("2")
                .build());

        GetBucketLifecycleConfigurationResponse bucketLifecycleConfiguration =
            s3.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(bucketName)
                .build());

        String region = s3.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucketName)
            .build()).locationConstraint().toString();

        GetBucketLoggingResponse bucketLoggingConfiguration =
            s3.getBucketLogging(GetBucketLoggingRequest.builder().bucket(bucketName)
                .build());

        GetBucketMetricsConfigurationResponse metricsConfig = s3.getBucketMetricsConfiguration(
            GetBucketMetricsConfigurationRequest.builder()
                .build());

        GetBucketMetricsConfigurationResponse metricsConfig2 = s3.getBucketMetricsConfiguration(
            GetBucketMetricsConfigurationRequest.builder().bucket("1").id("2")
                .build());

        GetBucketNotificationConfigurationResponse bucketNotificationConfiguration =
            s3.getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(bucketName)
                .build());

        GetBucketOwnershipControlsResponse ownershipControls = s3.getBucketOwnershipControls(
            GetBucketOwnershipControlsRequest.builder()
                .build());

        GetBucketPolicyResponse bucketPolicy = s3.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucketName)
            .build());

        GetBucketPolicyStatusResponse policyStatus = s3.getBucketPolicyStatus(
            GetBucketPolicyStatusRequest.builder()
                .build());

        GetBucketReplicationResponse bucketReplicationConfiguration =
            s3.getBucketReplication(GetBucketReplicationRequest.builder().bucket(bucketName)
                .build());

        GetBucketTaggingResponse bucketTaggingConfiguration =
            s3.getBucketTagging(GetBucketTaggingRequest.builder().bucket(bucketName)
                .build());

        GetBucketVersioningResponse bucketVersioningConfiguration =
            s3.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(bucketName)
                .build());

        GetBucketWebsiteResponse bucketWebsiteConfiguration =
            s3.getBucketWebsite(GetBucketWebsiteRequest.builder().bucket(bucketName)
                .build());

        ResponseInputStream<GetObjectResponse> s3Object1 = s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(objectKey)
                .build());

        ResponseInputStream<GetObjectResponse> s3Object2 = s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(objectKey).versionId("3")
                .build());

        GetObjectRequest getObjectRequestRequesterPaysTrue = GetObjectRequest.builder().bucket(bucketName).key(objectKey).requestPayer(RequestPayer.REQUESTER)
            .build();

        GetObjectRequest getObjectRequestRequesterPaysFalse = GetObjectRequest.builder().bucket(bucketName).key(objectKey)
            .build();

        GetObjectAclResponse objectAcl = s3.getObjectAcl(GetObjectAclRequest.builder().bucket(bucketName).key(objectKey)
            .build());

        GetObjectAclResponse objectAcl2 = s3.getObjectAcl(GetObjectAclRequest.builder().bucket(bucketName).key(objectKey).versionId("3")
            .build());

        GetObjectLegalHoldResponse legalHoldResult = s3.getObjectLegalHold(
            GetObjectLegalHoldRequest.builder()
                .build());

        GetObjectLockConfigurationResponse lockConfigResult = s3.getObjectLockConfiguration(
            GetObjectLockConfigurationRequest.builder()
                .build());

        HeadObjectResponse objectMetadata = s3.headObject(
            HeadObjectRequest.builder().bucket(bucketName).key(objectKey)
                .build());

        HeadObjectResponse objectMetadata2 = s3.headObject(
            HeadObjectRequest.builder().bucket(bucketName).key(objectKey).versionId("3")
                .build());

        GetObjectRetentionResponse retentionResult = s3.getObjectRetention(
            GetObjectRetentionRequest.builder()
                .build());

        GetObjectTaggingResponse taggingResult = s3.getObjectTagging(
            GetObjectTaggingRequest.builder().bucket(bucketName).key(objectKey)
                .build());

        GetObjectTaggingResponse taggingResult2 = s3.getObjectTagging(
            GetObjectTaggingRequest.builder().bucket(bucketName).key(objectKey).versionId("3")
                .build());

        GetPublicAccessBlockResponse publicAccessBlockResult = s3.getPublicAccessBlock(
            GetPublicAccessBlockRequest.builder()
                .build());

        s3.headBucket(HeadBucketRequest.builder().bucket(bucketName)
            .build());


        ListBucketAnalyticsConfigurationsResponse analyticsConfigs = s3.listBucketAnalyticsConfigurations(
            ListBucketAnalyticsConfigurationsRequest.builder()
                .build());

        ListBucketIntelligentTieringConfigurationsResponse tieringConfigs = s3.listBucketIntelligentTieringConfigurations(
            ListBucketIntelligentTieringConfigurationsRequest.builder()
                .build());

        ListBucketInventoryConfigurationsResponse inventoryConfigs = s3.listBucketInventoryConfigurations(
            ListBucketInventoryConfigurationsRequest.builder()
                .build());

        ListBucketMetricsConfigurationsResponse metricsConfigs = s3.listBucketMetricsConfigurations(
            ListBucketMetricsConfigurationsRequest.builder()
                .build());

        List<Bucket> buckets = s3.listBuckets().buckets();

        ListObjectsResponse objectListing = s3.listObjects(
            ListObjectsRequest.builder()
                .build());

        ListObjectsResponse objectListing2 = s3.listObjects(
            ListObjectsRequest.builder().bucket("1").prefix("2").marker("3").delimiter("4").maxKeys(4)
                .build());

        s3.putBucketAnalyticsConfiguration(
            PutBucketAnalyticsConfigurationRequest.builder()
                .build());
        s3.putBucketAnalyticsConfiguration(
            PutBucketAnalyticsConfigurationRequest.builder().bucket("name").analyticsConfiguration(AnalyticsConfiguration.builder()
                .build())
                .build());


        /*AWS SDK for Java v2 migration: Transform for setBucketLifecycleConfiguration method not supported. Please manually migrate your code by using builder pattern, updating from BucketLifecycleConfiguration.Rule to LifecycleRule, StorageClass to TransitionStorageClass, and adjust imports and names. Please reference https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/migration-s3-client.html#V1-setBucketLifecycleConfiguration*/s3.putBucketLifecycleConfiguration(
        PutBucketLifecycleConfigurationRequest.builder().bucket(bucketName).lifecycleConfiguration(BucketLifecycleConfiguration.builder()
            .build())
            .build());

        s3.putBucketMetricsConfiguration(
            PutBucketMetricsConfigurationRequest.builder()
                .build());
        s3.putBucketMetricsConfiguration(
            PutBucketMetricsConfigurationRequest.builder().bucket(bucketName).metricsConfiguration(MetricsConfiguration.builder()
                .build())
                .build());

        s3.putBucketNotificationConfiguration(
            PutBucketNotificationConfigurationRequest.builder().bucket(bucketName).notificationConfiguration(NotificationConfiguration.builder()
                .build())
                .build());

        s3.putBucketOwnershipControls(
            PutBucketOwnershipControlsRequest.builder().bucket(bucketName).ownershipControls(OwnershipControls.builder()
                .build())
                .build());

        s3.putBucketOwnershipControls(
            PutBucketOwnershipControlsRequest.builder()
                .build());

        s3.putBucketReplication(
            PutBucketReplicationRequest.builder()
                .build());
        s3.putBucketReplication(
            PutBucketReplicationRequest.builder().bucket(bucketName).replicationConfiguration(ReplicationConfiguration.builder()
                .build())
                .build());

        /*AWS SDK for Java v2 migration: Transform for setBucketTaggingConfiguration method not supported. Please manually migrate your code by using builder pattern, replacing TagSet.setTag() with .tagSet(Arrays.asList(Tag.builder())), and use Tagging instead of BucketTaggingConfiguration, and adjust imports and names. Please reference https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/migration-s3-client.html#V1-setBucketTaggingConfiguration*/s3.putBucketTagging(
        PutBucketTaggingRequest.builder().bucket(bucketName).tagging(Tagging.builder()
            .build())
            .build());

        DeleteObjectRequest deleteVersionRequest = DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).versionId("id")
            .build();

        ListPartsRequest listPartsRequest = ListPartsRequest.builder().bucket(bucketName).key(objectKey).uploadId("id")
            .build();

        RestoreObjectRequest restoreObjectRequest = RestoreObjectRequest.builder().bucket(bucketName).key(objectKey)
            .build();
        RestoreObjectRequest restoreObjectRequest2 = RestoreObjectRequest.builder().bucket(bucketName).key(objectKey).restoreRequest(RestoreRequest.builder().days(77).build())
            .build();
        RestoreObjectResponse restoreObjectResult = s3.restoreObject(restoreObjectRequest);

        GetBucketRequestPaymentRequest getRequestPaymentConfigurationRequest =
            GetBucketRequestPaymentRequest.builder().bucket(bucketName)
                .build();

        PutBucketPolicyRequest setBucketPolicyRequest = PutBucketPolicyRequest.builder().bucket(bucketName).policy("policyText")
            .build();

        List<Tag> tags = new ArrayList<>();
        GetObjectTaggingResponse getObjectTaggingResult = GetObjectTaggingResponse.builder().tagSet(tags)
            .build();

        PutBucketVersioningRequest setBucketVersioningConfigurationRequest =
            PutBucketVersioningRequest.builder().bucket(bucketName).versioningConfiguration(VersioningConfiguration.builder()
                .build())
                .build();
        s3.putBucketVersioning(setBucketVersioningConfigurationRequest);
    }
}