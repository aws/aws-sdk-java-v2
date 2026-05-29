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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;
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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
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
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
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
                                                               .streamManager(streamManager)
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

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
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

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
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

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
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

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
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

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(expectedExceptionClass);
    }

    @Test
    public void execute_nonRetryableHttpException_doesNotWrapWithIOException() {
        HttpException exception = new HttpException(0x0801); // AWS_ERROR_HTTP_HEADER_NOT_FOUND
        CrtAsyncRequestContext context = crtAsyncRequestContext();
        CompletableFuture<HttpStreamBase> completableFuture = CompletableFutureUtils.failedFuture(exception);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(completableFuture);

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        ArgumentCaptor<Exception> argumentCaptor = ArgumentCaptor.forClass(Exception.class);
        Mockito.verify(responseHandler).onError(argumentCaptor.capture());

        Exception actualException = argumentCaptor.getValue();
        assertThat(actualException).isEqualTo(exception);
        assertThatThrownBy(executeFuture::join).hasCause(exception);
    }

    @Test
    public void execute_streamActivateThrows_failsRequestFutureWithIoExceptionAndInvokesOnError() {
        CrtRuntimeException activateError = new CrtRuntimeException("activate failed");
        CrtAsyncRequestContext context = crtAsyncRequestContext();

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.doThrow(activateError).when(httpStream).activate();

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        assertThat(executeFuture).hasFailedWithThrowableThat()
                                 .isInstanceOf(IOException.class)
                                 .hasCauseInstanceOf(CrtRuntimeException.class);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        Mockito.verify(responseHandler, Mockito.times(1)).onError(errorCaptor.capture());
        assertThat(errorCaptor.getValue()).isInstanceOf(IOException.class)
                                          .hasCauseInstanceOf(CrtRuntimeException.class);
        // Verify the acquired stream was cancelled and closed so the connection is not leaked.
        Mockito.verify(httpStream).cancel();
        Mockito.verify(httpStream).close();
    }

    @Test
    public void execute_publisherSubscribeThrowsSynchronously_failsRequestFutureAndInvokesOnError() {
        RuntimeException subscribeError = new RuntimeException("subscribe failure");
        SdkHttpContentPublisher throwingPublisher = new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(0L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                throw subscribeError;
            }
        };
        CrtAsyncRequestContext context = crtAsyncRequestContext(throwingPublisher);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        assertThat(executeFuture).hasFailedWithThrowableThat().isSameAs(subscribeError);
        Mockito.verify(responseHandler, Mockito.times(1)).onError(subscribeError);
        // Verify the acquired stream was cancelled and closed so the connection is not leaked.
        Mockito.verify(httpStream).cancel();
        Mockito.verify(httpStream).close();
    }

    @Test
    public void execute_publisherDeliversBody_writesAllChunksAndEndOfStream() {
        byte[] chunk1 = "hello ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "world".getBytes(StandardCharsets.UTF_8);
        TestPublisher publisher = TestPublisher.builder()
                                               .contentLength(chunk1.length + chunk2.length)
                                               .emit(ByteBuffer.wrap(chunk1))
                                               .emit(ByteBuffer.wrap(chunk2))
                                               .complete()
                                               .build();
        CrtAsyncRequestContext context = crtAsyncRequestContext(publisher);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.when(httpStream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(httpStream.writeData(Mockito.isNull(), Mockito.eq(true)))
               .thenReturn(CompletableFuture.completedFuture(null));

        requestExecutor.execute(context);

        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(httpStream, Mockito.times(2)).writeData(dataCaptor.capture(), Mockito.eq(false));
        assertThat(dataCaptor.getAllValues().get(0)).isEqualTo(chunk1);
        assertThat(dataCaptor.getAllValues().get(1)).isEqualTo(chunk2);
        Mockito.verify(httpStream, Mockito.times(1)).writeData(Mockito.isNull(), Mockito.eq(true));
    }

    @Test
    public void execute_publisherSignalsError_failsExecuteFutureAndClosesConnection() {
        RuntimeException publisherError = new RuntimeException("publisher failure");
        byte[] chunk = "data".getBytes(StandardCharsets.UTF_8);
        TestPublisher publisher = TestPublisher.builder()
                                               .contentLength(chunk.length)
                                               .emit(ByteBuffer.wrap(chunk))
                                               .error(publisherError)
                                               .build();
        CrtAsyncRequestContext context = crtAsyncRequestContext(publisher);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.when(httpStream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        Mockito.verify(responseHandler).onError(publisherError);
        Mockito.verify(httpStream).cancel();
        Mockito.verify(httpStream).close();
        assertThat(executeFuture).hasFailedWithThrowableThat().isSameAs(publisherError);
    }

    @Test
    public void execute_writeDataFutureFails_failsExecuteFutureAndClosesConnection() {
        RuntimeException writeError = new RuntimeException("write failure");
        byte[] chunk = "data".getBytes(StandardCharsets.UTF_8);
        TestPublisher publisher = TestPublisher.builder()
                                               .contentLength(chunk.length)
                                               .emit(ByteBuffer.wrap(chunk))
                                               .complete()
                                               .build();
        CrtAsyncRequestContext context = crtAsyncRequestContext(publisher);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.when(httpStream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenReturn(CompletableFutureUtils.failedFuture(writeError));
        // After the write failure cancels the subscription, the publisher must not emit further
        // signals per Reactive Streams rule 1.6, but stub the EOS write defensively in case the
        // subscriber's onComplete still fires before cancellation propagates.
        Mockito.when(httpStream.writeData(Mockito.isNull(), Mockito.eq(true)))
               .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<Void> executeFuture = requestExecutor.execute(context);

        Mockito.verify(responseHandler).onError(writeError);
        Mockito.verify(httpStream).cancel();
        Mockito.verify(httpStream).close();
        assertThat(executeFuture).hasFailedWithThrowableThat().isSameAs(writeError);
    }

    @Test
    public void execute_emptyBodyPublisher_subscribesAndSendsEndOfStreamMarker() {
        // createProvider("") returns a publisher with contentLength = Optional.of(0L) that emits
        // onComplete with no onNext. The subscriber should send writeData(null, true) as the EOS
        // marker and never call writeData with body bytes.
        CrtAsyncRequestContext context = crtAsyncRequestContext();

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.when(httpStream.writeData(Mockito.isNull(), Mockito.eq(true)))
               .thenReturn(CompletableFuture.completedFuture(null));

        requestExecutor.execute(context);

        ArgumentCaptor<Boolean> manualWritesCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(streamManager).acquireStream(Mockito.any(HttpRequestBase.class),
                                                    Mockito.any(HttpStreamBaseResponseHandler.class),
                                                    manualWritesCaptor.capture());
        assertThat(manualWritesCaptor.getValue()).isTrue();
        Mockito.verify(httpStream, Mockito.never()).writeData(Mockito.any(byte[].class), Mockito.eq(false));
        Mockito.verify(httpStream).writeData(Mockito.isNull(), Mockito.eq(true));
    }

    @Test
    public void execute_chunkedBodyPublisher_useManualDataWritesIsTrue() {
        byte[] chunk = "data".getBytes(StandardCharsets.UTF_8);
        TestPublisher publisher = TestPublisher.builder()
                                               .unknownContentLength()
                                               .emit(ByteBuffer.wrap(chunk))
                                               .complete()
                                               .build();
        CrtAsyncRequestContext context = crtAsyncRequestContext(publisher);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.when(httpStream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(httpStream.writeData(Mockito.isNull(), Mockito.eq(true)))
               .thenReturn(CompletableFuture.completedFuture(null));

        requestExecutor.execute(context);

        ArgumentCaptor<Boolean> manualWritesCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(streamManager).acquireStream(Mockito.any(HttpRequestBase.class),
                                                    Mockito.any(HttpStreamBaseResponseHandler.class),
                                                    manualWritesCaptor.capture());
        assertThat(manualWritesCaptor.getValue()).isTrue();
        Mockito.verify(httpStream).writeData(Mockito.eq(chunk), Mockito.eq(false));
        Mockito.verify(httpStream).writeData(Mockito.isNull(), Mockito.eq(true));
    }

    @Test
    public void execute_nonEmptyBodyPublisher_useManualDataWritesIsTrue() {
        byte[] chunk = "data".getBytes(StandardCharsets.UTF_8);
        TestPublisher publisher = TestPublisher.builder()
                                               .contentLength(chunk.length)
                                               .emit(ByteBuffer.wrap(chunk))
                                               .complete()
                                               .build();
        CrtAsyncRequestContext context = crtAsyncRequestContext(publisher);

        Mockito.when(streamManager.acquireStream(Mockito.any(HttpRequestBase.class),
                                                 Mockito.any(HttpStreamBaseResponseHandler.class),
                                                 Mockito.anyBoolean()))
               .thenReturn(CompletableFuture.completedFuture(httpStream));
        Mockito.when(httpStream.writeData(Mockito.any(byte[].class), Mockito.eq(false)))
               .thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(httpStream.writeData(Mockito.isNull(), Mockito.eq(true)))
               .thenReturn(CompletableFuture.completedFuture(null));

        requestExecutor.execute(context);

        ArgumentCaptor<Boolean> manualWritesCaptor = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(streamManager).acquireStream(Mockito.any(HttpRequestBase.class),
                                                    Mockito.any(HttpStreamBaseResponseHandler.class),
                                                    manualWritesCaptor.capture());
        assertThat(manualWritesCaptor.getValue()).isTrue();
    }

    private CrtAsyncRequestContext crtAsyncRequestContext(SdkHttpContentPublisher publisher) {
        SdkHttpFullRequest request = createRequest(URI.create("http://localhost"));
        return CrtAsyncRequestContext.builder()
                                     .readBufferSize(2000)
                                     .streamManager(streamManager)
                                     .request(AsyncExecuteRequest.builder()
                                                            .request(request)
                                                            .requestContentPublisher(publisher)
                                                            .responseHandler(responseHandler)
                                                            .build())
                                     .build();
    }

    /**
     * A test {@link SdkHttpContentPublisher} that emits a fixed sequence of buffers, then either
     * completes or signals an error. All emissions are delivered synchronously inside the first
     * {@code request(n)} call.
     */
    private static final class TestPublisher implements SdkHttpContentPublisher {
        private final List<ByteBuffer> buffers;
        private final boolean complete;
        private final Throwable error;
        private final Optional<Long> contentLength;

        private TestPublisher(Builder b) {
            this.buffers = b.buffers;
            this.complete = b.complete;
            this.error = b.error;
            this.contentLength = b.contentLength;
        }

        static Builder builder() {
            return new Builder();
        }

        @Override
        public Optional<Long> contentLength() {
            return contentLength;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                private boolean delivered;

                @Override
                public void request(long n) {
                    if (delivered) {
                        return;
                    }
                    delivered = true;
                    for (ByteBuffer b : buffers) {
                        s.onNext(b);
                    }
                    if (error != null) {
                        s.onError(error);
                    } else if (complete) {
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                }
            });
        }

        static final class Builder {
            private final List<ByteBuffer> buffers = new ArrayList<>();
            private boolean complete;
            private Throwable error;
            private Optional<Long> contentLength = Optional.of(0L);

            Builder emit(ByteBuffer buf) {
                buffers.add(buf);
                return this;
            }

            Builder complete() {
                this.complete = true;
                return this;
            }

            Builder error(Throwable t) {
                this.error = t;
                return this;
            }

            Builder contentLength(long len) {
                this.contentLength = Optional.of(len);
                return this;
            }

            Builder unknownContentLength() {
                this.contentLength = Optional.empty();
                return this;
            }

            TestPublisher build() {
                return new TestPublisher(this);
            }
        }
    }

    private CrtAsyncRequestContext crtAsyncRequestContext() {
        SdkHttpFullRequest request = createRequest(URI.create("http://localhost"));
        return CrtAsyncRequestContext.builder()
                                     .readBufferSize(2000)
                                     .streamManager(streamManager)
                                     .request(AsyncExecuteRequest.builder()
                                                            .request(request)
                                                            .requestContentPublisher(createProvider(""))
                                                            .responseHandler(responseHandler)
                                                            .build())
                                     .build();
    }
}
