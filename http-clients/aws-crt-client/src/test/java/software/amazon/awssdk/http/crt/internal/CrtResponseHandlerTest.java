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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.async.AsyncResponseHandler;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.internal.response.CrtResponseAdapter;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class CrtResponseHandlerTest extends BaseHttpStreamResponseHandlerTest {

    @Override
    HttpStreamResponseHandler responseHandler() {
        AsyncResponseHandler<Void> responseHandler = new AsyncResponseHandler<>((response,
                                                                                          executionAttributes) -> null, Function.identity(), new ExecutionAttributes());

        responseHandler.prepare();
        return CrtResponseAdapter.toCrtResponseHandler(crtConn, requestFuture, responseHandler);
    }

    @Override
    HttpStreamResponseHandler responseHandlerWithMockedPublisher(SimplePublisher<ByteBuffer> simplePublisher) {
        AsyncResponseHandler<Void> responseHandler = new AsyncResponseHandler<>((response,
                                                                                 executionAttributes) -> null, Function.identity(), new ExecutionAttributes());

        responseHandler.prepare();
        return new CrtResponseAdapter(crtConn, requestFuture, responseHandler, simplePublisher);
    }

    @Test
    void publisherFailedToDeliverEvents_shouldShutDownConnection() {
        SdkAsyncHttpResponseHandler responseHandler = new TestAsyncHttpResponseHandler();

        HttpStreamResponseHandler crtResponseHandler = CrtResponseAdapter.toCrtResponseHandler(crtConn, requestFuture, responseHandler);
        HttpHeader[] httpHeaders = getHttpHeaders();
        crtResponseHandler.onResponseHeaders(httpStream, 200, HttpHeaderBlock.MAIN.getValue(),
                                          httpHeaders);
        crtResponseHandler.onResponseHeadersDone(httpStream, 0);
        crtResponseHandler.onResponseBody(httpStream, "{}".getBytes(StandardCharsets.UTF_8));

        crtResponseHandler.onResponseComplete(httpStream, 0);
        assertThatThrownBy(() -> requestFuture.join()).isInstanceOf(CancellationException.class).hasMessageContaining(
            "subscription has been cancelled");
        verify(crtConn).shutdown();
        verify(crtConn).close();
        verify(httpStream).close();
    }

    private static class TestAsyncHttpResponseHandler implements SdkAsyncHttpResponseHandler {

        @Override
        public void onHeaders(SdkHttpResponse headers) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new Subscriber<ByteBuffer>() {
                private Subscription subscription;
                @Override
                public void onSubscribe(Subscription s) {
                    subscription = s;
                    s.request(1);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                    subscription.cancel();
                }

                @Override
                public void onError(Throwable t) {

                }

                @Override
                public void onComplete() {

                }
            });
        }

        @Override
        public void onError(Throwable error) {

        }
    }
}
