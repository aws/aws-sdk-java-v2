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

/**
 * Page iterable represents the result from paginated operations such as scan and query.
 *
 * <p>
 * The result can be accessed either through iterable {@link Page}s or flattened items across <b>all</b>  pages via
 * {@link #items()}
 *
 * <p>
 * Example:
 * <p>
 * 1) Iterating through pages
 *
 * <pre>
 * {@code
 * PageIterable<MyItem> results = table.scan();
 * results.stream().forEach(p -> p.items().forEach(item -> System.out.println(item)))
 * }
 * </pre>
 *
 * 2) Iterating through items
 *
 * <pre>
 * {@code
 * PageIterable<MyItem> results = table.scan();
 * results.items().stream().forEach(item -> System.out.println(item));
 * }
 * </pre>
 * @param <T> The modelled type of the object in a page.
 */
@SdkPublicApi
public interface PageIterable<T> extends SdkIterable<Page<T>> {

    static <T> PageIterable<T> create(SdkIterable<Page<T>> pageIterable) {
        return pageIterable::iterator;
    }

    /**
     * Returns an iterable to iterate through the paginated {@link Page#items()} across <b>all</b> response pages.
     *
     * <p>
     * This method is useful if you are interested in iterating over the items in the response pages
     * instead of the top level pages.
     */
    default SdkIterable<T> items() {
        return PaginatedItemsIterable.<Page<T>, T>builder()
            .pagesIterable(this)
            .itemIteratorFunction(page -> page.items().iterator())
            .build();
    }
}
