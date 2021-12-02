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
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primaryPartitionKey;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags.primarySortKey;

import java.util.Objects;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class FlattenWithTagsTest extends LocalDynamoDbSyncTestBase {
    private static class Record {
        private String id;
        private Document document;

        private String getId() {
            return id;
        }

        private Record setId(String id) {
            this.id = id;
            return this;
        }

        private Document getDocument() {
            return document;
        }

        private Record setDocument(Document document) {
            this.document = document;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return Objects.equals(id, record.id) &&
                   Objects.equals(document, record.document);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, document);
        }
    }
    
    private static class Document {
        private String documentAttribute1;
        private String documentAttribute2;
        private String documentAttribute3;

        private String getDocumentAttribute1() {
            return documentAttribute1;
        }

        private Document setDocumentAttribute1(String documentAttribute1) {
            this.documentAttribute1 = documentAttribute1;
            return this;
        }

        private String getDocumentAttribute2() {
            return documentAttribute2;
        }

        private Document setDocumentAttribute2(String documentAttribute2) {
            this.documentAttribute2 = documentAttribute2;
            return this;
        }

        private String getDocumentAttribute3() {
            return documentAttribute3;
        }

        private Document setDocumentAttribute3(String documentAttribute3) {
            this.documentAttribute3 = documentAttribute3;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Document document = (Document) o;
            return Objects.equals(documentAttribute1, document.documentAttribute1) &&
                   Objects.equals(documentAttribute2, document.documentAttribute2) &&
                   Objects.equals(documentAttribute3, document.documentAttribute3);
        }

        @Override
        public int hashCode() {
            return Objects.hash(documentAttribute1, documentAttribute2, documentAttribute3);
        }
    }

    private static final StaticTableSchema<Document> DOCUMENT_SCHEMA =
        StaticTableSchema.builder(Document.class)
                         .newItemSupplier(Document::new)
                         .addAttribute(String.class, a -> a.name("documentAttribute1")
                                                           .getter(Document::getDocumentAttribute1)
                                                           .setter(Document::setDocumentAttribute1)
                                                           .addTag(primarySortKey()))
                         .addAttribute(String.class, a -> a.name("documentAttribute2")
                                                           .getter(Document::getDocumentAttribute2)
                                                           .setter(Document::setDocumentAttribute2))
                         .addAttribute(String.class, a -> a.name("documentAttribute3")
                                                           .getter(Document::getDocumentAttribute3)
                                                           .setter(Document::setDocumentAttribute3))
                         .build();

    private static final TableSchema<Record> TABLE_SCHEMA =
        StaticTableSchema.builder(Record.class)
                         .newItemSupplier(Record::new)
                         .addAttribute(String.class, a -> a.name("id")
                                                           .getter(Record::getId)
                                                           .setter(Record::setId)
                                                           .tags(primaryPartitionKey()))
                         .flatten(DOCUMENT_SCHEMA, Record::getDocument, Record::setDocument)
                         .build();


    private DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                          .dynamoDbClient(getDynamoDbClient())
                                                                          .build();

    private DynamoDbTable<Record> mappedTable = enhancedClient.table(getConcreteTableName("table-name"), TABLE_SCHEMA);

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
    public void update_allValues() {
        Document document = new Document()
                                    .setDocumentAttribute1("one")
                                    .setDocumentAttribute2("two")
                                    .setDocumentAttribute3("three");
        Record record = new Record()
                              .setId("id-value")
                              .setDocument(document);

        Record updatedRecord = mappedTable.updateItem(r -> r.item(record));
        Record fetchedRecord = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("one")));

        assertThat(updatedRecord, is(record));
        assertThat(fetchedRecord, is(record));
    }

    @Test
    public void update_someValues() {
        Document document = new Document()
                                    .setDocumentAttribute1("one")
                                    .setDocumentAttribute2("two");
        Record record = new Record()
                              .setId("id-value")
                              .setDocument(document);

        Record updatedRecord = mappedTable.updateItem(r -> r.item(record));
        Record fetchedRecord = mappedTable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("one")));

        assertThat(updatedRecord, is(record));
        assertThat(fetchedRecord, is(record));
    }
}
