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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.MetricsConfiguration;
import software.amazon.awssdk.services.s3.model.OwnershipControls;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

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

        //s3.deleteObjects(new DeleteObjectsRequest(bucketName));

        s3.deleteObjectTagging(DeleteObjectTaggingRequest.builder().bucket(bucketName).key(objectKey)
            .build());

        s3.deletePublicAccessBlock(DeletePublicAccessBlockRequest.builder()
            .build());

        // Not supported
        // s3.deleteVersion(new DeleteVersionRequest(bucketName, objectKey, "version-id-here"));
        // MultiFactorAuthentication mfa = new MultiFactorAuthentication("SN","token");
        // s3.deleteVersion(new DeleteVersionRequest(bucketName, objectKey, "version-id-here", mfa));

        AccelerateConfiguration accelerateConfig = s3.getBucketAccelerateConfiguration(
            GetBucketAccelerateConfigurationRequest.builder().bucket(bucketName)
                .build());

        AccessControlList acl = s3.getBucketAcl(
            GetBucketAclRequest.builder().bucket(bucketName)
                .build());

        GetBucketAnalyticsConfigurationResponse analyticsConfig = s3.getBucketAnalyticsConfiguration(
            GetBucketAnalyticsConfigurationRequest.builder()
                .build());

        GetBucketAnalyticsConfigurationResponse analyticsConfig2 = s3.getBucketAnalyticsConfiguration(
            GetBucketAnalyticsConfigurationRequest.builder().bucket("1").id("2")
                .build());

        CORSConfiguration corsConfig = s3.getBucketCors(
            GetBucketCorsRequest.builder().bucket(bucketName)
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

        BucketLifecycleConfiguration lifecycleConfig = s3.getBucketLifecycleConfiguration(
            GetBucketLifecycleConfigurationRequest.builder().bucket(bucketName)
                .build());

        String bucketLocation = s3.getBucketLocation(
            GetBucketLocationRequest.builder().bucket(bucketName)
                .build());

        BucketLoggingConfiguration loggingConfig = s3.getBucketLogging(
            GetBucketLoggingRequest.builder().bucket(bucketName)
                .build());

        GetBucketMetricsConfigurationResponse metricsConfig = s3.getBucketMetricsConfiguration(
            GetBucketMetricsConfigurationRequest.builder()
                .build());

        GetBucketMetricsConfigurationResponse metricsConfig2 = s3.getBucketMetricsConfiguration(
            GetBucketMetricsConfigurationRequest.builder().bucket("1").id("2")
                .build());

        NotificationConfiguration notificationConfig = s3.getBucketNotificationConfiguration(
            GetBucketNotificationConfigurationRequest.builder().bucket(bucketName)
                .build());

        GetBucketOwnershipControlsResponse ownershipControls = s3.getBucketOwnershipControls(
            GetBucketOwnershipControlsRequest.builder()
                .build());

        BucketPolicy bucketPolicy = s3.getBucketPolicy(
            GetBucketPolicyRequest.builder().bucket(bucketName)
                .build());

        GetBucketPolicyStatusResponse policyStatus = s3.getBucketPolicyStatus(
            GetBucketPolicyStatusRequest.builder()
                .build());

        ReplicationConfiguration replicationConfig = s3.getBucketReplication(
            GetBucketReplicationRequest.builder().bucket(bucketName)
                .build());

        Tagging taggingConfig = s3.getBucketTagging(
            GetBucketTaggingRequest.builder().bucket(bucketName)
                .build());

        BucketVersioningConfiguration versioningConfig = s3.getBucketVersioning(
            GetBucketVersioningRequest.builder().bucket(bucketName)
                .build());

        WebsiteConfiguration websiteConfig = s3.getBucketWebsite(
            GetBucketWebsiteRequest.builder().bucket(bucketName)
                .build());

        ResponseInputStream<GetObjectResponse> s3Object1 = s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(objectKey)
                .build());

        ResponseInputStream<GetObjectResponse> s3Object2 = s3.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(objectKey).versionId("3")
                .build());

        // Not. supported
        // S3ObjectId s3ObjectId = new S3ObjectId(bucketName, objectKey);
        // S3Object s3Object3 = s3.getObject(
        //         new GetObjectRequest(s3ObjectId));
        //
        // S3Object s3Object4 = s3.getObject(
        //         new GetObjectRequest(bucketName, objectKey, true));

        AccessControlList objectAcl = s3.getObjectAcl(
            GetObjectAclRequest.builder().bucket(bucketName).key(objectKey)
                .build());

        AccessControlList objectAcl2 = s3.getObjectAcl(
            GetObjectAclRequest.builder().bucket(bucketName).key(objectKey).versionId("3")
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

        Owner owner = s3.getS3AccountOwner(
            GetS3AccountOwnerRequest.builder()
                .build());

        s3.headBucket(
            HeadBucketRequest.builder().bucket(bucketName)
                .build());
        // Not supported
        // ObjectMetadata o1 = new ObjectMetadata();
        // InitiateMultipartUploadResult multipartUploadResult = s3.initiateMultipartUpload(
        //      new InitiateMultipartUploadRequest(bucketName, objectKey, o1));
        //
        // InitiateMultipartUploadResult multipartUploadResult2 = s3.initiateMultipartUpload(
        //      new InitiateMultipartUploadRequest(bucketName, objectKey));

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

        // ListBucketsPaginatedResult bucketsPaginated = s3.listBuckets(
        //         new ListBucketsPaginatedRequest());

        List<Bucket> buckets = s3.listBuckets(
            ListBucketsRequest.builder()
                .build());

        // Not supported
        // MultipartUploadListing multipartUploads = s3.listMultipartUploads(
        //                new ListMultipartUploadsRequest(bucketName));
        //
        // ObjectListing nextBatchObjects = s3.listNextBatchOfObjects(
        //                new ListNextBatchOfObjectsRequest(new ObjectListing()));
        //
        // VersionListing nextBatchVersions = s3.listNextBatchOfVersions(
        //                new ListNextBatchOfVersionsRequest(new VersionListing()));

        ListObjectsResponse objectListing = s3.listObjects(
            ListObjectsRequest.builder()
                .build());

        ListObjectsResponse objectListing2 = s3.listObjects(
            ListObjectsRequest.builder().bucket("1").prefix("2").marker("3").delimiter("4").maxKeys(4)
                .build());
        // Not supported
        // s3.putObject(
        //         new PutObjectRequest(bucketName, objectKey, new File("/path/to/file.txt")));
        //
        // s3.putObject(
        //         new PutObjectRequest(bucketName, objectKey, "/path/to/file.txt"));
        //
        // InputStream inputStream = new InputStream() {
        //     @Override
        //     public int read() throws IOException {
        //         return 0;
        //     }
        // };
        // ObjectMetadata o1 = new ObjectMetadata();
        // s3.putObject(
        //         new PutObjectRequest(bucketName, objectKey, inputStream, o1));

        // s3.restoreObject(
        //         new RestoreObjectRequest(bucketName, objectKey));
        // s3.restoreObject(
        //         new RestoreObjectRequest(bucketName, objectKey, 2));
        //
        // s3.restoreObjectV2(
        //         new RestoreObjectRequest(bucketName, objectKey, 3));

        s3.selectObjectContent(
            SelectObjectContentRequest.builder()
                .build());

        s3.putBucketAccelerateConfiguration(
            PutBucketAccelerateConfigurationRequest.builder().bucket(bucketName).accelerateConfiguration(new AccelerateConfiguration(BucketAccelerateStatus.ENABLED))
                .build());
        // not supported
        // s3.setBucketAcl(
        //          new SetBucketAclRequest(bucketName, new AccessControlList()));
        //
        // s3.setBucketAcl(
        //          new SetBucketAclRequest(bucketName, CannedAccessControlList.Private));
        ////
        s3.putBucketAnalyticsConfiguration(
            PutBucketAnalyticsConfigurationRequest.builder()
                .build());
        s3.putBucketAnalyticsConfiguration(
            PutBucketAnalyticsConfigurationRequest.builder().bucket("XXXX").analyticsConfiguration(AnalyticsConfiguration.builder()
                .build())
                .build());


        s3.putBucketLifecycleConfiguration(
            PutBucketLifecycleConfigurationRequest.builder().bucket(bucketName).lifecycleConfiguration(BucketLifecycleConfiguration.builder()
                .build())
                .build());
        //
        // s3.setBucketLoggingConfiguration(
        //          new SetBucketLoggingConfigurationRequest(bucketName,
        //                  new BucketLoggingConfiguration()));

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

        s3.bucketPolicy(
            PutBucketPolicyRequest.builder()
                .build());
        s3.bucketPolicy(
            new PutBucketPolicyRequest(bucketName, "XX"));

        s3.putBucketReplication(
            PutBucketReplicationRequest.builder()
                .build());
        s3.putBucketReplication(
            PutBucketReplicationRequest.builder().bucket(bucketName).replicationConfiguration(ReplicationConfiguration.builder()
                .build())
                .build());

        s3.putBucketTagging(
            PutBucketTaggingRequest.builder().bucket(bucketName).tagging(Tagging.builder()
                .build())
                .build());

        //        s3.setBucketVersioningConfiguration(
        //                new SetBucketVersioningConfigurationRequest(bucketName,new BucketVersioningConfiguration()));
        //        MultiFactorAuthentication mfa = new MultiFactorAuthentication("1","2");
        //        s3.setBucketVersioningConfiguration(
        //                new SetBucketVersioningConfigurationRequest(bucketName,new BucketVersioningConfiguration(),mfa));

        s3.putBucketWebsite(
            PutBucketWebsiteRequest.builder().bucket(bucketName).websiteConfiguration(new WebsiteConfiguration("1"))
                .build());
        ////
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, new AccessControlList()));
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, CannedAccessControlList.Private));
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, "3", new AccessControlList()));
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, "3", CannedAccessControlList.Private));

        s3.objectLegalHold(
            SetObjectLegalHoldRequest.builder()
                .build());

        s3.objectLockConfiguration(
            SetObjectLockConfigurationRequest.builder()
                .build());

        s3.objectRetention(
            SetObjectRetentionRequest.builder()
                .build());

        // s3.setObjectTagging(
        //     new SetObjectTaggingRequest(bucketName, objectKey,
        //                                 new ObjectTagging(new ArrayList<>())));
        //
        // s3.setObjectTagging(
        //     new SetObjectTaggingRequest(bucketName, objectKey, "3",
        //                                 new ObjectTagging(new ArrayList<>())));
        //
        // //        s3.setPublicAccessBlock(
        // //                new SetPublicAccessBlockRequest());
        // ////
        // s3.setRequestPaymentConfiguration(
        //     new SetRequestPaymentConfigurationRequest(bucketName,
        //                                               new RequestPaymentConfiguration(RequestPaymentConfiguration.Payer.Requester)));

        s3.uploadPart(
            UploadPartRequest.builder()
                .build());

        s3.writeGetObjectResponse(
            WriteGetObjectResponseRequest.builder()
                .build());

        s3.listParts(new ListPartsRequest(bucketName, objectKey, "3"));
    }
}