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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.VersionedRecordExtension.AttributeTags.version;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.integerNumberAttribute;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.stringAttribute;

import java.util.Objects;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbEnhancedClient;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.DynamoDbTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.VersionedRecordExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.GetItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.PutItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.StaticTableSchema;
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
                         .attributes(
                             stringAttribute("id", Record::getId, Record::setId).as(primaryPartitionKey()),
                             stringAttribute("attribute", Record::getAttribute, Record::setAttribute),
                             integerNumberAttribute("version", Record::getVersion, Record::setVersion).as(version()))
                         .build();

    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .extendWith(VersionedRecordExtension.builder().build())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void createTable() {
        mappedTable.createTable(CreateTableEnhancedRequest.create(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void putNewRecordSetsInitialVersion() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));

        Record result = mappedTable.getItem(GetItemEnhancedRequest.create(Key.create(stringValue("id"))));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateNewRecordSetsInitialVersion() {
        Record result = mappedTable.updateItem(UpdateItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));

        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatches() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));

        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one").setVersion(1)));

        Record result = mappedTable.getItem(GetItemEnhancedRequest.create(Key.create(stringValue("id"))));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatchesConditionExpressionMatches() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
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

        Record result = mappedTable.getItem(GetItemEnhancedRequest.create(Key.create(stringValue("id"))));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionDoesNotMatchConditionExpressionMatches() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
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
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
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
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
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

        Record result = mappedTable.getItem(GetItemEnhancedRequest.create(Key.create(stringValue("id"))));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateExistingRecordVersionDoesNotMatchConditionExpressionMatches() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
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
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
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
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));

        Record result =
            mappedTable.updateItem(UpdateItemEnhancedRequest.create(new Record().setId("id").setAttribute("one").setVersion(1)));

        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putNewRecordTwice() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void updateNewRecordTwice() {
        mappedTable.updateItem(UpdateItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
        mappedTable.updateItem(UpdateItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putRecordWithWrongVersionNumber() {
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one")));
        mappedTable.putItem(PutItemEnhancedRequest.create(new Record().setId("id").setAttribute("one").setVersion(2)));
    }
}
