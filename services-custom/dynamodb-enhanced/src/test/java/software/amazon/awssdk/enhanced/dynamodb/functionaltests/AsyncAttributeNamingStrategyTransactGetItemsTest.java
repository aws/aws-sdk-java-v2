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

import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.Document;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.PlainNamingItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactGetItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks an async transaction get across a strategy table and a table without a strategy.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyTransactGetItemsTest extends LocalDynamoDbAsyncTestBase {

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
     * Creates both async tables and writes one item to each.
     */
    @Before
    public void createTables() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        strategyTableName = getConcreteTableName("naming-async-transact-get-strategy");
        plainTableName = getConcreteTableName("naming-async-transact-get-plain");
        strategyTable = enhancedClient.table(strategyTableName, NamingStrategySupport.itemSchema(strategy));
        plainTable = enhancedClient.table(plainTableName, NamingStrategySupport.plainSchema());
        strategyTable.createTable().join();
        plainTable.createTable().join();
        strategyTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1")).join();
        PlainNamingItem plain = new PlainNamingItem();
        plain.setId("plain-1");
        plain.setCustomerId("plain-customer");
        plainTable.putItem(plain).join();
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
    public void transactGetItems_whenTablesUseDifferentSchemas_readsBoth() {
        TransactGetItemsEnhancedRequest request = TransactGetItemsEnhancedRequest.builder()
            .addGetItem(strategyTable, Key.builder().partitionValue("id-1").sortValue("o-1").build())
            .addGetItem(plainTable, Key.builder().partitionValue("plain-1").build())
            .build();

        List<Document> results = enhancedClient.transactGetItems(request).join();

        assertThat(results.get(0).getItem(strategyTable).getCustomerId()).isEqualTo("c-1");
        assertThat(results.get(1).getItem(plainTable).getCustomerId()).isEqualTo("plain-customer");
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
