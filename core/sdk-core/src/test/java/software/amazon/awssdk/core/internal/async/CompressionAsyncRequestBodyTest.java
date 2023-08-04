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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.http.async.SimpleSubscriber;

public final class CompressionAsyncRequestBodyTest {
    private static final Compressor compressor = new GzipCompressor();;
    private final static String TEST_STRING =
        "RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest-RequestCompressionTest";
    private static final String EXPECTED_TEST_STRING =
        new String(compressor.compress(TEST_STRING.getBytes()));

    private final AsyncRequestBody provider = CompressionAsyncRequestBody.builder()
                                                                         .compressor(compressor)
                                                                         .asyncRequestBody(customAsyncRequestBodyWithoutContentLength())
                                                                         .build();

    @Test
    public void hasCorrectContent() throws InterruptedException {
        StringBuilder sb = new StringBuilder();
        CountDownLatch done = new CountDownLatch(1);

        Subscriber<ByteBuffer> subscriber = new SimpleSubscriber(buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            sb.append(new String(bytes));
        }) {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                done.countDown();
            }

            @Override
            public void onComplete() {
                super.onComplete();
                done.countDown();
            }
        };

        provider.subscribe(subscriber);
        done.await(10, TimeUnit.SECONDS);
        assertThat(sb).hasToString(EXPECTED_TEST_STRING);
    }

    protected AsyncRequestBody customAsyncRequestBodyWithoutContentLength() {
        return new AsyncRequestBody() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                Flowable.fromPublisher(AsyncRequestBody.fromBytes(TEST_STRING.getBytes()))
                        .subscribe(s);
            }
        };
    }
}