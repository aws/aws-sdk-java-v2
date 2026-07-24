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

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LargeDatasetInitializer;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbTestBase;
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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

/**
 * Standalone benchmark runner for Enhanced Query (join and aggregation) scenarios. Connects to real DynamoDB (or DynamoDB Local)
 * and runs a fixed set of query scenarios with warm-up and multiple iterations, then prints latency stats (avg, p50, p95) and row
 * counts.
 * <p>
 * Environment variables:
 * <ul>
 *   <li>{@code AWS_REGION} – Optional. Region for DynamoDB (e.g. us-east-1). If unset, uses default region.</li>
 *   <li>{@code CUSTOMERS_TABLE} – Name of the Customers table (default: customers_large).</li>
 *   <li>{@code ORDERS_TABLE} – Name of the Orders table (default: orders_large).</li>
 *   <li>{@code CREATE_AND_SEED} – If "true", creates tables (if missing) and seeds 1000 customers x 1000 orders.
 *   Requires DynamoDB create/put permissions.</li>
 *   <li>{@code BENCHMARK_ITERATIONS} – Number of measured iterations per scenario (default: 5).</li>
 *   <li>{@code BENCHMARK_WARMUP} – Number of warm-up runs per scenario (default: 2).</li>
 *   <li>{@code BENCHMARK_OUTPUT_FILE} – Optional. If set, append CSV results to this file.</li>
 *   <li>{@code USE_LOCAL_DYNAMODB} – If "true", uses in-process DynamoDB Local: starts LocalDynamoDb, creates and seeds
 *   tables (1000 customers x 1000 orders), runs benchmarks, then stops. No AWS credentials required. Use
 *   {@code run-enhanced-query-benchmark-local.sh} to run this mode.</li>
 * </ul>
 * <p>
 * Run from repo root:
 * <pre>
 * mvn test-compile exec:java -pl services-custom/dynamodb-enhanced \
 *   -Dexec.mainClass="software.amazon.awssdk.enhanced.dynamodb.functionaltests.EnhancedQueryBenchmarkRunner" \
 *   -Dexec.classpathScope=test
 * </pre>
 */
public final class EnhancedQueryBenchmarkRunner {

    private static final String CUSTOMERS_TABLE_ENV = "CUSTOMERS_TABLE";
    private static final String ORDERS_TABLE_ENV = "ORDERS_TABLE";
    private static final String CREATE_AND_SEED_ENV = "CREATE_AND_SEED";
    private static final String BENCHMARK_ITERATIONS_ENV = "BENCHMARK_ITERATIONS";
    private static final String BENCHMARK_WARMUP_ENV = "BENCHMARK_WARMUP";
    private static final String BENCHMARK_OUTPUT_FILE_ENV = "BENCHMARK_OUTPUT_FILE";
    private static final String USE_LOCAL_DYNAMODB_ENV = "USE_LOCAL_DYNAMODB";

    private static final String DEFAULT_CUSTOMERS_TABLE = "customers_large";
    private static final String DEFAULT_ORDERS_TABLE = "orders_large";
    private static final int DEFAULT_ITERATIONS = 5;
    private static final int DEFAULT_WARMUP = 2;

    // Table column widths for aligned benchmark output
    private static final int COL_SCENARIO = 38;
    private static final int COL_DDB_OP = 26;
    private static final int COL_DESCRIPTION = 62;
    private static final int COL_AVG = 10;
    private static final int COL_P50 = 10;
    private static final int COL_P95 = 10;
    private static final int COL_ROWS = 8;

    // Unicode box-drawing for table borders (easy to read)
    private static final char BOX_H = '\u2500';  // ─ horizontal
    private static final char BOX_V = '\u2502';  // │ vertical
    private static final String BOX_TL = "\u250c"; // ┌ top-left
    private static final String BOX_TC = "\u252c"; // ┬ top-center
    private static final String BOX_TR = "\u2510"; // ┐ top-right
    private static final String BOX_ML = "\u251c"; // ├ mid-left
    private static final String BOX_MC = "\u253c"; // ┼ mid-cross
    private static final String BOX_MR = "\u2524"; // ┤ mid-right
    private static final String BOX_BL = "\u2514"; // └ bottom-left
    private static final String BOX_BC = "\u2534"; // ┴ bottom-center
    private static final String BOX_BR = "\u2518"; // ┘ bottom-right

