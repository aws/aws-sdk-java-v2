package software.amazon.awssdk.services.jsonprotocoltests.model.eventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventOne;
import software.amazon.awssdk.services.jsonprotocoltests.model.EventStreamOperationResponseHandler;

/**
 * A specialization of {@code software.amazon.awssdk.services.jsonprotocoltests.model.EventOne} that represents the
 * {@code EventStream#SecondEventOne} event. Do not use this class directly. Instead, use the static builder methods on
 * {@link software.amazon.awssdk.services.jsonprotocoltests.model.EventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultSecondEventOne extends EventOne {
    private static final long serialVersionUID = 1L;

    DefaultSecondEventOne(BuilderImpl builderImpl) {
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
        visitor.visitSecondEventOne(this);
    }

    public interface Builder extends EventOne.Builder {
        @Override
        DefaultSecondEventOne build();
    }

    private static final class BuilderImpl extends EventOne.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultSecondEventOne event) {
            super(event);
        }

        @Override
        public DefaultSecondEventOne build() {
            return new DefaultSecondEventOne(this);
        }
    }
}

