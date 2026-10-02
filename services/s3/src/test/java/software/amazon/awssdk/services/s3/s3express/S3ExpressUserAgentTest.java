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

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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

    public static final UnaryOperator<String> METRIC_SEARCH_PATTERN = 
        metric -> ".*m/[a-zA-Z0-9+-,]*" + metric + ".*";
    
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
                .build();
    }

    @Test
    void putObject_whenS3ExpressBucket_shouldIncludeS3ExpressFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(S3_EXPRESS_BUCKET)
                .key(KEY)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromString(CONTENTS));

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);

        assertThat(userAgentHeaders.get(0)).matches(METRIC_SEARCH_PATTERN.apply("J"));
    }

    @Test
    void getObject_whenS3ExpressBucket_shouldIncludeS3ExpressFeatureIdInUserAgent() {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(S3_EXPRESS_BUCKET)
                .key(KEY)
                .build();

        s3Client.getObject(getRequest, ResponseTransformer.toBytes());

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);

        assertThat(userAgentHeaders.get(0)).matches(METRIC_SEARCH_PATTERN.apply("J"));
    }

    @Test
    void putObject_whenRegularS3Bucket_shouldNotIncludeS3ExpressFeatureIdInUserAgent() {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(REGULAR_BUCKET)
                .key(KEY)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromString(CONTENTS));

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);

        assertThat(userAgentHeaders.get(0)).doesNotMatch(METRIC_SEARCH_PATTERN.apply("J"));
    }

    @Test
    void getObject_whenRegularS3Bucket_shouldNotIncludeS3ExpressFeatureIdInUserAgent() {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(REGULAR_BUCKET)
                .key(KEY)
                .build();

        s3Client.getObject(getRequest, ResponseTransformer.toBytes());

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);

        assertThat(userAgentHeaders.get(0)).doesNotMatch(METRIC_SEARCH_PATTERN.apply("J"));
    }

}
