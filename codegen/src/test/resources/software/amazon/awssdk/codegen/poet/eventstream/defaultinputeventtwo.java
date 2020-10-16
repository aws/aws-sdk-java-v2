package software.amazon.awssdk.services.jsonprotocoltests.model.inputeventstreamtwo;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.jsonprotocoltests.model.InputEventTwo;

/**
 * A specialization of {@code software.amazon.awssdk.services.jsonprotocoltests.model.InputEventTwo} that represents the
 * {@code InputEventStreamTwo#InputEventTwo} event. Do not use this class directly. Instead, use the static builder
 * methods on {@link software.amazon.awssdk.services.jsonprotocoltests.model.InputEventStreamTwo}.
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

