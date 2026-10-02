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

package software.amazon.awssdk.enhanced.dynamodb;

import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.enhanced.dynamodb.model.CompositeKeyRecord;

public class QueryGSICompositeKeysBeanSchemaIntegrationTest extends QueryGSICompositeKeysIntegrationTestBase {

    private static final String TABLE_NAME = createTestTableName();

    @BeforeAll
    public static void setup() {
        dynamoDbClient = createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, TableSchema.fromClass(CompositeKeyRecord.class));
        mappedTable.createTable();
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        insertRecords();
        waitForGsiConsistency();
    }
}
