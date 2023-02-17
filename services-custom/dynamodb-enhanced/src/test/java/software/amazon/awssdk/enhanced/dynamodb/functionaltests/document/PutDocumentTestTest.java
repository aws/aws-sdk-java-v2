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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.converter.CustomAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.converter.CustomClass;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.Default2EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

public class PutDocumentTestTest extends LocalDynamoDbSyncTestBase {
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

    @Test
    void putSetOfValues(){

        table.putItem(r -> r.item(EnhancedDocument.builder()
                                                  .addString(HASH_KEY, "first_hash")
                                                  .addString(SORT_KEY, "1_sort")
                                                  .addStringSet("stringSet",
                                                                Stream.of("str1", "str2", "str2").collect(Collectors.toSet()))
                                                  .addNumberSet("numberSet",
                                                                Stream.of(1, new BigDecimal(2), 3.00).collect(Collectors.toSet()))
                                                  .addSdkBytesSet("sdk_bytes",
                                                                  Stream.of(SdkBytes.fromUtf8String("a"),
                                                                            SdkBytes.fromUtf8String("b")).collect(Collectors.toSet()))
                                                  .build()));

    }


    @Test
    void putCustomClass_with_NoConverterInDoc(){
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () ->         table.putItem(EnhancedDocument.builder()
                                                        // Combination of different Types
                                                        .add("customClass", CustomClass.builder().foo("value").build())
                                                        .build())

        );

    }

    @Test
    void putListValues() {

        EnhancedDocument.Builder builder = Default2EnhancedDocument.builder()
                                                                   .addString(HASH_KEY, "first_hash")
                                                                   .addString(SORT_KEY, "1_sort")

                                                                   // Combination of different Types
                                                                   .addList("numberList",
                                                                    Stream.of(BigDecimal.valueOf(1), "word").collect(Collectors.toList()))


                                                                   .addList("customClass",
                                                                    Arrays.asList(CustomClass.builder().foo("value").build(),
                                                                                  CustomClass.builder().foo("value_2").build()))
                                                                   .attributeConverterProviders(
                                                               CustomAttributeConverterProvider.create(),
                                                               AttributeConverterProvider.defaultProvider())
                                       ;

                                                      ;

        String s = builder.build().toJson();
        System.out.println(s);

        table.putItem(EnhancedDocument.builder()
                                      .addString(HASH_KEY, "first_hash")
                                      .addString(SORT_KEY, "1_sort")
                                      // Combination of different Types
                                      .addList("numberList",
                                               Stream.of(BigDecimal.valueOf(1), "word").collect(Collectors.toList()))

                                      .addList("customClass", Collections.singletonList(CustomClass.builder().foo("value").build()))

                          .attributeConverterProviders(AttributeConverterProvider.defaultProvider(),
                                                       CustomAttributeConverterProvider.create())

                                      .build());


        System.out.println(EnhancedDocument.builder()
                                           .addString(HASH_KEY, "first_hash")
                                           .addString(SORT_KEY, "1_sort")
                                           .addList("numberList",
                                                    Stream.of(BigDecimal.valueOf(1), "word").collect(Collectors.toList()))

                                           .build().toJson());


    }


    @Test
    void putSimpleDocumentWithNoHash() {

        // ServiceSideException
        assertThatExceptionOfType(DynamoDbException.class)
            .isThrownBy(() ->table.putItem(EnhancedDocument.builder()
                                                           .addString(HASH_KEY, "first_hash")
                                                           .build()))
            .withMessageContaining("One of the required keys was not given a value (Service: DynamoDb, Status Code: 400");


        assertThatExceptionOfType(DynamoDbException.class)
            .isThrownBy(() ->table.putItem(EnhancedDocument
                                               .builder()
                                               .addString(SORT_KEY, "1_sort")
                                               .build()))
            .withMessageContaining("One of the required keys was not given a value (Service: DynamoDb, Status Code: 400");


    }








}
