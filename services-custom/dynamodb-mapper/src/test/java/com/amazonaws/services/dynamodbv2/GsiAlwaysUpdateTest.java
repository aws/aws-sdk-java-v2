/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.dynamodbv2;

import static org.junit.Assert.assertNotEquals;

import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTableMapper;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.pojos.GsiWithAlwaysUpdateTimestamp;
import com.amazonaws.waiters.WaiterParameters;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GsiAlwaysUpdateTest extends LocalDynamoDBTestBase {

    private static final String TABLE_NAME =
        GsiAlwaysUpdateTest.class.getSimpleName() + "-" + System.currentTimeMillis();

    private AmazonDynamoDB ddb;
    private DynamoDBTableMapper<GsiWithAlwaysUpdateTimestamp, String, String> mapper;

    @Before
    public void setup() throws InterruptedException {
        ddb = client();
        mapper = new DynamoDBMapper(ddb, DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(TABLE_NAME))
                .build()).newTableMapper(GsiWithAlwaysUpdateTimestamp.class);
        mapper.createTable(new ProvisionedThroughput(5L, 5L));
        ddb.waiters().tableExists()
                .run(new WaiterParameters<DescribeTableRequest>(new DescribeTableRequest(TABLE_NAME)));
    }

    @After
    public void tearDown() {
        mapper.deleteTableIfExists();
        ddb.waiters().tableNotExists()
                .run(new WaiterParameters<DescribeTableRequest>(new DescribeTableRequest(TABLE_NAME)));
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
