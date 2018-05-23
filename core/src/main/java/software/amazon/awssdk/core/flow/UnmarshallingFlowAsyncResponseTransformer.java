/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.flow;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.pagination.async.SdkPublisher;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

/**
 * Unmarshalling layer on top of the {@link AsyncResponseTransformer} to decode flow events and deliver them to the
 * subscriber.
 *
 * @param <ResponseT> Initial response type of flow operation.
 * @param <EventT> Type of flow event in payload frames.
 * @param <ReturnT> Transformed result type of {@link FlowResponseTransformer}.
 */
@SdkProtectedApi
public class UnmarshallingFlowAsyncResponseTransformer<ResponseT, EventT, ReturnT>
    implements AsyncResponseTransformer<ResponseT, ReturnT> {

    private static final Logger log = LoggerFactory.getLogger(UnmarshallingFlowAsyncResponseTransformer.class);

    private static final ExecutionAttributes EMPTY_EXECUTION_ATTRIBUTES = new ExecutionAttributes();

    /**
     * {@link FlowResponseTransformer} provided by customer.
     */
    private final FlowResponseTransformer<ResponseT, EventT, ReturnT> flowResponseTransformer;

    /**
     * Unmarshalls the event POJO.
     */
    private final HttpResponseHandler<EventT> eventUnmarshaller;

    /**
     * Remaining demand (i.e number of unmarshalled flow events) we need to provide to the customers subscriber.
     */
    private final AtomicLong remainingDemand = new AtomicLong(0);

    /**
     * Reference to customers subscriber to flow events.
     */
    private final AtomicReference<Subscriber<? super EventT>> subscriberRef = new AtomicReference<>();

    /**
     * Flow message decoder that decodes the binary data into "frames". These frames are then passed to the
     * unmarshaller to produce the event POJO.
     */
    private final MessageDecoder decoder = createDecoder();

    /**
     * Initial unmarshalled response of flow operation.
     */
    private ResponseT response;

    public UnmarshallingFlowAsyncResponseTransformer(FlowResponseTransformer<ResponseT, EventT, ReturnT> flowResponseTransformer,
                                                     HttpResponseHandler<EventT> eventUnmarshaller) {
        this.flowResponseTransformer = flowResponseTransformer;
        this.eventUnmarshaller = eventUnmarshaller;
    }

    @Override
    public void responseReceived(ResponseT response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        CompletableFuture<Subscription> dataSubscriptionFuture = new CompletableFuture<>();
        publisher.subscribe(new ByteSubscriber(dataSubscriptionFuture));
        dataSubscriptionFuture.thenAccept(dataSubscription ->
                                              flowResponseTransformer.onStream(new FlowEventPublisher(dataSubscription)));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        flowResponseTransformer.exceptionOccurred(throwable);
    }

    @Override
    public ReturnT complete() {
        return flowResponseTransformer.complete();
    }

    /**
     * Create the flow {@link MessageDecoder} which will decode the raw bytes into {@link Message} frames.
     *
     * @return Decoder.
     */
    private MessageDecoder createDecoder() {
        return new MessageDecoder(m -> {
            if (isEvent(m)) {
                if (m.getHeaders().get(":event-type").getString().equals("initial-response")) {
                    // TODO unmarshall initial response and call responseRecieved.
                    flowResponseTransformer.responseReceived(response);
                } else {
                    try {
                        remainingDemand.decrementAndGet();
                        subscriberRef.get().onNext(eventUnmarshaller.handle(adaptMessageToResponse(m), EMPTY_EXECUTION_ATTRIBUTES));
                    } catch (Exception e) {
                        throw new SdkClientException(e);
                    }
                }
            } else if (isError(m)) {
                FlowException flowException = FlowException.create(m.getHeaders().get(":error-message").getString(),
                                                                   m.getHeaders().get(":error-code").getString());
                // TODO prevent connection reset exception from propagating
                runAndLogError(log, "Error thrown from Subscriber#onError, ignoring.",
                               () -> subscriberRef.get().onError(flowException));
                runAndLogError(log, "Error thrown from FlowResponseTransformer#exceptionOccurred, ignoring.",
                               () -> flowResponseTransformer.exceptionOccurred(flowException));
            }
        });
    }

    /**
     * @param m Message frame.
     * @return True if frame is an event frame, false if not.
     */
    private boolean isEvent(Message m) {
        return "event".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * @param m Message frame.
     * @return True if frame is an error frame, false if not.
     */
    private boolean isError(Message m) {
        return "error".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * Transforms a flow message into a {@link HttpResponse} so we can reuse our existing generated unmarshallers.
     *
     * @param m Message to transform.
     */
    private HttpResponse adaptMessageToResponse(Message m) {
        HttpResponse response = new HttpResponse(null);
        response.setContent(new ByteArrayInputStream(m.getPayload()));
        m.getHeaders().forEach((k, v) -> response.addHeader(k, v.getString()));
        return response;
    }

    /**
     * Subscriber for the raw bytes from the stream. Feeds them to the {@link MessageDecoder} as they arrive
     * and will request as much as needed to fulfill any outstanding demand.
     */
    private class ByteSubscriber implements Subscriber<ByteBuffer> {

        private final CompletableFuture<Subscription> dataSubscriptionFuture;

        private Subscription subscription;

        /**
         * @param dataSubscriptionFuture Future to notify when the {@link Subscription} object is available.
         */
        private ByteSubscriber(CompletableFuture<Subscription> dataSubscriptionFuture) {
            this.dataSubscriptionFuture = dataSubscriptionFuture;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            dataSubscriptionFuture.complete(subscription);
            this.subscription = subscription;
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            decoder.feed(BinaryUtils.copyBytesFrom(buffer));
            // If we still haven't fulfilled the outstanding demand then keep requesting byte chunks until we do
            if (remainingDemand.get() > 0) {
                this.subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * Publisher of flow events. Tracks outstanding demand and requests raw data from the stream until that demand is fulfiled.
     */
    private class FlowEventPublisher implements SdkPublisher<EventT> {

        private final Subscription dataSubscription;

        private FlowEventPublisher(Subscription dataSubscription) {
            this.dataSubscription = dataSubscription;
        }

        @Override
        public void subscribe(Subscriber<? super EventT> subscriber) {

            subscriberRef.set(subscriber);
            // TODO fail on multiple subscribers
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long l) {
                    // Kick off the first request to the byte buffer publisher which will keep requesting
                    // bytes until we can fulfill the demand of the event publisher.
                    dataSubscription.request(1);
                    remainingDemand.addAndGet(l);
                }

                @Override
                public void cancel() {
                    dataSubscription.cancel();
                }
            });
        }
    }

}
