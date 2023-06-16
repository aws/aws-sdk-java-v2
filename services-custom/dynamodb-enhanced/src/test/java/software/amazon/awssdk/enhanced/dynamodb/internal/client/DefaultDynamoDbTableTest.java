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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithSort;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedLocalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDynamoDbTableTest {
    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockDynamoDbEnhancedClientExtension;

    @Test
    public void index_constructsCorrectMappedIndex() {
        DefaultDynamoDbTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                       mockDynamoDbEnhancedClientExtension,
                                       FakeItemWithIndices.getTableSchema(),
                                       TABLE_NAME);

        DefaultDynamoDbIndex<FakeItemWithIndices> dynamoDbMappedIndex = dynamoDbMappedTable.index("gsi_1");

        assertThat(dynamoDbMappedIndex.dynamoDbClient(), is(sameInstance(mockDynamoDbClient)));
        assertThat(dynamoDbMappedIndex.mapperExtension(), is(sameInstance(mockDynamoDbEnhancedClientExtension)));
        assertThat(dynamoDbMappedIndex.tableSchema(), is(sameInstance(FakeItemWithIndices.getTableSchema())));
        assertThat(dynamoDbMappedIndex.indexName(), is("gsi_1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void index_invalidIndex_throwsIllegalArgumentException() {
        DefaultDynamoDbTable<FakeItemWithIndices> dynamoDbMappedTable =
            new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                       mockDynamoDbEnhancedClientExtension,
                                       FakeItemWithIndices.getTableSchema(),
                                       TABLE_NAME);

        dynamoDbMappedTable.index("invalid");
    }

    @Test
    public void keyFrom_primaryIndex_partitionAndSort() {
        FakeItemWithSort item = FakeItemWithSort.createUniqueFakeItemWithSort();
        DefaultDynamoDbTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DefaultDynamoDbTable<>(mockDynamoDbClient,
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
        DefaultDynamoDbTable<FakeItem> dynamoDbMappedIndex =
            new DefaultDynamoDbTable<>(mockDynamoDbClient,
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
        DefaultDynamoDbTable<FakeItemWithSort> dynamoDbMappedIndex =
            new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                       mockDynamoDbEnhancedClientExtension,
                                       FakeItemWithSort.getTableSchema(),
                                       "test_table");

        Key key = dynamoDbMappedIndex.keyFrom(item);

        assertThat(key.partitionKeyValue(), is(stringValue(item.getId())));
        assertThat(key.sortKeyValue(), is(Optional.empty()));
    }

    @Test
    public void createTable_doesNotTreatPrimaryIndexAsAnyOfSecondaryIndexes() {
        DefaultDynamoDbTable<FakeItem> dynamoDbMappedIndex =
            Mockito.spy(new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                                   mockDynamoDbEnhancedClientExtension,
                                                   FakeItem.getTableSchema(),
                                                   "test_table"));

        dynamoDbMappedIndex.createTable();

        CreateTableEnhancedRequest request = captureCreateTableRequest(dynamoDbMappedIndex);

        assertThat(request.localSecondaryIndices().size(), is(0));
        assertThat(request.globalSecondaryIndices().size(), is(0));
    }

    @Test
    public void createTable_groupsSecondaryIndexesExistingInTableSchema() {
        DefaultDynamoDbTable<FakeItemWithIndices> dynamoDbMappedIndex =
            Mockito.spy(new DefaultDynamoDbTable<>(mockDynamoDbClient,
                                                   mockDynamoDbEnhancedClientExtension,
                                                   FakeItemWithIndices.getTableSchema(),
                                                   "test_table"));

        dynamoDbMappedIndex.createTable();

        CreateTableEnhancedRequest request = captureCreateTableRequest(dynamoDbMappedIndex);

        assertThat(request.localSecondaryIndices().size(), is(1));
        Iterator<EnhancedLocalSecondaryIndex> lsiIterator = request.localSecondaryIndices().iterator();
        assertThat(lsiIterator.next().indexName(), is("lsi_1"));

        assertThat(request.globalSecondaryIndices().size(), is(2));
        List<String> globalIndicesNames = request.globalSecondaryIndices().stream()
                                                 .map(EnhancedGlobalSecondaryIndex::indexName)
                                                 .collect(Collectors.toList());
        assertThat(globalIndicesNames, containsInAnyOrder("gsi_1", "gsi_2"));
    }

    private static <T> CreateTableEnhancedRequest captureCreateTableRequest(DefaultDynamoDbTable<T> index) {
        ArgumentCaptor<CreateTableEnhancedRequest> createTableOperationCaptor =
            ArgumentCaptor.forClass(CreateTableEnhancedRequest.class);
        verify(index).createTable(createTableOperationCaptor.capture());
        return createTableOperationCaptor.getValue();
    }
}
