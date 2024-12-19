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
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
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
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;

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

    private void getObjectMetaData_to_headObject(S3Client s3, String bucket, String key) {
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

    private void listObjects(S3Client s3, String bucket) {
        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(bucket)
            .build();
        ListObjectsResponse objectListing = s3.listObjects(listObjectsRequest);
        System.out.println(objectListing);
    }

    private void listObjectsV2(S3Client s3, String bucket) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucket)
            .build();
        ListObjectsV2Response listObjectsV2Result = s3.listObjectsV2(listObjectsV2Request);
        System.out.println(listObjectsV2Result);
    }

    private void cors(S3Client s3, String bucket) {
        CORSRule corsRule = CORSRule.builder().id("id").maxAgeSeconds(99)
            .build();
        CORSConfiguration cors = CORSConfiguration.builder().corsRules(corsRule)
            .build();
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
        s3.getBucketVersioning(GetBucketVersioningRequest.builder()
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
}