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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;

import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@RunWith(MockitoJUnitRunner.class)
public class ReadBatchTest {

    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;

    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
    }

    @Test
    public void builder_minimal() {
        ReadBatch builtObject = ReadBatch.builder(FakeItem.class).build();

        assertThat(builtObject.tableName(), is(nullValue()));
        assertThat(builtObject.keysAndAttributes(), is(nullValue()));
    }

    @Test
    public void builder_maximal_consumer_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        ReadBatch builtObject = ReadBatch.builder(FakeItem.class)
                                         .mappedTableResource(fakeItemMappedTable)
                                         .addGetItem(r -> r.key(k -> k.partitionValue(fakeItem.getId())))
                                         .build();

        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem,
                                                                                      FakeItem.getTableMetadata().primaryKeys());

        assertThat(builtObject.tableName(), is(TABLE_NAME));
        assertThat(builtObject.keysAndAttributes().keys(), containsInAnyOrder(Collections.singletonList(fakeItemMap).toArray()));
    }

    @Test
    public void builder_maximal_builder_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        GetItemEnhancedRequest getItem = GetItemEnhancedRequest.builder()
                                                               .key(k -> k.partitionValue(fakeItem.getId()))
                                                               .build();

        ReadBatch builtObject = ReadBatch.builder(FakeItem.class)
                                         .mappedTableResource(fakeItemMappedTable)
                                         .addGetItem(getItem)
                                         .build();

        Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem,
                                                                                      FakeItem.getTableMetadata().primaryKeys());

        assertThat(builtObject.tableName(), is(TABLE_NAME));
        assertThat(builtObject.keysAndAttributes().keys(), containsInAnyOrder(Collections.singletonList(fakeItemMap).toArray()));
    }

}
