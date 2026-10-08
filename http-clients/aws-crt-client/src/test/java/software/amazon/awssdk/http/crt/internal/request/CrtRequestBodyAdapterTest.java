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

package software.amazon.awssdk.http.crt.internal.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

class CrtRequestBodyAdapterTest {

    @Test
    void sendRequestBody_publisherThrows_signalsErrorWithoutThrowing() {
        AtomicReference<Throwable> signaled = new AtomicReference<>();
        CrtRequestBodyAdapter adapter =
            new CrtRequestBodyAdapter(erroringPublisher(new RuntimeException("Something wrong happened")), 16, signaled::set);

        assertThatNoException().isThrownBy(() -> adapter.sendRequestBody(ByteBuffer.allocate(16)));

        assertThat(signaled.get()).isInstanceOf(RuntimeException.class)
                                  .hasMessageContaining("Something wrong happened");
    }

    @Test
    void sendRequestBody_publisherThrowsCheckedException_signalsWrappedError() {
        AtomicReference<Throwable> signaled = new AtomicReference<>();
        CrtRequestBodyAdapter adapter =
            new CrtRequestBodyAdapter(erroringPublisher(new IOException("Some I/O error happened")), 16, signaled::set);

        assertThatNoException().isThrownBy(() -> adapter.sendRequestBody(ByteBuffer.allocate(16)));

        assertThat(signaled.get()).isInstanceOf(UncheckedIOException.class)
                                  .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void sendRequestBody_calledAgainAfterError_signalsErrorOnlyOnce() {
        AtomicInteger signalCount = new AtomicInteger(0);
        CrtRequestBodyAdapter adapter =
            new CrtRequestBodyAdapter(erroringPublisher(new RuntimeException("boom")), 16, t -> signalCount.incrementAndGet());

        adapter.sendRequestBody(ByteBuffer.allocate(16));
        assertThatNoException().isThrownBy(() -> adapter.sendRequestBody(ByteBuffer.allocate(16)));

        assertThat(signalCount.get()).isEqualTo(1);
    }

    private static SdkHttpContentPublisher erroringPublisher(Throwable error) {
        return new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(0L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onError(error);
                    }

                    @Override
                    public void cancel() {
                    }
                });
            }
        };
    }
}
