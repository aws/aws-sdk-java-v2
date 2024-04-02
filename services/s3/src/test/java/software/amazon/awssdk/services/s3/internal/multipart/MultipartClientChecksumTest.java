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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

class MultipartClientChecksumTest {
    private MockAsyncHttpClient mockAsyncHttpClient;
    private ChecksumCapturingInterceptor checksumCapturingInterceptor;
    private S3AsyncClient s3Client;

    @BeforeEach
    void init() {
        this.mockAsyncHttpClient = new MockAsyncHttpClient();
        this.checksumCapturingInterceptor = new ChecksumCapturingInterceptor();
        s3Client = S3AsyncClient.builder()
                                .httpClient(mockAsyncHttpClient)
                                .endpointOverride(URI.create("http://localhost"))
                                .overrideConfiguration(c -> c.addExecutionInterceptor(checksumCapturingInterceptor))
                                .multipartEnabled(true)
                                .region(Region.US_EAST_1)
                                .build();
    }

    @AfterEach
    void reset() {
        this.mockAsyncHttpClient.reset();
    }

    @Test
    public void putObject_default_shouldAddCrc32() {
        HttpExecuteResponse response = HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder().statusCode(200).build())
                                                          .build();
        mockAsyncHttpClient.stubResponses(response);

        PutObjectRequest putObjectRequest = putObjectRequestBuilder().build();

        s3Client.putObject(putObjectRequest, AsyncRequestBody.fromString("hello world"));
        assertThat(checksumCapturingInterceptor.checksumHeader).isEqualTo("CRC32");
    }

    @Test
    public void putObject_withNonCrc32ChecksumType_shouldNotAddCrc32() {
        HttpExecuteResponse response = HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder().statusCode(200).build())
                                                          .build();
        mockAsyncHttpClient.stubResponses(response);

        PutObjectRequest putObjectRequest =
            putObjectRequestBuilder()
                .checksumAlgorithm(ChecksumAlgorithm.SHA256)
                .build();

        s3Client.putObject(putObjectRequest, AsyncRequestBody.fromString("hello world"));
        assertThat(checksumCapturingInterceptor.checksumHeader).isEqualTo("SHA256");
    }

    @Test
    public void putObject_withNonCrc32ChecksumValue_shouldNotAddCrc32() {
        HttpExecuteResponse response = HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder().statusCode(200).build())
                                                          .build();
        mockAsyncHttpClient.stubResponses(response);

        PutObjectRequest putObjectRequest =
            putObjectRequestBuilder()
                .checksumSHA256("checksumVal")
                .build();

        s3Client.putObject(putObjectRequest, AsyncRequestBody.fromString("hello world"));
        assertThat(checksumCapturingInterceptor.checksumHeader).isNull();
        assertThat(checksumCapturingInterceptor.headers.get("x-amz-checksum-sha256")).contains("checksumVal");
    }

    @Test
    public void putObject_withCrc32Value_shouldNotAddCrc32TypeHeader() {
        HttpExecuteResponse response = HttpExecuteResponse.builder()
                                                          .response(SdkHttpResponse.builder().statusCode(200).build())
                                                          .build();
        mockAsyncHttpClient.stubResponses(response);

        PutObjectRequest putObjectRequest =
            putObjectRequestBuilder()
                .checksumCRC32("checksumVal")
                .build();

        s3Client.putObject(putObjectRequest, AsyncRequestBody.fromString("hello world"));
        assertThat(checksumCapturingInterceptor.checksumHeader).isNull();
        assertThat(checksumCapturingInterceptor.headers.get("x-amz-checksum-crc32")).contains("checksumVal");
    }

    private PutObjectRequest.Builder putObjectRequestBuilder() {
        return PutObjectRequest.builder().bucket("bucket").key("key");
    }

    private static final class ChecksumCapturingInterceptor implements ExecutionInterceptor {
        String checksumHeader;
        Map<String, List<String>> headers;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpRequest sdkHttpRequest = context.httpRequest();
            headers = sdkHttpRequest.headers();
            String checksumHeaderName = "x-amz-sdk-checksum-algorithm";
            if (headers.containsKey(checksumHeaderName)) {
                List<String> checksumHeaderVals = headers.get(checksumHeaderName);
                assertThat(checksumHeaderVals).hasSize(1);
                checksumHeader = checksumHeaderVals.get(0);
            }
        }
    }
}
