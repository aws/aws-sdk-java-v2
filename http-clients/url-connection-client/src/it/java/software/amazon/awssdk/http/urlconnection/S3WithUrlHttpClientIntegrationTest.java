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

package software.amazon.awssdk.http.urlconnection;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3WithUrlHttpClientIntegrationTest {

    /**
     * The name of the bucket created, used, and deleted by these tests.
     */
    private static String BUCKET_NAME = "java-sdk-integ-" + System.currentTimeMillis();

    private static String KEY = "key";

    private static Region REGION = Region.US_WEST_2;

    private static S3Client s3;

    /**
     * Creates all the test resources for the tests.
     */
    @BeforeClass
    public static void createResources() throws Exception {
        s3 = S3Client.builder()
                     .region(REGION)
                     .httpClient(UrlConnectionHttpClient.builder().build())
                     .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                     .overrideConfiguration(o -> o.addExecutionInterceptor(new UserAgentVerifyingInterceptor()))
                     .build();

        createBucket(BUCKET_NAME, REGION);
    }

    /**
     * Releases all resources created in this test.
     */
    @AfterClass
    public static void tearDown() {
        deleteObject(BUCKET_NAME, KEY);
        deleteBucket(BUCKET_NAME);
    }

    @Test
    public void verifyPutObject() {
        assertThat(objectCount(BUCKET_NAME)).isEqualTo(0);

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(KEY).build(),
                     RequestBody.fromString("foobar"));


        assertThat(objectCount(BUCKET_NAME)).isEqualTo(1);
    }


    private static void createBucket(String bucket, Region region) {
        s3.createBucket(CreateBucketRequest
                            .builder()
                            .bucket(bucket)
                            .createBucketConfiguration(
                                CreateBucketConfiguration.builder()
                                                         .locationConstraint(region.id())
                                                         .build())
                            .build());
    }

    private static void deleteObject(String bucket, String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();
        s3.deleteObject(deleteObjectRequest);
    }

    private static void deleteBucket(String bucket) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    private int objectCount(String bucket) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                                                           .bucket(bucket)
                                                           .build();

        return s3.listObjectsV2(listReq).keyCount();
    }

    private static final class UserAgentVerifyingInterceptor implements ExecutionInterceptor {

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            assertThat(context.httpRequest().firstMatchingHeader("User-Agent").get()).containsIgnoringCase("io/sync");
            assertThat(context.httpRequest().firstMatchingHeader("User-Agent").get()).containsIgnoringCase("http/UrlConnection");
        }
    }
}
