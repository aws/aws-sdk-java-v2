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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithStreamingRequestCompressionRequest;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class RequestCompressionTest {
    private static final String COMPRESSABLE_BODY =
        "RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest";
    private static final String CRLF = "\r\n";
    MockSyncHttpClient mockHttpClient;
    MockAsyncHttpClient mockAsyncHttpClient;
    ProtocolRestJsonClient syncClient;
    ProtocolRestJsonAsyncClient asyncClient;
    Compressor compressor;

    @BeforeEach
    public void setUp() {
        mockHttpClient = new MockSyncHttpClient();
        mockAsyncHttpClient = new MockAsyncHttpClient();
        syncClient = ProtocolRestJsonClient.builder()
                                           .credentialsProvider(AnonymousCredentialsProvider.create())
                                           .region(Region.US_EAST_1)
                                           .httpClient(mockHttpClient)
                                           .build();
        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                 .region(Region.US_EAST_1)
                                                 .httpClient(mockAsyncHttpClient)
                                                 .build();
        compressor = new GzipCompressor();
    }

    @AfterEach
    public void reset() {
        mockHttpClient.reset();
        mockAsyncHttpClient.reset();
    }

    @Test
    public void sync_nonStreaming_compression_compressesCorrectly() throws InterruptedException {
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        byte[] uncompressedData = COMPRESSABLE_BODY.getBytes(StandardCharsets.UTF_8);
        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromByteArray(uncompressedData))
                                                     .overrideConfiguration(o -> o.requestCompressionConfiguration(
                                                         c -> c.minimumCompressionThresholdInBytes(1)))
                                                     .build();
        syncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        byte[] loggedBody = SdkBytes.fromInputStream(loggedStream).asByteArray();
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());
        byte[] compressedData = compressor.compress(uncompressedData);

        assertThat(loggedBody).isEqualTo(compressedData);
        assertThat(loggedSize).isEqualTo(compressedData.length);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
    }

    @Test
    public void async_nonStreaming_compression_compressesCorrectly() throws InterruptedException {
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        byte[] uncompressedData = COMPRESSABLE_BODY.getBytes(StandardCharsets.UTF_8);
        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromByteArray(uncompressedData))
                                                     .overrideConfiguration(o -> o.requestCompressionConfiguration(
                                                         c -> c.minimumCompressionThresholdInBytes(1)))
                                                     .build();

        asyncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        byte[] loggedBody = SdkBytes.fromInputStream(loggedStream).asByteArray();
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());
        byte[] compressedData = compressor.compress(uncompressedData);

        assertThat(loggedBody).isEqualTo(compressedData);
        assertThat(loggedSize).isEqualTo(compressedData.length);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
    }

    @Test
    public void sync_streaming_compression_compressesCorrectly() throws InterruptedException {
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));
        byte[] uncompressedData = COMPRESSABLE_BODY.getBytes(StandardCharsets.UTF_8);
        PutOperationWithStreamingRequestCompressionRequest request =
            PutOperationWithStreamingRequestCompressionRequest.builder().build();
        syncClient.putOperationWithStreamingRequestCompression(request, RequestBody.fromBytes(uncompressedData),
                                                               ResponseTransformer.toBytes());

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        byte[] loggedBody = SdkBytes.fromInputStream(loggedStream).asByteArray();
        byte[] compressedData = compressor.compress(uncompressedData);
        String chunkBody = Integer.toHexString(compressedData.length) + CRLF + new String(compressedData) + CRLF + "0" + CRLF;

        assertThat(new String(loggedBody)).isEqualTo(chunkBody);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
        assertThat(loggedRequest.matchingHeaders("Content-Length")).isEmpty();
    }

    private HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .build();
    }
}
