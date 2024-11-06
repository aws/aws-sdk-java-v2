package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
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

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = Collections
            .unmodifiableMap(new HashMap<String, SdkField<?>>() {
                {
                    put("Build", BUILD_FIELD);
                    put("super", SUPER_FIELD);
                    put("toString", TO_STRING_FIELD);
                    put("equals", EQUALS_FIELD);
                }
            });

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
     * For responses, this returns true if the service returned a value for the Build property. This DOES NOT check that
     * the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful
     * because the SDK will never return a null collection or map, but you may need to differentiate between the service
     * returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true
     * if a value for the property was specified in the request builder, and false if a value was not specified.
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
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasBuild} method.
     * </p>
     * 
     * @return The value of the Build property for this object.
     */
    public final List<String> build() {
        return build;
    }

    /**
     * For responses, this returns true if the service returned a value for the Super property. This DOES NOT check that
     * the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is useful
     * because the SDK will never return a null collection or map, but you may need to differentiate between the service
     * returning nothing (or null) and the service returning an empty collection or map. For requests, this returns true
     * if a value for the property was specified in the request builder, and false if a value was not specified.
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
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasSuperValue} method.
     * </p>
     * 
     * @return The value of the Super property for this object.
     */
    public final List<String> superValue() {
        return superValue;
    }

    /**
     * For responses, this returns true if the service returned a value for the ToString property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
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
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasToStringValue} method.
     * </p>
     * 
     * @return The value of the ToString property for this object.
     */
    public final Map<String, String> toStringValue() {
        return toStringValue;
    }

    /**
     * For responses, this returns true if the service returned a value for the Equals property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
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
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasEqualsValue} method.
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
        hashCode = 31 * hashCode + Objects.hashCode(hasBuild() ? build() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasSuperValue() ? superValue() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasToStringValue() ? toStringValue() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasEqualsValue() ? equalsValue() : null);
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
        return hasBuild() == other.hasBuild() && Objects.equals(build(), other.build())
                && hasSuperValue() == other.hasSuperValue() && Objects.equals(superValue(), other.superValue())
                && hasToStringValue() == other.hasToStringValue() && Objects.equals(toStringValue(), other.toStringValue())
                && hasEqualsValue() == other.hasEqualsValue() && Objects.equals(equalsValue(), other.equalsValue());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("ExistenceCheckNamingResponse").add("Build", hasBuild() ? build() : null)
                .add("Super", hasSuperValue() ? superValue() : null).add("ToString", hasToStringValue() ? toStringValue() : null)
                .add("Equals", hasEqualsValue() ? equalsValue() : null).build();
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

    @Override
    public final Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
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
        private List<String> build = DefaultSdkAutoConstructList.getInstance();

        private List<String> superValue = DefaultSdkAutoConstructList.getInstance();

        private Map<String, String> toStringValue = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, String> equalsValue = DefaultSdkAutoConstructMap.getInstance();

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
            if (build instanceof SdkAutoConstructList) {
                return null;
            }
            return build;
        }

        public final void setBuild(Collection<String> build) {
            this.build = ListOfStringsCopier.copy(build);
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

        public final Collection<String> getSuperValue() {
            if (superValue instanceof SdkAutoConstructList) {
                return null;
            }
            return superValue;
        }

        public final void setSuperValue(Collection<String> superValue) {
            this.superValue = ListOfStringsCopier.copy(superValue);
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

        public final Map<String, String> getToStringValue() {
            if (toStringValue instanceof SdkAutoConstructMap) {
                return null;
            }
            return toStringValue;
        }

        public final void setToStringValue(Map<String, String> toStringValue) {
            this.toStringValue = MapOfStringToStringCopier.copy(toStringValue);
        }

        @Override
        public final Builder toStringValue(Map<String, String> toStringValue) {
            this.toStringValue = MapOfStringToStringCopier.copy(toStringValue);
            return this;
        }

        public final Map<String, String> getEqualsValue() {
            if (equalsValue instanceof SdkAutoConstructMap) {
                return null;
            }
            return equalsValue;
        }

        public final void setEqualsValue(Map<String, String> equalsValue) {
            this.equalsValue = MapOfStringToStringCopier.copy(equalsValue);
        }

        @Override
        public final Builder equalsValue(Map<String, String> equalsValue) {
            this.equalsValue = MapOfStringToStringCopier.copy(equalsValue);
            return this;
        }

        @Override
        public ExistenceCheckNamingResponse build() {
            return new ExistenceCheckNamingResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        @Override
        public Map<String, SdkField<?>> sdkFieldNameToField() {
            return SDK_NAME_TO_FIELD;
        }
    }
}
