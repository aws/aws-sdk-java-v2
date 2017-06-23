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

package software.amazon.awssdk.services.dynamodb;

import java.util.HashMap;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.util.TableUtils;
import software.amazon.awssdk.services.dynamodb.util.TableUtils.TableNeverTransitionedToStateException;
import software.amazon.awssdk.test.AwsIntegrationTestBase;

public class GzipConfigurationIntegrationTest extends AwsIntegrationTestBase {

    private static final String TABLE_NAME = "test-gzip-" + System.currentTimeMillis();
    private static final String KEY_NAME = "key";
    private static final String VALUE_NAME = "value";
    private static final int ITEMS_COUNT = 500;
    private static DynamoDBClient dynamo;

    @BeforeClass
    public static void setup() throws TableNeverTransitionedToStateException,
            InterruptedException {
        dynamo = DynamoDBClient.builder()
                               .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                               .overrideConfiguration(ClientOverrideConfiguration.builder().gzipEnabled(true).build())
                               .build();
        createTable();
        // For this integration test, if the payload is not big enough, the service will not compress the data.
        putItems(ITEMS_COUNT);
    }

    @AfterClass
    public static void tearDown() {
        dynamo.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
    }

    private static void createTable() throws TableNeverTransitionedToStateException, InterruptedException {
        dynamo.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                            .attributeName(KEY_NAME)
                            .attributeType(ScalarAttributeType.S)
                            .build())
                .keySchema(
                        KeySchemaElement.builder()
                            .keyType(KeyType.HASH)
                            .attributeName(KEY_NAME)
                            .build())
                .provisionedThroughput(
                        ProvisionedThroughput.builder().readCapacityUnits(100L).writeCapacityUnits(100L).build())
                .build());

        TableUtils.waitUntilActive(dynamo, TABLE_NAME);
    }

    @SuppressWarnings("serial")
    private static void putItems(int count) {
        for (int i = 0; i < count; ++i) {
            dynamo.putItem(PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(new HashMap<String, AttributeValue>() {
                        {
                            put(KEY_NAME, AttributeValue.builder()
                                    .s(UUID.randomUUID().toString()).build());
                            put(VALUE_NAME, AttributeValue.builder()
                                    .s(UUID.randomUUID().toString()).build());
                        }
                    })
                    .build());
        }
    }

    @Test
    public void gzipConfigurationIntegrationTest() throws TableNeverTransitionedToStateException,
            InterruptedException {
        int count = dynamo.scan(ScanRequest.builder().tableName(TABLE_NAME).build())
                .count();
        Assert.assertEquals(ITEMS_COUNT, count);
    }

}
