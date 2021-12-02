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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
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
    private static final String BUCKET_NAME = "java-sdk-integ-" + System.currentTimeMillis();
    private static final String KEY = "key";
    private static final Region REGION = Region.US_WEST_2;
    private static final CapturingInterceptor capturingInterceptor = new CapturingInterceptor();
    private static final String SIGNED_PAYLOAD_HEADER_VALUE = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";
    private static final String UNSIGNED_PAYLOAD_HEADER_VALUE = "UNSIGNED-PAYLOAD";

    private static S3Client s3;
    private static S3Client s3Http;

    /**
     * Creates all the test resources for the tests.
     */
    @BeforeClass
    public static void createResources() throws Exception {
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                                                  .region(REGION)
                                                  .httpClient(UrlConnectionHttpClient.builder().build())
                                                  .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                  .overrideConfiguration(o -> o.addExecutionInterceptor(new UserAgentVerifyingInterceptor())
                                                                               .addExecutionInterceptor(capturingInterceptor));
        s3 = s3ClientBuilder.build();
        s3Http = s3ClientBuilder.endpointOverride(URI.create("http://s3.us-west-2.amazonaws.com"))
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

    @Before
    public void methodSetup() {
        capturingInterceptor.reset();
    }

    @Test
    public void verifyPutObject() {
        assertThat(objectCount(BUCKET_NAME)).isEqualTo(0);

        s3.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(KEY).build(), RequestBody.fromString("foobar"));

        assertThat(objectCount(BUCKET_NAME)).isEqualTo(1);
        assertThat(getSha256Values()).contains(UNSIGNED_PAYLOAD_HEADER_VALUE);
    }

    @Test
    public void verifyPutObject_httpCauses_payloadSigning() {
        s3Http.putObject(PutObjectRequest.builder().bucket(BUCKET_NAME).key(KEY).build(), RequestBody.fromString("foobar"));
        assertThat(getSha256Values()).contains(SIGNED_PAYLOAD_HEADER_VALUE);
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

    private List<String> getSha256Values() {
        return capturingInterceptor.capturedRequests().stream()
                                   .map(SdkHttpHeaders::headers)
                                   .map(m -> m.getOrDefault("x-amz-content-sha256", Collections.emptyList()))
                                   .flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static final class UserAgentVerifyingInterceptor implements ExecutionInterceptor {

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            assertThat(context.httpRequest().firstMatchingHeader("User-Agent").get()).containsIgnoringCase("io/sync");
            assertThat(context.httpRequest().firstMatchingHeader("User-Agent").get()).containsIgnoringCase("http/UrlConnection");
        }
    }

    private static class CapturingInterceptor implements ExecutionInterceptor {
        private final List<SdkHttpRequest> capturedRequests = new ArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            capturedRequests.add(context.httpRequest());
        }

        public void reset() {
            capturedRequests.clear();
        }

        public List<SdkHttpRequest> capturedRequests() {
            return capturedRequests;
        }
    }

}
