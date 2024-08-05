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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.Header.TRANSFER_ENCODING;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationChunkedEncodingRequest;

public final class TransferEncodingChunkedFunctionalTests {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Test
    public void apacheClientStreamingOperation_withoutContentLength_addsTransferEncodingDoesNotAddContentLength() {
        stubSuccessfulResponse();
        try (ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                                   .httpClient(ApacheHttpClient.builder().build())
                                                                   .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                                   .build()) {
            TestContentProvider provider = new TestContentProvider(RandomStringUtils.random(1000).getBytes(StandardCharsets.UTF_8));
            RequestBody requestBody = RequestBody.fromContentProvider(provider, "binary/octet-stream");
            client.streamingInputOperationChunkedEncoding(StreamingInputOperationChunkedEncodingRequest.builder().build(), requestBody);

            verify(postRequestedFor(anyUrl()).withHeader(TRANSFER_ENCODING, equalTo("chunked")));
            verify(postRequestedFor(anyUrl()).withoutHeader(CONTENT_LENGTH));
        }
    }

    @Test
    public void nettyClientStreamingOperation_withoutContentLength_addsTransferEncodingDoesNotAddContentLength() {
        stubSuccessfulResponse();
        try (ProtocolRestJsonAsyncClient client = ProtocolRestJsonAsyncClient.builder()
                                                                             .httpClient(NettyNioAsyncHttpClient.create())
                                                                             .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                                             .build()) {
            client.streamingInputOperationChunkedEncoding(StreamingInputOperationChunkedEncodingRequest.builder().build(),
                                                          customAsyncRequestBodyWithoutContentLength()).join();

            verify(postRequestedFor(anyUrl()).withHeader(TRANSFER_ENCODING, equalTo("chunked")));
        }
    }

    private void stubSuccessfulResponse() {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)));
    }

    private AsyncRequestBody customAsyncRequestBodyWithoutContentLength() {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes("Random text".getBytes()))
                        .subscribe(s);
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
