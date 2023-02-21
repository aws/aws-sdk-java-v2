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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.PutDocumentTestTest.HASH_KEY;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.PutDocumentTestTest.SORT_KEY;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.converter.CustomAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.mapper.DocumentTableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * TODO : Will not be merged to final master branch
 */
public class APISurfaceAreaReview extends LocalDynamoDbSyncTestBase {


    /**
     * Objectives of Dynamo DB Document API
     *
     * 1. The API provides access to DynamoDB for complex data models without requiring the use of DynamoDB Mapper.
     * For example, it provides APIs for converting data between JSON and AttributeValue formats.
     *
     * 2. The API offers functionality for manipulating semi-structured data for each attribute value. For example, it includes
     *    APIs for accessing AttributeValue as string sets, number sets, string lists, number lists, and more.
     *
     * 3. The API allows direct reading and writing of DynamoDB elements as maps with string keys and AttributeValue value
     */


    /**
     * {@link EnhancedDocument}
     * Changes      : New class
     * Type         :  Interface
     * package      :  software.amazon.awssdk.enhanced.dynamodb.document
     * API Access   : Public API
     */


    /**
     * {@link DocumentTableSchema}
     * Changes       : New class
     * Type         :Implementation of {@link TableSchema}.
     * package      :package software.amazon.awssdk.enhanced.dynamodb.mapper;
     * API Access   :Public API
     */

    /**
     * {@link TableSchema}
     * Changes       : New API added
     * Type         : Interface
     * package      :software.amazon.awssdk.enhanced.dynamodbt
     * API Access   :Public API
     */


    /**
     * Creating a table Schema.
     */
    @Test
    void createTableSchema() {

        // TEST-Code start
        String tableName = getConcreteTableName("table-name");
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(getDynamoDbClient())
                                                                      .build();
        // TEST-Code end

        /**
         * Creating A  TABLE {@link DynamoDbTable specifying primaryKey, sortKey and attributeConverterProviders
         */

        DynamoDbTable<EnhancedDocument> table =
            enhancedClient.table(tableName, TableSchema.fromDocumentSchemaBuilder()
                                                       .primaryKey("sampleHashKey", AttributeValueType.S)
                                                       .sortKey("sampleSortKey", AttributeValueType.S)
                                                       .attributeConverterProviders(CustomAttributeConverterProvider.create(),
                                                                                    AttributeConverterProvider.defaultProvider())
                                                       .build());


        /**
         * Creating SCHEMA with Primary and secondary keys
         */

        DocumentTableSchema tableSchema = TableSchema.fromDocumentSchemaBuilder()
                                                     .primaryKey("sampleHashKey", AttributeValueType.S)
                                                     .sortKey("sampleSortKey", AttributeValueType.S)
                                                     .attributeConverterProviders(CustomAttributeConverterProvider.create(),
                                                                                  AttributeConverterProvider.defaultProvider())
                                                     .build();

        assertThat(tableSchema.attributeNames()).isEqualTo(Arrays.asList("sampleHashKey", "sampleSortKey"));


        /**
         * Creating SCHEMA with NO Primary and secondary keys

         */

        DocumentTableSchema tableSchemaNoKey = TableSchema.fromDocumentSchemaBuilder()
                                                          .attributeConverterProviders(CustomAttributeConverterProvider.create(),
                                                                                       AttributeConverterProvider.defaultProvider())
                                                          .build();
        DynamoDbTable<EnhancedDocument> tableWithNoPrimaryDefined = enhancedClient.table(tableName, tableSchemaNoKey);
        // User cannot create a table

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(
                () -> tableWithNoPrimaryDefined.createTable()
            )
            .withMessage("Attempt to execute an operation that requires a primary index without defining any primary key "
                         + "attributes in the table metadata.");


    }

    /**
     * Question 1 : Should we add API to input {@link TableMetadata} in DocumentTableSchema.Builder ?
     * Note that  {@link TableMetadata} cannot be set by user on DocumentTableSchema.Builder.
     *
     */


    /**
     * Creating implementation of {@link EnhancedDocument} interface
     */

    /**
     * Case 1: Creating simple Enhanced document using default types like String , Number , bytes , boolean.
     */


