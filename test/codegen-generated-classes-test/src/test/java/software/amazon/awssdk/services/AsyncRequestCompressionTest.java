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

import io.reactivex.Flowable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithRequestCompressionRequest;
import software.amazon.awssdk.services.protocolrestjson.model.PutOperationWithStreamingRequestCompressionRequest;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

public class AsyncRequestCompressionTest {
    private static final String UNCOMPRESSED_BODY =
        "RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest";
    private String compressedBody;
    private int compressedLen;
    private MockAsyncHttpClient mockAsyncHttpClient;
    private ProtocolRestJsonAsyncClient asyncClient;
    private Compressor compressor;

    @BeforeEach
    public void setUp() {
        mockAsyncHttpClient = new MockAsyncHttpClient();
        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .credentialsProvider(AnonymousCredentialsProvider.create())
                                                 .region(Region.US_EAST_1)
                                                 .httpClient(mockAsyncHttpClient)
                                                 .build();
        compressor = new GzipCompressor();
        byte[] compressedBodyBytes = compressor.compress(UNCOMPRESSED_BODY.getBytes());
        compressedBody = new String(compressedBodyBytes);
        compressedLen = compressedBodyBytes.length;
    }

    @AfterEach
    public void reset() {
        mockAsyncHttpClient.reset();
    }

    @Test
    public void asyncNonStreamingOperation_compressionEnabledThresholdOverridden_compressesCorrectly() {
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromUtf8String(UNCOMPRESSED_BODY))
                                                     .overrideConfiguration(o -> o.compressionConfiguration(
                                                         c -> c.minimumCompressionThresholdInBytes(1)))
                                                     .build();

        asyncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedSize).isEqualTo(compressedLen);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
    }

    @Test
    public void asyncNonStreamingOperation_payloadSizeLessThanCompressionThreshold_doesNotCompress() {
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromUtf8String(UNCOMPRESSED_BODY))
                                                     .build();

        asyncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());

        assertThat(loggedBody).isEqualTo(UNCOMPRESSED_BODY);
        assertThat(loggedSize).isEqualTo(UNCOMPRESSED_BODY.length());
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding")).isEmpty();
    }

    @Test
    public void asyncStreamingOperation_compressionEnabled_compressesCorrectly() {
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        mockAsyncHttpClient.setAsyncRequestBodyLength(compressedBody.length());
        PutOperationWithStreamingRequestCompressionRequest request =
            PutOperationWithStreamingRequestCompressionRequest.builder().build();
        asyncClient.putOperationWithStreamingRequestCompression(request, customAsyncRequestBodyWithoutContentLength(),
                                                                AsyncResponseTransformer.toBytes()).join();

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();
        String loggedBody = new String(mockAsyncHttpClient.getStreamingPayload().get());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
        assertThat(loggedRequest.matchingHeaders("Content-Length")).isEmpty();
        assertThat(loggedRequest.firstMatchingHeader("Transfer-Encoding").get()).isEqualTo("chunked");
    }

    @Test
    public void asyncNonStreamingOperation_compressionEnabledThresholdOverriddenWithRetry_compressesCorrectly() {
        mockAsyncHttpClient.stubNextResponse(mockErrorResponse(), Duration.ofMillis(500));
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        PutOperationWithRequestCompressionRequest request =
            PutOperationWithRequestCompressionRequest.builder()
                                                     .body(SdkBytes.fromUtf8String(UNCOMPRESSED_BODY))
                                                     .overrideConfiguration(o -> o.compressionConfiguration(
                                                         c -> c.minimumCompressionThresholdInBytes(1)))
                                                     .build();

        asyncClient.putOperationWithRequestCompression(request);

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();
        InputStream loggedStream = loggedRequest.contentStreamProvider().get().newStream();
        String loggedBody = new String(SdkBytes.fromInputStream(loggedStream).asByteArray());
        int loggedSize = Integer.valueOf(loggedRequest.firstMatchingHeader("Content-Length").get());

        assertThat(loggedBody).isEqualTo(compressedBody);
        assertThat(loggedSize).isEqualTo(compressedLen);
        assertThat(loggedRequest.firstMatchingHeader("Content-encoding").get()).isEqualTo("gzip");
    }

    @Test
    public void asyncStreamingOperation_compressionEnabledWithRetry_compressesCorrectly() {
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));
        mockAsyncHttpClient.stubNextResponse(mockResponse(), Duration.ofMillis(500));

        mockAsyncHttpClient.setAsyncRequestBodyLength(compressedBody.length());
        PutOperationWithStreamingRequestCompressionRequest request =
            PutOperationWithStreamingRequestCompressionRequest.builder().build();
        asyncClient.putOperationWithStreamingRequestCompression(request, customAsyncRequestBodyWithoutContentLength(),
                                                                AsyncResponseTransformer.toBytes()).join();

        SdkHttpFullRequest loggedRequest = (SdkHttpFullRequest) mockAsyncHttpClient.getLastRequest();
        String loggedBody = new String(mockAsyncHttpClient.getStreamingPayload().get());

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

    protected AsyncRequestBody customAsyncRequestBodyWithoutContentLength() {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes(UNCOMPRESSED_BODY.getBytes()))
                        .subscribe(s);
            }
        };
    }
}
