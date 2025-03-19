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


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration;
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration;
import com.amazonaws.services.s3.model.ownership.OwnershipControls;
import java.util.ArrayList;
import java.util.List;

public class S3RequestConstructor {
    AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    String bucketName = "bucketName";
    String objectKey = "objectKey";

    private S3RequestConstructor() {

    }

    public void requestconstructor() {

        s3.abortMultipartUpload(new AbortMultipartUploadRequest("name","key","upload"));
        s3.completeMultipartUpload(new CompleteMultipartUploadRequest("name","key","upload",new ArrayList<>()));

        s3.copyObject(new CopyObjectRequest());
        s3.copyObject(new CopyObjectRequest("1","2","3","4"));
        s3.copyObject(new CopyObjectRequest("1","2","3","4","5"));
        s3.copyPart(new CopyPartRequest());

        s3.deleteBucket(new DeleteBucketRequest("name"));

        DeleteBucketAnalyticsConfigurationResult dbacR1 =
            s3.deleteBucketAnalyticsConfiguration(new DeleteBucketAnalyticsConfigurationRequest());
        DeleteBucketAnalyticsConfigurationResult dbacR2 =
            s3.deleteBucketAnalyticsConfiguration(new DeleteBucketAnalyticsConfigurationRequest("1","2"));

        DeleteBucketEncryptionResult dbeR1 = s3.deleteBucketEncryption(new DeleteBucketEncryptionRequest());

        DeleteBucketIntelligentTieringConfigurationResult dbitcR1 =
            s3.deleteBucketIntelligentTieringConfiguration(new DeleteBucketIntelligentTieringConfigurationRequest());

        s3.deleteBucketIntelligentTieringConfiguration(new DeleteBucketIntelligentTieringConfigurationRequest("1","2"));

        DeleteBucketInventoryConfigurationResult dbicR1 =
            s3.deleteBucketInventoryConfiguration(new DeleteBucketInventoryConfigurationRequest());

        s3.deleteBucketInventoryConfiguration(new DeleteBucketInventoryConfigurationRequest("1","2"));

        s3.deleteBucketLifecycleConfiguration(new DeleteBucketLifecycleConfigurationRequest("name"));

        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest());
        s3.deleteBucketMetricsConfiguration(new DeleteBucketMetricsConfigurationRequest("1","2"));

        s3.deleteBucketOwnershipControls(new DeleteBucketOwnershipControlsRequest());

        s3.deleteBucketPolicy(new DeleteBucketPolicyRequest("name"));

        s3.deleteBucketTaggingConfiguration(new DeleteBucketTaggingConfigurationRequest(bucketName));

        s3.deleteBucketWebsiteConfiguration(new DeleteBucketWebsiteConfigurationRequest(bucketName));

        s3.deleteObject(new DeleteObjectRequest(bucketName, objectKey));

        //s3.deleteObjects(new DeleteObjectsRequest(bucketName));

        s3.deleteObjectTagging(new DeleteObjectTaggingRequest(bucketName, objectKey));

        s3.deletePublicAccessBlock(new DeletePublicAccessBlockRequest());

        // Not supported
        // s3.deleteVersion(new DeleteVersionRequest(bucketName, objectKey, "version-id-here"));
        // MultiFactorAuthentication mfa = new MultiFactorAuthentication("SN","token");
        // s3.deleteVersion(new DeleteVersionRequest(bucketName, objectKey, "version-id-here", mfa));

        //INCOMPATIBLE RESPONSE
        s3.getBucketAccelerateConfiguration(new GetBucketAccelerateConfigurationRequest(bucketName));

        //INCOMPATIBLE RESPONSE
        s3.getBucketAcl(new GetBucketAclRequest(bucketName));

        GetBucketAnalyticsConfigurationResult analyticsConfig = s3.getBucketAnalyticsConfiguration(
            new GetBucketAnalyticsConfigurationRequest());

