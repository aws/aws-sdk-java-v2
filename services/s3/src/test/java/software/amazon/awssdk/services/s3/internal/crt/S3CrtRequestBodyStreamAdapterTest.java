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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;

class S3CrtRequestBodyStreamAdapterTest {

    @Test
    void getRequestData_fillsInputBuffer_publisherBuffersAreSmaller() {
        int inputBufferSize = 16;

        List<ByteBuffer> data = Stream.generate(() -> (byte) 42)
                                      .limit(inputBufferSize)
                                      .map(b -> {
                                          ByteBuffer bb = ByteBuffer.allocate(1);
                                          bb.put(b);
                                          bb.flip();
                                          return bb;
                                      })
                                      .collect(Collectors.toList());

        SdkHttpContentPublisher requestBody = requestBody(Flowable.fromIterable(data), 42L);

        S3CrtRequestBodyStreamAdapter adapter = new S3CrtRequestBodyStreamAdapter(requestBody);

        ByteBuffer inputBuffer = ByteBuffer.allocate(inputBufferSize);
        adapter.sendRequestBody(inputBuffer);

        assertThat(inputBuffer.remaining()).isEqualTo(0);
    }

    @Test
    void getRequestData_fillsInputBuffer_limitsOutstandingDemand() {
        int inputBufferSize = 2000;
        int maximumOutstandingDemand = 1024;

        RequestTrackingPublisher requestTrackingPublisher = new RequestTrackingPublisher();
        SdkHttpContentPublisher requestBody = requestBody(requestTrackingPublisher, 42L);

        S3CrtRequestBodyStreamAdapter adapter = new S3CrtRequestBodyStreamAdapter(requestBody);

        ByteBuffer inputBuffer = ByteBuffer.allocate(inputBufferSize);
        for (int i = 0; i < maximumOutstandingDemand; i++) {
            // we are under the minimum buffer size, so each request here increases outstanding demand by 1
            adapter.sendRequestBody(inputBuffer);
            // release 1 byte of data, calling onNext (satisfies one request, but then requests 1 more)
            requestTrackingPublisher.release(1);
        }
        // we should have 2x requests
        assertThat(requestTrackingPublisher.requests()).isEqualTo(maximumOutstandingDemand * 2);
        // but the total released bytes is only maximumOutstandingDemand
        assertThat(inputBuffer.remaining()).isEqualTo(inputBufferSize - maximumOutstandingDemand + 1);

        // now that we have reached maximum outstanding demand, new requests won't be sent
        adapter.sendRequestBody(inputBuffer);
        assertThat(requestTrackingPublisher.requests()).isEqualTo(maximumOutstandingDemand * 2);



    }

    private static SdkHttpContentPublisher requestBody(Publisher<ByteBuffer> delegate, long size) {
        return new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(size);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
                delegate.subscribe(subscriber);
            }
        };
    }

    @Test
    public void getRequestData_fillsInputBuffer_publisherBuffersAreLarger() {
        int bodySize = 16;

        ByteBuffer data = ByteBuffer.allocate(bodySize);
        data.put(new byte[bodySize]);
        data.flip();

        SdkHttpContentPublisher requestBody = requestBody(Flowable.just(data), 16L);

        S3CrtRequestBodyStreamAdapter adapter = new S3CrtRequestBodyStreamAdapter(requestBody);

        ByteBuffer inputBuffer = ByteBuffer.allocate(1);

        for (int i = 0; i < bodySize; ++i) {
            adapter.sendRequestBody(inputBuffer);
            assertThat(inputBuffer.remaining()).isEqualTo(0);
            inputBuffer.flip();
        }
    }

    @Test
    public void getRequestData_publisherThrows_surfacesException() {
        Publisher<ByteBuffer> errorPublisher = Flowable.error(new RuntimeException("Something wrong happened"));

        SdkHttpContentPublisher requestBody = requestBody(errorPublisher, 0L);
        S3CrtRequestBodyStreamAdapter adapter = new S3CrtRequestBodyStreamAdapter(requestBody);

        assertThatThrownBy(() -> adapter.sendRequestBody(ByteBuffer.allocate(16)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Something wrong happened");
    }

    @Test
    public void getRequestData_publisherThrows_wrapsExceptionIfNotRuntimeException() {
        Publisher<ByteBuffer> errorPublisher = Flowable.error(new IOException("Some I/O error happened"));

        SdkHttpContentPublisher requestBody = requestBody(errorPublisher, 0L);
        S3CrtRequestBodyStreamAdapter adapter = new S3CrtRequestBodyStreamAdapter(requestBody);

        assertThatThrownBy(() -> adapter.sendRequestBody(ByteBuffer.allocate(16)))
            .isInstanceOf(RuntimeException.class)
            .hasCauseInstanceOf(IOException.class);
    }

    private static class RequestTrackingPublisher implements Publisher<ByteBuffer> {
        ByteBufferStoringSubscriber subscriber;
        RequestTrackingSubscription subscription = new RequestTrackingSubscription();

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            assertThat(subscriber).isInstanceOf(ByteBufferStoringSubscriber.class);
            this.subscriber = (ByteBufferStoringSubscriber) subscriber;
            this.subscriber.onSubscribe(subscription);
        }

        // publish up to n requests
        public void release(int n) {
            for (int i = 0; i < n; i++) {
                ByteBuffer buffer = ByteBuffer.allocate(1);
                subscriber.onNext(buffer);
            }
        }

        public long requests() {
            return subscription.requests;
        }
    }

    private static class RequestTrackingSubscription implements Subscription {

            long requests = 0;

            @Override
            public void request(long n) {
                requests += n;
            }

            @Override
            public void cancel() {

            }

    }
}
