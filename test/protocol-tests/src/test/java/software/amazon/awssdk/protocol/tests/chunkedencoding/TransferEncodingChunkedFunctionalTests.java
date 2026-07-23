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

package software.amazon.awssdk.protocol.tests.chunkedencoding;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationChunkedEncodingRequest;
import software.amazon.awssdk.utils.AttributeMap;

public final class TransferEncodingChunkedFunctionalTests {
    // A single-chunk body and a body large enough to span multiple HTTP chunks (streamed in STREAM_BUFFER_SIZE pieces).
    private static final int SMALL_BODY_SIZE = 1024;
    private static final int MULTI_CHUNK_BODY_SIZE = 1024 * 1024;
    private static final int STREAM_BUFFER_SIZE = 128 * 1024;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
                                                         .build();

    @BeforeEach
    public void setUp() {
        stubSuccessfulResponse();
    }

    // The clients are tested over HTTPS so the request takes the default unsigned-payload path. Over plaintext HTTP the
    // SDK forces payload signing, which buffers the body and attaches a Content-Length that conflicts with
    // Transfer-Encoding: chunked - a configuration real chunked callers never use. Each client is exercised with a
    // single-chunk body and a multi-chunk body; the large case additionally proves the streamed body arrives intact.
    private static Stream<Arguments> asyncClients() {
        return Stream.of(
            Arguments.of("Netty, small body", trustAllAsyncClient(NettyNioAsyncHttpClient::builder), SMALL_BODY_SIZE),
            Arguments.of("Netty, large body", trustAllAsyncClient(NettyNioAsyncHttpClient::builder), MULTI_CHUNK_BODY_SIZE),
            Arguments.of("CRT, small body", trustAllAsyncClient(AwsCrtAsyncHttpClient::builder), SMALL_BODY_SIZE),
            Arguments.of("CRT, large body", trustAllAsyncClient(AwsCrtAsyncHttpClient::builder), MULTI_CHUNK_BODY_SIZE)
        );
    }

    private static Stream<Arguments> syncClients() {
        return Stream.of(
            Arguments.of("Apache, small body", trustAllSyncClient(ApacheHttpClient::builder), SMALL_BODY_SIZE),
            Arguments.of("Apache, large body", trustAllSyncClient(ApacheHttpClient::builder), MULTI_CHUNK_BODY_SIZE),
            Arguments.of("CRT, small body", trustAllSyncClient(AwsCrtHttpClient::builder), SMALL_BODY_SIZE),
            Arguments.of("CRT, large body", trustAllSyncClient(AwsCrtHttpClient::builder), MULTI_CHUNK_BODY_SIZE)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asyncClients")
    public void asyncClientStreamingOperation_withoutContentLength_usesChunkedEncodingAndStreamsBody(
        String description, Supplier<SdkAsyncHttpClient> httpClient, int bodySize) {
        byte[] body = randomBody(bodySize);
        try (ProtocolRestJsonAsyncClient client = asyncClient(httpClient.get())) {
            client.streamingInputOperationChunkedEncoding(StreamingInputOperationChunkedEncodingRequest.builder().build(),
                                                          streamingBodyWithoutContentLength(body)).join();

            verifyChunkedRequest(body);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncClients")
    public void syncClientStreamingOperation_withoutContentLength_usesChunkedEncodingAndStreamsBody(
        String description, Supplier<SdkHttpClient> httpClient, int bodySize) {
        byte[] body = randomBody(bodySize);
        try (ProtocolRestJsonClient client = syncClient(httpClient.get())) {
            RequestBody requestBody = RequestBody.fromContentProvider(new TestContentProvider(body), "binary/octet-stream");
            client.streamingInputOperationChunkedEncoding(StreamingInputOperationChunkedEncodingRequest.builder().build(), requestBody);

            verifyChunkedRequest(body);
        }
    }

    private void verifyChunkedRequest(byte[] body) {
        wireMock.verify(postRequestedFor(anyUrl()).withHeader(TRANSFER_ENCODING, equalTo("chunked")));
        wireMock.verify(postRequestedFor(anyUrl()).withoutHeader(CONTENT_LENGTH));
        wireMock.verify(postRequestedFor(anyUrl()).withRequestBody(binaryEqualTo(body)));
    }

    private static byte[] randomBody(int size) {
        return RandomStringUtils.randomAscii(size).getBytes(StandardCharsets.UTF_8);
    }

    private static Supplier<SdkAsyncHttpClient> trustAllAsyncClient(Supplier<SdkAsyncHttpClient.Builder<?>> builder) {
        return () -> trustAllClient(builder.get());
    }

    private static Supplier<SdkHttpClient> trustAllSyncClient(Supplier<SdkHttpClient.Builder<?>> builder) {
        return () -> trustAllClient(builder.get());
    }

    private ProtocolRestJsonAsyncClient asyncClient(SdkAsyncHttpClient httpClient) {
        return ProtocolRestJsonAsyncClient.builder()
                                          .httpClient(httpClient)
                                          .endpointOverride(URI.create("https://localhost:" + wireMock.getHttpsPort()))
                                          .build();
    }

    private ProtocolRestJsonClient syncClient(SdkHttpClient httpClient) {
        return ProtocolRestJsonClient.builder()
                                     .httpClient(httpClient)
                                     .endpointOverride(URI.create("https://localhost:" + wireMock.getHttpsPort()))
                                     .build();
    }

    private void stubSuccessfulResponse() {
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    private static SdkAsyncHttpClient trustAllClient(SdkAsyncHttpClient.Builder<?> builder) {
        return builder.buildWithDefaults(trustAll());
    }

    private static SdkHttpClient trustAllClient(SdkHttpClient.Builder<?> builder) {
        return builder.buildWithDefaults(trustAll());
    }

    private static AttributeMap trustAll() {
        return AttributeMap.builder()
                           .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                           .build();
    }

    // A streaming body that reports no content length, forcing chunked transfer encoding. The payload is split into
    // multiple buffers so a large body is emitted as more than one HTTP chunk.
    private AsyncRequestBody streamingBodyWithoutContentLength(byte[] body) {
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int offset = 0; offset < body.length; offset += STREAM_BUFFER_SIZE) {
            int end = Math.min(offset + STREAM_BUFFER_SIZE, body.length);
            buffers.add(ByteBuffer.wrap(body, offset, end - offset));
        }
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromIterable(buffers).subscribe(s);
            }
        };
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
            return Collections.unmodifiableList(createdStreams);
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
