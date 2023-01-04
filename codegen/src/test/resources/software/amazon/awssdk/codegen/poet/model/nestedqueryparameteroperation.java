package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
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
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class NestedQueryParameterOperation implements SdkPojo, Serializable,
                                                            ToCopyableBuilder<NestedQueryParameterOperation.Builder, NestedQueryParameterOperation> {
    private static final SdkField<String> QUERY_PARAM_ONE_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("QueryParamOne")
        .getter(getter(NestedQueryParameterOperation::queryParamOne))
        .setter(setter(Builder::queryParamOne))
        .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("QueryParamOne").build(),
                RequiredTrait.create()).build();

    private static final SdkField<String> QUERY_PARAM_TWO_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("QueryParamTwo").getter(getter(NestedQueryParameterOperation::queryParamTwo))
        .setter(setter(Builder::queryParamTwo))
        .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("QueryParamTwo").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(QUERY_PARAM_ONE_FIELD,
                                                                                                   QUERY_PARAM_TWO_FIELD));

    private static final long serialVersionUID = 1L;

    private final String queryParamOne;

    private final String queryParamTwo;

    private NestedQueryParameterOperation(BuilderImpl builder) {
        this.queryParamOne = builder.queryParamOne;
        this.queryParamTwo = builder.queryParamTwo;
    }

    /**
     * Returns the value of the QueryParamOne property for this object.
     *
     * @return The value of the QueryParamOne property for this object.
     */
    public final String queryParamOne() {
        return queryParamOne;
    }

    /**
     * Returns the value of the QueryParamTwo property for this object.
     *
     * @return The value of the QueryParamTwo property for this object.
     */
    public final String queryParamTwo() {
        return queryParamTwo;
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
        hashCode = 31 * hashCode + Objects.hashCode(queryParamOne());
        hashCode = 31 * hashCode + Objects.hashCode(queryParamTwo());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        return equalsBySdkFields(obj);
    }

    @Override
    public final boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NestedQueryParameterOperation)) {
            return false;
        }
        NestedQueryParameterOperation other = (NestedQueryParameterOperation) obj;
        return Objects.equals(queryParamOne(), other.queryParamOne()) && Objects.equals(queryParamTwo(), other.queryParamTwo());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("NestedQueryParameterOperation").add("QueryParamOne", queryParamOne())
                       .add("QueryParamTwo", queryParamTwo()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "QueryParamOne":
                return Optional.ofNullable(clazz.cast(queryParamOne()));
            case "QueryParamTwo":
                return Optional.ofNullable(clazz.cast(queryParamTwo()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<NestedQueryParameterOperation, T> g) {
        return obj -> g.apply((NestedQueryParameterOperation) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, NestedQueryParameterOperation> {
        /**
         * Sets the value of the QueryParamOne property for this object.
         *
         * @param queryParamOne
         *        The new value for the QueryParamOne property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder queryParamOne(String queryParamOne);

        /**
         * Sets the value of the QueryParamTwo property for this object.
         *
         * @param queryParamTwo
         *        The new value for the QueryParamTwo property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder queryParamTwo(String queryParamTwo);
    }

    static final class BuilderImpl implements Builder {
        private String queryParamOne;

        private String queryParamTwo;

        private BuilderImpl() {
        }

        private BuilderImpl(NestedQueryParameterOperation model) {
            queryParamOne(model.queryParamOne);
            queryParamTwo(model.queryParamTwo);
        }

        public final String getQueryParamOne() {
            return queryParamOne;
        }

        public final void setQueryParamOne(String queryParamOne) {
            this.queryParamOne = queryParamOne;
        }

        @Override
        public final Builder queryParamOne(String queryParamOne) {
            this.queryParamOne = queryParamOne;
            return this;
        }

        public final String getQueryParamTwo() {
            return queryParamTwo;
        }

        public final void setQueryParamTwo(String queryParamTwo) {
            this.queryParamTwo = queryParamTwo;
        }

        @Override
        public final Builder queryParamTwo(String queryParamTwo) {
            this.queryParamTwo = queryParamTwo;
            return this;
        }

        @Override
        public NestedQueryParameterOperation build() {
            return new NestedQueryParameterOperation(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
