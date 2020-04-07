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

/**
 * Represents the result from paginated operations such as scan and query.
 * <p>
 * You can either subscribe to the {@link Page}s or flattened items across <b>all</b> pages via {@link #items()}.
 *
 * Example:
 * <p>
 * 1) Subscribing to {@link Page}s
 * <pre>
 * {@code
 *
 * PagePublisher<MyItem> publisher = mappedTable.scan();
 * publisher.subscribe(page -> page.items().forEach(item -> System.out.println(item)));
 * }
 * </pre>
 *
 * <p>
 * 2) Subscribing to items across all pages.
 * <pre>
 * {@code
 *
 * PagePublisher<<MyItem> publisher = mappedTable.scan();
 * publisher.items().subscribe(item -> System.out.println(item));
 * }
 * </pre>
 *
 * @param <T> The modelled type of the object in a page.
 */
@SdkPublicApi
public interface PagePublisher<T> extends SdkPublisher<Page<T>> {

    /**
     * Creates a flattened items publisher with the underlying page publisher.
     */
    static <T> PagePublisher<T> create(SdkPublisher<Page<T>> publisher) {
        return publisher::subscribe;
    }

    /**
     * Returns a publisher that can be used to request a stream of items across all pages.
     *
     * <p>
     * This method is useful if you are interested in subscribing the items in the response pages
     * instead of the top level pages.
     */
    default SdkPublisher<T> items() {
        return this.flatMapIterable(Page::items);
    }
}
