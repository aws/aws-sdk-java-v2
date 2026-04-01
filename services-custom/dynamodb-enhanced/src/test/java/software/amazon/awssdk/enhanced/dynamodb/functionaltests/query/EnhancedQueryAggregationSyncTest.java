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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LargeDatasetInitializer;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbLargeDatasetTestBase;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.query.condition.Condition;
import software.amazon.awssdk.enhanced.dynamodb.query.engine.QueryExpressionBuilder;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.AggregationFunction;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.ExecutionMode;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.JoinType;
import software.amazon.awssdk.enhanced.dynamodb.query.enums.SortDirection;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryLatencyReport;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryResult;
import software.amazon.awssdk.enhanced.dynamodb.query.result.EnhancedQueryRow;
import software.amazon.awssdk.enhanced.dynamodb.query.spec.QueryExpressionSpec;

/**
 * Aggregation-focused integration tests for the sync enhanced query API. Assumes the large dataset has been seeded once via
 * {@link LargeDatasetInitializer#main(String[])}. No create/delete; each test measures query time, prints ms, fails if &gt; 5
 * seconds. Covers GROUP BY, COUNT/SUM/MIN/MAX/AVG, filter, tree condition, ORDER BY aggregate/key, limit, ExecutionMode
 * (base-only, ALLOW_SCAN, STRICT_KEY_ONLY).
 */
public class EnhancedQueryAggregationSyncTest extends LocalDynamoDbLargeDatasetTestBase {

