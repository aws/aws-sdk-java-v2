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
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.TOKEN_PREFIX;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingConverterItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStaticConverterItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks async put, get, and update for a converted attribute on a bean schema and a static schema.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyConverterCrudTest extends LocalDynamoDbAsyncTestBase {

    @Parameterized.Parameter(0)
    public String caseName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    @Parameterized.Parameter(2)
    public String schemaKind;

    private DynamoDbEnhancedAsyncClient enhancedClient;
    private String tableName;
    private String partitionName;

    /**
     * Returns each strategy for a bean schema and a static schema.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> rows = new ArrayList<>();
        for (Object[] strategy : NamingStrategySupport.parameters()) {
            rows.add(new Object[] {strategy[0] + "-bean", strategy[1], "bean"});
            rows.add(new Object[] {strategy[0] + "-static", strategy[1], "static"});
        }
        return rows;
    }

    /**
     * Creates the async table for the current schema kind.
     */
    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        tableName = getConcreteTableName("naming-async-converter-crud");
        partitionName = "bean".equals(schemaKind) ? "PK" : NamingStrategySupport.stored(strategy, "PK");
        if ("bean".equals(schemaKind)) {
            enhancedClient.table(tableName, NamingStrategySupport.converterBeanSchema(strategy)).createTable().join();
        } else {
            enhancedClient.table(tableName, NamingStrategySupport.converterStaticSchema(strategy)).createTable().join();
        }
    }

    /**
     * Deletes the table.
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
    public void putItem_getItem_updateItem_whenConverted_storesResolvedNameAndConverterValue() {
        if ("bean".equals(schemaKind)) {
            exerciseBean();
        } else {
            exerciseStatic();
        }
        assertThat(caseName).isNotBlank();
    }

    /**
     * Exercises the bean schema.
     */
    private void exerciseBean() {
        DynamoDbAsyncTable<NamingConverterItem> table =
            enhancedClient.table(tableName, NamingStrategySupport.converterBeanSchema(strategy));
        NamingConverterItem item = bean("c-1");
        table.putItem(item).join();
        assertStored("c-1");
        assertThat(table.getItem(key()).join().getCustomerCode()).isEqualTo(new NamingToken("c-1"));
        item.setCustomerCode(new NamingToken("c-2"));
        table.updateItem(request -> request.item(item)).join();
        assertStored("c-2");
    }

    /**
     * Exercises the static schema.
     */
    private void exerciseStatic() {
        DynamoDbAsyncTable<NamingStaticConverterItem> table =
            enhancedClient.table(tableName, NamingStrategySupport.converterStaticSchema(strategy));
        NamingStaticConverterItem item = staticItem("c-1");
        table.putItem(item).join();
        assertStored("c-1");
        assertThat(table.getItem(key()).join().getCustomerCode()).isEqualTo(new NamingToken("c-1"));
        item.setCustomerCode(new NamingToken("c-2"));
        table.updateItem(request -> request.item(item)).join();
        assertStored("c-2");
    }

    /**
     * Asserts the low-level value written by the converter.
     */
    private void assertStored(String token) {
        String codeName = NamingStrategySupport.stored(strategy, "customerCode");
        assertThat(lowLevel()).containsEntry(codeName, stringValue(TOKEN_PREFIX + token));
    }

    /**
     * Reads the item with the low-level client.
     */
    private Map<String, AttributeValue> lowLevel() {
        Map<String, AttributeValue> itemKey = new HashMap<>();
        itemKey.put(partitionName, AttributeValue.fromS("id-1"));
        itemKey.put(NamingStrategySupport.stored(strategy, "orderId"), AttributeValue.fromS("o-1"));
        return getDynamoDbClient().getItem(request -> request.tableName(tableName).key(itemKey)).item();
    }

    /**
     * Returns the enhanced-client key.
     */
    private Key key() {
        return Key.builder().partitionValue("id-1").sortValue("o-1").build();
    }

    /**
     * Returns a bean item.
     */
    private static NamingConverterItem bean(String token) {
        NamingConverterItem item = new NamingConverterItem();
        item.setId("id-1");
        item.setOrderId("o-1");
        item.setCustomerCode(new NamingToken(token));
        return item;
    }

    /**
     * Returns a static item.
     */
    private static NamingStaticConverterItem staticItem(String token) {
        NamingStaticConverterItem item = new NamingStaticConverterItem();
        item.setId("id-1");
        item.setOrderId("o-1");
        item.setCustomerCode(new NamingToken(token));
        return item;
    }
}
