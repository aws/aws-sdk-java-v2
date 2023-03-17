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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests.document;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo;
import static software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.sortBetween;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttribConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttributeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedTestRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;

public class BasicQueryTest extends LocalDynamoDbSyncTestBase {
    private DynamoDbClient lowLevelClient;
    private  DynamoDbTable<EnhancedDocument> docMappedtable ;
    private  DynamoDbTable<EnhancedDocument> neseteddocMappedtable ;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private DynamoDbEnhancedClient enhancedClient;
    private final String tableName = getConcreteTableName("doc-table-name");
    private final String nestedTableName = getConcreteTableName("doc-nested-table-name");

    @Before
    public void createTable() {


        lowLevelClient = getDynamoDbClient();
        enhancedClient = DynamoDbEnhancedClient.builder()
                                               .dynamoDbClient(lowLevelClient)
                                               .build();
        docMappedtable = enhancedClient.table(tableName,
                                              TableSchema.documentSchemaBuilder()
                                                         .attributeConverterProviders(defaultProvider())
                                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                               "id",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(TableMetadata.primaryIndexName(), "sort",
                                                                          AttributeValueType.N)
                                                         .attributeConverterProviders(defaultProvider())
                                                         .build());
        docMappedtable.createTable();
        neseteddocMappedtable = enhancedClient.table(nestedTableName,
                                              TableSchema.documentSchemaBuilder()
                                                         .attributeConverterProviders(
                                                             new InnerAttribConverterProvider<>(),
                                                             defaultProvider())
                                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                               "outerAttribOne",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(TableMetadata.primaryIndexName(), "sort",
                                                                          AttributeValueType.N)
                                                         .build());
        neseteddocMappedtable.createTable();

    }

