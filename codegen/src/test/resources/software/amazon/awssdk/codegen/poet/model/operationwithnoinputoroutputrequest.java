package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Generated;
import software.amazon.awssdk.core.AwsRequestOverrideConfig;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public class OperationWithNoInputOrOutputRequest extends JsonProtocolTestsRequest implements
                                                                                  ToCopyableBuilder<OperationWithNoInputOrOutputRequest.Builder, OperationWithNoInputOrOutputRequest> {
    private OperationWithNoInputOrOutputRequest(BuilderImpl builder) {
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
        if (!(obj instanceof OperationWithNoInputOrOutputRequest)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return ToString.builder("OperationWithNoInputOrOutputRequest").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder,
                                     CopyableBuilder<Builder, OperationWithNoInputOrOutputRequest> {
        @Override
        Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(OperationWithNoInputOrOutputRequest model) {
        }

        @Override
        public Builder requestOverrideConfig(AwsRequestOverrideConfig awsRequestOverrideConfig) {
            super.requestOverrideConfig(awsRequestOverrideConfig);
            return this;
        }

        @Override
        public Builder requestOverrideConfig(Consumer<AwsRequestOverrideConfig.Builder> builderConsumer) {
            super.requestOverrideConfig(builderConsumer);
            return this;
        }

        @Override
        public OperationWithNoInputOrOutputRequest build() {
            return new OperationWithNoInputOrOutputRequest(this);
        }
    }
}
