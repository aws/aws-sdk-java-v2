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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.ConsistentReads;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.TableNameOverride;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class JsonIntegrationTest extends AwsTestBase {

    private static final String TABLE_NAME = "test-table-"
                                             + UUID.randomUUID().toString();

    private static DynamoDBClient client;
    private static DynamoDbMapper mapper;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = DynamoDBClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_WEST_2).build();

        mapper = new DynamoDbMapper(
                client,
                new DynamoDbMapperConfig.Builder()
                        .withConversionSchema(ConversionSchemas.V2)
                        .withTableNameOverride(TableNameOverride
                                                       .withTableNameReplacement(TABLE_NAME))
                        .withConsistentReads(ConsistentReads.CONSISTENT)
                        .build());

        CreateTableRequest request = mapper
                .generateCreateTableRequest(TestClass.class).toBuilder()
                .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
                .build();

        client.createTable(request);

        Waiter.run(() -> client.describeTable(r -> r.tableName(TABLE_NAME)))
              .until(r -> r.table().tableStatus() == TableStatus.ACTIVE)
              .orFail();
    }

    @AfterClass
    public static void cleanup() {
        if (client == null) {
            return;
        }

        try {
            client.deleteTable(DeleteTableRequest.builder().tableName(TABLE_NAME).build());
        } catch (ResourceNotFoundException e) {
            // Ignored or expected.
        }
    }

    private static <T> boolean eq(T one, T two) {
        if (one == null) {
            return (two == null);
        } else {
            return one.equals(two);
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

        TestClass test = new TestClass();
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

        TestClass result = mapper.load(TestClass.class, "test");

        Assert.assertEquals(test, result);
    }

    @DynamoDbTable(tableName = "")
    public static class TestClass {

        private String id;
        private List<Map<String, ChildClass>> listOfMaps;
        private Map<String, List<ChildClass>> mapOfLists;

        @DynamoDbHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<Map<String, ChildClass>> getListOfMaps() {
            return listOfMaps;
        }

        public void setListOfMaps(List<Map<String, ChildClass>> listOfMaps) {
            this.listOfMaps = listOfMaps;
        }

        public Map<String, List<ChildClass>> getMapOfLists() {
            return mapOfLists;
        }

        public void setMapOfLists(Map<String, List<ChildClass>> mapOfLists) {
            this.mapOfLists = mapOfLists;
        }

        @Override
        public boolean equals(Object obj) {
            TestClass other = (TestClass) obj;

            return (eq(id, other.id)
                    && eq(listOfMaps, other.listOfMaps)
                    && eq(mapOfLists, other.mapOfLists));
        }

        @Override
        public String toString() {
            return "{id=" + id + ", listOfMaps=" + listOfMaps + ", mapOfLists="
                   + mapOfLists + "}";
        }
    }

    @DynamoDbDocument
    public static class ChildClass {

        private boolean bool;

        private ChildClass firstChild;
        private List<ChildClass> otherChildren;
        private Map<String, ChildClass> namedChildren;

        public boolean isBool() {
            return bool;
        }

        public void setBool(boolean bool) {
            this.bool = bool;
        }

        public ChildClass getFirstChild() {
            return firstChild;
        }

        public void setFirstChild(ChildClass firstChild) {
            this.firstChild = firstChild;
        }

        public List<ChildClass> getOtherChildren() {
            return otherChildren;
        }

        public void setOtherChildren(List<ChildClass> otherChildren) {
            this.otherChildren = otherChildren;
        }

        public Map<String, ChildClass> getNamedChildren() {
            return namedChildren;
        }

        public void setNamedChildren(Map<String, ChildClass> namedChildren) {
            this.namedChildren = namedChildren;
        }

        @Override
        public boolean equals(Object obj) {
            ChildClass other = (ChildClass) obj;

            return (eq(bool, other.bool)
                    && eq(firstChild, other.firstChild)
                    && eq(otherChildren, other.otherChildren)
                    && eq(namedChildren, other.namedChildren));
        }

        @Override
        public String toString() {
            return "{bool=" + bool + ", firstChild=" + firstChild
                   + ", otherChildren=" + otherChildren + ", namedChildren="
                   + namedChildren + "}";
        }
    }
}
