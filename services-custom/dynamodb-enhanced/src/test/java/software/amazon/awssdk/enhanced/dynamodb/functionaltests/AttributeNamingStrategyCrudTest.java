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
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collection;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks put, get, update, and delete against the stored attribute names.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyCrudTest extends LocalDynamoDbSyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<NamingStrategyItem> mappedTable;
    private String tableName;

    /**
     * Returns the five naming strategies.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return NamingStrategySupport.parameters();
    }

    /**
     * Creates the mapped table from the current strategy.
     */
    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        tableName = getConcreteTableName("naming-crud");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable();
    }

    /**
     * Deletes the mapped table.
     */
    @After
    public void deleteTable() {
        deleteQuietly(tableName);
    }

    @Test
    public void putItem_getItem_updateItem_deleteItem_whenStrategy_roundTripsStoredNames() {
        NamingStrategyItem item = NamingStrategySupport.item("id-1", "o-1", "c-1");
        mappedTable.putItemWithResponse(request -> request.item(item));

        Map<String, AttributeValue> stored = lowLevel("id-1", "o-1");
        String customerName = NamingStrategySupport.stored(strategy, "customerId");
        assertThat(stored).containsEntry("PK", stringValue("id-1"));
        assertThat(stored).containsEntry(customerName, stringValue("c-1"));
        assertThat(stored).containsEntry(NamingStrategySupport.stored(strategy, "displayName"), stringValue("Ada"));
        if (!customerName.equals("customerId")) {
            assertThat(stored).doesNotContainKey("customerId");
        }

        NamingStrategyItem loaded = mappedTable.getItemWithResponse(request -> request.key(key("id-1", "o-1"))).attributes();
        assertThat(loaded.getCustomerId()).isEqualTo("c-1");
        assertThat(loaded.getHomeCity().getCityName()).isEqualTo("Rome");
        assertThat(loaded.getFlatProfile().getDisplayName()).isEqualTo("Ada");

        item.setCustomerId("c-2");
        mappedTable.updateItem(request -> request.item(item));
        assertThat(lowLevel("id-1", "o-1")).containsEntry(customerName, stringValue("c-2"));

        mappedTable.deleteItem(key("id-1", "o-1"));
        assertThat(lowLevel("id-1", "o-1")).isEmpty();
        assertThat(mappedTable.getItem(key("id-1", "o-1"))).isNull();
    }

    @Test
    public void putItem_andGetItem_whenSchemaAlreadyBuilt_doNotCallTransform() {
        NamingStrategySupport.CountingStrategy counting = new NamingStrategySupport.CountingStrategy(strategy);
        String countingTable = getConcreteTableName("naming-crud-count");
        DynamoDbTable<NamingStrategyItem> counted = enhancedClient.table(countingTable, NamingStrategySupport.itemSchema(counting));
        counted.createTable();
        int callsAfterConstruction = counting.calls();

        counted.putItem(NamingStrategySupport.item("id-2", "o-2", "c-2"));
        counted.getItem(key("id-2", "o-2"));

        assertThat(counting.calls()).isEqualTo(callsAfterConstruction);
        assertThat(strategyName).isNotBlank();
        deleteQuietly(countingTable);
    }

    /**
     * Returns the primary key for an item.
     */
    private static Key key(String id, String orderId) {
        return Key.builder().partitionValue(id).sortValue(orderId).build();
    }

    /**
     * Reads the low-level item for the current strategy.
     */
    private Map<String, AttributeValue> lowLevel(String id, String orderId) {
        return NamingStrategySupport.lowLevelItem(getDynamoDbClient(), tableName, id, orderId, strategy);
    }

    /**
     * Deletes a table when it exists.
     */
    private void deleteQuietly(String name) {
        try {
            getDynamoDbClient().deleteTable(request -> request.tableName(name));
        } catch (ResourceNotFoundException ignored) {
            // The table was not created.
        }
    }
}
