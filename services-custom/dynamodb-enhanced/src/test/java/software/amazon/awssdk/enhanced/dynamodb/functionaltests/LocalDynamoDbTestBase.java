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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

public class LocalDynamoDbTestBase {
    private static final LocalDynamoDb localDynamoDb = new LocalDynamoDb();
    private static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
        ProvisionedThroughput.builder()
                             .readCapacityUnits(50L)
                             .writeCapacityUnits(50L)
                             .build();

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private String uniqueTableSuffix = UUID.randomUUID().toString();

    @BeforeClass
    public static void initializeLocalDynamoDb() {
        if (STARTED.compareAndSet(false, true)) {
            localDynamoDb.start();
        }
    }

    @AfterClass
    public static void stopLocalDynamoDb() {
        if (STARTED.compareAndSet(true, false)) {
            localDynamoDb.stop();
        }
    }

    protected static LocalDynamoDb localDynamoDb() {
        return localDynamoDb;
    }
    
    protected String getConcreteTableName(String logicalTableName) {
        return logicalTableName + "_" + uniqueTableSuffix;

    }

    protected ProvisionedThroughput getDefaultProvisionedThroughput() {
        return DEFAULT_PROVISIONED_THROUGHPUT;
    }

    private static final Path METRICS_FILE = Paths.get("target", "surefire-reports", "query-metrics.txt");

    /**
     * Appends a structured metric line for the script to read instead of parsing Surefire XML times or stdout logs.
     * Format per line: {@code ClassName.testName <queryMs> <rowCount>}
     */
    protected static synchronized void writeQueryMetric(String label, long queryMs, int rowCount) {
        try {
            Files.createDirectories(METRICS_FILE.getParent());
            Files.write(METRICS_FILE,
                        Collections.singletonList(label + " " + queryMs + " " + rowCount),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write query metric for " + label + ": " + e.getMessage());
        }
    }
}
