package software.amazon.awssdk.services.jsonprotocoltests.model.inputeventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.jsonprotocoltests.model.InputEvent;

/**
 * A specialization of {@code software.amazon.awssdk.services.jsonprotocoltests.model.InputEvent} that represents the
 * {@code InputEventStream#InputEvent} event. Do not use this class directly. Instead, use the static builder methods on
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.InputEventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultInputEvent extends InputEvent {
    private static final long serialVersionUID = 1L;

    DefaultInputEvent(BuilderImpl builderImpl) {
        super(builderImpl);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends InputEvent.Builder {
        @Override
        DefaultInputEvent build();
    }

    private static final class BuilderImpl extends InputEvent.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultInputEvent event) {
            super(event);
        }

        @Override
        public DefaultInputEvent build() {
            return new DefaultInputEvent(this);
        }
    }
}

