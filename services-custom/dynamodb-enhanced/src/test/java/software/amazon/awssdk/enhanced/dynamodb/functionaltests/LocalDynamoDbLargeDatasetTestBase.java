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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

/**
 * Test base for enhanced-query tests that use the large dataset. Uses the same in-process
 * {@link LocalDynamoDb} as all other functionaltests (from {@link LocalDynamoDbTestBase}).
 * Seeds the Customers and Orders tables once in {@code @BeforeClass} via
 * {@link LargeDatasetInitializer#initializeCustomersAndOrdersDataset}. Does not stop the DB in
 * {@code @AfterClass} so subsequent test classes in the same JVM reuse the same data.
 * <p>
 * Dataset size is {@value #SEED_CUSTOMER_COUNT} customers and {@value #SEED_ORDERS_PER_CUSTOMER}
 * orders per customer.
 */
public abstract class LocalDynamoDbLargeDatasetTestBase extends LocalDynamoDbTestBase {

    /** Number of customers seeded (c1 .. cN). */
    public static final int SEED_CUSTOMER_COUNT = 1000;
    /** Number of orders per customer (cK-o1 .. cK-oM). Amount for cK-oO is 10*K + O. */
    public static final int SEED_ORDERS_PER_CUSTOMER = 1000;
    /** For c1 with joined condition amount >= 50: orders 40..1000, count = 961. */
    public static final long EXPECTED_ORDER_COUNT_C1_AMOUNT_GE_50 = 961;
    /** For c1 amount >= 50: min amount = 10 + 40 = 50. */
    public static final int EXPECTED_MIN_AMOUNT_C1_GE_50 = 50;
    /** For c1 amount >= 50: max amount = 10 + 1000 = 1010. */
    public static final int EXPECTED_MAX_AMOUNT_C1_GE_50 = 1010;
    /** For c1 amount >= 50: sum(50..1010) = 961*10 + sum(40..1000) = 9610 + 961*520 = 509330. */
    public static final long EXPECTED_TOTAL_AMOUNT_C1_GE_50 = 509330L;
    /** For c1 amount >= 50: avg = 509330 / 961. */
    public static final double EXPECTED_AVG_AMOUNT_C1_GE_50 = (double) EXPECTED_TOTAL_AMOUNT_C1_GE_50 / EXPECTED_ORDER_COUNT_C1_AMOUNT_GE_50;

    /** For c1 without filter: all 1000 orders. */
    public static final long EXPECTED_ORDER_COUNT_C1 = SEED_ORDERS_PER_CUSTOMER;
    /** For c1 without filter: min amount = 10*1 + 1 = 11. */
    public static final int EXPECTED_MIN_AMOUNT_C1 = 11;
    /** For c1 without filter: max amount = 10*1 + 1000 = 1010. */
    public static final int EXPECTED_MAX_AMOUNT_C1 = 1010;
    /** For c1 without filter: sum(11..1010) = 1000 * (11+1010)/2 = 510500. */
    public static final long EXPECTED_TOTAL_AMOUNT_C1 = 510500L;
    /** For c1 without filter: avg = 510500 / 1000 = 510.5. */
    public static final double EXPECTED_AVG_AMOUNT_C1 = 510.5;

    /** Number of EU customers (odd IDs: c1, c3, c5 ... c999). */
    public static final int EXPECTED_EU_CUSTOMER_COUNT = 500;
    /** Number of NA customers (even IDs: c2, c4, c6 ... c1000). */
    public static final int EXPECTED_NA_CUSTOMER_COUNT = 500;

    private static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
        ProvisionedThroughput.builder()
                             .readCapacityUnits(50L)
                             .writeCapacityUnits(50L)
                             .build();

    private static DynamoDbClient dynamoDbClient;
    private static DynamoDbAsyncClient dynamoDbAsyncClient;
    private static boolean datasetSeeded;

    @BeforeClass
    public static void ensureLocalDynamoDbAndSeed() {
        initializeLocalDynamoDb();
        if (datasetSeeded) {
            return;
        }
        dynamoDbClient = localDynamoDb().createClient();
        dynamoDbAsyncClient = localDynamoDb().createAsyncClient();
        LargeDatasetInitializer.initializeCustomersAndOrdersDataset(
            dynamoDbClient,
            LargeDatasetInitializer.LARGE_CUSTOMERS_TABLE,
            LargeDatasetInitializer.LARGE_ORDERS_TABLE,
            SEED_CUSTOMER_COUNT,
            SEED_ORDERS_PER_CUSTOMER,
            DEFAULT_PROVISIONED_THROUGHPUT);
        datasetSeeded = true;
    }

    @AfterClass
    public static void stopLocalDynamoDb() {
        // Do not stop so other EnhancedQuery test classes in the same JVM reuse the DB and skip re-seeding.
    }

    protected static DynamoDbClient getDynamoDbClient() {
        return dynamoDbClient;
    }

    protected static DynamoDbAsyncClient getDynamoDbAsyncClient() {
        return dynamoDbAsyncClient;
    }

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

    public static <T> List<T> drainPublisher(SdkPublisher<T> publisher, int expectedNumberOfResults) {
        BufferingSubscriber<T> subscriber = new BufferingSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.waitForCompletion(5000L);

        assertThat(subscriber.isCompleted(), is(true));
        assertThat(subscriber.bufferedError(), is(nullValue()));
        assertThat(subscriber.bufferedItems().size(), is(expectedNumberOfResults));

        return subscriber.bufferedItems();
    }

    public static <T> List<T> drainPublisherToError(SdkPublisher<T> publisher,
                                                    int expectedNumberOfResults,
                                                    Class<? extends Throwable> expectedError) {
        BufferingSubscriber<T> subscriber = new BufferingSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.waitForCompletion(1000L);

        assertThat(subscriber.isCompleted(), is(false));
        assertThat(subscriber.bufferedError(), instanceOf(expectedError));
        assertThat(subscriber.bufferedItems().size(), is(expectedNumberOfResults));

        return subscriber.bufferedItems();
    }
}
