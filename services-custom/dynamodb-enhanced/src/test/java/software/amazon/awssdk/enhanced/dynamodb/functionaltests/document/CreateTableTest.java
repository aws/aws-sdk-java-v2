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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.document.converter.CustomAttributeConverterProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class CreateTableTest extends LocalDynamoDbSyncTestBase {

    private final DynamoDbClient dynamoDbClient = getDynamoDbClient();
    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(dynamoDbClient)
                                                                                .build();



    // Happy case case
    @Test
    void createTableWithPrimaryKeyAndSortKey() {
        String tableName = getConcreteTableName("table-name");
        DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
            tableName,
            TableSchema.fromDocumentSchemaBuilder()
                       .primaryKey("sampleHashKey", AttributeValueType.S)
                       .sortKey("sampleSortKey", AttributeValueType.S)
                       .attributeConverterProviders(CustomAttributeConverterProvider.create(),
                                                    AttributeConverterProvider.defaultProvider())
                       .build());

        table.createTable();
        assertThat(dynamoDbClient.listTables().tableNames().stream().filter(t -> tableName.equals(t)).findAny()).isPresent();

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


    }

    @Test
    void createTableWithPrimaryKeyAndNOSortKey(){
        String tableName = getConcreteTableName("table-name-no-sort");
        DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
            tableName,
            TableSchema.fromDocumentSchemaBuilder()
                       .primaryKey("sampleHashKey", AttributeValueType.S)
                       .build());

        table.createTable();

        assertThat(dynamoDbClient.listTables().tableNames().stream().filter(t -> tableName.equals(t)).findAny()).isPresent();


    }

    @Test
    void createTableWithPrimaryKeyAndNOSortKeOtherKeyy(){
        String tableName = getConcreteTableName("table-name-no-sort");
        DynamoDbTable<EnhancedDocument> table = enhancedClient.table(
            tableName,
            TableSchema.fromDocumentSchemaBuilder()
                       .primaryKey("sampleHashKey", AttributeValueType.S)
                       .build());

        table.createTable();

        assertThat(dynamoDbClient.listTables().tableNames().stream().filter(t -> tableName.equals(t)).findAny()).isPresent();


    }
}
