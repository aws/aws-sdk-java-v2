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
     * Returns true if the ListOfListOfStrings property was specified by the sender (it may be empty), or false if the
     * sender did not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS
     * service.
     */
    public boolean hasListOfListOfStrings() {
        return listOfListOfStrings != null && !(listOfListOfStrings instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasListOfListOfStrings()} to see if a value was sent in this field.
     * </p>
     *
     * @return The value of the ListOfListOfStrings property for this object.
     */
    public List<List<String>> listOfListOfStrings() {
        return listOfListOfStrings;
    }

    /**
     * Returns true if the ListOfListOfListOfStrings property was specified by the sender (it may be empty), or false if
     * the sender did not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS
     * service.
     */
    public boolean hasListOfListOfListOfStrings() {
        return listOfListOfListOfStrings != null && !(listOfListOfListOfStrings instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasListOfListOfListOfStrings()} to see if a value was sent in this field.
     * </p>
     *
     * @return The value of the ListOfListOfListOfStrings property for this object.
     */
    public List<List<List<String>>> listOfListOfListOfStrings() {
        return listOfListOfListOfStrings;
    }

    /**
     * Returns true if the MapOfStringToListOfListOfStrings property was specified by the sender (it may be empty), or
     * false if the sender did not specify the value (it will be empty). For responses returned by the SDK, the sender
     * is the AWS service.
     */
    public boolean hasMapOfStringToListOfListOfStrings() {
        return mapOfStringToListOfListOfStrings != null && !(mapOfStringToListOfListOfStrings instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToListOfListOfStrings property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasMapOfStringToListOfListOfStrings()} to see if a value was sent in this field.
     * </p>
     *
     * @return The value of the MapOfStringToListOfListOfStrings property for this object.
     */
    public Map<String, List<List<String>>> mapOfStringToListOfListOfStrings() {
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
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + super.hashCode();
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfListOfStrings() ? listOfListOfStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfListOfListOfStrings() ? listOfListOfListOfStrings() : null);
        hashCode = 31 * hashCode
                   + Objects.hashCode(hasMapOfStringToListOfListOfStrings() ? mapOfStringToListOfListOfStrings() : null);
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
    public String toString() {
        return ToString
            .builder("NestedContainersResponse")
            .add("ListOfListOfStrings", hasListOfListOfStrings() ? listOfListOfStrings() : null)
            .add("ListOfListOfListOfStrings", hasListOfListOfListOfStrings() ? listOfListOfListOfStrings() : null)
            .add("MapOfStringToListOfListOfStrings",
                 hasMapOfStringToListOfListOfStrings() ? mapOfStringToListOfListOfStrings() : null).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
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
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<NestedContainersResponse, T> g) {
        return obj -> g.apply((NestedContainersResponse) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

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

        public final void setListOfListOfStrings(Collection<? extends Collection<String>> listOfListOfStrings) {
            this.listOfListOfStrings = ListOfListOfStringsCopier.copy(listOfListOfStrings);
        }

        public final Collection<? extends Collection<? extends Collection<String>>> getListOfListOfListOfStrings() {
            if (listOfListOfListOfStrings instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfListOfListOfStrings;
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

        public final void setListOfListOfListOfStrings(
            Collection<? extends Collection<? extends Collection<String>>> listOfListOfListOfStrings) {
            this.listOfListOfListOfStrings = ListOfListOfListOfStringsCopier.copy(listOfListOfListOfStrings);
        }

        public final Map<String, ? extends Collection<? extends Collection<String>>> getMapOfStringToListOfListOfStrings() {
            if (mapOfStringToListOfListOfStrings instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToListOfListOfStrings;
        }

        @Override
        public final Builder mapOfStringToListOfListOfStrings(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings) {
            this.mapOfStringToListOfListOfStrings = MapOfStringToListOfListOfStringsCopier.copy(mapOfStringToListOfListOfStrings);
            return this;
        }

        public final void setMapOfStringToListOfListOfStrings(
            Map<String, ? extends Collection<? extends Collection<String>>> mapOfStringToListOfListOfStrings) {
            this.mapOfStringToListOfListOfStrings = MapOfStringToListOfListOfStringsCopier.copy(mapOfStringToListOfListOfStrings);
        }

        @Override
        public NestedContainersResponse build() {
            return new NestedContainersResponse(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
