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

package software.amazon.awssdk.enhanced.dynamodb.internal.operations;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.document.DocumentTableSchema;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItemWithIndices;
import software.amazon.awssdk.enhanced.dynamodb.internal.extensions.DefaultDynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.SearchResultItem;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;
import software.amazon.awssdk.services.dynamodb.model.VectorCapacity;

@RunWith(MockitoJUnitRunner.class)
public class SearchVectorsOperationTest {
    private static final String TABLE_NAME = "table-name";
    private static final String VECTOR_INDEX_NAME = "embeddings-index";
    private static final OperationContext VECTOR_INDEX_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, VECTOR_INDEX_NAME);

    private static final OperationContext PRIMARY_INDEX_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, TableMetadata.primaryIndexName());

    private static final OperationContext SECONDARY_INDEX_CONTEXT =
        DefaultOperationContext.create(TABLE_NAME, "gsi_1");

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    @Mock
    private DynamoDbAsyncClient mockDynamoDbAsyncClient;

    @Mock
    private DynamoDbEnhancedClientExtension mockExtension;

    @Test
    public void generateRequest_mapsRequestFields() {
        Expression condition = Expression.builder()
                                         .expression("#category = :category")
                                         .putExpressionName("#category", "category")
                                         .putExpressionValue(":category", AttributeValue.fromS("books"))
                                         .build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f, 2.0f})
                                                                      .topK(10)
                                                                      .searchConditionExpression(condition)
                                                                      .build());

        SearchVectorsRequest request = operation.generateRequest(DocumentTableSchema.builder().build(),
                                                                 VECTOR_INDEX_CONTEXT,
                                                                 null);

        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.indexName()).isEqualTo(VECTOR_INDEX_NAME);
        assertThat(request.topK()).isEqualTo(10);
        assertThat(request.searchConditionExpression()).isEqualTo("#category = :category");
        assertThat(request.expressionAttributeNames().get("#category")).isEqualTo("category");
        assertThat(request.expressionAttributeValues().get(":category")).isEqualTo(AttributeValue.fromS("books"));
        assertThat(request.searchVector()).hasSize(2);
        assertThat(request.searchVector().get(0).n()).isEqualTo("1.0");
        assertThat(request.searchVector().get(1).n()).isEqualTo("2.0");
    }

    @Test
    public void serviceCall_makesTheRightCallAndReturnsResponse() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());
        SearchVectorsRequest searchVectorsRequest = SearchVectorsRequest.builder().tableName(TABLE_NAME).build();
        SearchVectorsResponse expectedResponse = SearchVectorsResponse.builder().build();
        when(mockDynamoDbClient.searchVectors(any(SearchVectorsRequest.class))).thenReturn(expectedResponse);

        SearchVectorsResponse response = operation.serviceCall(mockDynamoDbClient).apply(searchVectorsRequest);

        assertThat(response).isSameAs(expectedResponse);
        verify(mockDynamoDbClient).searchVectors(searchVectorsRequest);
    }

    @Test
    public void transformResponse_mapsItemsAndScores() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", AttributeValue.fromS("item-1"));

        SearchVectorsResponse response = SearchVectorsResponse.builder()
                                                              .searchResults(SearchResultItem.builder()
                                                                                             .item(itemMap)
                                                                                             .score(0.95)
                                                                                             .build())
                                                              .build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<EnhancedDocument> enhancedResponse =
            operation.transformResponse(response, DocumentTableSchema.builder().build(), VECTOR_INDEX_CONTEXT, null);

        assertThat(enhancedResponse.results()).hasSize(1);
        assertThat(enhancedResponse.results().get(0).score()).isEqualTo(0.95);
        assertThat(enhancedResponse.results().get(0).item().getString("id")).isEqualTo("item-1");
    }

    @Test
    public void operationName_isSearchVectors() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        assertThat(operation.operationName()).isEqualTo(OperationName.SEARCH_VECTORS);
    }

    @Test
    public void generateRequest_withPrimaryIndex_throwsIllegalArgumentException() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        assertThatThrownBy(() -> operation.generateRequest(DocumentTableSchema.builder().build(),
                                                           PRIMARY_INDEX_CONTEXT, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SearchVectors cannot be executed against a primary index. A vector index name is required.");
    }

    @Test
    public void generateRequest_withSecondaryIndex_throwsIllegalArgumentException() {
        SearchVectorsOperation<FakeItemWithIndices> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        assertThatThrownBy(() -> operation.generateRequest(FakeItemWithIndices.getTableSchema(),
                                                           SECONDARY_INDEX_CONTEXT, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SearchVectors cannot be executed against a secondary index.");
    }

    @Test
    public void generateRequest_minimalRequest_onlySearchVector() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {0.5f})
                                                                      .build());

        SearchVectorsRequest request = operation.generateRequest(DocumentTableSchema.builder().build(),
                                                                 VECTOR_INDEX_CONTEXT, null);

        assertThat(request.tableName()).isEqualTo(TABLE_NAME);
        assertThat(request.indexName()).isEqualTo(VECTOR_INDEX_NAME);
        assertThat(request.searchVector()).hasSize(1);
        assertThat(request.topK()).isNull();
        assertThat(request.searchConditionExpression()).isNull();
        assertThat(request.projectionExpression()).isNull();
        assertThat(request.hasExpressionAttributeNames()).isFalse();
        assertThat(request.hasExpressionAttributeValues()).isFalse();
    }

    @Test
    public void generateRequest_withReturnConsumedCapacity() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                      .build());

        SearchVectorsRequest request = operation.generateRequest(DocumentTableSchema.builder().build(),
                                                                 VECTOR_INDEX_CONTEXT, null);

        assertThat(request.returnConsumedCapacityAsString()).isEqualTo("TOTAL");
    }

    @Test
    public void generateRequest_withProjectionExpression() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .attributesToProject("title", "category")
                                                                      .build());

        SearchVectorsRequest request = operation.generateRequest(DocumentTableSchema.builder().build(),
                                                                 VECTOR_INDEX_CONTEXT, null);

        assertThat(request.projectionExpression()).isEqualTo("#AMZN_MAPPED_title,#AMZN_MAPPED_category");
        assertThat(request.expressionAttributeNames().get("#AMZN_MAPPED_title")).isEqualTo("title");
        assertThat(request.expressionAttributeNames().get("#AMZN_MAPPED_category")).isEqualTo("category");
    }

    @Test
    public void generateRequest_withNestedProjectionExpression() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .addNestedAttributeToProject(
                                                                          NestedAttributeName.create("metadata", "author"))
                                                                      .build());

        SearchVectorsRequest request = operation.generateRequest(DocumentTableSchema.builder().build(),
                                                                 VECTOR_INDEX_CONTEXT, null);

        assertThat(request.projectionExpression()).isEqualTo("#AMZN_MAPPED_metadata.#AMZN_MAPPED_author");
        assertThat(request.expressionAttributeNames().get("#AMZN_MAPPED_metadata")).isEqualTo("metadata");
        assertThat(request.expressionAttributeNames().get("#AMZN_MAPPED_author")).isEqualTo("author");
    }

    @Test
    public void generateRequest_conditionAndProjection_mergesExpressionNames() {
        Expression condition = Expression.builder()
                                         .expression("#pk = :pk")
                                         .putExpressionName("#pk", "partitionKey")
                                         .putExpressionValue(":pk", AttributeValue.fromS("articles"))
                                         .build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .searchConditionExpression(condition)
                                                                      .addNestedAttributeToProject(
                                                                          NestedAttributeName.create("metadata", "title"))
                                                                      .build());

        SearchVectorsRequest request = operation.generateRequest(DocumentTableSchema.builder().build(),
                                                                 VECTOR_INDEX_CONTEXT, null);

        // Both the condition name and projection names must be present
        assertThat(request.expressionAttributeNames().get("#pk")).isEqualTo("partitionKey");
        assertThat(request.expressionAttributeNames().get("#AMZN_MAPPED_metadata")).isEqualTo("metadata");
        assertThat(request.expressionAttributeNames().get("#AMZN_MAPPED_title")).isEqualTo("title");
        assertThat(request.searchConditionExpression()).isEqualTo("#pk = :pk");
        assertThat(request.projectionExpression()).isEqualTo("#AMZN_MAPPED_metadata.#AMZN_MAPPED_title");
    }

    @Test
    public void transformResponse_nullSearchResults_returnsEmptyList() {
        SearchVectorsResponse response = SearchVectorsResponse.builder().build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<EnhancedDocument> enhancedResponse =
            operation.transformResponse(response, DocumentTableSchema.builder().build(), VECTOR_INDEX_CONTEXT, null);

        assertThat(enhancedResponse.results()).isEmpty();
    }

    @Test
    public void transformResponse_emptySearchResults_returnsEmptyList() {
        SearchVectorsResponse response = SearchVectorsResponse.builder()
                                                              .searchResults(Collections.emptyList())
                                                              .build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<EnhancedDocument> enhancedResponse =
            operation.transformResponse(response, DocumentTableSchema.builder().build(), VECTOR_INDEX_CONTEXT, null);

        assertThat(enhancedResponse.results()).isEmpty();
    }

    @Test
    public void transformResponse_nullScore_returnsNull() {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", AttributeValue.fromS("item-1"));

        SearchVectorsResponse response = SearchVectorsResponse.builder()
                                                              .searchResults(SearchResultItem.builder()
                                                                                             .item(itemMap)
                                                                                             .build())
                                                              .build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<EnhancedDocument> enhancedResponse =
            operation.transformResponse(response, DocumentTableSchema.builder().build(), VECTOR_INDEX_CONTEXT, null);

        assertThat(enhancedResponse.results().get(0).score()).isNull();
    }

    @Test
    public void transformResponse_mapsConsumedCapacityToVectorCapacity() {
        VectorCapacity vectorCapacity = VectorCapacity.builder()
                                                      .vectorSearchRequestBytes(42.0)
                                                      .build();

        SearchVectorsResponse response = SearchVectorsResponse.builder()
                                                              .consumedCapacity(vectorCapacity)
                                                              .build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<EnhancedDocument> enhancedResponse =
            operation.transformResponse(response, DocumentTableSchema.builder().build(), VECTOR_INDEX_CONTEXT, null);

        assertThat(enhancedResponse.consumedCapacity()).isNotNull();
        assertThat(enhancedResponse.consumedCapacity().vectorSearchRequestBytes()).isEqualTo(42.0);
    }

    @Test
    public void transformResponse_nullConsumedCapacity_returnsNullVectorCapacity() {
        SearchVectorsResponse response = SearchVectorsResponse.builder().build();

        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<EnhancedDocument> enhancedResponse =
            operation.transformResponse(response, DocumentTableSchema.builder().build(), VECTOR_INDEX_CONTEXT, null);

        assertThat(enhancedResponse.consumedCapacity()).isNull();
    }

    @Test
    public void asyncServiceCall_makesTheRightCallAndReturnsResponse() {
        SearchVectorsOperation<EnhancedDocument> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());
        SearchVectorsRequest searchVectorsRequest = SearchVectorsRequest.builder().tableName(TABLE_NAME).build();
        SearchVectorsResponse expectedResponse = SearchVectorsResponse.builder().build();
        when(mockDynamoDbAsyncClient.searchVectors(any(SearchVectorsRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(expectedResponse));

        CompletableFuture<SearchVectorsResponse> responseFuture =
            operation.asyncServiceCall(mockDynamoDbAsyncClient).apply(searchVectorsRequest);

        assertThat(responseFuture.join()).isSameAs(expectedResponse);
        verify(mockDynamoDbAsyncClient).searchVectors(searchVectorsRequest);
    }

    @Test
    public void transformResponse_withExtension_transformsSearchResultItems() {
        FakeItemWithIndices originalItem = FakeItemWithIndices.builder()
                                                              .id("original-id")
                                                              .sort("original-sort")
                                                              .build();

        Map<String, AttributeValue> originalItemMap = toAttributeValueMap(originalItem);
        Map<String, AttributeValue> transformedItemMap = new HashMap<>();
        transformedItemMap.put("id", AttributeValue.fromS("transformed-id"));
        transformedItemMap.put("sort", AttributeValue.fromS("transformed-sort"));

        when(mockExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(ReadModification.builder().transformedItem(transformedItemMap).build());

        SearchVectorsResponse response = SearchVectorsResponse.builder()
                                                              .searchResults(SearchResultItem.builder()
                                                                                             .item(originalItemMap)
                                                                                             .score(0.75)
                                                                                             .build())
                                                              .build();

        SearchVectorsOperation<FakeItemWithIndices> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        SearchVectorsEnhancedResponse<FakeItemWithIndices> enhancedResponse =
            operation.transformResponse(response,
                                        FakeItemWithIndices.getTableSchema(),
                                        VECTOR_INDEX_CONTEXT,
                                        mockExtension);

        assertThat(enhancedResponse.results()).hasSize(1);
        assertThat(enhancedResponse.results().get(0).score()).isEqualTo(0.75);
        FakeItemWithIndices resultItem = enhancedResponse.results().get(0).item();
        assertThat(resultItem.getId()).isEqualTo("transformed-id");
        assertThat(resultItem.getSort()).isEqualTo("transformed-sort");
    }

    @Test
    public void transformResponse_withExtension_invokesAfterReadForEachResult() {
        List<FakeItemWithIndices> originalItems = Arrays.asList(
            FakeItemWithIndices.builder().id("item-1").sort("sort-1").build(),
            FakeItemWithIndices.builder().id("item-2").sort("sort-2").build());

        List<Map<String, AttributeValue>> originalItemMaps =
            originalItems.stream().map(SearchVectorsOperationTest::toAttributeValueMap).collect(toList());

        ReadModification[] readModifications = originalItemMaps.stream()
                                                               .map(itemMap -> ReadModification.builder()
                                                                                               .transformedItem(itemMap)
                                                                                               .build())
                                                               .toArray(ReadModification[]::new);

        when(mockExtension.afterRead(any(DynamoDbExtensionContext.AfterRead.class)))
            .thenReturn(readModifications[0], Arrays.copyOfRange(readModifications, 1, readModifications.length));

        SearchVectorsResponse response = SearchVectorsResponse.builder()
                                                              .searchResults(originalItemMaps.stream()
                                                                                             .map(itemMap ->
                                                                                                      SearchResultItem.builder()
                                                                                                                      .item(itemMap)
                                                                                                                      .score(1.0)
                                                                                                                      .build())
                                                                                             .collect(toList()))
                                                              .build();

        SearchVectorsOperation<FakeItemWithIndices> operation =
            SearchVectorsOperation.create(SearchVectorsEnhancedRequest.builder()
                                                                      .searchVector(new float[] {1.0f})
                                                                      .build());

        operation.transformResponse(response,
                                    FakeItemWithIndices.getTableSchema(),
                                    VECTOR_INDEX_CONTEXT,
                                    mockExtension);

        InOrder inOrder = inOrder(mockExtension);
        originalItemMaps.forEach(itemMap -> inOrder.verify(mockExtension)
                                                   .afterRead(DefaultDynamoDbExtensionContext.builder()
                                                                                             .tableMetadata(
                                                                                                 FakeItemWithIndices.getTableSchema()
                                                                                                                    .tableMetadata())
                                                                                             .operationContext(
                                                                                                 VECTOR_INDEX_CONTEXT)
                                                                                             .tableSchema(
                                                                                                 FakeItemWithIndices.getTableSchema())
                                                                                             .items(itemMap)
                                                                                             .build()));
    }

    private static Map<String, AttributeValue> toAttributeValueMap(FakeItemWithIndices item) {
        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("id", AttributeValue.fromS(item.getId()));
        itemMap.put("sort", AttributeValue.fromS(item.getSort()));
        return itemMap;
    }
}