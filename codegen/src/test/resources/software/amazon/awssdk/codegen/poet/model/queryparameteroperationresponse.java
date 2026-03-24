package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.util.Arrays;
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
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class QueryParameterOperationResponse extends JsonProtocolTestsResponse implements ToCopyableBuilder<QueryParameterOperationResponse.Builder, QueryParameterOperationResponse> {
  private static final SdkField<String> RESPONSE_HEADER_MEMBER_FIELD = SdkField.<String>builder(MarshallingType.STRING)
  .memberName("ResponseHeaderMember")
  .getter(getter(QueryParameterOperationResponse::responseHeaderMember))
  .setter(setter(Builder::responseHeaderMember))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.HEADER)
  .locationName("x-amz-response-header")
  .build()).build();

  private static final SdkField<Integer> RESPONSE_STATUS_CODE_FIELD = SdkField.<Integer>builder(MarshallingType.INTEGER)
  .memberName("ResponseStatusCode")
  .getter(getter(QueryParameterOperationResponse::responseStatusCode))
  .setter(setter(Builder::responseStatusCode))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.STATUS_CODE)
  .locationName("ResponseStatusCode")
  .build()).build();

  private static final SdkField<String> URI_MEMBER_ON_OUTPUT_FIELD = SdkField.<String>builder(MarshallingType.STRING)
  .memberName("UriMemberOnOutput")
  .getter(getter(QueryParameterOperationResponse::uriMemberOnOutput))
  .setter(setter(Builder::uriMemberOnOutput))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.PAYLOAD)
  .locationName("UriMemberOnOutput")
  .build()).build();

  private static final SdkField<String> QUERY_MEMBER_ON_OUTPUT_FIELD = SdkField.<String>builder(MarshallingType.STRING)
  .memberName("QueryMemberOnOutput")
  .getter(getter(QueryParameterOperationResponse::queryMemberOnOutput))
  .setter(setter(Builder::queryMemberOnOutput))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.PAYLOAD)
  .locationName("QueryMemberOnOutput")
  .build()).build();

  private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(RESPONSE_HEADER_MEMBER_FIELD,RESPONSE_STATUS_CODE_FIELD,URI_MEMBER_ON_OUTPUT_FIELD,QUERY_MEMBER_ON_OUTPUT_FIELD));

  private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

  private final String responseHeaderMember;

  private final Integer responseStatusCode;

  private final String uriMemberOnOutput;

  private final String queryMemberOnOutput;

  private QueryParameterOperationResponse(BuilderImpl builder) {
    super(builder);
    this.responseHeaderMember = builder.responseHeaderMember;
    this.responseStatusCode = builder.responseStatusCode;
    this.uriMemberOnOutput = builder.uriMemberOnOutput;
    this.queryMemberOnOutput = builder.queryMemberOnOutput;
  }

  /**
   * Returns the value of the ResponseHeaderMember property for this object.
   * @return The value of the ResponseHeaderMember property for this object.
   */
  public final String responseHeaderMember() {
    return responseHeaderMember;
  }

  /**
   * Returns the value of the ResponseStatusCode property for this object.
   * @return The value of the ResponseStatusCode property for this object.
   */
  public final Integer responseStatusCode() {
    return responseStatusCode;
  }

  /**
   * Returns the value of the UriMemberOnOutput property for this object.
   * @return The value of the UriMemberOnOutput property for this object.
   */
  public final String uriMemberOnOutput() {
    return uriMemberOnOutput;
  }

  /**
   * Returns the value of the QueryMemberOnOutput property for this object.
   * @return The value of the QueryMemberOnOutput property for this object.
   */
  public final String queryMemberOnOutput() {
    return queryMemberOnOutput;
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
    hashCode = 31 * hashCode + Objects.hashCode(responseHeaderMember());
    hashCode = 31 * hashCode + Objects.hashCode(responseStatusCode());
    hashCode = 31 * hashCode + Objects.hashCode(uriMemberOnOutput());
    hashCode = 31 * hashCode + Objects.hashCode(queryMemberOnOutput());
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
    if (!(obj instanceof QueryParameterOperationResponse)) {
      return false;
    }
    QueryParameterOperationResponse other = (QueryParameterOperationResponse) obj;
    return Objects.equals(responseHeaderMember(), other.responseHeaderMember())&&Objects.equals(responseStatusCode(), other.responseStatusCode())&&Objects.equals(uriMemberOnOutput(), other.uriMemberOnOutput())&&Objects.equals(queryMemberOnOutput(), other.queryMemberOnOutput());
  }

  /**
   * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be redacted from this string using a placeholder value. 
   */
  @Override
  public final String toString() {
    return ToString.builder("QueryParameterOperationResponse").add("ResponseHeaderMember", responseHeaderMember()).add("ResponseStatusCode", responseStatusCode()).add("UriMemberOnOutput", uriMemberOnOutput()).add("QueryMemberOnOutput", queryMemberOnOutput()).build();
  }

  public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
    switch (fieldName) {
      case "ResponseHeaderMember":return Optional.ofNullable(clazz.cast(responseHeaderMember()));
      case "ResponseStatusCode":return Optional.ofNullable(clazz.cast(responseStatusCode()));
      case "UriMemberOnOutput":return Optional.ofNullable(clazz.cast(uriMemberOnOutput()));
      case "QueryMemberOnOutput":return Optional.ofNullable(clazz.cast(queryMemberOnOutput()));
      default:return Optional.empty();
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
    map.put("x-amz-response-header", RESPONSE_HEADER_MEMBER_FIELD);
    map.put("ResponseStatusCode", RESPONSE_STATUS_CODE_FIELD);
    map.put("UriMemberOnOutput", URI_MEMBER_ON_OUTPUT_FIELD);
    map.put("QueryMemberOnOutput", QUERY_MEMBER_ON_OUTPUT_FIELD);
    return Collections.unmodifiableMap(map);
  }

  private static <T> Function<Object, T> getter(Function<QueryParameterOperationResponse, T> g) {
    return obj -> g.apply((QueryParameterOperationResponse) obj);
  }

  private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
    return (obj, val) -> s.accept((Builder) obj, val);
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends JsonProtocolTestsResponse.Builder, SdkPojo, CopyableBuilder<Builder, QueryParameterOperationResponse> {
    /**
     * Sets the value of the ResponseHeaderMember property for this object.
     *
     * @param responseHeaderMember The new value for the ResponseHeaderMember property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder responseHeaderMember(String responseHeaderMember);

    /**
     * Sets the value of the ResponseStatusCode property for this object.
     *
     * @param responseStatusCode The new value for the ResponseStatusCode property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder responseStatusCode(Integer responseStatusCode);

    /**
     * Sets the value of the UriMemberOnOutput property for this object.
     *
     * @param uriMemberOnOutput The new value for the UriMemberOnOutput property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder uriMemberOnOutput(String uriMemberOnOutput);

    /**
     * Sets the value of the QueryMemberOnOutput property for this object.
     *
     * @param queryMemberOnOutput The new value for the QueryMemberOnOutput property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder queryMemberOnOutput(String queryMemberOnOutput);
  }

  static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
    private String responseHeaderMember;

    private Integer responseStatusCode;

    private String uriMemberOnOutput;

    private String queryMemberOnOutput;

    private BuilderImpl() {
    }

    private BuilderImpl(QueryParameterOperationResponse model) {
      super(model);responseHeaderMember(model.responseHeaderMember);
      responseStatusCode(model.responseStatusCode);
      uriMemberOnOutput(model.uriMemberOnOutput);
      queryMemberOnOutput(model.queryMemberOnOutput);
    }

    public final String getResponseHeaderMember() {
      return responseHeaderMember;
    }

    public final void setResponseHeaderMember(String responseHeaderMember) {
      this.responseHeaderMember = responseHeaderMember;
    }

    @Override
    public final Builder responseHeaderMember(String responseHeaderMember) {
      this.responseHeaderMember = responseHeaderMember;
      return this;
    }

    public final Integer getResponseStatusCode() {
      return responseStatusCode;
    }

    public final void setResponseStatusCode(Integer responseStatusCode) {
      this.responseStatusCode = responseStatusCode;
    }

    @Override
    public final Builder responseStatusCode(Integer responseStatusCode) {
      this.responseStatusCode = responseStatusCode;
      return this;
    }

    public final String getUriMemberOnOutput() {
      return uriMemberOnOutput;
    }

    public final void setUriMemberOnOutput(String uriMemberOnOutput) {
      this.uriMemberOnOutput = uriMemberOnOutput;
    }

    @Override
    public final Builder uriMemberOnOutput(String uriMemberOnOutput) {
      this.uriMemberOnOutput = uriMemberOnOutput;
      return this;
    }

    public final String getQueryMemberOnOutput() {
      return queryMemberOnOutput;
    }

    public final void setQueryMemberOnOutput(String queryMemberOnOutput) {
      this.queryMemberOnOutput = queryMemberOnOutput;
    }

    @Override
    public final Builder queryMemberOnOutput(String queryMemberOnOutput) {
      this.queryMemberOnOutput = queryMemberOnOutput;
      return this;
    }

    @Override
    public QueryParameterOperationResponse build() {
      return new QueryParameterOperationResponse(this);
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
