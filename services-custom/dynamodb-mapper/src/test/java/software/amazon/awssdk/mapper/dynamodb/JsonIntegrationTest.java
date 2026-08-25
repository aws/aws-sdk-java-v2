/*
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.mapper.dynamodb.pojos.TestDocClass;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.ConsistentReads;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapperConfig.TableNameOverride;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import static software.amazon.awssdk.mapper.dynamodb.pojos.TestDocClass.ChildClass;

public class JsonIntegrationTest extends LocalDynamoDBTestBase {

    private static final String TABLE_NAME = "test-table-"
            + UUID.randomUUID().toString();

    private static DynamoDbClient client;
    private static DynamoDBMapper mapper;

    @BeforeClass
    public static void setup() throws Exception {
        client = client();

        mapper = new DynamoDBMapper(
                client,
                new DynamoDBMapperConfig.Builder()
                        .withConversionSchema(ConversionSchemas.V2)
                        .withTableNameOverride(TableNameOverride
                                .withTableNameReplacement(TABLE_NAME))
                        .withConsistentReads(ConsistentReads.CONSISTENT)
                        .build());

        CreateTableRequest request = mapper
                .generateCreateTableRequest(TestDocClass.class)
                .toBuilder()
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1L).writeCapacityUnits(1L).build())
                .build();

        client.createTable(request);
        client.waiter().waitUntilTableExists(b -> b.tableName(TABLE_NAME));
    }

    @AfterClass
    public static void cleanup() {
        if (client == null) {
            return;
        }

        try {
            client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
        } catch (ResourceNotFoundException e) {
        }
    }

    @Test
    public void testIt() {
        final ChildClass child1 = new ChildClass();
        child1.setBool(true);

        final ChildClass child2 = new ChildClass();
        child2.setBool(true);

        final ChildClass parent = new ChildClass();
        parent.setFirstChild(child1);
        parent.setOtherChildren(Arrays.asList(child1, child2));
        parent.setNamedChildren(new HashMap<String, ChildClass>() {{
            put("one", child1);
            put("two", child2);
        }});

        TestDocClass test = new TestDocClass();
        test.setId("test");
        test.setListOfMaps(Arrays.<Map<String, ChildClass>>asList(
            new HashMap<String, ChildClass>() {{
                put("parent", parent);
            }},
            new HashMap<String, ChildClass>() {{
                put("parent", parent);
            }},
            null
        ));
        test.setMapOfLists(new HashMap<String, List<ChildClass>>() {{
            put("parent", Arrays.asList(child1, child2));
            put("child2", Collections.<ChildClass>emptyList());
            put("child1", null);
        }});

        mapper.save(test);

        TestDocClass result = mapper.load(TestDocClass.class, "test");

        Assert.assertEquals(test, result);
    }

}
