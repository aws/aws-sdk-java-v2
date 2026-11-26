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
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStrategyItem;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks a scan filter written with the stored attribute name.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyScanTest extends LocalDynamoDbSyncTestBase {

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
     * Creates the mapped table and writes one item.
     */
    @Before
    public void createTable() {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        tableName = getConcreteTableName("naming-scan");
        mappedTable = enhancedClient.table(tableName, NamingStrategySupport.itemSchema(strategy));
        mappedTable.createTable();
        mappedTable.putItem(NamingStrategySupport.item("id-1", "o-1", "c-1"));
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
    public void scan_whenFilterUsesStoredName_findsItem() {
        String storedCustomer = NamingStrategySupport.stored(strategy, "customerId");
        String absentName = storedCustomer.equals("customerId") ? "customer_id" : "customerId";

        List<NamingStrategyItem> found = scan(storedCustomer);
        List<NamingStrategyItem> missed = scan(absentName);

        assertThat(found).extracting(NamingStrategyItem::getCustomerId).containsExactly("c-1");
        assertThat(missed).isEmpty();
        assertThat(strategyName).isNotBlank();
    }

    /**
     * Scans with an equality filter on the given attribute name.
     */
    private List<NamingStrategyItem> scan(String attributeName) {
        ScanEnhancedRequest request = ScanEnhancedRequest.builder()
                                                         .filterExpression(NamingStrategySupport.equalTo(attributeName, "c-1"))
                                                         .build();
        return mappedTable.scan(request).items().stream().collect(Collectors.toList());
    }
}
