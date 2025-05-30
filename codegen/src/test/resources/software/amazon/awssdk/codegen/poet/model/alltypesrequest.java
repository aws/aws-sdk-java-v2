package software.amazon.awssdk.services.jsonprotocoltests.model;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.Mutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
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
public final class AllTypesRequest extends JsonProtocolTestsRequest implements
                                                                    ToCopyableBuilder<AllTypesRequest.Builder, AllTypesRequest> {
    private static final SdkField<String> STRING_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                        .memberName("StringMember").getter(getter(AllTypesRequest::stringMember)).setter(setter(Builder::stringMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StringMember").build()).build();

    private static final SdkField<Integer> INTEGER_MEMBER_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
                                                                          .memberName("IntegerMember").getter(getter(AllTypesRequest::integerMember)).setter(setter(Builder::integerMember))
                                                                          .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("IntegerMember").build()).build();

    private static final SdkField<Boolean> BOOLEAN_MEMBER_FIELD = SdkField.<Boolean> builder(MarshallingType.BOOLEAN)
                                                                          .memberName("BooleanMember").getter(getter(AllTypesRequest::booleanMember)).setter(setter(Builder::booleanMember))
                                                                          .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BooleanMember").build()).build();

    private static final SdkField<Float> FLOAT_MEMBER_FIELD = SdkField.<Float> builder(MarshallingType.FLOAT)
                                                                      .memberName("FloatMember").getter(getter(AllTypesRequest::floatMember)).setter(setter(Builder::floatMember))
                                                                      .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("FloatMember").build()).build();

    private static final SdkField<Double> DOUBLE_MEMBER_FIELD = SdkField.<Double> builder(MarshallingType.DOUBLE)
                                                                        .memberName("DoubleMember").getter(getter(AllTypesRequest::doubleMember)).setter(setter(Builder::doubleMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DoubleMember").build()).build();

    private static final SdkField<Long> LONG_MEMBER_FIELD = SdkField.<Long> builder(MarshallingType.LONG)
                                                                    .memberName("LongMember").getter(getter(AllTypesRequest::longMember)).setter(setter(Builder::longMember))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("LongMember").build()).build();

    private static final SdkField<Short> SHORT_MEMBER_FIELD = SdkField.<Short> builder(MarshallingType.SHORT)
                                                                      .memberName("ShortMember").getter(getter(AllTypesRequest::shortMember)).setter(setter(Builder::shortMember))
                                                                      .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ShortMember").build()).build();

    private static final SdkField<Byte> BYTE_MEMBER_FIELD = SdkField.<Byte> builder(MarshallingType.BYTE)
                                                                    .memberName("ByteMember").getter(getter(AllTypesRequest::byteMember)).setter(setter(Builder::byteMember))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ByteMember").build()).build();

    private static final SdkField<List<String>> SIMPLE_LIST_FIELD = SdkField
        .<List<String>> builder(MarshallingType.LIST)
        .memberName("SimpleList")
        .getter(getter(AllTypesRequest::simpleList))
        .setter(setter(Builder::simpleList))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("SimpleList").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<String> builder(MarshallingType.STRING)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<String>> LIST_OF_ENUMS_FIELD = SdkField
        .<List<String>> builder(MarshallingType.LIST)
        .memberName("ListOfEnums")
        .getter(getter(AllTypesRequest::listOfEnumsAsStrings))
        .setter(setter(Builder::listOfEnumsWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfEnums").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<String> builder(MarshallingType.STRING)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<Map<String, String>>> LIST_OF_MAPS_FIELD = SdkField
        .<List<Map<String, String>>> builder(MarshallingType.LIST)
        .memberName("ListOfMaps")
        .getter(getter(AllTypesRequest::listOfMaps))
        .setter(setter(Builder::listOfMaps))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfMaps").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<Map<String, String>> builder(MarshallingType.MAP)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build(),
                                        MapTrait.builder()
                                                .keyLocationName("key")
                                                .valueLocationName("value")
                                                .valueFieldInfo(
                                                    SdkField.<String> builder(MarshallingType.STRING)
                                                            .traits(LocationTrait.builder()
                                                                                 .location(MarshallLocation.PAYLOAD)
                                                                                 .locationName("value").build()).build())
                                                .build()).build()).build()).build();

    private static final SdkField<List<SimpleStruct>> LIST_OF_STRUCTS_FIELD = SdkField
        .<List<SimpleStruct>> builder(MarshallingType.LIST)
        .memberName("ListOfStructs")
        .getter(getter(AllTypesRequest::listOfStructs))
        .setter(setter(Builder::listOfStructs))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfStructs").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<SimpleStruct> builder(MarshallingType.SDK_POJO)
                                .constructor(SimpleStruct::builder)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build()).build()).build()).build();

    private static final SdkField<List<Map<String, String>>> LIST_OF_MAP_OF_ENUM_TO_STRING_FIELD = SdkField
        .<List<Map<String, String>>> builder(MarshallingType.LIST)
        .memberName("ListOfMapOfEnumToString")
        .getter(getter(AllTypesRequest::listOfMapOfEnumToStringAsStrings))
        .setter(setter(Builder::listOfMapOfEnumToStringWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfMapOfEnumToString").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<Map<String, String>> builder(MarshallingType.MAP)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build(),
                                        MapTrait.builder()
                                                .keyLocationName("key")
                                                .valueLocationName("value")
                                                .valueFieldInfo(
                                                    SdkField.<String> builder(MarshallingType.STRING)
                                                            .traits(LocationTrait.builder()
                                                                                 .location(MarshallLocation.PAYLOAD)
                                                                                 .locationName("value").build()).build())
                                                .build()).build()).build()).build();

    private static final SdkField<List<Map<String, SimpleStruct>>> LIST_OF_MAP_OF_STRING_TO_STRUCT_FIELD = SdkField
        .<List<Map<String, SimpleStruct>>> builder(MarshallingType.LIST)
        .memberName("ListOfMapOfStringToStruct")
        .getter(getter(AllTypesRequest::listOfMapOfStringToStruct))
        .setter(setter(Builder::listOfMapOfStringToStruct))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfMapOfStringToStruct").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<Map<String, SimpleStruct>> builder(MarshallingType.MAP)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build(),
                                        MapTrait.builder()
                                                .keyLocationName("key")
                                                .valueLocationName("value")
                                                .valueFieldInfo(
                                                    SdkField.<SimpleStruct> builder(MarshallingType.SDK_POJO)
                                                            .constructor(SimpleStruct::builder)
                                                            .traits(LocationTrait.builder()
                                                                                 .location(MarshallLocation.PAYLOAD)
                                                                                 .locationName("value").build()).build())
                                                .build()).build()).build()).build();

    private static final SdkField<Map<String, List<Integer>>> MAP_OF_STRING_TO_INTEGER_LIST_FIELD = SdkField
        .<Map<String, List<Integer>>> builder(MarshallingType.MAP)
        .memberName("MapOfStringToIntegerList")
        .getter(getter(AllTypesRequest::mapOfStringToIntegerList))
        .setter(setter(Builder::mapOfStringToIntegerList))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfStringToIntegerList").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<List<Integer>> builder(MarshallingType.LIST)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build(),
                                            ListTrait
                                                .builder()
                                                .memberLocationName(null)
                                                .memberFieldInfo(
                                                    SdkField.<Integer> builder(MarshallingType.INTEGER)
                                                            .traits(LocationTrait.builder()
                                                                                 .location(MarshallLocation.PAYLOAD)
                                                                                 .locationName("member").build()).build())
                                                .build()).build()).build()).build();

    private static final SdkField<Map<String, String>> MAP_OF_STRING_TO_STRING_FIELD = SdkField
        .<Map<String, String>> builder(MarshallingType.MAP)
        .memberName("MapOfStringToString")
        .getter(getter(AllTypesRequest::mapOfStringToString))
        .setter(setter(Builder::mapOfStringToString))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfStringToString").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<String> builder(MarshallingType.STRING)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, SimpleStruct>> MAP_OF_STRING_TO_SIMPLE_STRUCT_FIELD = SdkField
        .<Map<String, SimpleStruct>> builder(MarshallingType.MAP)
        .memberName("MapOfStringToSimpleStruct")
        .getter(getter(AllTypesRequest::mapOfStringToSimpleStruct))
        .setter(setter(Builder::mapOfStringToSimpleStruct))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfStringToSimpleStruct").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<SimpleStruct> builder(MarshallingType.SDK_POJO)
                                    .constructor(SimpleStruct::builder)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, String>> MAP_OF_ENUM_TO_ENUM_FIELD = SdkField
        .<Map<String, String>> builder(MarshallingType.MAP)
        .memberName("MapOfEnumToEnum")
        .getter(getter(AllTypesRequest::mapOfEnumToEnumAsStrings))
        .setter(setter(Builder::mapOfEnumToEnumWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfEnumToEnum").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<String> builder(MarshallingType.STRING)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, String>> MAP_OF_ENUM_TO_STRING_FIELD = SdkField
        .<Map<String, String>> builder(MarshallingType.MAP)
        .memberName("MapOfEnumToString")
        .getter(getter(AllTypesRequest::mapOfEnumToStringAsStrings))
        .setter(setter(Builder::mapOfEnumToStringWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfEnumToString").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<String> builder(MarshallingType.STRING)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, String>> MAP_OF_STRING_TO_ENUM_FIELD = SdkField
        .<Map<String, String>> builder(MarshallingType.MAP)
        .memberName("MapOfStringToEnum")
        .getter(getter(AllTypesRequest::mapOfStringToEnumAsStrings))
        .setter(setter(Builder::mapOfStringToEnumWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfStringToEnum").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<String> builder(MarshallingType.STRING)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, SimpleStruct>> MAP_OF_ENUM_TO_SIMPLE_STRUCT_FIELD = SdkField
        .<Map<String, SimpleStruct>> builder(MarshallingType.MAP)
        .memberName("MapOfEnumToSimpleStruct")
        .getter(getter(AllTypesRequest::mapOfEnumToSimpleStructAsStrings))
        .setter(setter(Builder::mapOfEnumToSimpleStructWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfEnumToSimpleStruct").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<SimpleStruct> builder(MarshallingType.SDK_POJO)
                                    .constructor(SimpleStruct::builder)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<Map<String, List<String>>> MAP_OF_ENUM_TO_LIST_OF_ENUMS_FIELD = SdkField
        .<Map<String, List<String>>> builder(MarshallingType.MAP)
        .memberName("MapOfEnumToListOfEnums")
        .getter(getter(AllTypesRequest::mapOfEnumToListOfEnumsAsStrings))
        .setter(setter(Builder::mapOfEnumToListOfEnumsWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfEnumToListOfEnums").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<List<String>> builder(MarshallingType.LIST)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build(),
                                            ListTrait
                                                .builder()
                                                .memberLocationName(null)
                                                .memberFieldInfo(
                                                    SdkField.<String> builder(MarshallingType.STRING)
                                                            .traits(LocationTrait.builder()
                                                                                 .location(MarshallLocation.PAYLOAD)
                                                                                 .locationName("member").build()).build())
                                                .build()).build()).build()).build();

    private static final SdkField<Map<String, Map<String, String>>> MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM_FIELD = SdkField
        .<Map<String, Map<String, String>>> builder(MarshallingType.MAP)
        .memberName("MapOfEnumToMapOfStringToEnum")
        .getter(getter(AllTypesRequest::mapOfEnumToMapOfStringToEnumAsStrings))
        .setter(setter(Builder::mapOfEnumToMapOfStringToEnumWithStrings))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MapOfEnumToMapOfStringToEnum")
                             .build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<Map<String, String>> builder(MarshallingType.MAP)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build(),
                                            MapTrait.builder()
                                                    .keyLocationName("key")
                                                    .valueLocationName("value")
                                                    .valueFieldInfo(
                                                        SdkField.<String> builder(MarshallingType.STRING)
                                                                .traits(LocationTrait.builder()
                                                                                     .location(MarshallLocation.PAYLOAD)
                                                                                     .locationName("value").build()).build())
                                                    .build()).build()).build()).build();

    private static final SdkField<Instant> TIMESTAMP_MEMBER_FIELD = SdkField.<Instant> builder(MarshallingType.INSTANT)
                                                                            .memberName("TimestampMember").getter(getter(AllTypesRequest::timestampMember))
                                                                            .setter(setter(Builder::timestampMember))
                                                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("TimestampMember").build()).build();

    private static final SdkField<StructWithTimestamp> STRUCT_WITH_NESTED_TIMESTAMP_MEMBER_FIELD = SdkField
        .<StructWithTimestamp> builder(MarshallingType.SDK_POJO)
        .memberName("StructWithNestedTimestampMember")
        .getter(getter(AllTypesRequest::structWithNestedTimestampMember))
        .setter(setter(Builder::structWithNestedTimestampMember))
        .constructor(StructWithTimestamp::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StructWithNestedTimestampMember")
                             .build()).build();

    private static final SdkField<SdkBytes> BLOB_ARG_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
                                                                     .memberName("BlobArg").getter(getter(AllTypesRequest::blobArg)).setter(setter(Builder::blobArg))
                                                                     .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BlobArg").build()).build();

    private static final SdkField<StructWithNestedBlobType> STRUCT_WITH_NESTED_BLOB_FIELD = SdkField
        .<StructWithNestedBlobType> builder(MarshallingType.SDK_POJO).memberName("StructWithNestedBlob")
        .getter(getter(AllTypesRequest::structWithNestedBlob)).setter(setter(Builder::structWithNestedBlob))
        .constructor(StructWithNestedBlobType::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StructWithNestedBlob").build())
        .build();

    private static final SdkField<Map<String, SdkBytes>> BLOB_MAP_FIELD = SdkField
        .<Map<String, SdkBytes>> builder(MarshallingType.MAP)
        .memberName("BlobMap")
        .getter(getter(AllTypesRequest::blobMap))
        .setter(setter(Builder::blobMap))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BlobMap").build(),
                MapTrait.builder()
                        .keyLocationName("key")
                        .valueLocationName("value")
                        .valueFieldInfo(
                            SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                         .locationName("value").build()).build()).build()).build();

    private static final SdkField<List<SdkBytes>> LIST_OF_BLOBS_FIELD = SdkField
        .<List<SdkBytes>> builder(MarshallingType.LIST)
        .memberName("ListOfBlobs")
        .getter(getter(AllTypesRequest::listOfBlobs))
        .setter(setter(Builder::listOfBlobs))
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ListOfBlobs").build(),
                ListTrait
                    .builder()
                    .memberLocationName(null)
                    .memberFieldInfo(
                        SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
                                .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                                                     .locationName("member").build()).build()).build()).build();

    private static final SdkField<RecursiveStructType> RECURSIVE_STRUCT_FIELD = SdkField
        .<RecursiveStructType> builder(MarshallingType.SDK_POJO).memberName("RecursiveStruct")
        .getter(getter(AllTypesRequest::recursiveStruct)).setter(setter(Builder::recursiveStruct))
        .constructor(RecursiveStructType::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RecursiveStruct").build()).build();

    private static final SdkField<BaseType> POLYMORPHIC_TYPE_WITH_SUB_TYPES_FIELD = SdkField
        .<BaseType> builder(MarshallingType.SDK_POJO)
        .memberName("PolymorphicTypeWithSubTypes")
        .getter(getter(AllTypesRequest::polymorphicTypeWithSubTypes))
        .setter(setter(Builder::polymorphicTypeWithSubTypes))
        .constructor(BaseType::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("PolymorphicTypeWithSubTypes")
                             .build()).build();

    private static final SdkField<SubTypeOne> POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES_FIELD = SdkField
        .<SubTypeOne> builder(MarshallingType.SDK_POJO)
        .memberName("PolymorphicTypeWithoutSubTypes")
        .getter(getter(AllTypesRequest::polymorphicTypeWithoutSubTypes))
        .setter(setter(Builder::polymorphicTypeWithoutSubTypes))
        .constructor(SubTypeOne::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("PolymorphicTypeWithoutSubTypes")
                             .build()).build();

    private static final SdkField<String> ENUM_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                    .memberName("EnumType").getter(getter(AllTypesRequest::enumTypeAsString)).setter(setter(Builder::enumType))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("EnumType").build()).build();

    private static final SdkField<Underscore_Name_Type> UNDERSCORE_NAME_TYPE_FIELD = SdkField
        .<Underscore_Name_Type> builder(MarshallingType.SDK_POJO).memberName("Underscore_Name_Type")
        .getter(getter(AllTypesRequest::underscore_Name_Type)).setter(setter(Builder::underscore_Name_Type))
        .constructor(Underscore_Name_Type::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Underscore_Name_Type").build())
        .build();

    private static final SdkField<Document> MY_DOCUMENT_FIELD = SdkField.<Document> builder(MarshallingType.DOCUMENT)
                                                                        .memberName("MyDocument").getter(getter(AllTypesRequest::myDocument)).setter(setter(Builder::myDocument))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MyDocument").build()).build();

    private static final SdkField<AllTypesUnionStructure> ALL_TYPES_UNION_STRUCTURE_FIELD = SdkField
        .<AllTypesUnionStructure> builder(MarshallingType.SDK_POJO).memberName("AllTypesUnionStructure")
        .getter(getter(AllTypesRequest::allTypesUnionStructure)).setter(setter(Builder::allTypesUnionStructure))
        .constructor(AllTypesUnionStructure::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AllTypesUnionStructure").build())
        .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(STRING_MEMBER_FIELD,
                                                                                                   INTEGER_MEMBER_FIELD, BOOLEAN_MEMBER_FIELD, FLOAT_MEMBER_FIELD, DOUBLE_MEMBER_FIELD, LONG_MEMBER_FIELD,
                                                                                                   SHORT_MEMBER_FIELD, BYTE_MEMBER_FIELD, SIMPLE_LIST_FIELD, LIST_OF_ENUMS_FIELD, LIST_OF_MAPS_FIELD,
                                                                                                   LIST_OF_STRUCTS_FIELD, LIST_OF_MAP_OF_ENUM_TO_STRING_FIELD, LIST_OF_MAP_OF_STRING_TO_STRUCT_FIELD,
                                                                                                   MAP_OF_STRING_TO_INTEGER_LIST_FIELD, MAP_OF_STRING_TO_STRING_FIELD, MAP_OF_STRING_TO_SIMPLE_STRUCT_FIELD,
                                                                                                   MAP_OF_ENUM_TO_ENUM_FIELD, MAP_OF_ENUM_TO_STRING_FIELD, MAP_OF_STRING_TO_ENUM_FIELD,
                                                                                                   MAP_OF_ENUM_TO_SIMPLE_STRUCT_FIELD, MAP_OF_ENUM_TO_LIST_OF_ENUMS_FIELD, MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM_FIELD,
                                                                                                   TIMESTAMP_MEMBER_FIELD, STRUCT_WITH_NESTED_TIMESTAMP_MEMBER_FIELD, BLOB_ARG_FIELD, STRUCT_WITH_NESTED_BLOB_FIELD,
                                                                                                   BLOB_MAP_FIELD, LIST_OF_BLOBS_FIELD, RECURSIVE_STRUCT_FIELD, POLYMORPHIC_TYPE_WITH_SUB_TYPES_FIELD,
                                                                                                   POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES_FIELD, ENUM_TYPE_FIELD, UNDERSCORE_NAME_TYPE_FIELD, MY_DOCUMENT_FIELD,
                                                                                                   ALL_TYPES_UNION_STRUCTURE_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final String stringMember;

    private final Integer integerMember;

    private final Boolean booleanMember;

    private final Float floatMember;

    private final Double doubleMember;

    private final Long longMember;

    private final Short shortMember;

    private final Byte byteMember;

    private final List<String> simpleList;

    private final List<String> listOfEnums;

    private final List<Map<String, String>> listOfMaps;

    private final List<SimpleStruct> listOfStructs;

    private final List<Map<String, String>> listOfMapOfEnumToString;

    private final List<Map<String, SimpleStruct>> listOfMapOfStringToStruct;

    private final Map<String, List<Integer>> mapOfStringToIntegerList;

    private final Map<String, String> mapOfStringToString;

    private final Map<String, SimpleStruct> mapOfStringToSimpleStruct;

    private final Map<String, String> mapOfEnumToEnum;

    private final Map<String, String> mapOfEnumToString;

    private final Map<String, String> mapOfStringToEnum;

    private final Map<String, SimpleStruct> mapOfEnumToSimpleStruct;

    private final Map<String, List<String>> mapOfEnumToListOfEnums;

    private final Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnum;

    private final Instant timestampMember;

    private final StructWithTimestamp structWithNestedTimestampMember;

    private final SdkBytes blobArg;

    private final StructWithNestedBlobType structWithNestedBlob;

    private final Map<String, SdkBytes> blobMap;

    private final List<SdkBytes> listOfBlobs;

    private final RecursiveStructType recursiveStruct;

    private final BaseType polymorphicTypeWithSubTypes;

    private final SubTypeOne polymorphicTypeWithoutSubTypes;

    private final String enumType;

    private final Underscore_Name_Type underscore_Name_Type;

    private final Document myDocument;

    private final AllTypesUnionStructure allTypesUnionStructure;

    private AllTypesRequest(BuilderImpl builder) {
        super(builder);
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.booleanMember = builder.booleanMember;
        this.floatMember = builder.floatMember;
        this.doubleMember = builder.doubleMember;
        this.longMember = builder.longMember;
        this.shortMember = builder.shortMember;
        this.byteMember = builder.byteMember;
        this.simpleList = builder.simpleList;
        this.listOfEnums = builder.listOfEnums;
        this.listOfMaps = builder.listOfMaps;
        this.listOfStructs = builder.listOfStructs;
        this.listOfMapOfEnumToString = builder.listOfMapOfEnumToString;
        this.listOfMapOfStringToStruct = builder.listOfMapOfStringToStruct;
        this.mapOfStringToIntegerList = builder.mapOfStringToIntegerList;
        this.mapOfStringToString = builder.mapOfStringToString;
        this.mapOfStringToSimpleStruct = builder.mapOfStringToSimpleStruct;
        this.mapOfEnumToEnum = builder.mapOfEnumToEnum;
        this.mapOfEnumToString = builder.mapOfEnumToString;
        this.mapOfStringToEnum = builder.mapOfStringToEnum;
        this.mapOfEnumToSimpleStruct = builder.mapOfEnumToSimpleStruct;
        this.mapOfEnumToListOfEnums = builder.mapOfEnumToListOfEnums;
        this.mapOfEnumToMapOfStringToEnum = builder.mapOfEnumToMapOfStringToEnum;
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
        this.underscore_Name_Type = builder.underscore_Name_Type;
        this.myDocument = builder.myDocument;
        this.allTypesUnionStructure = builder.allTypesUnionStructure;
    }

    /**
     * Returns the value of the StringMember property for this object.
     *
     * @return The value of the StringMember property for this object.
     */
    public final String stringMember() {
        return stringMember;
    }

    /**
     * Returns the value of the IntegerMember property for this object.
     *
     * @return The value of the IntegerMember property for this object.
     */
    public final Integer integerMember() {
        return integerMember;
    }

    /**
     * Returns the value of the BooleanMember property for this object.
     *
     * @return The value of the BooleanMember property for this object.
     */
    public final Boolean booleanMember() {
        return booleanMember;
    }

    /**
     * Returns the value of the FloatMember property for this object.
     *
     * @return The value of the FloatMember property for this object.
     */
    public final Float floatMember() {
        return floatMember;
    }

    /**
     * Returns the value of the DoubleMember property for this object.
     *
     * @return The value of the DoubleMember property for this object.
     */
    public final Double doubleMember() {
        return doubleMember;
    }

    /**
     * Returns the value of the LongMember property for this object.
     *
     * @return The value of the LongMember property for this object.
     */
    public final Long longMember() {
        return longMember;
    }

    /**
     * Returns the value of the ShortMember property for this object.
     *
     * @return The value of the ShortMember property for this object.
     */
    public final Short shortMember() {
        return shortMember;
    }

    /**
     * Returns the value of the ByteMember property for this object.
     *
     * @return The value of the ByteMember property for this object.
     */
    public final Byte byteMember() {
        return byteMember;
    }

    /**
     * For responses, this returns true if the service returned a value for the SimpleList property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasSimpleList() {
        return simpleList != null && !(simpleList instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the SimpleList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasSimpleList} method.
     * </p>
     *
     * @return The value of the SimpleList property for this object.
     */
    public final List<String> simpleList() {
        return simpleList;
    }

    /**
     * Returns the value of the ListOfEnums property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfEnums} method.
     * </p>
     *
     * @return The value of the ListOfEnums property for this object.
     */
    public final List<EnumType> listOfEnums() {
        return ListOfEnumsCopier.copyStringToEnum(listOfEnums);
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfEnums property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasListOfEnums() {
        return listOfEnums != null && !(listOfEnums instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfEnums property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfEnums} method.
     * </p>
     *
     * @return The value of the ListOfEnums property for this object.
     */
    public final List<String> listOfEnumsAsStrings() {
        return listOfEnums;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfMaps property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasListOfMaps() {
        return listOfMaps != null && !(listOfMaps instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfMaps property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfMaps} method.
     * </p>
     *
     * @return The value of the ListOfMaps property for this object.
     */
    public final List<Map<String, String>> listOfMaps() {
        return listOfMaps;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfStructs property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasListOfStructs() {
        return listOfStructs != null && !(listOfStructs instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfStructs property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfStructs} method.
     * </p>
     *
     * @return The value of the ListOfStructs property for this object.
     */
    public final List<SimpleStruct> listOfStructs() {
        return listOfStructs;
    }

    /**
     * Returns the value of the ListOfMapOfEnumToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfMapOfEnumToString} method.
     * </p>
     *
     * @return The value of the ListOfMapOfEnumToString property for this object.
     */
    public final List<Map<EnumType, String>> listOfMapOfEnumToString() {
        return ListOfMapOfEnumToStringCopier.copyStringToEnum(listOfMapOfEnumToString);
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfMapOfEnumToString property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasListOfMapOfEnumToString() {
        return listOfMapOfEnumToString != null && !(listOfMapOfEnumToString instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfMapOfEnumToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfMapOfEnumToString} method.
     * </p>
     *
     * @return The value of the ListOfMapOfEnumToString property for this object.
     */
    public final List<Map<String, String>> listOfMapOfEnumToStringAsStrings() {
        return listOfMapOfEnumToString;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfMapOfStringToStruct property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasListOfMapOfStringToStruct() {
        return listOfMapOfStringToStruct != null && !(listOfMapOfStringToStruct instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfMapOfStringToStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfMapOfStringToStruct} method.
     * </p>
     *
     * @return The value of the ListOfMapOfStringToStruct property for this object.
     */
    public final List<Map<String, SimpleStruct>> listOfMapOfStringToStruct() {
        return listOfMapOfStringToStruct;
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfStringToIntegerList property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasMapOfStringToIntegerList() {
        return mapOfStringToIntegerList != null && !(mapOfStringToIntegerList instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToIntegerList property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToIntegerList} method.
     * </p>
     *
     * @return The value of the MapOfStringToIntegerList property for this object.
     */
    public final Map<String, List<Integer>> mapOfStringToIntegerList() {
        return mapOfStringToIntegerList;
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfStringToString property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasMapOfStringToString() {
        return mapOfStringToString != null && !(mapOfStringToString instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToString} method.
     * </p>
     *
     * @return The value of the MapOfStringToString property for this object.
     */
    public final Map<String, String> mapOfStringToString() {
        return mapOfStringToString;
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfStringToSimpleStruct property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasMapOfStringToSimpleStruct() {
        return mapOfStringToSimpleStruct != null && !(mapOfStringToSimpleStruct instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToSimpleStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToSimpleStruct} method.
     * </p>
     *
     * @return The value of the MapOfStringToSimpleStruct property for this object.
     */
    public final Map<String, SimpleStruct> mapOfStringToSimpleStruct() {
        return mapOfStringToSimpleStruct;
    }

    /**
     * Returns the value of the MapOfEnumToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToEnum} method.
     * </p>
     *
     * @return The value of the MapOfEnumToEnum property for this object.
     */
    public final Map<EnumType, EnumType> mapOfEnumToEnum() {
        return MapOfEnumToEnumCopier.copyStringToEnum(mapOfEnumToEnum);
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfEnumToEnum property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasMapOfEnumToEnum() {
        return mapOfEnumToEnum != null && !(mapOfEnumToEnum instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfEnumToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToEnum} method.
     * </p>
     *
     * @return The value of the MapOfEnumToEnum property for this object.
     */
    public final Map<String, String> mapOfEnumToEnumAsStrings() {
        return mapOfEnumToEnum;
    }

    /**
     * Returns the value of the MapOfEnumToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToString} method.
     * </p>
     *
     * @return The value of the MapOfEnumToString property for this object.
     */
    public final Map<EnumType, String> mapOfEnumToString() {
        return MapOfEnumToStringCopier.copyStringToEnum(mapOfEnumToString);
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfEnumToString property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasMapOfEnumToString() {
        return mapOfEnumToString != null && !(mapOfEnumToString instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfEnumToString property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToString} method.
     * </p>
     *
     * @return The value of the MapOfEnumToString property for this object.
     */
    public final Map<String, String> mapOfEnumToStringAsStrings() {
        return mapOfEnumToString;
    }

    /**
     * Returns the value of the MapOfStringToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToEnum} method.
     * </p>
     *
     * @return The value of the MapOfStringToEnum property for this object.
     */
    public final Map<String, EnumType> mapOfStringToEnum() {
        return MapOfStringToEnumCopier.copyStringToEnum(mapOfStringToEnum);
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfStringToEnum property. This DOES
     * NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasMapOfStringToEnum() {
        return mapOfStringToEnum != null && !(mapOfStringToEnum instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfStringToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfStringToEnum} method.
     * </p>
     *
     * @return The value of the MapOfStringToEnum property for this object.
     */
    public final Map<String, String> mapOfStringToEnumAsStrings() {
        return mapOfStringToEnum;
    }

    /**
     * Returns the value of the MapOfEnumToSimpleStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToSimpleStruct} method.
     * </p>
     *
     * @return The value of the MapOfEnumToSimpleStruct property for this object.
     */
    public final Map<EnumType, SimpleStruct> mapOfEnumToSimpleStruct() {
        return MapOfEnumToSimpleStructCopier.copyStringToEnum(mapOfEnumToSimpleStruct);
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfEnumToSimpleStruct property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasMapOfEnumToSimpleStruct() {
        return mapOfEnumToSimpleStruct != null && !(mapOfEnumToSimpleStruct instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfEnumToSimpleStruct property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToSimpleStruct} method.
     * </p>
     *
     * @return The value of the MapOfEnumToSimpleStruct property for this object.
     */
    public final Map<String, SimpleStruct> mapOfEnumToSimpleStructAsStrings() {
        return mapOfEnumToSimpleStruct;
    }

    /**
     * Returns the value of the MapOfEnumToListOfEnums property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToListOfEnums} method.
     * </p>
     *
     * @return The value of the MapOfEnumToListOfEnums property for this object.
     */
    public final Map<EnumType, List<EnumType>> mapOfEnumToListOfEnums() {
        return MapOfEnumToListOfEnumsCopier.copyStringToEnum(mapOfEnumToListOfEnums);
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfEnumToListOfEnums property. This
     * DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasMapOfEnumToListOfEnums() {
        return mapOfEnumToListOfEnums != null && !(mapOfEnumToListOfEnums instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfEnumToListOfEnums property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToListOfEnums} method.
     * </p>
     *
     * @return The value of the MapOfEnumToListOfEnums property for this object.
     */
    public final Map<String, List<String>> mapOfEnumToListOfEnumsAsStrings() {
        return mapOfEnumToListOfEnums;
    }

    /**
     * Returns the value of the MapOfEnumToMapOfStringToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToMapOfStringToEnum} method.
     * </p>
     *
     * @return The value of the MapOfEnumToMapOfStringToEnum property for this object.
     */
    public final Map<EnumType, Map<String, EnumType>> mapOfEnumToMapOfStringToEnum() {
        return MapOfEnumToMapOfStringToEnumCopier.copyStringToEnum(mapOfEnumToMapOfStringToEnum);
    }

    /**
     * For responses, this returns true if the service returned a value for the MapOfEnumToMapOfStringToEnum property.
     * This DOES NOT check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the
     * property). This is useful because the SDK will never return a null collection or map, but you may need to
     * differentiate between the service returning nothing (or null) and the service returning an empty collection or
     * map. For requests, this returns true if a value for the property was specified in the request builder, and false
     * if a value was not specified.
     */
    public final boolean hasMapOfEnumToMapOfStringToEnum() {
        return mapOfEnumToMapOfStringToEnum != null && !(mapOfEnumToMapOfStringToEnum instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the MapOfEnumToMapOfStringToEnum property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasMapOfEnumToMapOfStringToEnum} method.
     * </p>
     *
     * @return The value of the MapOfEnumToMapOfStringToEnum property for this object.
     */
    public final Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnumAsStrings() {
        return mapOfEnumToMapOfStringToEnum;
    }

    /**
     * Returns the value of the TimestampMember property for this object.
     *
     * @return The value of the TimestampMember property for this object.
     */
    public final Instant timestampMember() {
        return timestampMember;
    }

    /**
     * Returns the value of the StructWithNestedTimestampMember property for this object.
     *
     * @return The value of the StructWithNestedTimestampMember property for this object.
     */
    public final StructWithTimestamp structWithNestedTimestampMember() {
        return structWithNestedTimestampMember;
    }

    /**
     * Returns the value of the BlobArg property for this object.
     *
     * @return The value of the BlobArg property for this object.
     */
    public final SdkBytes blobArg() {
        return blobArg;
    }

    /**
     * Returns the value of the StructWithNestedBlob property for this object.
     *
     * @return The value of the StructWithNestedBlob property for this object.
     */
    public final StructWithNestedBlobType structWithNestedBlob() {
        return structWithNestedBlob;
    }

    /**
     * For responses, this returns true if the service returned a value for the BlobMap property. This DOES NOT check
     * that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property). This is
     * useful because the SDK will never return a null collection or map, but you may need to differentiate between the
     * service returning nothing (or null) and the service returning an empty collection or map. For requests, this
     * returns true if a value for the property was specified in the request builder, and false if a value was not
     * specified.
     */
    public final boolean hasBlobMap() {
        return blobMap != null && !(blobMap instanceof SdkAutoConstructMap);
    }

    /**
     * Returns the value of the BlobMap property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasBlobMap} method.
     * </p>
     *
     * @return The value of the BlobMap property for this object.
     */
    public final Map<String, SdkBytes> blobMap() {
        return blobMap;
    }

    /**
     * For responses, this returns true if the service returned a value for the ListOfBlobs property. This DOES NOT
     * check that the value is non-empty (for which, you should check the {@code isEmpty()} method on the property).
     * This is useful because the SDK will never return a null collection or map, but you may need to differentiate
     * between the service returning nothing (or null) and the service returning an empty collection or map. For
     * requests, this returns true if a value for the property was specified in the request builder, and false if a
     * value was not specified.
     */
    public final boolean hasListOfBlobs() {
        return listOfBlobs != null && !(listOfBlobs instanceof SdkAutoConstructList);
    }

    /**
     * Returns the value of the ListOfBlobs property for this object.
     * <p>
     * Attempts to modify the collection returned by this method will result in an UnsupportedOperationException.
     * </p>
     * <p>
     * This method will never return null. If you would like to know whether the service returned this field (so that
     * you can differentiate between null and empty), you can use the {@link #hasListOfBlobs} method.
     * </p>
     *
     * @return The value of the ListOfBlobs property for this object.
     */
    public final List<SdkBytes> listOfBlobs() {
        return listOfBlobs;
    }

    /**
     * Returns the value of the RecursiveStruct property for this object.
     *
     * @return The value of the RecursiveStruct property for this object.
     */
    public final RecursiveStructType recursiveStruct() {
        return recursiveStruct;
    }

    /**
     * Returns the value of the PolymorphicTypeWithSubTypes property for this object.
     *
     * @return The value of the PolymorphicTypeWithSubTypes property for this object.
     */
    public final BaseType polymorphicTypeWithSubTypes() {
        return polymorphicTypeWithSubTypes;
    }

    /**
     * Returns the value of the PolymorphicTypeWithoutSubTypes property for this object.
     *
     * @return The value of the PolymorphicTypeWithoutSubTypes property for this object.
     */
    public final SubTypeOne polymorphicTypeWithoutSubTypes() {
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
    public final EnumType enumType() {
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
    public final String enumTypeAsString() {
        return enumType;
    }

    /**
     * Returns the value of the Underscore_Name_Type property for this object.
     *
     * @return The value of the Underscore_Name_Type property for this object.
     */
    public final Underscore_Name_Type underscore_Name_Type() {
        return underscore_Name_Type;
    }

    /**
     * Returns the value of the MyDocument property for this object.
     *
     * @return The value of the MyDocument property for this object.
     */
    public final Document myDocument() {
        return myDocument;
    }

    /**
     * Returns the value of the AllTypesUnionStructure property for this object.
     *
     * @return The value of the AllTypesUnionStructure property for this object.
     */
    public final AllTypesUnionStructure allTypesUnionStructure() {
        return allTypesUnionStructure;
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
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        hashCode = 31 * hashCode + Objects.hashCode(integerMember());
        hashCode = 31 * hashCode + Objects.hashCode(booleanMember());
        hashCode = 31 * hashCode + Objects.hashCode(floatMember());
        hashCode = 31 * hashCode + Objects.hashCode(doubleMember());
        hashCode = 31 * hashCode + Objects.hashCode(longMember());
        hashCode = 31 * hashCode + Objects.hashCode(shortMember());
        hashCode = 31 * hashCode + Objects.hashCode(byteMember());
        hashCode = 31 * hashCode + Objects.hashCode(hasSimpleList() ? simpleList() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfEnums() ? listOfEnumsAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfMaps() ? listOfMaps() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfStructs() ? listOfStructs() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfMapOfEnumToString() ? listOfMapOfEnumToStringAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfMapOfStringToStruct() ? listOfMapOfStringToStruct() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfStringToIntegerList() ? mapOfStringToIntegerList() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfStringToString() ? mapOfStringToString() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfStringToSimpleStruct() ? mapOfStringToSimpleStruct() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfEnumToEnum() ? mapOfEnumToEnumAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfEnumToString() ? mapOfEnumToStringAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfStringToEnum() ? mapOfStringToEnumAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfEnumToSimpleStruct() ? mapOfEnumToSimpleStructAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasMapOfEnumToListOfEnums() ? mapOfEnumToListOfEnumsAsStrings() : null);
        hashCode = 31 * hashCode
                   + Objects.hashCode(hasMapOfEnumToMapOfStringToEnum() ? mapOfEnumToMapOfStringToEnumAsStrings() : null);
        hashCode = 31 * hashCode + Objects.hashCode(timestampMember());
        hashCode = 31 * hashCode + Objects.hashCode(structWithNestedTimestampMember());
        hashCode = 31 * hashCode + Objects.hashCode(blobArg());
        hashCode = 31 * hashCode + Objects.hashCode(structWithNestedBlob());
        hashCode = 31 * hashCode + Objects.hashCode(hasBlobMap() ? blobMap() : null);
        hashCode = 31 * hashCode + Objects.hashCode(hasListOfBlobs() ? listOfBlobs() : null);
        hashCode = 31 * hashCode + Objects.hashCode(recursiveStruct());
        hashCode = 31 * hashCode + Objects.hashCode(polymorphicTypeWithSubTypes());
        hashCode = 31 * hashCode + Objects.hashCode(polymorphicTypeWithoutSubTypes());
        hashCode = 31 * hashCode + Objects.hashCode(enumTypeAsString());
        hashCode = 31 * hashCode + Objects.hashCode(underscore_Name_Type());
        hashCode = 31 * hashCode + Objects.hashCode(myDocument());
        hashCode = 31 * hashCode + Objects.hashCode(allTypesUnionStructure());
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
        if (!(obj instanceof AllTypesRequest)) {
            return false;
        }
        AllTypesRequest other = (AllTypesRequest) obj;
        return Objects.equals(stringMember(), other.stringMember()) && Objects.equals(integerMember(), other.integerMember())
               && Objects.equals(booleanMember(), other.booleanMember()) && Objects.equals(floatMember(), other.floatMember())
               && Objects.equals(doubleMember(), other.doubleMember()) && Objects.equals(longMember(), other.longMember())
               && Objects.equals(shortMember(), other.shortMember()) && Objects.equals(byteMember(), other.byteMember())
               && hasSimpleList() == other.hasSimpleList() && Objects.equals(simpleList(), other.simpleList())
               && hasListOfEnums() == other.hasListOfEnums()
               && Objects.equals(listOfEnumsAsStrings(), other.listOfEnumsAsStrings())
               && hasListOfMaps() == other.hasListOfMaps() && Objects.equals(listOfMaps(), other.listOfMaps())
               && hasListOfStructs() == other.hasListOfStructs() && Objects.equals(listOfStructs(), other.listOfStructs())
               && hasListOfMapOfEnumToString() == other.hasListOfMapOfEnumToString()
               && Objects.equals(listOfMapOfEnumToStringAsStrings(), other.listOfMapOfEnumToStringAsStrings())
               && hasListOfMapOfStringToStruct() == other.hasListOfMapOfStringToStruct()
               && Objects.equals(listOfMapOfStringToStruct(), other.listOfMapOfStringToStruct())
               && hasMapOfStringToIntegerList() == other.hasMapOfStringToIntegerList()
               && Objects.equals(mapOfStringToIntegerList(), other.mapOfStringToIntegerList())
               && hasMapOfStringToString() == other.hasMapOfStringToString()
               && Objects.equals(mapOfStringToString(), other.mapOfStringToString())
               && hasMapOfStringToSimpleStruct() == other.hasMapOfStringToSimpleStruct()
               && Objects.equals(mapOfStringToSimpleStruct(), other.mapOfStringToSimpleStruct())
               && hasMapOfEnumToEnum() == other.hasMapOfEnumToEnum()
               && Objects.equals(mapOfEnumToEnumAsStrings(), other.mapOfEnumToEnumAsStrings())
               && hasMapOfEnumToString() == other.hasMapOfEnumToString()
               && Objects.equals(mapOfEnumToStringAsStrings(), other.mapOfEnumToStringAsStrings())
               && hasMapOfStringToEnum() == other.hasMapOfStringToEnum()
               && Objects.equals(mapOfStringToEnumAsStrings(), other.mapOfStringToEnumAsStrings())
               && hasMapOfEnumToSimpleStruct() == other.hasMapOfEnumToSimpleStruct()
               && Objects.equals(mapOfEnumToSimpleStructAsStrings(), other.mapOfEnumToSimpleStructAsStrings())
               && hasMapOfEnumToListOfEnums() == other.hasMapOfEnumToListOfEnums()
               && Objects.equals(mapOfEnumToListOfEnumsAsStrings(), other.mapOfEnumToListOfEnumsAsStrings())
               && hasMapOfEnumToMapOfStringToEnum() == other.hasMapOfEnumToMapOfStringToEnum()
               && Objects.equals(mapOfEnumToMapOfStringToEnumAsStrings(), other.mapOfEnumToMapOfStringToEnumAsStrings())
               && Objects.equals(timestampMember(), other.timestampMember())
               && Objects.equals(structWithNestedTimestampMember(), other.structWithNestedTimestampMember())
               && Objects.equals(blobArg(), other.blobArg())
               && Objects.equals(structWithNestedBlob(), other.structWithNestedBlob()) && hasBlobMap() == other.hasBlobMap()
               && Objects.equals(blobMap(), other.blobMap()) && hasListOfBlobs() == other.hasListOfBlobs()
               && Objects.equals(listOfBlobs(), other.listOfBlobs())
               && Objects.equals(recursiveStruct(), other.recursiveStruct())
               && Objects.equals(polymorphicTypeWithSubTypes(), other.polymorphicTypeWithSubTypes())
               && Objects.equals(polymorphicTypeWithoutSubTypes(), other.polymorphicTypeWithoutSubTypes())
               && Objects.equals(enumTypeAsString(), other.enumTypeAsString())
               && Objects.equals(underscore_Name_Type(), other.underscore_Name_Type())
               && Objects.equals(myDocument(), other.myDocument())
               && Objects.equals(allTypesUnionStructure(), other.allTypesUnionStructure());
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public final String toString() {
        return ToString
            .builder("AllTypesRequest")
            .add("StringMember", stringMember())
            .add("IntegerMember", integerMember())
            .add("BooleanMember", booleanMember())
            .add("FloatMember", floatMember())
            .add("DoubleMember", doubleMember())
            .add("LongMember", longMember())
            .add("ShortMember", shortMember())
            .add("ByteMember", byteMember())
            .add("SimpleList", hasSimpleList() ? simpleList() : null)
            .add("ListOfEnums", hasListOfEnums() ? listOfEnumsAsStrings() : null)
            .add("ListOfMaps", hasListOfMaps() ? listOfMaps() : null)
            .add("ListOfStructs", hasListOfStructs() ? listOfStructs() : null)
            .add("ListOfMapOfEnumToString", hasListOfMapOfEnumToString() ? listOfMapOfEnumToStringAsStrings() : null)
            .add("ListOfMapOfStringToStruct", hasListOfMapOfStringToStruct() ? listOfMapOfStringToStruct() : null)
            .add("MapOfStringToIntegerList", hasMapOfStringToIntegerList() ? mapOfStringToIntegerList() : null)
            .add("MapOfStringToString", hasMapOfStringToString() ? mapOfStringToString() : null)
            .add("MapOfStringToSimpleStruct", hasMapOfStringToSimpleStruct() ? mapOfStringToSimpleStruct() : null)
            .add("MapOfEnumToEnum", hasMapOfEnumToEnum() ? mapOfEnumToEnumAsStrings() : null)
            .add("MapOfEnumToString", hasMapOfEnumToString() ? mapOfEnumToStringAsStrings() : null)
            .add("MapOfStringToEnum", hasMapOfStringToEnum() ? mapOfStringToEnumAsStrings() : null)
            .add("MapOfEnumToSimpleStruct", hasMapOfEnumToSimpleStruct() ? mapOfEnumToSimpleStructAsStrings() : null)
            .add("MapOfEnumToListOfEnums", hasMapOfEnumToListOfEnums() ? mapOfEnumToListOfEnumsAsStrings() : null)
            .add("MapOfEnumToMapOfStringToEnum",
                 hasMapOfEnumToMapOfStringToEnum() ? mapOfEnumToMapOfStringToEnumAsStrings() : null)
            .add("TimestampMember", timestampMember())
            .add("StructWithNestedTimestampMember", structWithNestedTimestampMember()).add("BlobArg", blobArg())
            .add("StructWithNestedBlob", structWithNestedBlob()).add("BlobMap", hasBlobMap() ? blobMap() : null)
            .add("ListOfBlobs", hasListOfBlobs() ? listOfBlobs() : null).add("RecursiveStruct", recursiveStruct())
            .add("PolymorphicTypeWithSubTypes", polymorphicTypeWithSubTypes())
            .add("PolymorphicTypeWithoutSubTypes", polymorphicTypeWithoutSubTypes()).add("EnumType", enumTypeAsString())
            .add("Underscore_Name_Type", underscore_Name_Type()).add("MyDocument", myDocument())
            .add("AllTypesUnionStructure", allTypesUnionStructure()).build();
    }

    public final <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        switch (fieldName) {
            case "StringMember":
                return Optional.ofNullable(clazz.cast(stringMember()));
            case "IntegerMember":
                return Optional.ofNullable(clazz.cast(integerMember()));
            case "BooleanMember":
                return Optional.ofNullable(clazz.cast(booleanMember()));
            case "FloatMember":
                return Optional.ofNullable(clazz.cast(floatMember()));
            case "DoubleMember":
                return Optional.ofNullable(clazz.cast(doubleMember()));
            case "LongMember":
                return Optional.ofNullable(clazz.cast(longMember()));
            case "ShortMember":
                return Optional.ofNullable(clazz.cast(shortMember()));
            case "ByteMember":
                return Optional.ofNullable(clazz.cast(byteMember()));
            case "SimpleList":
                return Optional.ofNullable(clazz.cast(simpleList()));
            case "ListOfEnums":
                return Optional.ofNullable(clazz.cast(listOfEnumsAsStrings()));
            case "ListOfMaps":
                return Optional.ofNullable(clazz.cast(listOfMaps()));
            case "ListOfStructs":
                return Optional.ofNullable(clazz.cast(listOfStructs()));
            case "ListOfMapOfEnumToString":
                return Optional.ofNullable(clazz.cast(listOfMapOfEnumToStringAsStrings()));
            case "ListOfMapOfStringToStruct":
                return Optional.ofNullable(clazz.cast(listOfMapOfStringToStruct()));
            case "MapOfStringToIntegerList":
                return Optional.ofNullable(clazz.cast(mapOfStringToIntegerList()));
            case "MapOfStringToString":
                return Optional.ofNullable(clazz.cast(mapOfStringToString()));
            case "MapOfStringToSimpleStruct":
                return Optional.ofNullable(clazz.cast(mapOfStringToSimpleStruct()));
            case "MapOfEnumToEnum":
                return Optional.ofNullable(clazz.cast(mapOfEnumToEnumAsStrings()));
            case "MapOfEnumToString":
                return Optional.ofNullable(clazz.cast(mapOfEnumToStringAsStrings()));
            case "MapOfStringToEnum":
                return Optional.ofNullable(clazz.cast(mapOfStringToEnumAsStrings()));
            case "MapOfEnumToSimpleStruct":
                return Optional.ofNullable(clazz.cast(mapOfEnumToSimpleStructAsStrings()));
            case "MapOfEnumToListOfEnums":
                return Optional.ofNullable(clazz.cast(mapOfEnumToListOfEnumsAsStrings()));
            case "MapOfEnumToMapOfStringToEnum":
                return Optional.ofNullable(clazz.cast(mapOfEnumToMapOfStringToEnumAsStrings()));
            case "TimestampMember":
                return Optional.ofNullable(clazz.cast(timestampMember()));
            case "StructWithNestedTimestampMember":
                return Optional.ofNullable(clazz.cast(structWithNestedTimestampMember()));
            case "BlobArg":
                return Optional.ofNullable(clazz.cast(blobArg()));
            case "StructWithNestedBlob":
                return Optional.ofNullable(clazz.cast(structWithNestedBlob()));
            case "BlobMap":
                return Optional.ofNullable(clazz.cast(blobMap()));
            case "ListOfBlobs":
                return Optional.ofNullable(clazz.cast(listOfBlobs()));
            case "RecursiveStruct":
                return Optional.ofNullable(clazz.cast(recursiveStruct()));
            case "PolymorphicTypeWithSubTypes":
                return Optional.ofNullable(clazz.cast(polymorphicTypeWithSubTypes()));
            case "PolymorphicTypeWithoutSubTypes":
                return Optional.ofNullable(clazz.cast(polymorphicTypeWithoutSubTypes()));
            case "EnumType":
                return Optional.ofNullable(clazz.cast(enumTypeAsString()));
            case "Underscore_Name_Type":
                return Optional.ofNullable(clazz.cast(underscore_Name_Type()));
            case "MyDocument":
                return Optional.ofNullable(clazz.cast(myDocument()));
            case "AllTypesUnionStructure":
                return Optional.ofNullable(clazz.cast(allTypesUnionStructure()));
            default:
                return Optional.empty();
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
        map.put("StringMember", STRING_MEMBER_FIELD);
        map.put("IntegerMember", INTEGER_MEMBER_FIELD);
        map.put("BooleanMember", BOOLEAN_MEMBER_FIELD);
        map.put("FloatMember", FLOAT_MEMBER_FIELD);
        map.put("DoubleMember", DOUBLE_MEMBER_FIELD);
        map.put("LongMember", LONG_MEMBER_FIELD);
        map.put("ShortMember", SHORT_MEMBER_FIELD);
        map.put("ByteMember", BYTE_MEMBER_FIELD);
        map.put("SimpleList", SIMPLE_LIST_FIELD);
        map.put("ListOfEnums", LIST_OF_ENUMS_FIELD);
        map.put("ListOfMaps", LIST_OF_MAPS_FIELD);
        map.put("ListOfStructs", LIST_OF_STRUCTS_FIELD);
        map.put("ListOfMapOfEnumToString", LIST_OF_MAP_OF_ENUM_TO_STRING_FIELD);
        map.put("ListOfMapOfStringToStruct", LIST_OF_MAP_OF_STRING_TO_STRUCT_FIELD);
        map.put("MapOfStringToIntegerList", MAP_OF_STRING_TO_INTEGER_LIST_FIELD);
        map.put("MapOfStringToString", MAP_OF_STRING_TO_STRING_FIELD);
        map.put("MapOfStringToSimpleStruct", MAP_OF_STRING_TO_SIMPLE_STRUCT_FIELD);
        map.put("MapOfEnumToEnum", MAP_OF_ENUM_TO_ENUM_FIELD);
        map.put("MapOfEnumToString", MAP_OF_ENUM_TO_STRING_FIELD);
        map.put("MapOfStringToEnum", MAP_OF_STRING_TO_ENUM_FIELD);
        map.put("MapOfEnumToSimpleStruct", MAP_OF_ENUM_TO_SIMPLE_STRUCT_FIELD);
        map.put("MapOfEnumToListOfEnums", MAP_OF_ENUM_TO_LIST_OF_ENUMS_FIELD);
        map.put("MapOfEnumToMapOfStringToEnum", MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM_FIELD);
        map.put("TimestampMember", TIMESTAMP_MEMBER_FIELD);
        map.put("StructWithNestedTimestampMember", STRUCT_WITH_NESTED_TIMESTAMP_MEMBER_FIELD);
        map.put("BlobArg", BLOB_ARG_FIELD);
        map.put("StructWithNestedBlob", STRUCT_WITH_NESTED_BLOB_FIELD);
        map.put("BlobMap", BLOB_MAP_FIELD);
        map.put("ListOfBlobs", LIST_OF_BLOBS_FIELD);
        map.put("RecursiveStruct", RECURSIVE_STRUCT_FIELD);
        map.put("PolymorphicTypeWithSubTypes", POLYMORPHIC_TYPE_WITH_SUB_TYPES_FIELD);
        map.put("PolymorphicTypeWithoutSubTypes", POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES_FIELD);
        map.put("EnumType", ENUM_TYPE_FIELD);
        map.put("Underscore_Name_Type", UNDERSCORE_NAME_TYPE_FIELD);
        map.put("MyDocument", MY_DOCUMENT_FIELD);
        map.put("AllTypesUnionStructure", ALL_TYPES_UNION_STRUCTURE_FIELD);
        return Collections.unmodifiableMap(map);
    }

    private static <T> Function<Object, T> getter(Function<AllTypesRequest, T> g) {
        return obj -> g.apply((AllTypesRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    @Mutable
    @NotThreadSafe
    public interface Builder extends JsonProtocolTestsRequest.Builder, SdkPojo, CopyableBuilder<Builder, AllTypesRequest> {
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
         * Sets the value of the ShortMember property for this object.
         *
         * @param shortMember
         *        The new value for the ShortMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder shortMember(Short shortMember);

        /**
         * Sets the value of the ByteMember property for this object.
         *
         * @param byteMember
         *        The new value for the ByteMember property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder byteMember(Byte byteMember);

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
        Builder listOfEnumsWithStrings(Collection<String> listOfEnums);

        /**
         * Sets the value of the ListOfEnums property for this object.
         *
         * @param listOfEnums
         *        The new value for the ListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfEnumsWithStrings(String... listOfEnums);

        /**
         * Sets the value of the ListOfEnums property for this object.
         *
         * @param listOfEnums
         *        The new value for the ListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfEnums(Collection<EnumType> listOfEnums);

        /**
         * Sets the value of the ListOfEnums property for this object.
         *
         * @param listOfEnums
         *        The new value for the ListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfEnums(EnumType... listOfEnums);

        /**
         * Sets the value of the ListOfMaps property for this object.
         *
         * @param listOfMaps
         *        The new value for the ListOfMaps property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMaps(Collection<? extends Map<String, String>> listOfMaps);

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
         * This is a convenience method that creates an instance of the
         * {@link software.amazon.awssdk.services.jsonprotocoltests.model.SimpleStruct.Builder} avoiding the need to
         * create one manually via
         * {@link software.amazon.awssdk.services.jsonprotocoltests.model.SimpleStruct#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes,
         * {@link software.amazon.awssdk.services.jsonprotocoltests.model.SimpleStruct.Builder#build()} is called
         * immediately and its result is passed to {@link #listOfStructs(List<SimpleStruct>)}.
         *
         * @param listOfStructs
         *        a consumer that will call methods on
         *        {@link software.amazon.awssdk.services.jsonprotocoltests.model.SimpleStruct.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #listOfStructs(java.util.Collection<SimpleStruct>)
         */
        Builder listOfStructs(Consumer<SimpleStruct.Builder>... listOfStructs);

        /**
         * Sets the value of the ListOfMapOfEnumToString property for this object.
         *
         * @param listOfMapOfEnumToString
         *        The new value for the ListOfMapOfEnumToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMapOfEnumToStringWithStrings(Collection<? extends Map<String, String>> listOfMapOfEnumToString);

        /**
         * Sets the value of the ListOfMapOfEnumToString property for this object.
         *
         * @param listOfMapOfEnumToString
         *        The new value for the ListOfMapOfEnumToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMapOfEnumToStringWithStrings(Map<String, String>... listOfMapOfEnumToString);

        /**
         * Sets the value of the ListOfMapOfStringToStruct property for this object.
         *
         * @param listOfMapOfStringToStruct
         *        The new value for the ListOfMapOfStringToStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMapOfStringToStruct(Collection<? extends Map<String, SimpleStruct>> listOfMapOfStringToStruct);

        /**
         * Sets the value of the ListOfMapOfStringToStruct property for this object.
         *
         * @param listOfMapOfStringToStruct
         *        The new value for the ListOfMapOfStringToStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfMapOfStringToStruct(Map<String, SimpleStruct>... listOfMapOfStringToStruct);

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
        Builder mapOfEnumToEnumWithStrings(Map<String, String> mapOfEnumToEnum);

        /**
         * Sets the value of the MapOfEnumToEnum property for this object.
         *
         * @param mapOfEnumToEnum
         *        The new value for the MapOfEnumToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToEnum(Map<EnumType, EnumType> mapOfEnumToEnum);

        /**
         * Sets the value of the MapOfEnumToString property for this object.
         *
         * @param mapOfEnumToString
         *        The new value for the MapOfEnumToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToStringWithStrings(Map<String, String> mapOfEnumToString);

        /**
         * Sets the value of the MapOfEnumToString property for this object.
         *
         * @param mapOfEnumToString
         *        The new value for the MapOfEnumToString property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToString(Map<EnumType, String> mapOfEnumToString);

        /**
         * Sets the value of the MapOfStringToEnum property for this object.
         *
         * @param mapOfStringToEnum
         *        The new value for the MapOfStringToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToEnumWithStrings(Map<String, String> mapOfStringToEnum);

        /**
         * Sets the value of the MapOfStringToEnum property for this object.
         *
         * @param mapOfStringToEnum
         *        The new value for the MapOfStringToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfStringToEnum(Map<String, EnumType> mapOfStringToEnum);

        /**
         * Sets the value of the MapOfEnumToSimpleStruct property for this object.
         *
         * @param mapOfEnumToSimpleStruct
         *        The new value for the MapOfEnumToSimpleStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToSimpleStructWithStrings(Map<String, SimpleStruct> mapOfEnumToSimpleStruct);

        /**
         * Sets the value of the MapOfEnumToSimpleStruct property for this object.
         *
         * @param mapOfEnumToSimpleStruct
         *        The new value for the MapOfEnumToSimpleStruct property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToSimpleStruct(Map<EnumType, SimpleStruct> mapOfEnumToSimpleStruct);

        /**
         * Sets the value of the MapOfEnumToListOfEnums property for this object.
         *
         * @param mapOfEnumToListOfEnums
         *        The new value for the MapOfEnumToListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToListOfEnumsWithStrings(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnums);

        /**
         * Sets the value of the MapOfEnumToListOfEnums property for this object.
         *
         * @param mapOfEnumToListOfEnums
         *        The new value for the MapOfEnumToListOfEnums property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToListOfEnums(Map<EnumType, ? extends Collection<EnumType>> mapOfEnumToListOfEnums);

        /**
         * Sets the value of the MapOfEnumToMapOfStringToEnum property for this object.
         *
         * @param mapOfEnumToMapOfStringToEnum
         *        The new value for the MapOfEnumToMapOfStringToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToMapOfStringToEnumWithStrings(Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnum);

        /**
         * Sets the value of the MapOfEnumToMapOfStringToEnum property for this object.
         *
         * @param mapOfEnumToMapOfStringToEnum
         *        The new value for the MapOfEnumToMapOfStringToEnum property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder mapOfEnumToMapOfStringToEnum(Map<EnumType, ? extends Map<String, EnumType>> mapOfEnumToMapOfStringToEnum);

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
         * This is a convenience method that creates an instance of the {@link StructWithTimestamp.Builder} avoiding the
         * need to create one manually via {@link StructWithTimestamp#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link StructWithTimestamp.Builder#build()} is called immediately and
         * its result is passed to {@link #structWithNestedTimestampMember(StructWithTimestamp)}.
         *
         * @param structWithNestedTimestampMember
         *        a consumer that will call methods on {@link StructWithTimestamp.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #structWithNestedTimestampMember(StructWithTimestamp)
         */
        default Builder structWithNestedTimestampMember(Consumer<StructWithTimestamp.Builder> structWithNestedTimestampMember) {
            return structWithNestedTimestampMember(StructWithTimestamp.builder().applyMutation(structWithNestedTimestampMember)
                                                                      .build());
        }

        /**
         * Sets the value of the BlobArg property for this object.
         *
         * @param blobArg
         *        The new value for the BlobArg property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobArg(SdkBytes blobArg);

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
         * This is a convenience method that creates an instance of the {@link StructWithNestedBlobType.Builder}
         * avoiding the need to create one manually via {@link StructWithNestedBlobType#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link StructWithNestedBlobType.Builder#build()} is called immediately
         * and its result is passed to {@link #structWithNestedBlob(StructWithNestedBlobType)}.
         *
         * @param structWithNestedBlob
         *        a consumer that will call methods on {@link StructWithNestedBlobType.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #structWithNestedBlob(StructWithNestedBlobType)
         */
        default Builder structWithNestedBlob(Consumer<StructWithNestedBlobType.Builder> structWithNestedBlob) {
            return structWithNestedBlob(StructWithNestedBlobType.builder().applyMutation(structWithNestedBlob).build());
        }

        /**
         * Sets the value of the BlobMap property for this object.
         *
         * @param blobMap
         *        The new value for the BlobMap property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder blobMap(Map<String, SdkBytes> blobMap);

        /**
         * Sets the value of the ListOfBlobs property for this object.
         *
         * @param listOfBlobs
         *        The new value for the ListOfBlobs property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(Collection<SdkBytes> listOfBlobs);

        /**
         * Sets the value of the ListOfBlobs property for this object.
         *
         * @param listOfBlobs
         *        The new value for the ListOfBlobs property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listOfBlobs(SdkBytes... listOfBlobs);

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
         * This is a convenience method that creates an instance of the {@link RecursiveStructType.Builder} avoiding the
         * need to create one manually via {@link RecursiveStructType#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link RecursiveStructType.Builder#build()} is called immediately and
         * its result is passed to {@link #recursiveStruct(RecursiveStructType)}.
         *
         * @param recursiveStruct
         *        a consumer that will call methods on {@link RecursiveStructType.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #recursiveStruct(RecursiveStructType)
         */
        default Builder recursiveStruct(Consumer<RecursiveStructType.Builder> recursiveStruct) {
            return recursiveStruct(RecursiveStructType.builder().applyMutation(recursiveStruct).build());
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
         * This is a convenience method that creates an instance of the {@link BaseType.Builder} avoiding the need to
         * create one manually via {@link BaseType#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link BaseType.Builder#build()} is called immediately and its result is
         * passed to {@link #polymorphicTypeWithSubTypes(BaseType)}.
         *
         * @param polymorphicTypeWithSubTypes
         *        a consumer that will call methods on {@link BaseType.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #polymorphicTypeWithSubTypes(BaseType)
         */
        default Builder polymorphicTypeWithSubTypes(Consumer<BaseType.Builder> polymorphicTypeWithSubTypes) {
            return polymorphicTypeWithSubTypes(BaseType.builder().applyMutation(polymorphicTypeWithSubTypes).build());
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
         * This is a convenience method that creates an instance of the {@link SubTypeOne.Builder} avoiding the need to
         * create one manually via {@link SubTypeOne#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link SubTypeOne.Builder#build()} is called immediately and its result
         * is passed to {@link #polymorphicTypeWithoutSubTypes(SubTypeOne)}.
         *
         * @param polymorphicTypeWithoutSubTypes
         *        a consumer that will call methods on {@link SubTypeOne.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #polymorphicTypeWithoutSubTypes(SubTypeOne)
         */
        default Builder polymorphicTypeWithoutSubTypes(Consumer<SubTypeOne.Builder> polymorphicTypeWithoutSubTypes) {
            return polymorphicTypeWithoutSubTypes(SubTypeOne.builder().applyMutation(polymorphicTypeWithoutSubTypes).build());
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

        /**
         * Sets the value of the Underscore_Name_Type property for this object.
         *
         * @param underscore_Name_Type
         *        The new value for the Underscore_Name_Type property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder underscore_Name_Type(Underscore_Name_Type underscore_Name_Type);

        /**
         * Sets the value of the Underscore_Name_Type property for this object.
         *
         * This is a convenience method that creates an instance of the {@link Underscore_Name_Type.Builder} avoiding
         * the need to create one manually via {@link Underscore_Name_Type#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link Underscore_Name_Type.Builder#build()} is called immediately and
         * its result is passed to {@link #underscore_Name_Type(Underscore_Name_Type)}.
         *
         * @param underscore_Name_Type
         *        a consumer that will call methods on {@link Underscore_Name_Type.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #underscore_Name_Type(Underscore_Name_Type)
         */
        default Builder underscore_Name_Type(Consumer<Underscore_Name_Type.Builder> underscore_Name_Type) {
            return underscore_Name_Type(Underscore_Name_Type.builder().applyMutation(underscore_Name_Type).build());
        }

        /**
         * Sets the value of the MyDocument property for this object.
         *
         * @param myDocument
         *        The new value for the MyDocument property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder myDocument(Document myDocument);

        /**
         * Sets the value of the AllTypesUnionStructure property for this object.
         *
         * @param allTypesUnionStructure
         *        The new value for the AllTypesUnionStructure property for this object.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder allTypesUnionStructure(AllTypesUnionStructure allTypesUnionStructure);

        /**
         * Sets the value of the AllTypesUnionStructure property for this object.
         *
         * This is a convenience method that creates an instance of the {@link AllTypesUnionStructure.Builder} avoiding
         * the need to create one manually via {@link AllTypesUnionStructure#builder()}.
         *
         * <p>
         * When the {@link Consumer} completes, {@link AllTypesUnionStructure.Builder#build()} is called immediately and
         * its result is passed to {@link #allTypesUnionStructure(AllTypesUnionStructure)}.
         *
         * @param allTypesUnionStructure
         *        a consumer that will call methods on {@link AllTypesUnionStructure.Builder}
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #allTypesUnionStructure(AllTypesUnionStructure)
         */
        default Builder allTypesUnionStructure(Consumer<AllTypesUnionStructure.Builder> allTypesUnionStructure) {
            return allTypesUnionStructure(AllTypesUnionStructure.builder().applyMutation(allTypesUnionStructure).build());
        }

        @Override
        Builder overrideConfiguration(AwsRequestOverrideConfiguration overrideConfiguration);

        @Override
        Builder overrideConfiguration(Consumer<AwsRequestOverrideConfiguration.Builder> builderConsumer);
    }

    static final class BuilderImpl extends JsonProtocolTestsRequest.BuilderImpl implements Builder {
        private String stringMember;

        private Integer integerMember;

        private Boolean booleanMember;

        private Float floatMember;

        private Double doubleMember;

        private Long longMember;

        private Short shortMember;

        private Byte byteMember;

        private List<String> simpleList = DefaultSdkAutoConstructList.getInstance();

        private List<String> listOfEnums = DefaultSdkAutoConstructList.getInstance();

        private List<Map<String, String>> listOfMaps = DefaultSdkAutoConstructList.getInstance();

        private List<SimpleStruct> listOfStructs = DefaultSdkAutoConstructList.getInstance();

        private List<Map<String, String>> listOfMapOfEnumToString = DefaultSdkAutoConstructList.getInstance();

        private List<Map<String, SimpleStruct>> listOfMapOfStringToStruct = DefaultSdkAutoConstructList.getInstance();

        private Map<String, List<Integer>> mapOfStringToIntegerList = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, String> mapOfStringToString = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, SimpleStruct> mapOfStringToSimpleStruct = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, String> mapOfEnumToEnum = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, String> mapOfEnumToString = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, String> mapOfStringToEnum = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, SimpleStruct> mapOfEnumToSimpleStruct = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, List<String>> mapOfEnumToListOfEnums = DefaultSdkAutoConstructMap.getInstance();

        private Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnum = DefaultSdkAutoConstructMap.getInstance();

        private Instant timestampMember;

        private StructWithTimestamp structWithNestedTimestampMember;

        private SdkBytes blobArg;

        private StructWithNestedBlobType structWithNestedBlob;

        private Map<String, SdkBytes> blobMap = DefaultSdkAutoConstructMap.getInstance();

        private List<SdkBytes> listOfBlobs = DefaultSdkAutoConstructList.getInstance();

        private RecursiveStructType recursiveStruct;

        private BaseType polymorphicTypeWithSubTypes;

        private SubTypeOne polymorphicTypeWithoutSubTypes;

        private String enumType;

        private Underscore_Name_Type underscore_Name_Type;

        private Document myDocument;

        private AllTypesUnionStructure allTypesUnionStructure;

        private BuilderImpl() {
        }

        private BuilderImpl(AllTypesRequest model) {
            super(model);
            stringMember(model.stringMember);
            integerMember(model.integerMember);
            booleanMember(model.booleanMember);
            floatMember(model.floatMember);
            doubleMember(model.doubleMember);
            longMember(model.longMember);
            shortMember(model.shortMember);
            byteMember(model.byteMember);
            simpleList(model.simpleList);
            listOfEnumsWithStrings(model.listOfEnums);
            listOfMaps(model.listOfMaps);
            listOfStructs(model.listOfStructs);
            listOfMapOfEnumToStringWithStrings(model.listOfMapOfEnumToString);
            listOfMapOfStringToStruct(model.listOfMapOfStringToStruct);
            mapOfStringToIntegerList(model.mapOfStringToIntegerList);
            mapOfStringToString(model.mapOfStringToString);
            mapOfStringToSimpleStruct(model.mapOfStringToSimpleStruct);
            mapOfEnumToEnumWithStrings(model.mapOfEnumToEnum);
            mapOfEnumToStringWithStrings(model.mapOfEnumToString);
            mapOfStringToEnumWithStrings(model.mapOfStringToEnum);
            mapOfEnumToSimpleStructWithStrings(model.mapOfEnumToSimpleStruct);
            mapOfEnumToListOfEnumsWithStrings(model.mapOfEnumToListOfEnums);
            mapOfEnumToMapOfStringToEnumWithStrings(model.mapOfEnumToMapOfStringToEnum);
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
            underscore_Name_Type(model.underscore_Name_Type);
            myDocument(model.myDocument);
            allTypesUnionStructure(model.allTypesUnionStructure);
        }

        public final String getStringMember() {
            return stringMember;
        }

        public final void setStringMember(String stringMember) {
            this.stringMember = stringMember;
        }

        @Override
        public final Builder stringMember(String stringMember) {
            this.stringMember = stringMember;
            return this;
        }

        public final Integer getIntegerMember() {
            return integerMember;
        }

        public final void setIntegerMember(Integer integerMember) {
            this.integerMember = integerMember;
        }

        @Override
        public final Builder integerMember(Integer integerMember) {
            this.integerMember = integerMember;
            return this;
        }

        public final Boolean getBooleanMember() {
            return booleanMember;
        }

        public final void setBooleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
        }

        @Override
        public final Builder booleanMember(Boolean booleanMember) {
            this.booleanMember = booleanMember;
            return this;
        }

        public final Float getFloatMember() {
            return floatMember;
        }

        public final void setFloatMember(Float floatMember) {
            this.floatMember = floatMember;
        }

        @Override
        public final Builder floatMember(Float floatMember) {
            this.floatMember = floatMember;
            return this;
        }

        public final Double getDoubleMember() {
            return doubleMember;
        }

        public final void setDoubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
        }

        @Override
        public final Builder doubleMember(Double doubleMember) {
            this.doubleMember = doubleMember;
            return this;
        }

        public final Long getLongMember() {
            return longMember;
        }

        public final void setLongMember(Long longMember) {
            this.longMember = longMember;
        }

        @Override
        public final Builder longMember(Long longMember) {
            this.longMember = longMember;
            return this;
        }

        public final Short getShortMember() {
            return shortMember;
        }

        public final void setShortMember(Short shortMember) {
            this.shortMember = shortMember;
        }

        @Override
        public final Builder shortMember(Short shortMember) {
            this.shortMember = shortMember;
            return this;
        }

        public final Byte getByteMember() {
            return byteMember;
        }

        public final void setByteMember(Byte byteMember) {
            this.byteMember = byteMember;
        }

        @Override
        public final Builder byteMember(Byte byteMember) {
            this.byteMember = byteMember;
            return this;
        }

        public final Collection<String> getSimpleList() {
            if (simpleList instanceof SdkAutoConstructList) {
                return null;
            }
            return simpleList;
        }

        public final void setSimpleList(Collection<String> simpleList) {
            this.simpleList = ListOfStringsCopier.copy(simpleList);
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

        public final Collection<String> getListOfEnums() {
            if (listOfEnums instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfEnums;
        }

        public final void setListOfEnums(Collection<String> listOfEnums) {
            this.listOfEnums = ListOfEnumsCopier.copy(listOfEnums);
        }

        @Override
        public final Builder listOfEnumsWithStrings(Collection<String> listOfEnums) {
            this.listOfEnums = ListOfEnumsCopier.copy(listOfEnums);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfEnumsWithStrings(String... listOfEnums) {
            listOfEnumsWithStrings(Arrays.asList(listOfEnums));
            return this;
        }

        @Override
        public final Builder listOfEnums(Collection<EnumType> listOfEnums) {
            this.listOfEnums = ListOfEnumsCopier.copyEnumToString(listOfEnums);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfEnums(EnumType... listOfEnums) {
            listOfEnums(Arrays.asList(listOfEnums));
            return this;
        }

        public final Collection<? extends Map<String, String>> getListOfMaps() {
            if (listOfMaps instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfMaps;
        }

        public final void setListOfMaps(Collection<? extends Map<String, String>> listOfMaps) {
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
        }

        @Override
        public final Builder listOfMaps(Collection<? extends Map<String, String>> listOfMaps) {
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfMaps(Map<String, String>... listOfMaps) {
            listOfMaps(Arrays.asList(listOfMaps));
            return this;
        }

        public final List<SimpleStruct.Builder> getListOfStructs() {
            List<SimpleStruct.Builder> result = ListOfSimpleStructsCopier.copyToBuilder(this.listOfStructs);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setListOfStructs(Collection<SimpleStruct.BuilderImpl> listOfStructs) {
            this.listOfStructs = ListOfSimpleStructsCopier.copyFromBuilder(listOfStructs);
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
            listOfStructs(Stream.of(listOfStructs).map(c -> SimpleStruct.builder().applyMutation(c).build())
                                .collect(Collectors.toList()));
            return this;
        }

        public final Collection<? extends Map<String, String>> getListOfMapOfEnumToString() {
            if (listOfMapOfEnumToString instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfMapOfEnumToString;
        }

        public final void setListOfMapOfEnumToString(Collection<? extends Map<String, String>> listOfMapOfEnumToString) {
            this.listOfMapOfEnumToString = ListOfMapOfEnumToStringCopier.copy(listOfMapOfEnumToString);
        }

        @Override
        public final Builder listOfMapOfEnumToStringWithStrings(Collection<? extends Map<String, String>> listOfMapOfEnumToString) {
            this.listOfMapOfEnumToString = ListOfMapOfEnumToStringCopier.copy(listOfMapOfEnumToString);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfMapOfEnumToStringWithStrings(Map<String, String>... listOfMapOfEnumToString) {
            listOfMapOfEnumToStringWithStrings(Arrays.asList(listOfMapOfEnumToString));
            return this;
        }

        public final List<Map<String, SimpleStruct.Builder>> getListOfMapOfStringToStruct() {
            List<Map<String, SimpleStruct.Builder>> result = ListOfMapOfStringToStructCopier
                .copyToBuilder(this.listOfMapOfStringToStruct);
            if (result instanceof SdkAutoConstructList) {
                return null;
            }
            return result;
        }

        public final void setListOfMapOfStringToStruct(
            Collection<? extends Map<String, SimpleStruct.BuilderImpl>> listOfMapOfStringToStruct) {
            this.listOfMapOfStringToStruct = ListOfMapOfStringToStructCopier.copyFromBuilder(listOfMapOfStringToStruct);
        }

        @Override
        public final Builder listOfMapOfStringToStruct(Collection<? extends Map<String, SimpleStruct>> listOfMapOfStringToStruct) {
            this.listOfMapOfStringToStruct = ListOfMapOfStringToStructCopier.copy(listOfMapOfStringToStruct);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfMapOfStringToStruct(Map<String, SimpleStruct>... listOfMapOfStringToStruct) {
            listOfMapOfStringToStruct(Arrays.asList(listOfMapOfStringToStruct));
            return this;
        }

        public final Map<String, ? extends Collection<Integer>> getMapOfStringToIntegerList() {
            if (mapOfStringToIntegerList instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToIntegerList;
        }

        public final void setMapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
        }

        @Override
        public final Builder mapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
            return this;
        }

        public final Map<String, String> getMapOfStringToString() {
            if (mapOfStringToString instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToString;
        }

        public final void setMapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
        }

        @Override
        public final Builder mapOfStringToString(Map<String, String> mapOfStringToString) {
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
            return this;
        }

        public final Map<String, SimpleStruct.Builder> getMapOfStringToSimpleStruct() {
            Map<String, SimpleStruct.Builder> result = MapOfStringToSimpleStructCopier
                .copyToBuilder(this.mapOfStringToSimpleStruct);
            if (result instanceof SdkAutoConstructMap) {
                return null;
            }
            return result;
        }

        public final void setMapOfStringToSimpleStruct(Map<String, SimpleStruct.BuilderImpl> mapOfStringToSimpleStruct) {
            this.mapOfStringToSimpleStruct = MapOfStringToSimpleStructCopier.copyFromBuilder(mapOfStringToSimpleStruct);
        }

        @Override
        public final Builder mapOfStringToSimpleStruct(Map<String, SimpleStruct> mapOfStringToSimpleStruct) {
            this.mapOfStringToSimpleStruct = MapOfStringToSimpleStructCopier.copy(mapOfStringToSimpleStruct);
            return this;
        }

        public final Map<String, String> getMapOfEnumToEnum() {
            if (mapOfEnumToEnum instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToEnum;
        }

        public final void setMapOfEnumToEnum(Map<String, String> mapOfEnumToEnum) {
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copy(mapOfEnumToEnum);
        }

        @Override
        public final Builder mapOfEnumToEnumWithStrings(Map<String, String> mapOfEnumToEnum) {
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copy(mapOfEnumToEnum);
            return this;
        }

        @Override
        public final Builder mapOfEnumToEnum(Map<EnumType, EnumType> mapOfEnumToEnum) {
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copyEnumToString(mapOfEnumToEnum);
            return this;
        }

        public final Map<String, String> getMapOfEnumToString() {
            if (mapOfEnumToString instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToString;
        }

        public final void setMapOfEnumToString(Map<String, String> mapOfEnumToString) {
            this.mapOfEnumToString = MapOfEnumToStringCopier.copy(mapOfEnumToString);
        }

        @Override
        public final Builder mapOfEnumToStringWithStrings(Map<String, String> mapOfEnumToString) {
            this.mapOfEnumToString = MapOfEnumToStringCopier.copy(mapOfEnumToString);
            return this;
        }

        @Override
        public final Builder mapOfEnumToString(Map<EnumType, String> mapOfEnumToString) {
            this.mapOfEnumToString = MapOfEnumToStringCopier.copyEnumToString(mapOfEnumToString);
            return this;
        }

        public final Map<String, String> getMapOfStringToEnum() {
            if (mapOfStringToEnum instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToEnum;
        }

        public final void setMapOfStringToEnum(Map<String, String> mapOfStringToEnum) {
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copy(mapOfStringToEnum);
        }

        @Override
        public final Builder mapOfStringToEnumWithStrings(Map<String, String> mapOfStringToEnum) {
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copy(mapOfStringToEnum);
            return this;
        }

        @Override
        public final Builder mapOfStringToEnum(Map<String, EnumType> mapOfStringToEnum) {
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copyEnumToString(mapOfStringToEnum);
            return this;
        }

        public final Map<String, SimpleStruct.Builder> getMapOfEnumToSimpleStruct() {
            Map<String, SimpleStruct.Builder> result = MapOfEnumToSimpleStructCopier.copyToBuilder(this.mapOfEnumToSimpleStruct);
            if (result instanceof SdkAutoConstructMap) {
                return null;
            }
            return result;
        }

        public final void setMapOfEnumToSimpleStruct(Map<String, SimpleStruct.BuilderImpl> mapOfEnumToSimpleStruct) {
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copyFromBuilder(mapOfEnumToSimpleStruct);
        }

        @Override
        public final Builder mapOfEnumToSimpleStructWithStrings(Map<String, SimpleStruct> mapOfEnumToSimpleStruct) {
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copy(mapOfEnumToSimpleStruct);
            return this;
        }

        @Override
        public final Builder mapOfEnumToSimpleStruct(Map<EnumType, SimpleStruct> mapOfEnumToSimpleStruct) {
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copyEnumToString(mapOfEnumToSimpleStruct);
            return this;
        }

        public final Map<String, ? extends Collection<String>> getMapOfEnumToListOfEnums() {
            if (mapOfEnumToListOfEnums instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToListOfEnums;
        }

        public final void setMapOfEnumToListOfEnums(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnums) {
            this.mapOfEnumToListOfEnums = MapOfEnumToListOfEnumsCopier.copy(mapOfEnumToListOfEnums);
        }

        @Override
        public final Builder mapOfEnumToListOfEnumsWithStrings(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnums) {
            this.mapOfEnumToListOfEnums = MapOfEnumToListOfEnumsCopier.copy(mapOfEnumToListOfEnums);
            return this;
        }

        @Override
        public final Builder mapOfEnumToListOfEnums(Map<EnumType, ? extends Collection<EnumType>> mapOfEnumToListOfEnums) {
            this.mapOfEnumToListOfEnums = MapOfEnumToListOfEnumsCopier.copyEnumToString(mapOfEnumToListOfEnums);
            return this;
        }

        public final Map<String, ? extends Map<String, String>> getMapOfEnumToMapOfStringToEnum() {
            if (mapOfEnumToMapOfStringToEnum instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToMapOfStringToEnum;
        }

        public final void setMapOfEnumToMapOfStringToEnum(Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnum) {
            this.mapOfEnumToMapOfStringToEnum = MapOfEnumToMapOfStringToEnumCopier.copy(mapOfEnumToMapOfStringToEnum);
        }

        @Override
        public final Builder mapOfEnumToMapOfStringToEnumWithStrings(
            Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnum) {
            this.mapOfEnumToMapOfStringToEnum = MapOfEnumToMapOfStringToEnumCopier.copy(mapOfEnumToMapOfStringToEnum);
            return this;
        }

        @Override
        public final Builder mapOfEnumToMapOfStringToEnum(
            Map<EnumType, ? extends Map<String, EnumType>> mapOfEnumToMapOfStringToEnum) {
            this.mapOfEnumToMapOfStringToEnum = MapOfEnumToMapOfStringToEnumCopier.copyEnumToString(mapOfEnumToMapOfStringToEnum);
            return this;
        }

        public final Instant getTimestampMember() {
            return timestampMember;
        }

        public final void setTimestampMember(Instant timestampMember) {
            this.timestampMember = timestampMember;
        }

        @Override
        public final Builder timestampMember(Instant timestampMember) {
            this.timestampMember = timestampMember;
            return this;
        }

        public final StructWithTimestamp.Builder getStructWithNestedTimestampMember() {
            return structWithNestedTimestampMember != null ? structWithNestedTimestampMember.toBuilder() : null;
        }

        public final void setStructWithNestedTimestampMember(StructWithTimestamp.BuilderImpl structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember != null ? structWithNestedTimestampMember
                .build() : null;
        }

        @Override
        public final Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
            return this;
        }

        public final ByteBuffer getBlobArg() {
            return blobArg == null ? null : blobArg.asByteBuffer();
        }

        public final void setBlobArg(ByteBuffer blobArg) {
            blobArg(blobArg == null ? null : SdkBytes.fromByteBuffer(blobArg));
        }

        @Override
        public final Builder blobArg(SdkBytes blobArg) {
            this.blobArg = blobArg;
            return this;
        }

        public final StructWithNestedBlobType.Builder getStructWithNestedBlob() {
            return structWithNestedBlob != null ? structWithNestedBlob.toBuilder() : null;
        }

        public final void setStructWithNestedBlob(StructWithNestedBlobType.BuilderImpl structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob != null ? structWithNestedBlob.build() : null;
        }

        @Override
        public final Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            this.structWithNestedBlob = structWithNestedBlob;
            return this;
        }

        public final Map<String, ByteBuffer> getBlobMap() {
            if (blobMap instanceof SdkAutoConstructMap) {
                return null;
            }
            return blobMap == null ? null : blobMap.entrySet().stream()
                                                   .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().asByteBuffer()));
        }

        public final void setBlobMap(Map<String, ByteBuffer> blobMap) {
            blobMap(blobMap == null ? null : blobMap.entrySet().stream()
                                                    .collect(Collectors.toMap(e -> e.getKey(), e -> SdkBytes.fromByteBuffer(e.getValue()))));
        }

        @Override
        public final Builder blobMap(Map<String, SdkBytes> blobMap) {
            this.blobMap = BlobMapTypeCopier.copy(blobMap);
            return this;
        }

        public final List<ByteBuffer> getListOfBlobs() {
            if (listOfBlobs instanceof SdkAutoConstructList) {
                return null;
            }
            return listOfBlobs == null ? null : listOfBlobs.stream().map(SdkBytes::asByteBuffer).collect(Collectors.toList());
        }

        public final void setListOfBlobs(Collection<ByteBuffer> listOfBlobs) {
            listOfBlobs(listOfBlobs == null ? null : listOfBlobs.stream().map(SdkBytes::fromByteBuffer)
                                                                .collect(Collectors.toList()));
        }

        @Override
        public final Builder listOfBlobs(Collection<SdkBytes> listOfBlobs) {
            this.listOfBlobs = ListOfBlobsTypeCopier.copy(listOfBlobs);
            return this;
        }

        @Override
        @SafeVarargs
        public final Builder listOfBlobs(SdkBytes... listOfBlobs) {
            listOfBlobs(Arrays.asList(listOfBlobs));
            return this;
        }

        public final RecursiveStructType.Builder getRecursiveStruct() {
            return recursiveStruct != null ? recursiveStruct.toBuilder() : null;
        }

        public final void setRecursiveStruct(RecursiveStructType.BuilderImpl recursiveStruct) {
            this.recursiveStruct = recursiveStruct != null ? recursiveStruct.build() : null;
        }

        @Override
        public final Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            this.recursiveStruct = recursiveStruct;
            return this;
        }

        public final BaseType.Builder getPolymorphicTypeWithSubTypes() {
            return polymorphicTypeWithSubTypes != null ? polymorphicTypeWithSubTypes.toBuilder() : null;
        }

        public final void setPolymorphicTypeWithSubTypes(BaseType.BuilderImpl polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes != null ? polymorphicTypeWithSubTypes.build() : null;
        }

        @Override
        public final Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
            return this;
        }

        public final SubTypeOne.Builder getPolymorphicTypeWithoutSubTypes() {
            return polymorphicTypeWithoutSubTypes != null ? polymorphicTypeWithoutSubTypes.toBuilder() : null;
        }

        public final void setPolymorphicTypeWithoutSubTypes(SubTypeOne.BuilderImpl polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes != null ? polymorphicTypeWithoutSubTypes.build()
                                                                                         : null;
        }

        @Override
        public final Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
            return this;
        }

        public final String getEnumType() {
            return enumType;
        }

        public final void setEnumType(String enumType) {
            this.enumType = enumType;
        }

        @Override
        public final Builder enumType(String enumType) {
            this.enumType = enumType;
            return this;
        }

        @Override
        public final Builder enumType(EnumType enumType) {
            this.enumType(enumType == null ? null : enumType.toString());
            return this;
        }

        public final Underscore_Name_Type.Builder getUnderscore_Name_Type() {
            return underscore_Name_Type != null ? underscore_Name_Type.toBuilder() : null;
        }

        public final void setUnderscore_Name_Type(Underscore_Name_Type.BuilderImpl underscore_Name_Type) {
            this.underscore_Name_Type = underscore_Name_Type != null ? underscore_Name_Type.build() : null;
        }

        @Override
        public final Builder underscore_Name_Type(Underscore_Name_Type underscore_Name_Type) {
            this.underscore_Name_Type = underscore_Name_Type;
            return this;
        }

        public final Document getMyDocument() {
            return myDocument;
        }

        public final void setMyDocument(Document myDocument) {
            this.myDocument = myDocument;
        }

        @Override
        public final Builder myDocument(Document myDocument) {
            this.myDocument = myDocument;
            return this;
        }

        public final AllTypesUnionStructure.Builder getAllTypesUnionStructure() {
            return allTypesUnionStructure != null ? allTypesUnionStructure.toBuilder() : null;
        }

        public final void setAllTypesUnionStructure(AllTypesUnionStructure.BuilderImpl allTypesUnionStructure) {
            this.allTypesUnionStructure = allTypesUnionStructure != null ? allTypesUnionStructure.build() : null;
        }

        @Override
        public final Builder allTypesUnionStructure(AllTypesUnionStructure allTypesUnionStructure) {
            this.allTypesUnionStructure = allTypesUnionStructure;
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
        public AllTypesRequest build() {
            return new AllTypesRequest(this);
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