    @Test
    void enhancedDocDefaultSimpleAttributes() {
        /**
         * No attributeConverters supplied, in this case it uses the {@link DefaultAttributeConverterProvider} and does not error
         */
        EnhancedDocument simpleDoc = EnhancedDocument.builder()
                                                     .addString("HashKey", "abcdefg123")
                                                     .addNull("nullKey")
                                                     .addNumber("numberKey", 2.0)
                                                     .addSdkBytes("sdkByte", SdkBytes.fromUtf8String("a"))
                                                     .addBoolean("booleanKey", true)
                                                     .addJson("jsonKey", "{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}")
                                                     .addStringSet("stingSet",
                                                                   Stream.of("a", "b", "c").collect(Collectors.toSet()))

                                                     .addNumberSet("numberSet", Stream.of(1, 2, 3, 4).collect(Collectors.toSet()))
                                                     .addSdkBytesSet("sdkByteSet",
                                                                     Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet()))
                                                     .build();

        /**
         *{
         *    "HashKey":"abcdefg123",
         *    "nullKey":null,
         *    "numberKey":2.0,
         *    "sdkByte":"a",
         *    "booleanKey":true,
         *    "jsonKey":{
         *       "1":[
         *          "a",
         *          "b",
         *          "c"
         *       ],
         *       "2":1
         *    },
         *    "stingSet":[
         *       "a",
         *       "b",
         *       "c"
         *    ],
         *    "numberSet":[
         *       1,
         *       2,
         *       3,
         *       4
         *    ],
         *    "sdkByteSet":[
         *       "a"
         *    ]
         * }

         */

        assertThat(simpleDoc.toJson()).isEqualTo("{\"HashKey\": \"abcdefg123\",\"nullKey\": null,\"numberKey\": 2.0,"
                                                            + "\"sdkByte\": \"a\",\"booleanKey\": true,\"jsonKey\": {\"1\": "
                                                            + "[\"a\", \"b\", \"c\"],\"2\": 1},\"stingSet\": [\"a\", \"b\", "
                                                            + "\"c\"],\"numberSet\": [1, 2, 3, 4],\"sdkByteSet\": [\"a\"]}");


        assertThat(simpleDoc.isPresent("HashKey")).isTrue();
        // No Null pointer or doesnot exist is thrown
        assertThat(simpleDoc.isPresent("HashKey2")).isFalse();
        assertThat(simpleDoc.getString("HashKey")).isEqualTo("abcdefg123");
        assertThat(simpleDoc.isNull("nullKey")).isTrue();

        assertThat(simpleDoc.getSdkNumber("numberKey")).isNotEqualTo(2.0);
        assertThat(simpleDoc.getSdkNumber("numberKey").doubleValue()).isEqualTo(2.0);

        assertThat(simpleDoc.getSdkBytes("sdkByte")).isEqualTo(SdkBytes.fromUtf8String("a"));
        assertThat(simpleDoc.getBoolean("booleanKey")).isTrue();
        assertThat(simpleDoc.getJson("jsonKey")).isEqualTo("{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}");
        assertThat(simpleDoc.getStringSet("stingSet")).isEqualTo(Stream.of("a", "b", "c").collect(Collectors.toSet()));
        
