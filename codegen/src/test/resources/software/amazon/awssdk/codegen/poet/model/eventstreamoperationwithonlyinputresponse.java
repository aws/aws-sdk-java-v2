package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Optional;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public final class EventStreamOperationWithOnlyInputResponse extends JsonProtocolTestsResponse implements
                                                                                               ToCopyableBuilder<EventStreamOperationWithOnlyInputResponse.Builder, EventStreamOperationWithOnlyInputResponse> {
    private EventStreamOperationWithOnlyInputResponse(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EventStreamOperationWithOnlyInputResponse)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToString.builder("EventStreamOperationWithOnlyInputResponse").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public interface Builder extends JsonProtocolTestsResponse.Builder,
                                     CopyableBuilder<Builder, EventStreamOperationWithOnlyInputResponse> {
    }

    static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(EventStreamOperationWithOnlyInputResponse model) {
            super(model);
        }

        @Override
        public EventStreamOperationWithOnlyInputResponse build() {
            return new EventStreamOperationWithOnlyInputResponse(this);
        }
    }
}