    private static final long MAX_QUERY_MS = 5_000L;

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

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<CustomerRecord> customersTable;
    private DynamoDbTable<OrderRecord> ordersTable;

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
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(getDynamoDbClient()).build();
        customersTable = enhancedClient.table(getConcreteTableName("customers"), CUSTOMER_SCHEMA);
        ordersTable = enhancedClient.table(getConcreteTableName("orders"), ORDER_SCHEMA);
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
        String label = "EnhancedQueryAggregationSyncTest." + testName;
        EnhancedQueryLatencyReport[] reportHolder = new EnhancedQueryLatencyReport[1];
        EnhancedQueryResult result = enhancedClient.enhancedQuery(spec, r -> reportHolder[0] = r);
        List<EnhancedQueryRow> rows = new ArrayList<>();
        result.forEach(rows::add);
        EnhancedQueryLatencyReport report = reportHolder[0];
        long elapsedMs = report != null ? report.totalMs() : 0L;
        if (report != null) {
            System.out.println(label
                               + " EnhancedQueryLatencyReport: baseQueryMs=" + report.baseQueryMs()
                               + " joinedLookupsMs=" + report.joinedLookupsMs()
                               + " inMemoryProcessingMs=" + report.inMemoryProcessingMs()
                               + " totalMs=" + report.totalMs() + " rows=" + rows.size());
        } else {
            System.out.println(label + " query took "
                               + elapsedMs + " ms, rows=" + rows.size());
        }
        writeQueryMetric(label, elapsedMs, rows.size());
        assertThat(elapsedMs).isLessThanOrEqualTo(MAX_QUERY_MS);
        return rows;
    }

    /**
     * Base-only (no join, no aggregation): key condition and limit. Expected response: One row for c1 with customerId, name,
     * region. DynamoDB operation: query()
     */
    @Test
    public void baseOnly_withKeyCondition() {
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
     * GROUP BY customerId, COUNT(orderId) as orderCount for c1. Expected response: One row; orderCount = 1000L. DynamoDB
     * operation: base=query(), join=query()
     */
    @Test
    public void aggregation_groupByCount() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        EnhancedQueryRow row = rows.get(0);
        assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
        assertThat(row.aggregates()).containsKey("orderCount");
        Map<String, Object> base = row.getItem("base");
        assertThat(base).containsEntry("customerId", "c1");
        assertThat(base).containsEntry("name", "Customer1");
        assertThat(base).containsEntry("region", "EU");
    }

    /**
     * GROUP BY customerId, SUM(amount) as totalAmount for c1. Expected response: One row; totalAmount = 510500L. DynamoDB
     * operation: base=query(), join=query()
     */
    @Test
    public void aggregation_groupBySum() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        EnhancedQueryRow row = rows.get(0);
        assertThat(((Number) row.getAggregate("totalAmount")).longValue()).isEqualTo(510500L);
        assertThat(row.aggregates()).containsKey("totalAmount");
        Map<String, Object> base = row.getItem("base");
        assertThat(base).containsEntry("customerId", "c1");
        assertThat(base).containsEntry("name", "Customer1");
        assertThat(base).containsEntry("region", "EU");
    }

    /**
     * GROUP BY customerId, MIN(amount) and MAX(amount) for c1. Expected response: One row; minAmount = 11, maxAmount = 110;
     * aggregates contain both keys. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void aggregation_groupByMinMax() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.MIN, "amount", "minAmount")
                                                         .aggregate(AggregationFunction.MAX, "amount", "maxAmount")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        EnhancedQueryRow row = rows.get(0);
        assertThat(((Number) row.getAggregate("minAmount")).intValue()).isEqualTo(11);
        assertThat(((Number) row.getAggregate("maxAmount")).intValue()).isEqualTo(1010);
        assertThat(row.aggregates()).containsKeys("minAmount", "maxAmount");
    }

    /**
     * GROUP BY customerId, AVG(amount) for c1. Expected response: One row; avgAmount = 510.5. DynamoDB operation: base=query(),
     * join=query()
     */
    @Test
    public void aggregation_groupByAvg() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.AVG, "amount", "avgAmount")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        Object avg = rows.get(0).getAggregate("avgAmount");
        assertThat(avg).isNotNull();
        double avgVal = avg instanceof Number ? ((Number) avg).doubleValue() : Double.parseDouble(avg.toString());
        assertThat(avgVal).isEqualTo(510.5);
        assertThat(rows.get(0).aggregates()).containsKey("avgAmount");
    }

    /**
     * Aggregation with withBaseTableCondition(region=EU); ALLOW_SCAN. Expected response: One row per EU customer; orderCount =
     * 1000L each. DynamoDB operation: base=scan(), join=query()
     */
    @Test
    public void aggregation_withFilter() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .filterBase(Condition.eq("region", "EU"))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(500)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(EXPECTED_EU_CUSTOMER_COUNT);
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
            assertThat(row.aggregates()).containsKey("orderCount");
            Map<String, Object> base = row.getItem("base");
            assertThat(base.get("region")).isEqualTo("EU");
            String customerId = (String) base.get("customerId");
            int num = Integer.parseInt(customerId.substring(1));
            assertThat(num % 2).as("EU customer should have odd ID: " + customerId).isEqualTo(1);
            assertThat((String) base.get("name")).isEqualTo("Customer" + num);
        }
    }

    /**
     * Aggregation with tree condition (region EU+name beginsWith C) OR region NA; ALLOW_SCAN. Expected response: One row per
     * matching customer; orderCount = 1000L each. DynamoDB operation: base=scan(), join=query()
     */
    @Test
    public void aggregation_treeCondition() {
        Condition tree = Condition.group(
            Condition.eq("region", "EU").and(Condition.beginsWith("name", "C"))
        ).or(
            Condition.eq("region", "NA"));

        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .filterBase(tree)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(500)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).isNotEmpty();
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
            assertThat(row.aggregates()).containsKey("orderCount");
            Map<String, Object> base = row.getItem("base");
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
     * ORDER BY aggregate orderCount DESC; ALLOW_SCAN, limit 20. Expected response: Up to 20 rows; orderCount non-increasing; each
     * orderCount = 1000L. DynamoDB operation: base=scan(), join=query()
     */
    @Test
    public void aggregation_orderByAggregate() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .orderByAggregate("orderCount", SortDirection.DESC)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(20)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSizeLessThanOrEqualTo(20);
        for (int i = 1; i < rows.size(); i++) {
            Number prev = (Number) rows.get(i - 1).getAggregate("orderCount");
            Number curr = (Number) rows.get(i).getAggregate("orderCount");
            assertThat(prev.longValue()).isGreaterThanOrEqualTo(curr.longValue());
        }
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
        }
    }

    /**
     * ORDER BY key customerId ASC; ALLOW_SCAN, limit 20. Expected response: Up to 20 rows; each has orderCount = 1000L. DynamoDB
     * operation: base=scan(), join=query()
     */
    @Test
    public void aggregation_orderByKey() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .orderBy("customerId", SortDirection.ASC)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(20)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSizeLessThanOrEqualTo(20);
        for (int i = 1; i < rows.size(); i++) {
            String prev = (String) rows.get(i - 1).getItem("base").get("customerId");
            String curr = (String) rows.get(i).getItem("base").get("customerId");
            assertThat(prev.compareTo(curr)).as("customerIds should be ASC: %s <= %s", prev, curr)
                                            .isLessThanOrEqualTo(0);
        }
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
            Map<String, Object> base = row.getItem("base");
            assertThat((String) base.get("customerId")).matches("c\\d+");
            assertThat((String) base.get("name")).startsWith("Customer");
            assertThat(base.get("region")).isIn("EU", "NA");
        }
    }

    /**
     * Limit on aggregation buckets; ALLOW_SCAN, limit 5. Expected response: At most 5 rows; each orderCount = 1000L. DynamoDB
     * operation: base=scan(), join=query()
     */
    @Test
    public void aggregation_limit() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(5)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSizeLessThanOrEqualTo(5);
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
        }
    }

    /**
     * STRICT_KEY_ONLY with base key c1: aggregation runs. Expected response: One row; orderCount = 1000L. DynamoDB operation:
     * base=query(), join=query()
     */
    @Test
    public void executionMode_strictKeyOnly_withKey() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(
                                                             "c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAggregate("orderCount")).isEqualTo(1000L);
        Map<String, Object> base = rows.get(0).getItem("base");
        assertThat(base).containsEntry("customerId", "c1");
        assertThat(base).containsEntry("name", "Customer1");
        assertThat(base).containsEntry("region", "EU");
    }

    /**
     * ALLOW_SCAN aggregation over full scan; limit 100. Expected response: up to limit rows; each orderCount = 1000L. DynamoDB
     * operation: base=scan(), join=query()
     */
    @Test
    public void executionMode_allowScan() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isEqualTo(1000L);
            assertThat(row.aggregates()).containsKey("orderCount");
            Map<String, Object> base = row.getItem("base");
            assertThat((String) base.get("customerId")).matches("c\\d+");
            assertThat((String) base.get("name")).startsWith("Customer");
            assertThat(base.get("region")).isIn("EU", "NA");
        }
    }

    /**
     * Join with withJoinedTableCondition(amount >= 50); groupBy customerId, COUNT(orderId) for c1. Expected response: One row;
     * orderCount = 961 (EXPECTED_ORDER_COUNT_C1_AMOUNT_GE_50). DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void aggregation_withJoinedTableCondition_returnsFilteredCount() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAggregate("orderCount")).isEqualTo(EXPECTED_ORDER_COUNT_C1_AMOUNT_GE_50);
        assertThat(rows.get(0).aggregates()).containsKey("orderCount");
        Map<String, Object> base = rows.get(0).getItem("base");
        assertThat(base).containsEntry("customerId", "c1");
        assertThat(base).containsEntry("name", "Customer1");
        assertThat(base).containsEntry("region", "EU");
    }

    /**
     * Join with withJoinedTableCondition(amount >= 50); ALLOW_SCAN; one row per customer with filtered orderCount. Expected
     * response: 100 rows (limited by spec); each row has orderCount present and <= SEED_ORDERS_PER_CUSTOMER. DynamoDB operation:
     * base=scan(), join=query()
     */
    @Test
    public void aggregation_withJoinedTableCondition_allowScan_returnsFilteredCounts() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(100)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(100);
        for (EnhancedQueryRow row : rows) {
            assertThat(row.getAggregate("orderCount")).isNotNull();
            long orderCount = ((Number) row.getAggregate("orderCount")).longValue();
            assertThat(orderCount).isGreaterThan(0);
            assertThat(orderCount).isLessThanOrEqualTo(SEED_ORDERS_PER_CUSTOMER);
            Map<String, Object> base = row.getItem("base");
            String customerId = (String) base.get("customerId");
            int k = Integer.parseInt(customerId.substring(1));
            long expectedCount = Math.max(0, SEED_ORDERS_PER_CUSTOMER - Math.max(0, 49 - 10 * k));
            assertThat(orderCount)
                .as("orderCount for %s should match formula", customerId)
                .isEqualTo(expectedCount);
        }
    }

    /**
     * STRICT_KEY_ONLY without base key condition: no scan; aggregation returns empty. Expected response: Empty list of rows.
     * DynamoDB operation: none (returns empty)
     */
    @Test
    public void executionMode_strictKeyOnly_withoutKey_returnsEmptyOrNoScan() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).isEmpty();
    }

    /**
     * Exercises all aggregation builders: from, join, baseKeyCondition, withBaseTableCondition, withJoinedTableCondition,
     * groupBy, aggregate (COUNT, SUM, MIN, MAX, AVG), orderBy, orderByAggregate, executionMode, limit. Expected response: One row
     * (c1); orderCount=961, totalAmount/min/max/avg present and consistent. DynamoDB operation: base=query(), join=query()
     */
    @Test
    public void allBuilders_aggregationSpec_returnsExpectedAggregates() {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTable)
                                                         .join(ordersTable, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterBase(Condition.eq("region", "EU"))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .aggregate(AggregationFunction.MIN, "amount", "minAmount")
                                                         .aggregate(AggregationFunction.MAX, "amount", "maxAmount")
                                                         .aggregate(AggregationFunction.AVG, "amount", "avgAmount")
                                                         .orderBy("customerId", SortDirection.ASC)
                                                         .orderByAggregate("orderCount", SortDirection.DESC)
                                                         .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(10)
                                                         .build();
        List<EnhancedQueryRow> rows = runAndMeasure(spec);
        assertThat(rows).hasSize(1);
        EnhancedQueryRow row = rows.get(0);
        assertThat(row.getAggregate("orderCount")).isEqualTo(EXPECTED_ORDER_COUNT_C1_AMOUNT_GE_50);
        assertThat(row.aggregates()).containsKeys("orderCount", "totalAmount", "minAmount", "maxAmount", "avgAmount");
        assertThat(((Number) row.getAggregate("minAmount")).intValue()).isEqualTo(EXPECTED_MIN_AMOUNT_C1_GE_50);
        assertThat(((Number) row.getAggregate("maxAmount")).intValue()).isEqualTo(EXPECTED_MAX_AMOUNT_C1_GE_50);
        assertThat(((Number) row.getAggregate("totalAmount")).longValue()).isEqualTo(EXPECTED_TOTAL_AMOUNT_C1_GE_50);
        assertThat(((Number) row.getAggregate("avgAmount")).doubleValue())
            .isEqualTo(EXPECTED_AVG_AMOUNT_C1_GE_50);
        long count = ((Number) row.getAggregate("orderCount")).longValue();
        double avg = ((Number) row.getAggregate("avgAmount")).doubleValue();
        long sum = ((Number) row.getAggregate("totalAmount")).longValue();
        assertThat(Math.abs(sum - count * avg)).as("sum should equal count * avg").isLessThan(1.0);
        Map<String, Object> base = row.getItem("base");
        assertThat(base).containsEntry("customerId", "c1");
        assertThat(base).containsEntry("region", "EU");
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
