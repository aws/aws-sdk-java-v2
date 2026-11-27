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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.PlainNamingItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks a batch get that reads one strategy table and one table without a strategy.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyBatchGetItemTest extends LocalDynamoDbSyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<NamingStrategyItem> strategyTable;
    private DynamoDbTable<PlainNamingItem> plainTable;
    private String strategyTableName;
    private String plainTableName;

    /**
     * Returns the five naming strategies.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return NamingStrategySupport.parameters();
    }

    /**
     * Creates both tables and writes one item to each.
     */
    @Before
    public void createTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        strategyTableName = getConcreteTableName("naming-batch-get-strategy");
        plainTableName = getConcreteTableName("naming-batch-get-plain");
        strategyTable = enhancedClient.table(strategyTableName, NamingStrategySupport.itemSchema(strategy));
        plainTable = enhancedClient.table(plainTableName, NamingStrategySupport.plainSchema());
        strategyTable.createTable();
        plainTable.createTable();
        strategyTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1"));
        PlainNamingItem plain = new PlainNamingItem();
        plain.setId("plain-1");
        plain.setCustomerId("plain-customer");
        plainTable.putItem(plain);
    }

    /**
     * Deletes both tables.
     */
    @After
    public void deleteTables() {
        deleteQuietly(strategyTableName);
        deleteQuietly(plainTableName);
    }

    @Test
    public void batchGetItem_whenTablesUseDifferentSchemas_readsBothStoredNames() {
        BatchGetResultPageIterable pages = enhancedClient.batchGetItem(request -> request.readBatches(
            ReadBatch.builder(NamingStrategyItem.class)
                     .mappedTableResource(strategyTable)
                     .addGetItem(Key.builder().partitionValue("id-1").sortValue("o-1").build())
                     .build(),
            ReadBatch.builder(PlainNamingItem.class)
                     .mappedTableResource(plainTable)
                     .addGetItem(Key.builder().partitionValue("plain-1").build())
                     .build()));

        List<NamingStrategyItem> strategyItems = new ArrayList<>();
        List<PlainNamingItem> plainItems = new ArrayList<>();
        pages.resultsForTable(strategyTable).forEach(strategyItems::add);
        pages.resultsForTable(plainTable).forEach(plainItems::add);

        assertThat(strategyItems).extracting(NamingStrategyItem::getCustomerId).containsExactly("c-1");
        assertThat(plainItems).extracting(PlainNamingItem::getCustomerId).containsExactly("plain-customer");
        assertThat(NamingStrategySupport.lowLevelItem(getDynamoDbClient(), strategyTableName, "id-1", "o-1", strategy))
            .containsEntry(NamingStrategySupport.stored(strategy, "customerId"), stringValue("c-1"));
        assertThat(NamingStrategySupport.lowLevelByKey(getDynamoDbClient(), plainTableName, "id", "plain-1"))
            .containsEntry("customerId", stringValue("plain-customer"));
        assertThat(strategyName).isNotBlank();
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