        GetBucketAnalyticsConfigurationResult analyticsConfig2 = s3.getBucketAnalyticsConfiguration(
            new GetBucketAnalyticsConfigurationRequest("1","2"));

        //INCOMPATIBLE RESPONSE
        s3.getBucketCrossOriginConfiguration(new GetBucketCrossOriginConfigurationRequest(bucketName));

        GetBucketEncryptionResult encryptionResult = s3.getBucketEncryption(
            new GetBucketEncryptionRequest());

        GetBucketIntelligentTieringConfigurationResult intelligentTieringConfig = s3.getBucketIntelligentTieringConfiguration(
            new GetBucketIntelligentTieringConfigurationRequest());

        GetBucketIntelligentTieringConfigurationResult intelligentTieringConfig2 = s3.getBucketIntelligentTieringConfiguration(
            new GetBucketIntelligentTieringConfigurationRequest("1","2"));

        GetBucketInventoryConfigurationResult inventoryConfig = s3.getBucketInventoryConfiguration(
            new GetBucketInventoryConfigurationRequest());

        GetBucketInventoryConfigurationResult inventoryConfig2 = s3.getBucketInventoryConfiguration(
            new GetBucketInventoryConfigurationRequest("1","2"));

        //INCOMPATIBLE RESPONSE
        s3.getBucketLifecycleConfiguration(new GetBucketLifecycleConfigurationRequest(bucketName));

        //INCOMPATIBLE RESPONSE
        s3.getBucketLocation(new GetBucketLocationRequest(bucketName));

        //INCOMPATIBLE RESPONSE
        s3.getBucketLoggingConfiguration(new GetBucketLoggingConfigurationRequest(bucketName));

        GetBucketMetricsConfigurationResult metricsConfig = s3.getBucketMetricsConfiguration(
            new GetBucketMetricsConfigurationRequest());

        GetBucketMetricsConfigurationResult metricsConfig2 = s3.getBucketMetricsConfiguration(
            new GetBucketMetricsConfigurationRequest("1","2"));

        //INCOMPATIBLE RESPONSE
        s3.getBucketNotificationConfiguration(new GetBucketNotificationConfigurationRequest(bucketName));

        GetBucketOwnershipControlsResult ownershipControls = s3.getBucketOwnershipControls(
            new GetBucketOwnershipControlsRequest());

        //INCOMPATIBLE RESPONSE
        s3.getBucketPolicy(new GetBucketPolicyRequest(bucketName));

        GetBucketPolicyStatusResult policyStatus = s3.getBucketPolicyStatus(
            new GetBucketPolicyStatusRequest());

        //INCOMPATIBLE RESPONSE
        s3.getBucketReplicationConfiguration(new GetBucketReplicationConfigurationRequest(bucketName));

        //INCOMPATIBLE RESPONSE
        s3.getBucketTaggingConfiguration(new GetBucketTaggingConfigurationRequest(bucketName));

        //INCOMPATIBLE RESPONSE
        s3.getBucketVersioningConfiguration(new GetBucketVersioningConfigurationRequest(bucketName));

        //INCOMPATIBLE RESPONSE
        s3.getBucketWebsiteConfiguration(new GetBucketWebsiteConfigurationRequest(bucketName));

        S3Object s3Object1 = s3.getObject(
            new GetObjectRequest(bucketName, objectKey));

        S3Object s3Object2 = s3.getObject(
            new GetObjectRequest(bucketName, objectKey, "3"));

        // Not supported
        // S3ObjectId s3ObjectId = new S3ObjectId(bucketName, objectKey);
        // S3Object s3Object3 = s3.getObject(
        //         new GetObjectRequest(s3ObjectId));
        //
        // S3Object s3Object4 = s3.getObject(
        //         new GetObjectRequest(bucketName, objectKey, true));

        //INCOMPATIBLE RESPONSE
        s3.getObjectAcl(new GetObjectAclRequest(bucketName, objectKey));

