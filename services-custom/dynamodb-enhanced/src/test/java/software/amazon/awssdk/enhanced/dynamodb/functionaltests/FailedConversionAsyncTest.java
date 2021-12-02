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
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnum;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnumRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnumShortened;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeEnumShortenedRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FailedConversionAsyncTest extends LocalDynamoDbAsyncTestBase {
    private static final TableSchema<FakeEnumRecord> TABLE_SCHEMA = TableSchema.fromClass(FakeEnumRecord.class);
    private static final TableSchema<FakeEnumShortenedRecord> SHORT_TABLE_SCHEMA =
            TableSchema.fromClass(FakeEnumShortenedRecord.class);

    private final DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder()
                    .dynamoDbClient(getDynamoDbAsyncClient())
                    .build();

    private final DynamoDbAsyncTable<FakeEnumRecord> mappedTable =
            enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);
    private final DynamoDbAsyncTable<FakeEnumShortenedRecord> mappedShortTable =
            enhancedClient.table(getConcreteTableName("table-name"), SHORT_TABLE_SCHEMA);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput())).join();
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build()).join();
    }

    @Test
    public void exceptionOnRead() {
        FakeEnumRecord record = new FakeEnumRecord();
        record.setId("123");
        record.setEnumAttribute(FakeEnum.TWO);
        mappedTable.putItem(record).join();

        assertThatThrownBy(() -> mappedShortTable.getItem(Key.builder().partitionValue("123").build()).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TWO")
                .hasMessageContaining("FakeEnumShortened");
    }

    @Test
    public void iterableExceptionOnRead() {
        FakeEnumRecord record = new FakeEnumRecord();
        record.setId("1");
        record.setEnumAttribute(FakeEnum.ONE);
        mappedTable.putItem(record).join();
        record.setId("2");
        record.setEnumAttribute(FakeEnum.TWO);
        mappedTable.putItem(record).join();

        List<Page<FakeEnumShortenedRecord>> results =
                drainPublisherToError(mappedShortTable.scan(r -> r.limit(1)), 1, IllegalArgumentException.class);

        assertThat(results).hasOnlyOneElementSatisfying(
                page -> assertThat(page.items()).hasOnlyOneElementSatisfying(item -> {
                    assertThat(item.getId()).isEqualTo("1");
                    assertThat(item.getEnumAttribute()).isEqualTo(FakeEnumShortened.ONE);
                }));
    }
}
