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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.versionAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class VersionedRecordTest extends LocalDynamoDbSyncTestBase {
    private static class Record {
        private String id;
        private String attribute;
        private Integer version;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private String getAttribute() {
            return attribute;
        }

        private Record setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }

        private Integer getVersion() {
            return version;
        }

        private Record setVersion(Integer version) {
            this.version = version;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(attribute, record.attribute) &&
                   Objects.equals(version, record.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, attribute, version);
        }
    }

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("attribute")
                                                           .getter(Record::getAttribute)
                                                           .setter(Record::setAttribute))
                         .addAttribute(Integer.class, a -> a.name("version")
                                                            .getter(Record::getVersion)
                                                            .setter(Record::setVersion)
                                                            .tags(versionAttribute()))
                         .build();

    @DynamoDbBean
    public static class AnnotatedRecord {
        private String id;
        private String attribute;
        private Long version;

        public AnnotatedRecord() {
        }

        @DynamoDbPartitionKey
        public String getId() { return id; }
        public AnnotatedRecord setId(String id) {
            this.id = id;
            return this;
        }

        @DynamoDbVersionAttribute(startAt = 5, incrementBy = 3)
        public Long getVersion() { return version; }
        public AnnotatedRecord setVersion(Long version) {
            this.version = version;
            return this;
        }

        public String getAttribute() {
            return attribute;
        }

        public AnnotatedRecord setAttribute(String attribute) {
            this.attribute = attribute;
            return this;
        }
    }

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .extensions(VersionedRecordExtension.builder().build())
                                                                          .build();

    private DynamoDbEnhancedClient customVersionedEnhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .extensions(VersionedRecordExtension
                                                                                          .builder()
                                                                                          .incrementBy(2L)
                                                                                          .startAt(10L)
                                                                                          .build()
                                                                          )
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    private DynamoDbTable<Record> mappedCustomVersionedTable = customVersionedEnhancedClient
        .table(getConcreteTableName("table-name2"), TABLE_SCHEMA);


    private static final TableSchema<AnnotatedRecord> ANNOTATED_TABLE_SCHEMA =
        TableSchema.fromBean(AnnotatedRecord.class);

    private DynamoDbTable<AnnotatedRecord> annotatedTable = enhancedClient
        .table(getConcreteTableName("annotated-table"), ANNOTATED_TABLE_SCHEMA);




    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        mappedCustomVersionedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
        annotatedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name2"))
                                                          .build());

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("annotated-table"))
                                                          .build());
    }



    @Test
    public void putNewRecordSetsInitialVersion() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateNewRecordSetsInitialVersion() {
        Record result = mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));

        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));

        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one").setVersion(1)));

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatchesConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").setAttribute("one").setVersion(1))
                                                  .conditionExpression(conditionExpression)
                                                  .build());

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionDoesNotMatchConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").setAttribute("one").setVersion(2))
                                                  .conditionExpression(conditionExpression)
                                                  .build());
    }

    @Test
    public void putExistingRecordVersionMatchesConditionExpressionDoesNotMatch() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").setAttribute("one").setVersion(1))
                                                  .conditionExpression(conditionExpression)
                                                  .build());
    }

    @Test
    public void updateExistingRecordVersionMatchesConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(new Record().setId("id").setAttribute("one").setVersion(1))
                                                        .conditionExpression(conditionExpression)
                                                        .build());

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateExistingRecordVersionDoesNotMatchConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(new Record().setId("id").setAttribute("one").setVersion(2))
                                                        .conditionExpression(conditionExpression)
                                                        .build());
    }

    @Test
    public void updateExistingRecordVersionMatchesConditionExpressionDoesNotMatch() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(new Record().setId("id").setAttribute("one").setVersion(1))
                                                        .conditionExpression(conditionExpression)
                                                        .build());
    }

    @Test
    public void updateExistingRecordVersionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));

        Record result =
            mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one").setVersion(1)));

        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putNewRecordTwice() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void updateNewRecordTwice() {
        mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        mappedTable.updateItem(r -> r.item(new Record().setId("id").setAttribute("one")));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putRecordWithWrongVersionNumber() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));
        mappedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one").setVersion(2)));
    }

    @Test
    public void updateVersionIncrementByExpected() {
        mappedCustomVersionedTable.putItem(r -> r.item(new Record().setId("id").setAttribute("one")));

        Record currentRecord = mappedCustomVersionedTable.getItem(r -> r.key(k -> k.partitionValue("id")));

        Record result = mappedCustomVersionedTable.updateItem(r -> r.item(new Record()
                                                                              .setId("id")
                                                                              .setAttribute("two")
                                                                              .setVersion(currentRecord.getVersion())));

        Record expectedResult = new Record().setId("id").setAttribute("two").setVersion(14);
        assertThat(result.getVersion(), is(expectedResult.getVersion()));
    }

    @Test
    public void customStartAtValueIsUsedForFirstRecord() {
        mappedCustomVersionedTable.putItem(r -> r.item(new Record().setId("custom-start").setAttribute("test")));

        Record record = mappedCustomVersionedTable.getItem(r -> r.key(k -> k.partitionValue("custom-start")));
        assertThat(record.getVersion(), is(12));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void recordWithVersionBetweenStartAtAndFirstVersionFails() {
        Record invalidRecord = new Record().setId("invalid-version").setAttribute("test").setVersion(11);
        mappedCustomVersionedTable.putItem(r -> r.item(invalidRecord));
    }

    @Test
    public void annotationBasedCustomVersioningWorks() {
        annotatedTable.putItem(r -> r.item(new AnnotatedRecord().setAttribute("test").setId("annotated")));

        AnnotatedRecord result = annotatedTable.getItem(r -> r.key(k -> k.partitionValue("annotated")));

        assertThat(result.getVersion(), is(8L));

        AnnotatedRecord updated = annotatedTable.updateItem(r -> r.item(new AnnotatedRecord()
                                                                            .setId("annotated")
                                                                            .setAttribute("updated")
                                                                            .setVersion(8L)));

        assertThat(updated.getVersion(), is(11L));
    }

    @Test
    public void updateItem_existingRecordWithVersionZero_defaultStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("version-zero"));
        item.put("version", AttributeValue.builder().n("0").build());
        
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("table-name")).item(item));

        Record retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("version-zero")));
        assertThat(retrieved.getVersion(), is(0));

        Record updated = mappedTable.updateItem(retrieved);
        assertThat(updated.getVersion(), is(1));
    }

    @Test
    public void updateItem_existingRecordWithVersionEqualToBuilderStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("version-ten"));
        item.put("version", AttributeValue.builder().n("10").build());
        
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("table-name2")).item(item));

        Record retrieved = mappedCustomVersionedTable.getItem(r -> r.key(k -> k.partitionValue("version-ten")));
        assertThat(retrieved.getVersion(), is(10));

        Record updated = mappedCustomVersionedTable.updateItem(retrieved);
        assertThat(updated.getVersion(), is(12));
    }

    @Test
    public void updateItem_existingRecordWithVersionEqualToAnnotationStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("version-five"));
        item.put("version", AttributeValue.builder().n("5").build());
        
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("annotated-table")).item(item));

        AnnotatedRecord retrieved = annotatedTable.getItem(r -> r.key(k -> k.partitionValue("version-five")));
        assertThat(retrieved.getVersion(), is(5L));

        AnnotatedRecord updated = annotatedTable.updateItem(retrieved);
        assertThat(updated.getVersion(), is(8L));
    }

    @Test
    public void putItem_existingRecordWithVersionEqualToStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("put-version-ten"));
        item.put("version", AttributeValue.builder().n("10").build());
        
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("table-name2")).item(item));

        Record overwrite = new Record().setId("put-version-ten").setVersion(10);
        mappedCustomVersionedTable.putItem(overwrite);

        Record retrieved = mappedCustomVersionedTable.getItem(r -> r.key(k -> k.partitionValue("put-version-ten")));
        assertThat(retrieved.getVersion(), is(12));
    }

    @Test
    public void putItem_newRecordWithVersionEqualToStartAt_shouldSucceed() {
        Record newRecord = new Record().setId("explicit-zero").setAttribute("test").setVersion(0);
        mappedTable.putItem(newRecord);

        Record retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("explicit-zero")));
        assertThat(retrieved.getVersion(), is(1));
    }

    @Test
    public void sdkV1MigrationFlow_createRetrieveUpdate_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("v1-record"));
        item.put("attribute", stringValue("initial"));
        item.put("version", AttributeValue.builder().n("0").build());
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("table-name")).item(item));

        Record retrieved = mappedTable.getItem(r -> r.key(k -> k.partitionValue("v1-record")));
        assertThat(retrieved.getVersion(), is(0));

        retrieved.setAttribute("updated");
        Record updated = mappedTable.updateItem(retrieved);
        assertThat(updated.getVersion(), is(1));
        assertThat(updated.getAttribute(), is("updated"));
    }

    @Test
    public void deleteItem_existingRecordWithVersionZero_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("delete-test"));
        item.put("attribute", stringValue("test"));
        item.put("version", AttributeValue.builder().n("0").build());
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("table-name")).item(item));

        Record toDelete = mappedTable.getItem(r -> r.key(k -> k.partitionValue("delete-test")));
        assertThat(toDelete.getVersion(), is(0));

        mappedTable.deleteItem(toDelete);

        Record shouldBeNull = mappedTable.getItem(r -> r.key(k -> k.partitionValue("delete-test")));
        assertThat(shouldBeNull, is(nullValue()));
    }

    @Test
    public void updateItem_bothBuilderAndAnnotationWithVersionEqualToStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("both-config"));
        item.put("attribute", stringValue("initial"));
        item.put("version", AttributeValue.builder().n("5").build());
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("annotated-table")).item(item));

        AnnotatedRecord retrieved = annotatedTable.getItem(r -> r.key(k -> k.partitionValue("both-config")));
        assertThat(retrieved.getVersion(), is(5L));

        retrieved.setAttribute("updated");
        AnnotatedRecord updated = annotatedTable.updateItem(retrieved);
        assertThat(updated.getVersion(), is(8L));
    }

    @Test
    public void putItem_annotationConfigWithVersionEqualToStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("annotation-put"));
        item.put("attribute", stringValue("initial"));
        item.put("version", AttributeValue.builder().n("5").build());
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("annotated-table")).item(item));

        AnnotatedRecord overwrite = new AnnotatedRecord().setId("annotation-put").setAttribute("overwritten").setVersion(5L);
        annotatedTable.putItem(overwrite);

        AnnotatedRecord retrieved = annotatedTable.getItem(r -> r.key(k -> k.partitionValue("annotation-put")));
        assertThat(retrieved.getAttribute(), is("overwritten"));
        assertThat(retrieved.getVersion(), is(8L));
    }

    @Test
    public void deleteItem_builderConfigWithVersionEqualToStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("delete-builder"));
        item.put("attribute", stringValue("test"));
        item.put("version", AttributeValue.builder().n("10").build());
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("table-name2")).item(item));

        Record toDelete = mappedCustomVersionedTable.getItem(r -> r.key(k -> k.partitionValue("delete-builder")));
        assertThat(toDelete.getVersion(), is(10));

        mappedCustomVersionedTable.deleteItem(toDelete);

        Record shouldBeNull = mappedCustomVersionedTable.getItem(r -> r.key(k -> k.partitionValue("delete-builder")));
        assertThat(shouldBeNull, is(nullValue()));
    }

    @Test
    public void deleteItem_annotationConfigWithVersionEqualToStartAt_shouldSucceed() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", stringValue("delete-annotation"));
        item.put("attribute", stringValue("test"));
        item.put("version", AttributeValue.builder().n("5").build());
        getDynamoDbClient().putItem(r -> r.tableName(getConcreteTableName("annotated-table")).item(item));

        AnnotatedRecord toDelete = annotatedTable.getItem(r -> r.key(k -> k.partitionValue("delete-annotation")));
        assertThat(toDelete.getVersion(), is(5L));

        annotatedTable.deleteItem(toDelete);

        AnnotatedRecord shouldBeNull = annotatedTable.getItem(r -> r.key(k -> k.partitionValue("delete-annotation")));
        assertThat(shouldBeNull, is(nullValue()));
    }

    @Test
    public void putItem_customInitialValue_firstVersionEqualsInitialValue() {
        DynamoDbEnhancedClient clientWithInitialValue = DynamoDbEnhancedClient.builder()
                                                                               .dynamoDbClient(getDynamoDbClient())
                                                                               .extensions(VersionedRecordExtension
                                                                                               .builder()
                                                                                               .incrementBy(2L)
                                                                                               .initialValue(10L)
                                                                                               .build())
                                                                               .build();
        DynamoDbTable<Record> table = clientWithInitialValue.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

        Record record = new Record().setId("initial-value-test").setAttribute("test");
        table.putItem(record);

        Record retrieved = table.getItem(r -> r.key(k -> k.partitionValue("initial-value-test")));
        assertThat(retrieved.getVersion(), is(10));
    }

    @Test
    public void updateItem_customInitialValue_versionMatchesInitialValue_shouldSucceed() {
        DynamoDbEnhancedClient clientWithInitialValue = DynamoDbEnhancedClient.builder()
                                                                               .dynamoDbClient(getDynamoDbClient())
                                                                               .extensions(VersionedRecordExtension
                                                                                               .builder()
                                                                                               .incrementBy(2L)
                                                                                               .initialValue(10L)
                                                                                               .build())
                                                                               .build();
        DynamoDbTable<Record> table = clientWithInitialValue.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

        Record record = new Record().setId("initial-value-or").setAttribute("first").setVersion(10);
        table.updateItem(record);

        Record retrieved = table.getItem(r -> r.key(k -> k.partitionValue("initial-value-or")));
        assertThat(retrieved.getAttribute(), is("first"));
        assertThat(retrieved.getVersion(), is(12));
    }

    @Test
    public void putItem_nullInitialValue_firstVersionEqualsIncrementBy() {
        DynamoDbEnhancedClient clientWithNullInitialValue = DynamoDbEnhancedClient.builder()
                                                                                   .dynamoDbClient(getDynamoDbClient())
                                                                                   .extensions(VersionedRecordExtension
                                                                                                   .builder()
                                                                                                   .incrementBy(5L)
                                                                                                   .build())
                                                                                   .build();
        DynamoDbTable<Record> table = clientWithNullInitialValue.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

        Record record = new Record().setId("null-initial-value").setAttribute("test");
        table.putItem(record);

        Record retrieved = table.getItem(r -> r.key(k -> k.partitionValue("null-initial-value")));
        assertThat(retrieved.getVersion(), is(5));
    }

    @Test
    public void putItem_explicitStartAtNegativeOne_usesInitialValue() {
        DynamoDbEnhancedClient clientWithExplicitStartAt = DynamoDbEnhancedClient.builder()
                                                                                  .dynamoDbClient(getDynamoDbClient())
                                                                                  .extensions(VersionedRecordExtension
                                                                                                  .builder()
                                                                                                  .startAt(-1L)
                                                                                                  .initialValue(7L)
                                                                                                  .incrementBy(2L)
                                                                                                  .build())
                                                                                  .build();
        DynamoDbTable<Record> table = clientWithExplicitStartAt.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

        Record record = new Record().setId("explicit-start-at").setAttribute("test");
        table.putItem(record);

        Record retrieved = table.getItem(r -> r.key(k -> k.partitionValue("explicit-start-at")));
        assertThat(retrieved.getVersion(), is(7));
    }

    @Test
    public void updateItem_versionDoesNotMatchInitialValue_shouldSucceed() {
        DynamoDbEnhancedClient clientWithInitialValue = DynamoDbEnhancedClient.builder()
                                                                               .dynamoDbClient(getDynamoDbClient())
                                                                               .extensions(VersionedRecordExtension
                                                                                               .builder()
                                                                                               .initialValue(5L)
                                                                                               .incrementBy(2L)
                                                                                               .build())
                                                                               .build();
        DynamoDbTable<Record> table = clientWithInitialValue.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

        Record first = new Record().setId("equality-test").setAttribute("first");
        table.putItem(first);

        Record retrieved = table.getItem(r -> r.key(k -> k.partitionValue("equality-test")));
        assertThat(retrieved.getVersion(), is(5));

        retrieved.setAttribute("second");
        table.updateItem(retrieved);

        Record updated = table.getItem(r -> r.key(k -> k.partitionValue("equality-test")));
        assertThat(updated.getAttribute(), is("second"));
        assertThat(updated.getVersion(), is(7));
    }
}
