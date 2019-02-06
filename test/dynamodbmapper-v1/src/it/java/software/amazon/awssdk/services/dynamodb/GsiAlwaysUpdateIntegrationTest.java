/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.junit.Assert.assertNotEquals;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTableMapper;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.services.dynamodb.pojos.GsiWithAlwaysUpdateTimestamp;
import software.amazon.awssdk.testutils.Waiter;

public class GsiAlwaysUpdateIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String TABLE_NAME =
            GsiAlwaysUpdateIntegrationTest.class.getSimpleName() + "-" + System.currentTimeMillis();

    private DynamoDbClient ddb;
    private DynamoDbTableMapper<GsiWithAlwaysUpdateTimestamp, String, String> mapper;

    @Before
    public void setup() {
        ddb = DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();
        mapper = new DynamoDbMapper(ddb, DynamoDbMapperConfig.builder()
                .withTableNameOverride(new DynamoDbMapperConfig.TableNameOverride(TABLE_NAME))
                .build()).newTableMapper(GsiWithAlwaysUpdateTimestamp.class);
        mapper.createTable(ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build());
        Waiter.run(() -> ddb.describeTable(r -> r.tableName(TABLE_NAME)))
              .ignoringException(ResourceNotFoundException.class)
              .until(r -> r.table().tableStatus() == TableStatus.ACTIVE)
              .orFail();
    }

    @After
    public void tearDown() {
        mapper.deleteTableIfExists();
        Waiter.run(() -> ddb.describeTable(r -> r.tableName(TABLE_NAME)))
              .untilException(ResourceNotFoundException.class)
              .orFail();
    }

    @Test
    public void pojoWithAlwaysGenerateGsi_SavesCorrectly() throws InterruptedException {
        final String hashKey = UUID.randomUUID().toString();
        final String rangeKey = UUID.randomUUID().toString();

        mapper.save(new GsiWithAlwaysUpdateTimestamp()
                .setHashKey(hashKey)
                .setRangeKey(rangeKey));
        final GsiWithAlwaysUpdateTimestamp created = mapper.load(hashKey, rangeKey);
        // Have to store it since the mapper will auto update any generated values in the saved object.
        Long createdDate = created.getLastModifiedDate();
        // Need to wait a bit for the timestamps to actually be different
        Thread.sleep(1000);
        mapper.save(created);
        final GsiWithAlwaysUpdateTimestamp updated = mapper.load(hashKey, rangeKey);
        assertNotEquals(createdDate, updated.getLastModifiedDate());
    }
}
