/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.annotations.SdkPublicApi;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@SdkPublicApi
public class Query<T> implements PaginatedTableOperation<T, QueryRequest, QueryResponse, Page<T>>,
                                 PaginatedIndexOperation<T, QueryRequest, QueryResponse, Page<T>> {

    private final QueryConditional queryConditional;
    private final Map<String, AttributeValue> exclusiveStartKey;
    private final Boolean scanIndexForward;
    private final Integer limit;
    private final Boolean consistentRead;
    private final Expression filterExpression;


    private Query(QueryConditional queryConditional,
                  Map<String, AttributeValue> exclusiveStartKey,
                  Boolean scanIndexForward,
                  Integer limit,
                  Boolean consistentRead,
                  Expression filterExpression) {
        this.queryConditional = queryConditional;
        this.exclusiveStartKey = exclusiveStartKey;
        this.scanIndexForward = scanIndexForward;
        this.limit = limit;
        this.consistentRead = consistentRead;
        this.filterExpression = filterExpression;
    }

    public static <T> Query<T> create(QueryConditional queryConditional) {
        return new Query<>(queryConditional, null, null, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().queryConditional(queryConditional)
                            .exclusiveStartKey(exclusiveStartKey)
                            .scanIndexForward(scanIndexForward)
                            .limit(limit)
                            .consistentRead(consistentRead)
                            .filterExpression(filterExpression);
    }

    @Override
    public QueryRequest generateRequest(TableSchema<T> tableSchema,
                                        OperationContext operationContext,
                                        MapperExtension mapperExtension) {
        Expression queryExpression = queryConditional.expression(tableSchema, operationContext.indexName());
        Map<String, AttributeValue> expressionValues = queryExpression.expressionValues();
        Map<String, String> expressionNames = queryExpression.expressionNames();

        if (filterExpression != null) {
            expressionValues = Expression.coalesceValues(expressionValues, filterExpression.expressionValues());
            expressionNames = Expression.coalesceNames(expressionNames, filterExpression.expressionNames());
        }

        QueryRequest.Builder queryRequest = QueryRequest.builder()
                                                        .tableName(operationContext.tableName())
                                                        .keyConditionExpression(queryExpression.expression())
                                                        .expressionAttributeValues(expressionValues)
                                                        .expressionAttributeNames(expressionNames)
                                                        .scanIndexForward(scanIndexForward)
                                                        .limit(limit)
                                                        .exclusiveStartKey(exclusiveStartKey)
                                                        .consistentRead(consistentRead);

        if (!TableMetadata.primaryIndexName().equals(operationContext.indexName())) {
            queryRequest = queryRequest.indexName(operationContext.indexName());
        }

        if (filterExpression != null) {
            queryRequest = queryRequest.filterExpression(filterExpression.expression());
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

    public QueryConditional queryConditional() {
        return queryConditional;
    }

    public Map<String, AttributeValue> exclusiveStartKey() {
        return exclusiveStartKey;
    }

    public Boolean scanIndexForward() {
        return scanIndexForward;
    }

    public Integer limit() {
        return limit;
    }

    public Boolean consistentRead() {
        return consistentRead;
    }

    public Expression filterExpression() {
        return filterExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Query<?> query = (Query<?>) o;

        if (queryConditional != null ? ! queryConditional.equals(query.queryConditional) :
            query.queryConditional != null) {
            return false;
        }
        if (exclusiveStartKey != null ? ! exclusiveStartKey.equals(query.exclusiveStartKey) :
            query.exclusiveStartKey != null) {
            return false;
        }
        if (scanIndexForward != null ? ! scanIndexForward.equals(query.scanIndexForward) :
            query.scanIndexForward != null) {
            return false;
        }
        if (limit != null ? ! limit.equals(query.limit) : query.limit != null) {
            return false;
        }
        if (consistentRead != null ? ! consistentRead.equals(query.consistentRead) : query.consistentRead != null) {
            return false;
        }
        return filterExpression != null ? filterExpression.equals(query.filterExpression) : query.filterExpression == null;
    }

    @Override
    public int hashCode() {
        int result = queryConditional != null ? queryConditional.hashCode() : 0;
        result = 31 * result + (exclusiveStartKey != null ? exclusiveStartKey.hashCode() : 0);
        result = 31 * result + (scanIndexForward != null ? scanIndexForward.hashCode() : 0);
        result = 31 * result + (limit != null ? limit.hashCode() : 0);
        result = 31 * result + (consistentRead != null ? consistentRead.hashCode() : 0);
        result = 31 * result + (filterExpression != null ? filterExpression.hashCode() : 0);
        return result;
    }

    public static final class Builder {
        private QueryConditional queryConditional;
        private Map<String, AttributeValue> exclusiveStartKey;
        private Boolean scanIndexForward;
        private Integer limit;
        private Boolean consistentRead;
        private Expression filterExpression;

        private Builder() {
        }

        public <T> Query<T> build() {
            return new Query<>(queryConditional,
                               exclusiveStartKey,
                               scanIndexForward,
                               limit,
                               consistentRead,
                               filterExpression);
        }

        public Builder queryConditional(QueryConditional queryConditional) {
            this.queryConditional = queryConditional;
            return this;
        }

        public Builder scanIndexForward(Boolean scanIndexForward) {
            this.scanIndexForward = scanIndexForward;
            return this;
        }

        public Builder exclusiveStartKey(Map<String, AttributeValue> exclusiveStartKey) {
            this.exclusiveStartKey = exclusiveStartKey;
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder consistentRead(Boolean consistentRead) {
            this.consistentRead = consistentRead;
            return this;
        }

        public Builder filterExpression(Expression filterExpression) {
            this.filterExpression = filterExpression;
            return this;
        }
    }
}
