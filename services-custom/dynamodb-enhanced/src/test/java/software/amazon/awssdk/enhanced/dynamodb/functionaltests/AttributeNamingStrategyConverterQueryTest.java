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

import java.util.ArrayList;
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
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingConverterItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingStaticConverterItem;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NamingStrategyModels.NamingToken;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Checks a query filter on the stored name of a converted attribute.
 */
@RunWith(Parameterized.class)
public class AttributeNamingStrategyConverterQueryTest extends LocalDynamoDbSyncTestBase {

    @Parameterized.Parameter(0)
    public String caseName;

    @Parameterized.Parameter(1)
    public AttributeNamingStrategy strategy;

    @Parameterized.Parameter(2)
    public String schemaKind;

    private DynamoDbEnhancedClient enhancedClient;
    private String tableName;

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
     * Creates the table and writes one converted item.
     */
    @Before
    public void createTable() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        tableName = getConcreteTableName("naming-converter-query");
        if ("bean".equals(schemaKind)) {
            DynamoDbTable<NamingConverterItem> table = enhancedClient.table(tableName, NamingStrategySupport.converterBeanSchema(strategy));
            table.createTable();
            NamingConverterItem item = new NamingConverterItem();
            item.setId("id-1");
            item.setOrderId("o-1");
            item.setCustomerCode(new NamingToken("c-1"));
            table.putItem(item);
        } else {
            DynamoDbTable<NamingStaticConverterItem> table =
                enhancedClient.table(tableName, NamingStrategySupport.converterStaticSchema(strategy));
            table.createTable();
            NamingStaticConverterItem item = new NamingStaticConverterItem();
            item.setId("id-1");
            item.setOrderId("o-1");
            item.setCustomerCode(new NamingToken("c-1"));
            table.putItem(item);
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
    public void query_whenFilterUsesStoredName_findsConvertedItem() {
        String storedCode = NamingStrategySupport.stored(strategy, "customerCode");
        Expression filter = NamingStrategySupport.equalTo(storedCode, TOKEN_PREFIX + "c-1");
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                                                           .queryConditional(QueryConditional.keyEqualTo(
                                                               Key.builder().partitionValue("id-1").build()))
                                                           .filterExpression(filter)
                                                           .build();
        if ("bean".equals(schemaKind)) {
            DynamoDbTable<NamingConverterItem> table = enhancedClient.table(tableName, NamingStrategySupport.converterBeanSchema(strategy));
            List<NamingConverterItem> items = table.query(request).items().stream().collect(Collectors.toList());
            assertThat(items).extracting(item -> item.getCustomerCode().value()).containsExactly("c-1");
        } else {
            DynamoDbTable<NamingStaticConverterItem> table =
                enhancedClient.table(tableName, NamingStrategySupport.converterStaticSchema(strategy));
            List<NamingStaticConverterItem> items = table.query(request).items().stream().collect(Collectors.toList());
            assertThat(items).extracting(item -> item.getCustomerCode().value()).containsExactly("c-1");
        }
        assertThat(caseName).isNotBlank();
    }
}
