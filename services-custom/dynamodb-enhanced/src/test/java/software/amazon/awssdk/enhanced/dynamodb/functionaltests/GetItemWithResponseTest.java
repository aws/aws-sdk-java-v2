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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.Arrays;
import java.util.Objects;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;


public class GetItemWithResponseTest extends LocalDynamoDbSyncTestBase {

    private static final String TEST_TABLE_NAME = "get-item-table-name-2";
    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(Integer.class, a -> a.name("id_1")
                                                            .getter(Record::getId)
                                                            .setter(Record::setId)
                                                            .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("stringAttr1")
                                                           .getter(Record::getStringAttr1)
                                                           .setter(Record::setStringAttr1))
                         .build();

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    private final DynamoDbTable<Record> mappedTable1 = enhancedClient.table(
        getConcreteTableName(TEST_TABLE_NAME),
        TABLE_SCHEMA
    );

    @Before
    public void createTable() {
        mappedTable1.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(r -> r.tableName(getConcreteTableName(TEST_TABLE_NAME)));
    }

    @Test
    public void getItemWithResponse_when_recordIsAbsent() {
        GetItemEnhancedResponse<Record> response = mappedTable1.getItemWithResponse(
            r -> r.key(k -> k.partitionValue(404))
        );

        assertThat(response.attributes()).isNull();
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void getItemWithResponse_when_recordIsPresent() {
        Record original = new Record().setId(1).setStringAttr1("a");
        mappedTable1.putItem(original);

        GetItemEnhancedResponse<Record> response = mappedTable1.getItemWithResponse(
            r -> r.key(k -> k.partitionValue(original.getId()))
        );

        assertThat(response.attributes()).isNotNull();
        assertThat(response.attributes()).isEqualTo(original);
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void getItemWithResponse_withReturnConsumedCapacity_when_recordIsAbsent_eventuallyConsistent() {
        GetItemEnhancedResponse<Record> response = mappedTable1.getItemWithResponse(
            r -> r.key(k -> k.partitionValue(404))
                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                  .consistentRead(false)
        );

        assertThat(response.attributes()).isNull();
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(0.5, Offset.offset(0.01));
    }

    @Test
    public void getItemWithResponse_withReturnConsumedCapacity_when_recordIsAbsent_stronglyConsistent() {
        GetItemEnhancedResponse<Record> response = mappedTable1.getItemWithResponse(
            r -> r.key(k -> k.partitionValue(404))
                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                  .consistentRead(true)
        );

        assertThat(response.attributes()).isNull();
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(1.0, Offset.offset(0.01));
    }

    @Test
    public void getItemWithResponse_withReturnConsumedCapacity_when_recordIsPresent_eventuallyConsistent() {
        Record original = new Record().setId(126).setStringAttr1(getStringAttrValue(100_000));
        mappedTable1.putItem(original);

        GetItemEnhancedResponse<Record> response = mappedTable1.getItemWithResponse(
            r -> r.key(k -> k.partitionValue(126))
                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                  .consistentRead(false)
        );

        assertThat(response.attributes()).isEqualTo(original);
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(12.5, Offset.offset(0.01));
    }

    @Test
    public void getItemWithResponse_withReturnConsumedCapacity_when_recordIsPresent_stronglyConsistent() {
        Record original = new Record().setId(142).setStringAttr1(getStringAttrValue(100_000));
        mappedTable1.putItem(original);

        GetItemEnhancedResponse<Record> response = mappedTable1.getItemWithResponse(
            r -> r.key(k -> k.partitionValue(142))
                  .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                  .consistentRead(true)
        );

        assertThat(response.attributes()).isEqualTo(original);
        ConsumedCapacity consumedCapacity = response.consumedCapacity();
        assertThat(consumedCapacity).isNotNull();
        assertThat(consumedCapacity.capacityUnits()).isCloseTo(25.0, Offset.offset(0.01));
    }

    private static String getStringAttrValue(int numChars) {
        char[] chars = new char[numChars];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }

    private static class Record {
        private Integer id;
        private String stringAttr1;

        private Integer getId() {
            return id;
        }

        private Record setId(Integer id) {
            this.id = id;
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
            return Objects.equals(id, record.id) && Objects.equals(stringAttr1, record.stringAttr1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, stringAttr1);
        }
    }
}
