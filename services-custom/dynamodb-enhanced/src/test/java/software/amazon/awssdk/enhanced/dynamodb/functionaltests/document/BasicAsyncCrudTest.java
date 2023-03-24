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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData;
import software.amazon.awssdk.enhanced.dynamodb.document.TestData;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.LocalDynamoDbAsyncTestBase;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@RunWith(Parameterized.class)
public class BasicAsyncCrudTest extends LocalDynamoDbAsyncTestBase {

    private static final String ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS = "a*t:t.r-i#bute+3/4(&?5=@)<6>!ch$ar%";
    private final TestData testData;

    @Rule
    public ExpectedException exception = ExpectedException.none();
    private DynamoDbEnhancedAsyncClient enhancedClient;
    private final String tableName = getConcreteTableName("table-name");
    private DynamoDbAsyncClient lowLevelClient;

    private DynamoDbAsyncTable<EnhancedDocument> docMappedtable ;

    @Before
    public void setUp(){
        lowLevelClient = getDynamoDbAsyncClient();
        enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                               .dynamoDbClient(lowLevelClient)
                                               .build();
        docMappedtable = enhancedClient.table(tableName,
                                              TableSchema.documentSchemaBuilder()
                                                         .addIndexPartitionKey(TableMetadata.primaryIndexName(),
                                                                               "id",
                                                                               AttributeValueType.S)
                                                         .addIndexSortKey(TableMetadata.primaryIndexName(), "sort", AttributeValueType.S)
                                                         .attributeConverterProviders(defaultProvider())
                                                         .build());
        docMappedtable.createTable().join();
    }

    public BasicAsyncCrudTest(TestData testData) {
        this.testData = testData;
    }

    @Parameterized.Parameters
    public static Collection<TestData> parameters() throws Exception {
        return EnhancedDocumentTestData.testDataInstance().getAllGenericScenarios();
    }

    private static EnhancedDocument appendKeysToDoc(TestData testData) {
        EnhancedDocument enhancedDocument = testData.getEnhancedDocument().toBuilder()
                                                    .putString("id", "id-value")
                                                    .putString("sort", "sort-value").build();
        return enhancedDocument;
    }

    private static Map<String, AttributeValue> simpleKey() {
        Map<String, AttributeValue> key = new LinkedHashMap<>();
        key.put("id", AttributeValue.fromS("id-value"));
        key.put("sort", AttributeValue.fromS("sort-value"));
        return key;
    }

    private static Map<String, AttributeValue> appendKeysToTestDataAttributeMap(Map<String, AttributeValue> attributeValueMap) {

        Map<String, AttributeValue> result = new LinkedHashMap<>(attributeValueMap);
        result.put("id", AttributeValue.fromS("id-value"));
        result.put("sort", AttributeValue.fromS("sort-value"));
        return result;
    }

    @After
    public void deleteTable() {
        getDynamoDbAsyncClient().deleteTable(DeleteTableRequest.builder()
                                                          .tableName(tableName)
                                                          .build()).join();
    }

