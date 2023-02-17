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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.document.Document2AttributeValueValidator.validateSpecificGetter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.Default2EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Pair;

public class Default2EnhancedDocumentTest {

    public static final String SIMPLE_NUMBER_KEY = "numberKey";
    public static final String BIG_DECIMAL_NUMBER_KEY = "bigDecimalNumberKey";
    public static final String BOOL_KEY = "boolKey";
    public static final String NULL_KEY = "nullKey";
    public static final String NUMBER_SET_KEY = "numberSet";
    public static final String SDK_BYTES_SET_KEY = "sdkBytesSet";
    public static final String STRING_SET_KEY = "stringSet";
    public static final String[] STRINGS_ARRAY = {"a", "b", "c"};
    public static final SdkBytes[] SDK_BYTES_ARRAY = {SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b"),
                                                      SdkBytes.fromUtf8String("c")};
    public static final String[] NUMBER_STRING_ARRAY = {"1", "2", "3"};
    public static final AttributeValue NUMBER_STRING_ARRAY_ATTRIBUTES_LISTS =
        AttributeValue.fromL(Arrays.asList(AttributeValue.fromN(NUMBER_STRING_ARRAY[0]),
                                           AttributeValue.fromN(NUMBER_STRING_ARRAY[1]),
                                           AttributeValue.fromN(NUMBER_STRING_ARRAY[2])));

    public static final AttributeValue STRING_ARRAY_ATTRIBUTES_LISTS =
        AttributeValue.fromL(Arrays.asList(AttributeValue.fromS(STRINGS_ARRAY[0]),
                                           AttributeValue.fromS(STRINGS_ARRAY[1]),
                                           AttributeValue.fromS(STRINGS_ARRAY[2])));
    public static final AttributeStringValueMap ARRAY_MAP_ATTRIBUTE_VALUE = new AttributeStringValueMap()
        .withKeyValue(SIMPLE_NUMBER_KEY, AttributeValue.fromN("1"))
        .withKeyValue("numberList",
                      NUMBER_STRING_ARRAY_ATTRIBUTES_LISTS)
        .withKeyValue("mapKey", AttributeValue.fromM(
            mapFromKeyValuePairs(Pair.of(EnhancedType.listOf(String.class), Arrays.asList(STRINGS_ARRAY)),
                                 Pair.of(EnhancedType.of(Integer.class), 1))));
    static final String SIMPLE_STRING = "stringValue";
    static final String SIMPLE_STRING_KEY = "stringKey";
    static final String SIMPLE_INT_NUMBER = "10";
    public static final String ARRAY_AND_MAP_IN_JSON = "{\"numberKey\": 1,"
                                                       + "\"numberList\": " + "[1, 2, 3],"
                                                       + "\"mapKey\": "
                                                       + "{\"1\": [\"a\", \"b\", \"c\"],"
                                                       + "\"2\": 1}"
                                                       + "}";

    private static Stream<Arguments> attributeValueMapsCorrespondingDocuments() {


        Default2EnhancedDocument simpleKeyValueDoc = (Default2EnhancedDocument)documentBuilder().add(SIMPLE_STRING_KEY,
                                                                                                  SIMPLE_STRING)
                                                  .build();

        return Stream.of(

            //1. Null value // {"nullKey": null}
            Arguments.of(map()
                             .withKeyValue("nullKey", AttributeValue.fromNul(true)),
                         documentBuilder()
                             .addNull("nullKey")
                             .build(),
                         "{" + "\"nullKey\": null" + "}"
            ),

            //2. Simple String
            Arguments.of(map()
                             .withKeyValue(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING)),
                         simpleKeyValueDoc,
                         "{" + "\"stringKey\": \"stringValue\"" + "}"
            ),

            // 3. Different Number Types
            Arguments.of(map()
                             .withKeyValue(SIMPLE_NUMBER_KEY, AttributeValue.fromN(SIMPLE_INT_NUMBER))
                             .withKeyValue(BIG_DECIMAL_NUMBER_KEY, AttributeValue.fromN(new BigDecimal(10).toString()))
                , documentBuilder()
                             .add(SIMPLE_NUMBER_KEY, Integer.valueOf(SIMPLE_INT_NUMBER))
                             .add(BIG_DECIMAL_NUMBER_KEY, new BigDecimal(10))
                             .build(),
                         "{" + "\"numberKey\": 10," + "\"bigDecimalNumberKey\": 10" + "}"

            ),
            // 4. String and Number combination
            Arguments.of(map()
                             .withKeyValue(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING))
                             .withKeyValue(SIMPLE_NUMBER_KEY, AttributeValue.fromN(SIMPLE_INT_NUMBER))
                , documentBuilder()
                             .add(SIMPLE_STRING_KEY, SIMPLE_STRING)
                             .add(SIMPLE_NUMBER_KEY, 10)
                             .build(),
                         "{\"stringKey\": \"stringValue\",\"numberKey\": 10}"
            ),

