/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.enhanced.dynamodb.document;

import static java.time.Instant.ofEpochMilli;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.document.TestData.TypeMap.typeMap;
import static software.amazon.awssdk.enhanced.dynamodb.document.TestData.dataBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.ChainConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.string.CharSequenceStringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Pair;

public final class EnhancedDocumentTestData implements ArgumentsProvider {

    private static long FIXED_INSTANT_TIME = 1677690845038L;

    public static final AttributeValue NUMBER_STRING_ARRAY_ATTRIBUTES_LISTS =
        AttributeValue.fromL(Arrays.asList(AttributeValue.fromN("1"),
                                           AttributeValue.fromN("2"),
                                           AttributeValue.fromN("3")));

    private static final EnhancedDocumentTestData INSTANCE = new EnhancedDocumentTestData();

    private Map<String, TestData> testScenarioMap;
    private List<TestData> testDataList;

    public EnhancedDocumentTestData() {
        initializeTestData();
    }

    public static EnhancedDocumentTestData testDataInstance() {
        return new EnhancedDocumentTestData();
    }

    public static EnhancedDocument.Builder defaultDocBuilder() {
        EnhancedDocument.Builder defaultBuilder = DefaultEnhancedDocument.builder();
        return defaultBuilder.addAttributeConverterProvider(defaultProvider());
    }

    public static AttributeStringValueMap map() {
        return new AttributeStringValueMap();
    }