    @Test
    public void putThenGetItemUsingKey() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData);
        docMappedtable.putItem(enhancedDocument).join();
        Map<String, AttributeValue> key = simpleKey();
        GetItemResponse lowLevelGet = lowLevelClient.getItem(r -> r.key(key).tableName(tableName)).join();
        Assertions.assertThat(lowLevelGet.item()).isEqualTo(enhancedDocument.toMap());
    }

    @Test
    public void putThenGetItemUsingKeyItem() throws ExecutionException, InterruptedException {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData);

        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();


        EnhancedDocument result = docMappedtable.getItem(EnhancedDocument.builder()
                                                                         .attributeConverterProviders(testData.getAttributeConverterProvider())
                                                                         .putString("id", "id-value")
                                                                         .putString("sort", "sort-value")
                                                                         .build()).join();

        Map<String, AttributeValue> attributeValueMap = appendKeysToTestDataAttributeMap(testData.getDdbItemMap());
        Assertions.assertThat(result.toMap()).isEqualTo(enhancedDocument.toMap());
        Assertions.assertThat(result.toMap()).isEqualTo(attributeValueMap);
    }

    @Test
    public void getNonExistentItem()  {
        EnhancedDocument item = docMappedtable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        Assertions.assertThat(item).isNull();
    }

    @Test
    public void updateOverwriteCompleteItem_usingShortcutForm() {

        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString("attribute3", "three")
                                                                     .build();
        docMappedtable.putItem(enhancedDocument).join();

        // Updating new Items other than the one present in testData
        EnhancedDocument updateDocument = EnhancedDocument.builder()
                                                          .putString("id", "id-value")
                                                          .putString("sort", "sort-value")
                                                          .putString("attribute", "four")
                                                          .putString("attribute2", "five")
                                                          .putString("attribute3", "six")
                                                          .build();

        EnhancedDocument result = docMappedtable.updateItem(updateDocument).join();

        Map<String, AttributeValue> updatedItemMap = new LinkedHashMap<>(testData.getDdbItemMap());

        updatedItemMap.put("attribute", AttributeValue.fromS("four"));
        updatedItemMap.put("attribute2", AttributeValue.fromS("five"));
        updatedItemMap.put("attribute3", AttributeValue.fromS("six"));
        updatedItemMap.put("id", AttributeValue.fromS("id-value"));
        updatedItemMap.put("sort", AttributeValue.fromS("sort-value"));
        Map<String, AttributeValue> key = simpleKey();
        GetItemResponse lowLevelGet = lowLevelClient.getItem(r -> r.key(key).tableName(tableName)).join();
        Assertions.assertThat(lowLevelGet.item()).isEqualTo(result.toMap());
        Assertions.assertThat(lowLevelGet.item()).isEqualTo(updatedItemMap);
    }

    @Test
    public void putTwiceThenGetItem() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString("attribute3", "three")
                                                                     .build();

        docMappedtable.putItem(enhancedDocument).join();

        // Updating new Items other than the one present in testData
        EnhancedDocument updateDocument = EnhancedDocument.builder()
                                                          .attributeConverterProviders(defaultProvider())
                                                          .putString("id", "id-value")
                                                          .putString("sort", "sort-value")
                                                          .putString("attribute", "four")
                                                          .putString("attribute2", "five")
                                                          .putString("attribute3", "six")
                                                          .build();
        docMappedtable.putItem(r -> r.item(updateDocument)).join();
        Map<String, AttributeValue> key = simpleKey();
        GetItemResponse lowLevelGet = lowLevelClient.getItem(r -> r.key(key).tableName(tableName)).join();

        // All the items are overwritten
        Assertions.assertThat(lowLevelGet.item()).isEqualTo(updateDocument.toMap());

        EnhancedDocument docGetItem = docMappedtable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"
        ))).join();
        Assertions.assertThat(lowLevelGet.item()).isEqualTo(docGetItem.toMap());

    }

    @Test
    public void putThenDeleteItem_usingShortcutForm() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString("attribute3", "three")
                                                                     .build();

        Map<String, AttributeValue> key = simpleKey();

        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();
        GetItemResponse lowLevelGetBeforeDelete = lowLevelClient.getItem(r -> r.key(key).tableName(tableName)).join();


        EnhancedDocument beforeDeleteResult =
            docMappedtable.deleteItem(Key.builder().partitionValue("id-value").sortValue("sort-value").build()).join();


        EnhancedDocument afterDeleteDoc =
            docMappedtable.getItem(Key.builder().partitionValue("id-value").sortValue("sort-value").build()).join();

        GetItemResponse lowLevelGetAfterDelete = lowLevelClient.getItem(r -> r.key(key).tableName(tableName)).join();

        assertThat(enhancedDocument.toMap(), is(EnhancedDocument.fromAttributeValueMap(lowLevelGetBeforeDelete.item()).toMap()));
        assertThat(beforeDeleteResult.toMap(), is(enhancedDocument.toMap()));
        assertThat(beforeDeleteResult.toMap(), is(lowLevelGetBeforeDelete.item()));
        assertThat(afterDeleteDoc, is(nullValue()));
        assertThat(lowLevelGetAfterDelete.item().size(), is(0));
    }

    @Test
    public void putThenDeleteItem_usingKeyItemForm() {

        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString("attribute3", "three")
                                                                     .build();

        docMappedtable.putItem(enhancedDocument).join();
        EnhancedDocument beforeDeleteResult =
            docMappedtable.deleteItem(enhancedDocument).join();
        EnhancedDocument afterDeleteResult =
            docMappedtable.getItem(Key.builder().partitionValue("id-value").sortValue("sort-value").build()).join();

        assertThat(beforeDeleteResult.toMap(), is(enhancedDocument.toMap()));
        assertThat(afterDeleteResult, is(nullValue()));

        Map<String, AttributeValue> key = simpleKey();
        GetItemResponse lowLevelGetBeforeDelete = lowLevelClient.getItem(r -> r.key(key).tableName(tableName)).join();
        assertThat(lowLevelGetBeforeDelete.item().size(), is(0));
    }

    @Test
    public void putWithConditionThatSucceeds() {

        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString("attribute3", "three")
                                                                     .build();

        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();


        EnhancedDocument newDoc = enhancedDocument.toBuilder().putString("attribute", "four").build();
        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("one"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        docMappedtable.putItem(PutItemEnhancedRequest.builder(EnhancedDocument.class)
                                                     .item(newDoc)
                                                     .conditionExpression(conditionExpression).build()).join();

        EnhancedDocument result = docMappedtable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result.toMap(), is(newDoc.toMap()));
    }

    @Test
    public void putWithConditionThatFails() throws ExecutionException, InterruptedException {

        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString("attribute3", "three")
                                                                     .build();

        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        EnhancedDocument newDoc = enhancedDocument.toBuilder().putString("attribute", "four").build();
        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ConditionalCheckFailedException.class));
        docMappedtable.putItem(PutItemEnhancedRequest.builder(EnhancedDocument.class)
                                                     .item(newDoc)
                                                     .conditionExpression(conditionExpression).build()).join();
    }

    @Test
    public void deleteNonExistentItem() {
        EnhancedDocument result =
            docMappedtable.deleteItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteWithConditionThatSucceeds() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();
        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        Key key = docMappedtable.keyFrom(enhancedDocument);
        docMappedtable.deleteItem(DeleteItemEnhancedRequest.builder().key(key).conditionExpression(conditionExpression).build()).join();

        EnhancedDocument result = docMappedtable.getItem(r -> r.key(key)).join();
        assertThat(result, is(nullValue()));
    }

    @Test
    public void deleteWithConditionThatFails() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();
        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ConditionalCheckFailedException.class));
        docMappedtable.deleteItem(DeleteItemEnhancedRequest.builder().key(docMappedtable.keyFrom(enhancedDocument))
                                                        .conditionExpression(conditionExpression)
                                                        .build()).join();
    }

    @Test
    public void updateOverwriteCompleteRecord_usingShortcutForm() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();

        docMappedtable.putItem(enhancedDocument).join();
        // Updating new Items other than the one present in testData
        EnhancedDocument.Builder updateDocBuilder = EnhancedDocument.builder()
                                                                    .attributeConverterProviders(defaultProvider())
                                                                    .putString("id", "id-value")
                                                                    .putString("sort", "sort-value")
                                                                    .putString("attribute", "four")
                                                                    .putString("attribute2", "five")
                                                                    .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "six");

        EnhancedDocument expectedDocument = updateDocBuilder.build();
        // Explicitly Nullify each of the previous members
        testData.getEnhancedDocument().toMap().keySet().forEach(r -> {
            updateDocBuilder.putNull(r);
        });

        EnhancedDocument updateDocument = updateDocBuilder.build();
        EnhancedDocument result = docMappedtable.updateItem(updateDocument).join();
        assertThat(result.toMap(), is(expectedDocument.toMap()));
        assertThat(result.toJson(), is("{\"a*t:t.r-i#bute+3/4(&?5=@)<6>!ch$ar%\":\"six\",\"attribute\":\"four\","
                                       + "\"attribute2\":\"five\",\"id\":\"id-value\",\"sort\":\"sort-value\"}"));
    }

    @Test
    public void updateCreatePartialRecord() {

        EnhancedDocument.Builder docBuilder = appendKeysToDoc(testData).toBuilder()
                                                                       .putString("attribute", "one");
        EnhancedDocument updateDoc = docBuilder.build();
        /**
         * Explicitly removing AttributeNull Value that are added in testData for testing.
         * This should not be treated as Null in partial update, because for a Document, an AttributeValue.fromNul(true) with a
         * Null value is treated as  Null or non-existent during updateItem.
         */
        testData.getEnhancedDocument().toMap().entrySet().forEach(entry -> {
            if (AttributeValue.fromNul(true).equals(entry.getValue())) {
                docBuilder.remove(entry.getKey());
            }
        });
        EnhancedDocument expectedDocUpdate = docBuilder.build();
        EnhancedDocument result = docMappedtable.updateItem(r -> r.item(updateDoc)).join();
        assertThat(result.toMap(), is(expectedDocUpdate.toMap()));
    }

    @Test
    public void updateCreateKeyOnlyRecord() {
        EnhancedDocument.Builder updateDocBuilder = appendKeysToDoc(testData).toBuilder();

        EnhancedDocument expectedDocument = EnhancedDocument.builder()
                                                            .attributeConverterProviders(defaultProvider())
                                                            .putString("id", "id-value")
                                                            .putString("sort", "sort-value").build();

        testData.getEnhancedDocument().toMap().keySet().forEach(r -> {
            updateDocBuilder.putNull(r);
        });

        EnhancedDocument cleanedUpDoc = updateDocBuilder.build();
        EnhancedDocument result = docMappedtable.updateItem(r -> r.item(cleanedUpDoc)).join();
        assertThat(result.toMap(), is(expectedDocument.toMap()));
    }

    @Test
    public void updateOverwriteModelledNulls() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();
        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        EnhancedDocument updateDocument = EnhancedDocument.builder().attributeConverterProviders(defaultProvider())
                                                          .putString("id", "id-value")
                                                          .putString("sort", "sort-value")
                                                          .putString("attribute", "four")
                                                          .putNull("attribute2")
                                                          .putNull(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS).build();

        EnhancedDocument result = docMappedtable.updateItem(r -> r.item(updateDocument)).join();


        assertThat(result.isPresent("attribute2"), is(false));
        assertThat(result.isPresent(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS), is(false));
        assertThat(result.getString("attribute"), is("four"));

        testData.getEnhancedDocument().toMap().entrySet().forEach(entry -> {
            if (AttributeValue.fromNul(true).equals(entry.getValue())) {
                assertThat(result.isPresent(entry.getKey()), is(true));
            } else {
                assertThat(result.toMap().get(entry.getKey()), is(testData.getDdbItemMap().get(entry.getKey())));
            }
        });
    }

    @Test
    public void updateCanIgnoreNullsDoesNotIgnoreNullAttributeValues() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();

        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        EnhancedDocument updateDocument = EnhancedDocument.builder()
                                                          .putString("id", "id-value")
                                                          .putString("sort", "sort-value")
                                                          .putNull("attribute")
                                                          .putNull(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                          .build();
        EnhancedDocument result = docMappedtable.updateItem(UpdateItemEnhancedRequest.builder(EnhancedDocument.class)
                                                                                     .item(updateDocument)
                                                                                     .ignoreNulls(true)
                                                                                     .build()).join();
        EnhancedDocument expectedResult = appendKeysToDoc(testData).toBuilder()
                                                                   .putString("attribute2", "two")
                                                                   .build();
        assertThat(result.toMap(), is(expectedResult.toMap()));
    }

    @Test
    public void updateKeyOnlyExistingRecordDoesNothing() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData);
        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();
        EnhancedDocument hashKeyAndSortOnly = EnhancedDocument.builder()
                                                              .putString("id", "id-value")
                                                              .putString("sort", "sort-value").build();
        EnhancedDocument result = docMappedtable.updateItem(UpdateItemEnhancedRequest.builder(EnhancedDocument.class)
                                                                                     .item(hashKeyAndSortOnly)
                                                                                     .ignoreNulls(true)
                                                                                     .build()).join();
        assertThat(result.toMap(), is(enhancedDocument.toMap()));
    }

    @Test
    public void updateWithConditionThatSucceeds() {
        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();

        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        EnhancedDocument newDoc = EnhancedDocument.builder()
                                                  .attributeConverterProviders(defaultProvider())
                                                  .putString("id", "id-value")
                                                  .putString("sort", "sort-value")
                                                  .putString("attribute", "four")
                                                  .build();
        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        docMappedtable.updateItem(UpdateItemEnhancedRequest.builder(EnhancedDocument.class)
                                                           .item(newDoc)
                                                           .conditionExpression(conditionExpression)
                                                           .build()).join();

        EnhancedDocument result =
            docMappedtable.getItem(r -> r.key(k -> k.partitionValue("id-value").sortValue("sort-value"))).join();
        assertThat(result.toMap(), is(enhancedDocument.toBuilder().putString("attribute", "four").build().toMap()));
    }

    @Test
    public void updateWithConditionThatFails() {

        EnhancedDocument enhancedDocument = appendKeysToDoc(testData).toBuilder()
                                                                     .putString("attribute", "one")
                                                                     .putString("attribute2", "two")
                                                                     .putString(ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS, "three")
                                                                     .build();
        docMappedtable.putItem(r -> r.item(enhancedDocument)).join();

        EnhancedDocument newDoc = EnhancedDocument.builder()
                                                  .attributeConverterProviders(defaultProvider())
                                                  .putString("id", "id-value")
                                                  .putString("sort", "sort-value")
                                                  .putString("attribute", "four")
                                                  .build();
        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", ATTRIBUTE_NAME_WITH_SPECIAL_CHARACTERS)
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("wrong"))
                                                   .build();

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ConditionalCheckFailedException.class));
        docMappedtable.updateItem(UpdateItemEnhancedRequest.builder(EnhancedDocument.class)
                                                           .item(newDoc)
                                                           .conditionExpression(conditionExpression)
                                                           .build()).join();
    }
}