   @After
    public void deleteTable() {

        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(tableName)
                                                          .build());
        getDynamoDbClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(nestedTableName)
                                                          .build());
    }

    private static final List<EnhancedDocument> DOCUMENTS =
        IntStream.range(0, 10)
            .mapToObj(i -> EnhancedDocument.builder()
                                           .putString("id", "id-value")
                                           .putNumber("sort", i)
                                           .putNumber("value", i)
                .build()

            ).collect(Collectors.toList());

    private static final List<EnhancedDocument> DOCUMENTS_WITH_PROVIDERS =
        IntStream.range(0, 10)
                 .mapToObj(i -> EnhancedDocument.builder()
                     .attributeConverterProviders(defaultProvider())
                                                .putString("id", "id-value")
                                                .putNumber("sort", i)
                                                .putNumber("value", i)
                                                .build()
                 ).collect(Collectors.toList());

    public static EnhancedDocument createDocumentFromNestedRecord(NestedTestRecord nestedTestRecord){

        EnhancedDocument.Builder enhancedDocument =
            EnhancedDocument.builder();
        if (nestedTestRecord.getOuterAttribOne() != null) {
            enhancedDocument.putString("outerAttribOne", nestedTestRecord.getOuterAttribOne());
        }
        if (nestedTestRecord.getSort() != null) {
            enhancedDocument.putNumber("sort", nestedTestRecord.getSort());
        }
        if (nestedTestRecord.getDotVariable() != null) {
            enhancedDocument.putString("test.com", nestedTestRecord.getDotVariable());
        }
        InnerAttributeRecord innerAttributeRecord = nestedTestRecord.getInnerAttributeRecord();
        if (innerAttributeRecord != null) {
            enhancedDocument.put("innerAttributeRecord", innerAttributeRecord, EnhancedType.of(InnerAttributeRecord.class));
        }
        return enhancedDocument.build();
    }

    private static final List<EnhancedDocument> NESTED_TEST_DOCUMENTS =
        IntStream.range(0, 10)
                 .mapToObj(i -> {
                     final NestedTestRecord nestedTestRecord = new NestedTestRecord();
                     nestedTestRecord.setOuterAttribOne("id-value-" + i);
                     nestedTestRecord.setSort(i);
                     final InnerAttributeRecord innerAttributeRecord = new InnerAttributeRecord();
                     innerAttributeRecord.setAttribOne("attribOne-"+i);
                     innerAttributeRecord.setAttribTwo(i);
                     nestedTestRecord.setInnerAttributeRecord(innerAttributeRecord);
                     nestedTestRecord.setDotVariable("v"+i);
                     return nestedTestRecord;
                 })
            .map(BasicQueryTest::createDocumentFromNestedRecord)

                 .collect(Collectors.toList());

    private static final List<NestedTestRecord> NESTED_TEST_RECORDS =
            IntStream.range(0, 10)
                    .mapToObj(i -> {
                        final NestedTestRecord nestedTestRecord = new NestedTestRecord();
                        nestedTestRecord.setOuterAttribOne("id-value-" + i);
                        nestedTestRecord.setSort(i);
                        final InnerAttributeRecord innerAttributeRecord = new InnerAttributeRecord();
                        innerAttributeRecord.setAttribOne("attribOne-"+i);
                        innerAttributeRecord.setAttribTwo(i);
                        nestedTestRecord.setInnerAttributeRecord(innerAttributeRecord);
                        nestedTestRecord.setDotVariable("v"+i);
                        return nestedTestRecord;
                    })
                    .collect(Collectors.toList());

    private void insertDocuments() {
        DOCUMENTS.forEach(document -> docMappedtable.putItem(r -> r.item(document)));
        NESTED_TEST_DOCUMENTS.forEach(nestedDocs -> neseteddocMappedtable.putItem(r -> r.item(nestedDocs)));
    }

    private void insertNestedDocuments() {
        NESTED_TEST_DOCUMENTS.forEach(nestedDocs -> neseteddocMappedtable.putItem(r -> r.item(nestedDocs)));
    }

    @Test
    public void queryAllRecordsDefaultSettings_shortcutForm() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(keyEqualTo(k -> k.partitionValue("id-value"))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().stream().map(i -> i.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS.stream().map(i -> i
                       .toBuilder()
                       .attributeConverterProviders(new InnerAttribConverterProvider<>(), defaultProvider())
                       .build()
                       .toJson()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withProjection() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
               .attributesToProject("value")
            ).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(DOCUMENTS.size()));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("id"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort"), is(nullValue()));
        assertThat(firstRecord.getNumber("value").intValue(), is(0));
    }

    @Test
    public void queryAllRecordsDefaultSettings_shortcutForm_viaItems() {
        insertDocuments();

        PageIterable<EnhancedDocument> query = docMappedtable.query(keyEqualTo(k -> k.partitionValue("id-value")));
        SdkIterable<EnhancedDocument> results = query.items();
        assertThat(results.stream().map(i -> i.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS.stream().map(i -> i
                       .toBuilder()
                       .attributeConverterProviders(new InnerAttribConverterProvider<>(), defaultProvider())
                       .build()
                       .toJson()).collect(Collectors.toList())));

    }

    @Test
    public void queryAllRecordsWithFilter() {
        insertDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#value >= :min_value AND #value <= :max_value")
                                          .expressionValues(expressionValues)
                                          .expressionNames(Collections.singletonMap("#value", "value"))
                                          .build();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .filterExpression(expression)
                                                  .build())
                       .iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().stream().map(i -> i.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS_WITH_PROVIDERS.stream().filter(r -> r.getNumber("sort").intValue() >= 3 && r.getNumber("sort").intValue() <= 5)
                          .map(doc -> doc.toJson())
                               .collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecordsWithFilterAndProjection() {
        insertDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#value >= :min_value AND #value <= :max_value")
                                          .expressionValues(expressionValues)
                                          .expressionNames(Collections.singletonMap("#value", "value"))
                                          .build();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .filterExpression(expression)
                                                  .attributesToProject("value")
                                                  .build())
                       .iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), hasSize(3));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));

        EnhancedDocument record = page.items().get(0);
        assertThat(record.getString("id"), nullValue());
        assertThat(record.getNumber("sort"), nullValue());
        assertThat(record.getNumber("value").intValue(), is(3));
    }

    @Test
    public void queryBetween() {
        insertDocuments();
        Key fromKey = Key.builder().partitionValue("id-value").sortValue(3).build();
        Key toKey = Key.builder().partitionValue("id-value").sortValue(5).build();
        Iterator<Page<EnhancedDocument>> results = docMappedtable.query(r -> r.queryConditional(sortBetween(fromKey, toKey))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().stream().map(i -> i.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS_WITH_PROVIDERS.stream().filter(r -> r.getNumber("sort").intValue() >= 3 && r.getNumber("sort").intValue() <= 5)
                                              .map(doc -> doc.toJson())
                                              .collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryLimit() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .limit(5)
                                                  .build())
                       .iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page3 = results.next();
        assertThat(results.hasNext(), is(false));

        Map<String, AttributeValue> expectedLastEvaluatedKey1 = new HashMap<>();
        expectedLastEvaluatedKey1.put("id", stringValue("id-value"));
        expectedLastEvaluatedKey1.put("sort", numberValue(4));
        Map<String, AttributeValue> expectedLastEvaluatedKey2 = new HashMap<>();
        expectedLastEvaluatedKey2.put("id", stringValue("id-value"));
        expectedLastEvaluatedKey2.put("sort", numberValue(9));
        assertThat(page1.items().stream().map(i -> i.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS_WITH_PROVIDERS.subList(0, 5).stream().map( doc -> doc.toJson()).collect(Collectors.toList())));
        assertThat(page1.lastEvaluatedKey(), is(expectedLastEvaluatedKey1));
        assertThat(page2.items().stream().map(i -> i.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS_WITH_PROVIDERS.subList(5, 10).stream().map( doc -> doc.toJson()).collect(Collectors.toList())));
        assertThat(page2.lastEvaluatedKey(), is(expectedLastEvaluatedKey2));
        assertThat(page3.items().stream().map(i -> i.toJson()).collect(Collectors.toList()), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty() {
        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(r -> r.queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryEmpty_viaItems() {
        PageIterable<EnhancedDocument> query = docMappedtable.query(keyEqualTo(k -> k.partitionValue("id-value")));
        SdkIterable<EnhancedDocument> results = query.items();
        assertThat(results.stream().collect(Collectors.toList()), is(empty()));
    }

    @Test
    public void queryExclusiveStartKey() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .exclusiveStartKey(exclusiveStartKey)
                                                  .build())
                       .iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().stream().map(doc -> doc.toJson()).collect(Collectors.toList()),
                   is(DOCUMENTS_WITH_PROVIDERS.subList(8, 10).stream().map(i -> i.toJson()).collect(Collectors.toList())));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryExclusiveStartKey_viaItems() {
        Map<String, AttributeValue> exclusiveStartKey = new HashMap<>();
        exclusiveStartKey.put("id", stringValue("id-value"));
        exclusiveStartKey.put("sort", numberValue(7));
        insertDocuments();
        SdkIterable<EnhancedDocument> results =
            docMappedtable.query(QueryEnhancedRequest.builder()
                                                  .queryConditional(keyEqualTo(k -> k.partitionValue("id-value")))
                                                  .exclusiveStartKey(exclusiveStartKey)
                                                  .build())
                       .items();

        assertThat(results.stream().map(doc -> doc.toJson()).collect(Collectors.toList()),
            is(DOCUMENTS_WITH_PROVIDERS.subList(8, 10).stream().map(i -> i.toJson()).collect(Collectors.toList())));
    }

    @Test
    public void queryNestedRecord_SingleAttributeName() {
        insertNestedDocuments();
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                .addNestedAttributeToProject(NestedAttributeName.builder().addElement("innerAttributeRecord")
                                                                .addElement("attribOne").build())).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(nullValue()));
        assertThat(firstRecord.getString("sort"), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is("attribOne-1"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(nullValue()));
        results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                .addAttributeToProject("sort")).iterator();
        assertThat(results.hasNext(), is(true));
        page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort").intValue(), is(1));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
    }

    @Test
    public void queryNestedRecord_withAttributeNameList() {
        insertNestedDocuments();
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                .addNestedAttributesToProject(Arrays.asList(
                    NestedAttributeName.builder().elements("innerAttributeRecord", "attribOne").build(),
                    NestedAttributeName.builder().addElement("outerAttribOne").build()))
                .addNestedAttributesToProject(NestedAttributeName.builder()
                                                                 .addElements(Arrays.asList("innerAttributeRecord",
                                                                                            "attribTwo")).build())).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is("id-value-1"));
        assertThat(firstRecord.getNumber("sort"), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is(
            "attribOne-1"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(1));
    }

    @Test
    public void queryNestedRecord_withAttributeNameListAndStringAttributeToProjectAppended() {
        insertNestedDocuments();
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                .addNestedAttributesToProject(Arrays.asList(
                    NestedAttributeName.builder().elements("innerAttributeRecord","attribOne").build()))
                .addNestedAttributesToProject(NestedAttributeName.create("innerAttributeRecord","attribTwo"))
                .addAttributeToProject("sort")).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(is(nullValue())));
        assertThat(firstRecord.getNumber("sort").intValue(), is(1));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is(
            "attribOne-1"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(1));
    }


    @Test
    public void queryAllRecordsDefaultSettings_withNestedProjectionNamesNotInNameMap() {
        insertNestedDocuments();
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-1")))
                .addNestedAttributeToProject( NestedAttributeName.builder().addElement("nonExistentSlot").build())).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord, is(nullValue()));
    }

    @Test
    public void queryRecordDefaultSettings_withDotInTheName() {
        insertNestedDocuments();
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
               .addNestedAttributeToProject( NestedAttributeName.create("test.com"))).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(is(nullValue())));
        assertThat(firstRecord.getNumber("sort"), is(is(nullValue())));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)) ,  is(nullValue()));
        assertThat(firstRecord.getString("test.com"), is("v7"));
        Iterator<Page<EnhancedDocument>> resultWithAttributeToProject =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                .attributesToProject( "test.com").build()).iterator();
        assertThat(resultWithAttributeToProject.hasNext(), is(true));
        Page<EnhancedDocument> pageResult = resultWithAttributeToProject.next();
        assertThat(resultWithAttributeToProject.hasNext(), is(false));
        assertThat(pageResult.items().size(), is(1));
        EnhancedDocument record = pageResult.items().get(0);
        assertThat(record.getString("outerAttribOne"), is(is(nullValue())));
        assertThat(record.getNumber("sort"), is(is(nullValue())));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)) ,  is(nullValue()));
        assertThat(record.getString("test.com"), is("v7"));
    }

    @Test
    public void queryRecordDefaultSettings_withEmptyAttributeList() {
        insertNestedDocuments();
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                .attributesToProject(new ArrayList<>()).build()).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is("id-value-7"));
        assertThat(firstRecord.getNumber("sort").intValue(), is(7));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(7));
        assertThat(firstRecord.getString("test.com"), is("v7"));
    }

    @Test
    public void queryRecordDefaultSettings_withNullAttributeList() {
        insertNestedDocuments();
        List<String> backwardCompatibilty = null;
        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.query(b -> b
                .queryConditional(keyEqualTo(k -> k.partitionValue("id-value-7")))
                .attributesToProject(backwardCompatibilty).build()).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(1));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is("id-value-7"));
        assertThat(firstRecord.getNumber("sort").intValue(), is(7));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(7));
        assertThat(firstRecord.getString("test.com"), is("v7"));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withNestedProjectionNameEmptyNameMap() {
        insertNestedDocuments();

        assertThatExceptionOfType(Exception.class).isThrownBy(
            () -> {
                Iterator<Page<EnhancedDocument>> results =  neseteddocMappedtable.query(b -> b.queryConditional(
                                                                                              keyEqualTo(k -> k.partitionValue("id-value-3")))
                                                                                          .attributesToProject("").build()).iterator();
                assertThat(results.hasNext(), is(true));
                Page<EnhancedDocument> page = results.next();
            });

        assertThatExceptionOfType(Exception.class).isThrownBy(
            () -> {
                Iterator<Page<EnhancedDocument>> results =  neseteddocMappedtable.query(b -> b.queryConditional(
                                                                                              keyEqualTo(k -> k.partitionValue("id-value-3")))
                                                                                          .addNestedAttributeToProject(NestedAttributeName.create("")).build()).iterator();
                assertThat(results.hasNext(), is(true));
                Page<EnhancedDocument> page = results.next();

            });
    }
}
