package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class ExistenceCheckNamingResponse extends JsonProtocolTestsResponse implements
        ToCopyableBuilder<ExistenceCheckNamingResponse.Builder, ExistenceCheckNamingResponse> {
    private static final SdkField<List<String>> BUILD_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("Build")
            .getter(getter(ExistenceCheckNamingResponse::build))
            .setter(setter(Builder::build))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Build").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<String>> SUPER_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("super")
            .getter(getter(ExistenceCheckNamingResponse::superValue))
            .setter(setter(Builder::superValue))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("super").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static final SdkField<Map<String, String>> TO_STRING_FIELD = SdkField
            .<Map<String, String>> builder(MarshallingType.MAP)
            .memberName("toString")
            .getter(getter(ExistenceCheckNamingResponse::toStringValue))
            .setter(setter(Builder::toStringValue))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("toString").build(),
                    MapTrait.builder()
                            .keyLocationName("key")
                            .valueLocationName("value")
                            .valueFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, String>> EQUALS_FIELD = SdkField
            .<Map<String, String>> builder(MarshallingType.MAP)
            .memberName("equals")
            .getter(getter(ExistenceCheckNamingResponse::equalsValue))
            .setter(setter(Builder::equalsValue))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("equals").build(),
                    MapTrait.builder()
                            .keyLocationName("key")
                            .valueLocationName("value")
                            .valueFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("value").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BUILD_FIELD, SUPER_FIELD,
            TO_STRING_FIELD, EQUALS_FIELD));

    private final List<String> build;

    private final List<String> superValue;

    private final Map<String, String> toStringValue;

    private final Map<String, String> equalsValue;

    private ExistenceCheckNamingResponse(BuilderImpl builder) {
        super(builder);
        this.build = builder.build;
        this.superValue = builder.superValue;
        this.toStringValue = builder.toStringValue;
        this.equalsValue = builder.equalsValue;
    }

    /**
     * Returns true if the Build property was specified by the sender (it may be empty), or false if the sender did not
     * specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public final boolean hasBuild() {
        return build != null && !(build instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the Build property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasBuild()} to see if a value was sent in this field.
     * </p>
     * 
     * @return The value of the Build property for this object.
     */
    public final List<String> build() {
        return build;
    }

    /**
     * Returns true if the Super property was specified by the sender (it may be empty), or false if the sender did not
     * specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public final boolean hasSuperValue() {
        return superValue != null && !(superValue instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the Super property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasSuperValue()} to see if a value was sent in this field.
     * </p>
     * 
     * @return The value of the Super property for this object.
     */
    public final List<String> superValue() {
        return superValue;
    }

    /**
     * Returns true if the ToString property was specified by the sender (it may be empty), or false if the sender did
     * not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public final boolean hasToStringValue() {
        return toStringValue != null && !(toStringValue instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the ToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasToStringValue()} to see if a value was sent in this field.
     * </p>
     * 
     * @return The value of the ToString property for this object.
     */
    public final Map<String, String> toStringValue() {
        return toStringValue;
    }

    /**
     * Returns true if the Equals property was specified by the sender (it may be empty), or false if the sender did not
     * specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public final boolean hasEqualsValue() {
        return equalsValue != null && !(equalsValue instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the Equals property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasEqualsValue()} to see if a value was sent in this field.
     * </p>
     * 
     * @return The value of the Equals property for this object.
     */
    public final Map<String, String> equalsValue() {
        return equalsValue;
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
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(build());
        hashCode = 31 * hashCode + Objects.hashCode(superValue());
        hashCode = 31 * hashCode + Objects.hashCode(toStringValue());
        hashCode = 31 * hashCode + Objects.hashCode(equalsValue());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExistenceCheckNamingResponse)) {
            return false;
        }
        ExistenceCheckNamingResponse other = (ExistenceCheckNamingResponse) obj;
        return Objects.equals(build(), other.build()) && Objects.equals(superValue(), other.superValue())
                && Objects.equals(toStringValue(), other.toStringValue()) && Objects.equals(equalsValue(), other.equalsValue());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExistenceCheckNamingResponse").add("Build", build()).add("Super", superValue())
                .add("ToString", toStringValue()).add("Equals", equalsValue()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "Build":
            return Optional.ofNullable(clazz.cast(build()));
        case "super":
            return Optional.ofNullable(clazz.cast(superValue()));
        case "toString":
            return Optional.ofNullable(clazz.cast(toStringValue()));
        case "equals":
            return Optional.ofNullable(clazz.cast(equalsValue()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<ExistenceCheckNamingResponse, T> g) {
        return obj -> g.apply((ExistenceCheckNamingResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends JsonProtocolTestsResponse.Builder, SdkPojo,
            CopyableBuilder<Builder, ExistenceCheckNamingResponse> {
        /**
         * Sets the value of the Build property for this object.
         *
         * @param build
         *        The new value for the Build property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder build(Collection<String> build);

        /**
         * Sets the value of the Build property for this object.
         *
         * @param build
         *        The new value for the Build property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder build(String... build);

        /**
         * Sets the value of the Super property for this object.
         *
         * @param superValue
         *        The new value for the Super property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder superValue(Collection<String> superValue);

        /**
         * Sets the value of the Super property for this object.
         *
         * @param superValue
         *        The new value for the Super property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder superValue(String... superValue);

        /**
         * Sets the value of the ToString property for this object.
         *
         * @param toStringValue
         *        The new value for the ToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder toStringValue(Map<String, String> toStringValue);

        /**
         * Sets the value of the Equals property for this object.
         *
         * @param equalsValue
         *        The new value for the Equals property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder equalsValue(Map<String, String> equalsValue);
    }

    static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
        private List<String> build;

        private List<String> superValue;

        private Map<String, String> toStringValue;

        private Map<String, String> equalsValue;

        private BuilderImpl() {
        }

        private BuilderImpl(ExistenceCheckNamingResponse model) {
            super(model);
            build(model.build);
            superValue(model.superValue);
            toStringValue(model.toStringValue);
            equalsValue(model.equalsValue);
        }

        public final Collection<String> getBuild() {
            return build;
        }

        @Override
        public final Builder build(Collection<String> build) {
            this.build = ListOfStringsCopier.copy(build);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder build(String... build) {
            build(Arrays.asList(build));
            return this;
        }

        public final void setBuild(Collection<String> build) {
            this.build = ListOfStringsCopier.copy(build);
        }

        public final Collection<String> getSuperValue() {
            return superValue;
        }

        @Override
        public final Builder superValue(Collection<String> superValue) {
            this.superValue = ListOfStringsCopier.copy(superValue);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder superValue(String... superValue) {
            superValue(Arrays.asList(superValue));
            return this;
        }

        public final void setSuperValue(Collection<String> superValue) {
            this.superValue = ListOfStringsCopier.copy(superValue);
        }

        public final Map<String, String> getToStringValue() {
            return toStringValue;
        }

        @Override
        public final Builder toStringValue(Map<String, String> toStringValue) {
            this.toStringValue = MapOfStringToStringCopier.copy(toStringValue);
            return this;
        }

        public final void setToStringValue(Map<String, String> toStringValue) {
            this.toStringValue = MapOfStringToStringCopier.copy(toStringValue);
        }

        public final Map<String, String> getEqualsValue() {
            return equalsValue;
        }

        @Override
        public final Builder equalsValue(Map<String, String> equalsValue) {
            this.equalsValue = MapOfStringToStringCopier.copy(equalsValue);
            return this;
        }

        public final void setEqualsValue(Map<String, String> equalsValue) {
            this.equalsValue = MapOfStringToStringCopier.copy(equalsValue);
        }

        @Override
        public ExistenceCheckNamingResponse build() {
            return new ExistenceCheckNamingResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}

