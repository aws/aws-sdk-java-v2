package software.amazon.awssdk.services.jsonprotocoltests.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.Generated;
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
public final class AllTypesUnionStructure implements SdkPojo, Serializable,
                                                     ToCopyableBuilder<AllTypesUnionStructure.Builder, AllTypesUnionStructure> {
    private static final SdkField<String> STRING_MEMBER_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                        .memberName("StringMember").getter(getter(AllTypesUnionStructure::stringMember))
                                                                        .setter(setter(Builder::stringMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StringMember").build()).build();

    private static final SdkField<Integer> INTEGER_MEMBER_FIELD = SdkField.<Integer> builder(MarshallingType.INTEGER)
                                                                          .memberName("IntegerMember").getter(getter(AllTypesUnionStructure::integerMember))
                                                                          .setter(setter(Builder::integerMember))
                                                                          .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("IntegerMember").build()).build();

    private static final SdkField<Boolean> BOOLEAN_MEMBER_FIELD = SdkField.<Boolean> builder(MarshallingType.BOOLEAN)
                                                                          .memberName("BooleanMember").getter(getter(AllTypesUnionStructure::booleanMember))
                                                                          .setter(setter(Builder::booleanMember))
                                                                          .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BooleanMember").build()).build();

    private static final SdkField<Float> FLOAT_MEMBER_FIELD = SdkField.<Float> builder(MarshallingType.FLOAT)
                                                                      .memberName("FloatMember").getter(getter(AllTypesUnionStructure::floatMember)).setter(setter(Builder::floatMember))
                                                                      .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("FloatMember").build()).build();

    private static final SdkField<Double> DOUBLE_MEMBER_FIELD = SdkField.<Double> builder(MarshallingType.DOUBLE)
                                                                        .memberName("DoubleMember").getter(getter(AllTypesUnionStructure::doubleMember))
                                                                        .setter(setter(Builder::doubleMember))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("DoubleMember").build()).build();

    private static final SdkField<Long> LONG_MEMBER_FIELD = SdkField.<Long> builder(MarshallingType.LONG)
                                                                    .memberName("LongMember").getter(getter(AllTypesUnionStructure::longMember)).setter(setter(Builder::longMember))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("LongMember").build()).build();

    private static final SdkField<Short> SHORT_MEMBER_FIELD = SdkField.<Short> builder(MarshallingType.SHORT)
                                                                      .memberName("ShortMember").getter(getter(AllTypesUnionStructure::shortMember)).setter(setter(Builder::shortMember))
                                                                      .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("ShortMember").build()).build();

    private static final SdkField<List<String>> SIMPLE_LIST_FIELD = SdkField
        .<List<String>> builder(MarshallingType.LIST)
        .memberName("SimpleList")
        .getter(getter(AllTypesUnionStructure::simpleList))
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
        .getter(getter(AllTypesUnionStructure::listOfEnumsAsStrings))
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
        .getter(getter(AllTypesUnionStructure::listOfMaps))
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
        .getter(getter(AllTypesUnionStructure::listOfStructs))
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
        .getter(getter(AllTypesUnionStructure::listOfMapOfEnumToStringAsStrings))
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
        .getter(getter(AllTypesUnionStructure::listOfMapOfStringToStruct))
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
        .getter(getter(AllTypesUnionStructure::mapOfStringToIntegerList))
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
        .getter(getter(AllTypesUnionStructure::mapOfStringToString))
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
        .getter(getter(AllTypesUnionStructure::mapOfStringToSimpleStruct))
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
        .getter(getter(AllTypesUnionStructure::mapOfEnumToEnumAsStrings))
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
        .getter(getter(AllTypesUnionStructure::mapOfEnumToStringAsStrings))
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
        .getter(getter(AllTypesUnionStructure::mapOfStringToEnumAsStrings))
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
        .getter(getter(AllTypesUnionStructure::mapOfEnumToSimpleStructAsStrings))
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
        .getter(getter(AllTypesUnionStructure::mapOfEnumToListOfEnumsAsStrings))
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
        .getter(getter(AllTypesUnionStructure::mapOfEnumToMapOfStringToEnumAsStrings))
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
                                                                            .memberName("TimestampMember").getter(getter(AllTypesUnionStructure::timestampMember))
                                                                            .setter(setter(Builder::timestampMember))
                                                                            .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("TimestampMember").build()).build();

    private static final SdkField<StructWithTimestamp> STRUCT_WITH_NESTED_TIMESTAMP_MEMBER_FIELD = SdkField
        .<StructWithTimestamp> builder(MarshallingType.SDK_POJO)
        .memberName("StructWithNestedTimestampMember")
        .getter(getter(AllTypesUnionStructure::structWithNestedTimestampMember))
        .setter(setter(Builder::structWithNestedTimestampMember))
        .constructor(StructWithTimestamp::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StructWithNestedTimestampMember")
                             .build()).build();

    private static final SdkField<SdkBytes> BLOB_ARG_FIELD = SdkField.<SdkBytes> builder(MarshallingType.SDK_BYTES)
                                                                     .memberName("BlobArg").getter(getter(AllTypesUnionStructure::blobArg)).setter(setter(Builder::blobArg))
                                                                     .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("BlobArg").build()).build();

    private static final SdkField<StructWithNestedBlobType> STRUCT_WITH_NESTED_BLOB_FIELD = SdkField
        .<StructWithNestedBlobType> builder(MarshallingType.SDK_POJO).memberName("StructWithNestedBlob")
        .getter(getter(AllTypesUnionStructure::structWithNestedBlob)).setter(setter(Builder::structWithNestedBlob))
        .constructor(StructWithNestedBlobType::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("StructWithNestedBlob").build())
        .build();

    private static final SdkField<Map<String, SdkBytes>> BLOB_MAP_FIELD = SdkField
        .<Map<String, SdkBytes>> builder(MarshallingType.MAP)
        .memberName("BlobMap")
        .getter(getter(AllTypesUnionStructure::blobMap))
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
        .getter(getter(AllTypesUnionStructure::listOfBlobs))
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
        .getter(getter(AllTypesUnionStructure::recursiveStruct)).setter(setter(Builder::recursiveStruct))
        .constructor(RecursiveStructType::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("RecursiveStruct").build()).build();

    private static final SdkField<BaseType> POLYMORPHIC_TYPE_WITH_SUB_TYPES_FIELD = SdkField
        .<BaseType> builder(MarshallingType.SDK_POJO)
        .memberName("PolymorphicTypeWithSubTypes")
        .getter(getter(AllTypesUnionStructure::polymorphicTypeWithSubTypes))
        .setter(setter(Builder::polymorphicTypeWithSubTypes))
        .constructor(BaseType::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("PolymorphicTypeWithSubTypes")
                             .build()).build();

    private static final SdkField<SubTypeOne> POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES_FIELD = SdkField
        .<SubTypeOne> builder(MarshallingType.SDK_POJO)
        .memberName("PolymorphicTypeWithoutSubTypes")
        .getter(getter(AllTypesUnionStructure::polymorphicTypeWithoutSubTypes))
        .setter(setter(Builder::polymorphicTypeWithoutSubTypes))
        .constructor(SubTypeOne::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("PolymorphicTypeWithoutSubTypes")
                             .build()).build();

    private static final SdkField<String> ENUM_TYPE_FIELD = SdkField.<String> builder(MarshallingType.STRING)
                                                                    .memberName("EnumType").getter(getter(AllTypesUnionStructure::enumTypeAsString)).setter(setter(Builder::enumType))
                                                                    .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("EnumType").build()).build();

    private static final SdkField<Underscore_Name_Type> UNDERSCORE_NAME_TYPE_FIELD = SdkField
        .<Underscore_Name_Type> builder(MarshallingType.SDK_POJO).memberName("Underscore_Name_Type")
        .getter(getter(AllTypesUnionStructure::underscore_Name_Type)).setter(setter(Builder::underscore_Name_Type))
        .constructor(Underscore_Name_Type::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("Underscore_Name_Type").build())
        .build();

    private static final SdkField<Document> MY_DOCUMENT_FIELD = SdkField.<Document> builder(MarshallingType.DOCUMENT)
                                                                        .memberName("MyDocument").getter(getter(AllTypesUnionStructure::myDocument)).setter(setter(Builder::myDocument))
                                                                        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("MyDocument").build()).build();

    private static final SdkField<AllTypesUnionStructure> ALL_TYPES_UNION_STRUCTURE_FIELD = SdkField
        .<AllTypesUnionStructure> builder(MarshallingType.SDK_POJO).memberName("AllTypesUnionStructure")
        .getter(getter(AllTypesUnionStructure::allTypesUnionStructure)).setter(setter(Builder::allTypesUnionStructure))
        .constructor(AllTypesUnionStructure::builder)
        .traits(LocationTrait.builder().location(MarshallLocation.PAYLOAD).locationName("AllTypesUnionStructure").build())
        .build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(Arrays.asList(STRING_MEMBER_FIELD,
                                                                                                   INTEGER_MEMBER_FIELD, BOOLEAN_MEMBER_FIELD, FLOAT_MEMBER_FIELD, DOUBLE_MEMBER_FIELD, LONG_MEMBER_FIELD,
                                                                                                   SHORT_MEMBER_FIELD, SIMPLE_LIST_FIELD, LIST_OF_ENUMS_FIELD, LIST_OF_MAPS_FIELD, LIST_OF_STRUCTS_FIELD,
                                                                                                   LIST_OF_MAP_OF_ENUM_TO_STRING_FIELD, LIST_OF_MAP_OF_STRING_TO_STRUCT_FIELD, MAP_OF_STRING_TO_INTEGER_LIST_FIELD,
                                                                                                   MAP_OF_STRING_TO_STRING_FIELD, MAP_OF_STRING_TO_SIMPLE_STRUCT_FIELD, MAP_OF_ENUM_TO_ENUM_FIELD,
                                                                                                   MAP_OF_ENUM_TO_STRING_FIELD, MAP_OF_STRING_TO_ENUM_FIELD, MAP_OF_ENUM_TO_SIMPLE_STRUCT_FIELD,
                                                                                                   MAP_OF_ENUM_TO_LIST_OF_ENUMS_FIELD, MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM_FIELD, TIMESTAMP_MEMBER_FIELD,
                                                                                                   STRUCT_WITH_NESTED_TIMESTAMP_MEMBER_FIELD, BLOB_ARG_FIELD, STRUCT_WITH_NESTED_BLOB_FIELD, BLOB_MAP_FIELD,
                                                                                                   LIST_OF_BLOBS_FIELD, RECURSIVE_STRUCT_FIELD, POLYMORPHIC_TYPE_WITH_SUB_TYPES_FIELD,
                                                                                                   POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES_FIELD, ENUM_TYPE_FIELD, UNDERSCORE_NAME_TYPE_FIELD, MY_DOCUMENT_FIELD,
                                                                                                   ALL_TYPES_UNION_STRUCTURE_FIELD));

    private static final long serialVersionUID = 1L;

    private final String stringMember;

    private final Integer integerMember;

    private final Boolean booleanMember;

    private final Float floatMember;

    private final Double doubleMember;

    private final Long longMember;

    private final Short shortMember;

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

    private final Type type;

    private AllTypesUnionStructure(BuilderImpl builder) {
        this.stringMember = builder.stringMember;
        this.integerMember = builder.integerMember;
        this.booleanMember = builder.booleanMember;
        this.floatMember = builder.floatMember;
        this.doubleMember = builder.doubleMember;
        this.longMember = builder.longMember;
        this.shortMember = builder.shortMember;
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
        this.type = builder.type;
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
        hashCode = 31 * hashCode + Objects.hashCode(stringMember());
        hashCode = 31 * hashCode + Objects.hashCode(integerMember());
        hashCode = 31 * hashCode + Objects.hashCode(booleanMember());
        hashCode = 31 * hashCode + Objects.hashCode(floatMember());
        hashCode = 31 * hashCode + Objects.hashCode(doubleMember());
        hashCode = 31 * hashCode + Objects.hashCode(longMember());
        hashCode = 31 * hashCode + Objects.hashCode(shortMember());
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
        if (!(obj instanceof AllTypesUnionStructure)) {
            return false;
        }
        AllTypesUnionStructure other = (AllTypesUnionStructure) obj;
        return Objects.equals(stringMember(), other.stringMember()) && Objects.equals(integerMember(), other.integerMember())
               && Objects.equals(booleanMember(), other.booleanMember()) && Objects.equals(floatMember(), other.floatMember())
               && Objects.equals(doubleMember(), other.doubleMember()) && Objects.equals(longMember(), other.longMember())
               && Objects.equals(shortMember(), other.shortMember()) && hasSimpleList() == other.hasSimpleList()
               && Objects.equals(simpleList(), other.simpleList()) && hasListOfEnums() == other.hasListOfEnums()
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
            .builder("AllTypesUnionStructure")
            .add("StringMember", stringMember())
            .add("IntegerMember", integerMember())
            .add("BooleanMember", booleanMember())
            .add("FloatMember", floatMember())
            .add("DoubleMember", doubleMember())
            .add("LongMember", longMember())
            .add("ShortMember", shortMember())
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

    /**
     * Create an instance of this class with {@link #stringMember()} initialized to the given value.
     *
     * Sets the value of the StringMember property for this object.
     *
     * @param stringMember
     *        The new value for the StringMember property for this object.
     */
    public static AllTypesUnionStructure fromStringMember(String stringMember) {
        return builder().stringMember(stringMember).build();
    }

    /**
     * Create an instance of this class with {@link #integerMember()} initialized to the given value.
     *
     * Sets the value of the IntegerMember property for this object.
     *
     * @param integerMember
     *        The new value for the IntegerMember property for this object.
     */
    public static AllTypesUnionStructure fromIntegerMember(Integer integerMember) {
        return builder().integerMember(integerMember).build();
    }

    /**
     * Create an instance of this class with {@link #booleanMember()} initialized to the given value.
     *
     * Sets the value of the BooleanMember property for this object.
     *
     * @param booleanMember
     *        The new value for the BooleanMember property for this object.
     */
    public static AllTypesUnionStructure fromBooleanMember(Boolean booleanMember) {
        return builder().booleanMember(booleanMember).build();
    }

    /**
     * Create an instance of this class with {@link #floatMember()} initialized to the given value.
     *
     * Sets the value of the FloatMember property for this object.
     *
     * @param floatMember
     *        The new value for the FloatMember property for this object.
     */
    public static AllTypesUnionStructure fromFloatMember(Float floatMember) {
        return builder().floatMember(floatMember).build();
    }

    /**
     * Create an instance of this class with {@link #doubleMember()} initialized to the given value.
     *
     * Sets the value of the DoubleMember property for this object.
     *
     * @param doubleMember
     *        The new value for the DoubleMember property for this object.
     */
    public static AllTypesUnionStructure fromDoubleMember(Double doubleMember) {
        return builder().doubleMember(doubleMember).build();
    }

    /**
     * Create an instance of this class with {@link #longMember()} initialized to the given value.
     *
     * Sets the value of the LongMember property for this object.
     *
     * @param longMember
     *        The new value for the LongMember property for this object.
     */
    public static AllTypesUnionStructure fromLongMember(Long longMember) {
        return builder().longMember(longMember).build();
    }

    /**
     * Create an instance of this class with {@link #shortMember()} initialized to the given value.
     *
     * Sets the value of the ShortMember property for this object.
     *
     * @param shortMember
     *        The new value for the ShortMember property for this object.
     */
    public static AllTypesUnionStructure fromShortMember(Short shortMember) {
        return builder().shortMember(shortMember).build();
    }

    /**
     * Create an instance of this class with {@link #simpleList()} initialized to the given value.
     *
     * Sets the value of the SimpleList property for this object.
     *
     * @param simpleList
     *        The new value for the SimpleList property for this object.
     */
    public static AllTypesUnionStructure fromSimpleList(List<String> simpleList) {
        return builder().simpleList(simpleList).build();
    }

    /**
     * Create an instance of this class with {@link #listOfEnumsAsStrings()} initialized to the given value.
     *
     * Sets the value of the ListOfEnums property for this object.
     *
     * @param listOfEnums
     *        The new value for the ListOfEnums property for this object.
     */
    public static AllTypesUnionStructure fromListOfEnumsWithStrings(List<String> listOfEnumsWithStrings) {
        return builder().listOfEnumsWithStrings(listOfEnumsWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #listOfEnumsAsStrings()} initialized to the given value.
     *
     * Sets the value of the ListOfEnums property for this object.
     *
     * @param listOfEnums
     *        The new value for the ListOfEnums property for this object.
     */
    public static AllTypesUnionStructure fromListOfEnums(List<EnumType> listOfEnumsWithStrings) {
        return builder().listOfEnums(listOfEnumsWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #listOfMaps()} initialized to the given value.
     *
     * Sets the value of the ListOfMaps property for this object.
     *
     * @param listOfMaps
     *        The new value for the ListOfMaps property for this object.
     */
    public static AllTypesUnionStructure fromListOfMaps(List<Map<String, String>> listOfMaps) {
        return builder().listOfMaps(listOfMaps).build();
    }

    /**
     * Create an instance of this class with {@link #listOfStructs()} initialized to the given value.
     *
     * Sets the value of the ListOfStructs property for this object.
     *
     * @param listOfStructs
     *        The new value for the ListOfStructs property for this object.
     */
    public static AllTypesUnionStructure fromListOfStructs(List<SimpleStruct> listOfStructs) {
        return builder().listOfStructs(listOfStructs).build();
    }

    /**
     * Create an instance of this class with {@link #listOfMapOfEnumToStringAsStrings()} initialized to the given value.
     *
     * Sets the value of the ListOfMapOfEnumToString property for this object.
     *
     * @param listOfMapOfEnumToString
     *        The new value for the ListOfMapOfEnumToString property for this object.
     */
    public static AllTypesUnionStructure fromListOfMapOfEnumToStringWithStrings(
        List<Map<String, String>> listOfMapOfEnumToStringWithStrings) {
        return builder().listOfMapOfEnumToStringWithStrings(listOfMapOfEnumToStringWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #listOfMapOfEnumToStringAsStrings()} initialized to the given value.
     *
     * Sets the value of the ListOfMapOfEnumToString property for this object.
     *
     * @param listOfMapOfEnumToString
     *        The new value for the ListOfMapOfEnumToString property for this object.
     */
    public static AllTypesUnionStructure fromListOfMapOfEnumToString(
        List<Map<EnumType, String>> listOfMapOfEnumToStringWithStrings) {
        return builder().listOfMapOfEnumToString(listOfMapOfEnumToStringWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #listOfMapOfStringToStruct()} initialized to the given value.
     *
     * Sets the value of the ListOfMapOfStringToStruct property for this object.
     *
     * @param listOfMapOfStringToStruct
     *        The new value for the ListOfMapOfStringToStruct property for this object.
     */
    public static AllTypesUnionStructure fromListOfMapOfStringToStruct(List<Map<String, SimpleStruct>> listOfMapOfStringToStruct) {
        return builder().listOfMapOfStringToStruct(listOfMapOfStringToStruct).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfStringToIntegerList()} initialized to the given value.
     *
     * Sets the value of the MapOfStringToIntegerList property for this object.
     *
     * @param mapOfStringToIntegerList
     *        The new value for the MapOfStringToIntegerList property for this object.
     */
    public static AllTypesUnionStructure fromMapOfStringToIntegerList(Map<String, List<Integer>> mapOfStringToIntegerList) {
        return builder().mapOfStringToIntegerList(mapOfStringToIntegerList).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfStringToString()} initialized to the given value.
     *
     * Sets the value of the MapOfStringToString property for this object.
     *
     * @param mapOfStringToString
     *        The new value for the MapOfStringToString property for this object.
     */
    public static AllTypesUnionStructure fromMapOfStringToString(Map<String, String> mapOfStringToString) {
        return builder().mapOfStringToString(mapOfStringToString).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfStringToSimpleStruct()} initialized to the given value.
     *
     * Sets the value of the MapOfStringToSimpleStruct property for this object.
     *
     * @param mapOfStringToSimpleStruct
     *        The new value for the MapOfStringToSimpleStruct property for this object.
     */
    public static AllTypesUnionStructure fromMapOfStringToSimpleStruct(Map<String, SimpleStruct> mapOfStringToSimpleStruct) {
        return builder().mapOfStringToSimpleStruct(mapOfStringToSimpleStruct).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToEnumAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToEnum property for this object.
     *
     * @param mapOfEnumToEnum
     *        The new value for the MapOfEnumToEnum property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToEnumWithStrings(Map<String, String> mapOfEnumToEnumWithStrings) {
        return builder().mapOfEnumToEnumWithStrings(mapOfEnumToEnumWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToEnumAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToEnum property for this object.
     *
     * @param mapOfEnumToEnum
     *        The new value for the MapOfEnumToEnum property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToEnum(Map<EnumType, EnumType> mapOfEnumToEnumWithStrings) {
        return builder().mapOfEnumToEnum(mapOfEnumToEnumWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToStringAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToString property for this object.
     *
     * @param mapOfEnumToString
     *        The new value for the MapOfEnumToString property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToStringWithStrings(Map<String, String> mapOfEnumToStringWithStrings) {
        return builder().mapOfEnumToStringWithStrings(mapOfEnumToStringWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToStringAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToString property for this object.
     *
     * @param mapOfEnumToString
     *        The new value for the MapOfEnumToString property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToString(Map<EnumType, String> mapOfEnumToStringWithStrings) {
        return builder().mapOfEnumToString(mapOfEnumToStringWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfStringToEnumAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfStringToEnum property for this object.
     *
     * @param mapOfStringToEnum
     *        The new value for the MapOfStringToEnum property for this object.
     */
    public static AllTypesUnionStructure fromMapOfStringToEnumWithStrings(Map<String, String> mapOfStringToEnumWithStrings) {
        return builder().mapOfStringToEnumWithStrings(mapOfStringToEnumWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfStringToEnumAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfStringToEnum property for this object.
     *
     * @param mapOfStringToEnum
     *        The new value for the MapOfStringToEnum property for this object.
     */
    public static AllTypesUnionStructure fromMapOfStringToEnum(Map<String, EnumType> mapOfStringToEnumWithStrings) {
        return builder().mapOfStringToEnum(mapOfStringToEnumWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToSimpleStructAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToSimpleStruct property for this object.
     *
     * @param mapOfEnumToSimpleStruct
     *        The new value for the MapOfEnumToSimpleStruct property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToSimpleStructWithStrings(
        Map<String, SimpleStruct> mapOfEnumToSimpleStructWithStrings) {
        return builder().mapOfEnumToSimpleStructWithStrings(mapOfEnumToSimpleStructWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToSimpleStructAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToSimpleStruct property for this object.
     *
     * @param mapOfEnumToSimpleStruct
     *        The new value for the MapOfEnumToSimpleStruct property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToSimpleStruct(
        Map<EnumType, SimpleStruct> mapOfEnumToSimpleStructWithStrings) {
        return builder().mapOfEnumToSimpleStruct(mapOfEnumToSimpleStructWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToListOfEnumsAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToListOfEnums property for this object.
     *
     * @param mapOfEnumToListOfEnums
     *        The new value for the MapOfEnumToListOfEnums property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToListOfEnumsWithStrings(
        Map<String, List<String>> mapOfEnumToListOfEnumsWithStrings) {
        return builder().mapOfEnumToListOfEnumsWithStrings(mapOfEnumToListOfEnumsWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToListOfEnumsAsStrings()} initialized to the given value.
     *
     * Sets the value of the MapOfEnumToListOfEnums property for this object.
     *
     * @param mapOfEnumToListOfEnums
     *        The new value for the MapOfEnumToListOfEnums property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToListOfEnums(
        Map<EnumType, List<EnumType>> mapOfEnumToListOfEnumsWithStrings) {
        return builder().mapOfEnumToListOfEnums(mapOfEnumToListOfEnumsWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToMapOfStringToEnumAsStrings()} initialized to the given
     * value.
     *
     * Sets the value of the MapOfEnumToMapOfStringToEnum property for this object.
     *
     * @param mapOfEnumToMapOfStringToEnum
     *        The new value for the MapOfEnumToMapOfStringToEnum property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToMapOfStringToEnumWithStrings(
        Map<String, Map<String, String>> mapOfEnumToMapOfStringToEnumWithStrings) {
        return builder().mapOfEnumToMapOfStringToEnumWithStrings(mapOfEnumToMapOfStringToEnumWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #mapOfEnumToMapOfStringToEnumAsStrings()} initialized to the given
     * value.
     *
     * Sets the value of the MapOfEnumToMapOfStringToEnum property for this object.
     *
     * @param mapOfEnumToMapOfStringToEnum
     *        The new value for the MapOfEnumToMapOfStringToEnum property for this object.
     */
    public static AllTypesUnionStructure fromMapOfEnumToMapOfStringToEnum(
        Map<EnumType, Map<String, EnumType>> mapOfEnumToMapOfStringToEnumWithStrings) {
        return builder().mapOfEnumToMapOfStringToEnum(mapOfEnumToMapOfStringToEnumWithStrings).build();
    }

    /**
     * Create an instance of this class with {@link #timestampMember()} initialized to the given value.
     *
     * Sets the value of the TimestampMember property for this object.
     *
     * @param timestampMember
     *        The new value for the TimestampMember property for this object.
     */
    public static AllTypesUnionStructure fromTimestampMember(Instant timestampMember) {
        return builder().timestampMember(timestampMember).build();
    }

    /**
     * Create an instance of this class with {@link #structWithNestedTimestampMember()} initialized to the given value.
     *
     * Sets the value of the StructWithNestedTimestampMember property for this object.
     *
     * @param structWithNestedTimestampMember
     *        The new value for the StructWithNestedTimestampMember property for this object.
     */
    public static AllTypesUnionStructure fromStructWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
        return builder().structWithNestedTimestampMember(structWithNestedTimestampMember).build();
    }

    /**
     * Create an instance of this class with {@link #structWithNestedTimestampMember()} initialized to the given value.
     *
     * Sets the value of the StructWithNestedTimestampMember property for this object.
     *
     * @param structWithNestedTimestampMember
     *        The new value for the StructWithNestedTimestampMember property for this object.
     */
    public static AllTypesUnionStructure fromStructWithNestedTimestampMember(
        Consumer<StructWithTimestamp.Builder> structWithNestedTimestampMember) {
        StructWithTimestamp.Builder builder = StructWithTimestamp.builder();
        structWithNestedTimestampMember.accept(builder);
        return fromStructWithNestedTimestampMember(builder.build());
    }

    /**
     * Create an instance of this class with {@link #blobArg()} initialized to the given value.
     *
     * Sets the value of the BlobArg property for this object.
     *
     * @param blobArg
     *        The new value for the BlobArg property for this object.
     */
    public static AllTypesUnionStructure fromBlobArg(SdkBytes blobArg) {
        return builder().blobArg(blobArg).build();
    }

    /**
     * Create an instance of this class with {@link #structWithNestedBlob()} initialized to the given value.
     *
     * Sets the value of the StructWithNestedBlob property for this object.
     *
     * @param structWithNestedBlob
     *        The new value for the StructWithNestedBlob property for this object.
     */
    public static AllTypesUnionStructure fromStructWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
        return builder().structWithNestedBlob(structWithNestedBlob).build();
    }

    /**
     * Create an instance of this class with {@link #structWithNestedBlob()} initialized to the given value.
     *
     * Sets the value of the StructWithNestedBlob property for this object.
     *
     * @param structWithNestedBlob
     *        The new value for the StructWithNestedBlob property for this object.
     */
    public static AllTypesUnionStructure fromStructWithNestedBlob(Consumer<StructWithNestedBlobType.Builder> structWithNestedBlob) {
        StructWithNestedBlobType.Builder builder = StructWithNestedBlobType.builder();
        structWithNestedBlob.accept(builder);
        return fromStructWithNestedBlob(builder.build());
    }

    /**
     * Create an instance of this class with {@link #blobMap()} initialized to the given value.
     *
     * Sets the value of the BlobMap property for this object.
     *
     * @param blobMap
     *        The new value for the BlobMap property for this object.
     */
    public static AllTypesUnionStructure fromBlobMap(Map<String, SdkBytes> blobMap) {
        return builder().blobMap(blobMap).build();
    }

    /**
     * Create an instance of this class with {@link #listOfBlobs()} initialized to the given value.
     *
     * Sets the value of the ListOfBlobs property for this object.
     *
     * @param listOfBlobs
     *        The new value for the ListOfBlobs property for this object.
     */
    public static AllTypesUnionStructure fromListOfBlobs(List<SdkBytes> listOfBlobs) {
        return builder().listOfBlobs(listOfBlobs).build();
    }

    /**
     * Create an instance of this class with {@link #recursiveStruct()} initialized to the given value.
     *
     * Sets the value of the RecursiveStruct property for this object.
     *
     * @param recursiveStruct
     *        The new value for the RecursiveStruct property for this object.
     */
    public static AllTypesUnionStructure fromRecursiveStruct(RecursiveStructType recursiveStruct) {
        return builder().recursiveStruct(recursiveStruct).build();
    }

    /**
     * Create an instance of this class with {@link #recursiveStruct()} initialized to the given value.
     *
     * Sets the value of the RecursiveStruct property for this object.
     *
     * @param recursiveStruct
     *        The new value for the RecursiveStruct property for this object.
     */
    public static AllTypesUnionStructure fromRecursiveStruct(Consumer<RecursiveStructType.Builder> recursiveStruct) {
        RecursiveStructType.Builder builder = RecursiveStructType.builder();
        recursiveStruct.accept(builder);
        return fromRecursiveStruct(builder.build());
    }

    /**
     * Create an instance of this class with {@link #polymorphicTypeWithSubTypes()} initialized to the given value.
     *
     * Sets the value of the PolymorphicTypeWithSubTypes property for this object.
     *
     * @param polymorphicTypeWithSubTypes
     *        The new value for the PolymorphicTypeWithSubTypes property for this object.
     */
    public static AllTypesUnionStructure fromPolymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
        return builder().polymorphicTypeWithSubTypes(polymorphicTypeWithSubTypes).build();
    }

    /**
     * Create an instance of this class with {@link #polymorphicTypeWithSubTypes()} initialized to the given value.
     *
     * Sets the value of the PolymorphicTypeWithSubTypes property for this object.
     *
     * @param polymorphicTypeWithSubTypes
     *        The new value for the PolymorphicTypeWithSubTypes property for this object.
     */
    public static AllTypesUnionStructure fromPolymorphicTypeWithSubTypes(Consumer<BaseType.Builder> polymorphicTypeWithSubTypes) {
        BaseType.Builder builder = BaseType.builder();
        polymorphicTypeWithSubTypes.accept(builder);
        return fromPolymorphicTypeWithSubTypes(builder.build());
    }

    /**
     * Create an instance of this class with {@link #polymorphicTypeWithoutSubTypes()} initialized to the given value.
     *
     * Sets the value of the PolymorphicTypeWithoutSubTypes property for this object.
     *
     * @param polymorphicTypeWithoutSubTypes
     *        The new value for the PolymorphicTypeWithoutSubTypes property for this object.
     */
    public static AllTypesUnionStructure fromPolymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
        return builder().polymorphicTypeWithoutSubTypes(polymorphicTypeWithoutSubTypes).build();
    }

    /**
     * Create an instance of this class with {@link #polymorphicTypeWithoutSubTypes()} initialized to the given value.
     *
     * Sets the value of the PolymorphicTypeWithoutSubTypes property for this object.
     *
     * @param polymorphicTypeWithoutSubTypes
     *        The new value for the PolymorphicTypeWithoutSubTypes property for this object.
     */
    public static AllTypesUnionStructure fromPolymorphicTypeWithoutSubTypes(
        Consumer<SubTypeOne.Builder> polymorphicTypeWithoutSubTypes) {
        SubTypeOne.Builder builder = SubTypeOne.builder();
        polymorphicTypeWithoutSubTypes.accept(builder);
        return fromPolymorphicTypeWithoutSubTypes(builder.build());
    }

    /**
     * Create an instance of this class with {@link #enumTypeAsString()} initialized to the given value.
     *
     * Sets the value of the EnumType property for this object.
     *
     * @param enumType
     *        The new value for the EnumType property for this object.
     * @see EnumType
     */
    public static AllTypesUnionStructure fromEnumType(String enumType) {
        return builder().enumType(enumType).build();
    }

    /**
     * Create an instance of this class with {@link #enumTypeAsString()} initialized to the given value.
     *
     * Sets the value of the EnumType property for this object.
     *
     * @param enumType
     *        The new value for the EnumType property for this object.
     * @see EnumType
     */
    public static AllTypesUnionStructure fromEnumType(EnumType enumType) {
        return builder().enumType(enumType).build();
    }

    /**
     * Create an instance of this class with {@link #underscore_Name_Type()} initialized to the given value.
     *
     * Sets the value of the Underscore_Name_Type property for this object.
     *
     * @param underscore_Name_Type
     *        The new value for the Underscore_Name_Type property for this object.
     */
    public static AllTypesUnionStructure fromUnderscore_Name_Type(Underscore_Name_Type underscore_Name_Type) {
        return builder().underscore_Name_Type(underscore_Name_Type).build();
    }

    /**
     * Create an instance of this class with {@link #underscore_Name_Type()} initialized to the given value.
     *
     * Sets the value of the Underscore_Name_Type property for this object.
     *
     * @param underscore_Name_Type
     *        The new value for the Underscore_Name_Type property for this object.
     */
    public static AllTypesUnionStructure fromUnderscore_Name_Type(Consumer<Underscore_Name_Type.Builder> underscore_Name_Type) {
        Underscore_Name_Type.Builder builder = Underscore_Name_Type.builder();
        underscore_Name_Type.accept(builder);
        return fromUnderscore_Name_Type(builder.build());
    }

    /**
     * Create an instance of this class with {@link #myDocument()} initialized to the given value.
     *
     * Sets the value of the MyDocument property for this object.
     *
     * @param myDocument
     *        The new value for the MyDocument property for this object.
     */
    public static AllTypesUnionStructure fromMyDocument(Document myDocument) {
        return builder().myDocument(myDocument).build();
    }

    /**
     * Create an instance of this class with {@link #allTypesUnionStructure()} initialized to the given value.
     *
     * Sets the value of the AllTypesUnionStructure property for this object.
     *
     * @param allTypesUnionStructure
     *        The new value for the AllTypesUnionStructure property for this object.
     */
    public static AllTypesUnionStructure fromAllTypesUnionStructure(AllTypesUnionStructure allTypesUnionStructure) {
        return builder().allTypesUnionStructure(allTypesUnionStructure).build();
    }

    /**
     * Create an instance of this class with {@link #allTypesUnionStructure()} initialized to the given value.
     *
     * Sets the value of the AllTypesUnionStructure property for this object.
     *
     * @param allTypesUnionStructure
     *        The new value for the AllTypesUnionStructure property for this object.
     */
    public static AllTypesUnionStructure fromAllTypesUnionStructure(Consumer<Builder> allTypesUnionStructure) {
        Builder builder = AllTypesUnionStructure.builder();
        allTypesUnionStructure.accept(builder);
        return fromAllTypesUnionStructure(builder.build());
    }

    /**
     * Retrieve an enum value representing which member of this object is populated.
     *
     * When this class is returned in a service response, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if the
     * service returned a member that is only known to a newer SDK version.
     *
     * When this class is created directly in your code, this will be {@link Type#UNKNOWN_TO_SDK_VERSION} if zero
     * members are set, and {@code null} if more than one member is set.
     */
    public Type type() {
        return type;
    }

    @Override
    public final List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    private static <T> Function<Object, T> getter(Function<AllTypesUnionStructure, T> g) {
        return obj -> g.apply((AllTypesUnionStructure) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    public interface Builder extends SdkPojo, CopyableBuilder<Builder, AllTypesUnionStructure> {
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
        default Builder allTypesUnionStructure(Consumer<Builder> allTypesUnionStructure) {
            return allTypesUnionStructure(AllTypesUnionStructure.builder().applyMutation(allTypesUnionStructure).build());
        }
    }

    static final class BuilderImpl implements Builder {
        private String stringMember;

        private Integer integerMember;

        private Boolean booleanMember;

        private Float floatMember;

        private Double doubleMember;

        private Long longMember;

        private Short shortMember;

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

        private Type type = Type.UNKNOWN_TO_SDK_VERSION;

        private Set<Type> setTypes = EnumSet.noneOf(Type.class);

        private BuilderImpl() {
        }

        private BuilderImpl(AllTypesUnionStructure model) {
            stringMember(model.stringMember);
            integerMember(model.integerMember);
            booleanMember(model.booleanMember);
            floatMember(model.floatMember);
            doubleMember(model.doubleMember);
            longMember(model.longMember);
            shortMember(model.shortMember);
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
            Object oldValue = this.stringMember;
            this.stringMember = stringMember;
            handleUnionValueChange(Type.STRING_MEMBER, oldValue, this.stringMember);
        }

        @Override
        public final Builder stringMember(String stringMember) {
            Object oldValue = this.stringMember;
            this.stringMember = stringMember;
            handleUnionValueChange(Type.STRING_MEMBER, oldValue, this.stringMember);
            return this;
        }

        public final Integer getIntegerMember() {
            return integerMember;
        }

        public final void setIntegerMember(Integer integerMember) {
            Object oldValue = this.integerMember;
            this.integerMember = integerMember;
            handleUnionValueChange(Type.INTEGER_MEMBER, oldValue, this.integerMember);
        }

        @Override
        public final Builder integerMember(Integer integerMember) {
            Object oldValue = this.integerMember;
            this.integerMember = integerMember;
            handleUnionValueChange(Type.INTEGER_MEMBER, oldValue, this.integerMember);
            return this;
        }

        public final Boolean getBooleanMember() {
            return booleanMember;
        }

        public final void setBooleanMember(Boolean booleanMember) {
            Object oldValue = this.booleanMember;
            this.booleanMember = booleanMember;
            handleUnionValueChange(Type.BOOLEAN_MEMBER, oldValue, this.booleanMember);
        }

        @Override
        public final Builder booleanMember(Boolean booleanMember) {
            Object oldValue = this.booleanMember;
            this.booleanMember = booleanMember;
            handleUnionValueChange(Type.BOOLEAN_MEMBER, oldValue, this.booleanMember);
            return this;
        }

        public final Float getFloatMember() {
            return floatMember;
        }

        public final void setFloatMember(Float floatMember) {
            Object oldValue = this.floatMember;
            this.floatMember = floatMember;
            handleUnionValueChange(Type.FLOAT_MEMBER, oldValue, this.floatMember);
        }

        @Override
        public final Builder floatMember(Float floatMember) {
            Object oldValue = this.floatMember;
            this.floatMember = floatMember;
            handleUnionValueChange(Type.FLOAT_MEMBER, oldValue, this.floatMember);
            return this;
        }

        public final Double getDoubleMember() {
            return doubleMember;
        }

        public final void setDoubleMember(Double doubleMember) {
            Object oldValue = this.doubleMember;
            this.doubleMember = doubleMember;
            handleUnionValueChange(Type.DOUBLE_MEMBER, oldValue, this.doubleMember);
        }

        @Override
        public final Builder doubleMember(Double doubleMember) {
            Object oldValue = this.doubleMember;
            this.doubleMember = doubleMember;
            handleUnionValueChange(Type.DOUBLE_MEMBER, oldValue, this.doubleMember);
            return this;
        }

        public final Long getLongMember() {
            return longMember;
        }

        public final void setLongMember(Long longMember) {
            Object oldValue = this.longMember;
            this.longMember = longMember;
            handleUnionValueChange(Type.LONG_MEMBER, oldValue, this.longMember);
        }

        @Override
        public final Builder longMember(Long longMember) {
            Object oldValue = this.longMember;
            this.longMember = longMember;
            handleUnionValueChange(Type.LONG_MEMBER, oldValue, this.longMember);
            return this;
        }

        public final Short getShortMember() {
            return shortMember;
        }

        public final void setShortMember(Short shortMember) {
            Object oldValue = this.shortMember;
            this.shortMember = shortMember;
            handleUnionValueChange(Type.SHORT_MEMBER, oldValue, this.shortMember);
        }

        @Override
        public final Builder shortMember(Short shortMember) {
            Object oldValue = this.shortMember;
            this.shortMember = shortMember;
            handleUnionValueChange(Type.SHORT_MEMBER, oldValue, this.shortMember);
            return this;
        }

        public final Collection<String> getSimpleList() {
            if (simpleList instanceof SdkAutoConstructList) {
                return null;
            }
            return simpleList;
        }

        public final void setSimpleList(Collection<String> simpleList) {
            Object oldValue = this.simpleList;
            this.simpleList = ListOfStringsCopier.copy(simpleList);
            handleUnionValueChange(Type.SIMPLE_LIST, oldValue, this.simpleList);
        }

        @Override
        public final Builder simpleList(Collection<String> simpleList) {
            Object oldValue = this.simpleList;
            this.simpleList = ListOfStringsCopier.copy(simpleList);
            handleUnionValueChange(Type.SIMPLE_LIST, oldValue, this.simpleList);
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
            Object oldValue = this.listOfEnums;
            this.listOfEnums = ListOfEnumsCopier.copy(listOfEnums);
            handleUnionValueChange(Type.LIST_OF_ENUMS, oldValue, this.listOfEnums);
        }

        @Override
        public final Builder listOfEnumsWithStrings(Collection<String> listOfEnums) {
            Object oldValue = this.listOfEnums;
            this.listOfEnums = ListOfEnumsCopier.copy(listOfEnums);
            handleUnionValueChange(Type.LIST_OF_ENUMS, oldValue, this.listOfEnums);
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
            Object oldValue = this.listOfEnums;
            this.listOfEnums = ListOfEnumsCopier.copyEnumToString(listOfEnums);
            handleUnionValueChange(Type.LIST_OF_ENUMS, oldValue, this.listOfEnums);
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
            Object oldValue = this.listOfMaps;
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
            handleUnionValueChange(Type.LIST_OF_MAPS, oldValue, this.listOfMaps);
        }

        @Override
        public final Builder listOfMaps(Collection<? extends Map<String, String>> listOfMaps) {
            Object oldValue = this.listOfMaps;
            this.listOfMaps = ListOfMapStringToStringCopier.copy(listOfMaps);
            handleUnionValueChange(Type.LIST_OF_MAPS, oldValue, this.listOfMaps);
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
            Object oldValue = this.listOfStructs;
            this.listOfStructs = ListOfSimpleStructsCopier.copyFromBuilder(listOfStructs);
            handleUnionValueChange(Type.LIST_OF_STRUCTS, oldValue, this.listOfStructs);
        }

        @Override
        public final Builder listOfStructs(Collection<SimpleStruct> listOfStructs) {
            Object oldValue = this.listOfStructs;
            this.listOfStructs = ListOfSimpleStructsCopier.copy(listOfStructs);
            handleUnionValueChange(Type.LIST_OF_STRUCTS, oldValue, this.listOfStructs);
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
            Object oldValue = this.listOfMapOfEnumToString;
            this.listOfMapOfEnumToString = ListOfMapOfEnumToStringCopier.copy(listOfMapOfEnumToString);
            handleUnionValueChange(Type.LIST_OF_MAP_OF_ENUM_TO_STRING, oldValue, this.listOfMapOfEnumToString);
        }

        @Override
        public final Builder listOfMapOfEnumToStringWithStrings(Collection<? extends Map<String, String>> listOfMapOfEnumToString) {
            Object oldValue = this.listOfMapOfEnumToString;
            this.listOfMapOfEnumToString = ListOfMapOfEnumToStringCopier.copy(listOfMapOfEnumToString);
            handleUnionValueChange(Type.LIST_OF_MAP_OF_ENUM_TO_STRING, oldValue, this.listOfMapOfEnumToString);
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
            Object oldValue = this.listOfMapOfStringToStruct;
            this.listOfMapOfStringToStruct = ListOfMapOfStringToStructCopier.copyFromBuilder(listOfMapOfStringToStruct);
            handleUnionValueChange(Type.LIST_OF_MAP_OF_STRING_TO_STRUCT, oldValue, this.listOfMapOfStringToStruct);
        }

        @Override
        public final Builder listOfMapOfStringToStruct(Collection<? extends Map<String, SimpleStruct>> listOfMapOfStringToStruct) {
            Object oldValue = this.listOfMapOfStringToStruct;
            this.listOfMapOfStringToStruct = ListOfMapOfStringToStructCopier.copy(listOfMapOfStringToStruct);
            handleUnionValueChange(Type.LIST_OF_MAP_OF_STRING_TO_STRUCT, oldValue, this.listOfMapOfStringToStruct);
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
            Object oldValue = this.mapOfStringToIntegerList;
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_INTEGER_LIST, oldValue, this.mapOfStringToIntegerList);
        }

        @Override
        public final Builder mapOfStringToIntegerList(Map<String, ? extends Collection<Integer>> mapOfStringToIntegerList) {
            Object oldValue = this.mapOfStringToIntegerList;
            this.mapOfStringToIntegerList = MapOfStringToIntegerListCopier.copy(mapOfStringToIntegerList);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_INTEGER_LIST, oldValue, this.mapOfStringToIntegerList);
            return this;
        }

        public final Map<String, String> getMapOfStringToString() {
            if (mapOfStringToString instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToString;
        }

        public final void setMapOfStringToString(Map<String, String> mapOfStringToString) {
            Object oldValue = this.mapOfStringToString;
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_STRING, oldValue, this.mapOfStringToString);
        }

        @Override
        public final Builder mapOfStringToString(Map<String, String> mapOfStringToString) {
            Object oldValue = this.mapOfStringToString;
            this.mapOfStringToString = MapOfStringToStringCopier.copy(mapOfStringToString);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_STRING, oldValue, this.mapOfStringToString);
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
            Object oldValue = this.mapOfStringToSimpleStruct;
            this.mapOfStringToSimpleStruct = MapOfStringToSimpleStructCopier.copyFromBuilder(mapOfStringToSimpleStruct);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_SIMPLE_STRUCT, oldValue, this.mapOfStringToSimpleStruct);
        }

        @Override
        public final Builder mapOfStringToSimpleStruct(Map<String, SimpleStruct> mapOfStringToSimpleStruct) {
            Object oldValue = this.mapOfStringToSimpleStruct;
            this.mapOfStringToSimpleStruct = MapOfStringToSimpleStructCopier.copy(mapOfStringToSimpleStruct);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_SIMPLE_STRUCT, oldValue, this.mapOfStringToSimpleStruct);
            return this;
        }

        public final Map<String, String> getMapOfEnumToEnum() {
            if (mapOfEnumToEnum instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToEnum;
        }

        public final void setMapOfEnumToEnum(Map<String, String> mapOfEnumToEnum) {
            Object oldValue = this.mapOfEnumToEnum;
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copy(mapOfEnumToEnum);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_ENUM, oldValue, this.mapOfEnumToEnum);
        }

        @Override
        public final Builder mapOfEnumToEnumWithStrings(Map<String, String> mapOfEnumToEnum) {
            Object oldValue = this.mapOfEnumToEnum;
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copy(mapOfEnumToEnum);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_ENUM, oldValue, this.mapOfEnumToEnum);
            return this;
        }

        @Override
        public final Builder mapOfEnumToEnum(Map<EnumType, EnumType> mapOfEnumToEnum) {
            Object oldValue = this.mapOfEnumToEnum;
            this.mapOfEnumToEnum = MapOfEnumToEnumCopier.copyEnumToString(mapOfEnumToEnum);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_ENUM, oldValue, this.mapOfEnumToEnum);
            return this;
        }

        public final Map<String, String> getMapOfEnumToString() {
            if (mapOfEnumToString instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToString;
        }

        public final void setMapOfEnumToString(Map<String, String> mapOfEnumToString) {
            Object oldValue = this.mapOfEnumToString;
            this.mapOfEnumToString = MapOfEnumToStringCopier.copy(mapOfEnumToString);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_STRING, oldValue, this.mapOfEnumToString);
        }

        @Override
        public final Builder mapOfEnumToStringWithStrings(Map<String, String> mapOfEnumToString) {
            Object oldValue = this.mapOfEnumToString;
            this.mapOfEnumToString = MapOfEnumToStringCopier.copy(mapOfEnumToString);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_STRING, oldValue, this.mapOfEnumToString);
            return this;
        }

        @Override
        public final Builder mapOfEnumToString(Map<EnumType, String> mapOfEnumToString) {
            Object oldValue = this.mapOfEnumToString;
            this.mapOfEnumToString = MapOfEnumToStringCopier.copyEnumToString(mapOfEnumToString);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_STRING, oldValue, this.mapOfEnumToString);
            return this;
        }

        public final Map<String, String> getMapOfStringToEnum() {
            if (mapOfStringToEnum instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfStringToEnum;
        }

        public final void setMapOfStringToEnum(Map<String, String> mapOfStringToEnum) {
            Object oldValue = this.mapOfStringToEnum;
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copy(mapOfStringToEnum);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_ENUM, oldValue, this.mapOfStringToEnum);
        }

        @Override
        public final Builder mapOfStringToEnumWithStrings(Map<String, String> mapOfStringToEnum) {
            Object oldValue = this.mapOfStringToEnum;
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copy(mapOfStringToEnum);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_ENUM, oldValue, this.mapOfStringToEnum);
            return this;
        }

        @Override
        public final Builder mapOfStringToEnum(Map<String, EnumType> mapOfStringToEnum) {
            Object oldValue = this.mapOfStringToEnum;
            this.mapOfStringToEnum = MapOfStringToEnumCopier.copyEnumToString(mapOfStringToEnum);
            handleUnionValueChange(Type.MAP_OF_STRING_TO_ENUM, oldValue, this.mapOfStringToEnum);
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
            Object oldValue = this.mapOfEnumToSimpleStruct;
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copyFromBuilder(mapOfEnumToSimpleStruct);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_SIMPLE_STRUCT, oldValue, this.mapOfEnumToSimpleStruct);
        }

        @Override
        public final Builder mapOfEnumToSimpleStructWithStrings(Map<String, SimpleStruct> mapOfEnumToSimpleStruct) {
            Object oldValue = this.mapOfEnumToSimpleStruct;
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copy(mapOfEnumToSimpleStruct);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_SIMPLE_STRUCT, oldValue, this.mapOfEnumToSimpleStruct);
            return this;
        }

        @Override
        public final Builder mapOfEnumToSimpleStruct(Map<EnumType, SimpleStruct> mapOfEnumToSimpleStruct) {
            Object oldValue = this.mapOfEnumToSimpleStruct;
            this.mapOfEnumToSimpleStruct = MapOfEnumToSimpleStructCopier.copyEnumToString(mapOfEnumToSimpleStruct);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_SIMPLE_STRUCT, oldValue, this.mapOfEnumToSimpleStruct);
            return this;
        }

        public final Map<String, ? extends Collection<String>> getMapOfEnumToListOfEnums() {
            if (mapOfEnumToListOfEnums instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToListOfEnums;
        }

        public final void setMapOfEnumToListOfEnums(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnums) {
            Object oldValue = this.mapOfEnumToListOfEnums;
            this.mapOfEnumToListOfEnums = MapOfEnumToListOfEnumsCopier.copy(mapOfEnumToListOfEnums);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_LIST_OF_ENUMS, oldValue, this.mapOfEnumToListOfEnums);
        }

        @Override
        public final Builder mapOfEnumToListOfEnumsWithStrings(Map<String, ? extends Collection<String>> mapOfEnumToListOfEnums) {
            Object oldValue = this.mapOfEnumToListOfEnums;
            this.mapOfEnumToListOfEnums = MapOfEnumToListOfEnumsCopier.copy(mapOfEnumToListOfEnums);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_LIST_OF_ENUMS, oldValue, this.mapOfEnumToListOfEnums);
            return this;
        }

        @Override
        public final Builder mapOfEnumToListOfEnums(Map<EnumType, ? extends Collection<EnumType>> mapOfEnumToListOfEnums) {
            Object oldValue = this.mapOfEnumToListOfEnums;
            this.mapOfEnumToListOfEnums = MapOfEnumToListOfEnumsCopier.copyEnumToString(mapOfEnumToListOfEnums);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_LIST_OF_ENUMS, oldValue, this.mapOfEnumToListOfEnums);
            return this;
        }

        public final Map<String, ? extends Map<String, String>> getMapOfEnumToMapOfStringToEnum() {
            if (mapOfEnumToMapOfStringToEnum instanceof SdkAutoConstructMap) {
                return null;
            }
            return mapOfEnumToMapOfStringToEnum;
        }

        public final void setMapOfEnumToMapOfStringToEnum(Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnum) {
            Object oldValue = this.mapOfEnumToMapOfStringToEnum;
            this.mapOfEnumToMapOfStringToEnum = MapOfEnumToMapOfStringToEnumCopier.copy(mapOfEnumToMapOfStringToEnum);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM, oldValue, this.mapOfEnumToMapOfStringToEnum);
        }

        @Override
        public final Builder mapOfEnumToMapOfStringToEnumWithStrings(
            Map<String, ? extends Map<String, String>> mapOfEnumToMapOfStringToEnum) {
            Object oldValue = this.mapOfEnumToMapOfStringToEnum;
            this.mapOfEnumToMapOfStringToEnum = MapOfEnumToMapOfStringToEnumCopier.copy(mapOfEnumToMapOfStringToEnum);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM, oldValue, this.mapOfEnumToMapOfStringToEnum);
            return this;
        }

        @Override
        public final Builder mapOfEnumToMapOfStringToEnum(
            Map<EnumType, ? extends Map<String, EnumType>> mapOfEnumToMapOfStringToEnum) {
            Object oldValue = this.mapOfEnumToMapOfStringToEnum;
            this.mapOfEnumToMapOfStringToEnum = MapOfEnumToMapOfStringToEnumCopier.copyEnumToString(mapOfEnumToMapOfStringToEnum);
            handleUnionValueChange(Type.MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM, oldValue, this.mapOfEnumToMapOfStringToEnum);
            return this;
        }

        public final Instant getTimestampMember() {
            return timestampMember;
        }

        public final void setTimestampMember(Instant timestampMember) {
            Object oldValue = this.timestampMember;
            this.timestampMember = timestampMember;
            handleUnionValueChange(Type.TIMESTAMP_MEMBER, oldValue, this.timestampMember);
        }

        @Override
        public final Builder timestampMember(Instant timestampMember) {
            Object oldValue = this.timestampMember;
            this.timestampMember = timestampMember;
            handleUnionValueChange(Type.TIMESTAMP_MEMBER, oldValue, this.timestampMember);
            return this;
        }

        public final StructWithTimestamp.Builder getStructWithNestedTimestampMember() {
            return structWithNestedTimestampMember != null ? structWithNestedTimestampMember.toBuilder() : null;
        }

        public final void setStructWithNestedTimestampMember(StructWithTimestamp.BuilderImpl structWithNestedTimestampMember) {
            Object oldValue = this.structWithNestedTimestampMember;
            this.structWithNestedTimestampMember = structWithNestedTimestampMember != null ? structWithNestedTimestampMember
                .build() : null;
            handleUnionValueChange(Type.STRUCT_WITH_NESTED_TIMESTAMP_MEMBER, oldValue, this.structWithNestedTimestampMember);
        }

        @Override
        public final Builder structWithNestedTimestampMember(StructWithTimestamp structWithNestedTimestampMember) {
            Object oldValue = this.structWithNestedTimestampMember;
            this.structWithNestedTimestampMember = structWithNestedTimestampMember;
            handleUnionValueChange(Type.STRUCT_WITH_NESTED_TIMESTAMP_MEMBER, oldValue, this.structWithNestedTimestampMember);
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
            Object oldValue = this.blobArg;
            this.blobArg = blobArg;
            handleUnionValueChange(Type.BLOB_ARG, oldValue, this.blobArg);
            return this;
        }

        public final StructWithNestedBlobType.Builder getStructWithNestedBlob() {
            return structWithNestedBlob != null ? structWithNestedBlob.toBuilder() : null;
        }

        public final void setStructWithNestedBlob(StructWithNestedBlobType.BuilderImpl structWithNestedBlob) {
            Object oldValue = this.structWithNestedBlob;
            this.structWithNestedBlob = structWithNestedBlob != null ? structWithNestedBlob.build() : null;
            handleUnionValueChange(Type.STRUCT_WITH_NESTED_BLOB, oldValue, this.structWithNestedBlob);
        }

        @Override
        public final Builder structWithNestedBlob(StructWithNestedBlobType structWithNestedBlob) {
            Object oldValue = this.structWithNestedBlob;
            this.structWithNestedBlob = structWithNestedBlob;
            handleUnionValueChange(Type.STRUCT_WITH_NESTED_BLOB, oldValue, this.structWithNestedBlob);
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
            Object oldValue = this.blobMap;
            this.blobMap = BlobMapTypeCopier.copy(blobMap);
            handleUnionValueChange(Type.BLOB_MAP, oldValue, this.blobMap);
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
            Object oldValue = this.listOfBlobs;
            this.listOfBlobs = ListOfBlobsTypeCopier.copy(listOfBlobs);
            handleUnionValueChange(Type.LIST_OF_BLOBS, oldValue, this.listOfBlobs);
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
            Object oldValue = this.recursiveStruct;
            this.recursiveStruct = recursiveStruct != null ? recursiveStruct.build() : null;
            handleUnionValueChange(Type.RECURSIVE_STRUCT, oldValue, this.recursiveStruct);
        }

        @Override
        public final Builder recursiveStruct(RecursiveStructType recursiveStruct) {
            Object oldValue = this.recursiveStruct;
            this.recursiveStruct = recursiveStruct;
            handleUnionValueChange(Type.RECURSIVE_STRUCT, oldValue, this.recursiveStruct);
            return this;
        }

        public final BaseType.Builder getPolymorphicTypeWithSubTypes() {
            return polymorphicTypeWithSubTypes != null ? polymorphicTypeWithSubTypes.toBuilder() : null;
        }

        public final void setPolymorphicTypeWithSubTypes(BaseType.BuilderImpl polymorphicTypeWithSubTypes) {
            Object oldValue = this.polymorphicTypeWithSubTypes;
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes != null ? polymorphicTypeWithSubTypes.build() : null;
            handleUnionValueChange(Type.POLYMORPHIC_TYPE_WITH_SUB_TYPES, oldValue, this.polymorphicTypeWithSubTypes);
        }

        @Override
        public final Builder polymorphicTypeWithSubTypes(BaseType polymorphicTypeWithSubTypes) {
            Object oldValue = this.polymorphicTypeWithSubTypes;
            this.polymorphicTypeWithSubTypes = polymorphicTypeWithSubTypes;
            handleUnionValueChange(Type.POLYMORPHIC_TYPE_WITH_SUB_TYPES, oldValue, this.polymorphicTypeWithSubTypes);
            return this;
        }

        public final SubTypeOne.Builder getPolymorphicTypeWithoutSubTypes() {
            return polymorphicTypeWithoutSubTypes != null ? polymorphicTypeWithoutSubTypes.toBuilder() : null;
        }

        public final void setPolymorphicTypeWithoutSubTypes(SubTypeOne.BuilderImpl polymorphicTypeWithoutSubTypes) {
            Object oldValue = this.polymorphicTypeWithoutSubTypes;
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes != null ? polymorphicTypeWithoutSubTypes.build()
                                                                                         : null;
            handleUnionValueChange(Type.POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES, oldValue, this.polymorphicTypeWithoutSubTypes);
        }

        @Override
        public final Builder polymorphicTypeWithoutSubTypes(SubTypeOne polymorphicTypeWithoutSubTypes) {
            Object oldValue = this.polymorphicTypeWithoutSubTypes;
            this.polymorphicTypeWithoutSubTypes = polymorphicTypeWithoutSubTypes;
            handleUnionValueChange(Type.POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES, oldValue, this.polymorphicTypeWithoutSubTypes);
            return this;
        }

        public final String getEnumType() {
            return enumType;
        }

        public final void setEnumType(String enumType) {
            Object oldValue = this.enumType;
            this.enumType = enumType;
            handleUnionValueChange(Type.ENUM_TYPE, oldValue, this.enumType);
        }

        @Override
        public final Builder enumType(String enumType) {
            Object oldValue = this.enumType;
            this.enumType = enumType;
            handleUnionValueChange(Type.ENUM_TYPE, oldValue, this.enumType);
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
            Object oldValue = this.underscore_Name_Type;
            this.underscore_Name_Type = underscore_Name_Type != null ? underscore_Name_Type.build() : null;
            handleUnionValueChange(Type.UNDERSCORE_NAME_TYPE, oldValue, this.underscore_Name_Type);
        }

        @Override
        public final Builder underscore_Name_Type(Underscore_Name_Type underscore_Name_Type) {
            Object oldValue = this.underscore_Name_Type;
            this.underscore_Name_Type = underscore_Name_Type;
            handleUnionValueChange(Type.UNDERSCORE_NAME_TYPE, oldValue, this.underscore_Name_Type);
            return this;
        }

        public final Document getMyDocument() {
            return myDocument;
        }

        public final void setMyDocument(Document myDocument) {
            Object oldValue = this.myDocument;
            this.myDocument = myDocument;
            handleUnionValueChange(Type.MY_DOCUMENT, oldValue, this.myDocument);
        }

        @Override
        public final Builder myDocument(Document myDocument) {
            Object oldValue = this.myDocument;
            this.myDocument = myDocument;
            handleUnionValueChange(Type.MY_DOCUMENT, oldValue, this.myDocument);
            return this;
        }

        public final Builder getAllTypesUnionStructure() {
            return allTypesUnionStructure != null ? allTypesUnionStructure.toBuilder() : null;
        }

        public final void setAllTypesUnionStructure(BuilderImpl allTypesUnionStructure) {
            Object oldValue = this.allTypesUnionStructure;
            this.allTypesUnionStructure = allTypesUnionStructure != null ? allTypesUnionStructure.build() : null;
            handleUnionValueChange(Type.ALL_TYPES_UNION_STRUCTURE, oldValue, this.allTypesUnionStructure);
        }

        @Override
        public final Builder allTypesUnionStructure(AllTypesUnionStructure allTypesUnionStructure) {
            Object oldValue = this.allTypesUnionStructure;
            this.allTypesUnionStructure = allTypesUnionStructure;
            handleUnionValueChange(Type.ALL_TYPES_UNION_STRUCTURE, oldValue, this.allTypesUnionStructure);
            return this;
        }

        @Override
        public AllTypesUnionStructure build() {
            return new AllTypesUnionStructure(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return SDK_FIELDS;
        }

        private final void handleUnionValueChange(Type type, Object oldValue, Object newValue) {
            if (this.type == type || oldValue == newValue) {
                return;
            }
            if (newValue == null || newValue instanceof SdkAutoConstructList || newValue instanceof SdkAutoConstructMap) {
                setTypes.remove(type);
            } else if (oldValue == null || oldValue instanceof SdkAutoConstructList || oldValue instanceof SdkAutoConstructMap) {
                setTypes.add(type);
            }
            if (setTypes.size() == 1) {
                this.type = setTypes.iterator().next();
            } else if (setTypes.isEmpty()) {
                this.type = Type.UNKNOWN_TO_SDK_VERSION;
            } else {
                this.type = null;
            }
        }
    }

    /**
     * @see AllTypesUnionStructure#type()
     */
    public enum Type {
        STRING_MEMBER,

        INTEGER_MEMBER,

        BOOLEAN_MEMBER,

        FLOAT_MEMBER,

        DOUBLE_MEMBER,

        LONG_MEMBER,

        SHORT_MEMBER,

        SIMPLE_LIST,

        LIST_OF_ENUMS,

        LIST_OF_MAPS,

        LIST_OF_STRUCTS,

        LIST_OF_MAP_OF_ENUM_TO_STRING,

        LIST_OF_MAP_OF_STRING_TO_STRUCT,

        MAP_OF_STRING_TO_INTEGER_LIST,

        MAP_OF_STRING_TO_STRING,

        MAP_OF_STRING_TO_SIMPLE_STRUCT,

        MAP_OF_ENUM_TO_ENUM,

        MAP_OF_ENUM_TO_STRING,

        MAP_OF_STRING_TO_ENUM,

        MAP_OF_ENUM_TO_SIMPLE_STRUCT,

        MAP_OF_ENUM_TO_LIST_OF_ENUMS,

        MAP_OF_ENUM_TO_MAP_OF_STRING_TO_ENUM,

        TIMESTAMP_MEMBER,

        STRUCT_WITH_NESTED_TIMESTAMP_MEMBER,

        BLOB_ARG,

        STRUCT_WITH_NESTED_BLOB,

        BLOB_MAP,

        LIST_OF_BLOBS,

        RECURSIVE_STRUCT,

        POLYMORPHIC_TYPE_WITH_SUB_TYPES,

        POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES,

        ENUM_TYPE,

        UNDERSCORE_NAME_TYPE,

        MY_DOCUMENT,

        ALL_TYPES_UNION_STRUCTURE,

        UNKNOWN_TO_SDK_VERSION
    }
}
