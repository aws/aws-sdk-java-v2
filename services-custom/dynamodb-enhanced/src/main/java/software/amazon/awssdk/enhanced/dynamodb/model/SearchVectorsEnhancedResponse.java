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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.model.VectorCapacity;

/**
 * Response from a vector search when using {@code searchVectorsWithResponse} on a vector index handle.
 *
 * @param <T> The type of the mapped items in the search results.
 */
@SdkPublicApi
@ThreadSafe
public final class SearchVectorsEnhancedResponse<T> {
    private final List<SearchResultItem<T>> results;
    private final VectorCapacity consumedCapacity;

    private SearchVectorsEnhancedResponse(Builder<T> builder) {
        if (builder.results == null) {
            this.results = Collections.emptyList();
        } else {
            this.results = Collections.unmodifiableList(new ArrayList<>(builder.results));
        }
        this.consumedCapacity = builder.consumedCapacity;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * The search hits, each containing a mapped item and similarity score.
     */
    public List<SearchResultItem<T>> results() {
        return results;
    }

    /**
     * Vector search capacity consumed by the operation, when returned by the service.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.SearchVectorsResponse#consumedCapacity() for more information.
     */
    public VectorCapacity consumedCapacity() {
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
        SearchVectorsEnhancedResponse<?> that = (SearchVectorsEnhancedResponse<?>) o;
        return Objects.equals(results, that.results) && Objects.equals(consumedCapacity, that.consumedCapacity);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(results);
        result = 31 * result + Objects.hashCode(consumedCapacity);
        return result;
    }

    /**
     * A builder for {@link SearchVectorsEnhancedResponse}.
     *
     * @param <T> The type of the mapped items in the search results.
     */
    @NotThreadSafe
    public static final class Builder<T> {
        private List<SearchResultItem<T>> results;
        private VectorCapacity consumedCapacity;

        private Builder() {
        }

        public Builder<T> results(List<SearchResultItem<T>> results) {
            this.results = results;
            return this;
        }

        public Builder<T> addResult(SearchResultItem<T> result) {
            if (this.results == null) {
                this.results = new ArrayList<>();
            }
            this.results.add(result);
            return this;
        }

        public Builder<T> consumedCapacity(VectorCapacity consumedCapacity) {
            this.consumedCapacity = consumedCapacity;
            return this;
        }

        public SearchVectorsEnhancedResponse<T> build() {
            return new SearchVectorsEnhancedResponse<>(this);
        }
    }
}
