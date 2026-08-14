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

import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * A single vector search hit containing a mapped item and its similarity score.
 *
 * @param <T> The type of the mapped item.
 */
@SdkPublicApi
@ThreadSafe
public final class SearchResultItem<T> {
    private final T item;
    private final Double score;

    private SearchResultItem(Builder<T> builder) {
        this.item = builder.item;
        this.score = builder.score;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * The mapped item returned by the search operation.
     */
    public T item() {
        return item;
    }

    /**
     * The similarity score for this item, as returned by the service.
     */
    public Double score() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchResultItem<?> that = (SearchResultItem<?>) o;
        return Objects.equals(score, that.score) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(item);
        result = 31 * result + Objects.hashCode(score);
        return result;
    }

    /**
     * A builder for {@link SearchResultItem}.
     *
     * @param <T> The type of the mapped item.
     */
    @NotThreadSafe
    public static final class Builder<T> {
        private T item;
        private Double score;

        private Builder() {
        }

        public Builder<T> item(T item) {
            this.item = item;
            return this;
        }

        public Builder<T> score(Double score) {
            this.score = score;
            return this;
        }

        public SearchResultItem<T> build() {
            return new SearchResultItem<>(this);
        }
    }
}
