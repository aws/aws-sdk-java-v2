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

package software.amazon.awssdk.benchmark.dynamodb.live;

import java.util.UUID;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.benchmark.dynamodb.DynamoDbBenchmarkSystemSetting;
import software.amazon.awssdk.benchmark.dynamodb.fixture.BenchmarkItem;
import software.amazon.awssdk.benchmark.dynamodb.fixture.DynamoDbBenchmarkFixture;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.Tag;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Shared Tier D live table lifecycle: opt-in gate, client, unique owned table, seed, teardown.
 *
 * <p>Automatic teardown deletes only the exact table created by this trial and only when
 * {@code createdByThisTrial} is true. Tags identify orphaned tables after interrupted runs.
 */
public final class LiveDynamoDbSupport implements SdkAutoCloseable {

    public static final String OWNERSHIP_TAG_KEY = "sdk-java-ddb-perf-benchmark";
    public static final String OWNERSHIP_TAG_VALUE = "owned";
    public static final String PURPOSE_TAG_KEY = "Purpose";
    public static final String PURPOSE_TAG_VALUE = "aws-sdk-java-v2-dynamodb-live-benchmark";

    private static final Logger LOG = Logger.loggerFor(LiveDynamoDbSupport.class);

    private final DynamoDbClient client;
    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbBenchmarkFixture fixture;
    private final String tableName;
    private final Region region;
    private final boolean createdByThisTrial;
    private final LiveRetryObserver retryObserver;
    private final boolean retryObserverEnabled;
    private boolean closed;

    private LiveDynamoDbSupport(DynamoDbClient client,
                                DynamoDbEnhancedClient enhancedClient,
                                DynamoDbBenchmarkFixture fixture,
                                String tableName,
                                Region region,
                                boolean createdByThisTrial,
                                LiveRetryObserver retryObserver,
                                boolean retryObserverEnabled) {
        this.client = client;
        this.enhancedClient = enhancedClient;
        this.fixture = fixture;
        this.tableName = tableName;
        this.region = region;
        this.createdByThisTrial = createdByThisTrial;
        this.retryObserver = retryObserver;
        this.retryObserverEnabled = retryObserverEnabled;
    }

    /**
     * Opens a live support context. The opt-in guard runs before any credential or AWS work.
     *
     * @param operationLabel short label embedded in the unique table name (e.g. {@code get}, {@code typedquery})
     */
    public static LiveDynamoDbSupport open(String operationLabel) {
        LiveBenchmarkGuard.requireLiveOptIn();

        Region region = resolveRegion();
        boolean retryObserve = LiveRetryObserver.isEnabled();
        LiveRetryObserver observer = retryObserve ? new LiveRetryObserver() : null;

        DynamoDbClient client = DynamoDbClient.builder()
                                              .region(region)
                                              .credentialsProvider(DefaultCredentialsProvider.create())
                                              .overrideConfiguration(o -> {
                                                  if (observer != null) {
                                                      o.addExecutionInterceptor(observer);
                                                  }
                                              })
                                              .build();

        String tableName = newUniqueTableName(operationLabel);
        boolean created = false;
        try {
            createOwnedTable(client, tableName);
            created = true;
            client.waiter().waitUntilTableExists(b -> b.tableName(tableName));

            DynamoDbBenchmarkFixture fixture = DynamoDbBenchmarkFixture.create();
            client.putItem(r -> r.tableName(tableName).item(fixture.attributeMap()));

            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(client)
                                                                          .build();

            LOG.info(() -> "region=" + region.id()
                           + " table=" + tableName
                           + " billing=PAY_PER_REQUEST"
                           + " createdByThisTrial=true"
                           + " retryObserver=" + retryObserve);

            return new LiveDynamoDbSupport(client, enhancedClient, fixture, tableName, region, created,
                                           observer, retryObserve);
        } catch (RuntimeException e) {
            if (created) {
                safeDelete(client, tableName);
            }
            client.close();
            throw e;
        }
    }

    public DynamoDbClient client() {
        return client;
    }

    public DynamoDbEnhancedClient enhancedClient() {
        return enhancedClient;
    }

    public DynamoDbTable<BenchmarkItem> typedTable() {
        return enhancedClient.table(tableName, fixture.tableSchema());
    }

