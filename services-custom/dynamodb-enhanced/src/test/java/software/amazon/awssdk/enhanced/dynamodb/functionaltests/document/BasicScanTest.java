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
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.JsonTestUtils.toJsonNode;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.numberValue;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

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
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbSyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttribConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.InnerAttributeRecord;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.NestedTestRecord;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.Select;

public class BasicScanTest extends LocalDynamoDbSyncTestBase {
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


    private void insertDocuments() {
        DOCUMENTS.forEach(document -> docMappedtable.putItem(r -> r.item(document)));
        NESTED_TEST_DOCUMENTS.forEach(nestedDocs -> neseteddocMappedtable.putItem(r -> r.item(nestedDocs)));
    }

    private void insertNestedDocuments() {
        NESTED_TEST_DOCUMENTS.forEach(nestedDocs -> neseteddocMappedtable.putItem(r -> r.item(nestedDocs)));
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

    @Test
    public void scanAllRecordsDefaultSettings() {
        insertDocuments();

        docMappedtable.scan(ScanEnhancedRequest.builder().build())
                   .forEach(p -> p.items().forEach(item -> System.out.println(item)));
        Iterator<Page<EnhancedDocument>> results = docMappedtable.scan(ScanEnhancedRequest.builder().build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(toJsonNode(page.items()),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS)));

        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void queryAllRecordsDefaultSettings_withProjection() {
        insertDocuments();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(b -> b.attributesToProject("sort")).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().size(), is(DOCUMENTS.size()));

        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("id"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort").intValue(), is(0));
    }

    @Test
    public void scanAllRecordsDefaultSettings_viaItems() {
        insertDocuments();
        SdkIterable<EnhancedDocument> items = docMappedtable.scan(ScanEnhancedRequest.builder().limit(2).build()).items();
        assertThat(toJsonNode(items.stream().collect(Collectors.toList())),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS)));
    }

    @Test
    public void scanAllRecordsWithFilter() {
        insertDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("sort >= :min_value AND sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .build();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(ScanEnhancedRequest.builder().filterExpression(expression).build()).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(toJsonNode(page.items()),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS
                                     .stream()
                                     .filter(r -> r.getNumber("sort").intValue() >= 3 && r.getNumber("sort").intValue() <= 5)
                                     .collect(Collectors.toList()))));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithFilterAndProjection() {
        insertDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(
                ScanEnhancedRequest.builder()
                                   .attributesToProject("sort")
                                   .filterExpression(expression)
                                   .build()
            ).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items(), hasSize(3));

        EnhancedDocument record = page.items().get(0);

        assertThat(record.getString("id"), is(nullValue()));
        assertThat(record.getNumber("sort").intValue(), is(3));
    }

    @Test
    public void scanLimit() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results = docMappedtable.scan(r -> r.limit(5)).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page1 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page2 = results.next();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page3 = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(toJsonNode(page1.items()),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS.subList(0, 5))));
        assertThat(page1.lastEvaluatedKey(), is(getKeyMap(4)));
        assertThat(toJsonNode(page2.items()),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS.subList(5, 10))));

        assertThat(page2.lastEvaluatedKey(), is(getKeyMap(9)));
        assertThat(page3.items(), is(empty()));
        assertThat(page3.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanLimit_viaItems() {
        insertDocuments();
        SdkIterable<EnhancedDocument> results = docMappedtable.scan(r -> r.limit(5)).items();
        assertThat(toJsonNode(results.stream().collect(Collectors.toList())),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS)));
    }

    @Test
    public void scanEmpty() {
        Iterator<Page<EnhancedDocument>> results = docMappedtable.scan().iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items(), is(empty()));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanEmpty_viaItems() {
        Iterator<EnhancedDocument> results = docMappedtable.scan().items().iterator();
        assertThat(results.hasNext(), is(false));
    }

    @Test
    public void scanExclusiveStartKey() {
        insertDocuments();
        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(r -> r.exclusiveStartKey(getKeyMap(7))).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(toJsonNode(page.items()),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS.subList(8, 10))));
        assertThat(page.lastEvaluatedKey(), is(nullValue()));
    }

    @Test
    public void scanExclusiveStartKey_viaItems() {
        insertDocuments();
        SdkIterable<EnhancedDocument> results =
            docMappedtable.scan(r -> r.exclusiveStartKey(getKeyMap(7))).items();
        assertThat(toJsonNode(results.stream().collect(Collectors.toList())),
                   is(toJsonNode(DOCUMENTS_WITH_PROVIDERS.subList(8, 10))));
    }

    private Map<String, AttributeValue> getKeyMap(int sort) {
        Map<String, AttributeValue> result = new HashMap<>();
        result.put("id", stringValue("id-value"));
        result.put("sort", numberValue(sort));
        return Collections.unmodifiableMap(result);
    }

    @Test
    public void scanAllRecordsWithFilterAndNestedProjectionSingleAttribute() {
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                                          .expression("#sort >= :min_value AND #sort <= :max_value")
                                          .expressionValues(expressionValues)
                                          .putExpressionName("#sort", "sort")
                                          .build();

        Iterator<Page<EnhancedDocument>> results =
            neseteddocMappedtable.scan(
                ScanEnhancedRequest.builder()
                                   .filterExpression(expression)
                                   .addNestedAttributesToProject(
                                       NestedAttributeName.create(Arrays.asList("innerAttributeRecord","attribOne")))
                                   .build()
            ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
            item1.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne()
                 .compareTo(item2.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne()));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort"), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is(
            "attribOne-3"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(),
                   is(nullValue()));

        //Attribute repeated with new and old attributeToProject
        results =
            neseteddocMappedtable.scan(
                ScanEnhancedRequest.builder()
                                   .filterExpression(expression)
                                   .addNestedAttributesToProject(NestedAttributeName.create("sort"))
                                   .addAttributeToProject("sort")
                                   .build()
            ).iterator();
        assertThat(results.hasNext(), is(true));
        page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
            item1.getNumber("sort").bigDecimalValue()
                 .compareTo(item2.getNumber("sort").bigDecimalValue()));
        firstRecord = page.items().get(0);
        assertThat(firstRecord.get("outerAttribOne", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
        assertThat(firstRecord.getNumber("sort").intValue(), is(3));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));

        results =
            neseteddocMappedtable.scan(
                ScanEnhancedRequest.builder()
                                   .filterExpression(expression)
                                   .addNestedAttributeToProject(
                                       NestedAttributeName.create(Arrays.asList("innerAttributeRecord","attribOne")))
                                   .build()
            ).iterator();
        assertThat(results.hasNext(), is(true));
        page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
            item1.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne()
                 .compareTo(item2.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne()));
        firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort"), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is(
            "attribOne-3"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(),
                   is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithFilterAndNestedProjectionMultipleAttribute() {
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        final ScanEnhancedRequest build = ScanEnhancedRequest.builder()
                .filterExpression(expression)
                .addAttributeToProject("outerAttribOne")
                .addNestedAttributesToProject(Arrays.asList(NestedAttributeName.builder().elements("innerAttributeRecord")
                        .addElement("attribOne").build()))
                .addNestedAttributeToProject(NestedAttributeName.builder()
                        .elements(Arrays.asList("innerAttributeRecord", "attribTwo")).build())
                .build();
        Iterator<Page<EnhancedDocument>> results =
                neseteddocMappedtable.scan(
                        build
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne()
                        .compareTo(item2.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne()));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is("id-value-3"));
        assertThat(firstRecord.getNumber("sort"), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is(
            "attribOne-3"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(3));
    }

    @Test
    public void scanAllRecordsWithNonExistigKeyName() {
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<EnhancedDocument>> results =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.builder().addElement("nonExistent").build())
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord, is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithDotInAttributeKeyName() {
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<EnhancedDocument>> results =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName
                                        .create("test.com")).build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getString("test.com")
                        .compareTo(item2.getString("test.com")));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("uterAttribOne"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort"), is(nullValue()));
        assertThat(firstRecord.getString("test.com"), is("v3"));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithSameNamesRepeated() {
        //Attribute repeated with new and old attributeToProject
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<EnhancedDocument> >results =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.builder().elements("sort").build())
                                .addAttributeToProject("sort")
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getNumber("sort").bigDecimalValue()
                        .compareTo(item2.getNumber("sort").bigDecimalValue()));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort").intValue(), is(3));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)), is(nullValue()));
    }

    @Test
    public void scanAllRecordsWithEmptyList() {
        //Attribute repeated with new and old attributeToProject
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<EnhancedDocument> >results =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(new ArrayList<>())
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getNumber("sort").bigDecimalValue()
                        .compareTo(item2.getNumber("sort").bigDecimalValue()));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is("id-value-3"));
        assertThat(firstRecord.getNumber("sort").intValue(), is(3));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(3));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is("attribOne-3"));
    }

    @Test
    public void scanAllRecordsWithNullAttributesToProject() {
        //Attribute repeated with new and old attributeToProject
        insertNestedDocuments();
        List<String> backwardCompatibilityNull = null;
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        Iterator<Page<EnhancedDocument> >results =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .attributesToProject("test.com")
                                .attributesToProject(backwardCompatibilityNull)
                                .build()
                ).iterator();
        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));
        assertThat(page.items().size(), is(3));
        Collections.sort(page.items(), (item1, item2) ->
                item1.getNumber("sort").bigDecimalValue()
                        .compareTo(item2.getNumber("sort").bigDecimalValue()));
        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("outerAttribOne"), is("id-value-3"));
        assertThat(firstRecord.getNumber("sort").intValue(), is(3));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribTwo(), is(3));
        assertThat(firstRecord.get("innerAttributeRecord", EnhancedType.of(InnerAttributeRecord.class)).getAttribOne(), is(
            "attribOne-3"));
    }

    @Test
    public void scanAllRecordsWithNestedProjectionNameEmptyNameMap() {
        insertNestedDocuments();
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":min_value", numberValue(3));
        expressionValues.put(":max_value", numberValue(5));
        Expression expression = Expression.builder()
                .expression("#sort >= :min_value AND #sort <= :max_value")
                .expressionValues(expressionValues)
                .putExpressionName("#sort", "sort")
                .build();

        final Iterator<Page<EnhancedDocument>> results =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addNestedAttributesToProject(NestedAttributeName.builder().elements("").build()).build()
                ).iterator();

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> { final boolean b = results.hasNext();
        Page<EnhancedDocument> next = results.next(); }).withMessageContaining("ExpressionAttributeNames contains invalid "
                                                                                + "value");

        final Iterator<Page<EnhancedDocument>> resultsAttributeToProject =
                neseteddocMappedtable.scan(
                        ScanEnhancedRequest.builder()
                                .filterExpression(expression)
                                .addAttributeToProject("").build()
                ).iterator();

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> {
            final boolean b = resultsAttributeToProject.hasNext();
            Page<EnhancedDocument> next = resultsAttributeToProject.next();
        });
    }

    @Test
    public void scanAllRecordsDefaultSettings_select() {
        insertDocuments();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(b -> b.select(Select.ALL_ATTRIBUTES)).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().size(), is(DOCUMENTS.size()));

        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("id"), is("id-value"));
        assertThat(firstRecord.getNumber("sort").intValue(), is(0));
        assertThat(firstRecord.getNumber("value").intValue(), is(0));
    }

    @Test
    public void scanAllRecordsDefaultSettings_select_specific_attr() {
        insertDocuments();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(b -> b.attributesToProject("sort").select(Select.SPECIFIC_ATTRIBUTES)).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.items().size(), is(DOCUMENTS.size()));

        EnhancedDocument firstRecord = page.items().get(0);
        assertThat(firstRecord.getString("id"), is(nullValue()));
        assertThat(firstRecord.getNumber("sort").intValue(), is(0));
    }


    @Test
    public void scanAllRecordsDefaultSettings_select_count() {
        insertDocuments();

        Iterator<Page<EnhancedDocument>> results =
            docMappedtable.scan(b -> b.select(Select.COUNT)).iterator();

        assertThat(results.hasNext(), is(true));
        Page<EnhancedDocument> page = results.next();
        assertThat(results.hasNext(), is(false));

        assertThat(page.count(), is(DOCUMENTS.size()));
        assertThat(page.items().size(), is(0));
    }
}
