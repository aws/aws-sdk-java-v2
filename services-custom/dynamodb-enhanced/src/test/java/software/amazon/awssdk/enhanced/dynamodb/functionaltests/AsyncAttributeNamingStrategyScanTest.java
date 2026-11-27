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
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks an async scan filter written with the stored attribute name.
 */
@RunWith(Parameterized.class)
public class AsyncAttributeNamingStrategyScanTest extends LocalDynamoDbAsyncTestBase {

    @Parameterized.Parameter(0)
    public String strategyName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

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
        DynamoDbEnhancedAsyncClient enhancedClient =
            DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
        tableName = getConcreteTableName("naming-async-scan");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable().join();
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1")).join();
    }

    /**
     * Deletes the async table.
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
    public void scan_whenFilterUsesStoredName_findsItem() {
        String storedCustomer = NamingStrategySupport.stored(strategy, "customerId");
        String absentName = storedCustomer.equals("customerId") ? "customer_id" : "customerId";

        assertThat(scan(storedCustomer, 1)).extracting(NamingStrategyItem::getCustomerId).containsExactly("c-1");
        assertThat(scan(absentName, 0)).isEmpty();
        assertThat(strategyName).isNotBlank();
    }

    /**
     * Scans with an equality filter on the given attribute name.
     */
    private List<NamingStrategyItem> scan(String attributeName, int expected) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                                                         .filterExpression(NamingStrategySupport.equalTo(attributeName, "c-1"))
                                                         .build();
        return drainPublisher(mappedTable.scan(request).items(), expected);
    }
}
