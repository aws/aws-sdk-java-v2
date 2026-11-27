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

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

/**
 * Idempotent utility to create and seed the Customers and Orders tables used by enhanced query integration tests. Intended to be
 * run from {@code @BeforeClass} / {@code @BeforeAll} so that a large dataset exists before tests run.
 * <p>
 * <b>Idempotency:</b> Safe to call multiple times. If the tables already exist, they are not
 * recreated. If item counts already meet or exceed the requested {@code customerCount} and
 * {@code customerCount * ordersPerCustomer}, seeding is skipped; otherwise only the missing items are written (by overwriting
 * items with fixed IDs so that repeated runs do not duplicate).
 * <p>
 * <b>Table layout:</b> Customers table has partition key {@code customerId} (String). Orders
 * table has partition key {@code customerId} (String) and sort key {@code orderId} (String), plus an {@code amount} (Integer)
 * attribute. This layout allows Query on Orders by customerId.
 * <p>
 * <b>Large-dataset tests:</b> The enhanced-query test classes use {@link LocalDynamoDbLargeDatasetTestBase}, which
 * uses the same in-process {@link LocalDynamoDb} as other functionaltests and calls {@link #initializeCustomersAndOrdersDataset} once in {@code @BeforeClass}.
 * No external process or environment variables are required. {@link #main(String[])} is optional and runs an in-memory seed for
 * quick verification of the initializer; it is not required for running the tests.
 */
public final class LargeDatasetInitializer {

    /**
     * Table name used for the large-dataset Customers table (seed and join/aggregation tests).
     */
    public static final String LARGE_CUSTOMERS_TABLE = "customers_large";
    /**
     * Table name used for the large-dataset Orders table (seed and join/aggregation tests).
     */
    public static final String LARGE_ORDERS_TABLE = "orders_large";

    /**
     * Default customer count for EC2 benchmarks (10,000 customers).
     */
    public static final int DEFAULT_CUSTOMER_COUNT = 10_000;

    /**
     * Default orders per customer for EC2 benchmarks (10,000 orders per customer = 100M total orders).
     */
    public static final int DEFAULT_ORDERS_PER_CUSTOMER = 10_000;

    /**
     * DynamoDB BatchWriteItem limit per request.
     */
    private static final int BATCH_SIZE = 25;

    /**
     * Number of batches to accumulate before submitting in parallel. Controls memory usage for large datasets.
     */
    private static final int PARALLEL_CHUNK_SIZE = 200;

    /**
     * Maximum retries for throttled BatchWriteItem requests.
     */
    private static final int MAX_RETRIES = 5;

    private static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
        ProvisionedThroughput.builder()
                             .readCapacityUnits(50L)
                             .writeCapacityUnits(50L)
                             .build();

    private LargeDatasetInitializer() {
    }

    /**
     * Ensures the Customers and Orders tables exist and contain at least {@code customerCount} customers and
     * {@code customerCount * ordersPerCustomer} orders. Creates the tables if they do not exist (using the given provisioned
     * throughput), then seeds data until counts are met. Safe to call multiple times; existing tables or sufficient counts cause
     * creation or seeding to be skipped.
     *
     * @param dynamoDbClient        low-level DynamoDB client (e.g. from
     *                              {@link LocalDynamoDbLargeDatasetTestBase#getDynamoDbClient()})
     * @param customersTableName    physical name of the Customers table
     * @param ordersTableName       physical name of the Orders table
     * @param customerCount         desired number of customer items
     * @param ordersPerCustomer     desired number of order items per customer (total orders = customerCount * ordersPerCustomer)
     * @param provisionedThroughput throughput used when creating tables; ignored if tables already exist
     */
    public static void initializeCustomersAndOrdersDataset(
        DynamoDbClient dynamoDbClient,
        String customersTableName,
        String ordersTableName,
        int customerCount,
        int ordersPerCustomer,
        ProvisionedThroughput provisionedThroughput
    ) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        DynamoDbTable<CustomerPojo> customersTable = enhancedClient.table(customersTableName, CUSTOMER_SCHEMA);
        DynamoDbTable<OrderPojo> ordersTable = enhancedClient.table(ordersTableName, ORDER_SCHEMA);

