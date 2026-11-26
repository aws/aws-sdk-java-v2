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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.PlainNamingItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks an async batch write across a strategy table and a table without a strategy.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyBatchWriteItemTest extends LocalDynamoDbAsyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    private DynamoDbEnhancedAsyncClient enhancedClient;
    private DynamoDbAsyncTable<NamingStrategyItem> strategyTable;
    private DynamoDbAsyncTable<PlainNamingItem> plainTable;
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
     * Creates both async tables.
     */
    @Before
    public void createTables() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        strategyTableName = getConcreteTableName("naming-async-batch-write-strategy");
        plainTableName = getConcreteTableName("naming-async-batch-write-plain");
        strategyTable = enhancedClient.table(strategyTableName, NamingStrategySupport.itemSchema(strategy));
        plainTable = enhancedClient.table(plainTableName, NamingStrategySupport.plainSchema());
        strategyTable.createTable().join();
        plainTable.createTable().join();
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
    public void batchWriteItem_whenTablesUseDifferentSchemas_storesEachName() {
        PlainNamingItem plain = new PlainNamingItem();
        plain.setId("plain-1");
        plain.setCustomerId("plain-customer");
        BatchWriteResult result = enhancedClient.batchWriteItem(request -> request.writeBatches(
            WriteBatch.builder(NamingStrategyItem.class).mappedTableResource(strategyTable)
                      .addPutItem(NamingStrategySupport.item("id-1", "o-1", "c-1")).build(),
            WriteBatch.builder(PlainNamingItem.class).mappedTableResource(plainTable).addPutItem(plain).build())).join();

        assertThat(result.unprocessedPutItemsForTable(strategyTable)).isEmpty();
        assertThat(result.unprocessedPutItemsForTable(plainTable)).isEmpty();
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
