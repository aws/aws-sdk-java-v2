/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.document.Document;
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
public class TestRequest implements SdkPojo,
        ToCopyableBuilder<TestRequest.Builder, TestRequest> {
    private static SdkField<Boolean> BOOLEAN_MEMBER_FIELD = SdkField.<Boolean> builder(MarshallingType.BOOLEAN)
            .memberName("booleanMember").getter(getter(TestRequest::booleanMember))
            .setter(setter(Builder::booleanMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("booleanMember").build()).build();

    private static SdkField<String> STRING_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                        .memberName("stringMember").getter(getter(TestRequest::stringMember)).setter(setter(Builder::stringMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("stringMember").build()).build();

    private static SdkField<Integer> INTEGER_MEMBER_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
            .memberName("integerMember").getter(getter(TestRequest::integerMember))
            .setter(setter(Builder::integerMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("integerMember").build()).build();

    private static SdkField<Long> LONG_MEMBER_FIELD = SdkField.<Long> builder(MarshallingType.LONG)
                                                                    .memberName("longMember").getter(getter(TestRequest::longMember)).setter(setter(Builder::longMember))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("longMember").build()).build();

    private static SdkField<Float> FLOAT_MEMBER_FIELD = SdkField.<Float> builder(MarshallingType.FLOAT)
                                                                      .memberName("floatMember").getter(getter(TestRequest::floatMember)).setter(setter(Builder::floatMember))
                                                                      .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("floatMember").build()).build();

    private static SdkField<Double> DOUBLE_MEMBER_FIELD = SdkField.<Double> builder(MarshallingType.DOUBLE)
                                                                        .memberName("doubleMember").getter(getter(TestRequest::doubleMember)).setter(setter(Builder::doubleMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("doubleMember").build()).build();

    private static SdkField<Instant> TIMESTAMP_MEMBER_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
            .memberName("timestampMember").getter(getter(TestRequest::timestampMember))
            .setter(setter(Builder::timestampMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("timestampMember").build()).build();

    private static SdkField<SdkBytes> BLOB_MEMBER_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
                                                                        .memberName("blobMember").getter(getter(TestRequest::blobMember)).setter(setter(Builder::blobMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("blobMember").build()).build();

    private static SdkField<List<String>> LIST_OF_STRINGS_MEMBER_FIELD = SdkField
            .<List<String>> builder(MarshallingType.LIST)
            .memberName("listOfStringsMember")
            .getter(getter(TestRequest::listOfStringsMember))
            .setter(setter(Builder::listOfStringsMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("listOfStringsMember").build(),
                    ListTrait
                            .builder()
                            .memberLocationName(null)
                            .memberFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("member").build()).build()).build()).build();

    private static SdkField<Map<String, String>> MAP_OF_STRING_TO_STRING_MEMBER_FIELD = SdkField
            .<Map<String, String>> builder(MarshallingType.MAP)
            .memberName("mapOfStringToStringMember")
            .getter(getter(TestRequest::mapOfStringToStringMember))
            .setter(setter(Builder::mapOfStringToStringMember))
            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("mapOfStringToStringMember").build(),
                    MapTrait.builder()
                            .keyLocationName("key")
                            .valueLocationName("value")
                            .valueFieldInfo(
                                    SdkField.<String> builder(MarshallingType.STRING)
                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                    .locationName("value").build()).build()).build()).build();

    private static SdkField<ComplexStructure> COMPLEX_STRUCT_MEMBER_FIELD = SdkField
        .<ComplexStructure> builder(MarshallingType.SDK_POJO).memberName("complexStructMember")
        .getter(getter(TestRequest::complexStructMember)).setter(setter(Builder::complexStructMember))
        .constructor(ComplexStructure::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("complexStructMember").build())
        .build();

    private static final SdkField<Document> DOCUMENT_MEMBER_FIELD = SdkField
        .<Document>builder(MarshallingType.DOCUMENT)
        .memberName("documentMember").getter(getter(TestRequest::documentField)).setter(setter(Builder::documentField))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("documentMember").build()).build();

    private static List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(BOOLEAN_MEMBER_FIELD,
            STRING_MEMBER_FIELD, INTEGER_MEMBER_FIELD, LONG_MEMBER_FIELD, FLOAT_MEMBER_FIELD, DOUBLE_MEMBER_FIELD,
            TIMESTAMP_MEMBER_FIELD, BLOB_MEMBER_FIELD, LIST_OF_STRINGS_MEMBER_FIELD, MAP_OF_STRING_TO_STRING_MEMBER_FIELD,
            COMPLEX_STRUCT_MEMBER_FIELD, DOCUMENT_MEMBER_FIELD));

    private static Map<String, SdkField<?>> SDK_NAME_TO_FIELD = Collections
            .unmodifiableMap(new HashMap<String, SdkField<?>>() {
                {
                    put("booleanMember", BOOLEAN_MEMBER_FIELD);
                    put("stringMember", STRING_MEMBER_FIELD);
                    put("integerMember", INTEGER_MEMBER_FIELD);
                    put("longMember", LONG_MEMBER_FIELD);
                    put("floatMember", FLOAT_MEMBER_FIELD);
                    put("doubleMember", DOUBLE_MEMBER_FIELD);
                    put("timestampMember", TIMESTAMP_MEMBER_FIELD);
                    put("blobMember", BLOB_MEMBER_FIELD);
                    put("listOfStringsMember", LIST_OF_STRINGS_MEMBER_FIELD);
                    put("mapOfStringToStringMember", MAP_OF_STRING_TO_STRING_MEMBER_FIELD);
                    put("complexStructMember", COMPLEX_STRUCT_MEMBER_FIELD);
                    put("documentMember", DOCUMENT_MEMBER_FIELD);
                }
            });

    private final Boolean booleanMember;

    private final String stringMember;

    private final Integer integerMember;

    private final Long longMember;

    private final Float floatMember;

    private final Double doubleMember;

    private final Instant timestampMember;

    private final SdkBytes blobMember;

    private final List<String> listOfStringsMember;

    private final Map<String, String> mapOfStringToStringMember;

    private final ComplexStructure complexStructMember;
    private final Document documentField;

    private TestRequest(BuilderImpl builder) {
        this.booleanMember = builder.booleanMember;
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.longMember = builder.longMember;
        this.floatMember = builder.floatMember;
        this.doubleMember = builder.doubleMember;
        this.timestampMember = builder.timestampMember;
        this.blobMember = builder.blobMember;
        this.listOfStringsMember = builder.listOfStringsMember;
        this.mapOfStringToStringMember = builder.mapOfStringToStringMember;
        this.complexStructMember = builder.complexStructMember;
        this.documentField = builder.documentField;
    }

    /**
     * Returns the value of the BooleanMember property for this object.
     * 
     * @return The value of the BooleanMember property for this object.
     */
    public Boolean booleanMember() {
        return booleanMember;
    }

    /**
     * Returns the value of the StringMember property for this object.
     * 
     * @return The value of the StringMember property for this object.
     */
    public String stringMember() {
        return stringMember;
    }

    /**
     * Returns the value of the IntegerMember property for this object.
     * 
     * @return The value of the IntegerMember property for this object.
     */
    public Integer integerMember() {
        return integerMember;
    }

    /**
     * Returns the value of the LongMember property for this object.
     * 
     * @return The value of the LongMember property for this object.
     */
    public Long longMember() {
        return longMember;
    }

    /**
     * Returns the value of the FloatMember property for this object.
     * 
     * @return The value of the FloatMember property for this object.
     */
    public Float floatMember() {
        return floatMember;
    }

    /**
     * Returns the value of the DoubleMember property for this object.
     * 
     * @return The value of the DoubleMember property for this object.
     */
    public Double doubleMember() {
        return doubleMember;
    }

    /**
     * Returns the value of the TimestampMember property for this object.
     * 
     * @return The value of the TimestampMember property for this object.
     */
    public Instant timestampMember() {
        return timestampMember;
    }

    /**
     * Returns the value of the BlobMember property for this object.
     * 
     * @return The value of the BlobMember property for this object.
     */
    public SdkBytes blobMember() {
        return blobMember;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfStringsMember property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public boolean hasListOfStringsMember() {
        return listOfStringsMember != null && !(listOfStringsMember instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfStringsMember property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfStringsMember} method.
     * </p>
     * 
     * @return The value of the ListOfStringsMember property for this object.
     */
    public List<String> listOfStringsMember() {
        return listOfStringsMember;
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfStringToStringMember property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public boolean hasMapOfStringToStringMember() {
        return mapOfStringToStringMember != null && !(mapOfStringToStringMember instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToStringMember property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToStringMember} method.
     * </p>
     * 
     * @return The value of the MapOfStringToStringMember property for this object.
     */
    public Map<String, String> mapOfStringToStringMember() {
        return mapOfStringToStringMember;
    }

    /**
     * Returns the value of the ComplexStructMember property for this object.
     * 
     * @return The value of the ComplexStructMember property for this object.
     */
    public ComplexStructure complexStructMember() {
        return complexStructMember;
    }

    /**
     * Returns the value of the MyDocument property for this object.
     *
     * @return The value of the MyDocument property for this object.
     */
    public final Document documentField() {
        return documentField;
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
        hashCode = 31 * hashCode + Objects.hashCode(booleanMember());
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        hashCode = 31 * hashCode + Objects.hashCode(integerMember());
        hashCode = 31 * hashCode + Objects.hashCode(longMember());
        hashCode = 31 * hashCode + Objects.hashCode(floatMember());
        hashCode = 31 * hashCode + Objects.hashCode(doubleMember());
        hashCode = 31 * hashCode + Objects.hashCode(timestampMember());
        hashCode = 31 * hashCode + Objects.hashCode(blobMember());
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfStringsMember() ? listOfStringsMember() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfStringToStringMember() ? mapOfStringToStringMember() : null);
        hashCode = 31 * hashCode + Objects.hashCode(complexStructMember());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && equalsBySdkFields(obj);
    }

    public boolean equalsBySdkFields(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TestRequest)) {
            return false;
        }
        TestRequest other = (TestRequest) obj;
        return Objects.equals(booleanMember(), other.booleanMember()) && Objects.equals(stringMember(), other.stringMember())
                && Objects.equals(integerMember(), other.integerMember()) && Objects.equals(longMember(), other.longMember())
                && Objects.equals(floatMember(), other.floatMember()) && Objects.equals(doubleMember(), other.doubleMember())
                && Objects.equals(timestampMember(), other.timestampMember()) && Objects.equals(blobMember(), other.blobMember())
                && hasListOfStringsMember() == other.hasListOfStringsMember()
                && Objects.equals(listOfStringsMember(), other.listOfStringsMember())
                && hasMapOfStringToStringMember() == other.hasMapOfStringToStringMember()
                && Objects.equals(mapOfStringToStringMember(), other.mapOfStringToStringMember())
                && Objects.equals(complexStructMember(), other.complexStructMember());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("EchoOperationRequest").add("BooleanMember", booleanMember()).add("StringMember", stringMember())
                .add("IntegerMember", integerMember()).add("LongMember", longMember()).add("FloatMember", floatMember())
                .add("DoubleMember", doubleMember()).add("TimestampMember", timestampMember()).add("BlobMember", blobMember())
                .add("ListOfStringsMember", hasListOfStringsMember() ? listOfStringsMember() : null)
                .add("MapOfStringToStringMember", hasMapOfStringToStringMember() ? mapOfStringToStringMember() : null)
                .add("ComplexStructMember", complexStructMember()).build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "booleanMember":
            return Optional.ofNullable(clazz.cast(booleanMember()));
        case "stringMember":
            return Optional.ofNullable(clazz.cast(stringMember()));
        case "integerMember":
            return Optional.ofNullable(clazz.cast(integerMember()));
        case "longMember":
            return Optional.ofNullable(clazz.cast(longMember()));
        case "floatMember":
            return Optional.ofNullable(clazz.cast(floatMember()));
        case "doubleMember":
            return Optional.ofNullable(clazz.cast(doubleMember()));
        case "timestampMember":
            return Optional.ofNullable(clazz.cast(timestampMember()));
        case "blobMember":
            return Optional.ofNullable(clazz.cast(blobMember()));
        case "listOfStringsMember":
            return Optional.ofNullable(clazz.cast(listOfStringsMember()));
        case "mapOfStringToStringMember":
            return Optional.ofNullable(clazz.cast(mapOfStringToStringMember()));
        case "complexStructMember":
            return Optional.ofNullable(clazz.cast(complexStructMember()));
        default:
            return Optional.empty();
        }
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    private static <T> Function<Object, T> getter(Function<TestRequest, T> g) {
        return obj -> g.apply((TestRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, TestRequest> {
        /**
         * Sets the value of the BooleanMember property for this object.
         *
         * @param booleanMember
         *        The new value for the BooleanMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder booleanMember(Boolean booleanMember);

        /**
         * Sets the value of the StringMember property for this object.
         *
         * @param stringMember
         *        The new value for the StringMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder stringMember(String stringMember);

        /**
         * Sets the value of the IntegerMember property for this object.
         *
         * @param integerMember
         *        The new value for the IntegerMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder integerMember(Integer integerMember);

        /**
         * Sets the value of the LongMember property for this object.
         *
         * @param longMember
         *        The new value for the LongMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder longMember(Long longMember);

        /**
         * Sets the value of the FloatMember property for this object.
         *
         * @param floatMember
         *        The new value for the FloatMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder floatMember(Float floatMember);

        /**
         * Sets the value of the DoubleMember property for this object.
         *
         * @param doubleMember
         *        The new value for the DoubleMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder doubleMember(Double doubleMember);

        /**
         * Sets the value of the TimestampMember property for this object.
         *
         * @param timestampMember
         *        The new value for the TimestampMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder timestampMember(Instant timestampMember);

        /**
         * Sets the value of the BlobMember property for this object.
         *
         * @param blobMember
         *        The new value for the BlobMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobMember(SdkBytes blobMember);

        /**
         * Sets the value of the ListOfStringsMember property for this object.
         *
         * @param listOfStringsMember
         *        The new value for the ListOfStringsMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStringsMember(Collection<String> listOfStringsMember);

        /**
         * Sets the value of the ListOfStringsMember property for this object.
         *
         * @param listOfStringsMember
         *        The new value for the ListOfStringsMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStringsMember(String... listOfStringsMember);

        /**
         * Sets the value of the MapOfStringToStringMember property for this object.
         *
         * @param mapOfStringToStringMember
         *        The new value for the MapOfStringToStringMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToStringMember(Map<String, String> mapOfStringToStringMember);

        /**
         * Sets the value of the ComplexStructMember property for this object.
         *
         * @param complexStructMember
         *        The new value for the ComplexStructMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder complexStructMember(ComplexStructure complexStructMember);

        /**
         * Sets the value of the MyDocument property for this object.
         *
         * @param documentField
         *        The new value for the MyDocument property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder documentField(Document documentField);

        /**
         * Sets the value of the ComplexStructMember property for this object.
         *
         * This is a convenience method that creates an instance of the {@link ComplexStructure.Builder} avoiding the
         * need to create one manually via {@link ComplexStructure#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link ComplexStructure.Builder#build()} is called immediately and its
         * result is passed to {@link #complexStructMember(ComplexStructure)}.
         * 
         * @param complexStructMember
         *        a consumer that will call methods on {@link ComplexStructure.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #complexStructMember(ComplexStructure)
         */
        default Builder complexStructMember(Consumer<ComplexStructure.Builder> complexStructMember) {
            return complexStructMember(ComplexStructure.builder().applyMutation(complexStructMember).build());
        }
    }

    static class BuilderImpl implements Builder {
        private Boolean booleanMember;

        private String stringMember;

        private Integer integerMember;

        private Long longMember;

        private Float floatMember;

        private Double doubleMember;

        private Instant timestampMember;

        private SdkBytes blobMember;

        private List<String> listOfStringsMember = DefaultSdkAutoConstructList.getInstance();

        private Map<String, String> mapOfStringToStringMember = DefaultSdkAutoConstructMap.getInstance();

        private ComplexStructure complexStructMember;
        private Document documentField;

        private BuilderImpl() {
        }

        private BuilderImpl(TestRequest model) {
            booleanMember(model.booleanMember);
            stringMember(model.stringMember);
            integerMember(model.integerMember);
            longMember(model.longMember);
            floatMember(model.floatMember);
            doubleMember(model.doubleMember);
            timestampMember(model.timestampMember);
            blobMember(model.blobMember);
            listOfStringsMember(model.listOfStringsMember);
            mapOfStringToStringMember(model.mapOfStringToStringMember);
            complexStructMember(model.complexStructMember);
            documentField(model.documentField);
        }

        public Boolean getBooleanMember() {
            return booleanMember;
        }

        public void setBooleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
        }

        @Override
        public Builder booleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
            return this;
        }

        public String getStringMember() {
            return stringMember;
        }

        public void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public Integer getIntegerMember() {
            return integerMember;
        }

        public void setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
        }

        @Override
        public Builder integerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        public Long getLongMember() {
            return longMember;
        }

        public void setLongMember(Long longMember) {
            this.longMember = longMember;
        }

        @Override
        public Builder longMember(Long longMember) {
            this.longMember = longMember;
            return this;
        }

        public Float getFloatMember() {
            return floatMember;
        }

        public void setFloatMember(Float floatMember) {
            this.floatMember = floatMember;
        }

        @Override
        public Builder floatMember(Float floatMember) {
            this.floatMember = floatMember;
            return this;
        }

        public Double getDoubleMember() {
            return doubleMember;
        }

        public void setDoubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
        }

        @Override
        public Builder doubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
            return this;
        }

        public Instant getTimestampMember() {
            return timestampMember;
        }

        public void setTimestampMember(Instant timestampMember) {
            this.timestampMember = timestampMember;
        }

        @Override
        public Builder timestampMember(Instant timestampMember) {
            this.timestampMember = timestampMember;
            return this;
        }

        public ByteBuffer getBlobMember() {
            return blobMember == null ? null : blobMember.asByteBuffer();
        }

        public void setBlobMember(ByteBuffer blobMember) {
            blobMember(blobMember == null ? null : SdkBytes.fromByteBuffer(blobMember));
        }

        @Override
        public Builder blobMember(SdkBytes blobMember) {
            this.blobMember = blobMember;
            return this;
        }

        public Collection<String> getListOfStringsMember() {
            if (listOfStringsMember instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfStringsMember;
        }

        public void setListOfStringsMember(Collection<String> listOfStringsMember) {
            this.listOfStringsMember = ListOfStringsCopier.copy(listOfStringsMember);
        }

        @Override
        public Builder listOfStringsMember(Collection<String> listOfStringsMember) {
            this.listOfStringsMember = ListOfStringsCopier.copy(listOfStringsMember);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfStringsMember(String... listOfStringsMember) {
            listOfStringsMember(Arrays.asList(listOfStringsMember));
            return this;
        }

        public Map<String, String> getMapOfStringToStringMember() {
            if (mapOfStringToStringMember instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToStringMember;
        }

        public void setMapOfStringToStringMember(Map<String, String> mapOfStringToStringMember) {
            this.mapOfStringToStringMember = MapOfStringToStringCopier.copy(mapOfStringToStringMember);
        }

        @Override
        public Builder mapOfStringToStringMember(Map<String, String> mapOfStringToStringMember) {
            this.mapOfStringToStringMember = MapOfStringToStringCopier.copy(mapOfStringToStringMember);
            return this;
        }

        public ComplexStructure.Builder getComplexStructMember() {
            return complexStructMember != null ? complexStructMember.toBuilder() : null;
        }

        public void setComplexStructMember(ComplexStructure.BuilderImpl complexStructMember) {
            this.complexStructMember = complexStructMember != null ? complexStructMember.build() : null;
        }

        @Override
        public Builder complexStructMember(ComplexStructure complexStructMember) {
            this.complexStructMember = complexStructMember;
            return this;
        }

        @Override
        public Builder documentField(Document documentField) {
            this.documentField = documentField;
            return this;
        }

        @Override
        public TestRequest build() {
            return new TestRequest(this);
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
