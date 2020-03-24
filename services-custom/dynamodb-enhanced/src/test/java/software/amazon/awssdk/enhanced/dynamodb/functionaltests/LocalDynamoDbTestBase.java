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

import java.util.UUID;
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

    private String uniqueTableSuffix = UUID.randomUUID().toString();

    @BeforeClass
    public static void initializeLocalDynamoDb() {
        localDynamoDb.start();
    }

    @AfterClass
    public static void stopLocalDynamoDb() {
        localDynamoDb.stop();
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
}