        //INCOMPATIBLE RESPONSE
        s3.getObjectAcl(new GetObjectAclRequest(bucketName, objectKey, "3"));

        GetObjectLegalHoldResult legalHoldResult = s3.getObjectLegalHold(
            new GetObjectLegalHoldRequest());

        GetObjectLockConfigurationResult lockConfigResult = s3.getObjectLockConfiguration(
            new GetObjectLockConfigurationRequest());

        ObjectMetadata objectMetadata = s3.getObjectMetadata(
            new GetObjectMetadataRequest(bucketName, objectKey));

        ObjectMetadata objectMetadata2 = s3.getObjectMetadata(
            new GetObjectMetadataRequest(bucketName, objectKey, "3"));

        GetObjectRetentionResult retentionResult = s3.getObjectRetention(
            new GetObjectRetentionRequest());

        GetObjectTaggingResult taggingResult = s3.getObjectTagging(
            new GetObjectTaggingRequest(bucketName, objectKey));

        GetObjectTaggingResult taggingResult2 = s3.getObjectTagging(
            new GetObjectTaggingRequest(bucketName, objectKey, "3"));

        GetPublicAccessBlockResult publicAccessBlockResult = s3.getPublicAccessBlock(
            new GetPublicAccessBlockRequest());

        //NON EXIST REQUEST
        //s3.getS3AccountOwner(new GetS3AccountOwnerRequest());

        s3.headBucket(new HeadBucketRequest(bucketName));

        // Not supported
        // ObjectMetadata o1 = new ObjectMetadata();
        // InitiateMultipartUploadResult multipartUploadResult = s3.initiateMultipartUpload(
        //      new InitiateMultipartUploadRequest(bucketName, objectKey, o1));
        //
        // InitiateMultipartUploadResult multipartUploadResult2 = s3.initiateMultipartUpload(
        //      new InitiateMultipartUploadRequest(bucketName, objectKey));

        ListBucketAnalyticsConfigurationsResult analyticsConfigs = s3.listBucketAnalyticsConfigurations(
            new ListBucketAnalyticsConfigurationsRequest());

        ListBucketIntelligentTieringConfigurationsResult tieringConfigs = s3.listBucketIntelligentTieringConfigurations(
            new ListBucketIntelligentTieringConfigurationsRequest());

        ListBucketInventoryConfigurationsResult inventoryConfigs = s3.listBucketInventoryConfigurations(
            new ListBucketInventoryConfigurationsRequest());

        ListBucketMetricsConfigurationsResult metricsConfigs = s3.listBucketMetricsConfigurations(
            new ListBucketMetricsConfigurationsRequest());

        // ListBucketsPaginatedResult bucketsPaginated = s3.listBuckets(
        //         new ListBucketsPaginatedRequest());

        //INCOMPATIBLE RESPONSE
        s3.listBuckets(new ListBucketsRequest());

        // Not supported
        // MultipartUploadListing multipartUploads = s3.listMultipartUploads(
        //                new ListMultipartUploadsRequest(bucketName));
        //
        // ObjectListing nextBatchObjects = s3.listNextBatchOfObjects(
        //                new ListNextBatchOfObjectsRequest(new ObjectListing()));
        //
        // VersionListing nextBatchVersions = s3.listNextBatchOfVersions(
        //                new ListNextBatchOfVersionsRequest(new VersionListing()));

        ObjectListing objectListing = s3.listObjects(
            new ListObjectsRequest());

        ObjectListing objectListing2 = s3.listObjects(
            new ListObjectsRequest("1","2","3","4",4));

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

        //INCOMPATIBLE RESPONSE
        //s3.selectObjectContent(new SelectObjectContentRequest());

        // AccelerateConfiguration Builder not supported
        // s3.setBucketAccelerateConfiguration(
        //     new SetBucketAccelerateConfigurationRequest(bucketName,
        //                                                 new BucketAccelerateConfiguration(BucketAccelerateStatus.Enabled)));

