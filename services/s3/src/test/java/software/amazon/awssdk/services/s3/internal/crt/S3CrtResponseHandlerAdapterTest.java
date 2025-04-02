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

package software.amazon.awssdk.services.s3.internal.crt;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER_ALTERNATE;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RunWith(MockitoJUnitRunner.class)
public class S3CrtResponseHandlerAdapterTest {
    private S3CrtResponseHandlerAdapter responseHandlerAdapter;

    private TestResponseHandler sdkResponseHandler;

    @Mock
    private S3FinishedResponseContext context;

    @Mock
    private S3MetaRequestWrapper s3MetaRequest;
    private CompletableFuture<Void> future;

    @Before
    public void setup() {
        future = new CompletableFuture<>();
        sdkResponseHandler = spy(new TestResponseHandler());
        responseHandlerAdapter = new S3CrtResponseHandlerAdapter(future,
                                                                 sdkResponseHandler,
                                                                 null,
                                                                 CompletableFuture.completedFuture(s3MetaRequest),
                                                                 false);
    }

    @Test
    public void successfulResponse_shouldCompleteFutureSuccessfully() throws Exception {
        HttpHeader[] httpHeaders = new HttpHeader[2];
        httpHeaders[0] = new HttpHeader("foo", "1");
        httpHeaders[1] = new HttpHeader("bar", "2");

        int statusCode = 200;
        responseHandlerAdapter.onResponseHeaders(statusCode, httpHeaders);

        stubOnResponseBody();

        responseHandlerAdapter.onFinished(stubResponseContext(0, 0, null));
        future.get(5, TimeUnit.SECONDS);

        SdkHttpResponse actualSdkHttpResponse = sdkResponseHandler.sdkHttpResponse;
        assertThat(actualSdkHttpResponse.statusCode()).isEqualTo(statusCode);
        assertThat(actualSdkHttpResponse.firstMatchingHeader("foo")).contains("1");
        assertThat(actualSdkHttpResponse.firstMatchingHeader("bar")).contains("2");

        assertThat(future).isCompleted();
        verify(s3MetaRequest, times(2)).incrementReadWindow(11L);
        verify(s3MetaRequest).close();
        verify(sdkResponseHandler).onHeaders(any(SdkHttpResponse.class));
    }

    @Test
    public void s3MetaRequestNotFinish_shouldFailFuture() throws Exception {
        S3CrtResponseHandlerAdapter responseHandlerAdapter = new S3CrtResponseHandlerAdapter(future,
                                                                                             sdkResponseHandler,
                                                                                             null,
                                                                                             new CompletableFuture<>(),
                                                                                             Duration.ofMillis(10),
                                                                                             false);
        int statusCode = 200;
        responseHandlerAdapter.onResponseHeaders(statusCode, new HttpHeader[0]);
        responseHandlerAdapter.onResponseBody(ByteBuffer.wrap("helloworld1".getBytes()), 1, 2);

        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
            .hasMessageContaining("Timeout waiting for metaRequest");
    }

    @Test
    public void nullByteBuffer_shouldCompleteFutureExceptionally() {
        HttpHeader[] httpHeaders = new HttpHeader[2];
        httpHeaders[0] = new HttpHeader("foo", "1");
        httpHeaders[1] = new HttpHeader("bar", "2");

        int statusCode = 200;
        responseHandlerAdapter.onResponseHeaders(statusCode, httpHeaders);
        responseHandlerAdapter.onResponseBody(null, 0, 0);

        Throwable actualException = sdkResponseHandler.error;
        assertThat(actualException).isInstanceOf(IllegalStateException.class).hasMessageContaining("ByteBuffer delivered is "
                                                                                                   + "null");
        assertThat(future).isCompletedExceptionally();
        verify(s3MetaRequest).close();
        verify(sdkResponseHandler).onHeaders(any(SdkHttpResponse.class));
    }

    @Test
    public void errorResponse_shouldCompleteFutureSuccessfully() {
        int statusCode = 400;
        responseHandlerAdapter.onResponseHeaders(statusCode, new HttpHeader[0]);

        byte[] errorPayload = "errorResponse".getBytes(StandardCharsets.UTF_8);
        stubOnResponseBody();
        responseHandlerAdapter.onFinished(stubResponseContext(1, statusCode, errorPayload));

        SdkHttpResponse actualSdkHttpResponse = sdkResponseHandler.sdkHttpResponse;
        assertThat(actualSdkHttpResponse.statusCode()).isEqualTo(400);
        assertThat(actualSdkHttpResponse.headers()).isEmpty();

        assertThat(future).isCompleted();
        verify(s3MetaRequest).close();
        verify(sdkResponseHandler).onHeaders(any(SdkHttpResponse.class));
    }

