package software.amazon.awssdk.services.sharedeventstream.model.streamdeathsinputeventstream;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sharedeventstream.model.StreamDeathsInputEventStream;
import software.amazon.awssdk.services.sharedeventstream.model.StreamDeathsInputEventStreamPerson;

/**
 * A specialization of
 * {@code software.amazon.awssdk.services.sharedeventstream.model.StreamDeathsInputEventStreamPerson} that represents
 * the {@code StreamDeathsInputEventStream$Person} event. Do not use this class directly. Instead, use the static
 * builder methods on {@link software.amazon.awssdk.services.sharedeventstream.model.StreamDeathsInputEventStream}.
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultPerson extends StreamDeathsInputEventStreamPerson {
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
    public StreamDeathsInputEventStream.EventType sdkEventType() {
        return StreamDeathsInputEventStream.EventType.PERSON;
    }

    public interface Builder extends StreamDeathsInputEventStreamPerson.Builder {
        @Override
        DefaultPerson build();
    }

    private static final class BuilderImpl extends StreamDeathsInputEventStreamPerson.BuilderImpl implements Builder {
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
