package software.amazon.awssdk.services.json.model.eventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.json.model.EventStream;
import software.amazon.awssdk.services.json.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.json.model.EventStreamOperationWithOnlyOutputResponseHandler;
import software.amazon.awssdk.services.json.model.EventTwo;

/**
 * A specialization of {@code software.amazon.awssdk.services.json.model.EventTwo} that represents the
 * {@code EventStream$secondeventtwo} event. Do not use this class directly. Instead, use the static builder methods on
 * {@link software.amazon.awssdk.services.json.model.EventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultSecondeventtwo extends EventTwo {
    private static final long serialVersionUID = 1L;

    DefaultSecondeventtwo(BuilderImpl builderImpl) {
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
        visitor.visitSecondeventtwo(this);
    }

    @Override
    public void accept(EventStreamOperationWithOnlyOutputResponseHandler.Visitor visitor) {
        visitor.visitSecondeventtwo(this);
    }

    @Override
    public EventStream.EventType sdkEventType() {
        return EventStream.EventType.SECONDEVENTTWO;
    }

    public interface Builder extends EventTwo.Builder {
        @Override
        DefaultSecondeventtwo build();
    }

    private static final class BuilderImpl extends EventTwo.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultSecondeventtwo event) {
            super(event);
        }

        @Override
        public DefaultSecondeventtwo build() {
            return new DefaultSecondeventtwo(this);
        }
    }
}

