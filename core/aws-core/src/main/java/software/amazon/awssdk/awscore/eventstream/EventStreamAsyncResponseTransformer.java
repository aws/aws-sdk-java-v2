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

package software.amazon.awssdk.awscore.eventstream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADERS;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

/**
 * Unmarshalling layer on top of the {@link AsyncResponseTransformer} to decode event stream messages and deliver them to the
 * subscriber.
 *
 * @param <ResponseT> Initial response type of event stream operation.
 * @param <EventT> Base type of event stream message frames.
 */
@SdkProtectedApi
public final class EventStreamAsyncResponseTransformer<ResponseT, EventT>
    implements AsyncResponseTransformer<SdkResponse, Void> {
    private static final Logger log = Logger.loggerFor(EventStreamAsyncResponseTransformer.class);

    /**
     * {@link EventStreamResponseHandler} provided by customer.
     */
    private final EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler;

    /**
     * Unmarshalls the initial response.
     */
    private final HttpResponseHandler<? extends ResponseT> initialResponseHandler;

    /**
     * Unmarshalls the event POJO.
     */
    private final HttpResponseHandler<? extends EventT> eventResponseHandler;

    /**
     * Unmarshalls exception events.
     */
    private final HttpResponseHandler<? extends Throwable> exceptionResponseHandler;

    private final Supplier<ExecutionAttributes> attributesFactory;

    /**
     * Future to notify on completion. Note that we do not notify this future in the event of an error, that
     * is handled separately by the generated client. Ultimately we need this due to a disconnect between
     * completion of the request (i.e. finish reading all the data from the wire) and the completion of the event
     * stream (i.e. deliver the last event to the subscriber).
     */
    private final CompletableFuture<Void> future;

    /**
     * Whether exceptions may be sent to the downstream event stream response handler. This prevents multiple exception
     * deliveries from being performed.
     */
    private final AtomicBoolean exceptionsMayBeSent = new AtomicBoolean(true);

    /**
     * The future generated via {@link #prepare()}.
     */
    private volatile CompletableFuture<Void> transformFuture;

    /**
     * Request Id for the streaming request. The value is populated when the initial response is received from the service.
     * As request id is not sent in event messages (including exceptions), this can be returned by the SDK along with
     * received exception details.
     */
    private volatile String requestId = null;

    /**
     * Extended Request Id for the streaming request. The value is populated when the initial response is received from the
     * service. As request id is not sent in event messages (including exceptions), this can be returned by the SDK along with
     * received exception details.
     */
    private volatile String extendedRequestId = null;

    private EventStreamAsyncResponseTransformer(
        EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler,
        HttpResponseHandler<? extends ResponseT> initialResponseHandler,
        HttpResponseHandler<? extends EventT> eventResponseHandler,
        HttpResponseHandler<? extends Throwable> exceptionResponseHandler,
        CompletableFuture<Void> future,
        String serviceName) {
        this.eventStreamResponseHandler = eventStreamResponseHandler;
        this.initialResponseHandler = initialResponseHandler;
        this.eventResponseHandler = eventResponseHandler;
        this.exceptionResponseHandler = exceptionResponseHandler;
        this.future = future;
        this.attributesFactory = () -> new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, serviceName);
    }

    /**
     * Creates a {@link Builder} used to create {@link EventStreamAsyncResponseTransformer}.
     *
     * @param <ResponseT> Initial response type.
     * @param <EventT> Event type being delivered.
     * @return New {@link Builder} instance.
     */
    public static <ResponseT, EventT> Builder<ResponseT, EventT> builder() {
        return new Builder<>();
    }

    @Override
    public CompletableFuture<Void> prepare() {
        transformFuture = new CompletableFuture<>();
        return transformFuture;
    }

    @Override
    public void onResponse(SdkResponse response) {
        // Capture the request IDs from the initial response, so that we can include them in each event.
        if (response != null && response.sdkHttpResponse() != null) {
            this.requestId = SdkHttpUtils.firstMatchingHeaderFromCollection(response.sdkHttpResponse().headers(),
                                                                            X_AMZN_REQUEST_ID_HEADERS)
                                         .orElse(null);
            this.extendedRequestId = response.sdkHttpResponse()
                                             .firstMatchingHeader(X_AMZ_ID_2_HEADER)
                                             .orElse(null);

            log.debug(() -> getLogPrefix() + "Received HTTP response headers: " + response);
        }
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        Validate.isTrue(transformFuture != null, "onStream() invoked without prepare().");

        exceptionsMayBeSent.set(true);

        SynchronousMessageDecoder decoder = new SynchronousMessageDecoder();
        eventStreamResponseHandler.onEventStream(publisher.flatMapIterable(decoder::decode)
                                                          .flatMapIterable(this::transformMessage)
                                                          .doAfterOnComplete(this::handleOnStreamComplete)
                                                          .doAfterOnError(this::handleOnStreamError)
                                                          .doAfterOnCancel(this::handleOnStreamCancel));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        if (exceptionsMayBeSent.compareAndSet(true, false)) {
            try {
                eventStreamResponseHandler.exceptionOccurred(throwable);
            } catch (RuntimeException e) {
                log.warn(() -> "Exception raised by exceptionOccurred. Ignoring.", e);
            }
            transformFuture.completeExceptionally(throwable);
        }
    }

    private void handleOnStreamComplete() {
        log.trace(() -> getLogPrefix() + "Event stream completed successfully.");
        exceptionsMayBeSent.set(false);
        eventStreamResponseHandler.complete();
        transformFuture.complete(null);
        future.complete(null);
    }

    private void handleOnStreamError(Throwable throwable) {
        log.trace(() -> getLogPrefix() + "Event stream failed.", throwable);
        exceptionOccurred(throwable);
    }

    private void handleOnStreamCancel() {
        log.trace(() -> getLogPrefix() + "Event stream cancelled.");
        exceptionsMayBeSent.set(false);
        transformFuture.complete(null);
        future.complete(null);
    }

    private static final class SynchronousMessageDecoder {
        private final MessageDecoder decoder = new MessageDecoder();

        private Iterable<Message> decode(ByteBuffer bytes) {
            decoder.feed(bytes);
            return decoder.getDecodedMessages();
        }
    }

    private Iterable<EventT> transformMessage(Message message) {
        try {
            if (isEvent(message)) {
                return transformEventMessage(message);
            } else if (isError(message) || isException(message)) {
                throw transformErrorMessage(message);
            } else {
                log.debug(() -> getLogPrefix() + "Decoded a message of an unknown type, it will be dropped: " + message);
                return emptyList();
            }
        } catch (Error | SdkException e) {
            throw e;
        } catch (Throwable e) {
            throw SdkClientException.builder().cause(e).build();
        }
    }

    private Iterable<EventT> transformEventMessage(Message message) throws Exception {
        SdkHttpFullResponse response = adaptMessageToResponse(message, false);
        if (message.getHeaders().get(":event-type").getString().equals("initial-response")) {
            ResponseT initialResponse = initialResponseHandler.handle(response, attributesFactory.get());
            eventStreamResponseHandler.responseReceived(initialResponse);
            log.debug(() -> getLogPrefix() + "Decoded initial response: " + initialResponse);
            return emptyList();
        }

        EventT event = eventResponseHandler.handle(response, attributesFactory.get());
        log.debug(() -> getLogPrefix() + "Decoded event: " + event);
        return singleton(event);
    }

    private Throwable transformErrorMessage(Message message) throws Exception {
        SdkHttpFullResponse errorResponse = adaptMessageToResponse(message, true);
        Throwable exception = exceptionResponseHandler.handle(errorResponse, attributesFactory.get());
        log.debug(() -> getLogPrefix() + "Decoded error or exception: " + exception, exception);
        return exception;
    }

    private String getLogPrefix() {
        if (requestId == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(");
        stringBuilder.append("RequestId: ").append(requestId);
        if (extendedRequestId != null) {
            stringBuilder.append(", ExtendedRequestId: ").append(extendedRequestId);
        }
        stringBuilder.append(") ");

        return stringBuilder.toString();
    }

    /**
     * Transforms an event stream message into a {@link SdkHttpFullResponse} so we can reuse our existing generated unmarshallers.
     *
     * @param message Message to transform.
     */
    private SdkHttpFullResponse adaptMessageToResponse(Message message, boolean isException) {
        Map<String, List<String>> headers =
            message.getHeaders()
                   .entrySet()
                   .stream()
                   .collect(HashMap::new, (m, e) -> m.put(e.getKey(), singletonList(e.getValue().getString())), Map::putAll);

        if (requestId != null) {
            headers.put(X_AMZN_REQUEST_ID_HEADER, singletonList(requestId));
        }
        if (extendedRequestId != null) {
            headers.put(X_AMZ_ID_2_HEADER, singletonList(extendedRequestId));
        }

        SdkHttpFullResponse.Builder builder =
            SdkHttpFullResponse.builder()
                               .content(AbortableInputStream.create(new ByteArrayInputStream(message.getPayload())))
                               .headers(headers);

        if (!isException) {
            builder.statusCode(200);
        }

        return builder.build();
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
     * @param m Message frame.
     * @return True if frame is an exception frame, false if not.
     */
    private boolean isException(Message m) {
        return "exception".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * Builder for {@link EventStreamAsyncResponseTransformer}.
     *
     * @param <ResponseT> Initial response type.
     * @param <EventT> Event type being delivered.
     */
    public static final class Builder<ResponseT, EventT> {

        private EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler;
        private HttpResponseHandler<? extends ResponseT> initialResponseHandler;
        private HttpResponseHandler<? extends EventT> eventResponseHandler;
        private HttpResponseHandler<? extends Throwable> exceptionResponseHandler;
        private CompletableFuture<Void> future;
        private String serviceName;

        private Builder() {
        }

        /**
         * @param eventStreamResponseHandler Response handler provided by customer.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> eventStreamResponseHandler(
            EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler) {
            this.eventStreamResponseHandler = eventStreamResponseHandler;
            return this;
        }

        /**
         * @param initialResponseHandler Response handler for the initial-response event stream message.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> initialResponseHandler(
            HttpResponseHandler<? extends ResponseT> initialResponseHandler) {
            this.initialResponseHandler = initialResponseHandler;
            return this;
        }

        /**
         * @param eventResponseHandler Response handler for the various event types.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> eventResponseHandler(HttpResponseHandler<? extends EventT> eventResponseHandler) {
            this.eventResponseHandler = eventResponseHandler;
            return this;
        }

        /**
         * @param exceptionResponseHandler Response handler for error and exception messages.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> exceptionResponseHandler(
            HttpResponseHandler<? extends Throwable> exceptionResponseHandler) {
            this.exceptionResponseHandler = exceptionResponseHandler;
            return this;
        }

        /**
         * This is no longer being used, but is left behind because this is a protected API.
         */
        @Deprecated
        public Builder<ResponseT, EventT> executor(Executor executor) {
            return this;
        }

        /**
         * @param future Future to notify when the last event has been delivered.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> future(CompletableFuture<Void> future) {
            this.future = future;
            return this;
        }

        /**
         * @param serviceName Descriptive name for the service to be used in exception unmarshalling.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public EventStreamAsyncResponseTransformer<ResponseT, EventT> build() {
            return new EventStreamAsyncResponseTransformer<>(eventStreamResponseHandler,
                                                             initialResponseHandler,
                                                             eventResponseHandler,
                                                             exceptionResponseHandler,
                                                             future,
                                                             serviceName);
        }
    }
}
