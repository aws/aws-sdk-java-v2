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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Integration test to verify that S3 Access Grants operations include the correct business metric feature ID
 * in the User-Agent header for tracking purposes.
 */
public class S3AccessGrantsUserAgentIntegrationTest extends S3IntegrationTestBase {
    private static final String KEY = "test-s3-access-grants-feature-id.txt";
    private static final String CONTENTS = "test content for S3 Access Grants feature id validation";
    private static final UserAgentCapturingInterceptor userAgentInterceptor = new UserAgentCapturingInterceptor();
    
    private static S3Client s3WithAccessGrants;
    private static S3Client regularS3;
    private static String testBucket;

    @BeforeAll
    static void setup() {
        testBucket = temporaryBucketName(S3AccessGrantsUserAgentIntegrationTest.class);

        s3WithAccessGrants = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")))
                .overrideConfiguration(o -> o.addExecutionInterceptor(userAgentInterceptor)
                                             .addExecutionInterceptor(new S3AccessGrantsSimulatorInterceptor()))
                .build();

        regularS3 = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")))
                .overrideConfiguration(o -> o.addExecutionInterceptor(userAgentInterceptor))
                .build();

        regularS3.createBucket(b -> b.bucket(testBucket));
        regularS3.waiter().waitUntilBucketExists(r -> r.bucket(testBucket));
    }

    @AfterAll
    static void teardown() {
        try {
            deleteBucketAndAllContents(testBucket);
        } catch (Exception e) { }
        
        if (s3WithAccessGrants != null) {
            s3WithAccessGrants.close();
        }
        if (regularS3 != null) {
            regularS3.close();
        }
    }

    @BeforeEach
    void reset() {
        userAgentInterceptor.reset();
    }

    @Test
    void putObject_whenS3AccessGrantsPluginActive_shouldIncludeS3AccessGrantsFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(testBucket)
                .key(KEY)
                .build();

        s3WithAccessGrants.putObject(putRequest, RequestBody.fromString(CONTENTS));

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        String userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        
        // Verify the User-Agent contains the S3 Access Grants business metric feature ID
        String expectedFeatureId = BusinessMetricFeatureId.S3_ACCESS_GRANTS.value();
        assertThat(userAgent).containsPattern("m/([A-Za-z0-9+\\-]+,)*" + expectedFeatureId + "(,[A-Za-z0-9+\\-]+)*");

        userAgentInterceptor.reset();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(testBucket)
                .key(KEY)
                .build();

        s3WithAccessGrants.getObject(getRequest, ResponseTransformer.toBytes());

        capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        assertThat(userAgent).containsPattern("m/([A-Za-z0-9+\\-]+,)*" + expectedFeatureId + "(,[A-Za-z0-9+\\-]+)*");
    }

    @Test
    void putObject_whenS3AccessGrantsPluginNotActive_shouldNotIncludeS3AccessGrantsFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(testBucket)
                .key(KEY + "-regular")
                .build();

        regularS3.putObject(putRequest, RequestBody.fromString(CONTENTS));

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        String userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        
        // Verify the User-Agent does not contain the S3 Access Grants business metric feature ID
        String s3AccessGrantsFeatureId = BusinessMetricFeatureId.S3_ACCESS_GRANTS.value();
        assertThat(userAgent).doesNotMatch(".*m/([A-Za-z0-9+\\-]+,)*" + s3AccessGrantsFeatureId + "(,[A-Za-z0-9+\\-]+)*.*");

        userAgentInterceptor.reset();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(testBucket)
                .key(KEY + "-regular")
                .build();

        regularS3.getObject(getRequest, ResponseTransformer.toBytes());

        capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();
        assertThat(userAgent).doesNotMatch(".*m/([A-Za-z0-9+\\-]+,)*" + s3AccessGrantsFeatureId + "(,[A-Za-z0-9+\\-]+)*.*");
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

    /**
     * Interceptor that simulates the presence of S3 Access Grants plugin by injecting
     * a mock identity provider with the expected class name pattern.
     */
    private static class S3AccessGrantsSimulatorInterceptor implements ExecutionInterceptor {
        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            IdentityProvider<AwsCredentialsIdentity> mockS3AccessGrantsProvider = new MockS3AccessGrantsIdentityProvider();

            IdentityProviders identityProviders = IdentityProviders.builder()
                    .putIdentityProvider(mockS3AccessGrantsProvider)
                    .build();

            executionAttributes.putAttribute(
                software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.IDENTITY_PROVIDERS,
                identityProviders
            );
        }
    }

    private static class MockS3AccessGrantsIdentityProvider implements IdentityProvider<AwsCredentialsIdentity> {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            // Return basic credentials for testing
            return CompletableFuture.completedFuture(
                AwsCredentialsIdentity.create("test-access-key", "test-secret-key")
            );
        }
    }
}
