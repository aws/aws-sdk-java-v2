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

package software.amazon.awssdk.nativeimagetest;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

public class DynamoDbEnhancedClientTestRunner implements TestRunner {
    private static final String TABLE_NAME = "native-image" + System.currentTimeMillis();
    private static final String ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS = "a*t:t.r-i#bute3";
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbEnhancedClientTestRunner.class);

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(Record::getSort)
                                                           .setter(Record::setSort)
                                                           .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("attribute")
                                                           .getter(Record::getAttribute)
                                                           .setter(Record::setAttribute))
                         .addAttribute(String.class, a -> a.name("attribute2*")
                                                           .getter(Record::getAttribute2)
                                                           .setter(Record::setAttribute2)
                                                           .tags(secondaryPartitionKey("gsi_1")))
                         .addAttribute(String.class, a -> a.name(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                           .getter(Record::getAttribute3)
                                                           .setter(Record::setAttribute3)
                                                           .tags(secondarySortKey("gsi_1")))
                         .build();


    private static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
        ProvisionedThroughput.builder()
                             .readCapacityUnits(50L)
                             .writeCapacityUnits(50L)
                             .build();

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbEnhancedClient enhancedClient;

    public DynamoDbEnhancedClientTestRunner() {
        dynamoDbClient = DependencyFactory.ddbClient();
        enhancedClient = DependencyFactory.enhancedClient(dynamoDbClient);
    }

    @Override
    public void runTests() {
        logger.info("starting to run DynamoDbEnhancedClient tests");
        try {
            DynamoDbTable<Record> mappedTable = enhancedClient.table(TABLE_NAME, TABLE_SCHEMA);
            mappedTable.createTable(r -> r.provisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT)
                                          .globalSecondaryIndices(
                                              EnhancedGlobalSecondaryIndex.builder()
                                                                          .indexName("gsi_1")
                                                                          .projection(p -> p.projectionType(ProjectionType.ALL))
                                                                          .provisionedThroughput(DEFAULT_PROVISIONED_THROUGHPUT)
                                                                          .build()));

            dynamoDbClient.waiter().waitUntilTableExists(b -> b.tableName(TABLE_NAME));

            Record record = new Record()
                .setId("id-value")
                .setSort("sort-value")
                .setAttribute("one")
                .setAttribute2("two")
                .setAttribute3("three");

            mappedTable.putItem(r -> r.item(record));

            mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value")));
        } finally {
            dynamoDbClient.deleteTable(b -> b.tableName(TABLE_NAME));
        }
    }

    private static final class Record {
        private String id;
        private String sort;
        private String attribute;
        private String attribute2;
        private String attribute3;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private String getSort() {
            return sort;
        }

        private Record setSort(String sort) {
            this.sort = sort;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private Record setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        private String getAttribute2() {
            return attribute2;
        }

        private Record setAttribute2(String attribute2) {
            this.attribute2 = attribute2;
            return this;
        }

        private String getAttribute3() {
            return attribute3;
        }

        private Record setAttribute3(String attribute3) {
            this.attribute3 = attribute3;
            return this;
        }
    }
}
