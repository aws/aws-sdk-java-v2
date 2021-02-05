package software.amazon.awssdk.services.json.model.inputeventstreamtwo;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.json.model.InputEventStreamTwo;
import software.amazon.awssdk.services.json.model.InputEventTwo;

/**
 * A specialization of {@code software.amazon.awssdk.services.json.model.InputEventTwo} that represents the
 * {@code InputEventStreamTwo$InputEventTwo} event. Do not use this class directly. Instead, use the static builder
 * methods on {@link software.amazon.awssdk.services.json.model.InputEventStreamTwo}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultInputEventTwo extends InputEventTwo {
    private static final long serialVersionUID = 1L;

    DefaultInputEventTwo(BuilderImpl builderImpl) {
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
        return InputEventStreamTwo.EventType.INPUT_EVENT_TWO;
    }

    public interface Builder extends InputEventTwo.Builder {
        @Override
        DefaultInputEventTwo build();
    }

    private static final class BuilderImpl extends InputEventTwo.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultInputEventTwo event) {
            super(event);
        }

        @Override
        public DefaultInputEventTwo build() {
            return new DefaultInputEventTwo(this);
        }
    }
}

