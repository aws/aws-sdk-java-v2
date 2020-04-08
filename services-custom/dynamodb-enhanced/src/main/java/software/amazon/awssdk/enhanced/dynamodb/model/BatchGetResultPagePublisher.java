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
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.MappedTableResource;

/**
 * Defines the result of {@link DynamoDbEnhancedAsyncClient#batchGetItem} operation.
 *
 * <p>
 * You can either subscribe to the {@link BatchGetResultPage}s or flattened items across <b>all</b> pages via
 * {@link #resultsForTable(MappedTableResource)}.
 *
 * Example:
 * <p>
 * 1) Subscribing to {@link BatchGetResultPage}s
 * <pre>
 * {@code
 * batchGetResultPagePublisher.subscribe(page -> {
 *     page.resultsForTable(firstItemTable).forEach(item -> System.out.println(item));
 *     page.resultsForTable(secondItemTable).forEach(item -> System.out.println(item));
 * });
 * }
 * </pre>
 *
 * <p>
 * 2) Subscribing to results across all pages.
 * <pre>
 * {@code
 * batchGetResultPagePublisher.resultsForTable(firstItemTable).subscribe(item -> System.out.println(item));
 * batchGetResultPagePublisher.resultsForTable(secondItemTable).subscribe(item -> System.out.println(item));
 * }
 * </pre>
 */
@SdkPublicApi
public interface BatchGetResultPagePublisher extends SdkPublisher<BatchGetResultPage> {

    /**
     * Creates a flattened items publisher with the underlying page publisher.
     */
    static BatchGetResultPagePublisher create(SdkPublisher<BatchGetResultPage> publisher) {
        return publisher::subscribe;
    }

    /**
     * Returns a publisher that can be used to request a stream of results belonging to the supplied table across all pages.
     *
     * <p>
     * This method is useful if you are interested in subscribing to the items in all response pages
     * instead of the top level pages.
     *
     * @param mappedTable the table to retrieve items for
     * @param <T> the type of the table items
     * @return a {@link SdkPublisher}
     */
    default <T> SdkPublisher<T> resultsForTable(MappedTableResource<T> mappedTable) {
        return this.flatMapIterable(p -> p.resultsForTable(mappedTable));
    }
}
