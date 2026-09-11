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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LargeDatasetInitializer;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbTestBase;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
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
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

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
    private static final String SEED_BENCHMARK_EXTENSIONS_ENV = "SEED_BENCHMARK_EXTENSIONS";

    private static final String DEFAULT_CUSTOMERS_TABLE = "customers_large";
    private static final String DEFAULT_ORDERS_TABLE = "orders_large";
    /** Defaults match HLD Appendix A9 (override via BENCHMARK_ITERATIONS / BENCHMARK_WARMUP). */
    private static final int DEFAULT_ITERATIONS = 10;
    private static final int DEFAULT_WARMUP = 3;

    private static final String ORPHAN_CUSTOMER_ID = "c_orphan";
    private static final String ORPHAN_ORDER_ID = "o_orphan";
    private static final String ORPHAN_ORDER_CUSTOMER_ID = "c_nonexistent";
    private static final int JOIN_PAGINATION_PAGE_SIZE = 100;
    private static final int SUMMARY_PAGINATION_PAGE_SIZE = 10;

    // Table column widths for aligned benchmark output
    private static final int COL_SCENARIO = 45;
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
        boolean seedExtensions = createAndSeed
                                 || "true".equalsIgnoreCase(System.getenv(SEED_BENCHMARK_EXTENSIONS_ENV));
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
            int customerCount = parseIntEnv("CUSTOMER_COUNT", LargeDatasetInitializer.DEFAULT_CUSTOMER_COUNT);
            int ordersPerCustomer = parseIntEnv("ORDERS_PER_CUSTOMER", LargeDatasetInitializer.DEFAULT_ORDERS_PER_CUSTOMER);
            if (createAndSeed) {
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

            if (seedExtensions) {
                seedBenchmarkExtensions(customersTableRef, ordersTableRef);
                System.out.println("Benchmark extension seed complete (orphan rows, c1 region modify).");
            }

            int havingThreshold = parseIntEnv("HAVING_ORDER_COUNT_THRESHOLD",
                                             Math.min(500, Math.max(0, ordersPerCustomer - 1)));
            List<Scenario> scenarios = buildScenarios(
                customersTableRef, ordersTableRef, havingThreshold, ordersPerCustomer);

            PrintStream out = System.out;
            List<Result> results = new ArrayList<>();

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
                Result result = runScenario(enhancedClient, dynamoDbClient, scenario, warmup, iterations);
                results.add(result);
                List<String> descLines = wrap(scenario.description, COL_DESCRIPTION);
                String namePadded = padRight(truncate(scenario.name, COL_SCENARIO), COL_SCENARIO);
                String ddbPadded = padRight(truncate(scenario.ddbOperation, COL_DDB_OP), COL_DDB_OP);
                String avgStr = padLeft(String.format(Locale.US, "%.2f", result.avgMs), COL_AVG);
                String p50Str = padLeft(String.format(Locale.US, "%.2f", (double) result.p50Ms), COL_P50);
                String p95Str = padLeft(String.format(Locale.US, "%.2f", (double) result.p95Ms), COL_P95);
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
                if (idx < scenarios.size() - 1) {
                    out.println();
                }
            }
            out.println(bottomBorder);

            if (outputFile != null && !outputFile.isEmpty()) {
                try {
                    writeCsv(outputFile, scenarios, results,
                             useLocalDynamoDb ? "local" : (regionStr != null ? regionStr : "default"),
                             customerCount, ordersPerCustomer, warmup, iterations);
                    out.println("Results written to " + outputFile);
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

    private static void seedBenchmarkExtensions(DynamoDbTable<CustomerRecord> customersTable,
                                                DynamoDbTable<OrderRecord> ordersTable) {
        CustomerRecord orphanCustomer = new CustomerRecord();
        orphanCustomer.setCustomerId(ORPHAN_CUSTOMER_ID);
        orphanCustomer.setName("OrphanCustomer");
        orphanCustomer.setRegion("EU");
        customersTable.putItem(orphanCustomer);

        OrderRecord orphanOrder = new OrderRecord();
        orphanOrder.setCustomerId(ORPHAN_ORDER_CUSTOMER_ID);
        orphanOrder.setOrderId(ORPHAN_ORDER_ID);
        orphanOrder.setAmount(999);
        ordersTable.putItem(orphanOrder);

        CustomerRecord c1 = customersTable.getItem(
            GetItemEnhancedRequest.builder().key(Key.builder().partitionValue("c1").build()).build());
        if (c1 != null) {
            c1.setRegion("APAC");
            c1.setName("Customer1Modified");
            customersTable.putItem(c1);
        }
    }

    private static List<Scenario> buildScenarios(DynamoDbTable<CustomerRecord> customersTableRef,
                                                 DynamoDbTable<OrderRecord> ordersTableRef,
                                                 int havingThreshold,
                                                 int ordersPerCustomer) {
        int joinRowLimit = ordersPerCustomer + 100;
        List<Scenario> scenarios = new ArrayList<>();

        scenarios.add(Scenario.fromSpec("single_customer_by_key",
                                        "Retrieve one customer by partition key. Establishes minimum DynamoDB round-trip latency.",
                                        "query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .project("customerId", "name", "region")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("scan_100_customers",
                                        "Read first 100 customers without key condition. Establishes scan baseline.",
                                        "scan()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .project("customerId", "name", "region")
                                                                    .limit(100)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("count_orders_one_customer",
                                        "COUNT all orders for customer c1. Returns 1 row with order count.",
                                        "base=query(), join=query()",
                                        () -> joinAggSpec(customersTableRef, ordersTableRef, JoinType.INNER, "c1",
                                                          AggregationFunction.COUNT, "orderId", "orderCount")));

        scenarios.add(Scenario.fromSpec("sum_amount_one_customer",
                                        "SUM of order amounts for customer c1. Returns 1 row with total revenue.",
                                        "base=query(), join=query()",
                                        () -> joinAggSpec(customersTableRef, ordersTableRef, JoinType.INNER, "c1",
                                                          AggregationFunction.SUM, "amount", "totalAmount")));

        scenarios.add(Scenario.fromSpec("avg_amount_one_customer",
                                        "AVG of order amounts for customer c1. Returns 1 row with average order value.",
                                        "base=query(), join=query()",
                                        () -> joinAggSpec(customersTableRef, ordersTableRef, JoinType.INNER, "c1",
                                                          AggregationFunction.AVG, "amount", "avgAmount")));

        scenarios.add(Scenario.fromSpec("min_amount_one_customer",
                                        "MIN order amount for customer c1. Returns 1 row with smallest order.",
                                        "base=query(), join=query()",
                                        () -> joinAggSpec(customersTableRef, ordersTableRef, JoinType.INNER, "c1",
                                                          AggregationFunction.MIN, "amount", "minAmount")));

        scenarios.add(Scenario.fromSpec("max_amount_one_customer",
                                        "MAX order amount for customer c1. Returns 1 row with largest order.",
                                        "base=query(), join=query()",
                                        () -> joinAggSpec(customersTableRef, ordersTableRef, JoinType.INNER, "c1",
                                                          AggregationFunction.MAX, "amount", "maxAmount")));

        scenarios.add(Scenario.fromSpec("all_five_functions_one_customer",
                                        "COUNT, SUM, AVG, MIN, MAX combined in one query for c1.",
                                        "base=query(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalAmount")
                                                                    .aggregate(AggregationFunction.AVG, "amount",
                                                                               "avgAmount")
                                                                    .aggregate(AggregationFunction.MIN, "amount",
                                                                               "minAmount")
                                                                    .aggregate(AggregationFunction.MAX, "amount",
                                                                               "maxAmount")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("count_and_sum_with_amount_filter",
                                        "COUNT + SUM only for orders where amount >= 50.",
                                        "base=query(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .filterJoined(Condition.gte("amount", 50))
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalAmount")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("count_per_customer_having_gt500",
                                        "COUNT per customer, HAVING orderCount > " + havingThreshold + ".",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .having(Condition.gt("orderCount", havingThreshold))
                                                                    .limit(20)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("count_and_sum_grouped_by_two_fields",
                                        "COUNT + SUM grouped by (customerId, region) for c1.",
                                        "base=query(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .groupBy("customerId", "region")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalAmount")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("top10_customers_by_order_count",
                                        "COUNT per customer, ORDER BY orderCount DESC, top 10.",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .orderByAggregate("orderCount", SortDirection.DESC)
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("global_sum_and_count_no_groupby",
                                        "SUM + COUNT for c1 without GROUP BY (single-bucket aggregation).",
                                        "query()",
                                        () -> QueryExpressionBuilder.from(ordersTableRef)
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "totalOrders")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalRevenue")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("scan_count_all_customers",
                                        "COUNT orders per customer over full customer scan (limit 20).",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .limit(20)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("scan_sum_only_eu_customers",
                                        "SUM(amount) per customer where region=EU (limit 500).",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .filterBase(Condition.eq("region", "EU"))
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalAmount")
                                                                    .limit(500)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("scan_having_orderby_full_combo",
                                        "COUNT+SUM, HAVING count > " + havingThreshold + ", ORDER BY totalAmount DESC.",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalAmount")
                                                                    .having(Condition.gt("orderCount", havingThreshold))
                                                                    .orderByAggregate("totalAmount", SortDirection.DESC)
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(joinAllOrdersScenario(customersTableRef, ordersTableRef, JoinType.INNER, "inner", joinRowLimit));
        scenarios.add(joinCountAndSumScenario(customersTableRef, ordersTableRef, JoinType.INNER, "inner"));
        scenarios.add(joinAllOrdersScenario(customersTableRef, ordersTableRef, JoinType.LEFT, "left", joinRowLimit));
        scenarios.add(joinCountAndSumScenario(customersTableRef, ordersTableRef, JoinType.LEFT, "left"));
        scenarios.add(joinAllOrdersScenario(customersTableRef, ordersTableRef, JoinType.RIGHT, "right", joinRowLimit));
        scenarios.add(joinCountAndSumScenario(customersTableRef, ordersTableRef, JoinType.RIGHT, "right"));
        scenarios.add(joinAllOrdersScenario(customersTableRef, ordersTableRef, JoinType.FULL, "full", joinRowLimit));
        scenarios.add(joinCountAndSumScenario(customersTableRef, ordersTableRef, JoinType.FULL, "full"));

        scenarios.add(Scenario.fromSpec("filtered_aggregate_large_orders_one_customer",
                                        "Dedicated filtered COUNT+SUM for orders with amount >= 50 on c1.",
                                        "base=query(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .filterJoined(Condition.gte("amount", 50))
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "largeOrders")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "largeRevenue")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(customScenario(
            "summary_pagination_having_page2",
            "Page 2 (offset " + SUMMARY_PAGINATION_PAGE_SIZE + ") after scan+HAVING+ORDER BY aggregate sort.",
            "base=scan(), join=query() page 2",
            (client, ignored) -> runSummaryPaginationPage2(client, customersTableRef, ordersTableRef, havingThreshold)));

        scenarios.add(customScenario(
            "join_pagination_page2",
            "Page 2 of joined orders for c1 (limit " + JOIN_PAGINATION_PAGE_SIZE + " + LEK).",
            "base=query(), join=query() page 2",
            (ignored, dynamoDbClient) -> runJoinPaginationPage2(dynamoDbClient, ordersTableRef.tableName())));

        scenarios.add(Scenario.fromSpec("having_with_between",
                                        "HAVING orderCount BETWEEN " + (havingThreshold - 1) + " AND "
                                        + (havingThreshold + 1) + ".",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .having(Condition.between("orderCount",
                                                                                                havingThreshold - 1,
                                                                                                havingThreshold + 1))
                                                                    .limit(20)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("having_with_or",
                                        "HAVING orderCount > " + havingThreshold + " OR orderCount < 5.",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .having(Condition.gt("orderCount", havingThreshold)
                                                                                  .or(Condition.lt("orderCount", 5)))
                                                                    .limit(20)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("outer_join_orphan_customer_left",
                                        "LEFT join on orphan customer " + ORPHAN_CUSTOMER_ID + " (parent-only row).",
                                        "base=query(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.LEFT, "customerId",
                                                                          "customerId")
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue(ORPHAN_CUSTOMER_ID)))
                                                                    .project("customerId", "name", "region", "orderId",
                                                                             "amount")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("outer_join_orphan_order_right",
                                        "RIGHT join surfacing orphan order " + ORPHAN_ORDER_ID + " (no matching customer).",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.RIGHT, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .filterJoined(Condition.eq("orderId", ORPHAN_ORDER_ID))
                                                                    .project("customerId", "name", "region", "orderId",
                                                                             "amount")
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(customScenario(
            "batch_get_five_customer_summaries",
            "Five key-scoped join+COUNT queries for c1..c5 (logical batch read).",
            "5x base=query(), join=query()",
            (client, ignored) -> runBatchFiveCustomerSummaries(client, customersTableRef, ordersTableRef)));

        scenarios.add(customScenario(
            "consistent_read_summary_one_customer",
            "Strongly consistent GetItem for customer c1.",
            "getItem(consistentRead=true)",
            (ignored, dynamoDbClient) -> runConsistentReadCustomer(dynamoDbClient, customersTableRef.tableName())));

        scenarios.add(Scenario.fromSpec("top10_by_total_amount_gsi",
                                        "COUNT+SUM per customer, ORDER BY totalAmount DESC, top 10.",
                                        "base=scan(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .executionMode(ExecutionMode.ALLOW_SCAN)
                                                                    .groupBy("customerId")
                                                                    .aggregate(AggregationFunction.COUNT, "orderId",
                                                                               "orderCount")
                                                                    .aggregate(AggregationFunction.SUM, "amount",
                                                                               "totalAmount")
                                                                    .orderByAggregate("totalAmount", SortDirection.DESC)
                                                                    .limit(10)
                                                                    .build()));

        scenarios.add(Scenario.fromSpec("customer_modify_fanout_region",
                                        "INNER join c1 after parent MODIFY (region=APAC from seed extension).",
                                        "base=query(), join=query()",
                                        () -> QueryExpressionBuilder.from(customersTableRef)
                                                                    .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                          "customerId")
                                                                    .keyCondition(QueryConditional.keyEqualTo(
                                                                        k -> k.partitionValue("c1")))
                                                                    .project("customerId", "name", "region", "orderId",
                                                                             "amount")
                                                                    .limit(10)
                                                                    .build()));

        return scenarios;
    }

    private static Scenario customScenario(String name,
                                           String description,
                                           String ddbOperation,
                                           ScenarioAction action) {
        return new Scenario(name, description, ddbOperation, action);
    }

    private static QueryExpressionSpec joinAggSpec(DynamoDbTable<CustomerRecord> customersTableRef,
                                                   DynamoDbTable<OrderRecord> ordersTableRef,
                                                   JoinType joinType,
                                                   String customerId,
                                                   AggregationFunction function,
                                                   String attribute,
                                                   String outputName) {
        return QueryExpressionBuilder.from(customersTableRef)
                                     .join(ordersTableRef, joinType, "customerId", "customerId")
                                     .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue(customerId)))
                                     .groupBy("customerId")
                                     .aggregate(function, attribute, outputName)
                                     .limit(10)
                                     .build();
    }

    private static Scenario joinAllOrdersScenario(DynamoDbTable<CustomerRecord> customersTableRef,
                                                  DynamoDbTable<OrderRecord> ordersTableRef,
                                                  JoinType joinType,
                                                  String suffix,
                                                  int joinRowLimit) {
        return Scenario.fromSpec("join_all_orders_one_customer_" + suffix,
                                 joinType + " join customer c1 with all orders (raw join, no aggregation).",
                                 "base=query(), join=query()",
                                 () -> QueryExpressionBuilder.from(customersTableRef)
                                                             .join(ordersTableRef, joinType, "customerId", "customerId")
                                                             .keyCondition(QueryConditional.keyEqualTo(
                                                                 k -> k.partitionValue("c1")))
                                                             .project("customerId", "name", "region", "orderId", "amount")
                                                             .limit(joinRowLimit)
                                                             .build());
    }

    private static Scenario joinCountAndSumScenario(DynamoDbTable<CustomerRecord> customersTableRef,
                                                    DynamoDbTable<OrderRecord> ordersTableRef,
                                                    JoinType joinType,
                                                    String suffix) {
        return Scenario.fromSpec("join_then_count_and_sum_" + suffix,
                                 joinType + " join c1 + COUNT + SUM collapsed to one aggregate row.",
                                 "base=query(), join=query()",
                                 () -> QueryExpressionBuilder.from(customersTableRef)
                                                             .join(ordersTableRef, joinType, "customerId", "customerId")
                                                             .keyCondition(QueryConditional.keyEqualTo(
                                                                 k -> k.partitionValue("c1")))
                                                             .groupBy("customerId")
                                                             .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                             .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                             .limit(10)
                                                             .build());
    }

    private static RunOutcome runSummaryPaginationPage2(DynamoDbEnhancedClient client,
                                                        DynamoDbTable<CustomerRecord> customersTableRef,
                                                        DynamoDbTable<OrderRecord> ordersTableRef,
                                                        int havingThreshold) {
        QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTableRef)
                                                         .join(ordersTableRef, JoinType.INNER, "customerId", "customerId")
                                                         .executionMode(ExecutionMode.ALLOW_SCAN)
                                                         .groupBy("customerId")
                                                         .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                         .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                         .having(Condition.gt("orderCount", havingThreshold))
                                                         .orderByAggregate("totalAmount", SortDirection.DESC)
                                                         .limit(SUMMARY_PAGINATION_PAGE_SIZE * 2)
                                                         .build();
        EnhancedQueryLatencyReport[] reportHolder = new EnhancedQueryLatencyReport[1];
        long start = System.nanoTime();
        List<EnhancedQueryRow> rows = new ArrayList<>();
        for (EnhancedQueryRow row : client.enhancedQuery(spec, report -> reportHolder[0] = report)) {
            rows.add(row);
        }
        int pageTwoRows = rows.size() <= SUMMARY_PAGINATION_PAGE_SIZE
                          ? 0 : Math.min(SUMMARY_PAGINATION_PAGE_SIZE, rows.size() - SUMMARY_PAGINATION_PAGE_SIZE);
        return outcomeFromReport(System.nanoTime() - start, pageTwoRows, reportHolder[0]);
    }

    private static RunOutcome runJoinPaginationPage2(DynamoDbClient client, String ordersTableName) {
        long start = System.nanoTime();
        QueryResponse page1 = client.query(QueryRequest.builder()
                                                        .tableName(ordersTableName)
                                                        .keyConditionExpression("customerId = :customerId")
                                                        .expressionAttributeValues(Collections.singletonMap(
                                                            ":customerId", AttributeValue.builder().s("c1").build()))
                                                        .limit(JOIN_PAGINATION_PAGE_SIZE)
                                                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                        .build());
        Map<String, AttributeValue> lek = page1.lastEvaluatedKey();
        if (lek == null || lek.isEmpty()) {
            return new RunOutcome((System.nanoTime() - start) / 1_000_000L, 0,
                                  capacityUnits(page1), 1L);
        }
        QueryResponse page2 = client.query(QueryRequest.builder()
                                                        .tableName(ordersTableName)
                                                        .keyConditionExpression("customerId = :customerId")
                                                        .expressionAttributeValues(Collections.singletonMap(
                                                            ":customerId", AttributeValue.builder().s("c1").build()))
                                                        .limit(JOIN_PAGINATION_PAGE_SIZE)
                                                        .exclusiveStartKey(lek)
                                                        .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                        .build());
        return new RunOutcome((System.nanoTime() - start) / 1_000_000L, page2.items().size(),
                              capacityUnits(page1) + capacityUnits(page2), 2L);
    }

    private static RunOutcome runBatchFiveCustomerSummaries(DynamoDbEnhancedClient client,
                                                            DynamoDbTable<CustomerRecord> customersTableRef,
                                                            DynamoDbTable<OrderRecord> ordersTableRef) {
        long start = System.nanoTime();
        int count = 0;
        double rcu = 0.0d;
        long requests = 0L;
        for (int i = 1; i <= 5; i++) {
            String customerId = "c" + i;
            QueryExpressionSpec spec = QueryExpressionBuilder.from(customersTableRef)
                                                             .join(ordersTableRef, JoinType.INNER, "customerId",
                                                                   "customerId")
                                                             .keyCondition(QueryConditional.keyEqualTo(
                                                                 k -> k.partitionValue(customerId)))
                                                             .groupBy("customerId")
                                                             .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                             .limit(10)
                                                             .build();
            EnhancedQueryLatencyReport[] reportHolder = new EnhancedQueryLatencyReport[1];
            for (EnhancedQueryRow ignored : client.enhancedQuery(spec, report -> reportHolder[0] = report)) {
                count++;
            }
            if (reportHolder[0] != null) {
                rcu += reportHolder[0].totalRcuConsumed();
                requests += reportHolder[0].totalDynamoDbRequestCount();
            }
        }
        return new RunOutcome((System.nanoTime() - start) / 1_000_000L, count, rcu, requests);
    }

    private static RunOutcome runConsistentReadCustomer(DynamoDbClient client, String customersTableName) {
        long start = System.nanoTime();
        software.amazon.awssdk.services.dynamodb.model.GetItemResponse response = client.getItem(
            GetItemRequest.builder()
                          .tableName(customersTableName)
                          .key(Collections.singletonMap("customerId", AttributeValue.builder().s("c1").build()))
                          .consistentRead(true)
                          .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                          .build());
        return new RunOutcome((System.nanoTime() - start) / 1_000_000L,
                              response.item() == null || response.item().isEmpty() ? 0 : 1,
                              capacityUnits(response), 1L);
    }

    private static RunOutcome outcomeFromReport(long elapsedNs, int rows, EnhancedQueryLatencyReport report) {
        return new RunOutcome(elapsedNs / 1_000_000L, rows,
                              report == null ? 0.0d : report.totalRcuConsumed(),
                              report == null ? 0L : report.totalDynamoDbRequestCount());
    }

    private static double capacityUnits(QueryResponse response) {
        return response.consumedCapacity() == null || response.consumedCapacity().capacityUnits() == null
               ? 0.0d : response.consumedCapacity().capacityUnits();
    }

    private static double capacityUnits(software.amazon.awssdk.services.dynamodb.model.GetItemResponse response) {
        return response.consumedCapacity() == null || response.consumedCapacity().capacityUnits() == null
               ? 0.0d : response.consumedCapacity().capacityUnits();
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

    private static final String READ_CSV_HEADER =
        "Run ID,Solution,Scenario ID,Scenario,Category,Description,Execution Path,Result Status,Expected Rows,Observed Rows,"
        + "Average Latency (ms),P50 Latency (ms),P95 Latency (ms),Average Read Capacity Units,"
        + "Average Write Capacity Units,Average DynamoDB Requests,Total Read Capacity Units,"
        + "Total Write Capacity Units,Total DynamoDB Requests,AWS Region,EC2 Instance Type,DynamoDB Billing Mode,"
        + "Read Consistency,Customer Count,Orders Per Customer,Warmup Iterations,Measured Iterations";

    private static void writeCsv(String outputFile,
                                 List<Scenario> scenarios,
                                 List<Result> results,
                                 String region,
                                 int customerCount,
                                 int ordersPerCustomer,
                                 int warmup,
                                 int iterations) throws java.io.IOException {
        java.nio.file.Path file = java.nio.file.Paths.get(outputFile);
        if (file.getParent() != null) {
            java.nio.file.Files.createDirectories(file.getParent());
        }
        try (java.io.PrintWriter out = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(
            file, java.nio.charset.StandardCharsets.UTF_8,
            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING))) {
            out.println(READ_CSV_HEADER);
            for (int i = 0; i < results.size(); i++) {
                Scenario scenario = scenarios.get(i);
                Result result = results.get(i);
                String consistency = "consistent_read_summary_one_customer".equals(scenario.name) ? "Strong" : "Eventual";
                out.printf(Locale.US,
                           "%s,Enhanced Queries,%s,%s,%s,%s,%s,PASS,%d,%d,%.2f,%.2f,%.2f,%.2f,0.00,%d,%.2f,0.00,%d,%s,%s,%s,%s,%d,%d,%d,%d%n",
                           csv(envOrDefault("BENCHMARK_RUN_ID", "not-configured")),
                           csv(scenario.name),
                           csv(readableScenarioName(scenario.name)),
                           csv(scenarioCategory(scenario.name)),
                           csv(scenario.description),
                           csv(scenario.ddbOperation),
                           result.expectedRows,
                           result.rows,
                           result.avgMs,
                           (double) result.p50Ms,
                           (double) result.p95Ms,
                           result.readCapacityUnits,
                           result.requestCount,
                           result.totalReadCapacityUnits,
                           result.totalRequestCount,
                           csv(region),
                           csv(envOrDefault("INSTANCE_TYPE", "not-configured")),
                           csv(envOrDefault("DYNAMODB_BILLING_MODE", "not-configured")),
                           consistency,
                           customerCount,
                           ordersPerCustomer,
                           warmup,
                           iterations);
            }
        }
    }

    private static String scenarioCategory(String scenarioId) {
        if (scenarioId.contains("join")) {
            return "Join";
        }
        if (scenarioId.contains("pagination")) {
            return "Pagination";
        }
        if (scenarioId.contains("consistent")) {
            return "Consistency";
        }
        if (scenarioId.contains("scan")) {
            return "Scan and aggregation";
        }
        if (scenarioId.contains("top10") || scenarioId.contains("having") || scenarioId.contains("grouped")) {
            return "Grouped aggregation";
        }
        if (scenarioId.contains("batch")) {
            return "Batch read";
        }
        return "Point read and aggregation";
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private static String readableScenarioName(String scenarioId) {
        StringBuilder result = new StringBuilder();
        for (String word : scenarioId.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
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

    @FunctionalInterface
    private interface ScenarioAction {
        RunOutcome run(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient);
    }

    private static final class RunOutcome {
        final long ms;
        final int rows;
        final Double readCapacityUnits;
        final Long requestCount;

        RunOutcome(long ms, int rows) {
            this(ms, rows, null, null);
        }

        RunOutcome(long ms, int rows, Double readCapacityUnits, Long requestCount) {
            this.ms = ms;
            this.rows = rows;
            this.readCapacityUnits = readCapacityUnits;
            this.requestCount = requestCount;
        }
    }

    private static class Scenario {
        final String name;
        final String description;
        final String ddbOperation;
        final ScenarioAction action;
        final boolean allowsEmptyResult;

        Scenario(String name, String description, String ddbOperation, ScenarioAction action) {
            this(name, description, ddbOperation, action, "having_with_between".equals(name));
        }

        Scenario(String name,
                 String description,
                 String ddbOperation,
                 ScenarioAction action,
                 boolean allowsEmptyResult) {
            this.name = name;
            this.description = description;
            this.ddbOperation = ddbOperation;
            this.action = action;
            this.allowsEmptyResult = allowsEmptyResult;
        }

        static Scenario fromSpec(String name,
                                 String description,
                                 String ddbOperation,
                                 Supplier<QueryExpressionSpec> specSupplier) {
            return new Scenario(name, description, ddbOperation, (client, dynamoDbClient) -> {
                EnhancedQueryLatencyReport[] reportHolder = new EnhancedQueryLatencyReport[1];
                EnhancedQueryResult result = client.enhancedQuery(specSupplier.get(), r -> reportHolder[0] = r);
                int count = 0;
                for (EnhancedQueryRow ignored : result) {
                    count++;
                }
                long ms = reportHolder[0] != null ? reportHolder[0].totalMs() : 0L;
                EnhancedQueryLatencyReport report = reportHolder[0];
                return new RunOutcome(ms, count,
                                      report == null ? null : report.totalRcuConsumed(),
                                      report == null ? null : report.totalDynamoDbRequestCount());
            });
        }
    }

    private static class Result {
        final double avgMs;
        final long p50Ms;
        final long p95Ms;
        final int expectedRows;
        final int rows;
        final Double readCapacityUnits;
        final Long requestCount;
        final double totalReadCapacityUnits;
        final long totalRequestCount;

        Result(double avgMs, long p50Ms, long p95Ms, int expectedRows, int rows,
               Double readCapacityUnits, Long requestCount,
               double totalReadCapacityUnits, long totalRequestCount) {
            this.avgMs = avgMs;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.expectedRows = expectedRows;
            this.rows = rows;
            this.readCapacityUnits = readCapacityUnits;
            this.requestCount = requestCount;
            this.totalReadCapacityUnits = totalReadCapacityUnits;
            this.totalRequestCount = totalRequestCount;
        }
    }

    private static Result runScenario(DynamoDbEnhancedClient enhancedClient,
                                      DynamoDbClient dynamoDbClient,
                                      Scenario scenario,
                                      int warmup,
                                      int iterations) {
        int expectedRows = scenario.action.run(enhancedClient, dynamoDbClient).rows;
        if (expectedRows == 0 && !scenario.allowsEmptyResult) {
            throw new IllegalStateException("Scenario " + scenario.name
                                            + " returned no rows during preflight validation");
        }
        for (int i = 0; i < warmup; i++) {
            scenario.action.run(enhancedClient, dynamoDbClient);
        }
        List<Long> times = new ArrayList<>(iterations);
        int rows = 0;
        double totalReadCapacityUnits = 0.0d;
        long totalRequestCount = 0L;
        for (int i = 0; i < iterations; i++) {
            RunOutcome outcome = scenario.action.run(enhancedClient, dynamoDbClient);
            times.add(outcome.ms);
            if (outcome.rows != expectedRows) {
                throw new IllegalStateException("Scenario " + scenario.name + " returned " + outcome.rows
                                                + " rows, expected " + expectedRows);
            }
            rows = outcome.rows;
            totalReadCapacityUnits += outcome.readCapacityUnits == null ? 0.0d : outcome.readCapacityUnits;
            totalRequestCount += outcome.requestCount == null ? 0L : outcome.requestCount;
        }
        Collections.sort(times);
        long p50 = times.get((int) (iterations * 0.5));
        long p95 = times.get((int) Math.min(Math.ceil(iterations * 0.95) - 1, iterations - 1));
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
        return new Result(avg, p50, p95, expectedRows, rows,
                          totalReadCapacityUnits / iterations,
                          totalRequestCount / iterations,
                          totalReadCapacityUnits, totalRequestCount);
    }
}
