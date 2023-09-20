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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConsumedCapacity;
import software.amazon.awssdk.utils.ToString;

/**
 * An immutable object that holds a page of queried or scanned results from DynamoDb.
 * <p>
 * Contains a reference to the last evaluated key for the current page; see {@link #lastEvaluatedKey()} for more information.
 * @param <T> The modelled type of the object that has been read.
 */
@SdkPublicApi
@ThreadSafe
public final class Page<T> {
    private final List<T> items;
    private final Map<String, AttributeValue> lastEvaluatedKey;
    private final Integer count;
    private final Integer scannedCount;
    private final ConsumedCapacity consumedCapacity;

    private Page(List<T> items, Map<String, AttributeValue> lastEvaluatedKey) {
        this.items = items;
        this.lastEvaluatedKey = lastEvaluatedKey;
        this.count = null;
        this.scannedCount = null;
        this.consumedCapacity = null;
    }

    private Page(Builder<T> builder) {
        this.items = builder.items;
        this.lastEvaluatedKey = builder.lastEvaluatedKey;
        this.count = builder.count;
        this.scannedCount = builder.scannedCount;
        this.consumedCapacity = builder.consumedCapacity;
    }

    /**
     * Static constructor for this object. Deprecated in favor of using the builder() pattern to construct this object.
     *
     * @param items A list of items to store for the page.
     * @param lastEvaluatedKey A 'lastEvaluatedKey' to store for the page.
     * @param <T> The modelled type of the object that has been read.
     * @return A newly constructed {@link Page} object.
     */
    @Deprecated
    public static <T> Page<T> create(List<T> items, Map<String, AttributeValue> lastEvaluatedKey) {
        return new Page<>(items, lastEvaluatedKey);
    }

    /**
     * Static constructor for this object that sets a null 'lastEvaluatedKey' which indicates this is the final page
     * of results. Deprecated in favor of using the builder() pattern to construct this object.
     * @param items A list of items to store for the page.
     * @param <T> The modelled type of the object that has been read.
     * @return A newly constructed {@link Page} object.
     */
    @Deprecated
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

    /**
     * The count of the returned items from the last page query or scan, after any filters were applied.
     */
    public Integer count() {
        return count;
    }

    /**
     * The scanned count of the returned items from the last page query or scan, before any filters were applied.
     * This number will be equal or greater than the count.
     */
    public Integer scannedCount() {
        return scannedCount;
    }

    /**
     * Returns the capacity units consumed by the last page query or scan. Will only be returned if it has been
     * explicitly requested by the user when calling the operation.
     *
     * @return The 'consumedCapacity' from the last query or scan operation or null if it was not requested.
     */
    public ConsumedCapacity consumedCapacity() {
        return consumedCapacity;
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
        if (lastEvaluatedKey != null ? ! lastEvaluatedKey.equals(page.lastEvaluatedKey) : page.lastEvaluatedKey != null) {
            return false;
        }
        if (consumedCapacity != null ? ! consumedCapacity.equals(page.consumedCapacity) : page.consumedCapacity != null) {
            return false;
        }
        if (count != null ? ! count.equals(page.count) : page.count != null) {
            return false;
        }
        return scannedCount != null ? scannedCount.equals(page.scannedCount) : page.scannedCount == null;
    }

    @Override
    public int hashCode() {
        int result = items != null ? items.hashCode() : 0;
        result = 31 * result + (lastEvaluatedKey != null ? lastEvaluatedKey.hashCode() : 0);
        result = 31 * result + (consumedCapacity != null ? consumedCapacity.hashCode() : 0);
        result = 31 * result + (count != null ? count.hashCode() : 0);
        result = 31 * result + (scannedCount != null ? scannedCount.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("Page")
                       .add("lastEvaluatedKey", lastEvaluatedKey)
                       .add("items", items)
                       .build();
    }

    public static <T> Builder<T> builder(Class<T> itemClass) {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private List<T> items;
        private Map<String, AttributeValue> lastEvaluatedKey;
        private Integer count;
        private Integer scannedCount;
        private ConsumedCapacity consumedCapacity;

        public Builder<T> items(List<T> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        public Builder<T> lastEvaluatedKey(Map<String, AttributeValue> lastEvaluatedKey) {
            this.lastEvaluatedKey = new HashMap<>(lastEvaluatedKey);
            return this;
        }

        public Builder<T> count(Integer count) {
            this.count = count;
            return this;
        }

        public Builder<T> scannedCount(Integer scannedCount) {
            this.scannedCount = scannedCount;
            return this;
        }

        public Builder<T> consumedCapacity(ConsumedCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        public Page<T> build() {
            return new Page<T>(this);
        }
    }
}