    // ANSI color for latency columns (AVG, P50, P95) – cyan, reset for terminal
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_RESET = "\033[0m";

    private static final ProvisionedThroughput PROVISIONED_THROUGHPUT =
        ProvisionedThroughput.builder().readCapacityUnits(50L).writeCapacityUnits(50L).build();

    // Minimal POJOs matching the table shape used by LargeDatasetInitializer and tests
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

    public static void main(String[] args) {
        String regionStr = System.getenv("AWS_REGION");
        String customersTable = System.getenv(CUSTOMERS_TABLE_ENV);
        if (customersTable == null || customersTable.isEmpty()) {
            customersTable = DEFAULT_CUSTOMERS_TABLE;
        }
        String ordersTable = System.getenv(ORDERS_TABLE_ENV);
        if (ordersTable == null || ordersTable.isEmpty()) {
            ordersTable = DEFAULT_ORDERS_TABLE;
        }
        boolean useLocalDynamoDb = "true".equalsIgnoreCase(System.getenv(USE_LOCAL_DYNAMODB_ENV));
        boolean createAndSeed = useLocalDynamoDb || "true".equalsIgnoreCase(System.getenv(CREATE_AND_SEED_ENV));
        int iterations = parseIntEnv(BENCHMARK_ITERATIONS_ENV, DEFAULT_ITERATIONS);
        int warmup = parseIntEnv(BENCHMARK_WARMUP_ENV, DEFAULT_WARMUP);
        String outputFile = System.getenv(BENCHMARK_OUTPUT_FILE_ENV);

        DynamoDbClient dynamoDbClient;
        if (useLocalDynamoDb) {
            dynamoDbClient = LocalDynamoDbTestBase.createLocalDynamoDbClient();
            System.out.println("Using in-process DynamoDB Local.");
        } else if (regionStr != null && !regionStr.isEmpty()) {
            dynamoDbClient = DynamoDbClient.builder().region(Region.of(regionStr)).build();
        } else {
            dynamoDbClient = DynamoDbClient.create();
        }

        try {
            if (createAndSeed) {
                int customerCount = parseIntEnv("CUSTOMER_COUNT", LargeDatasetInitializer.DEFAULT_CUSTOMER_COUNT);
                int ordersPerCustomer = parseIntEnv("ORDERS_PER_CUSTOMER", LargeDatasetInitializer.DEFAULT_ORDERS_PER_CUSTOMER);
                System.out.printf("Creating tables and seeding data (%,d customers x %,d orders)...%n",
                                  customerCount, ordersPerCustomer);
                LargeDatasetInitializer.initializeCustomersAndOrdersDataset(
                    dynamoDbClient,
                    customersTable,
                    ordersTable,
                    customerCount,
                    ordersPerCustomer,
                    PROVISIONED_THROUGHPUT);
                System.out.println("Seed complete.");
            }

            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
            DynamoDbTable<CustomerRecord> customersTableRef = enhancedClient.table(customersTable, CUSTOMER_SCHEMA);
            DynamoDbTable<OrderRecord> ordersTableRef = enhancedClient.table(ordersTable, ORDER_SCHEMA);

            List<Scenario> scenarios = Arrays.asList(

                // --- Group 1: Baselines ---

                new Scenario("single_customer_by_key",
                             "Retrieve one customer by partition key. Establishes minimum DynamoDB round-trip latency.",
                             "query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .project("customerId", "name", "region")
                                                         .limit(10)
                                                         .build()),

                new Scenario("scan_100_customers",
                             "Read first 100 customers without key condition. Establishes scan baseline.",
                             "scan()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .project("customerId", "name", "region")
                                                         .limit(100)
                                                         .build()),

                // --- Group 2: Individual aggregation functions (key-scoped) ---

                new Scenario("count_orders_one_customer",
                             "COUNT all orders for customer c1. Returns 1 row with order count.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("sum_amount_one_customer",
                             "SUM of order amounts for customer c1. Returns 1 row with total revenue.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("avg_amount_one_customer",
                             "AVG of order amounts for customer c1. Returns 1 row with average order value.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.AVG, "amount", "avgAmount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("min_amount_one_customer",
                             "MIN order amount for customer c1. Returns 1 row with smallest order.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.MIN, "amount", "minAmount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("max_amount_one_customer",
                             "MAX order amount for customer c1. Returns 1 row with largest order.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.MAX, "amount", "maxAmount")
                                                         .limit(10)
                                                         .build()),

                // --- Group 3: Combined and complex aggregations ---

                new Scenario("all_five_functions_one_customer",
                             "COUNT, SUM, AVG, MIN, MAX combined in one query for c1. Proves zero overhead for multiple functions.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .aggregate(AggregationFunction.AVG, "amount", "avgAmount")
                                                         .aggregate(AggregationFunction.MIN, "amount", "minAmount")
                                                         .aggregate(AggregationFunction.MAX, "amount", "maxAmount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("count_and_sum_with_amount_filter",
                             "COUNT + SUM only for orders where amount >= 50. Pre-aggregation filter reduces input rows.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .filterJoined(Condition.gte("amount", 50))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("count_per_customer_having_gt500",
                             "COUNT per customer across all customers, keep only groups with count > 500. Post-aggregation HAVING.",
                             "base=scan(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .having(Condition.gt("orderCount", 500))
                                                         .limit(20)
                                                         .build()),

                new Scenario("count_and_sum_grouped_by_two_fields",
                             "COUNT + SUM grouped by (customerId, region). Multi-field composite GROUP BY key.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId", "region")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .limit(10)
                                                         .build()),

                new Scenario("top10_customers_by_order_count",
                             "COUNT per customer, sorted by count descending, return only top 10. ORDER BY aggregate + limit.",
                             "base=scan(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .orderByAggregate("orderCount", SortDirection.DESC)
                                                         .limit(10)
                                                         .build()),

                new Scenario("global_sum_and_count_no_groupby",
                             "Global SUM + COUNT over all orders for c1 without GROUP BY. Single-bucket global aggregation.",
                             "query()",
                             () -> QueryExpressionBuilder.from(ordersTableRef)
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "totalOrders")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalRevenue")
                                                         .limit(10)
                                                         .build()),

                // --- Group 4: Scan-backed aggregations (ALLOW_SCAN) ---

                new Scenario("scan_count_all_customers",
                             "COUNT orders per customer, scanning the entire customers table. Shows full-table aggregation cost.",
                             "base=scan(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .limit(20)
                                                         .build()),

                new Scenario("scan_sum_only_eu_customers",
                             "SUM(amount) per customer, scan with region=EU filter. Shows filtered scan + aggregation cost.",
                             "base=scan(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .filterBase(Condition.eq("region", "EU"))
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .limit(500)
                                                         .build()),

                new Scenario("scan_having_orderby_full_combo",
                             "COUNT + SUM per customer, HAVING count > 500, ORDER BY total DESC, limit 10. All features on scan.",
                             "base=scan(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .having(Condition.gt("orderCount", 500))
                                                         .orderByAggregate("totalAmount", SortDirection.DESC)
                                                         .limit(10)
                                                         .build()),

                // --- Group 5: Join preview (Phase 2) ---

                new Scenario("join_all_orders_one_customer",
                             "INNER join customer c1 with all orders. Raw join without aggregation (Phase 2 preview).",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .project("customerId", "name", "region", "orderId", "amount")
                                                         .limit(1100)
                                                         .build()),

                new Scenario("join_then_count_and_sum",
                             "INNER join c1 + COUNT + SUM. Shows aggregation collapsing 1,000 joined rows into 1 result.",
                             "base=query(), join=query()",
                             () -> QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .limit(10)
                                                         .build())
            );

            PrintStream out = System.out;
            StringBuilder csv = new StringBuilder();
            if (outputFile != null && !outputFile.isEmpty()) {
                csv.append("scenario,description,ddbOperation,avgMs,p50Ms,p95Ms,rows,region,iterations\n");
            }

            out.println("Environment: " + (useLocalDynamoDb ? "DynamoDB Local (in-process)" :
                                           "AWS_REGION=" + (regionStr != null ? regionStr : "default"))
                        + " CUSTOMERS_TABLE=" + customersTable + " ORDERS_TABLE=" + ordersTable);
            out.println("Warmup=" + warmup + " Iterations=" + iterations);
            out.println();
            out.println("DynamoDB operations:");
            out.println("  query()  = read by partition (and optional sort) key; efficient, bounded by key.");
            out.println("  scan()   = full table (or index) read; no key condition; filters applied in-memory.");
            out.println("  base=query(), join=query() = base table read by key, then joined table read by key per row.");
            out.println("  base=scan(), join=query()  = base table scanned, then joined table read by key per row.");
            out.println();

            String topBorder = tableBorder(BOX_TL, BOX_TC, BOX_TR);
            String midBorder = tableBorder(BOX_ML, BOX_MC, BOX_MR);
            String bottomBorder = tableBorder(BOX_BL, BOX_BC, BOX_BR);
            out.println(topBorder);
            out.println(tableDataRow(
                padRight("SCENARIO", COL_SCENARIO),
                padRight("DDB OPERATION", COL_DDB_OP),
                padRight("DESCRIPTION", COL_DESCRIPTION),
                ANSI_CYAN + padLeft("AVG(ms)", COL_AVG) + ANSI_RESET,
                ANSI_CYAN + padLeft("P50(ms)", COL_P50) + ANSI_RESET,
                ANSI_CYAN + padLeft("P95(ms)", COL_P95) + ANSI_RESET,
                padLeft("ROWS", COL_ROWS)));
            out.println(midBorder);

            for (int idx = 0; idx < scenarios.size(); idx++) {
                Scenario scenario = scenarios.get(idx);
                Result result = runScenario(enhancedClient, scenario, warmup, iterations);
                List<String> descLines = wrap(scenario.description, COL_DESCRIPTION);
                String namePadded = padRight(truncate(scenario.name, COL_SCENARIO), COL_SCENARIO);
                String ddbPadded = padRight(truncate(scenario.ddbOperation, COL_DDB_OP), COL_DDB_OP);
                String avgStr = padLeft(String.format(Locale.US, "%.1f", result.avgMs), COL_AVG);
                String p50Str = padLeft(String.valueOf(result.p50Ms), COL_P50);
                String p95Str = padLeft(String.valueOf(result.p95Ms), COL_P95);
                String rowsStr = padLeft(String.valueOf(result.rows), COL_ROWS);
                String avgCol = ANSI_CYAN + avgStr + ANSI_RESET;
                String p50Col = ANSI_CYAN + p50Str + ANSI_RESET;
                String p95Col = ANSI_CYAN + p95Str + ANSI_RESET;
                for (int i = 0; i < descLines.size(); i++) {
                    String descCell = padRight(descLines.get(i), COL_DESCRIPTION);
                    if (i == 0) {
                        out.println(tableDataRow(namePadded, ddbPadded, descCell, avgCol, p50Col, p95Col, rowsStr));
                    } else {
                        out.println(tableDataRow(
                            repeat(' ', COL_SCENARIO), repeat(' ', COL_DDB_OP), descCell,
                            repeat(' ', COL_AVG), repeat(' ', COL_P50), repeat(' ', COL_P95), repeat(' ', COL_ROWS)));
                    }
                }
                if (csv.length() > 0) {
                    csv.append(String.format(Locale.US, "%s,\"%s\",\"%s\",%.2f,%d,%d,%d,%s,%d%n",
                                             scenario.name, scenario.description, scenario.ddbOperation,
                                             result.avgMs, result.p50Ms, result.p95Ms, result.rows,
                                             useLocalDynamoDb ? "local" : (regionStr != null ? regionStr : "default"),
                                             iterations));
                }
                if (idx < scenarios.size() - 1) {
                    out.println();
                }
            }
            out.println(bottomBorder);

            if (outputFile != null && !outputFile.isEmpty() && csv.length() > 0) {
                try {
                    java.nio.file.Files.write(java.nio.file.Paths.get(outputFile),
                                              csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                              java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    out.println("Results appended to " + outputFile);
                } catch (Exception e) {
                    System.err.println("Failed to write " + outputFile + ": " + e.getMessage());
                }
            }
        } finally {
            dynamoDbClient.close();
            if (useLocalDynamoDb) {
                LocalDynamoDbTestBase.stopLocalDynamoDb();
            }
        }
    }

    private static int parseIntEnv(String key, int defaultValue) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    /**
     * Wraps text to multiple lines of at most maxLen characters, breaking at word boundaries when possible.
     */
    private static List<String> wrap(String s, int maxLen) {
        List<String> lines = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            lines.add("");
            return lines;
        }
        String rest = s.trim();
        while (!rest.isEmpty()) {
            if (rest.length() <= maxLen) {
                lines.add(rest);
                break;
            }
            int breakAt = rest.lastIndexOf(' ', maxLen);
            if (breakAt <= 0) {
                breakAt = Math.min(maxLen, rest.length());
            }
            lines.add(rest.substring(0, breakAt).trim());
            rest = rest.substring(breakAt).trim();
        }
        return lines;
    }

    private static String repeat(char ch, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        return s + repeat(' ', width - s.length());
    }

    private static String padLeft(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        return repeat(' ', width - s.length()) + s;
    }

    /**
     * Builds a horizontal table border (top, middle, or bottom) using box-drawing characters.
     */
    private static String tableBorder(String left, String cross, String right) {
        return left + repeat(BOX_H, COL_SCENARIO) + cross + repeat(BOX_H, COL_DDB_OP) + cross
               + repeat(BOX_H, COL_DESCRIPTION) + cross + repeat(BOX_H, COL_AVG) + cross
               + repeat(BOX_H, COL_P50) + cross + repeat(BOX_H, COL_P95) + cross + repeat(BOX_H, COL_ROWS) + right;
    }

    /**
     * Builds one row of table cells with vertical borders.
     */
    private static String tableDataRow(String v1, String v2, String v3, String v4, String v5, String v6, String v7) {
        return "" + BOX_V + v1 + BOX_V + v2 + BOX_V + v3 + BOX_V + v4 + BOX_V + v5 + BOX_V + v6 + BOX_V + v7 + BOX_V;
    }

    private static class Scenario {
        final String name;
        final String description;
        final String ddbOperation;
        final Supplier<QueryExpressionSpec> specSupplier;

        Scenario(String name, String description, String ddbOperation, Supplier<QueryExpressionSpec> specSupplier) {
            this.name = name;
            this.description = description;
            this.ddbOperation = ddbOperation;
            this.specSupplier = specSupplier;
        }
    }

    private static class Result {
        final double avgMs;
        final long p50Ms;
        final long p95Ms;
        final int rows;

        Result(double avgMs, long p50Ms, long p95Ms, int rows) {
            this.avgMs = avgMs;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.rows = rows;
        }
    }

    private static Result runScenario(DynamoDbEnhancedClient enhancedClient, Scenario scenario, int warmup, int iterations) {
        QueryExpressionSpec spec = scenario.specSupplier.get();
        for (int i = 0; i < warmup; i++) {
            runOnce(enhancedClient, spec);
        }
        List<Long> times = new ArrayList<>(iterations);
        int rows = 0;
        for (int i = 0; i < iterations; i++) {
            long[] msHolder = new long[1];
            int[] rowsHolder = new int[1];
            runOnce(enhancedClient, spec, msHolder, rowsHolder);
            times.add(msHolder[0]);
            rows = rowsHolder[0];
        }
        Collections.sort(times);
        long p50 = times.get((int) (iterations * 0.5));
        long p95 = times.get((int) Math.min(Math.ceil(iterations * 0.95) - 1, iterations - 1));
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        return new Result(avg, p50, p95, rows);
    }

    private static void runOnce(DynamoDbEnhancedClient enhancedClient, QueryExpressionSpec spec, long[] outMs, int[] outRows) {
        EnhancedQueryLatencyReport[] reportHolder = new EnhancedQueryLatencyReport[1];
        EnhancedQueryResult result = enhancedClient.enhancedQuery(spec, r -> reportHolder[0] = r);
        int count = 0;
        for (EnhancedQueryRow row : result) {
            count++;
        }
        outMs[0] = reportHolder[0] != null ? reportHolder[0].totalMs() : 0L;
        outRows[0] = count;
    }

    private static void runOnce(DynamoDbEnhancedClient enhancedClient, QueryExpressionSpec spec) {
        runOnce(enhancedClient, spec, new long[1], new int[1]);
    }
}
