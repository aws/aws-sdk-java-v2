package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
import java.util.Collection;
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
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class QueryParameterOperationRequest extends JsonProtocolTestsRequest implements
                                                                                   ToCopyableBuilder<QueryParameterOperationRequest.Builder, QueryParameterOperationRequest> {
    private static final SdkField<String> PATH_PARAM_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("PathParam")
        .getter(getter(QueryParameterOperationRequest::pathParam))
        .setter(setter(Builder::pathParam))
        .traits(LocationTrait.builder().location(MarshallLocation.PATH).locationName("PathParam").build(),
                RequiredTrait.create()).build();

    private static final SdkField<String> QUERY_PARAM_ONE_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("QueryParamOne")
        .getter(getter(QueryParameterOperationRequest::queryParamOne))
        .setter(setter(Builder::queryParamOne))
        .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("QueryParamOne").build(),
                RequiredTrait.create()).build();

    private static final SdkField<String> QUERY_PARAM_TWO_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("QueryParamTwo").getter(getter(QueryParameterOperationRequest::queryParamTwo))
        .setter(setter(Builder::queryParamTwo))
        .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("QueryParamTwo").build()).build();

    private static final SdkField<String> STRING_HEADER_MEMBER_FIELD = SdkField
        .<String> builder(MarshallingType.STRING)
        .memberName("StringHeaderMember")
        .getter(getter(QueryParameterOperationRequest::stringHeaderMember))
        .setter(setter(Builder::stringHeaderMember))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("x-amz-header-string").build(),
                RequiredTrait.create()).build();

    private static final SdkField<NestedQueryParameterOperation> NESTED_QUERY_PARAMETER_OPERATION_FIELD = SdkField
        .<NestedQueryParameterOperation> builder(MarshallingType.SDK_POJO)
        .memberName("NestedQueryParameterOperation")
        .getter(getter(QueryParameterOperationRequest::nestedQueryParameterOperation))
        .setter(setter(Builder::nestedQueryParameterOperation))
        .constructor(NestedQueryParameterOperation::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("NestedQueryParameterOperation")
                             .build(), PayloadTrait.create()).build();

    private static final SdkField<List<Integer>> REQUIRED_LIST_QUERY_PARAMS_FIELD = SdkField
        .<List<Integer>> builder(MarshallingType.LIST)
        .memberName("RequiredListQueryParams")
        .getter(getter(QueryParameterOperationRequest::requiredListQueryParams))
        .setter(setter(Builder::requiredListQueryParams))
        .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("RequiredListQueryParams")
                             .build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<Integer> builder(MarshallingType.INTEGER)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build()).build()).build(), RequiredTrait.create())
        .build();

    private static final SdkField<List<Integer>> OPTIONAL_LIST_QUERY_PARAMS_FIELD = SdkField
        .<List<Integer>> builder(MarshallingType.LIST)
        .memberName("OptionalListQueryParams")
        .getter(getter(QueryParameterOperationRequest::optionalListQueryParams))
        .setter(setter(Builder::optionalListQueryParams))
        .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("OptionalListQueryParams")
                             .build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<Integer> builder(MarshallingType.INTEGER)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build()).build()).build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(PATH_PARAM_FIELD,
                                                                                                   QUERY_PARAM_ONE_FIELD, QUERY_PARAM_TWO_FIELD, STRING_HEADER_MEMBER_FIELD, NESTED_QUERY_PARAMETER_OPERATION_FIELD,
                                                                                                   REQUIRED_LIST_QUERY_PARAMS_FIELD, OPTIONAL_LIST_QUERY_PARAMS_FIELD));

    private final String pathParam;

    private final String queryParamOne;

    private final String queryParamTwo;

    private final String stringHeaderMember;

    private final NestedQueryParameterOperation nestedQueryParameterOperation;

    private final List<Integer> requiredListQueryParams;

    private final List<Integer> optionalListQueryParams;

    private QueryParameterOperationRequest(BuilderImpl builder) {
        super(builder);
        this.pathParam = builder.pathParam;
        this.queryParamOne = builder.queryParamOne;
        this.queryParamTwo = builder.queryParamTwo;
        this.stringHeaderMember = builder.stringHeaderMember;
        this.nestedQueryParameterOperation = builder.nestedQueryParameterOperation;
        this.requiredListQueryParams = builder.requiredListQueryParams;
        this.optionalListQueryParams = builder.optionalListQueryParams;
    }

    /**
     * Returns the value of the PathParam property for this object.
     *
     * @return The value of the PathParam property for this object.
     */
    public final String pathParam() {
        return pathParam;
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

    /**
     * Returns the value of the StringHeaderMember property for this object.
     *
     * @return The value of the StringHeaderMember property for this object.
     */
    public final String stringHeaderMember() {
        return stringHeaderMember;
    }

    /**
     * Returns the value of the NestedQueryParameterOperation property for this object.
     *
     * @return The value of the NestedQueryParameterOperation property for this object.
     */
    public final NestedQueryParameterOperation nestedQueryParameterOperation() {
        return nestedQueryParameterOperation;
    }

    /**
     * For responses, this returns true if the service returned a value for the RequiredListQueryParams property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasRequiredListQueryParams() {
        return requiredListQueryParams != null && !(requiredListQueryParams instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the RequiredListQueryParams property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasRequiredListQueryParams} method.
     * </p>
     *
     * @return The value of the RequiredListQueryParams property for this object.
     */
    public final List<Integer> requiredListQueryParams() {
        return requiredListQueryParams;
    }

    /**
     * For responses, this returns true if the service returned a value for the OptionalListQueryParams property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasOptionalListQueryParams() {
        return optionalListQueryParams != null && !(optionalListQueryParams instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the OptionalListQueryParams property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasOptionalListQueryParams} method.
     * </p>
     *
     * @return The value of the OptionalListQueryParams property for this object.
     */
    public final List<Integer> optionalListQueryParams() {
        return optionalListQueryParams;
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
        hashCode = 31 * hashCode + Objects.hashCode(pathParam());
        hashCode = 31 * hashCode + Objects.hashCode(queryParamOne());
        hashCode = 31 * hashCode + Objects.hashCode(queryParamTwo());
        hashCode = 31 * hashCode + Objects.hashCode(stringHeaderMember());
        hashCode = 31 * hashCode + Objects.hashCode(nestedQueryParameterOperation());
        hashCode = 31 * hashCode + Objects.hashCode(hasRequiredListQueryParams() ? requiredListQueryParams() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasOptionalListQueryParams() ? optionalListQueryParams() : null);
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
        if (!(obj instanceof QueryParameterOperationRequest)) {
            return false;
        }
        QueryParameterOperationRequest other = (QueryParameterOperationRequest) obj;
        return Objects.equals(pathParam(), other.pathParam()) && Objects.equals(queryParamOne(), other.queryParamOne())
               && Objects.equals(queryParamTwo(), other.queryParamTwo())
               && Objects.equals(stringHeaderMember(), other.stringHeaderMember())
               && Objects.equals(nestedQueryParameterOperation(), other.nestedQueryParameterOperation())
               && hasRequiredListQueryParams() == other.hasRequiredListQueryParams()
               && Objects.equals(requiredListQueryParams(), other.requiredListQueryParams())
               && hasOptionalListQueryParams() == other.hasOptionalListQueryParams()
               && Objects.equals(optionalListQueryParams(), other.optionalListQueryParams());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("QueryParameterOperationRequest").add("PathParam", pathParam())
                       .add("QueryParamOne", queryParamOne()).add("QueryParamTwo", queryParamTwo())
                       .add("StringHeaderMember", stringHeaderMember())
                       .add("NestedQueryParameterOperation", nestedQueryParameterOperation())
                       .add("RequiredListQueryParams", hasRequiredListQueryParams() ? requiredListQueryParams() : null)
                       .add("OptionalListQueryParams", hasOptionalListQueryParams() ? optionalListQueryParams() : null).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "PathParam":
                return Optional.ofNullable(clazz.cast(pathParam()));
            case "QueryParamOne":
                return Optional.ofNullable(clazz.cast(queryParamOne()));
            case "QueryParamTwo":
                return Optional.ofNullable(clazz.cast(queryParamTwo()));
            case "StringHeaderMember":
                return Optional.ofNullable(clazz.cast(stringHeaderMember()));
            case "NestedQueryParameterOperation":
                return Optional.ofNullable(clazz.cast(nestedQueryParameterOperation()));
            case "RequiredListQueryParams":
                return Optional.ofNullable(clazz.cast(requiredListQueryParams()));
            case "OptionalListQueryParams":
                return Optional.ofNullable(clazz.cast(optionalListQueryParams()));
            default:
                return Optional.empty();
        }
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<QueryParameterOperationRequest, T> g) {
        return obj -> g.apply((QueryParameterOperationRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends JsonProtocolTestsRequest.Builder, SdkPojo,
                                     CopyableBuilder<Builder, QueryParameterOperationRequest> {
        /**
         * Sets the value of the PathParam property for this object.
         *
         * @param pathParam
         *        The new value for the PathParam property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder pathParam(String pathParam);

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

        /**
         * Sets the value of the StringHeaderMember property for this object.
         *
         * @param stringHeaderMember
         *        The new value for the StringHeaderMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringHeaderMember(String stringHeaderMember);

        /**
         * Sets the value of the NestedQueryParameterOperation property for this object.
         *
         * @param nestedQueryParameterOperation
         *        The new value for the NestedQueryParameterOperation property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder nestedQueryParameterOperation(NestedQueryParameterOperation nestedQueryParameterOperation);

        /**
         * Sets the value of the NestedQueryParameterOperation property for this object.
         *
         * This is a convenience method that creates an instance of the {@link NestedQueryParameterOperation.Builder}
         * avoiding the need to create one manually via {@link NestedQueryParameterOperation#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link NestedQueryParameterOperation.Builder#build()} is called
         * immediately and its result is passed to {@link #nestedQueryParameterOperation(NestedQueryParameterOperation)}.
         *
         * @param nestedQueryParameterOperation
         *        a consumer that will call methods on {@link NestedQueryParameterOperation.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #nestedQueryParameterOperation(NestedQueryParameterOperation)
         */
        default Builder nestedQueryParameterOperation(
            Consumer<NestedQueryParameterOperation.Builder> nestedQueryParameterOperation) {
            return nestedQueryParameterOperation(NestedQueryParameterOperation.builder()
                                                                              .applyMutation(nestedQueryParameterOperation).build());
        }

        /**
         * Sets the value of the RequiredListQueryParams property for this object.
         *
         * @param requiredListQueryParams
         *        The new value for the RequiredListQueryParams property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder requiredListQueryParams(Collection<Integer> requiredListQueryParams);

        /**
         * Sets the value of the RequiredListQueryParams property for this object.
         *
         * @param requiredListQueryParams
         *        The new value for the RequiredListQueryParams property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder requiredListQueryParams(Integer... requiredListQueryParams);

        /**
         * Sets the value of the OptionalListQueryParams property for this object.
         *
         * @param optionalListQueryParams
         *        The new value for the OptionalListQueryParams property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder optionalListQueryParams(Collection<Integer> optionalListQueryParams);

        /**
         * Sets the value of the OptionalListQueryParams property for this object.
         *
         * @param optionalListQueryParams
         *        The new value for the OptionalListQueryParams property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder optionalListQueryParams(Integer... optionalListQueryParams);

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private String pathParam;

        private String queryParamOne;

        private String queryParamTwo;

        private String stringHeaderMember;

        private NestedQueryParameterOperation nestedQueryParameterOperation;

        private List<Integer> requiredListQueryParams = DefaultSdkAutoConstructList.getInstance();

        private List<Integer> optionalListQueryParams = DefaultSdkAutoConstructList.getInstance();

        private BuilderImpl() {
        }

        private BuilderImpl(QueryParameterOperationRequest model) {
            super(model);
            pathParam(model.pathParam);
            queryParamOne(model.queryParamOne);
            queryParamTwo(model.queryParamTwo);
            stringHeaderMember(model.stringHeaderMember);
            nestedQueryParameterOperation(model.nestedQueryParameterOperation);
            requiredListQueryParams(model.requiredListQueryParams);
            optionalListQueryParams(model.optionalListQueryParams);
        }

        public final String getPathParam() {
            return pathParam;
        }

        public final void setPathParam(String pathParam) {
            this.pathParam = pathParam;
        }

        @Override
        public final Builder pathParam(String pathParam) {
            this.pathParam = pathParam;
            return this;
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

        public final String getStringHeaderMember() {
            return stringHeaderMember;
        }

        public final void setStringHeaderMember(String stringHeaderMember) {
            this.stringHeaderMember = stringHeaderMember;
        }

        @Override
        public final Builder stringHeaderMember(String stringHeaderMember) {
            this.stringHeaderMember = stringHeaderMember;
            return this;
        }

        public final NestedQueryParameterOperation.Builder getNestedQueryParameterOperation() {
            return nestedQueryParameterOperation != null ? nestedQueryParameterOperation.toBuilder() : null;
        }

        public final void setNestedQueryParameterOperation(NestedQueryParameterOperation.BuilderImpl nestedQueryParameterOperation) {
            this.nestedQueryParameterOperation = nestedQueryParameterOperation != null ? nestedQueryParameterOperation.build()
                                                                                       : null;
        }

        @Override
        public final Builder nestedQueryParameterOperation(NestedQueryParameterOperation nestedQueryParameterOperation) {
            this.nestedQueryParameterOperation = nestedQueryParameterOperation;
            return this;
        }

        public final Collection<Integer> getRequiredListQueryParams() {
            if (requiredListQueryParams instanceof SdkAutoConstructList) {
                return null;
            }
            return requiredListQueryParams;
        }

        public final void setRequiredListQueryParams(Collection<Integer> requiredListQueryParams) {
            this.requiredListQueryParams = ListOfIntegersCopier.copy(requiredListQueryParams);
        }

        @Override
        public final Builder requiredListQueryParams(Collection<Integer> requiredListQueryParams) {
            this.requiredListQueryParams = ListOfIntegersCopier.copy(requiredListQueryParams);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder requiredListQueryParams(Integer... requiredListQueryParams) {
            requiredListQueryParams(Arrays.asList(requiredListQueryParams));
            return this;
        }

        public final Collection<Integer> getOptionalListQueryParams() {
            if (optionalListQueryParams instanceof SdkAutoConstructList) {
                return null;
            }
            return optionalListQueryParams;
        }

        public final void setOptionalListQueryParams(Collection<Integer> optionalListQueryParams) {
            this.optionalListQueryParams = ListOfIntegersCopier.copy(optionalListQueryParams);
        }

        @Override
        public final Builder optionalListQueryParams(Collection<Integer> optionalListQueryParams) {
            this.optionalListQueryParams = ListOfIntegersCopier.copy(optionalListQueryParams);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder optionalListQueryParams(Integer... optionalListQueryParams) {
            optionalListQueryParams(Arrays.asList(optionalListQueryParams));
            return this;
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
        public QueryParameterOperationRequest build() {
            return new QueryParameterOperationRequest(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }
    }
}
