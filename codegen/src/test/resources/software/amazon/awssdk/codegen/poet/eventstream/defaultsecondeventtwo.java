package software.amazon.awssdk.services.jsonprotocoltests.model.eventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventTwo;

/**
 * A specialization of {@code software.amazon.awssdk.services.jsonprotocoltests.model.EventTwo} that represents the
 * {@code EventStream#SecondEventTwo} event. Do not use this class directly. Instead, use the static builder methods on
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.EventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultSecondEventTwo extends EventTwo {
    private static final long serialVersionUID = 1L;

    DefaultSecondEventTwo(BuilderImpl builderImpl) {
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
        visitor.visitSecondEventTwo(this);
    }

    public interface Builder extends EventTwo.Builder {
        @Override
        DefaultSecondEventTwo build();
    }

    private static final class BuilderImpl extends EventTwo.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultSecondEventTwo event) {
            super(event);
        }

        @Override
        public DefaultSecondEventTwo build() {
            return new DefaultSecondEventTwo(this);
        }
    }
}