        ensureTableExists(customersTable, provisionedThroughput);
        ensureTableExists(ordersTable, provisionedThroughput);

        // Idempotency: skip seeding if tables already have at least the requested counts.
        int existingCustomers = dynamoDbClient.scan(
            ScanRequest.builder().tableName(customersTableName).select(Select.COUNT).build()).count();
        if (existingCustomers >= customerCount) {
            int existingOrders = dynamoDbClient.scan(
                ScanRequest.builder().tableName(ordersTableName).select(Select.COUNT).build()).count();
            if (existingOrders >= (long) customerCount * ordersPerCustomer) {
                System.out.println("LargeDatasetInitializer: skipping seed (customers=" + existingCustomers
                    + ", orders=" + existingOrders + " already meet or exceed requested counts).");
                return;
            }
        }

        long start = System.nanoTime();

        seedCustomers(enhancedClient, customersTable, customerCount);
        seedOrders(enhancedClient, ordersTable, customerCount, ordersPerCustomer);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Insertion of data took : " + elapsedMs / 1_000 + " seconds");
    }

    /**
     * Entry point for seeding. When run with {@code USE_LOCAL_DYNAMODB=true}, starts an in-process DynamoDB Local and seeds
     * into memory (useful for quick verification). Otherwise, seeds against real DynamoDB tables.
     * <p>
     * Environment variables (override CLI args):
     * <ul>
     *   <li>{@code CUSTOMER_COUNT} – number of customers to seed (default: 10000)</li>
     *   <li>{@code ORDERS_PER_CUSTOMER} – orders per customer (default: 10000)</li>
     *   <li>{@code CUSTOMERS_TABLE} – table name (default: customers_large)</li>
     *   <li>{@code ORDERS_TABLE} – table name (default: orders_large)</li>
     *   <li>{@code USE_LOCAL_DYNAMODB} – if "true", use in-process DynamoDB Local</li>
     *   <li>{@code AWS_REGION} – region for real DynamoDB (default: SDK default)</li>
     * </ul>
     * <p>
     * CLI arguments (lower priority): [customersTableName ordersTableName customerCount ordersPerCustomer].
     *
     * @param args optional: customersTableName, ordersTableName, customerCount, ordersPerCustomer
     */
    public static void main(String[] args) {
        String customersTableName = envOrDefault("CUSTOMERS_TABLE",
            args.length >= 1 ? args[0] : LARGE_CUSTOMERS_TABLE);
        String ordersTableName = envOrDefault("ORDERS_TABLE",
            args.length >= 2 ? args[1] : LARGE_ORDERS_TABLE);
        int customerCount = parseIntEnv("CUSTOMER_COUNT",
            args.length >= 3 ? Integer.parseInt(args[2]) : DEFAULT_CUSTOMER_COUNT);
        int ordersPerCustomer = parseIntEnv("ORDERS_PER_CUSTOMER",
            args.length >= 4 ? Integer.parseInt(args[3]) : DEFAULT_ORDERS_PER_CUSTOMER);

        boolean useLocal = "true".equalsIgnoreCase(System.getenv("USE_LOCAL_DYNAMODB"));

        long totalItems = (long) customerCount + (long) customerCount * ordersPerCustomer;
        System.out.printf("LargeDatasetInitializer: target %,d customers x %,d orders = %,d total items%n",
                          customerCount, ordersPerCustomer, totalItems);
        System.out.println("Mode: " + (useLocal ? "DynamoDB Local (in-process)" : "Real DynamoDB"));

        if (useLocal) {
            LocalDynamoDb local = new LocalDynamoDb();
            local.start();
            try {
                try (DynamoDbClient client = local.createClient()) {
                    initializeCustomersAndOrdersDataset(
                        client, customersTableName, ordersTableName,
                        customerCount, ordersPerCustomer, DEFAULT_PROVISIONED_THROUGHPUT);
                }
            } finally {
                local.stop();
            }
        } else {
            String regionStr = System.getenv("AWS_REGION");
            DynamoDbClient client;
            if (regionStr != null && !regionStr.isEmpty()) {
                client = DynamoDbClient.builder()
                    .region(software.amazon.awssdk.regions.Region.of(regionStr)).build();
            } else {
                client = DynamoDbClient.create();
            }
            try {
                initializeCustomersAndOrdersDataset(
                    client, customersTableName, ordersTableName,
                    customerCount, ordersPerCustomer, DEFAULT_PROVISIONED_THROUGHPUT);
            } finally {
                client.close();
            }
        }
    }

    /**
     * Creates the table if it does not exist. If the table already exists (ResourceInUseException), the exception is ignored so
     * that the call is idempotent.
     */
    private static void ensureTableExists(DynamoDbTable<?> table, ProvisionedThroughput throughput) {
        try {
            table.createTable(r -> r.provisionedThroughput(throughput));
        } catch (ResourceInUseException e) {
            // Table already exists; idempotent.
        }
    }

    /**
     * Writes customer items with IDs c1, c2, ... up to customerCount. Overwrites if already present. Builds batches of 25 and
     * runs BatchWriteItem in parallel (chunked for large counts).
     */
    private static void seedCustomers(DynamoDbEnhancedClient enhancedClient,
                                      DynamoDbTable<CustomerPojo> table,
                                      int customerCount) {
        System.out.printf("  Seeding %,d customers...%n", customerCount);
        AtomicLong written = new AtomicLong(0);
        List<BatchWriteItemEnhancedRequest> chunk = new ArrayList<>(PARALLEL_CHUNK_SIZE);
        for (int start = 1; start <= customerCount; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, customerCount + 1);
            WriteBatch.Builder<CustomerPojo> batchBuilder =
                WriteBatch.builder(CustomerPojo.class).mappedTableResource(table);
            for (int i = start; i < end; i++) {
                CustomerPojo c = new CustomerPojo();
                c.setCustomerId("c" + i);
                c.setName("Customer" + i);
                c.setRegion(i % 2 == 1 ? "EU" : "NA");
                batchBuilder.addPutItem(r -> r.item(c));
            }
            chunk.add(BatchWriteItemEnhancedRequest.builder().writeBatches(batchBuilder.build()).build());
            if (chunk.size() >= PARALLEL_CHUNK_SIZE) {
                submitChunkWithRetry(enhancedClient, chunk, written);
                chunk = new ArrayList<>(PARALLEL_CHUNK_SIZE);
            }
        }
        if (!chunk.isEmpty()) {
            submitChunkWithRetry(enhancedClient, chunk, written);
        }
        System.out.printf("  Customers seeding complete: %,d items written.%n", written.get());
    }

    /**
     * Writes order items for each customer: c1-o1, c1-o2, ... so that each customer has ordersPerCustomer orders. Overwrites if
     * already present. Processes in chunks of {@link #PARALLEL_CHUNK_SIZE} batches to avoid OOM on large datasets (e.g. 100M items).
     * Logs progress every 100,000 items.
     */
    private static void seedOrders(DynamoDbEnhancedClient enhancedClient,
                                   DynamoDbTable<OrderPojo> table,
                                   int customerCount,
                                   int ordersPerCustomer) {
        long totalOrders = (long) customerCount * ordersPerCustomer;
        AtomicLong written = new AtomicLong(0);
        long lastLogAt = 0;
        long logInterval = 100_000;

        List<BatchWriteItemEnhancedRequest> chunk = new ArrayList<>(PARALLEL_CHUNK_SIZE);
        WriteBatch.Builder<OrderPojo> batchBuilder = WriteBatch.builder(OrderPojo.class).mappedTableResource(table);
        int inBatch = 0;

        for (int c = 1; c <= customerCount; c++) {
            for (int o = 1; o <= ordersPerCustomer; o++) {
                OrderPojo ord = new OrderPojo();
                ord.setCustomerId("c" + c);
                ord.setOrderId("c" + c + "-o" + o);
                ord.setAmount(10 * c + o);
                batchBuilder.addPutItem(r -> r.item(ord));
                inBatch++;
                if (inBatch >= BATCH_SIZE) {
                    chunk.add(BatchWriteItemEnhancedRequest.builder().writeBatches(batchBuilder.build()).build());
                    batchBuilder = WriteBatch.builder(OrderPojo.class).mappedTableResource(table);
                    inBatch = 0;

                    if (chunk.size() >= PARALLEL_CHUNK_SIZE) {
                        submitChunkWithRetry(enhancedClient, chunk, written);
                        chunk = new ArrayList<>(PARALLEL_CHUNK_SIZE);
                        long currentWritten = written.get();
                        if (currentWritten - lastLogAt >= logInterval) {
                            System.out.printf("  Orders progress: %,d / %,d (%.1f%%)%n",
                                              currentWritten, totalOrders, 100.0 * currentWritten / totalOrders);
                            lastLogAt = currentWritten;
                        }
                    }
                }
            }
        }
        if (inBatch > 0) {
            chunk.add(BatchWriteItemEnhancedRequest.builder().writeBatches(batchBuilder.build()).build());
        }
        if (!chunk.isEmpty()) {
            submitChunkWithRetry(enhancedClient, chunk, written);
        }
        System.out.printf("  Orders seeding complete: %,d items written.%n", written.get());
    }

    /**
     * Submits a chunk of BatchWriteItem requests in parallel with exponential backoff retry on throttling.
     */
    private static void submitChunkWithRetry(DynamoDbEnhancedClient enhancedClient,
                                             List<BatchWriteItemEnhancedRequest> chunk,
                                             AtomicLong writtenCounter) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                final int batchSize = BATCH_SIZE;
                chunk.parallelStream().forEach(req -> {
                    enhancedClient.batchWriteItem(req);
                    writtenCounter.addAndGet(batchSize);
                });
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES && isThrottlingException(e)) {
                    long sleepMs = (long) Math.pow(2, attempt) * 100;
                    System.out.printf("  Throttled, retrying in %d ms (attempt %d/%d)...%n",
                                      sleepMs, attempt + 1, MAX_RETRIES);
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                } else {
                    throw new RuntimeException("Failed to write batch after " + (attempt + 1) + " attempts", e);
                }
            }
        }
    }

    private static boolean isThrottlingException(Exception e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("ProvisionedThroughputExceeded") || msg.contains("ThrottlingException"))) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMsg = cause.getMessage();
            return causeMsg != null
                   && (causeMsg.contains("ProvisionedThroughputExceeded") || causeMsg.contains("ThrottlingException"));
        }
        return false;
    }

    private static String envOrDefault(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : defaultValue;
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

    /**
     * Simple POJO for Customers table: customerId (PK), name, region.
     */
    private static class CustomerPojo {
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

    /**
     * Simple POJO for Orders table: customerId (PK), orderId (SK), amount.
     */
    private static class OrderPojo {
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

    private static final TableSchema<CustomerPojo> CUSTOMER_SCHEMA =
        StaticTableSchema.builder(CustomerPojo.class)
                         .newItemSupplier(CustomerPojo::new)
                         .addAttribute(String.class,
                                       a -> a.name("customerId").getter(CustomerPojo::getCustomerId).setter(CustomerPojo::setCustomerId).tags(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("name").getter(CustomerPojo::getName).setter(CustomerPojo::setName))
                         .addAttribute(String.class,
                                       a -> a.name("region").getter(CustomerPojo::getRegion).setter(CustomerPojo::setRegion))
                         .build();

    private static final TableSchema<OrderPojo> ORDER_SCHEMA =
        StaticTableSchema.builder(OrderPojo.class)
                         .newItemSupplier(OrderPojo::new)
                         .addAttribute(String.class,
                                       a -> a.name("customerId").getter(OrderPojo::getCustomerId).setter(OrderPojo::setCustomerId).tags(primaryPartitionKey()))
                         .addAttribute(String.class,
                                       a -> a.name("orderId").getter(OrderPojo::getOrderId).setter(OrderPojo::setOrderId).tags(primarySortKey()))
                         .addAttribute(Integer.class,
                                       a -> a.name("amount").getter(OrderPojo::getAmount).setter(OrderPojo::setAmount))
                         .build();
}
