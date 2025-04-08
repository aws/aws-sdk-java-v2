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
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketReplicationConfiguration;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketAnalyticsConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketEncryptionRequest;
import com.amazonaws.services.s3.model.DeleteBucketEncryptionResult;
import com.amazonaws.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketIntelligentTieringConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.DeleteBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketOwnershipControlsRequest;
import com.amazonaws.services.s3.model.DeleteBucketPolicyRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectTaggingRequest;
import com.amazonaws.services.s3.model.DeletePublicAccessBlockRequest;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GetBucketAccelerateConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketAclRequest;
import com.amazonaws.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketAnalyticsConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketEncryptionRequest;
import com.amazonaws.services.s3.model.GetBucketEncryptionResult;
import com.amazonaws.services.s3.model.GetBucketIntelligentTieringConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketIntelligentTieringConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketInventoryConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketInventoryConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.GetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketMetricsConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketMetricsConfigurationResult;
import com.amazonaws.services.s3.model.GetBucketNotificationConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketOwnershipControlsRequest;
import com.amazonaws.services.s3.model.GetBucketOwnershipControlsResult;
import com.amazonaws.services.s3.model.GetBucketPolicyRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyStatusRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyStatusResult;
import com.amazonaws.services.s3.model.GetBucketReplicationConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.GetBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.GetObjectAclRequest;
import com.amazonaws.services.s3.model.GetObjectLegalHoldRequest;
import com.amazonaws.services.s3.model.GetObjectLegalHoldResult;
import com.amazonaws.services.s3.model.GetObjectLockConfigurationRequest;
import com.amazonaws.services.s3.model.GetObjectLockConfigurationResult;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRetentionRequest;
import com.amazonaws.services.s3.model.GetObjectRetentionResult;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.GetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.GetPublicAccessBlockResult;
import com.amazonaws.services.s3.model.GetRequestPaymentConfigurationRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.ListBucketAnalyticsConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketAnalyticsConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketIntelligentTieringConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketIntelligentTieringConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketInventoryConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketInventoryConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketMetricsConfigurationsRequest;
import com.amazonaws.services.s3.model.ListBucketMetricsConfigurationsResult;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ownership.OwnershipControls;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketAnalyticsConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketMetricsConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketNotificationConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketOwnershipControlsRequest;
import com.amazonaws.services.s3.model.SetBucketReplicationConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetObjectAclRequest;

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

        s3.deleteObjects(new DeleteObjectsRequest(bucketName));

        s3.deleteObjectTagging(new DeleteObjectTaggingRequest(bucketName, objectKey));

        s3.deletePublicAccessBlock(new DeletePublicAccessBlockRequest());

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

        s3.headBucket(new HeadBucketRequest(bucketName));


        ListBucketAnalyticsConfigurationsResult analyticsConfigs = s3.listBucketAnalyticsConfigurations(
            new ListBucketAnalyticsConfigurationsRequest());

        ListBucketIntelligentTieringConfigurationsResult tieringConfigs = s3.listBucketIntelligentTieringConfigurations(
            new ListBucketIntelligentTieringConfigurationsRequest());

        ListBucketInventoryConfigurationsResult inventoryConfigs = s3.listBucketInventoryConfigurations(
            new ListBucketInventoryConfigurationsRequest());

        ListBucketMetricsConfigurationsResult metricsConfigs = s3.listBucketMetricsConfigurations(
            new ListBucketMetricsConfigurationsRequest());

        //INCOMPATIBLE RESPONSE
        s3.listBuckets(new ListBucketsRequest());

        ObjectListing objectListing = s3.listObjects(
            new ListObjectsRequest());

        ObjectListing objectListing2 = s3.listObjects(
            new ListObjectsRequest("1","2","3","4",4));

        s3.setBucketAnalyticsConfiguration(
            new SetBucketAnalyticsConfigurationRequest());
        s3.setBucketAnalyticsConfiguration(
            new SetBucketAnalyticsConfigurationRequest("name", new AnalyticsConfiguration()));


        s3.setBucketLifecycleConfiguration(
            new SetBucketLifecycleConfigurationRequest(bucketName,
                                                       new BucketLifecycleConfiguration()));

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

        s3.setBucketReplicationConfiguration(
            new SetBucketReplicationConfigurationRequest());
        s3.setBucketReplicationConfiguration(
            new SetBucketReplicationConfigurationRequest(bucketName,new BucketReplicationConfiguration()));

        s3.setBucketTaggingConfiguration(
            new SetBucketTaggingConfigurationRequest(bucketName,new BucketTaggingConfiguration()));

        DeleteVersionRequest deleteVersionRequest = new DeleteVersionRequest(bucketName, objectKey, "id");

        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectKey, "id");

        RestoreObjectRequest restoreObjectRequest = new RestoreObjectRequest(bucketName, objectKey);

        GetRequestPaymentConfigurationRequest getRequestPaymentConfigurationRequest =
            new GetRequestPaymentConfigurationRequest(bucketName);
    }
}