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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link SdkPublisher} that publishes response body content and also contains a reference to the {@link SdkResponse} returned
 * by the service.
 * <p>
 * <b>NOTE:</b> You must subscribe to this publisher promptly to avoid automatic cancellation. The default timeout
 * for subscribing is 60 seconds. If {@link #subscribe(Subscriber)} is not invoked before the timeout, the publisher
 * will automatically cancel the underlying subscription to prevent resource leakage.
 * <p>
 * The timeout can be customized by passing a {@link Duration} to the constructor, or disabled entirely by
 * passing {@link Duration#ZERO}.
 *
 * @param <ResponseT> Pojo response type.
 * @see AsyncResponseTransformer#toPublisher()
 */
@SdkPublicApi
public final class ResponsePublisher<ResponseT extends SdkResponse> implements SdkPublisher<ByteBuffer> {

    private static final Logger log = Logger.loggerFor(ResponsePublisher.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private final ResponseT response;
    private final SdkPublisher<ByteBuffer> publisher;
    private ScheduledFuture<?> timeoutTask;
    private volatile boolean subscribed = false;

    public ResponsePublisher(ResponseT response, SdkPublisher<ByteBuffer> publisher) {
        this(response, publisher, null);
    }

    public ResponsePublisher(ResponseT response, SdkPublisher<ByteBuffer> publisher, Duration timeout) {
        this.response = Validate.paramNotNull(response, "response");
        this.publisher = Validate.paramNotNull(publisher, "publisher");

        Duration resolvedTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
        scheduleTimeoutTask(resolvedTimeout);
    }

    /**
     * @return the unmarshalled response object from the service.
     */
    public ResponseT response() {
        return response;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        subscribed = true;
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }

        publisher.subscribe(subscriber);
    }

    private void scheduleTimeoutTask(Duration timeout) {
        if (timeout.equals(Duration.ZERO)) {
            return;
        }

        long timeoutInMillis = timeout.toMillis();
        timeoutTask = TimeoutScheduler.INSTANCE.schedule(() -> {
            if (!subscribed) {
                log.debug(() -> String.format("Publisher was not consumed before timeout of [%d] milliseconds, cancelling "
                                              + "subscription and closing connection.", timeoutInMillis));

                publisher.subscribe(new CancellingSubscriber());
            }
        }, timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    private static final class TimeoutScheduler {
        static final ScheduledExecutorService INSTANCE =
            Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "response-publisher-timeout-scheduler");
                t.setDaemon(true);
                return t;
            });
    }

    private static class CancellingSubscriber implements Subscriber<ByteBuffer> {

        @Override
        public void onSubscribe(Subscription s) {
            s.cancel();
        }

        @Override
        public void onNext(ByteBuffer b) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
    }

    @Override
    public String toString() {
        return ToString.builder("ResponsePublisher")
                       .add("response", response)
                       .add("publisher", publisher)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponsePublisher<?> that = (ResponsePublisher<?>) o;

        if (!Objects.equals(response, that.response)) {
            return false;
        }
        return Objects.equals(publisher, that.publisher);
    }

    @Override
    public int hashCode() {
        int result = response != null ? response.hashCode() : 0;
        result = 31 * result + (publisher != null ? publisher.hashCode() : 0);
        return result;
    }

    @SdkTestInternalApi
    public boolean hasTimeoutTask() {
        return timeoutTask != null;
    }

    @SdkTestInternalApi
    public boolean timeoutTaskDoneOrCancelled() {
        return timeoutTask != null && timeoutTask.isDone();
    }
}