    public DynamoDbBenchmarkFixture fixture() {
        return fixture;
    }

    public String tableName() {
        return tableName;
    }

    public Region region() {
        return region;
    }

    public Key partitionKey() {
        return Key.builder()
                  .partitionValue(DynamoDbBenchmarkFixture.PARTITION_KEY_VALUE)
                  .build();
    }

    public LiveRetryObserver retryObserver() {
        return retryObserver;
    }

    public boolean retryObserverEnabled() {
        return retryObserverEnabled;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (retryObserverEnabled && retryObserver != null) {
                LOG.info(() -> "retryObserver: " + retryObserver.summary());
                if (retryObserver.observedRetries()) {
                    LOG.warn(() -> "LIVE BENCHMARK WARNING: retries observed — interpret scores with caution "
                                   + "(hidden retries change latency interpretation). "
                                   + retryObserver.summary());
                }
            }
        } finally {
            try {
                if (createdByThisTrial) {
                    safeDelete(client, tableName);
                    LOG.info(() -> "deleted owned table=" + tableName);
                } else {
                    LOG.info(() -> "skipping delete (createdByThisTrial=false) table=" + tableName);
                }
            } finally {
                client.close();
            }
        }
    }

    private static Region resolveRegion() {
        return DynamoDbBenchmarkSystemSetting.REGION.getStringValue()
                                                    .filter(v -> !v.trim().isEmpty())
                                                    .map(v -> Region.of(v.trim()))
                                                    .orElseGet(LiveDynamoDbSupport::resolveDefaultRegion);
    }

    private static Region resolveDefaultRegion() {
        try {
            return new DefaultAwsRegionProviderChain().getRegion();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Unable to resolve AWS region for live DynamoDB benchmarks. Set "
                + DynamoDbBenchmarkSystemSetting.REGION.environmentVariable()
                + ", -D" + DynamoDbBenchmarkSystemSetting.REGION.property()
                + ", AWS_REGION, or configure a default region in the standard provider chain.",
                e);
        }
    }

    private static String newUniqueTableName(String operationLabel) {
        String safeLabel = operationLabel == null ? "op" : operationLabel.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeLabel.isEmpty()) {
            safeLabel = "op";
        }
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String name = "sdk-java-ddb-perf-" + safeLabel + "-" + suffix;
        if (name.length() > 255) {
            throw new IllegalStateException("Generated table name exceeds DynamoDB limit: " + name);
        }
        return name;
    }

    private static void createOwnedTable(DynamoDbClient client, String tableName) {
        String partitionKeyName = DynamoDbBenchmarkFixture.PARTITION_KEY_NAME;
        try {
            client.createTable(CreateTableRequest.builder()
                                                 .tableName(tableName)
                                                 .billingMode(BillingMode.PAY_PER_REQUEST)
                                                 .attributeDefinitions(AttributeDefinition.builder()
                                                                                          .attributeName(partitionKeyName)
                                                                                          .attributeType(ScalarAttributeType.S)
                                                                                          .build())
                                                 .keySchema(KeySchemaElement.builder()
                                                                            .attributeName(partitionKeyName)
                                                                            .keyType(KeyType.HASH)
                                                                            .build())
                                                 .tags(
                                                     Tag.builder()
                                                        .key(OWNERSHIP_TAG_KEY)
                                                        .value(OWNERSHIP_TAG_VALUE)
                                                        .build(),
                                                     Tag.builder()
                                                        .key(PURPOSE_TAG_KEY)
                                                        .value(PURPOSE_TAG_VALUE)
                                                        .build())
                                                 .build());
        } catch (ResourceInUseException e) {
            throw new IllegalStateException(
                "Refusing to adopt pre-existing table '" + tableName
                + "'. Live benchmarks only use uniquely named tables created by this trial.",
                e);
        }
    }

    private static void safeDelete(DynamoDbClient client, String tableName) {
        try {
            client.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
            client.waiter().waitUntilTableNotExists(b -> b.tableName(tableName));
        } catch (RuntimeException e) {
            LOG.warn(() -> "failed to delete owned table=" + tableName + " : " + e);
        }
    }
}
