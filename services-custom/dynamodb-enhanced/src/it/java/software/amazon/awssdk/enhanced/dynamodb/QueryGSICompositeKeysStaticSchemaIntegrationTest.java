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

package software.amazon.awssdk.enhanced.dynamodb;

import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.secondarySortKey;

import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.enhanced.dynamodb.mapper.Order;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CompositeKeyRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.FlattenedRecord;

public class QueryGSICompositeKeysStaticSchemaIntegrationTest extends QueryGSICompositeKeysIntegrationTestBase {

    private static final String TABLE_NAME = createTestTableName();

    private static final StaticTableSchema<FlattenedRecord> FLATTENED_RECORD_SCHEMA =
        StaticTableSchema.builder(FlattenedRecord.class)
                         .newItemSupplier(FlattenedRecord::new)
                         .addAttribute(Double.class, a -> a.name("flpk2")
                                                           .getter(FlattenedRecord::getFlpk2)
                                                           .setter(FlattenedRecord::setFlpk2)
                                                           .tags(secondaryPartitionKey("gsi5", Order.SECOND)))
                         .addAttribute(String.class, a -> a.name("flpk3")
                                                           .getter(FlattenedRecord::getFlpk3)
                                                           .setter(FlattenedRecord::setFlpk3)
                                                           .tags(secondaryPartitionKey("gsi5", Order.THIRD)))
                         .addAttribute(String.class, a -> a.name("flsk2")
                                                           .getter(FlattenedRecord::getFlsk2)
                                                           .setter(FlattenedRecord::setFlsk2)
                                                           .tags(secondarySortKey("gsi5", Order.SECOND),
                                                                 secondarySortKey("gsi6", Order.FIRST)))
                         .addAttribute(java.time.Instant.class, a -> a.name("flsk3")
                                                                      .getter(FlattenedRecord::getFlsk3)
                                                                      .setter(FlattenedRecord::setFlsk3)
                                                                      .tags(secondarySortKey("gsi5", Order.THIRD)))
                         .addAttribute(String.class, a -> a.name("fldata")
                                                           .getter(FlattenedRecord::getFldata)
                                                           .setter(FlattenedRecord::setFldata))
                         .build(ExecutionContext.FLATTENED);

    private static final TableSchema<CompositeKeyRecord> COMPOSITE_KEY_SCHEMA =
        StaticTableSchema.builder(CompositeKeyRecord.class)
                         .newItemSupplier(CompositeKeyRecord::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(CompositeKeyRecord::getId)
                                                           .setter(CompositeKeyRecord::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("sort")
                                                           .getter(CompositeKeyRecord::getSort)
                                                           .setter(CompositeKeyRecord::setSort)
                                                           .tags(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("pk1")
                                                           .getter(CompositeKeyRecord::getPk1)
                                                           .setter(CompositeKeyRecord::setPk1)
                                                           .tags(secondaryPartitionKey("gsi1", Order.FIRST),
                                                                 secondaryPartitionKey("gsi2", Order.FIRST),
                                                                 secondaryPartitionKey("gsi3", Order.FIRST),
                                                                 secondaryPartitionKey("gsi4", Order.FIRST),
                                                                 secondaryPartitionKey("gsi5", Order.FIRST)))
                         .addAttribute(Integer.class, a -> a.name("pk2")
                                                            .getter(CompositeKeyRecord::getPk2)
                                                            .setter(CompositeKeyRecord::setPk2)
                                                            .tags(secondaryPartitionKey("gsi2", Order.SECOND),
                                                                  secondaryPartitionKey("gsi3", Order.SECOND),
                                                                  secondaryPartitionKey("gsi4", Order.SECOND),
                                                                  secondaryPartitionKey("gsi6", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("pk3")
                                                           .getter(CompositeKeyRecord::getPk3)
                                                           .setter(CompositeKeyRecord::setPk3)
                                                           .tags(secondaryPartitionKey("gsi3", Order.THIRD),
                                                                 secondaryPartitionKey("gsi4", Order.THIRD),
                                                                 secondaryPartitionKey("gsi6", Order.SECOND)))
                         .addAttribute(java.time.Instant.class, a -> a.name("pk4")
                                                                      .getter(CompositeKeyRecord::getPk4)
                                                                      .setter(CompositeKeyRecord::setPk4)
                                                                      .tags(secondaryPartitionKey("gsi4", Order.FOURTH)))
                         .addAttribute(String.class, a -> a.name("sk1")
                                                           .getter(CompositeKeyRecord::getSk1)
                                                           .setter(CompositeKeyRecord::setSk1)
                                                           .tags(secondarySortKey("gsi1", Order.FIRST),
                                                                 secondarySortKey("gsi2", Order.FIRST),
                                                                 secondarySortKey("gsi3", Order.FIRST),
                                                                 secondarySortKey("gsi4", Order.FIRST),
                                                                 secondarySortKey("gsi5", Order.FIRST)))
                         .addAttribute(String.class, a -> a.name("sk2")
                                                           .getter(CompositeKeyRecord::getSk2)
                                                           .setter(CompositeKeyRecord::setSk2)
                                                           .tags(secondarySortKey("gsi2", Order.SECOND),
                                                                 secondarySortKey("gsi3", Order.SECOND),
                                                                 secondarySortKey("gsi4", Order.SECOND)))
                         .addAttribute(java.time.Instant.class, a -> a.name("sk3")
                                                                      .getter(CompositeKeyRecord::getSk3)
                                                                      .setter(CompositeKeyRecord::setSk3)
                                                                      .tags(secondarySortKey("gsi3", Order.THIRD),
                                                                            secondarySortKey("gsi4", Order.THIRD),
                                                                            secondarySortKey("gsi6", Order.SECOND)))
                         .addAttribute(Integer.class, a -> a.name("sk4")
                                                            .getter(CompositeKeyRecord::getSk4)
                                                            .setter(CompositeKeyRecord::setSk4)
                                                            .tags(secondarySortKey("gsi4", Order.FOURTH)))
                         .addAttribute(String.class, a -> a.name("data")
                                                           .getter(CompositeKeyRecord::getData)
                                                           .setter(CompositeKeyRecord::setData))
                         .flatten(FLATTENED_RECORD_SCHEMA, CompositeKeyRecord::getFlattenedRecord,
                                  CompositeKeyRecord::setFlattenedRecord)
                         .build();

    @BeforeAll
    public static void setup() {
        dynamoDbClient = createDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
        mappedTable = enhancedClient.table(TABLE_NAME, COMPOSITE_KEY_SCHEMA);
        mappedTable.createTable();
        dynamoDbClient.waiter().waitUntilTableExists(r -> r.tableName(TABLE_NAME));
        insertRecords();
        waitForGsiConsistency();
    }

}
