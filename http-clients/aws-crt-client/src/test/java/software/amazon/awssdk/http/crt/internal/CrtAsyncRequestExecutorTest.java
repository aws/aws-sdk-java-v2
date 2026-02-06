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
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.crt.http.HttpStreamBaseResponseHandler;
import software.amazon.awssdk.crt.http.HttpStreamManager;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@ExtendWith(MockitoExtension.class)
public class CrtAsyncRequestExecutorTest {

    private CrtAsyncRequestExecutor requestExecutor;
    @Mock
    private HttpStreamManager streamManager;

    @Mock
    private SdkAsyncHttpResponseHandler responseHandler;

    @Mock
    private HttpStreamBase httpStream;

    public static Stream<Entry<Integer, Class<? extends Throwable>>> mappedExceptions() {
        return Stream.of(
            new SimpleEntry<>(1029, SSLHandshakeException.class), // CRT_TLS_NEGOTIATION_ERROR_CODE
            new SimpleEntry<>(1048, ConnectException.class) // CRT_SOCKET_TIMEOUT
        );
    }

    @BeforeEach
    public void setup() {
        requestExecutor = new CrtAsyncRequestExecutor();
    }

    @AfterEach
    public void teardown() {
        Mockito.reset(streamManager, responseHandler, httpStream);
    }

    @Test
    public void execute_requestConversionFails_invokesOnError() {
        CrtAsyncRequestContext context = CrtAsyncRequestContext.builder()
                                                               .crtConnPool(streamManager)
                                                               .request(AsyncExecuteRequest.builder()
                                                                                           .responseHandler(responseHandler)
                                                                                           .build())
                                                               .build();

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(responseHandler).onError(argumentCaptor.capture());

        Exception actualException = argumentCaptor.getValue();
        assertThat(actualException).isInstanceOf(NullPointerException.class);
        assertThat(executeFuture).hasFailedWithThrowableThat().isInstanceOf(NullPointerException.class);
    }

    @Test
    public void execute_acquireStreamFails_invokesOnErrorAndWrapsWithIOException() {
        IllegalStateException exception = new IllegalStateException("connection closed");
        CrtAsyncRequestContext context = crtAsyncRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = new CompletableFuture<>();

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);
        completableFuture.completeExceptionally(exception);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(responseHandler).onError(argumentCaptor.capture());

        Exception actualException = argumentCaptor.getValue();
        assertThat(actualException).hasCause(exception);
        assertThat(executeFuture).hasFailedWithThrowableThat().hasCause(exception).isInstanceOf(IOException.class);
    }

    @Test
    public void execute_crtRuntimeException_invokesOnError() {
        CrtRuntimeException exception = new CrtRuntimeException("");
        CrtAsyncRequestContext context = crtAsyncRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(responseHandler).onError(argumentCaptor.capture());

        Exception actualException = argumentCaptor.getValue();
        assertThat(actualException).hasCause(exception);
        assertThat(executeFuture).hasFailedWithThrowableThat().hasCause(exception).isInstanceOf(IOException.class);
    }

    @Test
    public void execute_requestCancelled_invokesOnError() {
        CrtAsyncRequestContext context = crtAsyncRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = new CompletableFuture<>();

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);
        executeFuture.cancel(true);

        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(responseHandler).onError(argumentCaptor.capture());

        Exception actualException = argumentCaptor.getValue();
        assertThat(actualException).hasMessageContaining("The request was cancelled");
        assertThat(actualException).isInstanceOf(SdkCancellationException.class);
    }

    @Test
    public void execute_retryableHttpException_wrapsWithIOException() {
        HttpException exception = new HttpException(0x080a); // AWS_ERROR_HTTP_CONNECTION_CLOSED
        CrtAsyncRequestContext context = crtAsyncRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(IOException.class).hasRootCause(exception);
    }

    @ParameterizedTest
    @MethodSource("mappedExceptions")
    public void execute_httpException_mapsToCorrectException(Entry<Integer, Class<? extends Throwable>> entry) {
        int errorCode = entry.getKey();
        Class<? extends Throwable> expectedExceptionClass = entry.getValue();

        CrtAsyncRequestContext context = crtAsyncRequestContext();
        HttpException exception = new HttpException(errorCode);
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(expectedExceptionClass);
    }

    @Test
    public void execute_nonRetryableHttpException_doesNotWrapWithIOException() {
        HttpException exception = new HttpException(0x0801); // AWS_ERROR_HTTP_HEADER_NOT_FOUND
        CrtAsyncRequestContext context = crtAsyncRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(responseHandler).onError(argumentCaptor.capture());

        Exception actualException = argumentCaptor.getValue();
        assertThat(actualException).isEqualTo(exception);
        assertThatThrownBy(executeFuture::join).hasCause(exception);
    }

    private CrtAsyncRequestContext crtAsyncRequestContext() {
        SdkHttpFullRequest request = createRequest(URI.create("http://localhost"));
        return CrtAsyncRequestContext.builder()
                                     .readBufferSize(2000)
                                     .crtConnPool(streamManager)
                                     .request(AsyncExecuteRequest.builder()
                                                            .request(request)
                                                            .requestContentPublisher(createProvider(""))
                                                            .responseHandler(responseHandler)
                                                            .build())
                                     .build();
    }
}
