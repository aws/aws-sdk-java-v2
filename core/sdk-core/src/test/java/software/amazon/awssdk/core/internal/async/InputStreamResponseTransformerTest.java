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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SplittingTransformerConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.utils.async.SimplePublisher;

class InputStreamResponseTransformerTest {
    private SimplePublisher<ByteBuffer> publisher;
    private InputStreamResponseTransformer<SdkResponse> transformer;
    private SdkResponse response;
    private CompletableFuture<ResponseInputStream<SdkResponse>> resultFuture;

    @BeforeEach
    public void setup() {
        publisher = new SimplePublisher<>();
        transformer = new InputStreamResponseTransformer<>();
        resultFuture = transformer.prepare();
        response = VoidSdkResponse.builder().build();

        transformer.onResponse(response);

        assertThat(resultFuture).isNotDone();

        transformer.onStream(SdkPublisher.adapt(publisher));

        assertThat(resultFuture).isCompleted();
        assertThat(resultFuture.join().response()).isEqualTo(response);
    }

    @Test
    public void inputStreamReadsAreFromPublisher() throws IOException {
        InputStream stream = resultFuture.join();

        publisher.send(ByteBuffer.wrap(new byte[] { 0, 1, 2 }));
        publisher.complete();

        assertThat(stream.read()).isEqualTo(0);
        assertThat(stream.read()).isEqualTo(1);
        assertThat(stream.read()).isEqualTo(2);
        assertThat(stream.read()).isEqualTo(-1);
    }

    @Test
    public void inputStreamArrayReadsAreFromPublisher() throws IOException {
        InputStream stream = resultFuture.join();

        publisher.send(ByteBuffer.wrap(new byte[] { 0, 1, 2 }));
        publisher.complete();

        byte[] data = new byte[3];
        assertThat(stream.read(data)).isEqualTo(3);

        assertThat(data[0]).isEqualTo((byte) 0);
        assertThat(data[1]).isEqualTo((byte) 1);
        assertThat(data[2]).isEqualTo((byte) 2);
        assertThat(stream.read(data)).isEqualTo(-1);
    }

    @Test
    void exceptionOccurred_onStreamCalled_exceptionPropagatedToRead() {
        ResponseInputStream<SdkResponse> inputStream = resultFuture.join();
        transformer.exceptionOccurred(new IOException("I/O issue"));
        assertThatThrownBy(inputStream::read).isInstanceOf(IOException.class);
    }

    @Test
    void exceptionOccurred_onStreamNotCalled_doesNotFail() {
        InputStreamResponseTransformer<SdkResponse> responseTransformer = new InputStreamResponseTransformer<>();
        responseTransformer.prepare();
        responseTransformer.onResponse(response);
        assertThatNoException().isThrownBy(() -> responseTransformer.exceptionOccurred(new IOException("I/O issue")));
    }

    @Test
    public void exceptionOccurred_onStreamCalled_onSubscribeNotCalled_doesNotThrow() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();

