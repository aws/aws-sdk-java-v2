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
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.GSI_EMAIL;
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.LSI_CREATED;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.PlainNamingItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

/**
 * Checks async index names, resolved index keys, and a static partition key.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyIndexQueryTest extends LocalDynamoDbAsyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    private DynamoDbEnhancedAsyncClient enhancedClient;
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
     * Creates the async table and writes one item.
     */
    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        tableName = getConcreteTableName("naming-async-index");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable().join();
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1")).join();
    }

    /**
     * Deletes the async table.
     */
    @After
    public void deleteTable() {
        deleteQuietly(tableName);
    }

    @Test
    public void describeTable_whenStrategy_keepsIndexNamesAndResolvesKeys() {
        TableDescription description = mappedTable.describeTable().join().table();
        List<String> primaryNames = description.keySchema().stream().map(KeySchemaElement::attributeName)
                                               .collect(Collectors.toList());

        assertThat(primaryNames).contains("PK", NamingStrategySupport.stored(strategy, "orderId"));
        assertThat(description.globalSecondaryIndexes()).extracting(index -> index.indexName()).contains(GSI_EMAIL);
        assertThat(description.localSecondaryIndexes()).extracting(index -> index.indexName()).contains(LSI_CREATED);
        assertThat(strategyName).isNotBlank();
    }

    @Test
    public void query_whenIndexAndSortKey_usesResolvedNames() {
        List<Page<NamingStrategyItem>> pages = drainPublisher(
            mappedTable.index(GSI_EMAIL).query(QueryConditional.keyEqualTo(Key.builder().partitionValue("c-1@example.com").build())),
            1);
        List<NamingStrategyItem> bySort = drainPublisher(mappedTable.query(QueryConditional.keyEqualTo(
            Key.builder().partitionValue("id-1").sortValue("o-1").build())).items(), 1);

        assertThat(pages.get(0).items()).extracting(NamingStrategyItem::getCustomerId).containsExactly("c-1");
        assertThat(bySort).extracting(NamingStrategyItem::getOrderId).containsExactly("o-1");
    }

    @Test
    public void createTable_whenStaticSchema_storesTransformedPartitionKey() {
        String staticTable = getConcreteTableName("naming-async-index-static");
        DynamoDbAsyncTable<PlainNamingItem> table = enhancedClient.table(staticTable, NamingStrategySupport.staticKeySchema(strategy));
        table.createTable().join();
        PlainNamingItem item = new PlainNamingItem();
        item.setId("id-1");
        item.setCustomerId("c-1");
        table.putItem(item).join();

        String keyName = NamingStrategySupport.stored(strategy, "PK");
        assertThat(table.describeTable().join().table().keySchema()).extracting(KeySchemaElement::attributeName)
                                                                     .containsExactly(keyName);
        deleteQuietly(staticTable);
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
