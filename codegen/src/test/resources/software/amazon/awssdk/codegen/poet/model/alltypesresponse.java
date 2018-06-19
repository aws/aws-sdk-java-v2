package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;
import software.amazon.awssdk.core.adapter.StringToByteBufferAdapter;
import software.amazon.awssdk.core.internal.StandardMemberCopier;
import software.amazon.awssdk.core.runtime.TypeConverter;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public final class AllTypesResponse extends JsonProtocolTestsResponse implements
        ToCopyableBuilder<AllTypesResponse.Builder, AllTypesResponse> {
    private final String stringMember;

    private final Integer integerMember;

    private final Boolean booleanMember;

    private final Float floatMember;

    private final Double doubleMember;

    private final Long longMember;

    private final List<String> simpleList;

    private final List<String> listOfEnums;

    private final List<Map<String, String>> listOfMaps;

    private final List<SimpleStruct> listOfStructs;

    private final Map<String, List<Integer>> mapOfStringToIntegerList;

    private final Map<String, String> mapOfStringToString;

    private final Map<String, SimpleStruct> mapOfStringToSimpleStruct;

    private final Map<String, String> mapOfEnumToEnum;

    private final Map<String, String> mapOfEnumToString;

    private final Map<String, String> mapOfStringToEnum;

    private final Map<String, SimpleStruct> mapOfEnumToSimpleStruct;

    private final Instant timestampMember;

    private final StructWithTimestamp structWithNestedTimestampMember;

    private final ByteBuffer blobArg;

    private final StructWithNestedBlobType structWithNestedBlob;

    private final Map<String, ByteBuffer> blobMap;

    private final List<ByteBuffer> listOfBlobs;

    private final RecursiveStructType recursiveStruct;

    private final BaseType polymorphicTypeWithSubTypes;

    private final SubTypeOne polymorphicTypeWithoutSubTypes;

    private final String enumType;

    private AllTypesResponse(BuilderImpl builder) {
        super(builder);
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.booleanMember = builder.booleanMember;
        this.floatMember = builder.floatMember;
        this.doubleMember = builder.doubleMember;
        this.longMember = builder.longMember;
        this.simpleList = builder.simpleList;
        this.listOfEnums = builder.listOfEnums;
        this.listOfMaps = builder.listOfMaps;
        this.listOfStructs = builder.listOfStructs;
        this.mapOfStringToIntegerList = builder.mapOfStringToIntegerList;
        this.mapOfStringToString = builder.mapOfStringToString;
        this.mapOfStringToSimpleStruct = builder.mapOfStringToSimpleStruct;
        this.mapOfEnumToEnum = builder.mapOfEnumToEnum;
        this.mapOfEnumToString = builder.mapOfEnumToString;
        this.mapOfStringToEnum = builder.mapOfStringToEnum;
        this.mapOfEnumToSimpleStruct = builder.mapOfEnumToSimpleStruct;
        this.timestampMember = builder.timestampMember;
        this.structWithNestedTimestampMember = builder.structWithNestedTimestampMember;
        this.blobArg = builder.blobArg;
        this.structWithNestedBlob = builder.structWithNestedBlob;
        this.blobMap = builder.blobMap;
        this.listOfBlobs = builder.listOfBlobs;
        this.recursiveStruct = builder.recursiveStruct;
        this.polymorphicTypeWithSubTypes = builder.polymorphicTypeWithSubTypes;
        this.polymorphicTypeWithoutSubTypes = builder.polymorphicTypeWithoutSubTypes;
        this.enumType = builder.enumType;
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
     * Returns the value of the BooleanMember property for this object.
     *
     * @return The value of the BooleanMember property for this object.
     */
    public Boolean booleanMember() {
        return booleanMember;
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
     * Returns the value of the LongMember property for this object.
     *
     * @return The value of the LongMember property for this object.
     */
    public Long longMember() {
        return longMember;
    }

    /**
     * Returns the value of the SimpleList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the SimpleList property for this object.
     */
    public List<String> simpleList() {
        return simpleList;
    }

    /**
     * Returns the value of the ListOfEnums property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfEnums property for this object.
     */
    public List<EnumType> listOfEnums() {
        return TypeConverter.convert(listOfEnums, EnumType::fromValue);
    }

    /**
     * Returns the value of the ListOfEnums property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfEnums property for this object.
     */
    public List<String> listOfEnumsAsStrings() {
        return listOfEnums;
    }

    /**
     * Returns the value of the ListOfMaps property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfMaps property for this object.
     */
    public List<Map<String, String>> listOfMaps() {
        return listOfMaps;
    }

    /**
     * Returns the value of the ListOfStructs property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfStructs property for this object.
     */
    public List<SimpleStruct> listOfStructs() {
        return listOfStructs;
    }

    /**
     * Returns the value of the MapOfStringToIntegerList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfStringToIntegerList property for this object.
     */
    public Map<String, List<Integer>> mapOfStringToIntegerList() {
        return mapOfStringToIntegerList;
    }

    /**
     * Returns the value of the MapOfStringToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfStringToString property for this object.
     */
    public Map<String, String> mapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     * Returns the value of the MapOfStringToSimpleStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfStringToSimpleStruct property for this object.
     */
    public Map<String, SimpleStruct> mapOfStringToSimpleStruct() {
        return mapOfStringToSimpleStruct;
    }

    /**
     * Returns the value of the MapOfEnumToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfEnumToEnum property for this object.
     */
    public Map<EnumType, EnumType> mapOfEnumToEnum() {
        return TypeConverter.convert(mapOfEnumToEnum, EnumType::fromValue, EnumType::fromValue,
                (k, v) -> !Objects.equals(k, EnumType.UNKNOWN_TO_SDK_VERSION));
    }

    /**
     * Returns the value of the MapOfEnumToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfEnumToEnum property for this object.
     */
    public Map<String, String> mapOfEnumToEnumAsStrings() {
        return mapOfEnumToEnum;
    }

    /**
     * Returns the value of the MapOfEnumToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfEnumToString property for this object.
     */
    public Map<EnumType, String> mapOfEnumToString() {
        return TypeConverter.convert(mapOfEnumToString, EnumType::fromValue, Function.identity(),
                (k, v) -> !Objects.equals(k, EnumType.UNKNOWN_TO_SDK_VERSION));
    }

    /**
     * Returns the value of the MapOfEnumToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfEnumToString property for this object.
     */
    public Map<String, String> mapOfEnumToStringAsStrings() {
        return mapOfEnumToString;
    }

    /**
     * Returns the value of the MapOfStringToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfStringToEnum property for this object.
     */
    public Map<String, EnumType> mapOfStringToEnum() {
        return TypeConverter.convert(mapOfStringToEnum, Function.identity(), EnumType::fromValue, (k, v) -> true);
    }

    /**
     * Returns the value of the MapOfStringToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfStringToEnum property for this object.
     */
    public Map<String, String> mapOfStringToEnumAsStrings() {
        return mapOfStringToEnum;
    }

    /**
     * Returns the value of the MapOfEnumToSimpleStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfEnumToSimpleStruct property for this object.
     */
    public Map<EnumType, SimpleStruct> mapOfEnumToSimpleStruct() {
        return TypeConverter.convert(mapOfEnumToSimpleStruct, EnumType::fromValue, Function.identity(),
                (k, v) -> !Objects.equals(k, EnumType.UNKNOWN_TO_SDK_VERSION));
    }

    /**
     * Returns the value of the MapOfEnumToSimpleStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the MapOfEnumToSimpleStruct property for this object.
     */
    public Map<String, SimpleStruct> mapOfEnumToSimpleStructAsStrings() {
        return mapOfEnumToSimpleStruct;
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
     * Returns the value of the StructWithNestedTimestampMember property for this object.
     *
     * @return The value of the StructWithNestedTimestampMember property for this object.
     */
    public StructWithTimestamp structWithNestedTimestampMember() {
        return structWithNestedTimestampMember;
    }

    /**
     * Returns the value of the BlobArg property for this object.
     * <p>
     * This method will return a new read-only {@code ByteBuffer} each time it is invoked.
     * </p>
     *
     * @return The value of the BlobArg property for this object.
     */
    public ByteBuffer blobArg() {
        return blobArg == null ? null : blobArg.asReadOnlyBuffer();
    }

    /**
     * Returns the value of the StructWithNestedBlob property for this object.
     *
     * @return The value of the StructWithNestedBlob property for this object.
     */
    public StructWithNestedBlobType structWithNestedBlob() {
        return structWithNestedBlob;
    }

    /**
     * Returns the value of the BlobMap property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the BlobMap property for this object.
     */
    public Map<String, ByteBuffer> blobMap() {
        return blobMap;
    }

    /**
     * Returns the value of the ListOfBlobs property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     *
     * @return The value of the ListOfBlobs property for this object.
     */
    public List<ByteBuffer> listOfBlobs() {
        return listOfBlobs;
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
     * Returns the value of the PolymorphicTypeWithSubTypes property for this object.
     *
     * @return The value of the PolymorphicTypeWithSubTypes property for this object.
     */
    public BaseType polymorphicTypeWithSubTypes() {
        return polymorphicTypeWithSubTypes;
    }

    /**
     * Returns the value of the PolymorphicTypeWithoutSubTypes property for this object.
     *
     * @return The value of the PolymorphicTypeWithoutSubTypes property for this object.
     */
    public SubTypeOne polymorphicTypeWithoutSubTypes() {
        return polymorphicTypeWithoutSubTypes;
    }

    /**
     * Returns the value of the EnumType property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #enumType} will
     * return {@link EnumType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #enumTypeAsString}.
     * </p>
     *
     * @return The value of the EnumType property for this object.
     * @see EnumType
     */
    public EnumType enumType() {
        return EnumType.fromValue(enumType);
    }

    /**
     * Returns the value of the EnumType property for this object.
     * <p>
     * If the service returns an enum value that is not available in the current SDK version, {@link #enumType} will
     * return {@link EnumType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from
     * {@link #enumTypeAsString}.
     * </p>
     *
     * @return The value of the EnumType property for this object.
     * @see EnumType
     */
    public String enumTypeAsString() {
        return enumType;
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
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        hashCode = 31 * hashCode + Objects.hashCode(integerMember());
        hashCode = 31 * hashCode + Objects.hashCode(booleanMember());
        hashCode = 31 * hashCode + Objects.hashCode(floatMember());
        hashCode = 31 * hashCode + Objects.hashCode(doubleMember());
        hashCode = 31 * hashCode + Objects.hashCode(longMember());
        hashCode = 31 * hashCode + Objects.hashCode(simpleList());
        hashCode = 31 * hashCode + Objects.hashCode(listOfEnumsAsStrings());
        hashCode = 31 * hashCode + Objects.hashCode(listOfMaps());
        hashCode = 31 * hashCode + Objects.hashCode(listOfStructs());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfStringToIntegerList());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfStringToString());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfStringToSimpleStruct());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfEnumToEnumAsStrings());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfEnumToStringAsStrings());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfStringToEnumAsStrings());
        hashCode = 31 * hashCode + Objects.hashCode(mapOfEnumToSimpleStructAsStrings());
        hashCode = 31 * hashCode + Objects.hashCode(timestampMember());
        hashCode = 31 * hashCode + Objects.hashCode(structWithNestedTimestampMember());
        hashCode = 31 * hashCode + Objects.hashCode(blobArg());
        hashCode = 31 * hashCode + Objects.hashCode(structWithNestedBlob());
        hashCode = 31 * hashCode + Objects.hashCode(blobMap());
        hashCode = 31 * hashCode + Objects.hashCode(listOfBlobs());
        hashCode = 31 * hashCode + Objects.hashCode(recursiveStruct());
        hashCode = 31 * hashCode + Objects.hashCode(polymorphicTypeWithSubTypes());
        hashCode = 31 * hashCode + Objects.hashCode(polymorphicTypeWithoutSubTypes());
        hashCode = 31 * hashCode + Objects.hashCode(enumTypeAsString());
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
        if (!(obj instanceof AllTypesResponse)) {
            return false;
        }
        AllTypesResponse other = (AllTypesResponse) obj;
        return Objects.equals(stringMember(), other.stringMember()) && Objects.equals(integerMember(), other.integerMember())
                && Objects.equals(booleanMember(), other.booleanMember()) && Objects.equals(floatMember(), other.floatMember())
                && Objects.equals(doubleMember(), other.doubleMember()) && Objects.equals(longMember(), other.longMember())
                && Objects.equals(simpleList(), other.simpleList())
                && Objects.equals(listOfEnumsAsStrings(), other.listOfEnumsAsStrings())
                && Objects.equals(listOfMaps(), other.listOfMaps()) && Objects.equals(listOfStructs(), other.listOfStructs())
                && Objects.equals(mapOfStringToIntegerList(), other.mapOfStringToIntegerList())
                && Objects.equals(mapOfStringToString(), other.mapOfStringToString())
                && Objects.equals(mapOfStringToSimpleStruct(), other.mapOfStringToSimpleStruct())
                && Objects.equals(mapOfEnumToEnumAsStrings(), other.mapOfEnumToEnumAsStrings())
                && Objects.equals(mapOfEnumToStringAsStrings(), other.mapOfEnumToStringAsStrings())
                && Objects.equals(mapOfStringToEnumAsStrings(), other.mapOfStringToEnumAsStrings())
                && Objects.equals(mapOfEnumToSimpleStructAsStrings(), other.mapOfEnumToSimpleStructAsStrings())
                && Objects.equals(timestampMember(), other.timestampMember())
                && Objects.equals(structWithNestedTimestampMember(), other.structWithNestedTimestampMember())
                && Objects.equals(blobArg(), other.blobArg())
                && Objects.equals(structWithNestedBlob(), other.structWithNestedBlob())
                && Objects.equals(blobMap(), other.blobMap()) && Objects.equals(listOfBlobs(), other.listOfBlobs())
                && Objects.equals(recursiveStruct(), other.recursiveStruct())
                && Objects.equals(polymorphicTypeWithSubTypes(), other.polymorphicTypeWithSubTypes())
                && Objects.equals(polymorphicTypeWithoutSubTypes(), other.polymorphicTypeWithoutSubTypes())
                && Objects.equals(enumTypeAsString(), other.enumTypeAsString());
    }

    @Override
    public String toString() {
        return ToString.builder("AllTypesResponse").add("StringMember", stringMember()).add("IntegerMember", integerMember())
                .add("BooleanMember", booleanMember()).add("FloatMember", floatMember()).add("DoubleMember", doubleMember())
                .add("LongMember", longMember()).add("SimpleList", simpleList()).add("ListOfEnums", listOfEnumsAsStrings())
                .add("ListOfMaps", listOfMaps()).add("ListOfStructs", listOfStructs())
                .add("MapOfStringToIntegerList", mapOfStringToIntegerList()).add("MapOfStringToString", mapOfStringToString())
                .add("MapOfStringToSimpleStruct", mapOfStringToSimpleStruct()).add("MapOfEnumToEnum", mapOfEnumToEnumAsStrings())
                .add("MapOfEnumToString", mapOfEnumToStringAsStrings()).add("MapOfStringToEnum", mapOfStringToEnumAsStrings())
                .add("MapOfEnumToSimpleStruct", mapOfEnumToSimpleStructAsStrings()).add("TimestampMember", timestampMember())
                .add("StructWithNestedTimestampMember", structWithNestedTimestampMember()).add("BlobArg", blobArg())
                .add("StructWithNestedBlob", structWithNestedBlob()).add("BlobMap", blobMap()).add("ListOfBlobs", listOfBlobs())
                .add("RecursiveStruct", recursiveStruct()).add("PolymorphicTypeWithSubTypes", polymorphicTypeWithSubTypes())
                .add("PolymorphicTypeWithoutSubTypes", polymorphicTypeWithoutSubTypes()).add("EnumType", enumTypeAsString())
                .build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
        case "StringMember":
            return Optional.of(clazz.cast(stringMember()));
        case "IntegerMember":
            return Optional.of(clazz.cast(integerMember()));
        case "BooleanMember":
            return Optional.of(clazz.cast(booleanMember()));
        case "FloatMember":
            return Optional.of(clazz.cast(floatMember()));
        case "DoubleMember":
            return Optional.of(clazz.cast(doubleMember()));
        case "LongMember":
            return Optional.of(clazz.cast(longMember()));
        case "SimpleList":
            return Optional.of(clazz.cast(simpleList()));
        case "ListOfEnums":
            return Optional.of(clazz.cast(listOfEnumsAsStrings()));
        case "ListOfMaps":
            return Optional.of(clazz.cast(listOfMaps()));
        case "ListOfStructs":
            return Optional.of(clazz.cast(listOfStructs()));
        case "MapOfStringToIntegerList":
            return Optional.of(clazz.cast(mapOfStringToIntegerList()));
        case "MapOfStringToString":
            return Optional.of(clazz.cast(mapOfStringToString()));
        case "MapOfStringToSimpleStruct":
            return Optional.of(clazz.cast(mapOfStringToSimpleStruct()));
        case "MapOfEnumToEnum":
            return Optional.of(clazz.cast(mapOfEnumToEnumAsStrings()));
        case "MapOfEnumToString":
            return Optional.of(clazz.cast(mapOfEnumToStringAsStrings()));
        case "MapOfStringToEnum":
            return Optional.of(clazz.cast(mapOfStringToEnumAsStrings()));
        case "MapOfEnumToSimpleStruct":
            return Optional.of(clazz.cast(mapOfEnumToSimpleStructAsStrings()));
        case "TimestampMember":
            return Optional.of(clazz.cast(timestampMember()));
        case "StructWithNestedTimestampMember":
            return Optional.of(clazz.cast(structWithNestedTimestampMember()));
        case "BlobArg":
            return Optional.of(clazz.cast(blobArg()));
        case "StructWithNestedBlob":
            return Optional.of(clazz.cast(structWithNestedBlob()));
        case "BlobMap":
            return Optional.of(clazz.cast(blobMap()));
        case "ListOfBlobs":
            return Optional.of(clazz.cast(listOfBlobs()));
        case "RecursiveStruct":
            return Optional.of(clazz.cast(recursiveStruct()));
        case "PolymorphicTypeWithSubTypes":
            return Optional.of(clazz.cast(polymorphicTypeWithSubTypes()));
        case "PolymorphicTypeWithoutSubTypes":
            return Optional.of(clazz.cast(polymorphicTypeWithoutSubTypes()));
        case "EnumType":
            return Optional.of(clazz.cast(enumTypeAsString()));
        default:
            return Optional.empty();
        }
    }

    public interface Builder extends JsonProtocolTestsResponse.Builder, CopyableBuilder<Builder, AllTypesResponse> {
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
         * Sets the value of the BooleanMember property for this object.
         *
         * @param booleanMember
         *        The new value for the BooleanMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder booleanMember(Boolean booleanMember);

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
         * Sets the value of the LongMember property for this object.
         *
         * @param longMember
         *        The new value for the LongMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder longMember(Long longMember);

        /**
         * Sets the value of the SimpleList property for this object.
         *
         * @param simpleList
         *        The new value for the SimpleList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder simpleList(Collection<String> simpleList);

        /**
         * Sets the value of the SimpleList property for this object.
         *
         * @param simpleList
         *        The new value for the SimpleList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder simpleList(String... simpleList);

        /**
         * Sets the value of the ListOfEnums property for this object.
         *
         * @param listOfEnums
         *        The new value for the ListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfEnums(Collection<String> listOfEnums);

        /**
         * Sets the value of the ListOfEnums property for this object.
         *
         * @param listOfEnums
         *        The new value for the ListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfEnums(String... listOfEnums);

        /**
         * Sets the value of the ListOfMaps property for this object.
         *
         * @param listOfMaps
         *        The new value for the ListOfMaps property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Collection<Map<String, String>> listOfMaps);

        /**
         * Sets the value of the ListOfMaps property for this object.
         *
         * @param listOfMaps
         *        The new value for the ListOfMaps property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Map<String, String>... listOfMaps);

        /**
         * Sets the value of the ListOfStructs property for this object.
         *
         * @param listOfStructs
         *        The new value for the ListOfStructs property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStructs(Collection<SimpleStruct> listOfStructs);

        /**
         * Sets the value of the ListOfStructs property for this object.
         *
         * @param listOfStructs
         *        The new value for the ListOfStructs property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfStructs(SimpleStruct... listOfStructs);

        /**
         * Sets the value of the ListOfStructs property for this object.
         *
         * This is a convenience that creates an instance of the {@link List<SimpleStruct>.Builder} avoiding the need to
         * create one manually via {@link List<SimpleStruct>#builder()}.
         *
         * When the {@link Consumer} completes, {@link List<SimpleStruct>.Builder#build()} is called immediately and its
         * result is passed to {@link #listOfStructs(List<SimpleStruct>)}.
         *
         * @param listOfStructs
         *        a consumer that will call methods on {@link List<SimpleStruct>.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #listOfStructs(List<SimpleStruct>)
         */
        Builder listOfStructs(Consumer<SimpleStruct.Builder>... listOfStructs);

        /**
         * Sets the value of the MapOfStringToIntegerList property for this object.
         *
         * @param mapOfStringToIntegerList
         *        The new value for the MapOfStringToIntegerList property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList);

        /**
         * Sets the value of the MapOfStringToString property for this object.
         *
         * @param mapOfStringToString
         *        The new value for the MapOfStringToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToString(Map<String, String> mapOfStringToString);

        /**
         * Sets the value of the MapOfStringToSimpleStruct property for this object.
         *
         * @param mapOfStringToSimpleStruct
         *        The new value for the MapOfStringToSimpleStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToSimpleStruct(Map<String, SimpleStruct> mapOfStringToSimpleStruct);

        /**
         * Sets the value of the MapOfEnumToEnum property for this object.
         *
         * @param mapOfEnumToEnum
         *        The new value for the MapOfEnumToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToEnum(Map<String, String> mapOfEnumToEnum);

        /**
         * Sets the value of the MapOfEnumToString property for this object.
         *
         * @param mapOfEnumToString
         *        The new value for the MapOfEnumToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToString(Map<String, String> mapOfEnumToString);

        /**
         * Sets the value of the MapOfStringToEnum property for this object.
         *
         * @param mapOfStringToEnum
         *        The new value for the MapOfStringToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToEnum(Map<String, String> mapOfStringToEnum);

        /**
         * Sets the value of the MapOfEnumToSimpleStruct property for this object.
         *
         * @param mapOfEnumToSimpleStruct
         *        The new value for the MapOfEnumToSimpleStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToSimpleStruct(Map<String, SimpleStruct> mapOfEnumToSimpleStruct);

        /**
         * Sets the value of the TimestampMember property for this object.
         *
         * @param timestampMember
         *        The new value for the TimestampMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder timestampMember(Instant timestampMember);

        /**
         * Sets the value of the StructWithNestedTimestampMember property for this object.
         *
         * @param structWithNestedTimestampMember
         *        The new value for the StructWithNestedTimestampMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember);

        /**
         * Sets the value of the StructWithNestedTimestampMember property for this object.
         *
         * This is a convenience that creates an instance of the {@link StructWithTimestamp.Builder} avoiding the need
         * to create one manually via {@link StructWithTimestamp#builder()}.
         *
         * When the {@link Consumer} completes, {@link StructWithTimestamp.Builder#build()} is called immediately and
         * its result is passed to {@link #structWithNestedTimestampMember(StructWithTimestamp)}.
         *
         * @param structWithNestedTimestampMember
         *        a consumer that will call methods on {@link StructWithTimestamp.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #structWithNestedTimestampMember(StructWithTimestamp)
         */
        default Builder structWithNestedTimestampMember(Consumer<StructWithTimestamp.Builder> structWithNestedTimestampMember) {
            return structWithNestedTimestampMember(StructWithTimestamp.builder().apply(structWithNestedTimestampMember).build());
        }

        /**
         * Sets the value of the BlobArg property for this object.
         * <p>
         * To preserve immutability, the remaining bytes in the provided buffer will be copied into a new buffer when
         * set.
         * </p>
         *
         * @param blobArg
         *        The new value for the BlobArg property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobArg(ByteBuffer blobArg);

        Builder blobArg(String blobArg);

        /**
         * Sets the value of the StructWithNestedBlob property for this object.
         *
         * @param structWithNestedBlob
         *        The new value for the StructWithNestedBlob property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob);

        /**
         * Sets the value of the StructWithNestedBlob property for this object.
         *
         * This is a convenience that creates an instance of the {@link StructWithNestedBlobType.Builder} avoiding the
         * need to create one manually via {@link StructWithNestedBlobType#builder()}.
         *
         * When the {@link Consumer} completes, {@link StructWithNestedBlobType.Builder#build()} is called immediately
         * and its result is passed to {@link #structWithNestedBlob(StructWithNestedBlobType)}.
         *
         * @param structWithNestedBlob
         *        a consumer that will call methods on {@link StructWithNestedBlobType.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #structWithNestedBlob(StructWithNestedBlobType)
         */
        default Builder structWithNestedBlob(Consumer<StructWithNestedBlobType.Builder> structWithNestedBlob) {
            return structWithNestedBlob(StructWithNestedBlobType.builder().apply(structWithNestedBlob).build());
        }

        /**
         * Sets the value of the BlobMap property for this object.
         *
         * @param blobMap
         *        The new value for the BlobMap property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobMap(Map<String, ByteBuffer> blobMap);

        /**
         * Sets the value of the ListOfBlobs property for this object.
         *
         * @param listOfBlobs
         *        The new value for the ListOfBlobs property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(Collection<ByteBuffer> listOfBlobs);

        /**
         * Sets the value of the ListOfBlobs property for this object.
         *
         * @param listOfBlobs
         *        The new value for the ListOfBlobs property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(ByteBuffer... listOfBlobs);

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
        default Builder recursiveStruct(Consumer<RecursiveStructType.Builder> recursiveStruct) {
            return recursiveStruct(RecursiveStructType.builder().apply(recursiveStruct).build());
        }

        /**
         * Sets the value of the PolymorphicTypeWithSubTypes property for this object.
         *
         * @param polymorphicTypeWithSubTypes
         *        The new value for the PolymorphicTypeWithSubTypes property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes);

        /**
         * Sets the value of the PolymorphicTypeWithSubTypes property for this object.
         *
         * This is a convenience that creates an instance of the {@link BaseType.Builder} avoiding the need to create
         * one manually via {@link BaseType#builder()}.
         *
         * When the {@link Consumer} completes, {@link BaseType.Builder#build()} is called immediately and its result is
         * passed to {@link #polymorphicTypeWithSubTypes(BaseType)}.
         *
         * @param polymorphicTypeWithSubTypes
         *        a consumer that will call methods on {@link BaseType.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #polymorphicTypeWithSubTypes(BaseType)
         */
        default Builder polymorphicTypeWithSubTypes(Consumer<BaseType.Builder> polymorphicTypeWithSubTypes) {
            return polymorphicTypeWithSubTypes(BaseType.builder().apply(polymorphicTypeWithSubTypes).build());
        }

        /**
         * Sets the value of the PolymorphicTypeWithoutSubTypes property for this object.
         *
         * @param polymorphicTypeWithoutSubTypes
         *        The new value for the PolymorphicTypeWithoutSubTypes property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes);

        /**
         * Sets the value of the PolymorphicTypeWithoutSubTypes property for this object.
         *
         * This is a convenience that creates an instance of the {@link SubTypeOne.Builder} avoiding the need to create
         * one manually via {@link SubTypeOne#builder()}.
         *
         * When the {@link Consumer} completes, {@link SubTypeOne.Builder#build()} is called immediately and its result
         * is passed to {@link #polymorphicTypeWithoutSubTypes(SubTypeOne)}.
         *
         * @param polymorphicTypeWithoutSubTypes
         *        a consumer that will call methods on {@link SubTypeOne.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #polymorphicTypeWithoutSubTypes(SubTypeOne)
         */
        default Builder polymorphicTypeWithoutSubTypes(Consumer<SubTypeOne.Builder> polymorphicTypeWithoutSubTypes) {
            return polymorphicTypeWithoutSubTypes(SubTypeOne.builder().apply(polymorphicTypeWithoutSubTypes).build());
        }

        /**
         * Sets the value of the EnumType property for this object.
         *
         * @param enumType
         *        The new value for the EnumType property for this object.
         * @see EnumType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see EnumType
         */
        Builder enumType(String enumType);

        /**
         * Sets the value of the EnumType property for this object.
         *
         * @param enumType
         *        The new value for the EnumType property for this object.
         * @see EnumType
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see EnumType
         */
        Builder enumType(EnumType enumType);
    }

    static final class BuilderImpl extends JsonProtocolTestsResponse.BuilderImpl implements Builder {
        private String stringMember;

        private Integer integerMember;

        private Boolean booleanMember;

        private Float floatMember;

        private Double doubleMember;

        private Long longMember;

        private List<String> simpleList = DefaultSdkAutoConstructList.getInstance();

        private List<String> listOfEnums = DefaultSdkAutoConstructList.getInstance();

        private List<Map<String, String>> listOfMaps = DefaultSdkAutoConstructList.getInstance();

        private List<SimpleStruct> listOfStructs = DefaultSdkAutoConstructList.getInstance();

        private Map<String, List<Integer>> mapOfStringToIntegerList;

        private Map<String, String> mapOfStringToString;

        private Map<String, SimpleStruct> mapOfStringToSimpleStruct;

        private Map<String, String> mapOfEnumToEnum;

        private Map<String, String> mapOfEnumToString;

        private Map<String, String> mapOfStringToEnum;

        private Map<String, SimpleStruct> mapOfEnumToSimpleStruct;

        private Instant timestampMember;

        private StructWithTimestamp structWithNestedTimestampMember;

        private ByteBuffer blobArg;

        private StructWithNestedBlobType structWithNestedBlob;

        private Map<String, ByteBuffer> blobMap;

        private List<ByteBuffer> listOfBlobs = DefaultSdkAutoConstructList.getInstance();

        private RecursiveStructType recursiveStruct;

        private BaseType polymorphicTypeWithSubTypes;

        private SubTypeOne polymorphicTypeWithoutSubTypes;

        private String enumType;

        private BuilderImpl() {
        }

        private BuilderImpl(AllTypesResponse model) {
            super(model);
            stringMember(model.stringMember);
            integerMember(model.integerMember);
            booleanMember(model.booleanMember);
            floatMember(model.floatMember);
            doubleMember(model.doubleMember);
            longMember(model.longMember);
            simpleList(model.simpleList);
            listOfEnums(model.listOfEnums);
            listOfMaps(model.listOfMaps);
            listOfStructs(model.listOfStructs);
            mapOfStringToIntegerList(model.mapOfStringToIntegerList);
            mapOfStringToString(model.mapOfStringToString);
            mapOfStringToSimpleStruct(model.mapOfStringToSimpleStruct);
            mapOfEnumToEnum(model.mapOfEnumToEnum);
            mapOfEnumToString(model.mapOfEnumToString);
            mapOfStringToEnum(model.mapOfStringToEnum);
            mapOfEnumToSimpleStruct(model.mapOfEnumToSimpleStruct);
            timestampMember(model.timestampMember);
            structWithNestedTimestampMember(model.structWithNestedTimestampMember);
            blobArg(model.blobArg);
            structWithNestedBlob(model.structWithNestedBlob);
            blobMap(model.blobMap);
            listOfBlobs(model.listOfBlobs);
            recursiveStruct(model.recursiveStruct);
            polymorphicTypeWithSubTypes(model.polymorphicTypeWithSubTypes);
            polymorphicTypeWithoutSubTypes(model.polymorphicTypeWithoutSubTypes);
            enumType(model.enumType);
        }

        public final String getStringMember() {
            return stringMember;
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        public final Integer getIntegerMember() {
            return integerMember;
        }

        @Override
        public final Builder integerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        public final void setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
        }

        public final Boolean getBooleanMember() {
            return booleanMember;
        }

        @Override
        public final Builder booleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
            return this;
        }

        public final void setBooleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
        }

        public final Float getFloatMember() {
            return floatMember;
        }

        @Override
        public final Builder floatMember(Float floatMember) {
            this.floatMember = floatMember;
            return this;
        }

        public final void setFloatMember(Float floatMember) {
            this.floatMember = floatMember;
        }

        public final Double getDoubleMember() {
            return doubleMember;
        }

        @Override
        public final Builder doubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
            return this;
        }

        public final void setDoubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
        }

        public final Long getLongMember() {
            return longMember;
        }

        @Override
        public final Builder longMember(Long longMember) {
            this.longMember = longMember;
            return this;
        }

        public final void setLongMember(Long longMember) {
            this.longMember = longMember;
        }

        public final Collection<String> getSimpleList() {
            return simpleList;
        }

        @Override
        public final Builder simpleList(Collection<String> simpleList) {
            this.simpleList = ListOfStringsCopier.copy(simpleList);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder simpleList(String... simpleList) {
            simpleList(Arrays.asList(simpleList));
            return this;
        }

        public final void setSimpleList(Collection<String> simpleList) {
            this.simpleList = ListOfStringsCopier.copy(simpleList);
        }

        public final Collection<String> getListOfEnums() {
            return listOfEnums;
        }

        @Override
        public final Builder listOfEnums(Collection<String> listOfEnums) {
            this.listOfEnums = ListOfEnumsCopier.copy(listOfEnums);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfEnums(String... listOfEnums) {
            listOfEnums(Arrays.asList(listOfEnums));
            return this;
        }

        public final void setListOfEnums(Collection<String> listOfEnums) {
            this.listOfEnums = ListOfEnumsCopier.copy(listOfEnums);
        }

        public final Collection<Map<String, String>> getListOfMaps() {
            return listOfMaps;
        }

        @Override
        public final Builder listOfMaps(Collection<Map<String, String>> listOfMaps) {
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfMaps(Map<String, String>... listOfMaps) {
            listOfMaps(Arrays.asList(listOfMaps));
            return this;
        }

        public final void setListOfMaps(Collection<Map<String, String>> listOfMaps) {
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
        }

        public final Collection<SimpleStruct.Builder> getListOfStructs() {
            return listOfStructs != null ? listOfStructs.stream().map(SimpleStruct::toBuilder).collect(Collectors.toList())
                    : null;
        }

        @Override
        public final Builder listOfStructs(Collection<SimpleStruct> listOfStructs) {
            this.listOfStructs = ListOfSimpleStructsCopier.copy(listOfStructs);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfStructs(SimpleStruct... listOfStructs) {
            listOfStructs(Arrays.asList(listOfStructs));
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfStructs(Consumer<SimpleStruct.Builder>... listOfStructs) {
            listOfStructs(Stream.of(listOfStructs).map(c -> SimpleStruct.builder().apply(c).build()).collect(Collectors.toList()));
            return this;
        }

        public final void setListOfStructs(Collection<SimpleStruct.BuilderImpl> listOfStructs) {
            this.listOfStructs = ListOfSimpleStructsCopier.copyFromBuilder(listOfStructs);
        }

        public final Map<String, ? extends Collection<Integer>> getMapOfStringToIntegerList() {
            return mapOfStringToIntegerList;
        }

        @Override
        public final Builder mapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
            return this;
        }

        public final void setMapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
        }

        public final Map<String, String> getMapOfStringToString() {
            return mapOfStringToString;
        }

        @Override
        public final Builder mapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
            return this;
        }

        public final void setMapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
        }

        public final Map<String, SimpleStruct.Builder> getMapOfStringToSimpleStruct() {
            return mapOfStringToSimpleStruct != null ? CollectionUtils.mapValues(mapOfStringToSimpleStruct,
                    SimpleStruct::toBuilder) : null;
        }

        @Override
        public final Builder mapOfStringToSimpleStruct(Map<String, SimpleStruct> mapOfStringToSimpleStruct) {
            this.mapOfStringToSimpleStruct = MapOfStringToSimpleStructCopier.copy(mapOfStringToSimpleStruct);
            return this;
        }

        public final void setMapOfStringToSimpleStruct(Map<String, SimpleStruct.BuilderImpl> mapOfStringToSimpleStruct) {
            this.mapOfStringToSimpleStruct = MapOfStringToSimpleStructCopier.copyFromBuilder(mapOfStringToSimpleStruct);
        }

        public final Map<String, String> getMapOfEnumToEnum() {
            return mapOfEnumToEnum;
        }

        @Override
        public final Builder mapOfEnumToEnum(Map<String, String> mapOfEnumToEnum) {
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copy(mapOfEnumToEnum);
            return this;
        }

        public final void setMapOfEnumToEnum(Map<String, String> mapOfEnumToEnum) {
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copy(mapOfEnumToEnum);
        }

        public final Map<String, String> getMapOfEnumToString() {
            return mapOfEnumToString;
        }

        @Override
        public final Builder mapOfEnumToString(Map<String, String> mapOfEnumToString) {
            this.mapOfEnumToString = MapOfEnumToStringCopier.copy(mapOfEnumToString);
            return this;
        }

        public final void setMapOfEnumToString(Map<String, String> mapOfEnumToString) {
            this.mapOfEnumToString = MapOfEnumToStringCopier.copy(mapOfEnumToString);
        }

        public final Map<String, String> getMapOfStringToEnum() {
            return mapOfStringToEnum;
        }

        @Override
        public final Builder mapOfStringToEnum(Map<String, String> mapOfStringToEnum) {
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copy(mapOfStringToEnum);
            return this;
        }

        public final void setMapOfStringToEnum(Map<String, String> mapOfStringToEnum) {
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copy(mapOfStringToEnum);
        }

        public final Map<String, SimpleStruct.Builder> getMapOfEnumToSimpleStruct() {
            return mapOfEnumToSimpleStruct != null ? CollectionUtils.mapValues(mapOfEnumToSimpleStruct, SimpleStruct::toBuilder)
                    : null;
        }

        @Override
        public final Builder mapOfEnumToSimpleStruct(Map<String, SimpleStruct> mapOfEnumToSimpleStruct) {
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copy(mapOfEnumToSimpleStruct);
            return this;
        }

        public final void setMapOfEnumToSimpleStruct(Map<String, SimpleStruct.BuilderImpl> mapOfEnumToSimpleStruct) {
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copyFromBuilder(mapOfEnumToSimpleStruct);
        }

        public final Instant getTimestampMember() {
            return timestampMember;
        }

        @Override
        public final Builder timestampMember(Instant timestampMember) {
            this.timestampMember = timestampMember;
            return this;
        }

        public final void setTimestampMember(Instant timestampMember) {
            this.timestampMember = timestampMember;
        }

        public final StructWithTimestamp.Builder getStructWithNestedTimestampMember() {
            return structWithNestedTimestampMember != null ? structWithNestedTimestampMember.toBuilder() : null;
        }

        @Override
        public final Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
            return this;
        }

        public final void setStructWithNestedTimestampMember(StructWithTimestamp.BuilderImpl structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember != null ? structWithNestedTimestampMember
                    .build() : null;
        }

        public final ByteBuffer getBlobArg() {
            return blobArg;
        }

        @Override
        public final Builder blobArg(ByteBuffer blobArg) {
            this.blobArg = StandardMemberCopier.copy(blobArg);
            return this;
        }

        public final void setBlobArg(ByteBuffer blobArg) {
            this.blobArg = StandardMemberCopier.copy(blobArg);
        }

        public Builder blobArg(String blobArg) {
            blobArg(new StringToByteBufferAdapter().adapt(blobArg));
            return this;
        }

        public void setBlobArg(String blobArg) {
            this.blobArg = new StringToByteBufferAdapter().adapt(blobArg);
        }

        public final StructWithNestedBlobType.Builder getStructWithNestedBlob() {
            return structWithNestedBlob != null ? structWithNestedBlob.toBuilder() : null;
        }

        @Override
        public final Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
            return this;
        }

        public final void setStructWithNestedBlob(StructWithNestedBlobType.BuilderImpl structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob != null ? structWithNestedBlob.build() : null;
        }

        public final Map<String, ByteBuffer> getBlobMap() {
            return blobMap;
        }

        @Override
        public final Builder blobMap(Map<String, ByteBuffer> blobMap) {
            this.blobMap = BlobMapTypeCopier.copy(blobMap);
            return this;
        }

        public final void setBlobMap(Map<String, ByteBuffer> blobMap) {
            this.blobMap = BlobMapTypeCopier.copy(blobMap);
        }

        public final Collection<ByteBuffer> getListOfBlobs() {
            return listOfBlobs;
        }

        @Override
        public final Builder listOfBlobs(Collection<ByteBuffer> listOfBlobs) {
            this.listOfBlobs = ListOfBlobsTypeCopier.copy(listOfBlobs);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfBlobs(ByteBuffer... listOfBlobs) {
            listOfBlobs(Arrays.asList(listOfBlobs));
            return this;
        }

        public final void setListOfBlobs(Collection<ByteBuffer> listOfBlobs) {
            this.listOfBlobs = ListOfBlobsTypeCopier.copy(listOfBlobs);
        }

        public final RecursiveStructType.Builder getRecursiveStruct() {
            return recursiveStruct != null ? recursiveStruct.toBuilder() : null;
        }

        @Override
        public final Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
            return this;
        }

        public final void setRecursiveStruct(RecursiveStructType.BuilderImpl recursiveStruct) {
            this.recursiveStruct = recursiveStruct != null ? recursiveStruct.build() : null;
        }

        public final BaseType.Builder getPolymorphicTypeWithSubTypes() {
            return polymorphicTypeWithSubTypes != null ? polymorphicTypeWithSubTypes.toBuilder() : null;
        }

        @Override
        public final Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
            return this;
        }

        public final void setPolymorphicTypeWithSubTypes(BaseType.BuilderImpl polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes != null ? polymorphicTypeWithSubTypes.build() : null;
        }

        public final SubTypeOne.Builder getPolymorphicTypeWithoutSubTypes() {
            return polymorphicTypeWithoutSubTypes != null ? polymorphicTypeWithoutSubTypes.toBuilder() : null;
        }

        @Override
        public final Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
            return this;
        }

        public final void setPolymorphicTypeWithoutSubTypes(SubTypeOne.BuilderImpl polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes != null ? polymorphicTypeWithoutSubTypes.build()
                    : null;
        }

        public final String getEnumType() {
            return enumType;
        }

        @Override
        public final Builder enumType(String enumType) {
            this.enumType = enumType;
            return this;
        }

        @Override
        public final Builder enumType(EnumType enumType) {
            this.enumType(enumType.toString());
            return this;
        }

        public final void setEnumType(String enumType) {
            this.enumType = enumType;
        }

        @Override
        public AllTypesResponse build() {
            return new AllTypesResponse(this);
        }
    }
}
