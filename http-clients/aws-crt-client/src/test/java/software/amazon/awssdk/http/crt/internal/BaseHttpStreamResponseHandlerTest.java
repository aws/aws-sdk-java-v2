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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;

@ExtendWith(MockitoExtension.class)
public abstract class BaseHttpStreamResponseHandlerTest {
    @Mock HttpClientConnection crtConn;
    CompletableFuture requestFuture;

    @Mock
    private HttpStream httpStream;

    private HttpStreamResponseHandler responseHandler;

    abstract HttpStreamResponseHandler responseHandler();

    @BeforeEach
    public void setUp() {
        requestFuture = new CompletableFuture<>();
        responseHandler = responseHandler();
    }

    @Test
    void serverError_shouldShutdownConnection() {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, 500, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);

        responseHandler.onResponseHeadersDone(httpStream, 0);
        responseHandler.onResponseComplete(httpStream, 0);
        requestFuture.join();
        verify(crtConn).shutdown();
        verify(crtConn).close();
        verify(httpStream).close();
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 400, 202, 403 })
    void nonServerError_shouldNotShutdownConnection(int statusCode) {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, statusCode, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);

        responseHandler.onResponseHeadersDone(httpStream, 0);
        responseHandler.onResponseComplete(httpStream, 0);

        requestFuture.join();
        verify(crtConn, never()).shutdown();
        verify(crtConn).close();
        verify(httpStream).close();
    }

    @Test
    void failedToGetResponse_shouldShutdownConnection() {
        HttpHeader[] httpHeaders = getHttpHeaders();
        responseHandler.onResponseHeaders(httpStream, 200, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);

        responseHandler.onResponseComplete(httpStream, 1);
        assertThatThrownBy(() -> requestFuture.join()).hasRootCauseInstanceOf(HttpException.class);
        verify(crtConn).shutdown();
        verify(crtConn).close();
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
        verify(crtConn).shutdown();
        verify(crtConn).close();
        verify(httpStream).close();
        verify(httpStream, never()).incrementWindow(anyInt());
    }

    private static HttpHeader[] getHttpHeaders() {
        HttpHeader[] httpHeaders = new HttpHeader[1];
        httpHeaders[0] = new HttpHeader("Content-Length", "1");
        return httpHeaders;
    }
}