    private void initializeTestData() {

        testDataList = new ArrayList<>();
        testDataList.add(dataBuilder().scenario("nullKey")
                                      .ddbItemMap(map().withKeyValue("nullKey", AttributeValue.fromNul(true)).get())
                                      .enhancedDocument(defaultDocBuilder()
                                                            .putNull("nullKey")
                                                            .build())
                                      .json("{\"nullKey\":null}")
                                      .attributeConverterProvider(defaultProvider())
                                      .build());


        testDataList.add(dataBuilder().scenario("simpleString")
                                      .ddbItemMap(map().withKeyValue("stringKey", AttributeValue.fromS("stringValue")).get())
                                      .enhancedDocument(
                                          ((DefaultEnhancedDocument.DefaultBuilder)
                                              DefaultEnhancedDocument.builder()).putObject("stringKey", "stringValue")
                                                                 .addAttributeConverterProvider(defaultProvider()).build())
                                      .attributeConverterProvider(defaultProvider())
                                      .json("{\"stringKey\":\"stringValue\"}")

                                      .build());

        testDataList.add(dataBuilder().scenario("record")

                                      .ddbItemMap(map().withKeyValue("uniqueId", AttributeValue.fromS("id-value"))
                                                      .withKeyValue("sortKey",AttributeValue.fromS("sort-value"))
                                                      .withKeyValue("attributeKey", AttributeValue.fromS("one"))
                                                      .withKeyValue("attributeKey2", AttributeValue.fromS("two"))
                                                      .withKeyValue("attributeKey3", AttributeValue.fromS("three")).get())
                                      .enhancedDocument(
                                            defaultDocBuilder()
                                                .putString("uniqueId","id-value")
                                                .putString("sortKey","sort-value")
                                                .putString("attributeKey","one")
                                                .putString("attributeKey2","two")
                                                .putString("attributeKey3","three")
                                                .build()
                                      )


                                      .attributeConverterProvider(defaultProvider())
                                      .json("{\"uniqueId\":\"id-value\",\"sortKey\":\"sort-value\",\"attributeKey\":\"one\","
                                            + "\"attributeKey2\":\"two\",\"attributeKey3\":\"three\"}")

                                      .build());

        testDataList.add(dataBuilder().scenario("differentNumberTypes")
                                      .ddbItemMap(map()
                                                      .withKeyValue("numberKey", AttributeValue.fromN("10"))
                                                      .withKeyValue("bigDecimalNumberKey",
                                                                    AttributeValue.fromN(new BigDecimal(10).toString())).get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putNumber("numberKey", Integer.valueOf(10))
                                              .putNumber("bigDecimalNumberKey", new BigDecimal(10))
                                              .build())
                                      .attributeConverterProvider(defaultProvider())
                                      .json("{" + "\"numberKey\":10," + "\"bigDecimalNumberKey\":10" + "}")

                                      .build());

        testDataList.add(dataBuilder().scenario("allSimpleTypes")
                                      .ddbItemMap(map()
                                                      .withKeyValue("stringKey", AttributeValue.fromS("stringValue"))
                                                      .withKeyValue("numberKey", AttributeValue.fromN("10"))
                                                      .withKeyValue("boolKey", AttributeValue.fromBool(true))
                                                      .withKeyValue("nullKey", AttributeValue.fromNul(true))
                                                      .withKeyValue("numberSet", AttributeValue.fromNs(Arrays.asList("1", "2", "3")))
                                                      .withKeyValue("sdkBytesSet",
                                                                    AttributeValue.fromBs(Arrays.asList(SdkBytes.fromUtf8String("a")
                                                                        ,SdkBytes.fromUtf8String("b")
                                                                        ,SdkBytes.fromUtf8String("c"))))
                                                      .withKeyValue("stringSet",
                                                                    AttributeValue.fromSs(Arrays.asList("a", "b", "c"))).get())
                                      .enhancedDocument(defaultDocBuilder()
                                                            .putString("stringKey", "stringValue")
                                                            .putNumber("numberKey", 10)
                                                            .putBoolean("boolKey", true)
                                                            .putNull("nullKey")
                                                            .putNumberSet("numberSet", Stream.of(1, 2, 3).collect(Collectors.toSet()))
                                                            .putBytesSet("sdkBytesSet", Stream.of(SdkBytes.fromUtf8String("a"),
                                                                                                  SdkBytes.fromUtf8String("b"),
                                                                                                  SdkBytes.fromUtf8String("c"))
                                                                                              .collect(Collectors.toSet()))
                                                            .putStringSet("stringSet", Stream.of("a", "b", "c").collect(Collectors.toSet()))
                                                            .build())
                                      .json("{\"stringKey\":\"stringValue\",\"numberKey\":10,\"boolKey\":true,\"nullKey\":null,"
                                            + "\"numberSet\":[1,2,3],\"sdkBytesSet\":[\"YQ==\",\"Yg==\",\"Yw==\"],\"stringSet\":[\"a\","
                                            + "\"b\",\"c\"]}")
                             .attributeConverterProvider(defaultProvider())
                                      .build());

        testDataList.add(dataBuilder().scenario("differentNumberSets")
                             .isGeneric(false)
                                      .ddbItemMap(map()
                                                      .withKeyValue("floatSet", AttributeValue.fromNs(Arrays.asList("2.0", "3.0")))
                                                      .withKeyValue("integerSet", AttributeValue.fromNs(Arrays.asList("-1", "0", "1")))
                                                      .withKeyValue("bigDecimal", AttributeValue.fromNs(Arrays.asList("1000.002", "2000.003")))
                                                      .withKeyValue("sdkNumberSet", AttributeValue.fromNs(Arrays.asList("1", "2", "3"))).get())
                                      .enhancedDocument(defaultDocBuilder()
                                                            .putNumberSet("floatSet", Stream.of( Float.parseFloat("2.0"),
                                                                                                 Float.parseFloat("3.0") ).collect(Collectors.toCollection(LinkedHashSet::new)))
                                                            .putNumberSet("integerSet",Arrays.asList(-1,0, 1).stream().collect(Collectors.toCollection(LinkedHashSet::new)))
                                                            .putNumberSet("bigDecimal", Stream.of(BigDecimal.valueOf(1000.002), BigDecimal.valueOf(2000.003) ).collect(Collectors.toCollection(LinkedHashSet::new)))
                                                            .putNumberSet("sdkNumberSet", Stream.of(SdkNumber.fromInteger(1), SdkNumber.fromInteger(2), SdkNumber.fromInteger(3) ).collect(Collectors.toSet()))
                                                            .build())
                                      .json("{\"floatSet\": [2.0, 3.0],\"integerSet\": [-1, 0, 1],\"bigDecimal\": [1000.002, 2000.003],\"sdkNumberSet\": [1,2, 3]}")
                             .attributeConverterProvider(defaultProvider())
                                      .build());

        testDataList.add(dataBuilder().scenario("simpleListExcludingBytes")
                                      .ddbItemMap(map()
                                                      .withKeyValue("numberList",
                                                                    AttributeValue.fromL(
                                                                        Arrays.asList(AttributeValue.fromN("1"),
                                                                                      AttributeValue.fromN("2"))))
                                                      .withKeyValue("stringList",
                                                                    AttributeValue.fromL(
                                                                        Arrays.asList(
                                                                            AttributeValue.fromS("one"),
                                                                            AttributeValue.fromS("two")))).get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putList("numberList", Arrays.asList(1,2), EnhancedType.of(Integer.class))
                                              .putList("stringList", Arrays.asList("one","two"), EnhancedType.of(String.class))
                                              .build()
                                      )
                             .typeMap(typeMap()
                                          .addAttribute("numberList", EnhancedType.of(Integer.class))
                                          .addAttribute("stringList", EnhancedType.of(String.class)))
                                      .attributeConverterProvider(defaultProvider())
                                      .json("{\"numberList\":[1,2],\"stringList\":[\"one\",\"two\"]}")
                                      .build());

        testDataList.add(dataBuilder().scenario("customList")
                                      .ddbItemMap(
                                          map().withKeyValue("customClassForDocumentAPI", AttributeValue.fromL(
                                              Arrays.asList(
                                                  AttributeValue.fromM(getAttributeValueMapForCustomClassWithPrefix(1,10, false))
                                                  ,
                                                  AttributeValue.fromM(getAttributeValueMapForCustomClassWithPrefix(2,200, false))))).get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create()
                                                  ,defaultProvider())
                                      .putList("customClassForDocumentAPI"
                                          , Arrays.asList(getCustomClassForDocumentAPIWithBaseAndOffset(1,10)
                                              ,getCustomClassForDocumentAPIWithBaseAndOffset(2,200)),
                                               EnhancedType.of(CustomClassForDocumentAPI.class))
                                      .build()
                                      )
                             .typeMap(typeMap()
                                          .addAttribute("instantList", EnhancedType.of(Instant.class))
                                          .addAttribute("customClassForDocumentAPI", EnhancedType.of(CustomClassForDocumentAPI.class)))
                                      .attributeConverterProvider(ChainConverterProvider.create(CustomAttributeForDocumentConverterProvider.create(),
                                                                                                defaultProvider()))
                                      .json("{\"customClassForDocumentAPI\":[{\"instantList\":[\"2023-03-01T17:14:05.049Z\","
                                            + "\"2023-03-01T17:14:05.049Z\",\"2023-03-01T17:14:05.049Z\"],\"longNumber\":11,"
                                            + "\"string\":\"11\",\"stringSet\":[\"12\",\"13\",\"14\"]},"
                                            + "{\"instantList\":[\"2023-03-01T17:14:05.240Z\",\"2023-03-01T17:14:05.240Z\","
                                            + "\"2023-03-01T17:14:05.240Z\"],\"longNumber\":202,\"string\":\"202\","
                                            + "\"stringSet\":[\"203\",\"204\",\"205\"]}]}")
                                      .build());

