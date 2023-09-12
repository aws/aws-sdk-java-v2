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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

@RunWith(MockitoJUnitRunner.class)
public class S3CrtResponseHandlerAdapterTest {
    private S3CrtResponseHandlerAdapter responseHandlerAdapter;

    private TestResponseHandler sdkResponseHandler;

    @Mock
    private S3FinishedResponseContext context;

    @Mock
    private S3MetaRequest s3MetaRequest;
    private CompletableFuture<Void> future;

    @Before
    public void setup() {
        future = new CompletableFuture<>();
        sdkResponseHandler = new TestResponseHandler();
        responseHandlerAdapter = new S3CrtResponseHandlerAdapter(future,
                                                                 sdkResponseHandler,
                                                                 null);
        responseHandlerAdapter.metaRequest(s3MetaRequest);
    }

    @Test
    public void successfulResponse_shouldCompleteFutureSuccessfully() throws Exception {
        HttpHeader[] httpHeaders = new HttpHeader[2];
        httpHeaders[0] = new HttpHeader("foo", "1");
        httpHeaders[1] = new HttpHeader("bar", "2");

        int statusCode = 200;
        responseHandlerAdapter.onResponseHeaders(statusCode, httpHeaders);

        SdkHttpResponse actualSdkHttpResponse = sdkResponseHandler.sdkHttpResponse;
        assertThat(actualSdkHttpResponse.statusCode()).isEqualTo(statusCode);
        assertThat(actualSdkHttpResponse.firstMatchingHeader("foo")).contains("1");
        assertThat(actualSdkHttpResponse.firstMatchingHeader("bar")).contains("2");
        stubOnResponseBody();

        responseHandlerAdapter.onFinished(stubResponseContext(0, 0, null));
        future.get(5, TimeUnit.SECONDS);
        assertThat(future).isCompleted();
        verify(s3MetaRequest, times(2)).incrementReadWindow(11L);
        verify(s3MetaRequest).close();
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
    }

    @Test
    public void errorResponse_shouldCompleteFutureSuccessfully() {
        int statusCode = 400;
        responseHandlerAdapter.onResponseHeaders(statusCode, new HttpHeader[0]);

        SdkHttpResponse actualSdkHttpResponse = sdkResponseHandler.sdkHttpResponse;
        assertThat(actualSdkHttpResponse.statusCode()).isEqualTo(400);
        assertThat(actualSdkHttpResponse.headers()).isEmpty();

        byte[] errorPayload = "errorResponse".getBytes(StandardCharsets.UTF_8);
        stubOnResponseBody();

        responseHandlerAdapter.onFinished(stubResponseContext(1, statusCode, errorPayload));

        assertThat(future).isCompleted();
        verify(s3MetaRequest).close();
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

    private static final class TestResponseHandler implements SdkAsyncHttpResponseHandler {
        private SdkHttpResponse sdkHttpResponse;
        private Throwable error;
        @Override
        public void onHeaders(SdkHttpResponse headers) {
            this.sdkHttpResponse = headers;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new DrainingSubscriber<>());
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }
    }
}