    @Test
    public void requestFailed_shouldCompleteFutureExceptionally() {

        responseHandlerAdapter.onFinished(stubResponseContext(1, 0, null));
        Throwable actualException = sdkResponseHandler.error;
        String message = "Failed to send the request";
        assertThat(actualException).isInstanceOf(SdkClientException.class).hasMessageContaining(message);
        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(() -> future.join()).hasRootCauseInstanceOf(SdkClientException.class).hasMessageContaining(message);
        verify(s3MetaRequest).close();
    }

    @Test
    public void requestFailedMidwayDueToServerError_shouldCompleteFutureWithS3Exceptionally() {
        responseHandlerAdapter.onResponseHeaders(200, new HttpHeader[0]);
        responseHandlerAdapter.onResponseBody(ByteBuffer.wrap("helloworld".getBytes(StandardCharsets.UTF_8)), 0, 0);

        S3FinishedResponseContext errorContext = stubResponseContext(1, 404, "".getBytes());
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(X_AMZN_REQUEST_ID_HEADER_ALTERNATE, "1234"));
        headers.add(new HttpHeader(X_AMZ_ID_2_HEADER, "5678"));

        when(errorContext.getErrorHeaders()).thenReturn(headers.toArray(new HttpHeader[0]));

        responseHandlerAdapter.onFinished(errorContext);
        Throwable exceptionFromResponseHandler = sdkResponseHandler.error;
        Throwable exceptionFromSubscriber = sdkResponseHandler.subscriber.error;

        assertThat(exceptionFromResponseHandler).isInstanceOf(S3Exception.class);
        assertThat(((S3Exception) exceptionFromResponseHandler).statusCode()).isEqualTo(404);
        assertThat(((S3Exception) exceptionFromResponseHandler).requestId()).isEqualTo("1234");
        assertThat(((S3Exception) exceptionFromResponseHandler).extendedRequestId()).isEqualTo("5678");
        assertThat(exceptionFromResponseHandler).isEqualTo(exceptionFromSubscriber);

        assertThatThrownBy(() -> future.join()).hasRootCause(exceptionFromResponseHandler);
        assertThat(future).isCompletedExceptionally();
        verify(s3MetaRequest).close();
    }

    @Test
    public void requestFailedMidwayDueToIoError_shouldInvokeOnError() {
        responseHandlerAdapter.onResponseHeaders(200, new HttpHeader[0]);
        responseHandlerAdapter.onResponseBody(ByteBuffer.wrap("helloworld".getBytes(StandardCharsets.UTF_8)), 0, 0);

        S3FinishedResponseContext errorContext = stubResponseContext(1079, 0, "".getBytes());
        responseHandlerAdapter.onFinished(errorContext);
        Throwable exceptionFromResponseHandler = sdkResponseHandler.error;
        Throwable exceptionFromSubscriber = sdkResponseHandler.subscriber.error;

        assertThat(exceptionFromResponseHandler).isEqualTo(exceptionFromSubscriber);
        assertThat(exceptionFromResponseHandler).isInstanceOf(SdkClientException.class);
        assertThatThrownBy(() -> future.join()).hasRootCause(exceptionFromResponseHandler);
        assertThat(future).isCompletedExceptionally();
        verify(s3MetaRequest).close();
    }

    @Test
    public void requestFailedWithCause_shouldCompleteFutureExceptionallyWithCause() {
        RuntimeException cause = new RuntimeException("error");
        S3FinishedResponseContext s3FinishedResponseContext = stubResponseContext(1, 0, null);
        when(s3FinishedResponseContext.getCause()).thenReturn(cause);

        responseHandlerAdapter.onFinished(s3FinishedResponseContext);
        Throwable actualException = sdkResponseHandler.error;
        String message = "Failed to send the request";
        assertThat(actualException).isInstanceOf(SdkClientException.class).hasMessageContaining(message);
        assertThat(future).isCompletedExceptionally();

        assertThatThrownBy(() -> future.join()).hasRootCause(cause).hasMessageContaining(message);
        verify(s3MetaRequest).close();
    }

    private S3FinishedResponseContext stubResponseContext(int errorCode, int responseStatus, byte[] errorPayload) {
        Mockito.reset(context);
        when(context.getErrorCode()).thenReturn(errorCode);
        when(context.getResponseStatus()).thenReturn(responseStatus);
        when(context.getErrorPayload()).thenReturn(errorPayload);
        return context;
    }

    private void stubOnResponseBody() {
        responseHandlerAdapter.onResponseBody(ByteBuffer.wrap("helloworld1".getBytes()), 1, 2);
        responseHandlerAdapter.onResponseBody(ByteBuffer.wrap("helloworld2".getBytes()), 1, 2);
    }

    private static class TestResponseHandler implements SdkAsyncHttpResponseHandler {
        private SdkHttpResponse sdkHttpResponse;
        private Throwable error;
        private TestSubscriber subscriber = new TestSubscriber();

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            this.sdkHttpResponse = headers;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(subscriber);
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }

    private static class TestSubscriber extends DrainingSubscriber {
        private Throwable error;
        @Override
        public void onError(Throwable throwable) {
            error = throwable;
            super.onError(throwable);
        }
    }
}
