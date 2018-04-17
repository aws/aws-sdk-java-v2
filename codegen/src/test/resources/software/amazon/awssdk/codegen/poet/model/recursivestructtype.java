package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.ProtocolMarshaller;
import software.amazon.awssdk.core.protocol.StructuredPojo;
import software.amazon.awssdk.services.jsonprotocoltests.transform.RecursiveStructTypeMarshaller;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class RecursiveStructType implements StructuredPojo, ToCopyableBuilder<RecursiveStructType.Builder, RecursiveStructType> {
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
     * Returns the value of the RecursiveList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the RecursiveList property for this object.
     */
    public List<RecursiveStructType> recursiveList() {
        return recursiveList;
    }

    /**
     * Returns the value of the RecursiveMap property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
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
        hashCode = 31 * hashCode + Objects.hashCode(recursiveList());
        hashCode = 31 * hashCode + Objects.hashCode(recursiveMap());
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
        if (!(obj instanceof RecursiveStructType)) {
            return false;
        }
        RecursiveStructType other = (RecursiveStructType) obj;
        return Objects.equals(noRecurse(), other.noRecurse()) && Objects.equals(recursiveStruct(), other.recursiveStruct())
               && Objects.equals(recursiveList(), other.recursiveList()) && Objects.equals(recursiveMap(), other.recursiveMap());
    }

    @Override
    public String toString() {
        return ToString.builder("RecursiveStructType").add("NoRecurse", noRecurse()).add("RecursiveStruct", recursiveStruct())
                       .add("RecursiveList", recursiveList()).add("RecursiveMap", recursiveMap()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "NoRecurse":
                return Optional.of(clazz.cast(noRecurse()));
            case "RecursiveStruct":
                return Optional.of(clazz.cast(recursiveStruct()));
            case "RecursiveList":
                return Optional.of(clazz.cast(recursiveList()));
            case "RecursiveMap":
                return Optional.of(clazz.cast(recursiveMap()));
            default:
                return Optional.empty();
        }
    }

    @SdkInternalApi
    @Override
    public void marshall(ProtocolMarshaller protocolMarshaller) {
        RecursiveStructTypeMarshaller.getInstance().marshall(this, protocolMarshaller);
    }

    public interface Builder extends CopyableBuilder<Builder, RecursiveStructType> {
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
            return recursiveStruct(RecursiveStructType.builder().apply(recursiveStruct).build());
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

        private List<RecursiveStructType> recursiveList;

        private Map<String, RecursiveStructType> recursiveMap;

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
            recursiveList(Stream.of(recursiveList).map(c -> RecursiveStructType.builder().apply(c).build())
                                .collect(Collectors.toList()));
            return this;
        }

        public final void setRecursiveList(Collection<BuilderImpl> recursiveList) {
            this.recursiveList = RecursiveListTypeCopier.copyFromBuilder(recursiveList);
        }

        public final Map<String, Builder> getRecursiveMap() {
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
    }
}
