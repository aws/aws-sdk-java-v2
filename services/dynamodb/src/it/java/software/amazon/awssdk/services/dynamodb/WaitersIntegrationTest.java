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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import utils.resources.tables.BasicTempTable;
import utils.test.util.DynamoDBTestBase;

public class WaitersIntegrationTest extends DynamoDBTestBase {

    private static final String TABLE_NAME = "java-sdk-waiter-test" + System.currentTimeMillis();
    private static final String HASH_KEY_NAME = BasicTempTable.HASH_KEY_NAME;
    private static DynamoDbAsyncClient dynamoAsync;

    @BeforeClass
    public static void setUp() {
        DynamoDBTestBase.setUpTestBase();

        dynamoAsync = DynamoDbAsyncClient.builder().region(REGION).credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();

        dynamo.createTable(CreateTableRequest.builder().tableName(TABLE_NAME)
                                             .keySchema(KeySchemaElement.builder().keyType(KeyType.HASH)
                                                                        .attributeName(HASH_KEY_NAME)
                                                                        .build())
                                             .attributeDefinitions(AttributeDefinition.builder()
                                                                                      .attributeType(ScalarAttributeType.N)
                                                                                      .attributeName(HASH_KEY_NAME)
                                                                                      .build())
                                             .provisionedThroughput(ProvisionedThroughput.builder()
                                                                                         .readCapacityUnits(5L)
                                                                                         .writeCapacityUnits(5L)
                                                                                         .build())
                                             .build());
    }

    @AfterClass
    public static void cleanUp() {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());

        dynamo.close();
        dynamoAsync.close();
    }

    @Test
    public void checkTableExist_withSyncWaiter() {
        DynamoDbWaiter syncWaiter = dynamo.waiter();
        WaiterResponse<DescribeTableResponse> response = syncWaiter.waitUntilTableExists(
            DescribeTableRequest.builder().tableName(TABLE_NAME).build());

        assertThat(response.attemptsExecuted()).isGreaterThanOrEqualTo(1);
        assertThat(response.matched().response().get().table().tableName()).isEqualTo(TABLE_NAME);
    }

    @Test
    public void checkTableExist_withAsyncWaiter() throws ExecutionException, InterruptedException {
        DynamoDbAsyncWaiter asyncWaiter = dynamoAsync.waiter();
        CompletableFuture<WaiterResponse<DescribeTableResponse>> responseFuture = asyncWaiter.waitUntilTableExists(
            DescribeTableRequest.builder().tableName(TABLE_NAME).build());

        responseFuture.join();

        assertThat(responseFuture.get().attemptsExecuted()).isGreaterThanOrEqualTo(1);
        assertThat(responseFuture.get().matched().response().get().table().tableName()).isEqualTo(TABLE_NAME);
    }

}