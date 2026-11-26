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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks async put, get, update, and delete against stored names.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyCrudTest extends LocalDynamoDbAsyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    private DynamoDbAsyncTable<NamingStrategyItem> mappedTable;
    private String tableName;

    /**
     * Returns the five naming strategies.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return NamingStrategySupport.parameters();
    }

    /**
     * Creates the async table.
     */
    @Before
    public void createTable() {
        DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        tableName = getConcreteTableName("naming-async-crud");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable().join();
    }

    /**
     * Deletes the async table.
     */
    @After
    public void deleteTable() {
        deleteQuietly(tableName);
    }

    @Test
    public void putItem_getItem_updateItem_deleteItem_whenStrategy_roundTripsStoredNames() {
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1")).join();
        String customerName = NamingStrategySupport.stored(strategy, "customerId");
        Map<String, AttributeValue> stored = lowLevel();

        assertThat(stored).containsEntry("PK", stringValue("id-1"));
        assertThat(stored).containsEntry(customerName, stringValue("c-1"));
        if (!customerName.equals("customerId")) {
            assertThat(stored).doesNotContainKey("customerId");
        }
        NamingStrategyItem loaded = mappedTable.getItem(key()).join();
        assertThat(loaded.getCustomerId()).isEqualTo("c-1");
        loaded.setCustomerId("c-2");
        mappedTable.updateItem(loaded).join();
        assertThat(lowLevel()).containsEntry(customerName, stringValue("c-2"));
        mappedTable.deleteItem(key()).join();
        assertThat(lowLevel()).isEmpty();
        assertThat(strategyName).isNotBlank();
    }

    @Test
    public void putItem_andGetItem_whenSchemaAlreadyBuilt_doNotCallTransform() {
        NamingStrategySupport.CountingStrategy counting = new NamingStrategySupport.CountingStrategy(strategy);
        String secondTable = getConcreteTableName("naming-async-crud-count");
        DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        DynamoDbAsyncTable<NamingStrategyItem> counted =
            enhancedClient.table(secondTable, NamingStrategySupport.itemSchema(counting));
        counted.createTable().join();
        int callsAfterConstruction = counting.calls();

        counted.putItem(NamingStrategySupport.item("id-2", "o-2", "c-2")).join();
        counted.getItem(Key.builder().partitionValue("id-2").sortValue("o-2").build()).join();

        assertThat(counting.calls()).isEqualTo(callsAfterConstruction);
        deleteQuietly(secondTable);
    }

    /**
     * Returns the key of the written item.
     */
    private static Key key() {
        return Key.builder().partitionValue("id-1").sortValue("o-1").build();
    }

    /**
     * Reads the written item with the low-level client.
     */
    private Map<String, AttributeValue> lowLevel() {
        return NamingStrategySupport.lowLevelItem(getDynamoDbClient(), tableName, "id-1", "o-1", strategy);
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
