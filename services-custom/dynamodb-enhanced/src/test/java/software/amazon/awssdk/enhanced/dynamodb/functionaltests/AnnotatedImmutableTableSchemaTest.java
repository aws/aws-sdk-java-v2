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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.ImmutableFakeItem;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

public class AnnotatedImmutableTableSchemaTest extends LocalDynamoDbSyncTestBase {
    private static final String TABLE_NAME = "table-name";

    private final DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                                .dynamoDbClient(getDynamoDbClient())
                                                                                .build();

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName(TABLE_NAME))
                                                          .build());
    }

    @Test
    public void simpleItem_putAndGet() {
        TableSchema<ImmutableFakeItem> tableSchema =
            TableSchema.fromClass(ImmutableFakeItem.class);

        DynamoDbTable<ImmutableFakeItem> mappedTable =
            enhancedClient.table(getConcreteTableName(TABLE_NAME), tableSchema);

        mappedTable.createTable(r -> r.provisionedThroughput(ProvisionedThroughput.builder()
                                                                                  .readCapacityUnits(5L)
                                                                                  .writeCapacityUnits(5L)
                                                                                  .build()));
        ImmutableFakeItem immutableFakeItem = ImmutableFakeItem.builder()
                                                               .id("id123")
                                                               .attribute("test-value")
                                                               .build();

        mappedTable.putItem(immutableFakeItem);
        ImmutableFakeItem readItem = mappedTable.getItem(immutableFakeItem);
        assertThat(readItem).isEqualTo(immutableFakeItem);
    }
}
