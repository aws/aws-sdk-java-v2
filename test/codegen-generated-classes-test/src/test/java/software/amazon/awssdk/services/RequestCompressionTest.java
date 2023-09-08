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
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithStreamingRequestCompressionRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class RequestCompressionTest {
    private static final String UNCOMPRESSED_BODY =
        "RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest";
    private String compressedBody;
    private int compressedLen;
    private MockSyncHttpClient mockHttpClient;
    private ProtocolRestJsonClient syncClient;
    private Compressor compressor;
    private RequestBody requestBody;

    @BeforeEach
    public void setUp() {
        mockHttpClient = new MockSyncHttpClient();
        syncClient = ProtocolRestJsonClient.builder()
                                           .credentialsProvider(AnonymousCredentialsProvider.create())
                                           .region(Region.US_EAST_1)
                                           .httpClient(mockHttpClient)
                                           .build();
        compressor = new GzipCompressor();
        byte[] compressedBodyBytes = compressor.compress(UNCOMPRESSED_BODY.getBytes());
        compressedLen = compressedBodyBytes.length;
        compressedBody = new String(compressedBodyBytes);
        TestContentProvider provider = new TestContentProvider(UNCOMPRESSED_BODY.getBytes());
        requestBody = RequestBody.fromContentProvider(provider, "binary/octet-stream");
    }

    @AfterEach
    public void reset() {
        mockHttpClient.reset();
    }

    @Test
    public void syncNonStreamingOperation_compressionEnabledThresholdOverridden_compressesCorrectly() {
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromUtf8String(UNCOMPRESSED_BODY))
                                                     .overrideConfiguration(o -> o.compressionConfiguration(
                                                         c -> c.minimumCompressionThresholdInBytes(1)))
                                                     .build();
        syncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedSize).isEqualTo(compressedLen);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
    }

    @Test
    public void syncNonStreamingOperation_payloadSizeLessThanCompressionThreshold_doesNotCompress() {
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromUtf8String(UNCOMPRESSED_BODY))
                                                     .build();
        syncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());

        assertThat(loggedBody).isEqualTo(UNCOMPRESSED_BODY);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding")).isEmpty();
    }

    @Test
    public void syncStreamingOperation_compressionEnabled_compressesCorrectly() {
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithStreamingRequestCompressionRequest request =
            PutOperationWithStreamingRequestCompressionRequest.builder().build();
        syncClient.putOperationWithStreamingRequestCompression(request, requestBody, ResponseTransformer.toBytes());

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
        assertThat(loggedRequest.matchingHeaders("Content-Length")).isEmpty();
        assertThat(loggedRequest.firstMatchingHeader("Transfer-Encoding").get()).isEqualTo("chunked");
    }

    @Test
    public void syncNonStreamingOperation_compressionEnabledThresholdOverriddenWithRetry_compressesCorrectly() {
        mockHttpClient.stubNextResponse(mockErrorResponse(), Duration.ofMillis(500));
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromUtf8String(UNCOMPRESSED_BODY))
                                                     .overrideConfiguration(o -> o.compressionConfiguration(
                                                         c -> c.minimumCompressionThresholdInBytes(1)))
                                                     .build();
        syncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedSize).isEqualTo(compressedLen);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
    }

    @Test
    public void syncStreamingOperation_compressionEnabledWithRetry_compressesCorrectly() {
        mockHttpClient.stubNextResponse(mockErrorResponse(), Duration.ofMillis(500));
        mockHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithStreamingRequestCompressionRequest request =
            PutOperationWithStreamingRequestCompressionRequest.builder().build();
        syncClient.putOperationWithStreamingRequestCompression(request, requestBody, ResponseTransformer.toBytes());

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
        assertThat(loggedRequest.matchingHeaders("Content-Length")).isEmpty();
        assertThat(loggedRequest.firstMatchingHeader("Transfer-Encoding").get()).isEqualTo("chunked");
    }

    private HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .build();
    }

    private HttpExecuteResponse mockErrorResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(500).build())
                                  .build();
    }

    private static final class TestContentProvider implements ContentStreamProvider {
        private final byte[] content;
        private final List<CloseTrackingInputStream> createdStreams = new ArrayList<>();
        private CloseTrackingInputStream currentStream;

        private TestContentProvider(byte[] content) {
            this.content = content;
        }

        @Override
        public InputStream newStream() {
            if (currentStream != null) {
                invokeSafely(currentStream::close);
            }
            currentStream = new CloseTrackingInputStream(new ByteArrayInputStream(content));
            createdStreams.add(currentStream);
            return currentStream;
        }

        List<CloseTrackingInputStream> getCreatedStreams() {
            return createdStreams;
        }
    }

    private static class CloseTrackingInputStream extends FilterInputStream {
        private boolean isClosed = false;

        CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            isClosed = true;
        }

        boolean isClosed() {
            return isClosed;
        }
    }
}
