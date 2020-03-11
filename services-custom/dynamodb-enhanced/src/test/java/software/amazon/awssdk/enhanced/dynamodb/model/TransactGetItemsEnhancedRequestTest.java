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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;

import java.util.Arrays;
import java.util.List;
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
import software.amazon.awssdk.services.dynamodb.model.Get;
import software.amazon.awssdk.services.dynamodb.model.TransactGetItem;

@RunWith(MockitoJUnitRunner.class)
public class TransactGetItemsEnhancedRequestTest {

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
        TransactGetItemsEnhancedRequest builtObject = TransactGetItemsEnhancedRequest.builder().build();

        assertThat(builtObject.transactGetItems(), is(nullValue()));
    }

    @Test
    public void builder_maximal_consumer_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        TransactGetItemsEnhancedRequest builtObject =
            TransactGetItemsEnhancedRequest.builder()
                                           .addGetItem(fakeItemMappedTable, r -> r.key(k -> k.partitionValue(fakeItem.getId())))
                                           .addGetItem(fakeItemMappedTable, r -> r.key(k -> k.partitionValue(fakeItem.getId())))
                                           .build();

        assertThat(builtObject.transactGetItems(), is(getTransactGetItems(fakeItem)));
    }

    @Test
    public void builder_maximal_builder_style() {
        FakeItem fakeItem = createUniqueFakeItem();

        GetItemEnhancedRequest getItem = GetItemEnhancedRequest.builder()
                                                               .key(k -> k.partitionValue(fakeItem.getId()))
                                                               .build();

        TransactGetItemsEnhancedRequest builtObject =
            TransactGetItemsEnhancedRequest.builder()
                                           .addGetItem(fakeItemMappedTable, getItem)
                                           .addGetItem(fakeItemMappedTable, getItem)
                                           .build();

        assertThat(builtObject.transactGetItems(), is(getTransactGetItems(fakeItem)));
    }

    
    private List<TransactGetItem> getTransactGetItems(FakeItem fakeItem) {
        final Map<String, AttributeValue> fakeItemMap = FakeItem.getTableSchema().itemToMap(fakeItem, true);

        TransactGetItem getItem = TransactGetItem.builder()
                                                      .get(Get.builder()
                                                              .key(fakeItemMap)
                                                              .tableName(TABLE_NAME)
                                                              .build())
                                                      .build();

        return Arrays.asList(getItem, getItem);
    }

}
