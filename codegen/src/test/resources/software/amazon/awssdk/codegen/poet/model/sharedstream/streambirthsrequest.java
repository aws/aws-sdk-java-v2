package software.amazon.awssdk.services.sharedeventstream.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Generated("software.amazon.awssdk:codegen")
public final class StreamBirthsRequest extends SharedEventStreamRequest implements ToCopyableBuilder<StreamBirthsRequest.Builder, StreamBirthsRequest> {
    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList());

    private StreamBirthsRequest(BuilderImpl builder) {
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
        hashCode = 31 * hashCode + super.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof StreamBirthsRequest)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("StreamBirthsRequest").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    public interface Builder extends SharedEventStreamRequest.Builder, SdkPojo, CopyableBuilder<Builder, StreamBirthsRequest> {
        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(
            Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends SharedEventStreamRequest.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(StreamBirthsRequest model) {
            super(model);
        }

        @Override
        public Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration) {
            super.overrideConfiguration(overrideConfiguration);
            return this;
        }

        @Override
        public Builder overrideConfiguration(
            Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer) {
            super.overrideConfiguration(builderConsumer);
            return this;
        }

        @Override
        public StreamBirthsRequest build() {
            return new StreamBirthsRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
