package software.amazon.awssdk.services.json.model.inputeventstreamtwo;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.json.model.InputEvent;
import software.amazon.awssdk.services.json.model.InputEventStreamTwo;

/**
 * A specialization of {@code software.amazon.awssdk.services.json.model.InputEvent} that represents the
 * {@code InputEventStreamTwo$InputEventOne} event. Do not use this class directly. Instead, use the static builder
 * methods on {@link software.amazon.awssdk.services.json.model.InputEventStreamTwo}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultInputEventOne extends InputEvent {
    private static final long serialVersionUID = 1L;

    DefaultInputEventOne(BuilderImpl builderImpl) {
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
    public InputEventStreamTwo.EventType sdkEventType() {
        return InputEventStreamTwo.EventType.INPUT_EVENT_ONE;
    }

    public interface Builder extends InputEvent.Builder {
        @Override
        DefaultInputEventOne build();
    }

    private static final class BuilderImpl extends InputEvent.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultInputEventOne event) {
            super(event);
        }

        @Override
        public DefaultInputEventOne build() {
            return new DefaultInputEventOne(this);
        }
    }
}

