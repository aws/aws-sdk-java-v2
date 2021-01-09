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

package software.amazon.awssdk.enhanced.dynamodb.model;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.pagination.sync.PaginatedItemsIterable;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;

/**
 * Defines the result of {@link DynamoDbEnhancedClient#batchGetItem} operation.
 *
 * <p>
 * The result can be accessed either through iterable {@link BatchGetResultPage}s or flattened items
 * across <b>all</b>  pages via {@link #resultsForTable}
 *
 * <p>
 * Example:
 * <p>
 * 1) Iterating through pages
 *
 * <pre>
 * {@code
 * batchResults.forEach(page -> {
 *     page.resultsForTable(firstItemTable).forEach(item -> System.out.println(item));
 *     page.resultsForTable(secondItemTable).forEach(item -> System.out.println(item));
 * });
 * }
 * </pre>
 *
 * 2) Iterating through items across all pages
 *
 * <pre>
 * {@code
 * results.resultsForTable(firstItemTable).forEach(item -> System.out.println(item));
 * results.resultsForTable(secondItemTable).forEach(item -> System.out.println(item));
 * }
 * </pre>
 */
@SdkPublicApi
public interface BatchGetResultPageIterable extends SdkIterable<BatchGetResultPage> {

    static BatchGetResultPageIterable create(SdkIterable<BatchGetResultPage> pageIterable) {
        return pageIterable::iterator;
    }

    /**
     * Retrieve all items belonging to the supplied table across <b>all</b> pages.
     *
     * @param mappedTable the table to retrieve items for
     * @param <T> the type of the table items
     * @return iterable items
     */
    default <T> SdkIterable<T> resultsForTable(MappedTableResource<T> mappedTable) {
        return PaginatedItemsIterable.<BatchGetResultPage, T>builder()
            .pagesIterable(this)
            .itemIteratorFunction(page -> page.resultsForTable(mappedTable).iterator())
            .build();
    }
}