        assertThat(simpleDoc.getNumberSet("numberSet")
                            .stream().map(n -> n.intValue()).collect(Collectors.toSet()))
            .isEqualTo(Stream.of(1, 2, 3, 4).collect(Collectors.toSet()));
        
        
        assertThat(simpleDoc.getSdkBytesSet("sdkByteSet")).isEqualTo(Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet()));


        // Trying to access some other Types
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> simpleDoc.getBoolean("sdkByteSet"))
                                                              .withMessage("Value of attribute sdkByteSet of type "
                                                                           + "EnhancedType(java.util.Collections$UnmodifiableRandomAccessList) "
                                                                           + "cannot be converted into a Boolean value.");

        
    }

    @Test
    void generic_access_to_enhancedDcoument_attributes() {

        // test code ignore- start
        CustomClassForDocumentAPI customObject = getCustomObject("str", 25L, false);
        CustomClassForDocumentAPI customObjectOne = getCustomObject("str_one", 26L, false);
        CustomClassForDocumentAPI customObjectTwo = getCustomObject("str_two", 27L, true);
        // test code ignore- end


        // Class which directly adds a Custom Object by specifying the
        EnhancedDocument customDoc = EnhancedDocument.builder()
                                                            .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create())
                                                            .addString("direct_attr", "sample_value")
                                                            .add("custom_attr", customObject)
                                                            .addList("customO_list", Arrays.asList(customObjectOne,
                                                                                                   customObjectTwo)).build();

        assertThat(customDoc.toJson()).isEqualTo("{\"direct_attr\": \"sample_value\",\"custom_attr\": {\"aBoolean\": false,"
                                                 + "\"string\": \"str\",\"longNumber\": 25},\"customO_list\": [{\"aBoolean\": "
                                                 + "false,\"string\": \"str_one\",\"longNumber\": 26}, {\"aBoolean\": true,"
                                                 + "\"string\": \"str_two\",\"longNumber\": 27}]}");


        // Extracting Custom Object\
        CustomClassForDocumentAPI customAttr = customDoc.get("custom_attr", EnhancedType.of(CustomClassForDocumentAPI.class));
        System.out.println("customAttr " +customAttr);
        assertThat(customAttr).isEqualTo(customObject);


        // Extracting custom List
        List<CustomClassForDocumentAPI> extractedList = customDoc.getList("customO_list",
                                                                        EnhancedType.of(CustomClassForDocumentAPI.class));

        assertThat(extractedList).isEqualTo(Arrays.asList(customObjectOne,customObjectTwo));

    }


    /**
     * ERROR Case when Attribute Converters are not supplied and a custom Object is added
     */
    @Test
    void attributeConverter_Not_Added(){

        CustomClassForDocumentAPI customObject = getCustomObject("str", 25L, false);

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() ->
                EnhancedDocument.builder()
                                .addString("direct_attr", "sample_value")
                                .add("custom_attr", customObject)
                                .build()

            ).withMessage("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters"
                                      + ".document.CustomClassForDocumentAPI)");

    }


    @Test
    void putSimpleDocumentWithSimpleValues() {
        table.putItem(EnhancedDocument
                          .builder()
                          .addString(HASH_KEY, "first_hash")
                          .addString(SORT_KEY, "1_sort")
                          .addNull("null_Key")
                          .addBoolean("boolean_key", true)
                          .addNumber("number_int", 2)
                          .addNumber("number_SdkNumber", SdkNumber.fromString("5"))
                          .addSdkBytes("SdkBytes", SdkBytes.fromUtf8String("sample_binary"))
                          .build());

        assertThat(
            table.getItem(EnhancedDocument.builder()
                                          .add(SORT_KEY, "1_sort")
                                          .addString(HASH_KEY, "first_hash").build()).get(SORT_KEY)).isEqualTo("1_sort");


    }

    /**
     *- Does EnhancedDocument.Builder needs add(key, Object, attributeConverterProvider)
     *
     * - Should we refer bytes as binary or byte in apis example addBinary(byte[] byte) or addByte(byte[] byte)
     *
     * - Map<String, Object> getRawMap(String attributeName); ==> Can we remove the use of Object ? <Zoe>
     *
     * - getJsonPretty. Do we actually need this?
     *
     * - Object get(String attributeName); ==> Do we need this
     * -  We don't have jsonPretty utility elsewhere. Do we need it?
     *
     * - Builder add(String attributeName, Object value); ==> Using raw type is a bit code smell and error prone to me.Should  it
     *  be add(String attrbuteName, EnhancedAttributeValue value)?
     *
     * - Builder keyComponent(KeyAttributeMetadata keyAttrName, Object keyAttrValue); ==> Use case
     * - Boolean getBoolean(String attributeName) throws Illegal state exceptions ??
     * - Should we have toAttributeValueMap and fromAttributeValueMap methods in EnhancedDocument?
     */


    //END OF Surface API Review

    private static CustomClassForDocumentAPI getCustomObject(String str_one, long longNumber, boolean aBoolean) {
        return CustomClassForDocumentAPI.builder().string(str_one)
                                        .longNumber(longNumber)
                                        .aBoolean(aBoolean).build();
    }

    String tableName = getConcreteTableName("table-name");

    private  final DynamoDbClient dynamoDbClient = getDynamoDbClient();
    private  final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                 .dynamoDbClient(dynamoDbClient)
                                                                                 .build();
    public static final String HASH_KEY = "sampleHashKey";
    public static final String SORT_KEY = "sampleSortKey";
    private  DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
        tableName,
        TableSchema.fromDocumentSchemaBuilder()
                   .primaryKey(HASH_KEY, AttributeValueType.S)
                   .sortKey(SORT_KEY, AttributeValueType.S)
                   .attributeConverterProviders(CustomAttributeConverterProvider.create(),
                                                AttributeConverterProvider.defaultProvider())
                   .build());


    @BeforeEach
    public void setUp(){
        table.createTable();
    }

    @AfterEach
    public void clearAll(){
        table.describeTable();
    }


}
