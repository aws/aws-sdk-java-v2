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
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
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
public final class NestedContainersResponse extends JsonProtocolTestsResponse implements
                                                                              ToCopyableBuilder<NestedContainersResponse.Builder, NestedContainersResponse> {
    private static final SdkField<List<List<String>>> LIST_OF_LIST_OF_STRINGS_FIELD = SdkField
        .<List<List<String>>> builder(MarshallingType.LIST)
        .memberName("ListOfListOfStrings")
        .getter(getter(NestedContainersResponse::listOfListOfStrings))
        .setter(setter(Builder::listOfListOfStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfListOfStrings").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<List<String>> builder(MarshallingType.LIST)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build(),
                                        ListTrait
                                            .builder()
                                            .memberLocationName(null)
                                            .memberFieldInfo(
                                                SdkField.<String> builder(MarshallingType.STRING)
                                                        .traits(LocationTrait.builder()
                                                                             .location(MarshallLocation.PAYLOAD)
                                                                             .locationName("member").build()).build())
                                            .build()).build()).build()).build();

    private static final SdkField<List<List<List<String>>>> LIST_OF_LIST_OF_LIST_OF_STRINGS_FIELD = SdkField
        .<List<List<List<String>>>> builder(MarshallingType.LIST)
        .memberName("ListOfListOfListOfStrings")
        .getter(getter(NestedContainersResponse::listOfListOfListOfStrings))
        .setter(setter(Builder::listOfListOfListOfStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfListOfListOfStrings").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<List<List<String>>> builder(MarshallingType.LIST)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build(),
                                        ListTrait
                                            .builder()
                                            .memberLocationName(null)
                                            .memberFieldInfo(
                                                SdkField.<List<String>> builder(MarshallingType.LIST)
                                                        .traits(LocationTrait.builder()
                                                                             .location(MarshallLocation.PAYLOAD)
                                                                             .locationName("member").build(),
                                                                ListTrait
                                                                    .builder()
                                                                    .memberLocationName(null)
                                                                    .memberFieldInfo(
                                                                        SdkField.<String> builder(
                                                                                    MarshallingType.STRING)
                                                                                .traits(LocationTrait
                                                                                            .builder()
                                                                                            .location(
                                                                                                MarshallLocation.PAYLOAD)
                                                                                            .locationName(
                                                                                                "member")
                                                                                            .build())
                                                                                .build()).build())
                                                        .build()).build()).build()).build()).build();

    private static final SdkField<Map<String, List<List<String>>>> MAP_OF_STRING_TO_LIST_OF_LIST_OF_STRINGS_FIELD = SdkField
        .<Map<String, List<List<String>>>> builder(MarshallingType.MAP)
        .memberName("MapOfStringToListOfListOfStrings")
        .getter(getter(NestedContainersResponse::mapOfStringToListOfListOfStrings))
        .setter(setter(Builder::mapOfStringToListOfListOfStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfStringToListOfListOfStrings")
                             .build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<List<List<String>>> builder(MarshallingType.LIST)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build(),
                                            ListTrait
                                                .builder()
                                                .memberLocationName(null)
                                                .memberFieldInfo(
                                                    SdkField.<List<String>> builder(MarshallingType.LIST)
                                                            .traits(LocationTrait.builder()
                                                                                 .location(MarshallLocation.PAYLOAD)
                                                                                 .locationName("member").build(),
                                                                    ListTrait
                                                                        .builder()
                                                                        .memberLocationName(null)
                                                                        .memberFieldInfo(
                                                                            SdkField.<String> builder(
                                                                                        MarshallingType.STRING)
                                                                                    .traits(LocationTrait
                                                                                                .builder()
                                                                                                .location(
                                                                                                    MarshallLocation.PAYLOAD)
                                                                                                .locationName(
                                                                                                    "member")
                                                                                                .build())
                                                                                    .build()).build())
                                                            .build()).build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(LIST_OF_LIST_OF_STRINGS_FIELD,
                                                                                                   LIST_OF_LIST_OF_LIST_OF_STRINGS_FIELD, MAP_OF_STRING_TO_LIST_OF_LIST_OF_STRINGS_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final List<List<String>> listOfListOfStrings;

    private final List<List<List<String>>> listOfListOfListOfStrings;

    private final Map<String, List<List<String>>> mapOfStringToListOfListOfStrings;

    private NestedContainersResponse(BuilderImpl builder) {
        super(builder);
        this.listOfListOfStrings = builder.listOfListOfStrings;
        this.listOfListOfListOfStrings = builder.listOfListOfListOfStrings;
        this.mapOfStringToListOfListOfStrings = builder.mapOfStringToListOfListOfStrings;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfListOfStrings property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasListOfListOfStrings() {
        return listOfListOfStrings != null && !(listOfListOfStrings instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfListOfStrings} method.
     * </p>
     *
     * @return The value of the ListOfListOfStrings property for this object.
     */
    public final List<List<String>> listOfListOfStrings() {
        return listOfListOfStrings;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfListOfListOfStrings property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasListOfListOfListOfStrings() {
        return listOfListOfListOfStrings != null && !(listOfListOfListOfStrings instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfListOfListOfStrings} method.
     * </p>
     *
     * @return The value of the ListOfListOfListOfStrings property for this object.
     */
    public final List<List<List<String>>> listOfListOfListOfStrings() {
        return listOfListOfListOfStrings;
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfStringToListOfListOfStrings
     * property. This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()}
     * method on the property). This is useful because the SDK will never return a null collection or map, but you may
     * need to differentiate between the service returning nothing (or null) and the service returning an empty
     * collection or map. For requests, this returns true if a value for the property was specified in the request
     * builder, and false if a value was not specified.
     */
    public final boolean hasMapOfStringToListOfListOfStrings() {
        return mapOfStringToListOfListOfStrings != null && !(mapOfStringToListOfListOfStrings instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToListOfListOfStrings}
     * method.
     * </p>
     *
     * @return The value of the MapOfStringToListOfListOfStrings property for this object.
     */
    public final Map<String, List<List<String>>> mapOfStringToListOfListOfStrings() {
        return mapOfStringToListOfListOfStrings;
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
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfListOfStrings() ? listOfListOfStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfListOfListOfStrings() ? listOfListOfListOfStrings() : null);
        hashCode = 31 * hashCode
                   + Objects.hashCode(hasMapOfStringToListOfListOfStrings() ? mapOfStringToListOfListOfStrings() : null);
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
        if (!(obj instanceof NestedContainersResponse)) {
            return false;
        }
        NestedContainersResponse other = (NestedContainersResponse) obj;
        return hasListOfListOfStrings() == other.hasListOfListOfStrings()
               && Objects.equals(listOfListOfStrings(), other.listOfListOfStrings())
               && hasListOfListOfListOfStrings() == other.hasListOfListOfListOfStrings()
               && Objects.equals(listOfListOfListOfStrings(), other.listOfListOfListOfStrings())
               && hasMapOfStringToListOfListOfStrings() == other.hasMapOfStringToListOfListOfStrings()
               && Objects.equals(mapOfStringToListOfListOfStrings(), other.mapOfStringToListOfListOfStrings());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString
            .builder("NestedContainersResponse")
            .add("ListOfListOfStrings", hasListOfListOfStrings() ? listOfListOfStrings() : null)
            .add("ListOfListOfListOfStrings", hasListOfListOfListOfStrings() ? listOfListOfListOfStrings() : null)
            .add("MapOfStringToListOfListOfStrings",
                 hasMapOfStringToListOfListOfStrings() ? mapOfStringToListOfListOfStrings() : null).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "ListOfListOfStrings":
                return Optional.ofNullable(clazz.cast(listOfListOfStrings()));
            case "ListOfListOfListOfStrings":
                return Optional.ofNullable(clazz.cast(listOfListOfListOfStrings()));
            case "MapOfStringToListOfListOfStrings":
                return Optional.ofNullable(clazz.cast(mapOfStringToListOfListOfStrings()));
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

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("ListOfListOfStrings", LIST_OF_LIST_OF_STRINGS_FIELD);
        map.put("ListOfListOfListOfStrings", LIST_OF_LIST_OF_LIST_OF_STRINGS_FIELD);
        map.put("MapOfStringToListOfListOfStrings", MAP_OF_STRING_TO_LIST_OF_LIST_OF_STRINGS_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<NestedContainersResponse, T> g) {
        return obj -> g.apply((NestedContainersResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends JsonProtocolTestsResponse.Builder, SdkPojo,
                                     CopyableBuilder<Builder, NestedContainersResponse> {
        /**
         * Sets the value of the ListOfListOfStrings property for this object.
         *
         * @param listOfListOfStrings
         *        The new value for the ListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings);

        /**
         * Sets the value of the ListOfListOfStrings property for this object.
         *
         * @param listOfListOfStrings
         *        The new value for the ListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfStrings(Collection<String>... listOfListOfStrings);

        /**
         * Sets the value of the ListOfListOfListOfStrings property for this object.
         *
         * @param listOfListOfListOfStrings
         *        The new value for the ListOfListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListOfStrings(Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings);

        /**
         * Sets the value of the ListOfListOfListOfStrings property for this object.
         *
         * @param listOfListOfListOfStrings
         *        The new value for the ListOfListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfListOfListOfStrings(Collection<? extends Collection<String>>... listOfListOfListOfStrings);

        /**
         * Sets the value of the MapOfStringToListOfListOfStrings property for this object.
         *
         * @param mapOfStringToListOfListOfStrings
         *        The new value for the MapOfStringToListOfListOfStrings property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToListOfListOfStrings(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings);
    }

    static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
        private List<List<String>> listOfListOfStrings = DefaultSdkAutoConstructList.getInstance();

        private List<List<List<String>>> listOfListOfListOfStrings = DefaultSdkAutoConstructList.getInstance();

        private Map<String, List<List<String>>> mapOfStringToListOfListOfStrings = DefaultSdkAutoConstructMap.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(NestedContainersResponse model) {
            super(model);
            listOfListOfStrings(model.listOfListOfStrings);
            listOfListOfListOfStrings(model.listOfListOfListOfStrings);
            mapOfStringToListOfListOfStrings(model.mapOfStringToListOfListOfStrings);
        }

        public final Collection<? extends Collection<String>> getListOfListOfStrings() {
            if (listOfListOfStrings instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfListOfStrings;
        }

        public final void setListOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings) {
            this.listOfListOfStrings = ListOfListOfStringsCopier.copy(listOfListOfStrings);
        }

        @Override
        public final Builder listOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings) {
            this.listOfListOfStrings = ListOfListOfStringsCopier.copy(listOfListOfStrings);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfListOfStrings(Collection<String>... listOfListOfStrings) {
            listOfListOfStrings(Arrays.asList(listOfListOfStrings));
            return this;
        }

        public final Collection<? extends Collection<? extends Collection<String>>> getListOfListOfListOfStrings() {
            if (listOfListOfListOfStrings instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfListOfListOfStrings;
        }

        public final void setListOfListOfListOfStrings(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings) {
            this.listOfListOfListOfStrings = ListOfListOfListOfStringsCopier.copy(listOfListOfListOfStrings);
        }

        @Override
        public final Builder listOfListOfListOfStrings(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings) {
            this.listOfListOfListOfStrings = ListOfListOfListOfStringsCopier.copy(listOfListOfListOfStrings);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfListOfListOfStrings(Collection<? extends Collection<String>>... listOfListOfListOfStrings) {
            listOfListOfListOfStrings(Arrays.asList(listOfListOfListOfStrings));
            return this;
        }

        public final Map<String, ? extends Collection<? extends Collection<String>>> getMapOfStringToListOfListOfStrings() {
            if (mapOfStringToListOfListOfStrings instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToListOfListOfStrings;
        }

        public final void setMapOfStringToListOfListOfStrings(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings) {
            this.mapOfStringToListOfListOfStrings = MapOfStringToListOfListOfStringsCopier.copy(mapOfStringToListOfListOfStrings);
        }

        @Override
        public final Builder mapOfStringToListOfListOfStrings(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings) {
            this.mapOfStringToListOfListOfStrings = MapOfStringToListOfListOfStringsCopier.copy(mapOfStringToListOfListOfStrings);
            return this;
        }

        @Override
        public NestedContainersResponse build() {
            return new NestedContainersResponse(this);
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
