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

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.internal.util.NoopSubscription;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Splits an {@link AsyncRequestBody} to multiple smaller {@link AsyncRequestBody}s, each of which publishes a specific portion of
 * the original data.
 *
 * <p>If content length is known, each {@link AsyncRequestBody} is sent to the subscriber right after it's initialized.
 * Otherwise, it is sent after the entire content for that chunk is buffered. This is required to get content length.
 */
@SdkInternalApi
public class SplittingTransformer<ResponseT, ResultT> implements SdkPublisher<AsyncResponseTransformer<ResponseT, ResultT>> {
    private static final Logger log = Logger.loggerFor(SplittingTransformer.class);
    private final AsyncResponseTransformer upstreamResponseTransformer;
    private final SimplePublisher<AsyncResponseTransformer<ResponseT, ResultT>> downstreamPublisher = new SimplePublisher<>();

    private final ByteBufferStoringSubscriber bufferingSubscriber;

    public SplittingTransformer(AsyncResponseTransformer upstreamResponseTransformer) {
        bufferingSubscriber = new ByteBufferStoringSubscriber(1024 * 1024L);
        this.upstreamResponseTransformer = Validate.paramNotNull(upstreamResponseTransformer, "asyncRequestBody");
    }

    @Override
    public void subscribe(Subscriber<? super AsyncResponseTransformer<ResponseT, ResultT>> downStreamSubscriber) {

        downStreamSubscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                AsyncResponseTransformer<ResponseT, ResultT> asyncResponseTransformer =
                    new AsyncResponseTransformer<ResponseT, ResultT>() {
                        @Override
                        public CompletableFuture<ResultT> prepare() {
                            return null;
                        }

                        @Override
                        public void onResponse(ResponseT response) {


                        }

                        @Override
                        public void onStream(SdkPublisher<ByteBuffer> publisher) {
                            //
                            publisher.subscribe(bufferingSubscriber);


                        }

                        @Override
                        public void exceptionOccurred(Throwable error) {

                        }
                    };
                downStreamSubscriber.onNext(asyncResponseTransformer);
            }

            @Override
            public void cancel() {

            }
        });



        CompletableFuture<ResultT> prepare = upstreamResponseTransformer.prepare();
        // TODO: fix this
        upstreamResponseTransformer.onResponse(null);
        upstreamResponseTransformer.onStream(new SdkPublisher<ByteBuffer>() {
            @Override
            public void subscribe(Subscriber<? super ByteBuffer> s) {
                s.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {

                        ByteBuffer out = ByteBuffer.allocate(1024 * 1024);
                        bufferingSubscriber.transferTo(out);
                        s.onNext(out);
                    }

                    @Override
                    public void cancel() {

                    }
                });
            }
        });
    }

}
