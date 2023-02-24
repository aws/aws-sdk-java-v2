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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;


import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

public class DocumentSchemaTableTest extends LocalDynamoDbSyncTestBase {

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private final DynamoDbTable<EnhancedDocument> mappedTable = enhancedClient.table(
        getConcreteTableName("table-name"),
        TableSchema.fromDocumentSchemaBuilder()
                   .primaryKey("sampleHashKey", AttributeValueType.S)
                   .sortKey("sampleSortKey", AttributeValueType.S)
                   .build());




    // Creating a table

    @Test
    void createTableWithPrimaryKeyAndSortKey(){
        DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
            getConcreteTableName("table-name"),
            TableSchema.fromDocumentSchemaBuilder()
                       .primaryKey("sampleHashKey", AttributeValueType.S)
                       .sortKey("sampleSortKey", AttributeValueType.S)
                       .build());


        table.createTable();

        //assert Table exist

    }


    @Test
    void createTableWithOutPrimaryKeyAndSortKey(){
        DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
            getConcreteTableName("table-name"),
            TableSchema.fromDocumentSchemaBuilder()
                       .build());

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() ->table.createTable())
            .withMessage("Attempt to execute an operation that requires a primary index without defining any primary key attributes in the table metadata.");
        ;

    }

    @Test
    void createTableWithPrimaryKeyAndNOSortKey(){
        DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
            getConcreteTableName("table-name"),
            TableSchema.fromDocumentSchemaBuilder()
                       .build());

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() ->table.createTable())
            .withMessage("Attempt to execute an operation that requires a primary index without defining any primary key attributes in the table metadata.");
        ;

    }





    @Test
    void putItem() throws InterruptedException {


        mappedTable.createTable(r -> r.provisionedThroughput(t ->t.readCapacityUnits(10L).writeCapacityUnits(10L)));

        System.out.println("mappedTable.tableName() "+mappedTable.tableName());

        mappedTable.putItem(EnhancedDocument.builder()
                                .addString("sampleHashKey", "one_hash")
                                .addString("sampleSortKey", "one_sort")
                                            .build());

        Thread.sleep(2000);
        EnhancedDocument item = mappedTable.getItem(EnhancedDocument.builder()
                                                                    .addString("sampleHashKey", "one_hash")
                                                                    .addString("sampleSortKey", "one_sort")
                                                                    .build());
        System.out.println("item "+item.toJson());
    }


}
