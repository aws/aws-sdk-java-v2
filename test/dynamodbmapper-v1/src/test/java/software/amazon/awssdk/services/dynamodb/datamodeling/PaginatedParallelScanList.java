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

import java.util.LinkedList;
import java.util.List;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.PaginationLoadingStrategy;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

/**
 * Implementation of the List interface that represents the results from a parallel scan
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
public class PaginatedParallelScanList<T> extends PaginatedList<T> {

    /** The current parallel scan task which contains all the information about the scan request. */
    private final ParallelScanTask parallelScanTask;

    private final DynamoDbMapperConfig config;

    public PaginatedParallelScanList(
            DynamoDbMapper mapper,
            Class<T> clazz,
            DynamoDbClient dynamo,
            ParallelScanTask parallelScanTask,
            PaginationLoadingStrategy paginationLoadingStrategy,
            DynamoDbMapperConfig config) {
        super(mapper, clazz, dynamo, paginationLoadingStrategy);

        this.parallelScanTask = parallelScanTask;
        this.config = config;

        // Marshall the first batch of results in all Results
        allResults.addAll(marshalParallelScanResponsesIntoObjects(parallelScanTask.nextBatchOfScanResponses()));

        // If the results should be eagerly loaded at once
        if (paginationLoadingStrategy == PaginationLoadingStrategy.EAGER_LOADING) {
            loadAllResults();
        }
    }

    @Override
    protected boolean atEndOfResults() {
        return parallelScanTask.isAllSegmentScanFinished();
    }

    @Override
    protected List<T> fetchNextPage() {
        return marshalParallelScanResponsesIntoObjects(parallelScanTask.nextBatchOfScanResponses());
    }

    private List<T> marshalParallelScanResponsesIntoObjects(List<ScanResponse> scanResults) {
        List<T> allItems = new LinkedList<T>();
        for (ScanResponse scanResult : scanResults) {
            if (null != scanResult) {
                allItems.addAll(mapper.marshallIntoObjects(
                        mapper.toParameters(
                                scanResult.items(),
                                clazz,
                                parallelScanTask.getTableName(),
                                config)));
            }
        }
        return allItems;
    }
}
