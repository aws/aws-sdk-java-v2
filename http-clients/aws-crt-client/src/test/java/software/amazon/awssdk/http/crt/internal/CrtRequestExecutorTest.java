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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import javax.net.ssl.SSLHandshakeException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@ExtendWith(MockitoExtension.class)
public class CrtRequestExecutorTest {

    private CrtRequestExecutor requestExecutor;
    @Mock
    private HttpClientConnectionManager connectionManager;

    @Mock
    private HttpClientConnection httpClientConnection;

    public static Stream<Throwable> retryableExceptions() {
        return Stream.of(new CrtRuntimeException(""), new HttpException(0x080a), new IllegalStateException(
            "connection closed"));
    }

    public static Stream<Entry<Integer, Class<? extends Throwable>>> mappedExceptions() {
        return Stream.of(
            new SimpleEntry<>(0x0405, SSLHandshakeException.class), // For AWS_IO_TLS_ERROR_NEGOTIATION_FAILURE (1029)
            new SimpleEntry<>(0x0418, ConnectException.class) // For AWS_IO_SOCKET_TIMEOUT (1048)
        );
    }

    @BeforeEach
    public void setup() {
        requestExecutor = new CrtRequestExecutor();
    }

    @AfterEach
    public void teardown() {
        Mockito.reset(connectionManager, httpClientConnection);
    }

    @Test
    public void acquireConnectionThrowException_shouldInvokeOnError() {
        RuntimeException exception = new RuntimeException("error");
        CrtRequestContext context = crtRequestContext();
        CompletableFuture<HttpClientConnection> completableFuture = new CompletableFuture<>();

        Mockito.when(connectionManager.acquireConnection()).thenReturn(completableFuture);
        completableFuture.completeExceptionally(exception);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);

        assertThat(executeFuture).hasFailedWithThrowableThat().hasCause(exception).isInstanceOf(IOException.class);
    }

    @ParameterizedTest
    @MethodSource("retryableExceptions")
    public void makeRequestFailed_retryableException_shouldWrapWithIOException(Throwable throwable) {
        CrtRequestContext context = crtRequestContext();
        CompletableFuture<HttpClientConnection> completableFuture = new CompletableFuture<>();

        Mockito.when(connectionManager.acquireConnection()).thenReturn(completableFuture);
        completableFuture.complete(httpClientConnection);

        Mockito.when(httpClientConnection.makeRequest(Mockito.any(HttpRequest.class), Mockito.any(HttpStreamResponseHandler.class)))
               .thenThrow(throwable);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThat(executeFuture).hasFailedWithThrowableThat().hasCause(throwable).isInstanceOf(IOException.class);
    }

    @Test
    public void execute_AcquireConnectionFailure_shouldAlwaysWrapIOException() {
        CrtRequestContext context = crtRequestContext();
        RuntimeException exception = new RuntimeException("some failure");
        CompletableFuture<HttpClientConnection> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(connectionManager.acquireConnection()).thenReturn(completableFuture);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(IOException.class).hasRootCause(exception);
    }

    @ParameterizedTest
    @MethodSource("mappedExceptions")
    public void execute_AcquireConnectionFailure_shouldAlwaysBeInstanceOfIOException(Entry<Integer, Class<? extends Throwable>> entry) {
        int errorCode = entry.getKey();
        Class<? extends Throwable> ioExceptionSubclass = entry.getValue();

        CrtRequestContext context = crtRequestContext();
        HttpException exception = new HttpException(errorCode);
        CompletableFuture<HttpClientConnection> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(connectionManager.acquireConnection()).thenReturn(completableFuture);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(IOException.class).hasMessageContaining(exception.getMessage());
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(ioExceptionSubclass);
    }

    @Test
    public void executeRequest_failedOfNonRetryableHttpException_shouldNotWrapIOException() {
        HttpException exception = new HttpException(0x0801); // AWS_ERROR_HTTP_HEADER_NOT_FOUND
        CrtRequestContext context = crtRequestContext();
        CompletableFuture<HttpClientConnection> completableFuture = new CompletableFuture<>();

        Mockito.when(connectionManager.acquireConnection()).thenReturn(completableFuture);
        completableFuture.complete(httpClientConnection);

        Mockito.when(httpClientConnection.makeRequest(Mockito.any(HttpRequest.class), Mockito.any(HttpStreamResponseHandler.class)))
               .thenThrow(exception);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCause(exception);
    }

    private CrtRequestContext crtRequestContext() {
        SdkHttpFullRequest request = createRequest(URI.create("http://localhost"));
        return CrtRequestContext.builder()
                                .readBufferSize(2000)
                                .crtConnPool(connectionManager)
                                .request(HttpExecuteRequest.builder()
                                                           .request(request)
                                                           .contentStreamProvider(SdkBytes.fromUtf8String("test").asContentStreamProvider())
                                                           .build())
                                .build();
    }
}
