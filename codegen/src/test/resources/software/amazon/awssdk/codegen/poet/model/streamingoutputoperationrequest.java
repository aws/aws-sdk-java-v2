package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public class StreamingOutputOperationRequest extends JsonProtocolTestsRequest implements
                                                                              ToCopyableBuilder<StreamingOutputOperationRequest.Builder, StreamingOutputOperationRequest> {
    private StreamingOutputOperationRequest(BuilderImpl builder) {
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
        if (!(obj instanceof StreamingOutputOperationRequest)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToString.builder("StreamingOutputOperationRequest").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder, CopyableBuilder<Builder, StreamingOutputOperationRequest> {
        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(StreamingOutputOperationRequest model) {
            super(model);
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public StreamingOutputOperationRequest build() {
            return new StreamingOutputOperationRequest(this);
        }
    }
}
