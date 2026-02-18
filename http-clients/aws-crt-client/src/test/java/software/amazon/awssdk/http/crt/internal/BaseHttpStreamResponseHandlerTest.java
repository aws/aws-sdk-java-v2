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

package software.amazon.awssdk.http.crt.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamBaseResponseHandler;
import software.amazon.awssdk.utils.async.SimplePublisher;

@ExtendWith(MockitoExtension.class)
public abstract class BaseHttpStreamResponseHandlerTest {
    CompletableFuture requestFuture;

    @Mock
    HttpStream httpStream;

    @Mock
    SimplePublisher<ByteBuffer> simplePublisher;

    HttpStreamBaseResponseHandler responseHandler;

    abstract HttpStreamBaseResponseHandler responseHandler();

    abstract HttpStreamBaseResponseHandler responseHandlerWithMockedPublisher(SimplePublisher<ByteBuffer> simplePublisher);

    @BeforeEach
    public void setUp() {
        requestFuture = new CompletableFuture<>();
        responseHandler = responseHandler();
    }

    @Test
    void serverError_shouldCloseStream() {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, 500, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);

        responseHandler.onResponseHeadersDone(httpStream, 0);
        responseHandler.onResponseComplete(httpStream, 0);
        requestFuture.join();
        verify(httpStream).close();
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 400, 202, 403 })
    void nonServerError_shouldCloseStream(int statusCode) {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, statusCode, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);

        responseHandler.onResponseHeadersDone(httpStream, 0);
        responseHandler.onResponseComplete(httpStream, 0);

        requestFuture.join();
        verify(httpStream).close();
    }

    @Test
    void failedToGetResponse_shouldCloseStream() {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, 200, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);

        responseHandler.onResponseComplete(httpStream, 1);
        assertThatThrownBy(() -> requestFuture.join()).hasRootCauseInstanceOf(HttpException.class);
        verify(httpStream).close();
    }

    @Test
    void streamClosed_shouldNotIncreaseStreamWindow() throws InterruptedException {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, 500, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);
        responseHandler.onResponseHeadersDone(httpStream, 0);
        responseHandler.onResponseBody(httpStream, "{}".getBytes(StandardCharsets.UTF_8));

        responseHandler.onResponseComplete(httpStream, 0);
        requestFuture.join();
        verify(httpStream).close();
        verify(httpStream, never()).incrementWindow(anyInt());
    }

    @Test
    void publisherWritesFutureFails_shouldCloseStream() {
        SimplePublisher<ByteBuffer> simplePublisher = Mockito.mock(SimplePublisher.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        when(simplePublisher.send(any(ByteBuffer.class))).thenReturn(future);

        HttpStreamBaseResponseHandler handler = responseHandlerWithMockedPublisher(simplePublisher);
        HttpHeader[] httpHeaders = getHttpHeaders();

        handler.onResponseHeaders(httpStream, 200, HttpHeaderBlock.MAIN.getValue(),
                                  httpHeaders);
        handler.onResponseHeadersDone(httpStream, 0);
        handler.onResponseBody(httpStream,
                               RandomStringUtils.random(1 * 1024 * 1024).getBytes(StandardCharsets.UTF_8));
        RuntimeException runtimeException = new RuntimeException();
        future.completeExceptionally(runtimeException);

        try {
            requestFuture.join();
        } catch (Exception e) {
            // we don't verify here because it behaves differently in async and sync
        }

        verify(httpStream).close();
        verify(httpStream, never()).incrementWindow(anyInt());
    }

    @Test
    void publisherWritesFutureCompletesAfterStreamClosed_shouldNotInvokeIncrementWindow() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        when(simplePublisher.send(any(ByteBuffer.class))).thenReturn(future);
        when(simplePublisher.complete()).thenReturn(future);

        HttpStreamBaseResponseHandler handler = responseHandlerWithMockedPublisher(simplePublisher);


        HttpHeader[] httpHeaders = getHttpHeaders();

        handler.onResponseHeaders(httpStream, 200, HttpHeaderBlock.MAIN.getValue(),
                                  httpHeaders);
        handler.onResponseHeadersDone(httpStream, 0);
        handler.onResponseBody(httpStream,
                               RandomStringUtils.random(1 * 1024 * 1024).getBytes(StandardCharsets.UTF_8));
        handler.onResponseComplete(httpStream, 0);
        future.complete(null);

        requestFuture.join();
        verify(httpStream).close();
        verify(httpStream, never()).incrementWindow(anyInt());
    }

    @Test
    void publisherWritesFutureCompletesBeforeStreamClosed_shouldInvokeIncrementWindow() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        when(simplePublisher.send(any(ByteBuffer.class))).thenReturn(future);
        when(simplePublisher.complete()).thenReturn(future);

        HttpStreamBaseResponseHandler handler = responseHandlerWithMockedPublisher(simplePublisher);


        HttpHeader[] httpHeaders = getHttpHeaders();

        handler.onResponseHeaders(httpStream, 200, HttpHeaderBlock.MAIN.getValue(),
                                  httpHeaders);
        handler.onResponseHeadersDone(httpStream, 0);
        handler.onResponseBody(httpStream,
                               RandomStringUtils.random(1 * 1024 * 1024).getBytes(StandardCharsets.UTF_8));

        future.complete(null);
        handler.onResponseComplete(httpStream, 0);
        requestFuture.join();
        verify(httpStream).incrementWindow(anyInt());
        verify(httpStream).close();
    }

    static HttpHeader[] getHttpHeaders() {
        HttpHeader[] httpHeaders = new HttpHeader[1];
        httpHeaders[0] = new HttpHeader("Content-Length", "1");
        return httpHeaders;
    }
}
