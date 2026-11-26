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
 * Checks that a flattened child is stored on the parent item under resolved names.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyFlattenTest extends LocalDynamoDbSyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

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
     * Creates the mapped table.
     */
    @Before
    public void createTable() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        tableName = getConcreteTableName("naming-flatten");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable();
    }

    /**
     * Deletes the mapped table.
     */
    @After
    public void deleteTable() {
        try {
            getDynamoDbClient().deleteTable(request -> request.tableName(tableName));
        } catch (ResourceNotFoundException ignored) {
            // The table was not created.
        }
    }

    @Test
    public void putItem_whenChildIsFlattened_storesChildAttributesOnTheItem() {
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1"));
        Map<String, AttributeValue> stored = NamingStrategySupport.lowLevelItem(
            getDynamoDbClient(), tableName, "id-1", "o-1", strategy);
        String displayName = NamingStrategySupport.stored(strategy, "displayName");

        assertThat(stored).containsEntry(displayName, stringValue("Ada"));
        assertThat(stored).doesNotContainKey(NamingStrategySupport.stored(strategy, "flatProfile"));
        NamingStrategyItem loaded = mappedTable.getItem(Key.builder().partitionValue("id-1").sortValue("o-1").build());
        assertThat(loaded.getFlatProfile().getDisplayName()).isEqualTo("Ada");
        assertThat(strategyName).isNotBlank();
    }
}
