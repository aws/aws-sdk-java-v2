/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.testserver.MockH2Server;
import software.amazon.awssdk.utils.Logger;

public class GoAwayFrameMockServerTest {
    private static final Logger log = Logger.loggerFor(GoAwayFrameMockServerTest.class);
    private static MockH2Server server;

    @BeforeClass
    public static void setup() throws Exception {
        server = new MockH2Server(false);
        server.start();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stop();
    }

    @Test(timeout = 10_000)
    public void test() throws InterruptedException {
        SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder().protocol(Protocol.HTTP2).build();

        URI serverUri = server.getHttpUri();
        CompletableFuture<Void> call = startCall(client, serverUri);
//        Thread.sleep(1_000);
//        CompletableFuture<Void> call2 = startCall(client, serverUri);

        // TODO: Ensure the first call worked, but the second one didn't
        call.join();
//        assertThatThrownBy(call2::join).isNotNull();
    }

    private CompletableFuture<Void> startCall(SdkAsyncHttpClient client, URI serverUri) {
        return client.execute(AsyncExecuteRequest.builder()
                                                 .request(SdkHttpRequest.builder()
                                                                        .protocol(serverUri.getScheme())
                                                                        .host(serverUri.getHost())
                                                                        .port(serverUri.getPort())
                                                                        .method(SdkHttpMethod.POST)
                                                                        .encodedPath("/")
                                                                        .putHeader("host", serverUri.getHost() + ":" + serverUri.getPort())
                                                                        .build())
                                                 .requestContentPublisher(junkStream(100))
                                                 .fullDuplex(true)
                                                 .responseHandler(dataIgnoringResponseHandler())
                                                 .build());
    }

    public SdkHttpContentPublisher junkStream(long length) {
        Flowable<ByteBuffer> publisher = Flowable.generate(e -> e.onNext(ByteBuffer.wrap(new byte[]{'X'})));
        return new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(length);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                publisher.take(length).subscribe(s);
            }
        };
    }

    public SdkAsyncHttpResponseHandler dataIgnoringResponseHandler() {
        return new SdkAsyncHttpResponseHandler() {
            @Override
            public void onHeaders(SdkHttpResponse headers) {
                log.info(() -> "onHeaders");
            }

            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                // Read the response payload
                Flowable.fromPublisher(stream).subscribe(b -> {});
                log.info(() -> "onStream");
            }

            @Override
            public void onError(Throwable error) {
                log.error(() -> "Error on response: " + error.getMessage(), error);
            }
        };
    }
}