        testDataList.add(dataBuilder().scenario("ThreeLevelNestedList")
                                      .ddbItemMap(
                                          map().withKeyValue("threeLevelList",
                                                             AttributeValue.fromL(Arrays.asList(
                                                                 AttributeValue.fromL(Arrays.asList(
                                                                     AttributeValue.fromL(Arrays.asList(AttributeValue.fromS("l1_0"),
                                                                                                        AttributeValue.fromS("l1_1"))),
                                                                     AttributeValue.fromL(Arrays.asList(AttributeValue.fromS("l2_0"),
                                                                                                        AttributeValue.fromS("l2_1")))
                                                                 ))
                                                                 ,
                                                                 AttributeValue.fromL(Arrays.asList(
                                                                     AttributeValue.fromL(Arrays.asList(AttributeValue.fromS("l3_0"),
                                                                                                        AttributeValue.fromS("l3_1"))),
                                                                     AttributeValue.fromL(Arrays.asList(AttributeValue.fromS("l4_0"),
                                                                                                        AttributeValue.fromS("l4_1")))
                                                                 ))))).get()
                                      )
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putList("threeLevelList"
                                                  , Arrays.asList(
                                                      Arrays.asList(
                                                          Arrays.asList("l1_0", "l1_1"), Arrays.asList("l2_0", "l2_1")
                                                      ),
                                                      Arrays.asList(
                                                          Arrays.asList("l3_0", "l3_1"), Arrays.asList("l4_0", "l4_1")
                                                      )
                                                  )
                                                  , new EnhancedType<List<List<String>>>() {
                                                  }
                                              )

                                              .build()
                                      )
                                      .attributeConverterProvider(defaultProvider())
                                      .json("{\"threeLevelList\":[[[\"l1_0\",\"l1_1\"],[\"l2_0\",\"l2_1\"]],[[\"l3_0\","
                                            + "\"l3_1\"],[\"l4_0\",\"l4_1\"]]]}")
                                      .typeMap(typeMap()
                                                   .addAttribute("threeLevelList", new EnhancedType<List<List<String>>>() {
                                                   }))
                                      .build());

        // Test case for Nested List with Maps List<Map<String, List<String>>
        testDataList.add(dataBuilder().scenario("listOfMapOfListValues")
                                      .ddbItemMap(
                                          map()
                                              .withKeyValue("listOfListOfMaps",
                                                            AttributeValue.fromL(
                                                                Arrays.asList(
                                                                    AttributeValue.fromM(getStringListAttributeValueMap("a", 2, 2)),
                                                                    AttributeValue.fromM(getStringListAttributeValueMap("b", 1, 1))
                                                                )
                                                            )
                                              ).get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putList("listOfListOfMaps"
                                                  , Arrays.asList(
                                                      getStringListObjectMap("a", 2, 2),
                                                      getStringListObjectMap("b", 1, 1)
                                                  )
                                                  , new EnhancedType<Map<String, List<Integer>>>() {
                                                  }
                                              )
                                              .build()
                                      )
                                      .json("{\"listOfListOfMaps\":[{\"key_a_1\":[1,2],\"key_a_2\":[1,2]},{\"key_b_1\":[1]}]}")
                                      .attributeConverterProvider(defaultProvider())
                                      .typeMap(typeMap()
                                                   .addAttribute("listOfListOfMaps", new EnhancedType<Map<String, List<Integer>>>() {
                                                   }))
                                      .build());

        testDataList.add(dataBuilder().scenario("simpleMap")
                                      .ddbItemMap(
                                          map()
                                              .withKeyValue("simpleMap", AttributeValue.fromM(getStringSimpleAttributeValueMap(
                                                  "suffix", 7)))
                                              .get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putMap("simpleMap", getStringSimpleMap("suffix", 7, CharSequenceStringConverter.create()),
                                                      EnhancedType.of(CharSequence.class), EnhancedType.of(String.class))
                                              .build()
                                      )
                                      .attributeConverterProvider(defaultProvider())
                                      .typeMap(typeMap()
                                                   .addAttribute("simpleMap", EnhancedType.of(CharSequence.class),
                                                                 EnhancedType.of(String.class)))
                                      .json("{\"simpleMap\":{\"key_suffix_1\":\"1\",\"key_suffix_2\":\"2\","
                                            + "\"key_suffix_3\":\"3\",\"key_suffix_4\":\"4\",\"key_suffix_5\":\"5\","
                                            + "\"key_suffix_6\":\"6\",\"key_suffix_7\":\"7\"}}")
                                      .build());

        testDataList.add(dataBuilder().scenario("ElementsOfCustomType")
                                      .ddbItemMap(
                                          map().withKeyValue("customMapValue",
                                                             AttributeValue.fromM(
                                                                 map().withKeyValue("entryOne",
                                                                                    AttributeValue.fromM(getAttributeValueMapForCustomClassWithPrefix(2, 10, false)))
                                                                      .get()))
                                               .get()
                                      )
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create()
                                                  , defaultProvider())
                                              .putMap("customMapValue",
                                                      Stream.of(Pair.of("entryOne", customValueWithBaseAndOffset(2, 10)))
                                                                  .collect(Collectors.toMap(p -> CharSequenceStringConverter.create().fromString(p.left()), p -> p.right(),
                                                                                            (oldValue, newValue) -> oldValue,
                                                                                            LinkedHashMap::new))
                                                  , EnhancedType.of(CharSequence.class),
                                                      EnhancedType.of(CustomClassForDocumentAPI.class))
                                              .build()
                                      )
                                      .json("{\"customMapValue\":{\"entryOne\":{\"instantList\":[\"2023-03-01T17:14:05.050Z\","
                                            + "\"2023-03-01T17:14:05.050Z\",\"2023-03-01T17:14:05.050Z\"],\"longNumber\":12,"
                                            + "\"string\":\"12\",\"stringSet\":[\"13\",\"14\",\"15\"]}}}")
                                      .typeMap(typeMap()
                                                   .addAttribute("customMapValue", EnhancedType.of(CharSequence.class),
                                                                 EnhancedType.of(CustomClassForDocumentAPI.class)))
                                      .attributeConverterProvider(ChainConverterProvider.create(CustomAttributeForDocumentConverterProvider.create(),
                                                                                                defaultProvider()))
                                      .build());

        testDataList.add(dataBuilder().scenario("complexDocWithSdkBytesAndMapArrays_And_PutOverWritten")
                                      .ddbItemMap(map().withKeyValue("nullKey",AttributeValue.fromNul(true)).get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putNull("nullKey")
                                              .putNumber("numberKey", 1)
                                              .putString("stringKey", "stringValue")
                                              .putList("numberList", Arrays.asList(1, 2, 3), EnhancedType.of(Integer.class))
                                              .put("simpleDate", LocalDate.MIN, EnhancedType.of(LocalDate.class))
                                              .putStringSet("stringSet", Stream.of("one", "two").collect(Collectors.toSet()))
                                              .putBytes("sdkByteKey", SdkBytes.fromUtf8String("a"))
                                              .putBytesSet("sdkByteSet",
                                                           Stream.of(SdkBytes.fromUtf8String("a"),
                                                                     SdkBytes.fromUtf8String("b")).collect(Collectors.toSet()))
                                              .putNumberSet("numberSetSet", Stream.of(1, 2).collect(Collectors.toSet()))
                                              .putList("numberList", Arrays.asList(4, 5, 6), EnhancedType.of(Integer.class))
                                              .putMap("simpleMap",
                                                      mapFromSimpleKeyValue(Pair.of("78b3522c-2ab3-4162-8c5d"
                                                                                                       + "-f093fa76e68c", 3),
                                                                                               Pair.of("4ae1f694-52ce-4cf6-8211"
                                                                                                       + "-232ccf780da8", 9)),
                                                      EnhancedType.of(String.class), EnhancedType.of(Integer.class))
                                              .putMap("mapKey", mapFromSimpleKeyValue(Pair.of("1", Arrays.asList("a", "b"
                                                                , "c")), Pair.of("2",
                                                                                 Collections.singletonList("1"))),
                                                      EnhancedType.of(String.class), EnhancedType.listOf(String.class))
                                              .build()
                                      )
                                      .json("{\"nullKey\": null, \"numberKey\": 1, \"stringKey\":\"stringValue\", "
                                            + "\"simpleDate\":\"-999999999-01-01\",\"stringSet\": "
                                            + "[\"one\",\"two\"],\"sdkByteKey\":\"a\",\"sdkByteSet\":[\"a\",\"b\"], "
                                            + "\"numberSetSet\": [1,2], "
                                            + "\"numberList\": [4, 5, 6], "
                                            + "\"simpleMap\": {\"78b3522c-2ab3-4162-8c5d-f093fa76e68c\": 3,"
                                            + "\"4ae1f694-52ce-4cf6-8211-232ccf780da8\": 9}, \"mapKey\": {\"1\":[\"a\",\"b\","
                                            + " \"c\"],\"2\":[\"1\"]}}")
                                      .attributeConverterProvider(defaultProvider())
                             .isGeneric(false)
                                      .build());

        testDataList.add(dataBuilder().scenario("insertUsingPutJson")
                                      .ddbItemMap(
                                          map().withKeyValue("customMapValue",
                                                             AttributeValue.fromM(
                                                                 map().withKeyValue("entryOne",
                                                                                    AttributeValue.fromM(getAttributeValueMapForCustomClassWithPrefix(2, 10, true)))
                                                                      .get()))
                                               .get()
                                      )
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create()
                                                  ,defaultProvider())

                                              .putJson("customMapValue",
                                                       "{\"entryOne\": "
                                                       + "{"
                                                       + "\"instantList\": [\"2023-03-01T17:14:05.050Z\", \"2023-03-01T17:14:05.050Z\", \"2023-03-01T17:14:05.050Z\"], "
                                                       + "\"longNumber\": 12, "
                                                       + "\"string\": \"12\""
                                                       + "}"
                                                       + "}")
                                              .build()
                                      )
                                      .json("{\"customMapValue\":{\"entryOne\":{\"instantList\":[\"2023-03-01T17:14:05.050Z\","
                                            + "\"2023-03-01T17:14:05.050Z\",\"2023-03-01T17:14:05.050Z\"],\"longNumber\":12,"
                                            + "\"string\":\"12\"}}}")
                                      .typeMap(typeMap()
                                                   .addAttribute("customMapValue", EnhancedType.of(CharSequence.class),
                                                                 EnhancedType.of(CustomClassForDocumentAPI.class)))
                                      .attributeConverterProvider(ChainConverterProvider.create(CustomAttributeForDocumentConverterProvider.create(),
                                                                                                defaultProvider()))
                                      .build());

        testDataList.add(dataBuilder().scenario("putJsonWithSimpleMapOfStrings")
                                      .ddbItemMap(
                                          map()
                                              .withKeyValue("simpleMap", AttributeValue.fromM(getStringSimpleAttributeValueMap(
                                                  "suffix", 7)))
                                              .get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putJson("simpleMap",
                                                       "{\"key_suffix_1\":\"1\",\"key_suffix_2\":\"2\",\"key_suffix_3\":"
                                                       + " \"3\",\"key_suffix_4\": "
                                                       + "\"4\",\"key_suffix_5\":\"5\",\"key_suffix_6\":\"6\",\"key_suffix_7\":\"7\"}" )
                                              .build()
                                      )
                                      .attributeConverterProvider(defaultProvider())
                                      .typeMap(typeMap()
                                                   .addAttribute("simpleMap", EnhancedType.of(String.class),
                                                                 EnhancedType.of(String.class)))
                                      .json("{\"simpleMap\":{\"key_suffix_1\":\"1\",\"key_suffix_2\":\"2\","
                                            + "\"key_suffix_3\":\"3\",\"key_suffix_4\":\"4\",\"key_suffix_5\":\"5\","
                                            + "\"key_suffix_6\":\"6\",\"key_suffix_7\":\"7\"}}")

                                      .build());

        // singleSdkByte SetOfSdkBytes ListOfSdkBytes and Map of SdkBytes
        testDataList.add(dataBuilder().scenario("bytesSet")
                             .isGeneric(false)
                                      .ddbItemMap(
                                          map()
                                              .withKeyValue("bytes", AttributeValue.fromB(SdkBytes.fromUtf8String("HelloWorld")))
                                              .withKeyValue("setOfBytes", AttributeValue.fromBs(
                                                                Arrays.asList(SdkBytes.fromUtf8String("one"),
                                                                              SdkBytes.fromUtf8String("two"),
                                                                              SdkBytes.fromUtf8String("three"))))
                                              .withKeyValue("listOfBytes", AttributeValue.fromL(
                                                  Arrays.asList(SdkBytes.fromUtf8String("i1"),
                                                                SdkBytes.fromUtf8String("i2"),
                                                                SdkBytes.fromUtf8String("i3")).stream().map(
                                                                    s -> AttributeValue.fromB(s)).collect(Collectors.toList())))
                                              .withKeyValue("mapOfBytes", AttributeValue.fromM(
                                                  Stream.of(Pair.of("k1", AttributeValue.fromB(SdkBytes.fromUtf8String("v1")))
                                                            ,Pair.of("k2", AttributeValue.fromB(SdkBytes.fromUtf8String("v2"))))
                                                        .collect(Collectors.toMap(k->k.left(),
                                                                                  r ->r.right(),
                                                                                  (oldV, newV)-> oldV,
                                                                                  LinkedHashMap::new)


                                              ))).get())
                                      .enhancedDocument(
                                          defaultDocBuilder()
                                              .putBytes("bytes", SdkBytes.fromUtf8String("HelloWorld"))
                                              .putBytesSet("setOfBytes",
                                                  Arrays.asList(SdkBytes.fromUtf8String("one"),
                                                            SdkBytes.fromUtf8String("two"),
                                                            SdkBytes.fromUtf8String("three")).stream()
                                                      .collect(Collectors.toCollection(LinkedHashSet::new))
                                              )
                                              .putList("listOfBytes",
                                                       Arrays.asList(SdkBytes.fromUtf8String("i1"),
                                                                 SdkBytes.fromUtf8String("i2"),
                                                                 SdkBytes.fromUtf8String("i3"))
                                                       ,EnhancedType.of(SdkBytes.class)
                                              )
                                              .putMap("mapOfBytes"
                                                  , Stream.of(Pair.of("k1", SdkBytes.fromUtf8String("v1"))
                                                            ,Pair.of("k2", SdkBytes.fromUtf8String("v2")))
                                                                .collect(Collectors.toMap(k->k.left(),
                                                                                          r ->r.right(),
                                                                                          (oldV, newV)-> oldV,
                                                                         LinkedHashMap::new)

                                              ), EnhancedType.of(String.class), EnhancedType.of(SdkBytes.class))

                                              .build()
                                      )
                                      .json("{\"bytes\":\"HelloWorld\",\"setOfBytes\":[\"one\",\"two\",\"three\"], "
                                            + "\"listOfBytes\":[\"i1\",\"i2\",\"i3\"],\"mapOfBytes\": {\"k1\":\"v1\","
                                            + "\"k2\":\"v2\"}}")
                             .attributeConverterProvider(defaultProvider())
                             .typeMap(typeMap()
                                          .addAttribute("listOfBytes", EnhancedType.of(SdkBytes.class))
                                          .addAttribute("mapOfBytes", EnhancedType.of(String.class),
                                                        EnhancedType.of(SdkBytes.class)))
                                      .build());
        testScenarioMap = testDataList.stream().collect(Collectors.toMap(TestData::getScenario, Function.identity()));

        // testScenarioMap = testDataList.stream().collect(Collectors.toMap(k->k.getScenario(), Function.identity()));
    }

    public TestData dataForScenario(String scenario) {
        return testScenarioMap.get(scenario);
    }

    public List<TestData> getAllGenericScenarios() {
        return testScenarioMap.values().stream().filter(testData -> testData.isGeneric()).collect(Collectors.toList());
    }

    public static class AttributeStringValueMap {
        private Map<String, AttributeValue> attributeValueMap = new LinkedHashMap<>();

        public Map<String, AttributeValue> get() {
            return attributeValueMap;
        }

        public AttributeStringValueMap withKeyValue(String key, AttributeValue value) {
            attributeValueMap.put(key, value);
            return this;
        }
    }

    /**
     *
     * @param offset from base elements to differentiate the subsequent elements
     * @param base Start of the foest element
     * @param includeSets While testing FromJson, sets are excluded since Json treats sets as lists
     * @return Map with Key - value as String, AttributeValue>
     */
    /**
     * Creates a map of attribute values for a custom class with a prefix added to each key.
     *
     * @param offset The offset from the base element to differentiate subsequent elements.
     * @param base The start index of the first element.
     * @param excludeSetsInMap Whether to exclude sets when creating the map. (Json treats sets as lists.)
     * @return A map with key-value pairs of String and AttributeValue.
     */
    private static Map<String, AttributeValue> getAttributeValueMapForCustomClassWithPrefix(int offset, int base,
                                                                                            boolean excludeSetsInMap) {

        Map<String, AttributeValue> map = new LinkedHashMap<>();
        map.put("instantList",
                AttributeValue.fromL(Stream.of(
                                               ofEpochMilli(FIXED_INSTANT_TIME + base +offset) ,ofEpochMilli(FIXED_INSTANT_TIME + base + offset),
                                               ofEpochMilli(FIXED_INSTANT_TIME + base + offset))
                                           .map(r -> AttributeValue.fromS(String.valueOf(r))).collect(Collectors.toList())));
        map.put("longNumber", AttributeValue.fromN(String.valueOf(base + offset)));
        map.put("string", AttributeValue.fromS(String.valueOf(base + offset)));
        if(! excludeSetsInMap){
            map.put("stringSet",
                    AttributeValue.fromSs(Stream.of(1 + base + offset,2 + base +offset, 3 + base +offset).map(r -> String.valueOf(r)).collect(Collectors.toList())));

        }
        return map;
    }

    private static CustomClassForDocumentAPI getCustomClassForDocumentAPIWithBaseAndOffset(int offset, int base) {

        return CustomClassForDocumentAPI.builder()
                                        .instantList(Stream.of(
                                                               ofEpochMilli(FIXED_INSTANT_TIME + base + offset),
                                                               ofEpochMilli(FIXED_INSTANT_TIME + base + offset),
                                                               ofEpochMilli(FIXED_INSTANT_TIME + base + offset))
                                                           .collect(Collectors.toList()))
                                        .longNumber(Long.valueOf(base + offset))
                                        .string(String.valueOf(base + offset))
                                        .stringSet(Stream.of(1+ base + offset, 2 +base + offset, 3 + base  + offset).map(String::valueOf).collect(Collectors.toCollection(LinkedHashSet::new)))
                                        .build();

    }

    /**
     * getStringListAttributeValueMap("lvl_1", 3, 2)
     * {
     *          key_lvl_1_1=AttributeValue(L=[AttributeValue(N=1), AttributeValue(N=2)]),
     *          key_lvl_1_2=AttributeValue(L=[AttributeValue(N=1), AttributeValue(N=2)]),
     *          key_lvl_1_3=AttributeValue(L=[AttributeValue(N=1), AttributeValue(N=2)])
     * }
     */
    private static Map<String, AttributeValue> getStringListAttributeValueMap(String suffixKey, int numberOfKeys ,
                                                                              int nestedListLength ) {

        Map<String, AttributeValue> result = new LinkedHashMap<>();
        IntStream.range(1, numberOfKeys + 1).forEach(n->
                                                         result.put(String.format("key_%s_%d",suffixKey,n),
                                                                    AttributeValue.fromL(IntStream.range(1, nestedListLength+1).mapToObj(numb -> AttributeValue.fromN(String.valueOf(numb))).collect(Collectors.toList())))
        );
        return result;
    }

    /**
     * getStringListObjectMap("lvl_1", 3, 2)
     * {
     *  key_lvl_1_1=[1,2],
     *  key_lvl_1_2=[1,2],
     *  key_lvl_1_3=[1,2]
     * }
     */
    private static Map<String, List<Integer>> getStringListObjectMap(String suffixKey, int numberOfKeys ,
                                                                     int nestedListLength )  {
        Map<String, List<Integer>> result = new LinkedHashMap<>();
        IntStream.range(1, numberOfKeys + 1).forEach( n->
                                                          result.put(String.format("key_%s_%d",suffixKey,n),
                                                                     IntStream.range(1, nestedListLength+1).mapToObj(numb -> Integer.valueOf(numb)).collect(Collectors.toList()))
        );
        return result;
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return testDataInstance().getAllGenericScenarios().stream().map(Arguments::of);
    }

    /**
     * {
     *          key_suffix_1=AttributeValue(S=1),
     *          key_suffix_2=AttributeValue(S=2),
     *          key_suffix_3=AttributeValue(S=3)
     * }
     */
    private static Map<String, AttributeValue> getStringSimpleAttributeValueMap(String suffix, int numberOfElements) {

        return IntStream.range(1, numberOfElements + 1)
                        .boxed()
                        .collect(Collectors.toMap(n -> String.format("key_%s_%d", suffix, n),
                                                  n -> AttributeValue.fromS(String.valueOf(n))
                            , (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    }

    private static CustomClassForDocumentAPI customValueWithBaseAndOffset(int offset, int base) {

        return CustomClassForDocumentAPI.builder()
                                        .instantList(Stream.of(
                                            ofEpochMilli(FIXED_INSTANT_TIME + base +offset) ,ofEpochMilli(FIXED_INSTANT_TIME + base + offset),
                                            ofEpochMilli(FIXED_INSTANT_TIME + base + offset)).collect(Collectors.toList()))
                                        .longNumber(Long.valueOf(base + offset))
                                        .string(String.valueOf(base + offset))
                                        .stringSet(Stream.of(1 + base + offset,2 + base +offset, 3 + base +offset).map(r -> String.valueOf(r)).collect(Collectors.toCollection(LinkedHashSet::new)))

                                        .build();

    }

    /**
     * getStringSimpleMap("suffix", 2, CharSequenceStringConverter.create()))
     *{
     *      key_suffix_1=1,
     *      key_suffix_2=2
     *}
     */
    private static <T>Map<T, String> getStringSimpleMap(String suffix, int numberOfElements, StringConverter<T> stringConverter) {
        return IntStream.range(1, numberOfElements + 1)
                        .boxed()
                        .collect(Collectors.toMap(
                            n ->  stringConverter.fromString(String.format("key_%s_%d", suffix, n)),
                            n -> String.valueOf(n),
                            (key, value) -> key, // merge function to handle collisions
                            LinkedHashMap::new
                        ));
    }

    private <T> Map<String, T> mapFromSimpleKeyValue(Pair<String, T>...keyValuePair) {
        return Stream.of(keyValuePair)
                     .collect(Collectors.toMap(k ->k.left(),
                                               v ->v.right(),
                                               (oldValue, newValue) -> oldValue,
                                               LinkedHashMap::new));
    }
}
