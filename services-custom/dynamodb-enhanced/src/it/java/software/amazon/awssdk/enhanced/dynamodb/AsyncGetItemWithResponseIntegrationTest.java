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

import java.util.Arrays;
import java.util.Objects;
import org.assertj.core.data.Offset;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

public class AsyncGetItemWithResponseIntegrationTest extends DynamoDbEnhancedIntegrationTestBase {

    private static final String TABLE_NAME = createTestTableName();
    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(Integer.class, a -> a.name("id_1")
                                                            .getter(Record::getId)
                                                            .setter(Record::setId)
                                                            .tags(primaryPartitionKey()))
                         .addAttribute(Integer.class, a -> a.name("id_2")
                                                            .getter(Record::getId2)
                                                            .setter(Record::setId2)
                                                            .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("stringAttr1")
                                                           .getter(Record::getStringAttr1)
                                                           .setter(Record::setStringAttr1))
                         .build();

    private static DynamoDbAsyncClient asyncDynamoDbClient;
    private static DynamoDbEnhancedAsyncClient enhancedClient;
    private static DynamoDbAsyncTable<Record> mappedTable;

    @BeforeClass
    public static void setup() {
        asyncDynamoDbClient = createAsyncDynamoDbClient();
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(asyncDynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
        mappedTable.createTable().join();
        asyncDynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME)).join();
    }

    @AfterClass
    public static void teardown() {
        try {
            asyncDynamoDbClient.deleteTable(r -> r.tableName(TABLE_NAME));
        } finally {
            asyncDynamoDbClient.close();
        }
    }

    @Test
    public void getItem_withoutReturnConsumedCapacity() {
        Record record = new Record().setId(101).setId2(102).setStringAttr1(getStringAttrValue(80_000));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getId2())
                     .build();

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(req -> req.key(key)).join();
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void getItem_withReturnConsumedCapacity_eventualConsistent() {
        Record record = new Record().setId(101).setId2(102).setStringAttr1(getStringAttrValue(80 * 1024));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getId2())
                     .build();
        mappedTable.putItem(record).join();

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(
            req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
        ).join();
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        // An eventually consistent read request of an item up to 4 KB requires one-half read request unit.
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(10.0, Offset.offset(1.0));
    }

    @Test
    public void getItem_withReturnConsumedCapacity_stronglyConsistent() {
        Record record = new Record().setId(201).setId2(202).setStringAttr1(getStringAttrValue(80 * 1024));
        Key key = Key.builder()
                     .partitionValue(record.getId())
                     .sortValue(record.getId2())
                     .build();
        mappedTable.putItem(record).join();

        GetItemEnhancedResponse<Record> response = mappedTable.getItemWithResponse(
            req -> req.key(key).returnConsumedCapacity(ReturnConsumedCapacity.TOTAL).consistentRead(true)
        ).join();
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        // A strongly consistent read request of an item up to 4 KB requires one read request unit.
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(20.0, Offset.offset(1.0));
    }

    private static String getStringAttrValue(int numChars) {
        char[] chars = new char[numChars];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }

    private static final class Record {
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
}
