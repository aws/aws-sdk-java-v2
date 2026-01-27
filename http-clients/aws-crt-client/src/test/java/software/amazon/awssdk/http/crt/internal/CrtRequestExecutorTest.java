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
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.crt.http.HttpStreamBaseResponseHandler;
import software.amazon.awssdk.crt.http.HttpStreamManager;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@ExtendWith(MockitoExtension.class)
public class CrtRequestExecutorTest {

    private CrtRequestExecutor requestExecutor;
    @Mock
    private HttpStreamManager streamManager;

    @Mock
    private HttpStreamBase httpStream;

    public static Stream<Throwable> retryableExceptions() {
        return Stream.of(new CrtRuntimeException(""), new HttpException(0x080a), new IllegalStateException(
            "connection closed"));
    }

    public static Stream<Entry<Integer, Class<? extends Throwable>>> mappedExceptions() {
        return Stream.of(
            new SimpleEntry<>(1029, SSLHandshakeException.class), // CRT_TLS_NEGOTIATION_ERROR_CODE
            new SimpleEntry<>(1048, ConnectException.class) // CRT_SOCKET_TIMEOUT
        );
    }

    @BeforeEach
    public void setup() {
        requestExecutor = new CrtRequestExecutor();
    }

    @AfterEach
    public void teardown() {
        Mockito.reset(streamManager, httpStream);
    }

    @Test
    public void execute_requestConversionFails_failsFuture() {
        CrtRequestContext context = CrtRequestContext.builder()
                                                     .crtConnPool(streamManager)
                                                     .request(HttpExecuteRequest.builder().build())
                                                     .build();

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);

        assertThat(executeFuture).hasFailedWithThrowableThat().isInstanceOf(NullPointerException.class);
    }

    @Test
    public void execute_acquireStreamFails_wrapsWithIOException() {
        IllegalStateException exception = new IllegalStateException("connection closed");
        CrtRequestContext context = crtRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = new CompletableFuture<>();

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequest.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);
        completableFuture.completeExceptionally(exception);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);

        assertThat(executeFuture).hasFailedWithThrowableThat().hasCause(exception).isInstanceOf(IOException.class);
    }

    @ParameterizedTest
    @MethodSource("retryableExceptions")
    public void execute_retryableException_wrapsWithIOException(Throwable throwable) {
        CrtRequestContext context = crtRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(throwable);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequest.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThat(executeFuture).hasFailedWithThrowableThat().hasCause(throwable).isInstanceOf(IOException.class);
    }

    @ParameterizedTest
    @MethodSource("mappedExceptions")
    public void execute_httpException_mapsToCorrectException(Entry<Integer, Class<? extends Throwable>> entry) {
        int errorCode = entry.getKey();
        Class<? extends Throwable> expectedExceptionClass = entry.getValue();

        CrtRequestContext context = crtRequestContext();
        HttpException exception = new HttpException(errorCode);
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequest.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(expectedExceptionClass);
    }

    @Test
    public void execute_nonRetryableHttpException_doesNotWrapWithIOException() {
        HttpException exception = new HttpException(0x0801); // AWS_ERROR_HTTP_HEADER_NOT_FOUND
        CrtRequestContext context = crtRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequest.class), Mockito.any(HttpStreamBaseResponseHandler.class)))
               .thenReturn(completableFuture);

        CompletableFuture<SdkHttpFullResponse> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCause(exception);
    }

    private CrtRequestContext crtRequestContext() {
        SdkHttpFullRequest request = createRequest(URI.create("http://localhost"));
        return CrtRequestContext.builder()
                                .readBufferSize(2000)
                                .crtConnPool(streamManager)
                                .request(HttpExecuteRequest.builder()
                                                           .request(request)
                                                           .contentStreamProvider(SdkBytes.fromUtf8String("test").asContentStreamProvider())
                                                           .build())
                                .build();
    }
}
