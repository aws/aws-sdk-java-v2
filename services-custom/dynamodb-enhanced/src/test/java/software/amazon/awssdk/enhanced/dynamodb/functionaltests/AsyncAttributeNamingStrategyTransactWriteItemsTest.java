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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collection;
import java.util.concurrent.CompletionException;
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
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Checks that an async failed condition on a stored name cancels both writes.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyTransactWriteItemsTest extends LocalDynamoDbAsyncTestBase {

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
     * Creates both async tables and writes the strategy item.
     */
    @Before
    public void createTables() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        strategyTableName = getConcreteTableName("naming-async-transact-write-strategy");
        plainTableName = getConcreteTableName("naming-async-transact-write-plain");
        strategyTable = enhancedClient.table(strategyTableName, NamingStrategySupport.itemSchema(strategy));
        plainTable = enhancedClient.table(plainTableName, NamingStrategySupport.plainSchema());
        strategyTable.createTable().join();
        plainTable.createTable().join();
        strategyTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1")).join();
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
    public void transactWriteItems_whenStoredNameConditionFails_cancelsBothWrites() {
        PlainNamingItem plain = new PlainNamingItem();
        plain.setId("plain-1");
        plain.setCustomerId("plain-customer");
        String storedCustomer = NamingStrategySupport.stored(strategy, "customerId");
        TransactPutItemEnhancedRequest<NamingStrategyItem> failed = TransactPutItemEnhancedRequest.builder(NamingStrategyItem.class)
            .item(NamingStrategySupport.item("id-1", "o-1", "c-9"))
            .conditionExpression(NamingStrategySupport.equalTo(storedCustomer, "missing"))
            .build();

        assertThatThrownBy(() -> enhancedClient.transactWriteItems(request -> request
            .addPutItem(strategyTable, failed)
            .addPutItem(plainTable, plain)).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(TransactionCanceledException.class);

        assertThat(NamingStrategySupport.lowLevelItem(getDynamoDbClient(), strategyTableName, "id-1", "o-1", strategy))
            .containsEntry(storedCustomer, stringValue("c-1"));
        assertThat(NamingStrategySupport.lowLevelByKey(getDynamoDbClient(), plainTableName, "id", "plain-1")).isEmpty();
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
