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
import software.amazon.awssdk.core.traits.RequiredTrait;
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

    private static final SdkField<String> QUERY_PARAM_TWO_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                          .memberName("QueryParamTwo").getter(getter(QueryParameterOperationRequest::queryParamTwo))
                                                                          .setter(setter(Builder::queryParamTwo))
                                                                          .traits(LocationTrait.builder().location(MarshallLocation.QUERY_PARAM).locationName("QueryParamTwo").build()).build();

    private static final SdkField<String> STRING_HEADER_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                               .memberName("StringHeaderMember").getter(getter(QueryParameterOperationRequest::stringHeaderMember))
                                                                               .setter(setter(Builder::stringHeaderMember))
                                                                               .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("x-amz-header-string").build())
                                                                               .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(PATH_PARAM_FIELD,
                                                                                                   QUERY_PARAM_ONE_FIELD, QUERY_PARAM_TWO_FIELD, STRING_HEADER_MEMBER_FIELD));

    private final String pathParam;

    private final String queryParamOne;

    private final String queryParamTwo;

    private final String stringHeaderMember;

    private QueryParameterOperationRequest(BuilderImpl builder) {
        super(builder);
        this.pathParam = builder.pathParam;
        this.queryParamOne = builder.queryParamOne;
        this.queryParamTwo = builder.queryParamTwo;
        this.stringHeaderMember = builder.stringHeaderMember;
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
               && Objects.equals(stringHeaderMember(), other.stringHeaderMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString.builder("QueryParameterOperationRequest").add("PathParam", pathParam())
                       .add("QueryParamOne", queryParamOne()).add("QueryParamTwo", queryParamTwo())
                       .add("StringHeaderMember", stringHeaderMember()).build();
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

        private BuilderImpl() {
        }

        private BuilderImpl(QueryParameterOperationRequest model) {
            super(model);
            pathParam(model.pathParam);
            queryParamOne(model.queryParamOne);
            queryParamTwo(model.queryParamTwo);
            stringHeaderMember(model.stringHeaderMember);
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
