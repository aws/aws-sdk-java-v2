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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

/**
 * Functional tests for nested attribute (dot-path) filtering in the enhanced query engine.
 * Uses LocalDynamoDB with items that contain nested Map attributes (e.g. address.city).
 */
public class NestedAttributeFilteringTest extends LocalDynamoDbTestBase {

    private static final String TABLE_NAME = "nested_attr_test";

    private static class CustomerWithAddress {
        private String customerId;
        private String name;
        private Map<String, String> address;

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getAddress() {
            return address;
        }

        public void setAddress(Map<String, String> address) {
            this.address = address;
        }
    }

    private static final TableSchema<CustomerWithAddress> SCHEMA =
        StaticTableSchema.builder(CustomerWithAddress.class)
                         .newItemSupplier(CustomerWithAddress::new)
                         .addAttribute(String.class,
                                       a -> a.name("customerId")
                                             .getter(CustomerWithAddress::getCustomerId)
                                             .setter(CustomerWithAddress::setCustomerId)
                                             .tags(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("name")
                                             .getter(CustomerWithAddress::getName)
                                             .setter(CustomerWithAddress::setName))
                         .addAttribute(EnhancedType.mapOf(String.class, String.class),
                                       a -> a.name("address")
                                             .getter(CustomerWithAddress::getAddress)
                                             .setter(CustomerWithAddress::setAddress))
                         .build();

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<CustomerWithAddress> table;
    private String concreteTableName;

    @Before
    public void setUp() {
        concreteTableName = getConcreteTableName(TABLE_NAME);
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(localDynamoDb().createClient())
                                               .build();
        table = enhancedClient.table(concreteTableName, SCHEMA);
        table.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));

        putCustomer("c1", "Alice", "Seattle", "WA", "98101");
        putCustomer("c2", "Bob", "Portland", "OR", "97201");
        putCustomer("c3", "Charlie", "Seattle", "WA", "98102");
        putCustomer("c4", "Diana", "San Francisco", "CA", "94105");
        putCustomer("c5", "Eve", "New York", "NY", "10001");
    }

    @After
    public void tearDown() {
        try {
            localDynamoDb().createClient().deleteTable(DeleteTableRequest.builder()
                                                                        .tableName(concreteTableName)
                                                                        .build());
        } catch (Exception ignored) {
            // table may not exist
        }
    }

    private void putCustomer(String id, String name, String city, String state, String zip) {
        CustomerWithAddress c = new CustomerWithAddress();
        c.setCustomerId(id);
        c.setName(name);
        Map<String, String> address = new HashMap<>();
        address.put("city", city);
        address.put("state", state);
        address.put("zip", zip);
        c.setAddress(address);
        table.putItem(c);
    }

    private String currentTestName() {
        String thisClass = getClass().getName();
        for (StackTraceElement element : new Throwable().getStackTrace()) {
            if (thisClass.equals(element.getClassName())) {
                String method = element.getMethodName();
                if (!"currentTestName".equals(method)
                    && !"executeQuery".equals(method)
                    && !method.startsWith("lambda$")
                    && !method.startsWith("invoke")) {
                    return method;
                }
            }
        }
        return "unknownTest";
    }

    private List<EnhancedQueryRow> executeQuery(QueryExpressionSpec spec) {
        String label = "NestedAttributeFilteringTest." + currentTestName();
        long start = System.nanoTime();
        EnhancedQueryResult result = enhancedClient.enhancedQuery(spec);
        List<EnhancedQueryRow> rows = new ArrayList<>();
        result.forEach(rows::add);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        writeQueryMetric(label, elapsedMs, rows.size());
        return rows;
    }

    @Test
    public void filterByNestedCity_returnsMatchingRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
            .where(Condition.eq("address.city", "Seattle"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getItem("base")).containsEntry("name", "Alice");
    }

    @Test
    public void filterByNestedCity_noMatch() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
            .where(Condition.eq("address.city", "Portland"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).isEmpty();
    }

    @Test
    public void filterByNestedState_scanMode() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .executionMode(ExecutionMode.ALLOW_SCAN)
            .where(Condition.eq("address.state", "WA"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(2);

        List<String> names = new ArrayList<>();
        for (EnhancedQueryRow row : rows) {
            names.add((String) row.getItem("base").get("name"));
        }
        assertThat(names).containsExactlyInAnyOrder("Alice", "Charlie");
    }

    @Test
    public void filterByNestedZip_between() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .executionMode(ExecutionMode.ALLOW_SCAN)
            .where(Condition.between("address.zip", "98000", "98999"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(2);
    }

    @Test
    public void filterByNestedCity_contains() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .executionMode(ExecutionMode.ALLOW_SCAN)
            .where(Condition.contains("address.city", "land"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getItem("base")).containsEntry("name", "Bob");
    }

    @Test
    public void filterByNestedCity_beginsWith() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .executionMode(ExecutionMode.ALLOW_SCAN)
            .where(Condition.beginsWith("address.city", "San"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getItem("base")).containsEntry("name", "Diana");
    }

    @Test
    public void filterByNestedAndFlatAttribute_combined() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .executionMode(ExecutionMode.ALLOW_SCAN)
            .where(Condition.eq("address.state", "WA").and(Condition.eq("name", "Charlie")))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getItem("base")).containsEntry("customerId", "c3");
    }

    @Test
    public void filterByNestedAttribute_orCombination() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .executionMode(ExecutionMode.ALLOW_SCAN)
            .where(Condition.eq("address.state", "CA").or(Condition.eq("address.state", "NY")))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).hasSize(2);

        List<String> names = new ArrayList<>();
        for (EnhancedQueryRow row : rows) {
            names.add((String) row.getItem("base").get("name"));
        }
        assertThat(names).containsExactlyInAnyOrder("Diana", "Eve");
    }

    @Test
    public void filterByMissingNestedPath_returnsNoMatch() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(table)
            .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
            .where(Condition.eq("address.country", "US"))
            .build();

        List<EnhancedQueryRow> rows = executeQuery(spec);
        assertThat(rows).isEmpty();
    }
}
