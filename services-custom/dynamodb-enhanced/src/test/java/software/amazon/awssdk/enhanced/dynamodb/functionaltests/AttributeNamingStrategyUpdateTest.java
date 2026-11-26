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
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyAddress;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchemaParams;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks update behavior, nested ignore-nulls, and a stored-name condition.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyUpdateTest extends LocalDynamoDbSyncTestBase {

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
     * Creates the mapped table and writes one item.
     */
    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        tableName = getConcreteTableName("naming-update");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable();
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1"));
    }

    /**
     * Deletes the mapped table.
     */
    @After
    public void deleteTable() {
        deleteQuietly(tableName);
    }

    @Test
    public void updateItem_whenWriteIfNotExists_keepsOriginalStoredAttribute() {
        NamingStrategyItem update = NamingStrategySupport.item("id-1", "o-1", "c-1");
        update.setStatusCode("closed");
        mappedTable.updateItem(request -> request.item(update));

        String statusName = NamingStrategySupport.stored(strategy, "statusCode");
        assertThat(lowLevel()).containsEntry(statusName, stringValue("open"));
        assertThat(strategyName).isNotBlank();
    }

    @Test
    public void updateItem_whenScalarOnly_keepsSiblingUnderResolvedNames() {
        NamingStrategyAddress address = new NamingStrategyAddress();
        address.setCityName("Milan");
        NamingStrategyItem update = new NamingStrategyItem();
        update.setId("id-1");
        update.setOrderId("o-1");
        update.setHomeCity(address);

        mappedTable.updateItem(request -> request.item(update).ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY));

        NamingStrategyItem loaded = mappedTable.getItem(key());
        assertThat(loaded.getHomeCity().getCityName()).isEqualTo("Milan");
        assertThat(loaded.getHomeCity().getStreetName()).isEqualTo("Via");
        String homeCity = NamingStrategySupport.stored(strategy, "homeCity");
        String cityName = NamingStrategySupport.stored(strategy, "cityName");
        String streetName = NamingStrategySupport.stored(strategy, "streetName");
        assertThat(lowLevel().get(homeCity).m()).containsEntry(cityName, stringValue("Milan"))
                                                .containsEntry(streetName, stringValue("Via"));
    }

    @Test
    public void updateItem_whenConditionUsesStoredNameAndFails_throwsConditionalCheckFailedException() {
        String storedCustomer = NamingStrategySupport.stored(strategy, "customerId");
        assertThatThrownBy(() -> mappedTable.updateItem(request -> request.item(NamingStrategySupport.item("id-1", "o-1", "c-9"))
                                                                          .conditionExpression(NamingStrategySupport.equalTo(
                                                                              storedCustomer, "missing"))))
            .isInstanceOf(ConditionalCheckFailedException.class);
        assertThat(lowLevel()).containsEntry(storedCustomer, stringValue("c-1"));
    }

    @Test
    public void updateItem_whenCustomStrategyReturnsSpecialCharacter_expressionUsesResolvedName() {
        if (!"LOWER_SNAKE_CASE".equals(strategyName)) {
            return;
        }
        AttributeNamingStrategy special = source -> source + "*";
        String specialTable = getConcreteTableName("naming-update-special");
        DynamoDbTable<NamingStrategyItem> table = enhancedClient.table(
            specialTable,
            software.amazon.awssdk.enhanced.dynamodb.TableSchema.fromBean(
                BeanTableSchemaParams.builder(NamingStrategyItem.class).attributeNamingStrategy(special).build()));
        table.createTable();
        NamingStrategyItem item = NamingStrategySupport.item("id-9", "o-9", "c-9");
        table.putItem(item);
        item.setCustomerId("c-8");
        table.updateItem(request -> request.item(item));

        Map<String, AttributeValue> stored = NamingStrategySupport.lowLevelItem(
            getDynamoDbClient(), specialTable, "id-9", "o-9", special);
        assertThat(stored).containsEntry("customerId*", stringValue("c-8"));
        deleteQuietly(specialTable);
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
