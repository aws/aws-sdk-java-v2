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
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketAccelerateConfiguration;
import com.amazonaws.services.s3.model.BucketAccelerateStatus;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketReplicationConfiguration;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CORSRule;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.GetBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.SetBucketCrossOriginConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketLifecycleConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketNotificationConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketTaggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration;
import com.amazonaws.services.s3.model.intelligenttiering.IntelligentTieringConfiguration;
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration;
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration;
import com.amazonaws.services.s3.model.ownership.OwnershipControls;
import java.util.ArrayList;
import java.util.List;

public class S3 {

    private S3() {

    }

    private void headBucket(AmazonS3 s3, String bucket) {
        HeadBucketRequest headBucketRequest = new HeadBucketRequest(bucket);
        HeadBucketResult headBucketResult = s3.headBucket(headBucketRequest);
        System.out.println(headBucketResult);
    }

    private void createBucket(AmazonS3 s3, String bucket) {
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucket);
        s3.createBucket(createBucketRequest);
    }

    private void deleteBucket(AmazonS3 s3, String bucket) {
        DeleteBucketRequest deleteBucketRequest = new DeleteBucketRequest(bucket);
        s3.deleteBucket(deleteBucketRequest);
    }

    private void getObjectMetaData_to_headObject(AmazonS3 s3) {
        GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest("bucket", "key");
        ObjectMetadata objectMetadata = s3.getObjectMetadata(getObjectMetadataRequest);
        System.out.println(objectMetadata);
    }

    private void initiateMpu_to_createMpu(AmazonS3 s3, String bucket, String key) {
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucket, key);
        InitiateMultipartUploadResult initiateMultipartUploadResult = s3.initiateMultipartUpload(initiateMultipartUploadRequest);
        System.out.println(initiateMultipartUploadResult);
    }

    private void completeMpu(AmazonS3 s3, String bucket, String key) {
        PartETag partETag = new PartETag(7, "etag");
        List<PartETag> partETags = new ArrayList<>();
        partETags.add(partETag);

        CompleteMultipartUploadRequest completeMpuRequest1 =
            new CompleteMultipartUploadRequest().withBucketName(bucket).withKey(key).withPartETags(partETags);

        CompleteMultipartUploadRequest completeMpuRequest2 =
            new CompleteMultipartUploadRequest(bucket, key, "uploadId", partETags);
    }

    private void listObjects(AmazonS3 s3, String bucket) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucket);
        ObjectListing objectListing = s3.listObjects(listObjectsRequest);
        System.out.println(objectListing);
    }

    private void listObjectsV2(AmazonS3 s3, String bucket) {
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucket);
        ListObjectsV2Result listObjectsV2Result = s3.listObjectsV2(listObjectsV2Request);
        System.out.println(listObjectsV2Result);
    }

    private void copyPart(AmazonS3 s3) {
        CopyPartRequest copyPartRequest = new CopyPartRequest().withSourceBucketName("sourceBucket").withSourceKey("sourceKey").withDestinationBucketName("desBucket").withDestinationKey("desKey");
        CopyPartResult copyPartResult = s3.copyPart(copyPartRequest);
    }

    private void cors(AmazonS3 s3, String bucket) {
        CORSRule corsRule = new CORSRule().withId("id").withMaxAgeSeconds(99);
        BucketCrossOriginConfiguration cors = new BucketCrossOriginConfiguration().withRules(corsRule);
        s3.setBucketCrossOriginConfiguration("bucket", cors);
        SetBucketCrossOriginConfigurationRequest setBucketCrossOriginConfigurationRequest =
            new SetBucketCrossOriginConfigurationRequest(bucket, cors);
        s3.setBucketCrossOriginConfiguration(setBucketCrossOriginConfigurationRequest);

        GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest =
            new GetBucketCrossOriginConfigurationRequest(bucket);
        s3.getBucketCrossOriginConfiguration(getBucketCrossOriginConfigurationRequest);

        DeleteBucketCrossOriginConfigurationRequest deleteBucketCrossOriginConfigurationRequest =
            new DeleteBucketCrossOriginConfigurationRequest(bucket);
        s3.deleteBucketCrossOriginConfiguration(deleteBucketCrossOriginConfigurationRequest);
    }

    private void singleBucketArgMethods(AmazonS3 s3, String bucket) {
        s3.createBucket(bucket);
        s3.deleteBucket(bucket);
        s3.listObjects(bucket);
        s3.listObjectsV2(bucket);
        s3.getBucketCrossOriginConfiguration(bucket);
        s3.deleteBucketCrossOriginConfiguration(bucket);
        s3.getBucketVersioningConfiguration(bucket);
        s3.deleteBucketEncryption(bucket);
        s3.deleteBucketPolicy(bucket);
        s3.getBucketAccelerateConfiguration(bucket);
        s3.getBucketAcl(bucket);
        s3.getBucketEncryption(bucket);
        s3.getBucketLifecycleConfiguration(bucket);
        s3.getBucketNotificationConfiguration(bucket);
        s3.getBucketPolicy(bucket);
        s3.getBucketLocation(bucket);
        s3.deleteBucketLifecycleConfiguration(bucket);
        s3.deleteBucketReplicationConfiguration(bucket);
        s3.deleteBucketTaggingConfiguration(bucket);
        s3.deleteBucketWebsiteConfiguration(bucket);
        s3.getBucketLoggingConfiguration(bucket);
        s3.getBucketReplicationConfiguration(bucket);
        s3.getBucketTaggingConfiguration(bucket);
        s3.getBucketWebsiteConfiguration(bucket);
        s3.disableRequesterPays(bucket);
        s3.enableRequesterPays(bucket);
        s3.isRequesterPaysEnabled(bucket);
    }

    private void bucketKeyArgsMethods(AmazonS3 s3, String bucket, String key) {
        s3.deleteObject(bucket, key);
        s3.getObject(bucket, key);
        s3.getObjectAcl(bucket, key);
        s3.getObjectMetadata(bucket, key);
        s3.getUrl(bucket, key);
        String objectAsString = s3.getObjectAsString(bucket, key);
    }

    private void bucketIdArgsMethods(AmazonS3 s3, String bucket, String id) {
        s3.deleteBucketAnalyticsConfiguration(bucket, id);
        s3.deleteBucketIntelligentTieringConfiguration(bucket, id);
        s3.deleteBucketInventoryConfiguration(bucket, id);
        s3.deleteBucketMetricsConfiguration(bucket, id);
        s3.getBucketAnalyticsConfiguration(bucket, id);
        s3.getBucketIntelligentTieringConfiguration(bucket, id);
        s3.getBucketInventoryConfiguration(bucket, id);
        s3.getBucketMetricsConfiguration(bucket, id);
    }

    private void bucketPrefixArgsMethods(AmazonS3 s3, String bucket, String prefix) {
        s3.listObjects(bucket, prefix);
        s3.listObjectsV2(bucket, prefix);
        s3.listVersions(bucket, prefix);
    }

    private void enumArgMethods(AmazonS3 s3) {
        BucketAccelerateConfiguration accelerateConfig = new BucketAccelerateConfiguration(BucketAccelerateStatus.Suspended);
        s3.setBucketAccelerateConfiguration("bucket", accelerateConfig);

        StorageClass storageClass = StorageClass.DeepArchive;
        s3.changeObjectStorageClass("bucket", "key", storageClass);
    }

    private void variousMethods(AmazonS3 s3) {
        s3.deleteVersion("bucket", "key", "versionId");
        s3.copyObject("sourceBucket", "sourceKey", "destBucket", "destKey");
        s3.listVersions("bucket", "prefix", "keyMarker", "versionId", "delimiter", 22);
        s3.setBucketPolicy("bucket", "policyText");
        s3.restoreObject("bucket", "key", 98);
        s3.setObjectRedirectLocation("bucket", "key", "redirectLocation");
        s3.createBucket("bucket", "us-west-2");
        s3.getObjectAcl("bucket", "key", "versionId");
        List<Bucket> buckets = s3.listBuckets();
    }

    private void pojosWithConstructorArgs(String bucket) {
        AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucket, "key", "versionId");
        SetBucketLifecycleConfigurationRequest lifecycleRequest = new SetBucketLifecycleConfigurationRequest(bucket, new BucketLifecycleConfiguration());
        SetBucketNotificationConfigurationRequest notificationRequest = new SetBucketNotificationConfigurationRequest(bucket, new BucketNotificationConfiguration());
        SetBucketTaggingConfigurationRequest tagRequest = new SetBucketTaggingConfigurationRequest(bucket, new BucketTaggingConfiguration());
        SetBucketWebsiteConfigurationRequest websiteRequest = new SetBucketWebsiteConfigurationRequest(bucket, new BucketWebsiteConfiguration());
    }

    private void setBucketConfigs(AmazonS3 s3, String bucket) {
        s3.setBucketAnalyticsConfiguration(bucket, new AnalyticsConfiguration());
        s3.setBucketIntelligentTieringConfiguration(bucket, new IntelligentTieringConfiguration());
        s3.setBucketInventoryConfiguration(bucket, new InventoryConfiguration());
        s3.setBucketLifecycleConfiguration(bucket, new BucketLifecycleConfiguration());
        s3.setBucketMetricsConfiguration(bucket, new MetricsConfiguration());
        s3.setBucketNotificationConfiguration(bucket, new BucketNotificationConfiguration());
        s3.setBucketOwnershipControls(bucket, new OwnershipControls());
        s3.setBucketReplicationConfiguration(bucket, new BucketReplicationConfiguration());
        s3.setBucketTaggingConfiguration(bucket, new BucketTaggingConfiguration());
        s3.setBucketWebsiteConfiguration(bucket, new BucketWebsiteConfiguration());
    }

    private void setBucketNameTest(AmazonS3 s3, String bucket) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, "key").withBucketName(bucket);
    }
}