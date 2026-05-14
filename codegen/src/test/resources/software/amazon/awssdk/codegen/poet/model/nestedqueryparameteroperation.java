package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
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
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class NestedQueryParameterOperation implements SdkPojo, Serializable, ToCopyableBuilder<NestedQueryParameterOperation.Builder, NestedQueryParameterOperation> {
  private static final SdkField<String> QUERY_PARAM_ONE_FIELD = SdkField.<String>builder(MarshallingType.STRING)
  .memberName("QueryParamOne")
  .getter(getter(NestedQueryParameterOperation::queryParamOne))
  .setter(setter(Builder::queryParamOne))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.PAYLOAD)
  .locationName("QueryParamOne")
  .build(), RequiredTrait.create()).build();

  private static final SdkField<String> QUERY_PARAM_TWO_FIELD = SdkField.<String>builder(MarshallingType.STRING)
  .memberName("QueryParamTwo")
  .getter(getter(NestedQueryParameterOperation::queryParamTwo))
  .setter(setter(Builder::queryParamTwo))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.PAYLOAD)
  .locationName("QueryParamTwo")
  .build()).build();

  private static final SdkField<String> NESTED_HEADER_MEMBER_FIELD = SdkField.<String>builder(MarshallingType.STRING)
  .memberName("NestedHeaderMember")
  .getter(getter(NestedQueryParameterOperation::nestedHeaderMember))
  .setter(setter(Builder::nestedHeaderMember))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.PAYLOAD)
  .locationName("NestedHeaderMember")
  .build()).build();

  private static final SdkField<Integer> NESTED_STATUS_CODE_FIELD = SdkField.<Integer>builder(MarshallingType.INTEGER)
  .memberName("NestedStatusCode")
  .getter(getter(NestedQueryParameterOperation::nestedStatusCode))
  .setter(setter(Builder::nestedStatusCode))
  .traits(LocationTrait.builder()
  .location(MarshallLocation.PAYLOAD)
  .locationName("NestedStatusCode")
  .build()).build();

  private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(QUERY_PARAM_ONE_FIELD,QUERY_PARAM_TWO_FIELD,NESTED_HEADER_MEMBER_FIELD,NESTED_STATUS_CODE_FIELD));

  private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

  private static final long serialVersionUID = 1L;

  private final String queryParamOne;

  private final String queryParamTwo;

  private final String nestedHeaderMember;

  private final Integer nestedStatusCode;

  private NestedQueryParameterOperation(BuilderImpl builder) {
    this.queryParamOne = builder.queryParamOne;
    this.queryParamTwo = builder.queryParamTwo;
    this.nestedHeaderMember = builder.nestedHeaderMember;
    this.nestedStatusCode = builder.nestedStatusCode;
  }

  /**
   * Returns the value of the QueryParamOne property for this object.
   * @return The value of the QueryParamOne property for this object.
   */
  public final String queryParamOne() {
    return queryParamOne;
  }

  /**
   * Returns the value of the QueryParamTwo property for this object.
   * @return The value of the QueryParamTwo property for this object.
   */
  public final String queryParamTwo() {
    return queryParamTwo;
  }

  /**
   * Returns the value of the NestedHeaderMember property for this object.
   * @return The value of the NestedHeaderMember property for this object.
   */
  public final String nestedHeaderMember() {
    return nestedHeaderMember;
  }

  /**
   * Returns the value of the NestedStatusCode property for this object.
   * @return The value of the NestedStatusCode property for this object.
   */
  public final Integer nestedStatusCode() {
    return nestedStatusCode;
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
    hashCode = 31 * hashCode + Objects.hashCode(nestedHeaderMember());
    hashCode = 31 * hashCode + Objects.hashCode(nestedStatusCode());
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
    return Objects.equals(queryParamOne(), other.queryParamOne())&&Objects.equals(queryParamTwo(), other.queryParamTwo())&&Objects.equals(nestedHeaderMember(), other.nestedHeaderMember())&&Objects.equals(nestedStatusCode(), other.nestedStatusCode());
  }

  /**
   * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be redacted from this string using a placeholder value. 
   */
  @Override
  public final String toString() {
    return ToString.builder("NestedQueryParameterOperation").add("QueryParamOne", queryParamOne()).add("QueryParamTwo", queryParamTwo()).add("NestedHeaderMember", nestedHeaderMember()).add("NestedStatusCode", nestedStatusCode()).build();
  }

  public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
    switch (fieldName) {
      case "QueryParamOne":return Optional.ofNullable(clazz.cast(queryParamOne()));
      case "QueryParamTwo":return Optional.ofNullable(clazz.cast(queryParamTwo()));
      case "NestedHeaderMember":return Optional.ofNullable(clazz.cast(nestedHeaderMember()));
      case "NestedStatusCode":return Optional.ofNullable(clazz.cast(nestedStatusCode()));
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
    map.put("QueryParamOne", QUERY_PARAM_ONE_FIELD);
    map.put("QueryParamTwo", QUERY_PARAM_TWO_FIELD);
    map.put("NestedHeaderMember", NESTED_HEADER_MEMBER_FIELD);
    map.put("NestedStatusCode", NESTED_STATUS_CODE_FIELD);
    return Collections.unmodifiableMap(map);
  }

  private static <T> Function<Object, T> getter(Function<NestedQueryParameterOperation, T> g) {
    return obj -> g.apply((NestedQueryParameterOperation) obj);
  }

  private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
    return (obj, val) -> s.accept((Builder) obj, val);
  }

  @Mutable
  @NotThreadSafe
  public interface Builder extends SdkPojo, CopyableBuilder<Builder, NestedQueryParameterOperation> {
    /**
     * Sets the value of the QueryParamOne property for this object.
     *
     * @param queryParamOne The new value for the QueryParamOne property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder queryParamOne(String queryParamOne);

    /**
     * Sets the value of the QueryParamTwo property for this object.
     *
     * @param queryParamTwo The new value for the QueryParamTwo property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder queryParamTwo(String queryParamTwo);

    /**
     * Sets the value of the NestedHeaderMember property for this object.
     *
     * @param nestedHeaderMember The new value for the NestedHeaderMember property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder nestedHeaderMember(String nestedHeaderMember);

    /**
     * Sets the value of the NestedStatusCode property for this object.
     *
     * @param nestedStatusCode The new value for the NestedStatusCode property for this object.
     * @return Returns a reference to this object so that method calls can be chained together.
     */
    Builder nestedStatusCode(Integer nestedStatusCode);
  }

  static final class BuilderImpl implements Builder {
    private String queryParamOne;

    private String queryParamTwo;

    private String nestedHeaderMember;

    private Integer nestedStatusCode;

    private BuilderImpl() {
    }

    private BuilderImpl(NestedQueryParameterOperation model) {
      queryParamOne(model.queryParamOne);
      queryParamTwo(model.queryParamTwo);
      nestedHeaderMember(model.nestedHeaderMember);
      nestedStatusCode(model.nestedStatusCode);
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

    public final String getNestedHeaderMember() {
      return nestedHeaderMember;
    }

    public final void setNestedHeaderMember(String nestedHeaderMember) {
      this.nestedHeaderMember = nestedHeaderMember;
    }

    @Override
    public final Builder nestedHeaderMember(String nestedHeaderMember) {
      this.nestedHeaderMember = nestedHeaderMember;
      return this;
    }

    public final Integer getNestedStatusCode() {
      return nestedStatusCode;
    }

    public final void setNestedStatusCode(Integer nestedStatusCode) {
      this.nestedStatusCode = nestedStatusCode;
    }

    @Override
    public final Builder nestedStatusCode(Integer nestedStatusCode) {
      this.nestedStatusCode = nestedStatusCode;
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

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
      return SDK_NAME_TO_FIELD;
    }
  }
}
