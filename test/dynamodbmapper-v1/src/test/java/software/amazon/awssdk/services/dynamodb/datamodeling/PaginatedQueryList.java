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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.util.List;

import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.PaginationLoadingStrategy;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/**
 * Implementation of the List interface that represents the results from a query
 * in AWS DynamoDB. Paginated results are loaded on demand when the user
 * executes an operation that requires them. Some operations, such as size(),
 * must fetch the entire list, but results are lazily fetched page by page when
 * possible.
 * <p>
 * This is an unmodifiable list, so callers should not invoke any operations
 * that modify this list, otherwise they will throw an
 * UnsupportedOperationException.
 *
 * @param <T>
 *            The type of objects held in this list.
 * @see PaginatedList
 */
public class PaginatedQueryList<T> extends PaginatedList<T> {

    /** The current query request. */
    private QueryRequest queryRequest;

    private final DynamoDbMapperConfig config;

    /** The current results for the last executed query operation. */
    private QueryResponse queryResult;

    public PaginatedQueryList(
            DynamoDbMapper mapper,
            Class<T> clazz,
            DynamoDbClient dynamo,
            QueryRequest queryRequest,
            QueryResponse queryResult,
            PaginationLoadingStrategy paginationLoadingStrategy,
            DynamoDbMapperConfig config) {
        super(mapper, clazz, dynamo, paginationLoadingStrategy);

        this.queryRequest = queryRequest;
        this.queryResult = queryResult;
        this.config = config;


        allResults.addAll(mapper.marshallIntoObjects(
                mapper.toParameters(
                        queryResult.items(),
                        clazz,
                        queryRequest.tableName(),
                        config)));

        // If the results should be eagerly loaded at once
        if (paginationLoadingStrategy == PaginationLoadingStrategy.EAGER_LOADING) {
            loadAllResults();
        }
    }

    @Override
    protected synchronized boolean atEndOfResults() {
        return queryResult.lastEvaluatedKey() instanceof SdkAutoConstructMap;
    }

    @Override
    protected synchronized List<T> fetchNextPage() {
        queryRequest = queryRequest.toBuilder().exclusiveStartKey(queryResult.lastEvaluatedKey()).build();
        queryResult = dynamo.query(DynamoDbMapper.applyUserAgent(queryRequest));
        return mapper.marshallIntoObjects(mapper.toParameters(
                queryResult.items(),
                clazz,
                queryRequest.tableName(),
                config));
    }
}