        // not supported
        // s3.setBucketAcl(
        //          new SetBucketAclRequest(bucketName, new AccessControlList()));
        //
        // s3.setBucketAcl(
        //          new SetBucketAclRequest(bucketName, CannedAccessControlList.Private));
        ////
        s3.setBucketAnalyticsConfiguration(
            new SetBucketAnalyticsConfigurationRequest());
        s3.setBucketAnalyticsConfiguration(
            new SetBucketAnalyticsConfigurationRequest("XXXX", new AnalyticsConfiguration()));


        s3.setBucketLifecycleConfiguration(
            new SetBucketLifecycleConfigurationRequest(bucketName,
                                                       new BucketLifecycleConfiguration()));
        //
        // s3.setBucketLoggingConfiguration(
        //          new SetBucketLoggingConfigurationRequest(bucketName,
        //                  new BucketLoggingConfiguration()));

        s3.setBucketMetricsConfiguration(
            new SetBucketMetricsConfigurationRequest());
        s3.setBucketMetricsConfiguration(
            new SetBucketMetricsConfigurationRequest(bucketName,new MetricsConfiguration()));

        s3.setBucketNotificationConfiguration(
            new SetBucketNotificationConfigurationRequest(bucketName,
                                                          new BucketNotificationConfiguration()));

        s3.setBucketOwnershipControls(
            new SetBucketOwnershipControlsRequest(bucketName,
                                                  new OwnershipControls()));

        s3.setBucketOwnershipControls(
            new SetBucketOwnershipControlsRequest());

        //NON EXIST METHOD
        //s3.setBucketPolicy(new SetBucketPolicyRequest());
        //NON EXIST METHOD
        //s3.setBucketPolicy(new SetBucketPolicyRequest(bucketName, "XX"));

        s3.setBucketReplicationConfiguration(
            new SetBucketReplicationConfigurationRequest());
        s3.setBucketReplicationConfiguration(
            new SetBucketReplicationConfigurationRequest(bucketName,new BucketReplicationConfiguration()));

        s3.setBucketTaggingConfiguration(
            new SetBucketTaggingConfigurationRequest(bucketName,new BucketTaggingConfiguration()));

        //        s3.setBucketVersioningConfiguration(
        //                new SetBucketVersioningConfigurationRequest(bucketName,new BucketVersioningConfiguration()));
        //        MultiFactorAuthentication mfa = new MultiFactorAuthentication("1","2");
        //        s3.setBucketVersioningConfiguration(
        //                new SetBucketVersioningConfigurationRequest(bucketName,new BucketVersioningConfiguration(),mfa));

        //INCOMPATIBLE RESPONSE
        //s3.setBucketWebsiteConfiguration(new SetBucketWebsiteConfigurationRequest(bucketName,new BucketWebsiteConfiguration
        // ("1")));

        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, new AccessControlList()));
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, CannedAccessControlList.Private));
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, "3", new AccessControlList()));
        //        s3.setObjectAcl(
        //                new SetObjectAclRequest(bucketName, objectKey, "3", CannedAccessControlList.Private));

        //INCOMPATIBLE RESPONSE
        //s3.setObjectLegalHold(new SetObjectLegalHoldRequest());

        //INCOMPATIBLE RESPONSE
        //s3.setObjectLockConfiguration(new SetObjectLockConfigurationRequest());

        //INCOMPATIBLE RESPONSE
        //s3.setObjectRetention(new SetObjectRetentionRequest());

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

        //INCOMPATIBLE RESPONSE
        //s3.uploadPart(new UploadPartRequest());

        //INCOMPATIBLE RESPONSE
        //s3.writeGetObjectResponse(new WriteGetObjectResponseRequest());

        //INCOMPATIBLE RESPONSE
        //s3.listParts(new ListPartsRequest(bucketName, objectKey, "3"));
    }
}