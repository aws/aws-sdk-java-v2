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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.extensions;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;

import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.NamingStrategySupport;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingCounterItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks that the async atomic counter writes the resolved attribute name.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyAtomicCounterTest extends LocalDynamoDbAsyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    private DynamoDbAsyncTable<NamingCounterItem> mappedTable;
    private String tableName;

    /**
     * Returns the five naming strategies.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return NamingStrategySupport.parameters();
    }

    /**
     * Creates the async counter table.
     */
    @Before
    public void createTable() {
        DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        tableName = getConcreteTableName("naming-async-counter");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.beanSchema(NamingCounterItem.class, strategy));
        mappedTable.createTable().join();
    }

    /**
     * Deletes the counter table.
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
    public void updateItem_whenAtomicCounter_writesResolvedName() {
        NamingCounterItem item = new NamingCounterItem();
        item.setId("id-1");
        mappedTable.updateItem(request -> request.item(item)).join();

        String counterName = NamingStrategySupport.stored(strategy, "hitCount");
        assertThat(NamingStrategySupport.lowLevelByKey(getDynamoDbClient(), tableName, NamingStrategySupport.stored(strategy, "id"), "id-1"))
            .containsEntry(counterName, numberValue(0));
        assertThat(strategyName).isNotBlank();
    }
}
