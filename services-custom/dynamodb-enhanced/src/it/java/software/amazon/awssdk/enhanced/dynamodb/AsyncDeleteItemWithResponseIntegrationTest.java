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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import java.util.Objects;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedResponse;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;

public class AsyncDeleteItemWithResponseIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {
    private static class Record {
        private Integer id;
        private Integer id2;
        private String stringAttr1;

        private Integer getId() {
            return id;
        }

        private Record setId(Integer id) {
            this.id = id;
            return this;
        }

        private Integer getId2() {
            return id2;
        }

        private Record setId2(Integer id2) {
            this.id2 = id2;
            return this;
        }

        private String getStringAttr1() {
            return stringAttr1;
        }

        private Record setStringAttr1(String stringAttr1) {
            this.stringAttr1 = stringAttr1;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Record record = (Record) o;
            return Objects.equals(id, record.id)
                   && Objects.equals(id2, record.id2)
                   && Objects.equals(stringAttr1, record.stringAttr1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, id2, stringAttr1);
        }
    }

    private static final String TABLE_NAME = createTestTableName();

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(Integer.class, a -> a.name("id_1")
                                                            .getter(Record::getId)
                                                            .setter(Record::setId)
                                                            .tags(primaryPartitionKey(), secondaryPartitionKey("index1")))
                         .addAttribute(Integer.class, a -> a.name("id_2")
                                                            .getter(Record::getId2)
                                                            .setter(Record::setId2)
                                                            .tags(primarySortKey(), secondarySortKey("index1")))
                         .addAttribute(String.class, a -> a.name("stringAttr1")
                                                           .getter(Record::getStringAttr1)
                                                           .setter(Record::setStringAttr1))
                         .build();

    private static final EnhancedLocalSecondaryIndex LOCAL_SECONDARY_INDEX = EnhancedLocalSecondaryIndex.builder()
                                                                                                        .indexName("index1")
                                                                                                        .projection(Projection.builder()
                                                                                                                              .projectionType(ProjectionType.ALL)
                                                                                                                              .build())
                                                                                                        .build();

    private static DynamoDbAsyncClient dynamoDbClient;
    private static DynamoDbEnhancedAsyncClient enhancedClient;
    private static DynamoDbAsyncTable<Record> mappedTable;

    @BeforeClass
    public static void setup() {
        dynamoDbClient = createAsyncDynamoDbClient();
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
        mappedTable.createTable(r -> r.localSecondaryIndices(LOCAL_SECONDARY_INDEX)).join();
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME)).join();
    }

    @AfterClass
    public static void teardown() {
        try {
            dynamoDbClient.deleteTable(r -> r.tableName(TABLE_NAME));
        } finally {
            dynamoDbClient.close();
        }
    }

    @Test
    public void deleteItem_returnConsumedCapacity_unset_consumedCapacityNull() {
        Key key = Key.builder().partitionValue(1).sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response = mappedTable.deleteItemWithResponse(r -> r.key(key)).join();

        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void deleteItem_returnConsumedCapacity_set_consumedCapacityNotNull() {
        Key key = Key.builder().partitionValue(1).sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response =
            mappedTable.deleteItemWithResponse(r -> r.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)).join();

        assertThat(response.consumedCapacity()).isNotNull();
    }

    @Test
    public void delete_returnItemCollectionMetrics_set_itemCollectionMetricsNotNull() {
        Key key = Key.builder().partitionValue(1).sortValue(10).build();

        DeleteItemEnhancedResponse<Record> response =
            mappedTable.deleteItemWithResponse(r -> r.key(key).returnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE))
                       .join();

        assertThat(response.itemCollectionMetrics()).isNotNull();
    }
}
