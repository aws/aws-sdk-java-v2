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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Unit test to verify that S3 Express operations include the correct business metric feature ID
 * in the User-Agent header.
 */
public class S3ExpressUserAgentTest {
    private static final String KEY = "test-feature-id.txt";
    private static final String CONTENTS = "test content for feature id validation";
    private static final String S3_EXPRESS_BUCKET = "my-test-bucket--use1-az4--x-s3";
    private static final String REGULAR_BUCKET = "my-test-bucket-regular";
    
    private final UserAgentCapturingInterceptor userAgentInterceptor = new UserAgentCapturingInterceptor();
    private MockSyncHttpClient mockHttpClient;
    private S3Client s3Client;

    @BeforeEach
    void setup() {
        // Mock HTTP client
        mockHttpClient = new MockSyncHttpClient();
        
        // Mock CreateSession response for S3 Express authentication
        String createSessionResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<CreateSessionResult>\n" +
                "    <Credentials>\n" +
                "        <SessionToken>mock-session-token</SessionToken>\n" +
                "        <SecretAccessKey>mock-secret-key</SecretAccessKey>\n" +
                "        <AccessKeyId>mock-access-key</AccessKeyId>\n" +
                "        <Expiration>2025-12-31T23:59:59Z</Expiration>\n" +
                "    </Credentials>\n" +
                "</CreateSessionResult>";
        
        HttpExecuteResponse createSessionHttpResponse = HttpExecuteResponse.builder()
                .response(SdkHttpResponse.builder().statusCode(200).build())
                .responseBody(AbortableInputStream.create(new StringInputStream(createSessionResponse)))
                .build();

        HttpExecuteResponse putResponse = HttpExecuteResponse.builder()
                .response(SdkHttpResponse.builder().statusCode(200).build())
                .responseBody(AbortableInputStream.create(new StringInputStream("")))
                .build();
        
        HttpExecuteResponse getResponse = HttpExecuteResponse.builder()
                .response(SdkHttpResponse.builder().statusCode(200).build())
                .responseBody(AbortableInputStream.create(new StringInputStream(CONTENTS)))
                .build();

        mockHttpClient.stubResponses(
                createSessionHttpResponse, // First CreateSession call for S3 Express bucket
                putResponse,               // PUT operation
                createSessionHttpResponse, // Second CreateSession call for S3 Express bucket  
                getResponse,               // GET operation
                putResponse,               // PUT operation for regular bucket
                getResponse                // GET operation for regular bucket
        );
        
        // S3 client with mocked HTTP client
        s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .httpClient(mockHttpClient)
                .overrideConfiguration(o -> o.addExecutionInterceptor(userAgentInterceptor))
                .build();
        
        userAgentInterceptor.reset();
    }

    @Test
    void putObject_whenS3ExpressBucket_shouldIncludeS3ExpressFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(S3_EXPRESS_BUCKET)
                .key(KEY)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromString(CONTENTS));

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(2); // CreateSession + PutObject calls
        
        // The second User-Agent is from the actual PutObject call
        String userAgent = capturedUserAgents.get(1);
        assertThat(userAgent).isNotNull();

        String expectedFeatureId = BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value();
        String businessMetrics = extractBusinessMetrics(userAgent);

        assertThat(businessMetrics).contains(expectedFeatureId);
        assertThat(userAgent).contains(" m/" + businessMetrics);
    }

    @Test
    void getObject_whenS3ExpressBucket_shouldIncludeS3ExpressFeatureIdInUserAgent() {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(S3_EXPRESS_BUCKET)
                .key(KEY)
                .build();

        s3Client.getObject(getRequest, ResponseTransformer.toBytes());

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(2);

        String userAgent = capturedUserAgents.get(1);
        assertThat(userAgent).isNotNull();

        String expectedFeatureId = BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value();
        String businessMetrics = extractBusinessMetrics(userAgent);

        assertThat(businessMetrics).isNotNull();
        assertThat(businessMetrics).contains(expectedFeatureId);
        assertThat(userAgent).contains(" m/" + businessMetrics);
    }

    @Test
    void putObject_whenRegularS3Bucket_shouldNotIncludeS3ExpressFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(REGULAR_BUCKET)
                .key(KEY)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromString(CONTENTS));

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        String userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();

        String s3ExpressFeatureId = BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value();

        String businessMetrics = extractBusinessMetrics(userAgent);
        if (businessMetrics != null) {
            assertThat(businessMetrics).doesNotContain(s3ExpressFeatureId);
        }

    }

    @Test
    void getObject_whenRegularS3Bucket_shouldNotIncludeS3ExpressFeatureIdInUserAgent() {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(REGULAR_BUCKET)
                .key(KEY)
                .build();

        s3Client.getObject(getRequest, ResponseTransformer.toBytes());

        List<String> capturedUserAgents = userAgentInterceptor.getCapturedUserAgents();
        assertThat(capturedUserAgents).hasSize(1);
        
        String userAgent = capturedUserAgents.get(0);
        assertThat(userAgent).isNotNull();

        String s3ExpressFeatureId = BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value();

        String businessMetrics = extractBusinessMetrics(userAgent);
        if (businessMetrics != null) {
            assertThat(businessMetrics).doesNotContain(s3ExpressFeatureId);
        }

    }

    /**
     * Extracts the business metrics section from a User-Agent string.
     * Business metrics appear as "m/D,J" where D and J are feature IDs.
     */
    private String extractBusinessMetrics(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        
        // Pattern to match business metrics: " m/feature1,feature2"
        Pattern pattern = Pattern.compile(" m/([A-Za-z0-9+\\-,]+)");
        Matcher matcher = pattern.matcher(userAgent);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
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
