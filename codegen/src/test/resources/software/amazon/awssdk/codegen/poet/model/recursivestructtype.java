package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class RecursiveStructType implements SdkPojo, Serializable,
                                                  ToCopyableBuilder<RecursiveStructType.Builder, RecursiveStructType> {
    private static final SdkField<String> NO_RECURSE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
        .memberName("NoRecurse").getter(getter(RecursiveStructType::noRecurse)).setter(setter(Builder::noRecurse))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NoRecurse").build()).build();

    private static final SdkField<RecursiveStructType> RECURSIVE_STRUCT_FIELD = SdkField
        .<RecursiveStructType> builder(MarshallingType.SDK_POJO).memberName("RecursiveStruct")
                                                                .getter(getter(RecursiveStructType::recursiveStruct)).setter(setter(Builder::recursiveStruct))
                                                                .constructor(RecursiveStructType::builder)
                                                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RecursiveStruct").build()).build();

    private static final SdkField<List<RecursiveStructType>> RECURSIVE_LIST_FIELD = SdkField
        .<List<RecursiveStructType>> builder(MarshallingType.LIST)
        .memberName("RecursiveList")
        .getter(getter(RecursiveStructType::recursiveList))
        .setter(setter(Builder::recursiveList))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RecursiveList").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<RecursiveStructType> builder(MarshallingType.SDK_POJO)
                            .constructor(RecursiveStructType::builder)
                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                 .locationName("member").build()).build()).build()).build();

    private static final SdkField<Map<String, RecursiveStructType>> RECURSIVE_MAP_FIELD = SdkField
        .<Map<String, RecursiveStructType>> builder(MarshallingType.MAP)
        .memberName("RecursiveMap")
        .getter(getter(RecursiveStructType::recursiveMap))
        .setter(setter(Builder::recursiveMap))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RecursiveMap").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<RecursiveStructType> builder(MarshallingType.SDK_POJO)
                                .constructor(RecursiveStructType::builder)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("value").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(NO_RECURSE_FIELD,
                                                                                                   RECURSIVE_STRUCT_FIELD, RECURSIVE_LIST_FIELD, RECURSIVE_MAP_FIELD));

    private static final long serialVersionUID = 1L;

    private final String noRecurse;

    private final RecursiveStructType recursiveStruct;

    private final List<RecursiveStructType> recursiveList;

    private final Map<String, RecursiveStructType> recursiveMap;

    private RecursiveStructType(BuilderImpl builder) {
        this.noRecurse = builder.noRecurse;
        this.recursiveStruct = builder.recursiveStruct;
        this.recursiveList = builder.recursiveList;
        this.recursiveMap = builder.recursiveMap;
    }

    /**
     * Returns the value of the NoRecurse property for this object.
     *
     * @return The value of the NoRecurse property for this object.
     */
    public String noRecurse() {
        return noRecurse;
    }

    /**
     * Returns the value of the RecursiveStruct property for this object.
     *
     * @return The value of the RecursiveStruct property for this object.
     */
    public RecursiveStructType recursiveStruct() {
        return recursiveStruct;
    }

    /**
     * Returns true if the RecursiveList property was specified by the sender (it may be empty), or false if the sender
     * did not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public boolean hasRecursiveList() {
        return recursiveList != null && !(recursiveList instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the RecursiveList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasRecursiveList()} to see if a value was sent in this field.
     * </p>
     *
     * @return The value of the RecursiveList property for this object.
     */
    public List<RecursiveStructType> recursiveList() {
        return recursiveList;
    }

    /**
     * Returns true if the RecursiveMap property was specified by the sender (it may be empty), or false if the sender
     * did not specify the value (it will be empty). For responses returned by the SDK, the sender is the AWS service.
     */
    public boolean hasRecursiveMap() {
        return recursiveMap != null && !(recursiveMap instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the RecursiveMap property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * You can use {@link #hasRecursiveMap()} to see if a value was sent in this field.
     * </p>
     *
     * @return The value of the RecursiveMap property for this object.
     */
    public Map<String, RecursiveStructType> recursiveMap() {
        return recursiveMap;
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
        hashCode = 31 * hashCode + Objects.hashCode(noRecurse());
        hashCode = 31 * hashCode + Objects.hashCode(recursiveStruct());
        hashCode = 31 * hashCode + Objects.hashCode(hasRecursiveList() ? recursiveList() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasRecursiveMap() ? recursiveMap() : null);
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RecursiveStructType)) {
            return false;
        }
        RecursiveStructType other = (RecursiveStructType) obj;
        return Objects.equals(noRecurse(), other.noRecurse()) && Objects.equals(recursiveStruct(), other.recursiveStruct())
               && hasRecursiveList() == other.hasRecursiveList() && Objects.equals(recursiveList(), other.recursiveList())
               && hasRecursiveMap() == other.hasRecursiveMap() && Objects.equals(recursiveMap(), other.recursiveMap());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("RecursiveStructType").add("NoRecurse", noRecurse()).add("RecursiveStruct", recursiveStruct())
                       .add("RecursiveList", hasRecursiveList() ? recursiveList() : null)
                       .add("RecursiveMap", hasRecursiveMap() ? recursiveMap() : null).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "NoRecurse":
                return Optional.ofNullable(clazz.cast(noRecurse()));
            case "RecursiveStruct":
                return Optional.ofNullable(clazz.cast(recursiveStruct()));
            case "RecursiveList":
                return Optional.ofNullable(clazz.cast(recursiveList()));
            case "RecursiveMap":
                return Optional.ofNullable(clazz.cast(recursiveMap()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<RecursiveStructType, T> g) {
        return obj -> g.apply((RecursiveStructType) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, RecursiveStructType> {
        /**
         * Sets the value of the NoRecurse property for this object.
         *
         * @param noRecurse
         *        The new value for the NoRecurse property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder noRecurse(String noRecurse);

        /**
         * Sets the value of the RecursiveStruct property for this object.
         *
         * @param recursiveStruct
         *        The new value for the RecursiveStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveStruct(RecursiveStructType recursiveStruct);

        /**
         * Sets the value of the RecursiveStruct property for this object.
         *
         * This is a convenience that creates an instance of the {@link RecursiveStructType.Builder} avoiding the need
         * to create one manually via {@link RecursiveStructType#builder()}.
         *
         * When the {@link Consumer} completes, {@link RecursiveStructType.Builder#build()} is called immediately and
         * its result is passed to {@link #recursiveStruct(RecursiveStructType)}.
         *
         * @param recursiveStruct
         *        a consumer that will call methods on {@link RecursiveStructType.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #recursiveStruct(RecursiveStructType)
         */
        default Builder recursiveStruct(Consumer<Builder> recursiveStruct) {
            return recursiveStruct(RecursiveStructType.builder().applyMutation(recursiveStruct).build());
        }

        /**
         * Sets the value of the RecursiveList property for this object.
         *
         * @param recursiveList
         *        The new value for the RecursiveList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(Collection<RecursiveStructType> recursiveList);

        /**
         * Sets the value of the RecursiveList property for this object.
         *
         * @param recursiveList
         *        The new value for the RecursiveList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveList(RecursiveStructType... recursiveList);

        /**
         * Sets the value of the RecursiveList property for this object.
         *
         * This is a convenience that creates an instance of the {@link List<RecursiveStructType>.Builder} avoiding the
         * need to create one manually via {@link List<RecursiveStructType>#builder()}.
         *
         * When the {@link Consumer} completes, {@link List<RecursiveStructType>.Builder#build()} is called immediately
         * and its result is passed to {@link #recursiveList(List<RecursiveStructType>)}.
         *
         * @param recursiveList
         *        a consumer that will call methods on {@link List<RecursiveStructType>.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #recursiveList(List<RecursiveStructType>)
         */
        Builder recursiveList(Consumer<Builder>... recursiveList);

        /**
         * Sets the value of the RecursiveMap property for this object.
         *
         * @param recursiveMap
         *        The new value for the RecursiveMap property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap);
    }

    static final class BuilderImpl implements Builder {
        private String noRecurse;

        private RecursiveStructType recursiveStruct;

        private List<RecursiveStructType> recursiveList = DefaultSdkAutoConstructList.getInstance();

        private Map<String, RecursiveStructType> recursiveMap = DefaultSdkAutoConstructMap.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(RecursiveStructType model) {
            noRecurse(model.noRecurse);
            recursiveStruct(model.recursiveStruct);
            recursiveList(model.recursiveList);
            recursiveMap(model.recursiveMap);
        }

        public final String getNoRecurse() {
            return noRecurse;
        }

        @Override
        public final Builder noRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
            return this;
        }

        public final void setNoRecurse(String noRecurse) {
            this.noRecurse = noRecurse;
        }

        public final Builder getRecursiveStruct() {
            return recursiveStruct != null ? recursiveStruct.toBuilder() : null;
        }

        @Override
        public final Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
            return this;
        }

        public final void setRecursiveStruct(BuilderImpl recursiveStruct) {
            this.recursiveStruct = recursiveStruct != null ? recursiveStruct.build() : null;
        }

        public final Collection<Builder> getRecursiveList() {
            if (recursiveList instanceof SdkAutoConstructList) {
                return null;
            }
            return recursiveList != null ? recursiveList.stream().map(RecursiveStructType::toBuilder)
                                                        .collect(Collectors.toList()) : null;
        }

        @Override
        public final Builder recursiveList(Collection<RecursiveStructType> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copy(recursiveList);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder recursiveList(RecursiveStructType... recursiveList) {
            recursiveList(Arrays.asList(recursiveList));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder recursiveList(Consumer<Builder>... recursiveList) {
            recursiveList(Stream.of(recursiveList).map(c -> RecursiveStructType.builder().applyMutation(c).build())
                                .collect(Collectors.toList()));
            return this;
        }

        public final void setRecursiveList(Collection<BuilderImpl> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copyFromBuilder(recursiveList);
        }

        public final Map<String, Builder> getRecursiveMap() {
            if (recursiveMap instanceof SdkAutoConstructMap) {
                return null;
            }
            return recursiveMap != null ? CollectionUtils.mapValues(recursiveMap, RecursiveStructType::toBuilder) : null;
        }

        @Override
        public final Builder recursiveMap(Map<String, RecursiveStructType> recursiveMap) {
            this.recursiveMap = RecursiveMapTypeCopier.copy(recursiveMap);
            return this;
        }

        public final void setRecursiveMap(Map<String, BuilderImpl> recursiveMap) {
            this.recursiveMap = RecursiveMapTypeCopier.copyFromBuilder(recursiveMap);
        }

        @Override
        public RecursiveStructType build() {
            return new RecursiveStructType(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
