/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import utils.resources.RequiredResources;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.RequiredResources.ResourceRetentionPolicy;
import utils.resources.ResourceCentricBlockJUnit4ClassRunner;
import utils.resources.tables.BasicTempTableWithLowThroughput;
import utils.test.util.DynamoDBTestBase;

/**
 * DynamoDB integration tests around ProvisionedThroughput/throttling errors.
 */
@RunWith(ResourceCentricBlockJUnit4ClassRunner.class)
@RequiredResources({
                            @RequiredResource(resource = BasicTempTableWithLowThroughput.class,
                                              creationPolicy = ResourceCreationPolicy.ALWAYS_RECREATE,
                                              retentionPolicy = ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS)
                    })
public class ProvisionedThroughputThrottlingIntegrationTest extends DynamoDBTestBase {

    private static final String tableName = BasicTempTableWithLowThroughput.TEMP_TABLE_NAME;
    private static final String HASH_KEY_NAME = BasicTempTableWithLowThroughput.HASH_KEY_NAME;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBTestBase.setUpTestBase();
    }

    /**
     * Tests that throttling errors and delayed retries are automatically
     * handled for users.
     *
     * We trigger ProvisionedThroughputExceededExceptions here because of the
     * low throughput on our test table, but users shouldn't see any problems
     * because of the backoff and retry strategy.
     */
    @Test
    public void testProvisionedThroughputExceededRetryHandling() throws Exception {
        for (int i = 0; i < 20; i++) {
            Map<String, AttributeValue> item = Collections
                    .singletonMap(HASH_KEY_NAME, AttributeValue.builder().s(UUID.randomUUID().toString()).build());
            dynamo.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
        }
    }

}
