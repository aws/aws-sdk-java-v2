package software.amazon.awssdk.services.sharedeventstream.model.streambirthsinputeventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sharedeventstream.model.StreamBirthsInputEventStream;
import software.amazon.awssdk.services.sharedeventstream.model.StreamBirthsInputEventStreamPerson;

/**
 * A specialization of
 * {@code software.amazon.awssdk.services.sharedeventstream.model.StreamBirthsInputEventStreamPerson} that represents
 * the {@code StreamBirthsInputEventStream$Person} event. Do not use this class directly. Instead, use the static
 * builder methods on {@link software.amazon.awssdk.services.sharedeventstream.model.StreamBirthsInputEventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultPerson extends StreamBirthsInputEventStreamPerson {
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
    public StreamBirthsInputEventStream.EventType sdkEventType() {
        return StreamBirthsInputEventStream.EventType.PERSON;
    }

    public interface Builder extends StreamBirthsInputEventStreamPerson.Builder {
        @Override
        DefaultPerson build();
    }

    private static final class BuilderImpl extends StreamBirthsInputEventStreamPerson.BuilderImpl implements Builder {
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
