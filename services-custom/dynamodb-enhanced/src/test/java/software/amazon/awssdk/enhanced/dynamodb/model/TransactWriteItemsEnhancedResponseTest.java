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

import com.google.common.collect.ImmutableList;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
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
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ItemCollectionMetrics;
import software.amazon.awssdk.utils.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class TransactWriteItemsEnhancedResponseTest {

    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;


    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).extensions().build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
    }


    @Test
    public void builder_minimal() {
        TransactWriteItemsEnhancedResponse<Void> builtObject = TransactWriteItemsEnhancedResponse.<Void>builder(null).build();

        assertThat(builtObject.items(), is(nullValue()));
        assertThat(builtObject.consumedCapacity(), is(nullValue()));
        assertThat(builtObject.itemCollectionMetrics(), is(nullValue()));
    }

    @Test
    public void builder_maximal_consumer_style() {
        ImmutableMap<String, List<ItemCollectionMetrics>> metricCollection = ImmutableMap.of("a",
                                                                                             ImmutableList.of(ItemCollectionMetrics.builder().build())
        );
        List<ConsumedCapacity> consumedCapacity =
            ImmutableList.of(software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity.builder().capacityUnits(2.0).build());
        TransactWriteItemsEnhancedResponse<Void> builtObject = TransactWriteItemsEnhancedResponse.<Void>builder(null)
                                                                                                 .itemCollectionMetrics(metricCollection)
                                                                                                 .consumedCapacity(consumedCapacity)
                                                                                                 .build();

        assertThat(builtObject.items(), is(nullValue()));
        assertThat(builtObject.consumedCapacity(), is(consumedCapacity));
        assertThat(builtObject.itemCollectionMetrics(), is(metricCollection));
    }

    @Test
    public void test_write_batch_equalsAndHashcode() {

        EqualsVerifier.forClass(TransactWriteItemsEnhancedResponse.class)
                      .withNonnullFields("consumedCapacity", "itemCollectionMetrics")
                      .withPrefabValues(ItemCollectionMetrics.class,
                                        ItemCollectionMetrics.builder().itemCollectionKey(com.google.common.collect.ImmutableMap.of("a",
                                                                                                                                    AttributeValue.builder().build())).sizeEstimateRangeGB(20.0).build(),
                                        ItemCollectionMetrics.builder().itemCollectionKey(com.google.common.collect.ImmutableMap.of("b",
                                                                                                                                    AttributeValue.builder().build())).sizeEstimateRangeGB(30.0).build())
                      .withPrefabValues(ConsumedCapacity.class, ConsumedCapacity.builder().capacityUnits(5.0).build(),
                                        ConsumedCapacity.builder().capacityUnits(15.0).build())
                      .verify();
    }

}
