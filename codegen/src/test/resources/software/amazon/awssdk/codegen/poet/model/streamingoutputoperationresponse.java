package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Optional;
import javax.annotation.Generated;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class StreamingOutputOperationResponse extends JsonProtocolTestsResponse implements
                                                                                ToCopyableBuilder<StreamingOutputOperationResponse.Builder, StreamingOutputOperationResponse> {
    private StreamingOutputOperationResponse(BuilderImpl builder) {
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
        if (!(obj instanceof StreamingOutputOperationResponse)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToString.builder("StreamingOutputOperationResponse").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public interface Builder extends JsonProtocolTestsResponse.Builder,
                                     CopyableBuilder<Builder, StreamingOutputOperationResponse> {
    }

    static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(StreamingOutputOperationResponse model) {
        }

        @Override
        public StreamingOutputOperationResponse build() {
            return new StreamingOutputOperationResponse(this);
        }
    }
}
