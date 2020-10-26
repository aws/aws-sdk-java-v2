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

import java.util.Iterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnum;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnumRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnumShortenedRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FailedConversionSyncTest extends LocalDynamoDbSyncTestBase {
    private static final TableSchema<FakeEnumRecord> TABLE_SCHEMA = TableSchema.fromClass(FakeEnumRecord.class);
    private static final TableSchema<FakeEnumShortenedRecord> SHORT_TABLE_SCHEMA =
            TableSchema.fromClass(FakeEnumShortenedRecord.class);

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private final DynamoDbTable<FakeEnumRecord> mappedTable =
            enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private final DynamoDbTable<FakeEnumShortenedRecord> mappedShortTable =
            enhancedClient.table(getConcreteTableName("table-name"), SHORT_TABLE_SCHEMA);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void exceptionOnRead() {
        FakeEnumRecord record = new FakeEnumRecord();
        record.setId("123");
        record.setEnumAttribute(FakeEnum.TWO);
        mappedTable.putItem(record);

        assertThatThrownBy(() -> mappedShortTable.getItem(Key.builder().partitionValue("123").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TWO")
                .hasMessageContaining("FakeEnumShortened");
    }

    @Test
    public void iterableExceptionOnRead() {
        FakeEnumRecord record = new FakeEnumRecord();
        record.setId("1");
        record.setEnumAttribute(FakeEnum.ONE);
        mappedTable.putItem(record);
        record.setId("2");
        record.setEnumAttribute(FakeEnum.TWO);
        mappedTable.putItem(record);

        Iterator<Page<FakeEnumShortenedRecord>> results = mappedShortTable.scan(r -> r.limit(1)).iterator();

        assertThatThrownBy(() -> {
            // We can't guarantee the order they will be returned
            results.next();
            results.next();
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TWO")
                .hasMessageContaining("FakeEnumShortened");
    }
}
