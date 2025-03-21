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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AccelerateConfiguration;
import software.amazon.awssdk.services.s3.model.AnalyticsConfiguration;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAccelerateStatus;
import software.amazon.awssdk.services.s3.model.BucketLifecycleConfiguration;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAclRequest;
import software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.IntelligentTieringConfiguration;
import software.amazon.awssdk.services.s3.model.InventoryConfiguration;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.MetricsConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.OwnershipControls;
import software.amazon.awssdk.services.s3.model.Payer;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest;
import software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentRequest;
import software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.ReplicationConfiguration;
import software.amazon.awssdk.services.s3.model.RequestPaymentConfiguration;
import software.amazon.awssdk.services.s3.model.RestoreObjectRequest;
import software.amazon.awssdk.services.s3.model.RestoreRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.WebsiteConfiguration;

public class S3 {

    private S3() {

    }

    private void headBucket(S3Client s3, String bucket) {
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucket)
            .build();
        HeadBucketResponse headBucketResult = s3.headBucket(headBucketRequest);
        System.out.println(headBucketResult);
    }

    private void createBucket(S3Client s3, String bucket) {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucket)
            .build();
        s3.createBucket(createBucketRequest);
    }

    private void deleteBucket(S3Client s3, String bucket) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket)
            .build();
        s3.deleteBucket(deleteBucketRequest);
    }

    private void getObjectMetaData_to_headObject(S3Client s3) {
        HeadObjectRequest getObjectMetadataRequest = HeadObjectRequest.builder().bucket("bucket").key("key")
            .build();
        HeadObjectResponse objectMetadata = s3.headObject(getObjectMetadataRequest);
        System.out.println(objectMetadata);
    }

    private void initiateMpu_to_createMpu(S3Client s3, String bucket, String key) {
        CreateMultipartUploadRequest initiateMultipartUploadRequest = CreateMultipartUploadRequest.builder().bucket(bucket).key(key)
            .build();
        CreateMultipartUploadResponse initiateMultipartUploadResult = s3.createMultipartUpload(initiateMultipartUploadRequest);
        System.out.println(initiateMultipartUploadResult);
    }

    private void completeMpu(S3Client s3, String bucket, String key) {
        CompletedPart partETag = CompletedPart.builder().partNumber(7).eTag("etag")
            .build();
        List<CompletedPart> partETags = new ArrayList<>();
        partETags.add(partETag);

        CompleteMultipartUploadRequest completeMpuRequest1 =
            CompleteMultipartUploadRequest.builder().bucket(bucket).key(key).multipartUpload(CompletedMultipartUpload.builder().parts(partETags).build())
            .build();

        CompleteMultipartUploadRequest completeMpuRequest2 =
            CompleteMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId("uploadId").multipartUpload(CompletedMultipartUpload.builder().parts(partETags).build())
                .build();
    }

    private void listObjects(S3Client s3, String bucket) {
        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(bucket)
            .build();
        ListObjectsRequest listObjectsRequest2 = ListObjectsRequest.builder().bucket("bucketName").prefix("prefix").marker("marker").delimiter("delimiter").maxKeys(4)
            .build();

        ListObjectsResponse objectListing = s3.listObjects(listObjectsRequest);
        ListObjectsResponse objectListing2 = s3.listObjects(listObjectsRequest2);
        System.out.println(objectListing);
    }

    private void listObjectsV2(S3Client s3, String bucket) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket)
            .build();
        ListObjectsV2Response listObjectsV2Result = s3.listObjectsV2(listObjectsV2Request);
        System.out.println(listObjectsV2Result);
    }

    private void copyPart(S3Client s3) {
        UploadPartCopyRequest copyPartRequest = UploadPartCopyRequest.builder().sourceBucket("sourceBucket").sourceKey("sourceKey").destinationBucket("desBucket").destinationKey("desKey")
            .build();
        UploadPartCopyResponse copyPartResult = s3.uploadPartCopy(copyPartRequest);
    }

    private void cors(S3Client s3, String bucket) {
        CORSRule corsRule = CORSRule.builder().id("id").maxAgeSeconds(99)
            .build();
        CORSConfiguration cors = CORSConfiguration.builder().corsRules(corsRule)
            .build();
        s3.putBucketCors(PutBucketCorsRequest.builder().bucket("bucket").corsConfiguration(cors)
            .build());
        PutBucketCorsRequest setBucketCrossOriginConfigurationRequest =
            PutBucketCorsRequest.builder().bucket(bucket).corsConfiguration(cors)
                .build();
        s3.putBucketCors(setBucketCrossOriginConfigurationRequest);

        GetBucketCorsRequest getBucketCrossOriginConfigurationRequest =
            GetBucketCorsRequest.builder().bucket(bucket)
                .build();
        s3.getBucketCors(getBucketCrossOriginConfigurationRequest);

        DeleteBucketCorsRequest deleteBucketCrossOriginConfigurationRequest =
            DeleteBucketCorsRequest.builder().bucket(bucket)
                .build();
        s3.deleteBucketCors(deleteBucketCrossOriginConfigurationRequest);
    }

    private void singleBucketArgMethods(S3Client s3, String bucket) {
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket)
            .build());
        s3.listObjects(ListObjectsRequest.builder().bucket(bucket)
            .build());
        s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket)
            .build());
        s3.getBucketCors(GetBucketCorsRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketCors(DeleteBucketCorsRequest.builder().bucket(bucket)
            .build());
        s3.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketEncryption(DeleteBucketEncryptionRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketPolicy(DeleteBucketPolicyRequest.builder().bucket(bucket)
            .build());
        s3.getBucketAccelerateConfiguration(GetBucketAccelerateConfigurationRequest.builder().bucket(bucket)
            .build());
        s3.getBucketAcl(GetBucketAclRequest.builder().bucket(bucket)
            .build());
        s3.getBucketEncryption(GetBucketEncryptionRequest.builder().bucket(bucket)
            .build());
        s3.getBucketLifecycleConfiguration(GetBucketLifecycleConfigurationRequest.builder().bucket(bucket)
            .build());
        s3.getBucketNotificationConfiguration(GetBucketNotificationConfigurationRequest.builder().bucket(bucket)
            .build());
        s3.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucket)
            .build());
        s3.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketLifecycle(DeleteBucketLifecycleRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketReplication(DeleteBucketReplicationRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketTagging(DeleteBucketTaggingRequest.builder().bucket(bucket)
            .build());
        s3.deleteBucketWebsite(DeleteBucketWebsiteRequest.builder().bucket(bucket)
            .build());
        s3.getBucketLogging(GetBucketLoggingRequest.builder().bucket(bucket)
            .build());
        s3.getBucketReplication(GetBucketReplicationRequest.builder().bucket(bucket)
            .build());
        s3.getBucketTagging(GetBucketTaggingRequest.builder().bucket(bucket)
            .build());
        s3.getBucketWebsite(GetBucketWebsiteRequest.builder().bucket(bucket)
            .build());
        s3.putBucketRequestPayment(PutBucketRequestPaymentRequest.builder().bucket(bucket).requestPaymentConfiguration(RequestPaymentConfiguration.builder().payer(Payer.BUCKET_OWNER).build()).build());
        s3.putBucketRequestPayment(PutBucketRequestPaymentRequest.builder().bucket(bucket).requestPaymentConfiguration(RequestPaymentConfiguration.builder().payer(Payer.REQUESTER).build()).build());
        s3.getBucketRequestPayment(GetBucketRequestPaymentRequest.builder().bucket(bucket).build()).payer().toString().equals("Requester");
    }

    private void bucketKeyArgsMethods(S3Client s3, String bucket, String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key)
            .build());
        s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key)
            .build());
        s3.getObjectAcl(GetObjectAclRequest.builder().bucket(bucket).key(key)
            .build());
        s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key)
            .build());
        s3.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build());
        String objectAsString = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asUtf8String();
    }

    private void bucketIdArgsMethods(S3Client s3, String bucket, String id) {
        s3.deleteBucketAnalyticsConfiguration(DeleteBucketAnalyticsConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.deleteBucketIntelligentTieringConfiguration(DeleteBucketIntelligentTieringConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.deleteBucketMetricsConfiguration(DeleteBucketMetricsConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.getBucketAnalyticsConfiguration(GetBucketAnalyticsConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.getBucketIntelligentTieringConfiguration(GetBucketIntelligentTieringConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
        s3.getBucketMetricsConfiguration(GetBucketMetricsConfigurationRequest.builder().bucket(bucket).id(id)
            .build());
    }

    private void bucketPrefixArgsMethods(S3Client s3, String bucket, String prefix) {
        s3.listObjects(ListObjectsRequest.builder().bucket(bucket).prefix(prefix)
            .build());
        s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix)
            .build());
        s3.listObjectVersions(ListObjectVersionsRequest.builder().bucket(bucket).prefix(prefix)
            .build());
    }

    private void enumArgMethods(S3Client s3) {
        AccelerateConfiguration accelerateConfig = AccelerateConfiguration.builder().status(BucketAccelerateStatus.SUSPENDED)
            .build();
        s3.putBucketAccelerateConfiguration(PutBucketAccelerateConfigurationRequest.builder().bucket("bucket").accelerateConfiguration(accelerateConfig)
            .build());

        StorageClass storageClass = StorageClass.DEEP_ARCHIVE;
        s3.copyObject(CopyObjectRequest.builder().sourceBucket("bucket").sourceKey("key").destinationBucket("bucket").destinationKey("key").storageClass(storageClass).build());
    }

    private void variousMethods(S3Client s3) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket("bucket").key("key").versionId("versionId")
            .build());
        s3.copyObject(CopyObjectRequest.builder().sourceBucket("sourceBucket").sourceKey("sourceKey").destinationBucket("destBucket").destinationKey("destKey")
            .build());
        s3.listObjectVersions(ListObjectVersionsRequest.builder().bucket("bucket").prefix("prefix").keyMarker("keyMarker").versionIdMarker("versionId").delimiter("delimiter").maxKeys(22)
            .build());
        s3.putBucketPolicy(PutBucketPolicyRequest.builder().bucket("bucket").policy("policyText")
            .build());
        s3.restoreObject(RestoreObjectRequest.builder().bucket("bucket").key("key").restoreRequest(RestoreRequest.builder().days(98).build()).build());
        s3.copyObject(CopyObjectRequest.builder().sourceBucket("bucket").sourceKey("key").destinationBucket("bucket").destinationKey("key").websiteRedirectLocation("redirectLocation").build());
        s3.createBucket(CreateBucketRequest.builder().bucket("bucket").createBucketConfiguration(CreateBucketConfiguration.builder().locationConstraint("us-west-2").build()).build());
        s3.getObjectAcl(GetObjectAclRequest.builder().bucket("bucket").key("key").versionId("versionId")
            .build());
        List<Bucket> buckets = s3.listBuckets().buckets();
    }

    private void pojosWithConstructorArgs(String bucket) {
        AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder().bucket(bucket).key("key").uploadId("versionId")
            .build();
        PutBucketLifecycleConfigurationRequest lifecycleRequest = PutBucketLifecycleConfigurationRequest.builder().bucket(bucket).lifecycleConfiguration(BucketLifecycleConfiguration.builder()
            .build())
            .build();
        PutBucketNotificationConfigurationRequest notificationRequest = PutBucketNotificationConfigurationRequest.builder().bucket(bucket).notificationConfiguration(NotificationConfiguration.builder()
            .build())
            .build();
        PutBucketTaggingRequest tagRequest = PutBucketTaggingRequest.builder().bucket(bucket).tagging(Tagging.builder()
            .build())
            .build();
        PutBucketWebsiteRequest websiteRequest = PutBucketWebsiteRequest.builder().bucket(bucket).websiteConfiguration(WebsiteConfiguration.builder()
            .build())
            .build();
    }

    private void setBucketConfigs(S3Client s3, String bucket) {
        s3.putBucketAnalyticsConfiguration(PutBucketAnalyticsConfigurationRequest.builder().bucket(bucket).analyticsConfiguration(AnalyticsConfiguration.builder()
            .build())
            .build());
        s3.putBucketIntelligentTieringConfiguration(PutBucketIntelligentTieringConfigurationRequest.builder().bucket(bucket).intelligentTieringConfiguration(IntelligentTieringConfiguration.builder()
            .build())
            .build());
        s3.putBucketInventoryConfiguration(PutBucketInventoryConfigurationRequest.builder().bucket(bucket).inventoryConfiguration(InventoryConfiguration.builder()
            .build())
            .build());
        s3.putBucketLifecycleConfiguration(PutBucketLifecycleConfigurationRequest.builder().bucket(bucket).lifecycleConfiguration(BucketLifecycleConfiguration.builder()
            .build())
            .build());
        s3.putBucketMetricsConfiguration(PutBucketMetricsConfigurationRequest.builder().bucket(bucket).metricsConfiguration(MetricsConfiguration.builder()
            .build())
            .build());
        s3.putBucketNotificationConfiguration(PutBucketNotificationConfigurationRequest.builder().bucket(bucket).notificationConfiguration(NotificationConfiguration.builder()
            .build())
            .build());
        s3.putBucketOwnershipControls(PutBucketOwnershipControlsRequest.builder().bucket(bucket).ownershipControls(OwnershipControls.builder()
            .build())
            .build());
        s3.putBucketReplication(PutBucketReplicationRequest.builder().bucket(bucket).replicationConfiguration(ReplicationConfiguration.builder()
            .build())
            .build());
        s3.putBucketTagging(PutBucketTaggingRequest.builder().bucket(bucket).tagging(Tagging.builder()
            .build())
            .build());
        s3.putBucketWebsite(PutBucketWebsiteRequest.builder().bucket(bucket).websiteConfiguration(WebsiteConfiguration.builder()
            .build())
            .build());
    }

    private void setBucketNameTest(S3Client s3, String bucket) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key("key").bucket(bucket)
            .build();
    }
}