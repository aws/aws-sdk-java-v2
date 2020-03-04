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

package software.amazon.awssdk.enhanced.dynamodb.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbAsyncTableTest {
    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void index_constructsCorrectMappedIndex() {
        DefaultDynamoDbAsyncTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithIndices.getTableSchema(),
                                            TABLE_NAME);

        DefaultDynamoDbAsyncIndex<FakeItemWithIndices> dynamoDbMappedIndex = dynamoDbMappedTable.index("gsi_1");

        assertThat(dynamoDbMappedIndex.dynamoDbClient(), is(sameInstance(mockDynamoDbAsyncClient)));
        assertThat(dynamoDbMappedIndex.mapperExtension(), is(sameInstance(mockDynamoDbEnhancedClientExtension)));
        assertThat(dynamoDbMappedIndex.tableSchema(), is(sameInstance(FakeItemWithIndices.getTableSchema())));
        assertThat(dynamoDbMappedIndex.indexName(), is("gsi_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void index_invalidIndex_throwsIllegalArgumentException() {
        DefaultDynamoDbAsyncTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithIndices.getTableSchema(),
                                            TABLE_NAME);

        dynamoDbMappedTable.index("invalid");
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithSort();
        DefaultDynamoDbAsyncTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithSort.getTableSchema(),
                                            "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.of(stringValue(item.getSort()))));
    }

    @Test
    public void keyFrom_primaryIndex_partitionOnly() {
        FakeItem item = FakeItem.createUniqueFakeItem();
        DefaultDynamoDbAsyncTable<FakeItem> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItem.getTableSchema(),
                                            "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndNullSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithoutSort();
        DefaultDynamoDbAsyncTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DefaultDynamoDbAsyncTable<>(mockDynamoDbAsyncClient,
                                            mockDynamoDbEnhancedClientExtension,
                                            FakeItemWithSort.getTableSchema(),
                                            "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }
}
