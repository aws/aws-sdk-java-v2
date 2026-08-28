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
package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertNotEquals;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.mapper.dynamodb.pojos.GsiWithAlwaysUpdateTimestamp;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public class GsiAlwaysUpdateTest extends LocalDynamoDBTestBase {

    private static final String TABLE_NAME =
        GsiAlwaysUpdateTest.class.getSimpleName() + "-" + System.currentTimeMillis();

    private DynamoDbClient ddb;
    private DynamoDBTableMapper<GsiWithAlwaysUpdateTimestamp, String, String> mapper;

    @Before
    public void setup() throws InterruptedException {
        ddb = client();
        mapper = new DynamoDBMapper(ddb, DynamoDBMapperConfig.builder()
                .withTableNameOverride(new DynamoDBMapperConfig.TableNameOverride(TABLE_NAME))
                .build()).newTableMapper(GsiWithAlwaysUpdateTimestamp.class);
        // Table setup is built directly against the v2 client rather than via the mapper's
        // createTable(), which is part of the deferred table-admin port. Against DynamoDB Local
        // the table is active immediately, so no waiter is needed. The behavior under test is that
        // an ALWAYS auto-generated timestamp indexed by a GSI is regenerated on every save.
        ProvisionedThroughput throughput = ProvisionedThroughput.builder()
                                                                .readCapacityUnits(5L).writeCapacityUnits(5L).build();
        ddb.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(KeySchemaElement.builder().attributeName("hashKey").keyType(KeyType.HASH).build(),
                           KeySchemaElement.builder().attributeName("rangeKey").keyType(KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("hashKey").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("rangeKey").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("lastModifiedDate")
                                           .attributeType(ScalarAttributeType.N).build())
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName("last-mod-date")
                        .keySchema(KeySchemaElement.builder().attributeName("lastModifiedDate").keyType(KeyType.HASH).build())
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .provisionedThroughput(throughput)
                        .build())
                .provisionedThroughput(throughput)
                .build());
    }

    @After
    public void tearDown() {
        ddb.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
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
