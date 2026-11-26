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
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks query filters and nested projection against stored names.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyQueryTest extends LocalDynamoDbSyncTestBase {

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
     * Creates the mapped table and writes two items.
     */
    @Before
    public void createTable() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        tableName = getConcreteTableName("naming-query");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable();
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1"));
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-2", "c-2"));
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
    public void query_whenFilterUsesStoredName_findsItemAndJavaNameDoesNot() {
        String storedCustomer = NamingStrategySupport.stored(strategy, "customerId");
        String absentName = storedCustomer.equals("customerId") ? "customer_id" : "customerId";
        Key partition = Key.builder().partitionValue("id-1").build();

        List<NamingStrategyItem> found = query(partition, NamingStrategySupport.equalTo(storedCustomer, "c-1"));
        List<NamingStrategyItem> missed = query(partition, NamingStrategySupport.equalTo(absentName, "c-1"));

        assertThat(found).extracting(NamingStrategyItem::getOrderId).containsExactly("o-1");
        assertThat(missed).isEmpty();
        assertThat(strategyName).isNotBlank();
    }

    @Test
    public void query_whenNestedProjectionUsesStoredNames_readsInnerAttribute() {
        String homeCity = NamingStrategySupport.stored(strategy, "homeCity");
        String cityName = NamingStrategySupport.stored(strategy, "cityName");
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                                                           .queryConditional(QueryConditional.keyEqualTo(
                                                               Key.builder().partitionValue("id-1").sortValue("o-1").build()))
                                                           .addNestedAttributesToProject(NestedAttributeName.create(homeCity, cityName))
                                                           .build();

        NamingStrategyItem item = mappedTable.query(request).items().stream().collect(Collectors.toList()).get(0);

        assertThat(item.getHomeCity().getCityName()).isEqualTo("Rome");
        assertThat(item.getHomeCity().getStreetName()).isNull();
        assertThat(item.getCustomerId()).isNull();
    }

    /**
     * Queries the partition with the given filter.
     */
    private List<NamingStrategyItem> query(Key partition, software.amazon.awssdk.enhanced.dynamodb.Expression filter) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                                                           .queryConditional(QueryConditional.keyEqualTo(partition))
                                                           .filterExpression(filter)
                                                           .build();
        return mappedTable.query(request).items().stream().collect(Collectors.toList());
    }
}
