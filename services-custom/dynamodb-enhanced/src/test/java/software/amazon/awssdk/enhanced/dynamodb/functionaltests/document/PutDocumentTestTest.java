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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBetween;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomAttributeForDocumentConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.converters.document.CustomClassForDocumentAPI;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
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
        TableSchema.documentSchemaBuilder()
                   .addIndexPartitionKey(TableMetadata.primaryIndexName(),HASH_KEY, AttributeValueType.S)
                   .addIndexPartitionKey("part-index","group", AttributeValueType.S)
                   .addIndexSortKey("sort_index","sort_key2", AttributeValueType.N)
                   .attributeConverterProviders(CustomAttributeForDocumentConverterProvider.create(),
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
                          .putString(HASH_KEY, "first_hash")
                          .putString(SORT_KEY, "1_sort")
                          .putNumber("party", 1 )
                          .build());

        assertThat(
            table.getItem(EnhancedDocument.builder()
                                          .putString(SORT_KEY, "1_sort")
                                          .putString(HASH_KEY, "first_hash").build()).get(SORT_KEY, EnhancedType.of(String.class))).isEqualTo("1_sort");


    }

    @Test
    void putSetOfValues(){
        table.putItem(EnhancedDocument
                          .builder()
                          .putString(HASH_KEY, "first_hash_1")
                          .putString(SORT_KEY, "1_sort")
                          .putNumber("sort_key2", 2)
                          .putNumber("party", 1 )
                          .build());


        table.putItem(EnhancedDocument
                          .builder()
                          .putString(HASH_KEY, "first_hash_2")
                          .putString(SORT_KEY, "1_sort")
                          .putNumber("sort_key2", 3)
                          .putNumber("party", 1 )

                          .build());



        table.putItem(EnhancedDocument
                          .builder()
                          .putString(HASH_KEY, "first_hash_2")
                          .putString(SORT_KEY, "1_sort")
                          .putNumber("sort_key2", 6)
                          .putNumber("party", 1 )
                          .build());

        PageIterable<EnhancedDocument> firstHashOne =
            table.query(sortBetween(r->r.sortValue(3).partitionValue( "part-index"),  s-> s.sortValue(10).partitionValue(
                "part-index")));


        // PageIterable<EnhancedDocument> query =
        //     table.query(q -> q.queryConditional(QueryConditional.sortBeginsWith(Key.builder().sortValue("2").partitionValue(
        //         "first").build())));

    }





    @Test
    void putCustomClass_with_NoConverterInDoc(){
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () ->         table.putItem(EnhancedDocument.builder()
                                                        // Combination of different Types
                                                        .putWithType("customClass",
                                                                     CustomClassForDocumentAPI.builder().string("value").build(),
                                                                     EnhancedType.of(CustomClassForDocumentAPI.class))
                                                        .build())

        );

    }



    @Test
    void putSimpleDocumentWithNoHash() {

        // ServiceSideException
        assertThatExceptionOfType(DynamoDbException.class)
            .isThrownBy(() ->table.putItem(EnhancedDocument.builder()
                                                           .putString(HASH_KEY, "first_hash")
                                                           .build()))
            .withMessageContaining("One of the required keys was not given a value (Service: DynamoDb, Status Code: 400");


        assertThatExceptionOfType(DynamoDbException.class)
            .isThrownBy(() ->table.putItem(EnhancedDocument
                                               .builder()
                                               .putString(SORT_KEY, "1_sort")
                                               .build()))
            .withMessageContaining("One of the required keys was not given a value (Service: DynamoDb, Status Code: 400");


    }








}
