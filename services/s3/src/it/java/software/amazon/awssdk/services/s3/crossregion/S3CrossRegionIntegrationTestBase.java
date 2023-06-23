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

package software.amazon.awssdk.services.s3.crossregion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

public abstract class S3CrossRegionIntegrationTestBase extends S3IntegrationTestBase {

    public static final String X_AMZ_BUCKET_REGION = "x-amz-bucket-region";

    protected static final Region CROSS_REGION = Region.of("eu-central-1");

    private static final String KEY = "key";

    @Test
    void getApi_CrossRegionCall() {
        s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY), RequestBody.fromString(
            "TEST_STRING"));
        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder().bucket(bucketName()).checksumMode(ChecksumMode.ENABLED).key(KEY).build();
        ResponseBytes<GetObjectResponse> response = getAPICall(getObjectRequest);
        assertThat(new String(response.asByteArray())).isEqualTo("TEST_STRING");
    }

    @Test
    void putApi_CrossRegionCall() {
        s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY), RequestBody.fromString(
            "TEST_STRING"));
        PutObjectRequest putObjectRequest =
            PutObjectRequest.builder().bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY).build();
        PutObjectResponse response = putAPICall(putObjectRequest, "TEST_STRING");
        assertThat(response.checksumCRC32()).isEqualTo("S9ke8w==");
    }

    @Test
    void deleteApi_CrossRegionCall() {
        s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY), RequestBody.fromString(
            "TEST_STRING"));
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName()).key(KEY).build();
        DeleteObjectResponse response = deleteObjectAPICall(deleteObjectRequest);
        assertThat(response).isNotNull();
    }

    @Test
    void postApi_CrossRegionCall() {
        s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY), RequestBody.fromString(
            "TEST_STRING"));
        s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY + "_1"),
                     RequestBody.fromString("TEST_STRING"));
        DeleteObjectsRequest deleteObjectsRequest =
            DeleteObjectsRequest.builder().bucket(bucketName()).delete(d -> d.objects(o -> o.key(KEY), o -> o.key(KEY + "_1"))).build();
        DeleteObjectsResponse response = postObjectAPICall(deleteObjectsRequest);
        assertThat(response).isNotNull();
    }

    @Test
    void cachedRegionGetsUsed_when_CrossRegionCall() {
        putAPICall(PutObjectRequest.builder().bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY).build(),
                   "TEST_STRING");
        GetObjectRequest getObjectRequest =
            GetObjectRequest.builder().bucket(bucketName()).checksumMode(ChecksumMode.ENABLED).key(KEY).build();
        ResponseBytes<GetObjectResponse> response = getAPICall(getObjectRequest);
        assertThat(new String(response.asByteArray())).isEqualTo("TEST_STRING");
    }

    @Test
    void paginatedApi_CrossRegionCall() {
        s3.deleteObject(p -> p.bucket(bucketName()).key(KEY));
        int maxKeys = 3;
        int totalKeys = maxKeys * 2 ;
        IntStream.range(0, totalKeys )
                 .forEach(
                     i ->
                         s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY + "_" + i),
                                      RequestBody.fromString("TEST_STRING"))
                 );
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName()).maxKeys(maxKeys).build();
        List<S3Object> s3ObjectList = paginatedAPICall(listObjectsV2Request);
        assertThat(s3ObjectList).hasSize(totalKeys);
        IntStream.range(0, totalKeys ).forEach(i -> s3.deleteObject(p -> p.bucket(bucketName()).key(KEY + "_" + i)));
    }

    @Test
    void headApi_CrossRegionCall() {
        s3.putObject(p -> p.bucket(bucketName()).checksumAlgorithm(ChecksumAlgorithm.CRC32).key(KEY), RequestBody.fromString(
            "TEST_STRING"));
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucketName()).build();
        HeadBucketResponse response = headAPICall(headBucketRequest);
        assertThat(response).isNotNull();
    }

    protected abstract List<S3Object> paginatedAPICall(ListObjectsV2Request listObjectsV2Request);

    protected abstract DeleteObjectsResponse postObjectAPICall(DeleteObjectsRequest deleteObjectsRequest);

    protected abstract HeadBucketResponse headAPICall(HeadBucketRequest headBucketRequest);

    protected abstract DeleteObjectResponse deleteObjectAPICall(DeleteObjectRequest deleteObjectRequest);

    protected abstract PutObjectResponse putAPICall(PutObjectRequest putObjectRequest, String testString);

    protected abstract ResponseBytes<GetObjectResponse> getAPICall(GetObjectRequest getObjectRequest);

    protected abstract String bucketName();

}
