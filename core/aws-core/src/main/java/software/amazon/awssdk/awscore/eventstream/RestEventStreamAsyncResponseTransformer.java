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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

/**
 * Unmarshalling layer on top of the {@link AsyncResponseTransformer} to decode event stream messages for Rest services
 * and deliver them to the subscriber.
 *
 * @param <ResponseT> Initial response type of event stream operation.
 * @param <EventT> Base type of event stream message frames.
 */
@SdkProtectedApi
public class RestEventStreamAsyncResponseTransformer<ResponseT extends SdkResponse, EventT>
    implements AsyncResponseTransformer<ResponseT, Void> {

    private final EventStreamAsyncResponseTransformer<ResponseT, EventT> delegate;
    private final EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler;

    private RestEventStreamAsyncResponseTransformer(
        EventStreamAsyncResponseTransformer<ResponseT, EventT> delegateAsyncResponseTransformer,
        EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler) {
        this.delegate = delegateAsyncResponseTransformer;
        this.eventStreamResponseHandler = eventStreamResponseHandler;
    }

    @Override
    public CompletableFuture<Void> prepare() {
        return delegate.prepare();
    }

    @Override
    public void onResponse(ResponseT response) {
        delegate.onResponse(response);
        eventStreamResponseHandler.responseReceived(response);
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        delegate.onStream(publisher);
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        delegate.exceptionOccurred(throwable);
    }

    public static <ResponseT extends SdkResponse, EventT> Builder<ResponseT, EventT> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link RestEventStreamAsyncResponseTransformer}.
     *
     * @param <ResponseT> Initial response type.
     * @param <EventT> Event type being delivered.
     */
    public static final class Builder<ResponseT extends SdkResponse, EventT> {
        private EventStreamAsyncResponseTransformer<ResponseT, EventT> delegateAsyncResponseTransformer;
        private EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler;

        private Builder() {
        }

        /**
         * @param delegateAsyncResponseTransformer {@link EventStreamAsyncResponseTransformer} that can be delegated the work
         * common for RPC and REST services
         * @return This object for method chaining
         */
        public Builder<ResponseT, EventT> eventStreamAsyncResponseTransformer(
            EventStreamAsyncResponseTransformer<ResponseT, EventT> delegateAsyncResponseTransformer) {
            this.delegateAsyncResponseTransformer = delegateAsyncResponseTransformer;
            return this;
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

        public RestEventStreamAsyncResponseTransformer<ResponseT, EventT> build() {
            return new RestEventStreamAsyncResponseTransformer<>(delegateAsyncResponseTransformer,
                                                                 eventStreamResponseHandler);
        }
    }
}
