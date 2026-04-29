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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.BufferingSubscriber;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LargeDatasetInitializer;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbLargeDatasetTestBase;
import software.amazon.awssdk.enhanced.dynamodb.internal.client.DefaultDynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;

/**
 * Join-focused integration tests for the async enhanced query API. Assumes the large dataset has been seeded once via
 * {@link LargeDatasetInitializer#main(String[])}. Does not create or delete tables. Each test drains the publisher, measures
 * execution time, prints ms, and fails if &gt; 1 second.
 */
public class EnhancedQueryJoinAsyncTest extends LocalDynamoDbLargeDatasetTestBase {

    private static final long MAX_QUERY_MS = 1_000L;
    private static final long DRAIN_TIMEOUT_MS = 10_000L;

    private static class CustomerRecord {
        private String customerId;
        private String name;
        private String region;

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

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CustomerRecord that = (CustomerRecord) o;
            return Objects.equals(customerId, that.customerId) && Objects.equals(name, that.name) && Objects.equals(region,
                                                                                                                    that.region);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customerId, name, region);
        }
    }

    private static class OrderRecord {
        private String customerId;
        private String orderId;
        private Integer amount;

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OrderRecord that = (OrderRecord) o;
            return Objects.equals(customerId, that.customerId) && Objects.equals(orderId, that.orderId) && Objects.equals(amount, that.amount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customerId, orderId, amount);
        }
    }

    private static final TableSchema<CustomerRecord> CUSTOMER_SCHEMA =
        StaticTableSchema.builder(CustomerRecord.class)
                         .newItemSupplier(CustomerRecord::new)
                         .addAttribute(String.class,
                                       a -> a.name("customerId").getter(CustomerRecord::getCustomerId).setter(CustomerRecord::setCustomerId).tags(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("name").getter(CustomerRecord::getName).setter(CustomerRecord::setName))
                         .addAttribute(String.class,
                                       a -> a.name("region").getter(CustomerRecord::getRegion).setter(CustomerRecord::setRegion))
                         .build();

    private static final TableSchema<OrderRecord> ORDER_SCHEMA =
        StaticTableSchema.builder(OrderRecord.class)
                         .newItemSupplier(OrderRecord::new)
                         .addAttribute(String.class,
                                       a -> a.name("customerId").getter(OrderRecord::getCustomerId).setter(OrderRecord::setCustomerId).tags(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("orderId").getter(OrderRecord::getOrderId).setter(OrderRecord::setOrderId).tags(primarySortKey()))
                         .addAttribute(Integer.class,
                                       a -> a.name("amount").getter(OrderRecord::getAmount).setter(OrderRecord::setAmount))
                         .build();

    private DynamoDbEnhancedAsyncClient enhancedAsyncClient;
    private DynamoDbAsyncTable<CustomerRecord> customersTable;
    private DynamoDbAsyncTable<OrderRecord> ordersTable;

    @Override
    protected String getConcreteTableName(String logicalTableName) {
        if ("customers".equals(logicalTableName)) {
            return LargeDatasetInitializer.LARGE_CUSTOMERS_TABLE;
        }
        if ("orders".equals(logicalTableName)) {
            return LargeDatasetInitializer.LARGE_ORDERS_TABLE;
        }
        return super.getConcreteTableName(logicalTableName);
    }

    @Before
    public void setUp() {
        enhancedAsyncClient = DefaultDynamoDbEnhancedAsyncClient.builder()
                                                                .dynamoDbClient(getDynamoDbAsyncClient())
                                                                .build();
        customersTable = enhancedAsyncClient.table(getConcreteTableName("customers"), CUSTOMER_SCHEMA);
        ordersTable = enhancedAsyncClient.table(getConcreteTableName("orders"), ORDER_SCHEMA);
    }

    private String currentTestName() {
        String thisClass = getClass().getName();
        for (StackTraceElement element : new Throwable().getStackTrace()) {
            if (thisClass.equals(element.getClassName())) {
                String method = element.getMethodName();
                if (!"currentTestName".equals(method)
                    && !"runAndMeasure".equals(method)
                    && !method.startsWith("lambda$")
                    && !method.startsWith("invoke")) {
                    return method;
                }
            }
        }
        return "unknownTest";
    }

    private List<EnhancedQueryRow> runAndMeasure(QueryExpressionSpec spec) {
        String testName = currentTestName();
        String label = "EnhancedQueryJoinAsyncTest." + testName;
        long start = System.nanoTime();
        BufferingSubscriber<EnhancedQueryRow> subscriber = new BufferingSubscriber<>();
        enhancedAsyncClient.enhancedQuery(spec).subscribe(subscriber);
        subscriber.waitForCompletion(DRAIN_TIMEOUT_MS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        List<EnhancedQueryRow> rows = subscriber.bufferedItems();
        System.out.println(label + " query took "
                           + elapsedMs + " ms, rows=" + rows.size());
        writeQueryMetric(label, elapsedMs, rows.size());
        assertThat(subscriber.bufferedError()).isNull();
        assertThat(elapsedMs).isLessThanOrEqualTo(MAX_QUERY_MS);
        return rows;
    }

    /**
     * Base-only: query customers with key condition and limit. Expected response: Single row for c1 with customerId, name,
     * region; name=Customer1, region=EU. DynamoDB operation: query()
     */
    @Test
    public void baseOnly_withKeyConditionAndLimit() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        Map<String, Object> base = rows.get(0).getItem("base");
        assertThat(base).containsOnly(
            entry("customerId", "c1"),
            entry("name", "Customer1"),
            entry("region", "EU"));
    }

    /**
     * INNER join on customerId with key condition for c1. Expected response: One row per order of c1; each row has base and
     * joined with customerId, orderId, amount; first order c1-o1, amount 11. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinInner_returnsMatchingPairs() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(SEED_ORDERS_PER_CUSTOMER + 100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(SEED_ORDERS_PER_CUSTOMER);
        Set<String> orderIds = new HashSet<>();
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            assertThat(base).containsOnly(
                entry("customerId", "c1"),
                entry("name", "Customer1"),
                entry("region", "EU"));
            assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
            assertThat(joined.get("customerId")).isEqualTo("c1");
            String orderId = (String) joined.get("orderId");
            assertThat(orderId).matches("c1-o\\d+");
            assertThat(orderIds.add(orderId)).as("Order ID should be unique: " + orderId).isTrue();
            int orderNum = Integer.parseInt(orderId.substring(orderId.lastIndexOf('o') + 1));
            int amount = ((Number) joined.get("amount")).intValue();
            assertThat(amount).as("amount for %s should be 10+%d", orderId, orderNum).isEqualTo(10 + orderNum);
        }
        assertThat(orderIds).hasSize(SEED_ORDERS_PER_CUSTOMER);
        assertThat(rows.get(0).getItem("joined").get("orderId")).isEqualTo("c1-o1");
        assertThat(((Number) rows.get(0).getItem("joined").get("amount")).intValue()).isEqualTo(11);
    }

    /**
     * LEFT join on customerId for c1: every base row with optional joined (limit 50). Expected response: 50 rows; each has base
     * (c1) and joined (c1, orderId, amount). DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinLeft_returnsBaseRowsWithOptionalJoined() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.LEFT, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(50)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(50);
        Set<String> orderIds = new HashSet<>();
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            assertThat(base).containsOnly(
                entry("customerId", "c1"),
                entry("name", "Customer1"),
                entry("region", "EU"));
            assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
            assertThat(joined.get("customerId")).isEqualTo("c1");
            String orderId = (String) joined.get("orderId");
            assertThat(orderIds.add(orderId)).as("Order ID should be unique: " + orderId).isTrue();
            int orderNum = Integer.parseInt(orderId.substring(orderId.lastIndexOf('o') + 1));
            int amount = ((Number) joined.get("amount")).intValue();
            assertThat(amount).isEqualTo(10 + orderNum);
        }
        assertThat(orderIds).hasSize(50);
    }

    /**
     * RIGHT join on customerId for c1: every joined row with optional base. Expected response: SEED_ORDERS_PER_CUSTOMER rows;
     * each has joined (c1, orderId, amount) and base (c1). DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinRight_returnsJoinedRowsWithOptionalBase() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.RIGHT, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(SEED_ORDERS_PER_CUSTOMER + 100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(SEED_ORDERS_PER_CUSTOMER);
        Set<String> orderIds = new HashSet<>();
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
            assertThat(joined.get("customerId")).isEqualTo("c1");
            String orderId = (String) joined.get("orderId");
            assertThat(orderIds.add(orderId)).as("Order ID should be unique: " + orderId).isTrue();
            int orderNum = Integer.parseInt(orderId.substring(orderId.lastIndexOf('o') + 1));
            int amount = ((Number) joined.get("amount")).intValue();
            assertThat(amount).isEqualTo(10 + orderNum);
            assertThat(base).containsOnly(
                entry("customerId", "c1"),
                entry("name", "Customer1"),
                entry("region", "EU"));
        }
        assertThat(orderIds).hasSize(SEED_ORDERS_PER_CUSTOMER);
    }

    /**
     * FULL join on customerId for c1: union of LEFT and RIGHT (limit 150). Expected response: 150 rows; rows may have empty base
     * (right-only) or empty joined (left-only); when present, base is c1. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinFull_returnsUnionOfLeftAndRight() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.FULL, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(150)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(150);
        int bothPresent = 0;
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            if (!base.isEmpty()) {
                assertThat(base).containsOnlyKeys("customerId", "name", "region");
                assertThat(base.get("customerId")).isEqualTo("c1");
                assertThat(base.get("name")).isEqualTo("Customer1");
                assertThat(base.get("region")).isEqualTo("EU");
            }
            if (!joined.isEmpty()) {
                assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
                assertThat(joined.get("customerId")).isEqualTo("c1");
                int orderNum = Integer.parseInt(((String) joined.get("orderId")).substring(
                    ((String) joined.get("orderId")).lastIndexOf('o') + 1));
                assertThat(((Number) joined.get("amount")).intValue()).isEqualTo(10 + orderNum);
            }
            assertThat(!base.isEmpty() || !joined.isEmpty())
                .as("At least one of base or joined must be present").isTrue();
            if (!base.isEmpty() && !joined.isEmpty()) {
                assertThat(base.get("customerId")).isEqualTo(joined.get("customerId"));
                bothPresent++;
            }
        }
        assertThat(bothPresent).as("Most rows should have both base and joined").isGreaterThan(0);
    }

    /**
     * Filter base table with region=EU; ALLOW_SCAN with limit. Expected response: Only rows with region=EU; customerId and name
     * present; name starts with "Customer". DynamoDB operation: scan()
     */
    @Test
    public void withCondition_returnsFilteredRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .where(Condition.eq("region", "EU"))
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(600)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(EXPECTED_EU_CUSTOMER_COUNT);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            assertThat(base).containsOnlyKeys("customerId", "name", "region");
            assertThat(base.get("region")).isEqualTo("EU");
            String customerId = (String) base.get("customerId");
            int num = Integer.parseInt(customerId.substring(1));
            assertThat(num % 2).as("EU customer should have odd ID: " + customerId).isEqualTo(1);
            assertThat((String) base.get("name")).isEqualTo("Customer" + num);
        }
    }

    /**
     * Tree condition: (region=EU AND name begins with "C") OR region=NA; ALLOW_SCAN. Expected response: Non-empty rows; each base
     * has customerId, region (EU or NA). DynamoDB operation: scan()
     */
    @Test
    public void treeCondition_returnsMatchingRows() {
        Condition tree = Condition.group(
            Condition.eq("region", "EU").and(Condition.beginsWith("name", "C"))
        ).or(Condition.eq("region", "NA"));
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .where(tree)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(1000)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(SEED_CUSTOMER_COUNT);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            assertThat(base).containsOnlyKeys("customerId", "region", "name");
            String region = (String) base.get("region");
            String name = (String) base.get("name");
            boolean matchesCondition =
                ("EU".equals(region) && name.startsWith("C")) || "NA".equals(region);
            assertThat(matchesCondition)
                .as("Row should match tree condition: region=%s, name=%s", region, name)
                .isTrue();
        }
    }

    /**
     * Base-only scan with limit 5; no join. Expected response: Exactly 5 rows; each has base with customerId (cN), name, region.
     * DynamoDB operation: scan()
     */
    @Test
    public void limit_enforced() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(5)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(5);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            assertThat(base).containsOnlyKeys("customerId", "name", "region");
            assertThat((String) base.get("customerId")).matches("c\\d+");
        }
    }

    /**
     * STRICT_KEY_ONLY with base key condition for c1. Expected response: One row; base contains customerId c1, name Customer1,
     * region EU. DynamoDB operation: query()
     */
    @Test
    public void executionMode_strictKeyOnly_withKey() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        Map<String, Object> base = rows.get(0).getItem("base");
        assertThat(base).containsOnly(
            entry("customerId", "c1"),
            entry("name", "Customer1"),
            entry("region", "EU"));
    }

    /**
     * STRICT_KEY_ONLY without base key condition: no scan allowed. Expected response: Empty list of rows. DynamoDB operation:
     * none (returns empty)
     */
    @Test
    public void executionMode_strictKeyOnly_withoutKey_returnsEmptyOrNoScan() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).isEmpty();
    }

    /**
     * ALLOW_SCAN with limit 100: full table scan. Expected response: up to limit rows; each has base customerId (cN), name,
     * region (EU or NA). DynamoDB operation: scan()
     */
    @Test
    public void executionMode_allowScan() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            assertThat(base).containsOnlyKeys("customerId", "name", "region");
            String customerId = (String) base.get("customerId");
            assertThat(customerId).matches("c\\d+");
            int num = Integer.parseInt(customerId.substring(1));
            assertThat((String) base.get("name")).isEqualTo("Customer" + num);
            String region = (String) base.get("region");
            assertThat(region).isIn("EU", "NA");
            if (num % 2 == 1) {
                assertThat(region).isEqualTo("EU");
            } else {
                assertThat(region).isEqualTo("NA");
            }
        }
    }

    /**
     * INNER join with joined-table condition amount >= 50; base key c1. Expected response: 50 rows; every joined row has amount
     * >= 50. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinInner_withJoinedTableCondition_returnsFilteredJoinedRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        Set<String> orderIds = new HashSet<>();
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            assertThat(base).containsOnly(
                entry("customerId", "c1"),
                entry("name", "Customer1"),
                entry("region", "EU"));
            assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
            assertThat(joined.get("customerId")).isEqualTo("c1");
            int amount = ((Number) joined.get("amount")).intValue();
            assertThat(amount).isGreaterThanOrEqualTo(50);
            assertThat(amount).isLessThanOrEqualTo(EXPECTED_MAX_AMOUNT_C1);
            String orderId = (String) joined.get("orderId");
            assertThat(orderIds.add(orderId)).as("Order ID should be unique: " + orderId).isTrue();
            int orderNum = Integer.parseInt(orderId.substring(orderId.lastIndexOf('o') + 1));
            assertThat(amount).isEqualTo(10 + orderNum);
        }
    }

    /**
     * LEFT join with joined-table condition amount >= 50; base key c1. Expected response: up to limit rows; all joined rows
     * satisfy amount >= 50. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinLeft_withJoinedTableCondition_returnsFilteredJoinedRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.LEFT, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> joined = row.getItem("joined");
            assertThat(((Number) joined.get("amount")).intValue()).isGreaterThanOrEqualTo(50);
        }
    }

    /**
     * RIGHT join with joined-table condition amount >= 50; base key c1. Expected response: 100 rows; every row has joined data
     * with amount >= 50; base may be empty (right-only) or match joined customerId. DynamoDB operation: base=query(),
     * join=query()
     */
    @Test
    public void joinRight_withJoinedTableCondition_returnsFilteredJoinedRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.RIGHT, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> joined = row.getItem("joined");
            Map<String, Object> base = row.getItem("base");
            assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
            assertThat((Number) joined.get("amount")).isNotNull();
            assertThat(((Number) joined.get("amount")).intValue()).isGreaterThanOrEqualTo(50);
            assertThat((String) joined.get("customerId")).matches("c\\d+");
            if (!base.isEmpty()) {
                assertThat(base).containsOnlyKeys("customerId", "name", "region");
                assertThat(base.get("customerId")).isEqualTo(joined.get("customerId"));
            }
        }
    }

    /**
     * FULL join with joined-table condition amount >= 50; base key c1. Expected response: 150 rows; base may be empty; every row
     * with joined has amount >= 50; when base present it is c1. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void joinFull_withJoinedTableCondition_returnsFilteredJoinedRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.FULL, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(150)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(150);
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            if (!base.isEmpty()) {
                assertThat(base).containsOnly(
                    entry("customerId", "c1"),
                    entry("name", "Customer1"),
                    entry("region", "EU"));
            }
            if (!joined.isEmpty()) {
                assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
                assertThat(((Number) joined.get("amount")).intValue()).isGreaterThanOrEqualTo(50);
                assertThat((String) joined.get("customerId")).matches("c\\d+");
            }
        }
    }

    /**
     * Exercises all applicable join builders: from, join, baseKeyCondition, withBaseTableCondition, withJoinedTableCondition,
     * executionMode, limit. Expected response: up to limit rows (c1, EU, amount >= 50). DynamoDB operation: base=query(),
     * join=query()
     */
    @Test
    public void allBuilders_joinSpec_returnsExpectedRows() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterBase(Condition.eq("region", "EU"))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        Set<String> orderIds = new HashSet<>();
        for (EnhancedQueryRow row : rows) {
            Map<String, Object> base = row.getItem("base");
            Map<String, Object> joined = row.getItem("joined");
            assertThat(base).containsOnly(
                entry("customerId", "c1"),
                entry("name", "Customer1"),
                entry("region", "EU"));
            assertThat(joined).containsOnlyKeys("customerId", "orderId", "amount");
            assertThat(joined.get("customerId")).isEqualTo("c1");
            int amount = ((Number) joined.get("amount")).intValue();
            assertThat(amount).isGreaterThanOrEqualTo(EXPECTED_MIN_AMOUNT_C1_GE_50);
            assertThat(amount).isLessThanOrEqualTo(EXPECTED_MAX_AMOUNT_C1_GE_50);
            String orderId = (String) joined.get("orderId");
            assertThat(orderIds.add(orderId)).as("Order ID should be unique: " + orderId).isTrue();
            int orderNum = Integer.parseInt(orderId.substring(orderId.lastIndexOf('o') + 1));
            assertThat(amount).isEqualTo(10 + orderNum);
        }
        assertThat(orderIds).hasSize(100);
    }

    /**
     * Base key condition for non-existent customer: no rows match. Expected response: Empty list of rows, no error. DynamoDB
     * operation: query()
     */
    @Test
    public void emptyResult_returnsEmptyList() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c99999")))
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).isEmpty();
    }
}
