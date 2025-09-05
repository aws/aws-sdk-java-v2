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

package software.amazon.awssdk.services.s3.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Integration test to verify that S3 Express operations include the correct business metric feature ID
 * in the User-Agent header for tracking purposes.
 */
public class S3ExpressUserAgentIntegrationTest extends S3ExpressIntegrationTestBase {
    private static final String KEY = "test-feature-id.txt";
    private static final String CONTENTS = "test content for feature id validation";
    private static final Region TEST_REGION = Region.US_EAST_1;
    private static final String AZ = "use1-az4";
    private static final UserAgentCapturingInterceptor userAgentInterceptor = new UserAgentCapturingInterceptor();
    
    private static S3Client s3;
    private static S3Client regularS3;
    private static String s3ExpressBucket;
    private static String regularBucket;

    private static final String S3EXPRESS_BUCKET_PATTERN = temporaryBucketName(S3ExpressUserAgentIntegrationTest.class) + "--%s--x-s3";
    private static final String REGULAR_BUCKET_PATTERN = temporaryBucketName(S3ExpressUserAgentIntegrationTest.class) + "-regular";

    @BeforeAll
    static void setup() {
        s3 = s3ClientBuilder(TEST_REGION)
                .overrideConfiguration(o -> o.addExecutionInterceptor(userAgentInterceptor))
                .build();
        
        regularS3 = s3ClientBuilder(TEST_REGION)
                .overrideConfiguration(o -> o.addExecutionInterceptor(userAgentInterceptor))
                .build();
        
        s3ExpressBucket = String.format(S3EXPRESS_BUCKET_PATTERN, AZ);
        regularBucket = REGULAR_BUCKET_PATTERN;
        
        // Create S3 Express bucket
        createBucketS3Express(s3, s3ExpressBucket, AZ);
        
        // Create regular S3 bucket
        regularS3.createBucket(b -> b.bucket(regularBucket));
        regularS3.waiter().waitUntilBucketExists(r -> r.bucket(regularBucket));
    }

    @AfterAll
    static void teardown() {
        try {
            deleteBucketAndAllContents(s3, s3ExpressBucket);
        } catch (Exception e) {
            System.err.println("Failed to delete S3 Express bucket: " + e.getMessage());
        }
        
        try {
            deleteBucketAndAllContents(regularS3, regularBucket);
        } catch (Exception e) {
            System.err.println("Failed to delete regular bucket: " + e.getMessage());
        }
        
        s3.close();
        regularS3.close();
    }

    @BeforeEach
    void reset() {
        userAgentInterceptor.reset();
    }

    @Test
    void putObject_whenS3ExpressBucket_shouldIncludeS3ExpressFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3ExpressBucket)
                .key(KEY)
                .build();

        s3.putObject(putRequest, RequestBody.fromString(CONTENTS));

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        String userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        
        // Verify the User-Agent contains the S3 Express business metric feature ID
        String expectedFeatureId = BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value();
        assertThat(userAgent).containsPattern("m/([A-Za-z0-9+\\-]+,)*" + expectedFeatureId + "(,[A-Za-z0-9+\\-]+)*");

        userAgentInterceptor.reset();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3ExpressBucket)
                .key(KEY)
                .build();

        s3.getObject(getRequest, ResponseTransformer.toBytes());

        capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        assertThat(userAgent).containsPattern("m/([A-Za-z0-9+\\-]+,)*" + expectedFeatureId + "(,[A-Za-z0-9+\\-]+)*");
    }

    @Test
    void putObject_whenRegularS3Bucket_shouldNotIncludeS3ExpressFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(regularBucket)
                .key(KEY)
                .build();

        regularS3.putObject(putRequest, RequestBody.fromString(CONTENTS));

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        String userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        
        // Verify the User-Agent does not contain the S3 Express business metric feature ID
        String s3ExpressFeatureId = BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value();
        assertThat(userAgent).doesNotMatch(".*m/([A-Za-z0-9+\\-]+,)*" + s3ExpressFeatureId + "(,[A-Za-z0-9+\\-]+)*.*");
        

        userAgentInterceptor.reset();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(regularBucket)
                .key(KEY)
                .build();

        regularS3.getObject(getRequest, ResponseTransformer.toBytes());

        capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        assertThat(userAgent).doesNotMatch(".*m/([A-Za-z0-9+\\-]+,)*" + s3ExpressFeatureId + "(,[A-Za-z0-9+\\-]+)*.*");
    }

    /**
     * Interceptor to capture User-Agent headers from HTTP requests
     */
    private static class UserAgentCapturingInterceptor implements ExecutionInterceptor {
        private final List<String> capturedUserAgents = new ArrayList<>();
        private final AtomicReference<String> lastUserAgent = new AtomicReference<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest httpRequest = context.httpRequest();
            List<String> userAgentHeaders = httpRequest.headers().get("User-Agent");
            
            if (userAgentHeaders != null && !userAgentHeaders.isEmpty()) {
                String userAgent = userAgentHeaders.get(0);
                capturedUserAgents.add(userAgent);
                lastUserAgent.set(userAgent);
            }
        }

        public List<String> getCapturedUserAgents() {
            return new ArrayList<>(capturedUserAgents);
        }

        public String getLastUserAgent() {
            return lastUserAgent.get();
        }

        public void reset() {
            capturedUserAgents.clear();
            lastUserAgent.set(null);
        }
    }
}
