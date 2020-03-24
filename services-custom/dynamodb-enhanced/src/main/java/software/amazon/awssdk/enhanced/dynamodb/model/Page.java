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

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An immutable object that holds a page of queried or scanned results from DynamoDb.
 * <p>
 * Contains a reference to the last evaluated key for the current page; see {@link #lastEvaluatedKey()} for more information.
 * @param <T> The modelled type of the object that has been read.
 */
@SdkPublicApi
public final class Page<T> {
    private final List<T> items;
    private final Map<String, AttributeValue> lastEvaluatedKey;

    private Page(List<T> items, Map<String, AttributeValue> lastEvaluatedKey) {
        this.items = items;
        this.lastEvaluatedKey = lastEvaluatedKey;
    }

    /**
     * Static constructor for this object.
     * @param items A list of items to store for the page.
     * @param lastEvaluatedKey A 'lastEvaluatedKey' to store for the page.
     * @param <T> The modelled type of the object that has been read.
     * @return A newly constructed {@link Page} object.
     */
    public static <T> Page<T> create(List<T> items, Map<String, AttributeValue> lastEvaluatedKey) {
        return new Page<>(items, lastEvaluatedKey);
    }

    /**
     * Static constructor for this object that sets a null 'lastEvaluatedKey' which indicates this is the final page
     * of results.
     * @param items A list of items to store for the page.
     * @param <T> The modelled type of the object that has been read.
     * @return A newly constructed {@link Page} object.
     */
    public static <T> Page<T> create(List<T> items) {
        return new Page<>(items, null);
    }

    /**
     * Returns a page of mapped objects that represent records from a database query or scan.
     * @return A list of mapped objects.
     */
    public List<T> items() {
        return items;
    }

    /**
     * Returns the 'lastEvaluatedKey' that DynamoDb returned from the last page query or scan. This key can be used
     * to continue the query or scan if passed into a request.
     * @return The 'lastEvaluatedKey' from the last query or scan operation or null if the no more pages are available.
     */
    public Map<String, AttributeValue> lastEvaluatedKey() {
        return lastEvaluatedKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Page<?> page = (Page<?>) o;

        if (items != null ? ! items.equals(page.items) : page.items != null) {
            return false;
        }
        return lastEvaluatedKey != null ? lastEvaluatedKey.equals(page.lastEvaluatedKey) : page.lastEvaluatedKey == null;
    }

    @Override
    public int hashCode() {
        int result = items != null ? items.hashCode() : 0;
        result = 31 * result + (lastEvaluatedKey != null ? lastEvaluatedKey.hashCode() : 0);
        return result;
    }
}
