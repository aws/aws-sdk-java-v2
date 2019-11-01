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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.functionaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.AttributeValues.stringValue;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.VersionedRecordExtension.AttributeTags.version;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.AttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.integerNumber;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.staticmapper.Attributes.string;

import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import software.amazon.awssdk.extensions.dynamodb.mappingclient.Key;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedDatabase;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MappedTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.extensions.VersionedRecordExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.CreateTable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.GetItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.PutItem;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.operations.UpdateItem;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class VersionedRecordTest extends LocalDynamoDbTestBase {
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
        TableSchema.builder()
                   .newItemSupplier(Record::new)
                   .attributes(
                       string("id", Record::getId, Record::setId).as(primaryPartitionKey()),
                       string("attribute", Record::getAttribute, Record::setAttribute),
                       integerNumber("version", Record::getVersion, Record::setVersion).as(version()))
                   .build();

    private MappedDatabase mappedDatabase = MappedDatabase.builder()
                                                          .dynamoDbClient(getDynamoDbClient())
                                                          .extendWith(VersionedRecordExtension.builder().build())
                                                          .build();

    private MappedTable<Record> mappedTable = mappedDatabase.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

    @Before
    public void createTable() {
        mappedTable.execute(CreateTable.of(getDefaultProvisionedThroughput()));
    }

    @After
    public void deleteTable() {
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(getConcreteTableName("table-name"))
                                                          .build());
    }

    @Test
    public void putNewRecordSetsInitialVersion() {
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one")));

        Record result = mappedTable.execute(GetItem.of(Key.of(stringValue("id"))));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateNewRecordSetsInitialVersion() {
        Record result = mappedTable.execute(UpdateItem.of(new Record().setId("id").setAttribute("one")));

        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(1);

        assertThat(result, is(expectedResult));
    }

    @Test
    public void putExistingRecordVersionMatches() {
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one")));

        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one").setVersion(1)));

        Record result = mappedTable.execute(GetItem.of(Key.of(stringValue("id"))));
        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test
    public void updateExistingRecordVersionMatches() {
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one")));

        Record result =
            mappedTable.execute(UpdateItem.of(new Record().setId("id").setAttribute("one").setVersion(1)));

        Record expectedResult = new Record().setId("id").setAttribute("one").setVersion(2);
        assertThat(result, is(expectedResult));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putNewRecordTwice() {
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one")));
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one")));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void updateNewRecordTwice() {
        mappedTable.execute(UpdateItem.of(new Record().setId("id").setAttribute("one")));
        mappedTable.execute(UpdateItem.of(new Record().setId("id").setAttribute("one")));
    }

    @Test(expected = ConditionalCheckFailedException.class)
    public void putRecordWithWrongVersionNumber() {
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one")));
        mappedTable.execute(PutItem.of(new Record().setId("id").setAttribute("one").setVersion(2)));
    }
}
