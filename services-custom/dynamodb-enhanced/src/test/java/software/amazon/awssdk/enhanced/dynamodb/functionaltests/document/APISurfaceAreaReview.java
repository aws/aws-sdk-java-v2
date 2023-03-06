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
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
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
     * package      :software.amazon.awssdk.enhanced.dynamodb
     * API Access   :Public API
     */


    /**
     * The Surface API focuses on the creation and retrieval of attributes using the DocumentAPI.
     * It covers the creation of {@link EnhancedDocument} during a Put operation and retrieval of attributes during a
     * Get operation from DDB. While it doesn't provide in-depth details about EnhancedDynamoDB operations,
     * it does cover the usage of EnhancedDocument with Enhanced DynamoDB operations.
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
            enhancedClient.table(tableName, TableSchema.documentSchemaBuilder()
                                                       .addIndexPartitionKey(TableMetadata.primaryIndexName(),"sampleHashKey", AttributeValueType.S)
                                                       .addIndexSortKey("sort-index","sampleSortKey", AttributeValueType.S)
                                                       .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(),
                                                                                    defaultProvider())
                                                       .build());





        /**
         * Creating SCHEMA with Primary and secondary keys
         */

        DocumentTableSchema tableSchema = TableSchema.documentSchemaBuilder()
                                                     .addIndexPartitionKey(TableMetadata.primaryIndexName(),"sampleHashKey", AttributeValueType.S)
                                                     .addIndexSortKey("sort-index","sampleSortKey", AttributeValueType.S)
                                                     .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(),
                                                                                  defaultProvider())
                                                     .build();

        assertThat(tableSchema.attributeNames()).isEqualTo(Arrays.asList("sampleHashKey", "sampleSortKey"));


        /**
         * Creating SCHEMA with NO Primary and secondary keys

         */

        DocumentTableSchema tableSchemaNoKey = TableSchema.documentSchemaBuilder()
                                                          .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(),
                                                                                       defaultProvider())
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
            .attributeConverterProviders(defaultProvider())
                                                     .putString("HashKey", "abcdefg123")
                                                     .putNull("nullKey")
                                                     .putNumber("numberKey", 2.0)
                                                     .putBytes("sdkByte", SdkBytes.fromUtf8String("a"))
                                                     .putBoolean("booleanKey", true)
                                                     .putJson("jsonKey", "{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}")
                                                     .putStringSet("stingSet",
                                                                   Stream.of("a", "b", "c").collect(Collectors.toSet()))

                                                     .putNumberSet("numberSet", Stream.of(1, 2, 3, 4).collect(Collectors.toSet()))
                                                     .putBytesSet("sdkByteSet",
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

        assertThat(simpleDoc.toJson()).isEqualTo("{\"HashKey\": \"abcdefg123\", \"nullKey\": null, \"numberKey\": 2.0, "
                                                 + "\"sdkByte\": \"a\", \"booleanKey\": true, \"jsonKey\": {\"1\": [\"a\", "
                                                 + "\"b\", \"c\"],\"2\": 1}, \"stingSet\": [\"a\", \"b\", \"c\"], "
                                                 + "\"numberSet\": [1, 2, 3, 4], \"sdkByteSet\": [\"a\"]}");


        assertThat(simpleDoc.isPresent("HashKey")).isTrue();
        // No Null pointer or doesnot exist is thrown
        assertThat(simpleDoc.isPresent("HashKey2")).isFalse();
        assertThat(simpleDoc.getString("HashKey")).isEqualTo("abcdefg123");
        assertThat(simpleDoc.isNull("nullKey")).isTrue();

        assertThat(simpleDoc.getNumber("numberKey")).isNotEqualTo(2.0);
        assertThat(simpleDoc.getNumber("numberKey").doubleValue()).isEqualTo(2.0);

        assertThat(simpleDoc.getBytes("sdkByte")).isEqualTo(SdkBytes.fromUtf8String("a"));
        assertThat(simpleDoc.getBoolean("booleanKey")).isTrue();
        assertThat(simpleDoc.getJson("jsonKey")).isEqualTo("{\"1\": [\"a\", \"b\", \"c\"],\"2\": 1}");
        assertThat(simpleDoc.getStringSet("stingSet")).isEqualTo(Stream.of("a", "b", "c").collect(Collectors.toSet()));

        assertThat(simpleDoc.getNumberSet("numberSet")
                            .stream().map(n -> n.intValue()).collect(Collectors.toSet()))
            .isEqualTo(Stream.of(1, 2, 3, 4).collect(Collectors.toSet()));


        assertThat(simpleDoc.getBytesSet("sdkByteSet")).isEqualTo(Stream.of(SdkBytes.fromUtf8String("a")).collect(Collectors.toSet()));


        // Trying to access some other Types
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> simpleDoc.getBoolean("sdkByteSet"))
                                                              .withMessageContaining("BooleanAttributeConverter cannot convert "
                                                                                     + "an attribute of type BS into the requested type class java.lang.Boolean");


    }

    @Test
    void generic_access_to_enhancedDcoument_attributes() {

        // test code ignore- start
        CustomClassForDocumentAPI customObject = getCustomObject("str", 25L, false);
        CustomClassForDocumentAPI customObjectOne = getCustomObject("str_one", 26L, false);
        CustomClassForDocumentAPI customObjectTwo = getCustomObject("str_two", 27L, false);
        // test code ignore- end


        // Class which directly adds a Custom Object by specifying the
        EnhancedDocument customDoc = EnhancedDocument.builder()
                                                            .attributeConverterProviders(
                                                                CustomAttributeForDocumentConverterProvider.create(),
                                                                defaultProvider()
                                                            )
                                                            .putString("direct_attr", "sample_value")
                                                            .putWithType("custom_attr", customObject,
                                                                         EnhancedType.of(CustomClassForDocumentAPI.class))
                                                            .putList(
                                                                "custom_list"
                                                                , Arrays.asList(customObjectOne, customObjectTwo)
                                                                , EnhancedType.of(CustomClassForDocumentAPI.class)
                                                            )
                                                     .build();

        assertThat(customDoc.toJson()).isEqualTo("{\"direct_attr\": \"sample_value\", \"custom_attr\": {\"string\": \"str\","
                                                 + "\"longNumber\": 25}, \"custom_list\": [{\"string\": \"str_one\","
                                                 + "\"longNumber\": 26}, {\"string\": \"str_two\",\"longNumber\": 27}]}");


        // Extracting Custom Object\
        CustomClassForDocumentAPI customAttr = customDoc.get("custom_attr", EnhancedType.of(CustomClassForDocumentAPI.class));


        customDoc.get("custom_attr", EnhancedType.of(CustomClassForDocumentAPI.class));

        System.out.println("customAttr " +customAttr);
        assertThat(customAttr).isEqualTo(customObject);


        // Extracting custom List
        List<CustomClassForDocumentAPI> extractedList = customDoc.getList("custom_list",
                                                                        EnhancedType.of(CustomClassForDocumentAPI.class));

        assertThat(extractedList).isEqualTo(Arrays.asList(customObjectOne,customObjectTwo));

    }


    /**
     * ERROR Case when Attribute Converters are not supplied and a custom Object is added
     */
    @Test
    void attributeConverter_Not_Added(){

        CustomClassForDocumentAPI customObject = getCustomObject("str", 25L, false);

        EnhancedDocument enhancedDocument = EnhancedDocument.builder()
                                                 .putString("direct_attr", "sample_value")
            .attributeConverterProviders(defaultProvider())
                                                 .putWithType("custom_attr", customObject,
                                                              EnhancedType.of(CustomClassForDocumentAPI.class))
                                                 .build();

        // Note that Converter Not found exception is thrown even though we are accessing the string attribute
        // because all the attributes are converted during lazy loading
        //
        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> enhancedDocument.getString("direct_attr")


            ).withMessage("Converter not found for EnhancedType(software.amazon.awssdk.enhanced.dynamodb.converters"
                                      + ".document.CustomClassForDocumentAPI)");

    }


    @Test
    void putSimpleDocumentWithSimpleValues() {
        table.putItem(EnhancedDocument
                          .builder()
                          .putString(HASH_KEY, "first_hash")
                          .putString(SORT_KEY, "1_sort")
                          .putNull("null_Key")
                          .putBoolean("boolean_key", true)
                          .putNumber("number_int", 2)
                          .putNumber("number_SdkNumber", SdkNumber.fromString("5"))
                          .putBytes("SdkBytes", SdkBytes.fromUtf8String("sample_binary"))
                          .build());

        assertThat(
            table.getItem(EnhancedDocument.builder()
                              .attributeConverterProviders(defaultProvider())
                                          .putString(SORT_KEY, "1_sort")
                                          .putString(HASH_KEY, "first_hash").build()).getString(SORT_KEY)).isEqualTo("1_sort");


    }

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
        TableSchema.documentSchemaBuilder()
                   .addIndexPartitionKey(TableMetadata.primaryIndexName(),HASH_KEY, AttributeValueType.S)
                   .addIndexSortKey("sort-key",SORT_KEY, AttributeValueType.S)
                   .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(),
                                                defaultProvider())
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
