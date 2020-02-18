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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.operations;

import static software.amazon.awssdk.extensions.dynamodb.mappingclient.core.Utils.readAndTransformPaginatedItems;

import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Expression;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.MapperExtension;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.OperationContext;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.Page;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.PaginatedIndexOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.PaginatedTableOperation;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableMetadata;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.TableSchema;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@SdkInternalApi
public class QueryOperation<T> implements PaginatedTableOperation<T, QueryRequest, QueryResponse, Page<T>>,
                                          PaginatedIndexOperation<T, QueryRequest, QueryResponse, Page<T>> {

    private final QueryEnhancedRequest request;

    private QueryOperation(QueryEnhancedRequest request) {
        this.request = request;
    }

    public static <T> QueryOperation<T> create(QueryEnhancedRequest request) {
        return new QueryOperation<>(request);
    }

    @Override
    public QueryRequest generateRequest(TableSchema<T> tableSchema,
                                        OperationContext operationContext,
                                        MapperExtension mapperExtension) {
        Expression queryExpression = this.request.queryConditional().expression(tableSchema, operationContext.indexName());
        Map<String, AttributeValue> expressionValues = queryExpression.expressionValues();
        Map<String, String> expressionNames = queryExpression.expressionNames();

        if (this.request.filterExpression() != null) {
            expressionValues = Expression.coalesceValues(expressionValues, this.request.filterExpression().expressionValues());
            expressionNames = Expression.coalesceNames(expressionNames, this.request.filterExpression().expressionNames());
        }

        QueryRequest.Builder queryRequest = QueryRequest.builder()
                                                        .tableName(operationContext.tableName())
                                                        .keyConditionExpression(queryExpression.expression())
                                                        .expressionAttributeValues(expressionValues)
                                                        .expressionAttributeNames(expressionNames)
                                                        .scanIndexForward(this.request.scanIndexForward())
                                                        .limit(this.request.limit())
                                                        .exclusiveStartKey(this.request.exclusiveStartKey())
                                                        .consistentRead(this.request.consistentRead());

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            queryRequest = queryRequest.indexName(operationContext.indexName());
        }

        if (this.request.filterExpression() != null) {
            queryRequest = queryRequest.filterExpression(this.request.filterExpression().expression());
        }

        return queryRequest.build();
    }

    @Override
    public Function<QueryRequest, SdkIterable<QueryResponse>> serviceCall(DynamoDbClient dynamoDbClient) {
        return dynamoDbClient::queryPaginator;
    }

    @Override
    public Function<QueryRequest, SdkPublisher<QueryResponse>> asyncServiceCall(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return dynamoDbAsyncClient::queryPaginator;
    }

    @Override
    public Page<T> transformResponse(QueryResponse response,
                                     TableSchema<T> tableSchema,
                                     OperationContext context,
                                     MapperExtension mapperExtension) {

        return readAndTransformPaginatedItems(response,
                                              tableSchema,
                                              context,
                                              mapperExtension,
                                              QueryResponse::items,
                                              QueryResponse::lastEvaluatedKey);
    }

}
