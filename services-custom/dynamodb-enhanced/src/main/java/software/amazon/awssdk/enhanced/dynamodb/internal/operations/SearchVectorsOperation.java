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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.OperationContext;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils;
import software.amazon.awssdk.enhanced.dynamodb.internal.ProjectionExpression;
import software.amazon.awssdk.enhanced.dynamodb.internal.SearchVectorUtils;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchResultItem;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchVectorsEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest;
import software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse;
import software.amazon.awssdk.utils.CollectionUtils;

@SdkInternalApi
public class SearchVectorsOperation<T> implements VectorIndexOperation<T, SearchVectorsRequest, SearchVectorsResponse,
    SearchVectorsEnhancedResponse<T>> {

    private final SearchVectorsEnhancedRequest request;

    private SearchVectorsOperation(SearchVectorsEnhancedRequest request) {
        this.request = request;
    }

    public static <T> SearchVectorsOperation<T> create(SearchVectorsEnhancedRequest request) {
        return new SearchVectorsOperation<>(request);
    }

    @Override
    public OperationName operationName() {
        return OperationName.SEARCH_VECTORS;
    }

    /**
     * Builds the low-level {@link SearchVectorsRequest}.
     *
     * @throws IllegalArgumentException if {@code context.indexName()} is the primary index
     *                                  ({@link TableMetadata#primaryIndexName()}) — message:
     *                                  {@code "SearchVectors cannot be executed against a primary index. A vector index name is
     *                                  required."}
     * @throws IllegalArgumentException if {@code context.indexName()} is a GSI or LSI
     */
    @Override
    public SearchVectorsRequest generateRequest(TableSchema<T> tableSchema,
                                                OperationContext context,
                                                DynamoDbEnhancedClientExtension extension) {
        if (TableMetadata.primaryIndexName().equals(context.indexName())) {
            throw new IllegalArgumentException(
                "SearchVectors cannot be executed against a primary index. A vector index name is required.");
        }

        boolean isSecondaryIndex = tableSchema.tableMetadata()
                                              .indices()
                                              .stream()
                                              .anyMatch(index -> index.name().equals(context.indexName()));
        if (isSecondaryIndex) {
            throw new IllegalArgumentException("SearchVectors cannot be executed against a secondary index.");
        }

        Map<String, AttributeValue> expressionValues = null;
        Map<String, String> expressionNames = null;

        if (request.searchConditionExpression() != null) {
            expressionValues = request.searchConditionExpression().expressionValues();
            expressionNames = request.searchConditionExpression().expressionNames();
        }

        String projectionExpressionAsString = null;
        if (request.nestedAttributesToProject() != null) {
            ProjectionExpression attributesToProject = ProjectionExpression.create(request.nestedAttributesToProject());
            projectionExpressionAsString = attributesToProject.projectionExpressionAsString().orElse(null);
            expressionNames = Expression.joinNames(expressionNames, attributesToProject.expressionAttributeNames());
        }

        SearchVectorsRequest.Builder searchVectorsRequest =
            SearchVectorsRequest.builder()
                                .tableName(context.tableName())
                                .indexName(context.indexName())
                                .searchVector(SearchVectorUtils.toSearchVector(
                                    request.searchVector()))
                                .returnConsumedCapacity(request.returnConsumedCapacityAsString());

        if (request.topK() != null) {
            searchVectorsRequest.topK(request.topK());
        }

        if (request.searchConditionExpression() != null) {
            searchVectorsRequest.searchConditionExpression(request.searchConditionExpression().expression());
            searchVectorsRequest.expressionAttributeValues(expressionValues);
        }

        if (expressionNames != null) {
            searchVectorsRequest.expressionAttributeNames(expressionNames);
        }

        if (projectionExpressionAsString != null) {
            searchVectorsRequest.projectionExpression(projectionExpressionAsString);
        }

        return searchVectorsRequest.build();
    }

    @Override
    public SearchVectorsEnhancedResponse<T> transformResponse(SearchVectorsResponse response,
                                                              TableSchema<T> tableSchema,
                                                              OperationContext context,
                                                              DynamoDbEnhancedClientExtension extension) {
        List<SearchResultItem<T>> results;
        if (CollectionUtils.isNullOrEmpty(response.searchResults())) {
            results = Collections.emptyList();
        } else {
            results = response.searchResults()
                              .stream()
                              .map(searchResult -> SearchResultItem.<T>builder()
                                                                   .item(EnhancedClientUtils.readAndTransformSingleItem(
                                                                       searchResult.item(),
                                                                       tableSchema,
                                                                       context,
                                                                       extension))
                                                                   .score(searchResult.score())
                                                                   .build())
                              .collect(Collectors.toList());
        }

        return SearchVectorsEnhancedResponse.<T>builder()
                                            .results(results)
                                            .consumedCapacity(response.consumedCapacity())
                                            .build();
    }

    @Override
    public Function<SearchVectorsRequest, SearchVectorsResponse> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::searchVectors;
    }

    @Override
    public Function<SearchVectorsRequest, CompletableFuture<SearchVectorsResponse>> asyncServiceCall(
        DynamoDbAsyncClient dynamoDbAsyncClient) {
        return dynamoDbAsyncClient::searchVectors;
    }
}
