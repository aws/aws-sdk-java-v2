/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@RunWith(MockitoJUnitRunner.class)
public class DynamoDbAsyncMappedTableTest {
    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;

    @Mock
    private MapperExtension mockMapperExtension;

    @Mock
    private TableOperation<FakeItem, Object, Object, FakeItem> mockTableOperation;

    @Test
    public void execute_callsOperationCorrectly() {
        FakeItem expectedOutput = FakeItem.createUniqueFakeItem();
        when(mockTableOperation.executeOnPrimaryIndexAsync(any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(expectedOutput));
        DynamoDbAsyncMappedTable<FakeItem> dynamoDbAsyncMappedTable =
            new DynamoDbAsyncMappedTable<>(mockDynamoDbAsyncClient,
                                           mockMapperExtension,
                                           FakeItem.getTableSchema(),
                                           TABLE_NAME);

        FakeItem actualOutput = dynamoDbAsyncMappedTable.execute(mockTableOperation).join();

        assertThat(actualOutput, is(expectedOutput));
        verify(mockTableOperation).executeOnPrimaryIndexAsync(FakeItem.getTableSchema(),
                                                              dynamoDbAsyncMappedTable.tableName(),
                                                              mockMapperExtension,
                                                              mockDynamoDbAsyncClient);
    }

    @Test
    public void index_constructsCorrectMappedIndex() {
        DynamoDbAsyncMappedTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DynamoDbAsyncMappedTable<>(mockDynamoDbAsyncClient,
                                           mockMapperExtension,
                                           FakeItemWithIndices.getTableSchema(),
                                           TABLE_NAME);

        DynamoDbAsyncMappedIndex<FakeItemWithIndices> dynamoDbMappedIndex = dynamoDbMappedTable.index("gsi_1");

        assertThat(dynamoDbMappedIndex.dynamoDbClient(), is(sameInstance(mockDynamoDbAsyncClient)));
        assertThat(dynamoDbMappedIndex.mapperExtension(), is(sameInstance(mockMapperExtension)));
        assertThat(dynamoDbMappedIndex.tableSchema(), is(sameInstance(FakeItemWithIndices.getTableSchema())));
        assertThat(dynamoDbMappedIndex.indexName(), is("gsi_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void index_invalidIndex_throwsIllegalArgumentException() {
        DynamoDbAsyncMappedTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DynamoDbAsyncMappedTable<>(mockDynamoDbAsyncClient,
                                           mockMapperExtension,
                                           FakeItemWithIndices.getTableSchema(),
                                           TABLE_NAME);

        dynamoDbMappedTable.index("invalid");
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithSort();
        DynamoDbAsyncMappedTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DynamoDbAsyncMappedTable<>(mockDynamoDbAsyncClient,
                                           mockMapperExtension,
                                           FakeItemWithSort.getTableSchema(),
                                           "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.of(stringValue(item.getSort()))));
    }

    @Test
    public void keyFrom_primaryIndex_partitionOnly() {
        FakeItem item = FakeItem.createUniqueFakeItem();
        DynamoDbAsyncMappedTable<FakeItem> dynamoDbMappedIndex =
            new DynamoDbAsyncMappedTable<>(mockDynamoDbAsyncClient,
                                           mockMapperExtension,
                                           FakeItem.getTableSchema(),
                                           "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndNullSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithoutSort();
        DynamoDbAsyncMappedTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DynamoDbAsyncMappedTable<>(mockDynamoDbAsyncClient,
                                           mockMapperExtension,
                                           FakeItemWithSort.getTableSchema(),
                                           "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }
}
