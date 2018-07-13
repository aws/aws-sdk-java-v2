package software.amazon.awssdk.services.json.model;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.eventstream.DefaultEventStreamResponseHandlerBuilder;
import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandlerFromBuilder;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultEventStreamOperationResponseHandlerBuilder
        extends
        DefaultEventStreamResponseHandlerBuilder<EventStreamOperationResponse, EventStream, EventStreamOperationResponseHandler.Builder>
        implements EventStreamOperationResponseHandler.Builder {
    @Override
    public EventStreamOperationResponseHandler.Builder subscriber(EventStreamOperationResponseHandler.Visitor visitor) {
        subscriber(e -> e.accept(visitor));
        return this;
    }

    @Override
    public EventStreamOperationResponseHandler build() {
        return new Impl(this);
    }

    @Generated("software.amazon.awssdk:codegen")
    private static final class Impl extends EventStreamResponseHandlerFromBuilder<EventStreamOperationResponse, EventStream>
            implements EventStreamOperationResponseHandler {
        private Impl(DefaultEventStreamOperationResponseHandlerBuilder builder) {
            super(builder);
        }
    }
}
