package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class DeprecatedRenameRequest extends JsonProtocolTestsRequest implements
                                                                            ToCopyableBuilder<DeprecatedRenameRequest.Builder, DeprecatedRenameRequest> {
    private static final SdkField<String> NEW_NAME_NO_DEPRECATION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("NewNameNoDeprecation").getter(getter(DeprecatedRenameRequest::newNameNoDeprecation))
        .setter(setter(Builder::newNameNoDeprecation))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OriginalNameNoDeprecation").build())
        .build();

    private static final SdkField<String> NEW_NAME_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("NewName").getter(getter(DeprecatedRenameRequest::newName)).setter(setter(Builder::newName))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OriginalNameDeprecated").build())
        .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NEW_NAME_NO_DEPRECATION_FIELD,
                                                                                                   NEW_NAME_FIELD));

    private final String newNameNoDeprecation;

    private final String newName;

    private DeprecatedRenameRequest(BuilderImpl builder) {
        super(builder);
        this.newNameNoDeprecation = builder.newNameNoDeprecation;
        this.newName = builder.newName;
    }

    /**
     * Returns the value of the NewNameNoDeprecation property for this object.
     *
     * @return The value of the NewNameNoDeprecation property for this object.
     */
    public String newNameNoDeprecation() {
        return newNameNoDeprecation;
    }

    /**
     * Returns the value of the NewName property for this object.
     *
     * @return The value of the NewName property for this object.
     * @deprecated Use {@link #newName()}
     */
    @Deprecated
    public String originalNameDeprecated() {
        return newName;
    }

    /**
     * Returns the value of the NewName property for this object.
     *
     * @return The value of the NewName property for this object.
     */
    public String newName() {
        return newName;
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
        hashCode = 31 * hashCode + Objects.hashCode(newNameNoDeprecation());
        hashCode = 31 * hashCode + Objects.hashCode(newName());
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
        if (!(obj instanceof DeprecatedRenameRequest)) {
            return false;
        }
        DeprecatedRenameRequest other = (DeprecatedRenameRequest) obj;
        return Objects.equals(newNameNoDeprecation(), other.newNameNoDeprecation()) && Objects.equals(newName(), other.newName());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("DeprecatedRenameRequest").add("NewNameNoDeprecation", newNameNoDeprecation())
                       .add("NewName", newName()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "NewNameNoDeprecation":
                return Optional.ofNullable(clazz.cast(newNameNoDeprecation()));
            case "NewName":
                return Optional.ofNullable(clazz.cast(newName()));
            case "OriginalNameDeprecated":
                return Optional.ofNullable(clazz.cast(newName()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DeprecatedRenameRequest, T> g) {
        return obj -> g.apply((DeprecatedRenameRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder, SdkPojo, CopyableBuilder<Builder, DeprecatedRenameRequest> {
        /**
         * Sets the value of the NewNameNoDeprecation property for this object.
         *
         * @param newNameNoDeprecation
         *        The new value for the NewNameNoDeprecation property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder newNameNoDeprecation(String newNameNoDeprecation);

        /**
         * Sets the value of the NewName property for this object.
         *
         * @param newName
         *        The new value for the NewName property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder newName(String newName);

        /**
         * Sets the value of the NewName property for this object.
         *
         * @param newName
         *        The new value for the NewName property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         * @deprecated Use {@link #newName(String)}
         */
        @Deprecated
        Builder originalNameDeprecated(String newName);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private String newNameNoDeprecation;

        private String newName;

        private BuilderImpl() {
        }

        private BuilderImpl(DeprecatedRenameRequest model) {
            super(model);
            newNameNoDeprecation(model.newNameNoDeprecation);
            newName(model.newName);
        }

        public final String getNewNameNoDeprecation() {
            return newNameNoDeprecation;
        }

        @Override
        public final Builder newNameNoDeprecation(String newNameNoDeprecation) {
            this.newNameNoDeprecation = newNameNoDeprecation;
            return this;
        }

        public final void setNewNameNoDeprecation(String newNameNoDeprecation) {
            this.newNameNoDeprecation = newNameNoDeprecation;
        }

        public final String getNewName() {
            return newName;
        }

        @Override
        public final Builder newName(String newName) {
            this.newName = newName;
            return this;
        }

        @Override
        public final Builder originalNameDeprecated(String newName) {
            this.newName = newName;
            return this;
        }

        public final void setNewName(String newName) {
            this.newName = newName;
        }

        /**
         * @deprecated Use {@link #setNewName} instead
         */
        @Deprecated
        public final void setOriginalNameDeprecated(String newName) {
            this.newName = newName;
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
        public DeprecatedRenameRequest build() {
            return new DeprecatedRenameRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
