package software.amazon.awssdk.services.json.model.eventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.json.model.EventOne;
import software.amazon.awssdk.services.json.model.EventStream;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputResponseHandler;

/**
 * A specialization of {@code software.amazon.awssdk.services.json.model.EventOne} that represents the
 * {@code EventStream$EventOne} event. Do not use this class directly. Instead, use the static builder methods on
 * {@link software.amazon.awssdk.services.json.model.EventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultEventOne extends EventOne {
    private static final long serialVersionUID = 1L;

    DefaultEventOne(BuilderImpl builderImpl) {
        super(builderImpl);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public void accept(EventStreamOperationResponseHandler.Visitor visitor) {
        visitor.visitEventOne(this);
    }

    @Override
    public void accept(EventStreamOperationWithOnlyOutputResponseHandler.Visitor visitor) {
        visitor.visitEventOne(this);
    }

    @Override
    public EventStream.EventType sdkEventType() {
        return EventStream.EventType.EVENT_ONE;
    }

    public interface Builder extends EventOne.Builder {
        @Override
        DefaultEventOne build();
    }

    private static final class BuilderImpl extends EventOne.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultEventOne event) {
            super(event);
        }

        @Override
        public DefaultEventOne build() {
            return new DefaultEventOne(this);
        }
    }
}

