package software.amazon.awssdk.services.sharedeventstream.model.eventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sharedeventstream.model.EventStream;
import software.amazon.awssdk.services.sharedeventstream.model.EventStreamPerson;
import software.amazon.awssdk.services.sharedeventstream.model.StreamBirthsResponseHandler;
import software.amazon.awssdk.services.sharedeventstream.model.StreamDeathsResponseHandler;

/**
 * A specialization of {@code software.amazon.awssdk.services.sharedeventstream.model.EventStreamPerson} that represents
 * the {@code EventStream$Person} event. Do not use this class directly. Instead, use the static builder methods on
 * {@link software.amazon.awssdk.services.sharedeventstream.model.EventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultPerson extends EventStreamPerson {
    private static final long serialVersionUID = 1L;

    DefaultPerson(BuilderImpl builderImpl) {
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
    public void accept(StreamBirthsResponseHandler.Visitor visitor) {
        visitor.visitPerson(this);
    }

    @Override
    public void accept(StreamDeathsResponseHandler.Visitor visitor) {
        visitor.visitPerson(this);
    }

    @Override
    public EventStream.EventType sdkEventType() {
        return EventStream.EventType.PERSON;
    }

    public interface Builder extends EventStreamPerson.Builder {
        @Override
        DefaultPerson build();
    }

    private static final class BuilderImpl extends EventStreamPerson.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(DefaultPerson event) {
            super(event);
        }

        @Override
        public DefaultPerson build() {
            return new DefaultPerson(this);
        }
    }
}
