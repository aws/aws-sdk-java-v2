package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import software.amazon.awssdk.core.AmazonWebServiceRequest;
import software.amazon.awssdk.core.runtime.StandardMemberCopier;
import software.amazon.awssdk.core.runtime.TypeConverter;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 */
@Generated("software.amazon.awssdk:codegen")
public class AllTypesRequest extends AmazonWebServiceRequest implements
                                                             ToCopyableBuilder<AllTypesRequest.Builder, AllTypesRequest> {
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

    private AllTypesRequest(BuilderImpl builder) {
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
    public List<String> listOfEnumsStrings() {
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
    public Map<String, String> mapOfEnumToEnumStrings() {
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
    public Map<String, String> mapOfEnumToStringStrings() {
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
    public Map<String, String> mapOfStringToEnumStrings() {
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
    public Map<String, SimpleStruct> mapOfEnumToSimpleStructStrings() {
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
     * return {@link EnumType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from {@link #enumTypeString}.
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
     * return {@link EnumType#UNKNOWN_TO_SDK_VERSION}. The raw value returned by the service is available from {@link #enumTypeString}.
     * </p>
     *
     * @return The value of the EnumType property for this object.
     * @see EnumType
     */
    public String enumTypeString() {
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
        hashCode = 31 * hashCode + ((stringMember() == null) ? 0 : stringMember().hashCode());
        hashCode = 31 * hashCode + ((integerMember() == null) ? 0 : integerMember().hashCode());
        hashCode = 31 * hashCode + ((booleanMember() == null) ? 0 : booleanMember().hashCode());
        hashCode = 31 * hashCode + ((floatMember() == null) ? 0 : floatMember().hashCode());
        hashCode = 31 * hashCode + ((doubleMember() == null) ? 0 : doubleMember().hashCode());
        hashCode = 31 * hashCode + ((longMember() == null) ? 0 : longMember().hashCode());
        hashCode = 31 * hashCode + ((simpleList() == null) ? 0 : simpleList().hashCode());
        hashCode = 31 * hashCode + ((listOfEnumsStrings() == null) ? 0 : listOfEnumsStrings().hashCode());
        hashCode = 31 * hashCode + ((listOfMaps() == null) ? 0 : listOfMaps().hashCode());
        hashCode = 31 * hashCode + ((listOfStructs() == null) ? 0 : listOfStructs().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToIntegerList() == null) ? 0 : mapOfStringToIntegerList().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToString() == null) ? 0 : mapOfStringToString().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToSimpleStruct() == null) ? 0 : mapOfStringToSimpleStruct().hashCode());
        hashCode = 31 * hashCode + ((mapOfEnumToEnumStrings() == null) ? 0 : mapOfEnumToEnumStrings().hashCode());
        hashCode = 31 * hashCode + ((mapOfEnumToStringStrings() == null) ? 0 : mapOfEnumToStringStrings().hashCode());
        hashCode = 31 * hashCode + ((mapOfStringToEnumStrings() == null) ? 0 : mapOfStringToEnumStrings().hashCode());
        hashCode = 31 * hashCode + ((mapOfEnumToSimpleStructStrings() == null) ? 0 : mapOfEnumToSimpleStructStrings().hashCode());
        hashCode = 31 * hashCode + ((timestampMember() == null) ? 0 : timestampMember().hashCode());
        hashCode = 31 * hashCode
                   + ((structWithNestedTimestampMember() == null) ? 0 : structWithNestedTimestampMember().hashCode());
        hashCode = 31 * hashCode + ((blobArg() == null) ? 0 : blobArg().hashCode());
        hashCode = 31 * hashCode + ((structWithNestedBlob() == null) ? 0 : structWithNestedBlob().hashCode());
        hashCode = 31 * hashCode + ((blobMap() == null) ? 0 : blobMap().hashCode());
        hashCode = 31 * hashCode + ((listOfBlobs() == null) ? 0 : listOfBlobs().hashCode());
        hashCode = 31 * hashCode + ((recursiveStruct() == null) ? 0 : recursiveStruct().hashCode());
        hashCode = 31 * hashCode + ((polymorphicTypeWithSubTypes() == null) ? 0 : polymorphicTypeWithSubTypes().hashCode());
        hashCode = 31 * hashCode + ((polymorphicTypeWithoutSubTypes() == null) ? 0 : polymorphicTypeWithoutSubTypes().hashCode());
        hashCode = 31 * hashCode + ((enumTypeString() == null) ? 0 : enumTypeString().hashCode());
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
        if (!(obj instanceof AllTypesRequest)) {
            return false;
        }
        AllTypesRequest other = (AllTypesRequest) obj;
        if (other.stringMember() == null ^ this.stringMember() == null) {
            return false;
        }
        if (other.stringMember() != null && !other.stringMember().equals(this.stringMember())) {
            return false;
        }
        if (other.integerMember() == null ^ this.integerMember() == null) {
            return false;
        }
        if (other.integerMember() != null && !other.integerMember().equals(this.integerMember())) {
            return false;
        }
        if (other.booleanMember() == null ^ this.booleanMember() == null) {
            return false;
        }
        if (other.booleanMember() != null && !other.booleanMember().equals(this.booleanMember())) {
            return false;
        }
        if (other.floatMember() == null ^ this.floatMember() == null) {
            return false;
        }
        if (other.floatMember() != null && !other.floatMember().equals(this.floatMember())) {
            return false;
        }
        if (other.doubleMember() == null ^ this.doubleMember() == null) {
            return false;
        }
        if (other.doubleMember() != null && !other.doubleMember().equals(this.doubleMember())) {
            return false;
        }
        if (other.longMember() == null ^ this.longMember() == null) {
            return false;
        }
        if (other.longMember() != null && !other.longMember().equals(this.longMember())) {
            return false;
        }
        if (other.simpleList() == null ^ this.simpleList() == null) {
            return false;
        }
        if (other.simpleList() != null && !other.simpleList().equals(this.simpleList())) {
            return false;
        }
        if (other.listOfEnumsStrings() == null ^ this.listOfEnumsStrings() == null) {
            return false;
        }
        if (other.listOfEnumsStrings() != null && !other.listOfEnumsStrings().equals(this.listOfEnumsStrings())) {
            return false;
        }
        if (other.listOfMaps() == null ^ this.listOfMaps() == null) {
            return false;
        }
        if (other.listOfMaps() != null && !other.listOfMaps().equals(this.listOfMaps())) {
            return false;
        }
        if (other.listOfStructs() == null ^ this.listOfStructs() == null) {
            return false;
        }
        if (other.listOfStructs() != null && !other.listOfStructs().equals(this.listOfStructs())) {
            return false;
        }
        if (other.mapOfStringToIntegerList() == null ^ this.mapOfStringToIntegerList() == null) {
            return false;
        }
        if (other.mapOfStringToIntegerList() != null && !other.mapOfStringToIntegerList().equals(this.mapOfStringToIntegerList())) {
            return false;
        }
        if (other.mapOfStringToString() == null ^ this.mapOfStringToString() == null) {
            return false;
        }
        if (other.mapOfStringToString() != null && !other.mapOfStringToString().equals(this.mapOfStringToString())) {
            return false;
        }
        if (other.mapOfStringToSimpleStruct() == null ^ this.mapOfStringToSimpleStruct() == null) {
            return false;
        }
        if (other.mapOfStringToSimpleStruct() != null
            && !other.mapOfStringToSimpleStruct().equals(this.mapOfStringToSimpleStruct())) {
            return false;
        }
        if (other.mapOfEnumToEnumStrings() == null ^ this.mapOfEnumToEnumStrings() == null) {
            return false;
        }
        if (other.mapOfEnumToEnumStrings() != null && !other.mapOfEnumToEnumStrings().equals(this.mapOfEnumToEnumStrings())) {
            return false;
        }
        if (other.mapOfEnumToStringStrings() == null ^ this.mapOfEnumToStringStrings() == null) {
            return false;
        }
        if (other.mapOfEnumToStringStrings() != null && !other.mapOfEnumToStringStrings().equals(this.mapOfEnumToStringStrings())) {
            return false;
        }
        if (other.mapOfStringToEnumStrings() == null ^ this.mapOfStringToEnumStrings() == null) {
            return false;
        }
        if (other.mapOfStringToEnumStrings() != null && !other.mapOfStringToEnumStrings().equals(this.mapOfStringToEnumStrings())) {
            return false;
        }
        if (other.mapOfEnumToSimpleStructStrings() == null ^ this.mapOfEnumToSimpleStructStrings() == null) {
            return false;
        }
        if (other.mapOfEnumToSimpleStructStrings() != null
            && !other.mapOfEnumToSimpleStructStrings().equals(this.mapOfEnumToSimpleStructStrings())) {
            return false;
        }
        if (other.timestampMember() == null ^ this.timestampMember() == null) {
            return false;
        }
        if (other.timestampMember() != null && !other.timestampMember().equals(this.timestampMember())) {
            return false;
        }
        if (other.structWithNestedTimestampMember() == null ^ this.structWithNestedTimestampMember() == null) {
            return false;
        }
        if (other.structWithNestedTimestampMember() != null
            && !other.structWithNestedTimestampMember().equals(this.structWithNestedTimestampMember())) {
            return false;
        }
        if (other.blobArg() == null ^ this.blobArg() == null) {
            return false;
        }
        if (other.blobArg() != null && !other.blobArg().equals(this.blobArg())) {
            return false;
        }
        if (other.structWithNestedBlob() == null ^ this.structWithNestedBlob() == null) {
            return false;
        }
        if (other.structWithNestedBlob() != null && !other.structWithNestedBlob().equals(this.structWithNestedBlob())) {
            return false;
        }
        if (other.blobMap() == null ^ this.blobMap() == null) {
            return false;
        }
        if (other.blobMap() != null && !other.blobMap().equals(this.blobMap())) {
            return false;
        }
        if (other.listOfBlobs() == null ^ this.listOfBlobs() == null) {
            return false;
        }
        if (other.listOfBlobs() != null && !other.listOfBlobs().equals(this.listOfBlobs())) {
            return false;
        }
        if (other.recursiveStruct() == null ^ this.recursiveStruct() == null) {
            return false;
        }
        if (other.recursiveStruct() != null && !other.recursiveStruct().equals(this.recursiveStruct())) {
            return false;
        }
        if (other.polymorphicTypeWithSubTypes() == null ^ this.polymorphicTypeWithSubTypes() == null) {
            return false;
        }
        if (other.polymorphicTypeWithSubTypes() != null
            && !other.polymorphicTypeWithSubTypes().equals(this.polymorphicTypeWithSubTypes())) {
            return false;
        }
        if (other.polymorphicTypeWithoutSubTypes() == null ^ this.polymorphicTypeWithoutSubTypes() == null) {
            return false;
        }
        if (other.polymorphicTypeWithoutSubTypes() != null
            && !other.polymorphicTypeWithoutSubTypes().equals(this.polymorphicTypeWithoutSubTypes())) {
            return false;
        }
        if (other.enumTypeString() == null ^ this.enumTypeString() == null) {
            return false;
        }
        if (other.enumTypeString() != null && !other.enumTypeString().equals(this.enumTypeString())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (stringMember() != null) {
            sb.append("StringMember: ").append(stringMember()).append(",");
        }
        if (integerMember() != null) {
            sb.append("IntegerMember: ").append(integerMember()).append(",");
        }
        if (booleanMember() != null) {
            sb.append("BooleanMember: ").append(booleanMember()).append(",");
        }
        if (floatMember() != null) {
            sb.append("FloatMember: ").append(floatMember()).append(",");
        }
        if (doubleMember() != null) {
            sb.append("DoubleMember: ").append(doubleMember()).append(",");
        }
        if (longMember() != null) {
            sb.append("LongMember: ").append(longMember()).append(",");
        }
        if (simpleList() != null) {
            sb.append("SimpleList: ").append(simpleList()).append(",");
        }
        if (listOfEnumsStrings() != null) {
            sb.append("ListOfEnums: ").append(listOfEnumsStrings()).append(",");
        }
        if (listOfMaps() != null) {
            sb.append("ListOfMaps: ").append(listOfMaps()).append(",");
        }
        if (listOfStructs() != null) {
            sb.append("ListOfStructs: ").append(listOfStructs()).append(",");
        }
        if (mapOfStringToIntegerList() != null) {
            sb.append("MapOfStringToIntegerList: ").append(mapOfStringToIntegerList()).append(",");
        }
        if (mapOfStringToString() != null) {
            sb.append("MapOfStringToString: ").append(mapOfStringToString()).append(",");
        }
        if (mapOfStringToSimpleStruct() != null) {
            sb.append("MapOfStringToSimpleStruct: ").append(mapOfStringToSimpleStruct()).append(",");
        }
        if (mapOfEnumToEnumStrings() != null) {
            sb.append("MapOfEnumToEnum: ").append(mapOfEnumToEnumStrings()).append(",");
        }
        if (mapOfEnumToStringStrings() != null) {
            sb.append("MapOfEnumToString: ").append(mapOfEnumToStringStrings()).append(",");
        }
        if (mapOfStringToEnumStrings() != null) {
            sb.append("MapOfStringToEnum: ").append(mapOfStringToEnumStrings()).append(",");
        }
        if (mapOfEnumToSimpleStructStrings() != null) {
            sb.append("MapOfEnumToSimpleStruct: ").append(mapOfEnumToSimpleStructStrings()).append(",");
        }
        if (timestampMember() != null) {
            sb.append("TimestampMember: ").append(timestampMember()).append(",");
        }
        if (structWithNestedTimestampMember() != null) {
            sb.append("StructWithNestedTimestampMember: ").append(structWithNestedTimestampMember()).append(",");
        }
        if (blobArg() != null) {
            sb.append("BlobArg: ").append(blobArg()).append(",");
        }
        if (structWithNestedBlob() != null) {
            sb.append("StructWithNestedBlob: ").append(structWithNestedBlob()).append(",");
        }
        if (blobMap() != null) {
            sb.append("BlobMap: ").append(blobMap()).append(",");
        }
        if (listOfBlobs() != null) {
            sb.append("ListOfBlobs: ").append(listOfBlobs()).append(",");
        }
        if (recursiveStruct() != null) {
            sb.append("RecursiveStruct: ").append(recursiveStruct()).append(",");
        }
        if (polymorphicTypeWithSubTypes() != null) {
            sb.append("PolymorphicTypeWithSubTypes: ").append(polymorphicTypeWithSubTypes()).append(",");
        }
        if (polymorphicTypeWithoutSubTypes() != null) {
            sb.append("PolymorphicTypeWithoutSubTypes: ").append(polymorphicTypeWithoutSubTypes()).append(",");
        }
        if (enumTypeString() != null) {
            sb.append("EnumType: ").append(enumTypeString()).append(",");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
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
                return Optional.of(clazz.cast(listOfEnumsStrings()));
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
                return Optional.of(clazz.cast(mapOfEnumToEnumStrings()));
            case "MapOfEnumToString":
                return Optional.of(clazz.cast(mapOfEnumToStringStrings()));
            case "MapOfStringToEnum":
                return Optional.of(clazz.cast(mapOfStringToEnumStrings()));
            case "MapOfEnumToSimpleStruct":
                return Optional.of(clazz.cast(mapOfEnumToSimpleStructStrings()));
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
                return Optional.of(clazz.cast(enumTypeString()));
            default:
                return Optional.empty();
        }
    }

    public interface Builder extends CopyableBuilder<Builder, AllTypesRequest> {
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

        /**
         * Sets the value of the StructWithNestedBlob property for this object.
         *
         * @param structWithNestedBlob
         *        The new value for the StructWithNestedBlob property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob);

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
         * Sets the value of the PolymorphicTypeWithSubTypes property for this object.
         *
         * @param polymorphicTypeWithSubTypes
         *        The new value for the PolymorphicTypeWithSubTypes property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes);

        /**
         * Sets the value of the PolymorphicTypeWithoutSubTypes property for this object.
         *
         * @param polymorphicTypeWithoutSubTypes
         *        The new value for the PolymorphicTypeWithoutSubTypes property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes);

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

    static final class BuilderImpl implements Builder {
        private String stringMember;

        private Integer integerMember;

        private Boolean booleanMember;

        private Float floatMember;

        private Double doubleMember;

        private Long longMember;

        private List<String> simpleList;

        private List<String> listOfEnums;

        private List<Map<String, String>> listOfMaps;

        private List<SimpleStruct> listOfStructs;

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

        private List<ByteBuffer> listOfBlobs;

        private RecursiveStructType recursiveStruct;

        private BaseType polymorphicTypeWithSubTypes;

        private SubTypeOne polymorphicTypeWithoutSubTypes;

        private String enumType;

        private BuilderImpl() {
        }

        private BuilderImpl(AllTypesRequest model) {
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
        public AllTypesRequest build() {
            return new AllTypesRequest(this);
        }
    }
}
