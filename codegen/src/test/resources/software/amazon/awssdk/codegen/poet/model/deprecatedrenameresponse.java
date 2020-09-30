package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
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
public final class DeprecatedRenameResponse extends JsonProtocolTestsResponse implements
                                                                              ToCopyableBuilder<DeprecatedRenameResponse.Builder, DeprecatedRenameResponse> {
    private static final SdkField<String> ORIGINAL_NAME_NO_DEPRECATION_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("OriginalNameNoDeprecation").getter(getter(DeprecatedRenameResponse::originalNameNoDeprecation))
        .setter(setter(Builder::originalNameNoDeprecation))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OriginalNameNoDeprecation").build())
        .build();

    private static final SdkField<String> ORIGINAL_NAME_DEPRECATED_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("OriginalNameDeprecated").getter(getter(DeprecatedRenameResponse::originalNameDeprecated))
        .setter(setter(Builder::originalNameDeprecated))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("OriginalNameDeprecated").build())
        .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(
        ORIGINAL_NAME_NO_DEPRECATION_FIELD, ORIGINAL_NAME_DEPRECATED_FIELD));

    private final String originalNameNoDeprecation;

    private final String originalNameDeprecated;

    private DeprecatedRenameResponse(BuilderImpl builder) {
        super(builder);
        this.originalNameNoDeprecation = builder.originalNameNoDeprecation;
        this.originalNameDeprecated = builder.originalNameDeprecated;
    }

    /**
     * Returns the value of the OriginalNameNoDeprecation property for this object.
     *
     * @return The value of the OriginalNameNoDeprecation property for this object.
     */
    public String originalNameNoDeprecation() {
        return originalNameNoDeprecation;
    }

    /**
     * Returns the value of the OriginalNameDeprecated property for this object.
     *
     * @return The value of the OriginalNameDeprecated property for this object.
     */
    public String originalNameDeprecated() {
        return originalNameDeprecated;
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
        hashCode = 31 * hashCode + Objects.hashCode(originalNameNoDeprecation());
        hashCode = 31 * hashCode + Objects.hashCode(originalNameDeprecated());
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
        if (!(obj instanceof DeprecatedRenameResponse)) {
            return false;
        }
        DeprecatedRenameResponse other = (DeprecatedRenameResponse) obj;
        return Objects.equals(originalNameNoDeprecation(), other.originalNameNoDeprecation())
               && Objects.equals(originalNameDeprecated(), other.originalNameDeprecated());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("DeprecatedRenameResponse").add("OriginalNameNoDeprecation", originalNameNoDeprecation())
                       .add("OriginalNameDeprecated", originalNameDeprecated()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "OriginalNameNoDeprecation":
                return Optional.ofNullable(clazz.cast(originalNameNoDeprecation()));
            case "OriginalNameDeprecated":
                return Optional.ofNullable(clazz.cast(originalNameDeprecated()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<DeprecatedRenameResponse, T> g) {
        return obj -> g.apply((DeprecatedRenameResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends JsonProtocolTestsResponse.Builder, SdkPojo,
                                     CopyableBuilder<Builder, DeprecatedRenameResponse> {
        /**
         * Sets the value of the OriginalNameNoDeprecation property for this object.
         *
         * @param originalNameNoDeprecation
         *        The new value for the OriginalNameNoDeprecation property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder originalNameNoDeprecation(String originalNameNoDeprecation);

        /**
         * Sets the value of the OriginalNameDeprecated property for this object.
         *
         * @param originalNameDeprecated
         *        The new value for the OriginalNameDeprecated property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder originalNameDeprecated(String originalNameDeprecated);
    }

    static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
        private String originalNameNoDeprecation;

        private String originalNameDeprecated;

        private BuilderImpl() {
        }

        private BuilderImpl(DeprecatedRenameResponse model) {
            super(model);
            originalNameNoDeprecation(model.originalNameNoDeprecation);
            originalNameDeprecated(model.originalNameDeprecated);
        }

        public final String getOriginalNameNoDeprecation() {
            return originalNameNoDeprecation;
        }

        @Override
        public final Builder originalNameNoDeprecation(String originalNameNoDeprecation) {
            this.originalNameNoDeprecation = originalNameNoDeprecation;
            return this;
        }

        public final void setOriginalNameNoDeprecation(String originalNameNoDeprecation) {
            this.originalNameNoDeprecation = originalNameNoDeprecation;
        }

        public final String getOriginalNameDeprecated() {
            return originalNameDeprecated;
        }

        @Override
        public final Builder originalNameDeprecated(String originalNameDeprecated) {
            this.originalNameDeprecated = originalNameDeprecated;
            return this;
        }

        public final void setOriginalNameDeprecated(String originalNameDeprecated) {
            this.originalNameDeprecated = originalNameDeprecated;
        }

        @Override
        public DeprecatedRenameResponse build() {
            return new DeprecatedRenameResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
