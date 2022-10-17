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
import static software.amazon.awssdk.enhanced.dynamodb.extensions.VersionedRecordExtension.AttributeTags.versionAttribute;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;

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
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class VersionedRecordWithSpecialCharactersTest extends LocalDynamoDbSyncTestBase {
    private static class Record {
        private String id;
        private String _attribute;
        private Integer _version;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private String get_attribute() {
            return _attribute;
        }

        private Record set_attribute(String _attribute) {
            this._attribute = _attribute;
            return this;
        }

        private Integer get_version() {
            return _version;
        }

        private Record set_version(Integer _version) {
            this._version = _version;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(_attribute, record._attribute) &&
                   Objects.equals(_version, record._version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, _attribute, _version);
        }
    }

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .addAttribute(String.class, a -> a.name("_attribute")
                                                           .getter(Record::get_attribute)
                                                           .setter(Record::set_attribute))
                         .addAttribute(Integer.class, a -> a.name("_version")
                                                            .getter(Record::get_version)
                                                            .setter(Record::set_version)
                                                            .tags(versionAttribute()))
                         .build();

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .extensions(VersionedRecordExtension.builder().build())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(r -> r.provisionedThroughput(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void putNewRecordSetsInitialVersion() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").set_attribute("one").set_version(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateNewRecordSetsInitialVersion() {
        Record result = mappedTable.updateItem(r -> r.item(new Record().setId("id").set_attribute("one")));

        Record expectedResult = new Record().setId("id").set_attribute("one").set_version(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));

        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one").set_version(1)));

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").set_attribute("one").set_version(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatchesConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "_attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").set_attribute("one").set_version(1))
                                                  .conditionExpression(conditionExpression)
                                                  .build());

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").set_attribute("one").set_version(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionDoesNotMatchConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "_attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").set_attribute("one").set_version(2))
                                                  .conditionExpression(conditionExpression)
                                                  .build());
    }

    @Test
    public void putExistingRecordVersionMatchesConditionExpressionDoesNotMatch() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.putItem(PutItemEnhancedRequest.builder(Record.class)
                                                  .item(new Record().setId("id").set_attribute("one").set_version(1))
                                                  .conditionExpression(conditionExpression)
                                                  .build());
    }

    @Test
    public void updateExistingRecordVersionMatchesConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "_attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(new Record().setId("id").set_attribute("one").set_version(1))
                                                        .conditionExpression(conditionExpression)
                                                        .build());

        Record result = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id")));
        Record expectedResult = new Record().setId("id").set_attribute("one").set_version(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateExistingRecordVersionDoesNotMatchConditionExpressionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "_attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("one"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(new Record().setId("id").set_attribute("one").set_version(2))
                                                        .conditionExpression(conditionExpression)
                                                        .build());
    }

    @Test
    public void updateExistingRecordVersionMatchesConditionExpressionDoesNotMatch() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        Expression conditionExpression = Expression.builder()
                                                   .expression("#k = :v OR #k = :v1")
                                                   .putExpressionName("#k", "attribute")
                                                   .putExpressionValue(":v", stringValue("wrong"))
                                                   .putExpressionValue(":v1", stringValue("wrong2"))
                                                   .build();

        exception.expect(ConditionalCheckFailedException.class);
        mappedTable.updateItem(UpdateItemEnhancedRequest.builder(Record.class)
                                                        .item(new Record().setId("id").set_attribute("one").set_version(1))
                                                        .conditionExpression(conditionExpression)
                                                        .build());
    }

    @Test
    public void updateExistingRecordVersionMatches() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));

        Record result =
            mappedTable.updateItem(r -> r.item(new Record().setId("id").set_attribute("one").set_version(1)));

        Record expectedResult = new Record().setId("id").set_attribute("one").set_version(2);
        assertThat(result, is(expectedResult));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putRecordWithWrongVersionNumber() {
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one")));
        mappedTable.putItem(r -> r.item(new Record().setId("id").set_attribute("one").set_version(2)));
    }
}