        for (int i = 0; i < 1024; ++i) {
            InputStreamResponseTransformer<SdkResponse> responseTransformer = new InputStreamResponseTransformer<>();
            CompletableFuture<ResponseInputStream<SdkResponse>> cf = responseTransformer.prepare();
            responseTransformer.onResponse(response);

            AtomicBoolean onSubscribeCalled = new AtomicBoolean();

            Phaser phaser = new Phaser(2);

            responseTransformer.exceptionOccurred(new IOException("I/O issue"));
            phaser.arrive();

            exec.submit(() -> {
                phaser.arriveAndAwaitAdvance();

                responseTransformer.onStream(SdkPublisher.adapt(s -> {
                    // the exception should not be forwarded to the inputstream subscriber if onSubscribe is not yet called

                    s.onSubscribe(new Subscription() {
                        @Override
                        public void request(long n) {
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                    onSubscribeCalled.set(true);
                }));
            }).get();

            assertThatThrownBy(() -> cf.join().read()).hasMessageContaining("I/O issue");
            assertThat(onSubscribeCalled.get()).isTrue();
        }
    }

    @Test
    void onStream_whenCompatibilityDisabled_preservesZeroAvailable() throws IOException {
        assertThat(availableAfterHeader(AsyncResponseTransformer.toBlockingInputStream(), gzipHeader())).isZero();
    }

    @Test
    void onStream_whenCompatibilityEnabled_coercesAvailableAboveZeroWhileOpen() throws IOException {
        assertThat(availableAfterHeader(AsyncResponseTransformer.toBlockingInputStream(true), gzipHeader()))
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    void onStream_whenCompatibilityEnabledAndContentIsNotGzip_preservesZeroAvailable() throws IOException {
        assertThat(availableAfterHeader(AsyncResponseTransformer.toBlockingInputStream(true), new byte[] {1, 2, 3}))
            .isZero();
    }

    @Test
    void onStream_whenCompatibilityEnabled_preservesAbort() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AsyncResponseTransformer<SdkResponse, ResponseInputStream<SdkResponse>> responseTransformer =
            AsyncResponseTransformer.toBlockingInputStream(true);
        CompletableFuture<ResponseInputStream<SdkResponse>> future = responseTransformer.prepare();
        responseTransformer.onResponse(VoidSdkResponse.builder().build());
        responseTransformer.onStream(SdkPublisher.adapt(subscriber -> subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        })));

        future.join().abort();

        assertThat(cancelled).isTrue();
    }

    @Test
    void split_whenCompatibilityEnabled_preservesCompatibilityOnCombinedStream() throws Exception {
        AsyncResponseTransformer<SdkResponse, ResponseInputStream<SdkResponse>> responseTransformer =
            AsyncResponseTransformer.toBlockingInputStream(true);
        AsyncResponseTransformer.SplitResult<SdkResponse, ResponseInputStream<SdkResponse>> splitResult =
            responseTransformer.split(SplittingTransformerConfiguration.builder().bufferSizeInBytes(16L).build());
        AtomicBoolean failed = new AtomicBoolean();
        CountDownLatch partCompleted = new CountDownLatch(1);

        splitResult.publisher().subscribe(new org.reactivestreams.Subscriber<
            AsyncResponseTransformer<SdkResponse, SdkResponse>>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(AsyncResponseTransformer<SdkResponse, SdkResponse> partTransformer) {
                partTransformer.prepare().whenComplete((ignored, error) -> {
                    failed.set(error != null);
                    partCompleted.countDown();
                });
                partTransformer.onResponse(VoidSdkResponse.builder().build());
                partTransformer.onStream(AsyncRequestBody.fromBytes(gzipHeader()));
                subscription.cancel();
            }

            @Override
            public void onError(Throwable throwable) {
                failed.set(true);
                partCompleted.countDown();
            }

            @Override
            public void onComplete() {
            }
        });

        ResponseInputStream<SdkResponse> stream = splitResult.resultFuture().join();
        assertThat(partCompleted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(failed).isFalse();
        stream.read();
        stream.read();
        stream.read();

        assertThat(stream.available()).isGreaterThanOrEqualTo(1);
        stream.close();
    }

    private static int availableAfterHeader(
        AsyncResponseTransformer<SdkResponse, ResponseInputStream<SdkResponse>> responseTransformer,
        byte[] header) throws IOException {
        SimplePublisher<ByteBuffer> body = new SimplePublisher<>();
        CompletableFuture<ResponseInputStream<SdkResponse>> future = responseTransformer.prepare();
        responseTransformer.onResponse(VoidSdkResponse.builder().build());
        responseTransformer.onStream(SdkPublisher.adapt(body));
        ResponseInputStream<SdkResponse> stream = future.join();
        body.send(ByteBuffer.wrap(header));
        for (int i = 0; i < header.length; i++) {
            stream.read();
        }
        int available = stream.available();
        body.complete();
        stream.close();
        return available;
    }

    private static byte[] gzipHeader() {
        return new byte[] {0x1f, (byte) 0x8b, 0x08};
    }

}