            // 5. String,Number, Bool, Null together
            Arguments.of(map()
                             .withKeyValue(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING))
                             .withKeyValue(SIMPLE_NUMBER_KEY, AttributeValue.fromN(SIMPLE_INT_NUMBER))
                             .withKeyValue(BOOL_KEY, AttributeValue.fromBool(true))
                             .withKeyValue(NULL_KEY, AttributeValue.fromNul(true))
                , documentBuilder()
                             .add(SIMPLE_STRING_KEY, SIMPLE_STRING)
                             .add(SIMPLE_NUMBER_KEY, 10)
                             .add(BOOL_KEY, true)
                             .add(NULL_KEY, null)
                             .build()
                , "{\"stringKey\": \"stringValue\",\"numberKey\": 10,\"boolKey\": true,\"nullKey\": null}"
            ),

            //6. Nested Array with a map
            Arguments.of(
                map()
                    .withKeyValue("numberStringSet", AttributeValue.fromL(Arrays.asList(
                        AttributeValue.fromS("One"),
                        AttributeValue.fromN("1"),
                        AttributeValue.fromNul(true),
                        AttributeValue.fromSs(new ArrayList<>()),
                        AttributeValue.fromBs(
                            Arrays.asList(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b"))),
                        AttributeValue.fromM(mapFromSimpleKeyAttributeValue(Pair.of(SIMPLE_NUMBER_KEY,
                                                                                    AttributeValue.fromS(SIMPLE_STRING))))
                    )))
                , documentBuilder()
                    .addList("numberStringSet",
                             Arrays.asList("One",
                                           1,
                                           null,
                                           new HashSet<String>(),
                                           getSdkBytesSet(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b")),
                                           mapFromSimpleKeyValue(Pair.of(SIMPLE_NUMBER_KEY, SIMPLE_STRING))
                             ))
                    .build()
                , "{\"numberStringSet\": [\"One\", 1, null, [], [\"a\", \"b\"], {\"numberKey\": \"stringValue\"}]}"),

            // 7. Different kinds of Sets together
            Arguments.of(map()
                             .withKeyValue(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING))
                             .withKeyValue(NUMBER_SET_KEY, AttributeValue.fromNs(Arrays.asList(NUMBER_STRING_ARRAY)))
                             .withKeyValue(SDK_BYTES_SET_KEY, AttributeValue.fromBs(Arrays.asList(SDK_BYTES_ARRAY)))
                             .withKeyValue(STRING_SET_KEY, AttributeValue.fromSs(Arrays.asList(STRINGS_ARRAY))),
                         documentBuilder()
                             .add(SIMPLE_STRING_KEY, SIMPLE_STRING)
                             .addNumberSet(NUMBER_SET_KEY, getNumberSet(1, 2, 3))
                             .addSdkBytesSet(SDK_BYTES_SET_KEY, getSdkBytesSet(SDK_BYTES_ARRAY[0],
                                                                               SDK_BYTES_ARRAY[1],
                                                                               SDK_BYTES_ARRAY[2]))
                             .addStringSet(STRING_SET_KEY, getStringSet(STRINGS_ARRAY))
                             .build(),
                         "{\"stringKey\": \"stringValue\",\"numberSet\": [1, 2, 3],\"sdkBytesSet\": [\"a\", \"b\", \"c\"],"
                         + "\"stringSet\": [\"a\", \"b\", \"c\"]}"),

            //  8. List , Map and Simple Type together
            Arguments.of(map()
                             .withKeyValue(SIMPLE_NUMBER_KEY, AttributeValue.fromN("1"))
                             .withKeyValue("numberList",
                                           NUMBER_STRING_ARRAY_ATTRIBUTES_LISTS)
                             .withKeyValue("sdkByteKey", AttributeValue.fromB(SdkBytes.fromUtf8String("a")))
                             .withKeyValue("mapKey", AttributeValue.fromM(
                                 mapFromKeyValuePairs(Pair.of(EnhancedType.listOf(String.class), Arrays.asList(STRINGS_ARRAY)),
                                                      Pair.of(EnhancedType.of(Integer.class), 1)

                                 ))),
                         documentBuilder()
                             .add(SIMPLE_NUMBER_KEY, 1)
                             .addList("numberList", Arrays.asList(1, 2, 3))
                             .addSdkBytes("sdkByteKey", SdkBytes.fromUtf8String("a"))
                             .addMap("mapKey", mapFromSimpleKeyValue(
                                 Pair.of("1", Arrays.asList(STRINGS_ARRAY)),
                                 Pair.of("2", 1)
                             ))
                             .build(),
                         "{\"numberKey\": 1,\"numberList\": [1, 2, 3],\"sdkByteKey\": \"a\",\"mapKey\": {\"1\": [\"a\", \"b\", "
                         + "\"c\"],\"2\": 1}}"),



            //10 .Construction of document from EnhancedDocument
            Arguments.of(map()
                             .withKeyValue("level1_k1", AttributeValue.fromM(
                                 mapFromSimpleKeyAttributeValue(Pair.of(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING)))))
                             .withKeyValue("level1_k2", AttributeValue.fromM(

                                 mapFromSimpleKeyAttributeValue(Pair.of(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING)),
                                                                Pair.of("level2_k1", AttributeValue.fromM(
                                                                    mapFromSimpleKeyAttributeValue(
                                                                        Pair.of(SIMPLE_STRING_KEY, AttributeValue.fromS(SIMPLE_STRING)))))))),
                         documentBuilder()
                             .addEnhancedDocument("level1_k1", simpleKeyValueDoc)
                             .addEnhancedDocument("level1_k2" ,
                                                  simpleKeyValueDoc.toBuilder()
                                                                          .addEnhancedDocument("level2_k1",
                                                                                               simpleKeyValueDoc)
                                                                          .build())
                             .build() ,
                         "{\"level1_k1\": {\"stringKey\": \"stringValue\"},\"level1_k2\": {\"stringKey\": \"stringValue\","
                         + "\"level2_k1\": {\"stringKey\": \"stringValue\"}}}"));
    }

    private static Map<String, Object> mapFromSimpleKeyValue(Pair<String, Object>... pairs) {
        return Stream.of(pairs).collect(Collectors.toMap(Pair::left, Pair::right, (a, b) -> b));
    }

    private static Map<String, AttributeValue> mapFromSimpleKeyAttributeValue(Pair<String, AttributeValue>... pairs) {
        return Stream.of(pairs).collect(Collectors.toMap(Pair::left, Pair::right, (a, b) -> b));
    }

    private static Map<String, AttributeValue> mapFromKeyValuePairs(Pair<EnhancedType, Object>... pairs) {
        Map<String, AttributeValue> result = new HashMap<>();
        DefaultAttributeConverterProvider provider = DefaultAttributeConverterProvider.create();
        AtomicInteger index = new AtomicInteger(0);
        Stream.of(pairs).forEach(pair ->
                                 {
                                     index.incrementAndGet();
                                     result.put(index.toString(), provider.converterFor(pair.left()).transformFrom(pair.right()));
                                 });
        return result;
    }

    private static Set<Number> getNumberSet(Number... numbers) {
        return Stream.of(numbers).map(number -> SdkNumber.fromString(number.toString())).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<SdkBytes> getSdkBytesSet(SdkBytes... sdkBytes) {
        return Stream.of(sdkBytes).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> getStringSet(String... strings) {
        return Stream.of(strings).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Default2EnhancedDocument.DefaultBuilder documentBuilder() {
        Default2EnhancedDocument.DefaultBuilder defaultBuilder = Default2EnhancedDocument.builder();
        defaultBuilder.addAttributeConverterProvider(AttributeConverterProvider.defaultProvider());
        return defaultBuilder;
    }

    public static AttributeStringValueMap map() {
        return new AttributeStringValueMap();
    }

    public static void validateAttributeValueMapAndDocument(AttributeStringValueMap attributeStringValueMap,
                                                             Default2EnhancedDocument enhancedDocument) {
        // assert for keys in Document
        assertThat(attributeStringValueMap.getAttributeValueMap().keySet()).isEqualTo(enhancedDocument.asMap().keySet());

        attributeStringValueMap
            .getAttributeValueMap()
            .entrySet().forEach(
                entry -> {
                    assertThat(validateSpecificGetter(entry.getValue(), enhancedDocument, entry.getKey())).isTrue();
                }
            );
    }

    @ParameterizedTest
    @MethodSource("attributeValueMapsCorrespondingDocuments")
    void validate_BuilderMethodsOfDefaultDocument(AttributeStringValueMap expectedMap,
                                                  Default2EnhancedDocument enhancedDocument,
                                                  String expectedJson) {
        /**
         * The builder method internally creates a AttributeValueMap which is saved to the ddb, if this matches then
         * the document is as expected
         */
        assertThat(enhancedDocument.toAttributeValueMap()).isEqualTo(expectedMap.getAttributeValueMap());
    }

    @ParameterizedTest
    @MethodSource("attributeValueMapsCorrespondingDocuments")
    void validate_GetterMethodsOfDefaultDocument(AttributeStringValueMap expectedMap,
                                                 Default2EnhancedDocument enhancedDocument,
                                                 String expectedJson) {
        Default2EnhancedDocument Default2EnhancedDocument = (Default2EnhancedDocument) enhancedDocument
            .builder()
            .attributeValueMap(expectedMap.getAttributeValueMap())
            .addAttributeConverterProvider(DefaultAttributeConverterProvider.create())
            .build();

       validateAttributeValueMapAndDocument(expectedMap, Default2EnhancedDocument);
        assertThat(Default2EnhancedDocument.toJson()).isEqualTo(expectedJson);
    }

    @Test
    void copyCreatedFromToBuilder(){
        Default2EnhancedDocument originalDoc = (Default2EnhancedDocument) documentBuilder()
            .add(SIMPLE_STRING_KEY, SIMPLE_STRING)
            .build();
        Default2EnhancedDocument copiedDoc = (Default2EnhancedDocument)  originalDoc.toBuilder().build();
        Default2EnhancedDocument copyAndAlter =
            (Default2EnhancedDocument)  originalDoc.toBuilder().addString("keyOne", "valueOne").build();
        assertThat(originalDoc.toAttributeValueMap()).isEqualTo(copiedDoc.toAttributeValueMap());
        assertThat(originalDoc.asMap().keySet().size()).isEqualTo(1);
        assertThat(copyAndAlter.asMap().keySet().size()).isEqualTo(2);
        assertThat(copyAndAlter.getString(SIMPLE_STRING_KEY)).isEqualTo(SIMPLE_STRING);
        assertThat(copyAndAlter.getString("keyOne")).isEqualTo("valueOne");
        assertThat(originalDoc).isEqualTo(copiedDoc);
    }

    @Test
    void nullDocumentGet(){

        Default2EnhancedDocument nullDocument = (Default2EnhancedDocument) documentBuilder()
            .addNull("nullDocument")
            .addString("nonNull", "stringValue")
            .build();
        assertThat(nullDocument.isNull("nullDocument")).isTrue();
        assertThat(nullDocument.isNull("nonNull")).isFalse();
        assertThat(nullDocument.toAttributeValueMap().get("nullDocument")).isEqualTo(AttributeValue.fromNul(true));

        Default2EnhancedDocument document = (Default2EnhancedDocument) Default2EnhancedDocument
            .builder().attributeValueMap(
                mapFromSimpleKeyAttributeValue(Pair.of("nullAttribute", AttributeValue.fromNul(true))))
            .addAttributeConverterProvider(DefaultAttributeConverterProvider.create())
            .build();

        assertThat(document.isNull("nullAttribute")).isTrue();
    }

    @Test
    void multipleGetterForDocument(){

        Default2EnhancedDocument document = (Default2EnhancedDocument) documentBuilder()
            .add("nullKey", null)
            .add(SIMPLE_NUMBER_KEY, 1)
            .add(SIMPLE_STRING_KEY, SIMPLE_STRING)
            .addList("numberList", Arrays.asList(1, 2, 3))
            .add("simpleDate", LocalDate.MIN)
            .addStringSet("stringSet", Stream.of("one", "two").collect(Collectors.toSet()))
            .addSdkBytes("sdkByteKey", SdkBytes.fromUtf8String("a"))
            .addSdkBytesSet("sdkByteSet",
                            Stream.of(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b")).collect(Collectors.toSet()))
            .addNumberSet("numberSetSet", Stream.of(1, 2).collect(Collectors.toSet()))
            .addList("numberList", Arrays.asList(1, 2, 3))
            .addMap("simpleMap", mapFromSimpleKeyValue(Pair.of("k1", 3), Pair.of("k2", 9)))
            .addMap("mapKey", mapFromSimpleKeyValue(Pair.of("1", Arrays.asList(STRINGS_ARRAY)), Pair.of("2", 1)))
            .addEnhancedDocument("nestedDoc", documentBuilder().addStringSet("innerKey" ,
                                                                             getStringSet(STRINGS_ARRAY)).build())
            .build();

        assertThat(document.getString(SIMPLE_STRING_KEY)).isEqualTo(SIMPLE_STRING);
        assertThat(document.getSdkNumber(SIMPLE_NUMBER_KEY).intValue()).isEqualTo(1);

        assertThat(document.getList("numberList", EnhancedType.of(BigDecimal.class))).isEqualTo(Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
        assertThat(document.getList("numberList")).isEqualTo(Arrays.asList(SdkNumber.fromInteger(1),
                                                                           SdkNumber.fromInteger(2),
                                                                           SdkNumber.fromInteger(3)));

        assertThat(document.get("simpleDate", EnhancedType.of(LocalDate.class))).isEqualTo(LocalDate.MIN);
        assertThat(document.getStringSet("stringSet")).isEqualTo(Stream.of("one", "two").collect(Collectors.toSet()));
        assertThat(document.getSdkBytes("sdkByteKey")).isEqualTo(SdkBytes.fromUtf8String("a"));
        assertThat(document.getSdkBytesSet("sdkByteSet")).isEqualTo(Stream.of(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b")).collect(Collectors.toSet()));
        assertThat(document.getNumberSet("numberSetSet")).isEqualTo(Stream.of(SdkNumber.fromInteger(1),
                                                                              SdkNumber.fromInteger(2)).collect(Collectors.toSet()));
        assertThat(document.getList("numberList").containsAll(Arrays.asList(SdkNumber.fromInteger(1),
                                                                            SdkNumber.fromInteger(2),
                                                                            SdkNumber.fromInteger(3)))).isTrue();


        Map<String , BigDecimal> bigDecimalMap = new LinkedHashMap<>();
        bigDecimalMap.put("k1", BigDecimal.valueOf(3));
        bigDecimalMap.put("k2", BigDecimal.valueOf(9));
        assertThat(document.getMap("simpleMap", EnhancedType.of(BigDecimal.class))).isEqualTo(bigDecimalMap);
        assertThat(document.getMapAsDocument("nestedDoc").getStringSet("innerKey")).isEqualTo(getStringSet(STRINGS_ARRAY));
        assertThat(document.getTypeOf("nullKey")).isNull();
    }

    public static class AttributeStringValueMap {
        Map<String, AttributeValue> attributeValueMap = new LinkedHashMap<>();

        public Map<String, AttributeValue> getAttributeValueMap() {
            return attributeValueMap;
        }

        AttributeStringValueMap withKeyValue(String key, AttributeValue value) {
            attributeValueMap.put(key, value);
            return this;
        }
    }
}
