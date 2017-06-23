/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.document;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

public class TableRecycleIntegrationTest extends IntegrationTestBase {
    @Test
    public void placeholder() {
    }

    // This test can be interrupted and then re-run later on and still work
    //    @Test
    public void testCreateDeleteTable() throws InterruptedException {
        final String tableName = "TableRecycleIntegrationTest-testCreateDeleteTable";
        final String hashKeyName = "mykey";

        for (int i = 0; i < 2; i++) {
            Table table = dynamo.getTable(tableName);
            TableDescription desc = table.waitForActiveOrDelete();
            System.err.println(i + ") Started with table: " + desc);
            if (desc == null) {
                // table not exist; let's create it
                table = dynamo.createTable(newCreateTableRequest(tableName, hashKeyName));
                desc = table.waitForActive();
                System.err.println("Created table: " + desc);
            } else {
                System.err.println("Existing table :" + desc);
            }
            // Table must be active at this stage, let's delete it
            table.delete();
            table.waitForDelete();  // blocks till the table is deleted
            Assert.assertNull(table.waitForActiveOrDelete());
        }
    }

    private CreateTableRequest newCreateTableRequest(String tableName, String hashKeyName) {
        return CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(KeySchemaElement.builder().attributeName(hashKeyName).keyType(KeyType.HASH).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(hashKeyName).attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
                .build();
    }
}
