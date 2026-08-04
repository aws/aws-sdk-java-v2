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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.dynamodb.model.Projection;

/**
 * Enhanced model representation of a vector index on a DynamoDb table. This is optionally used with the {@code createTable}
 * operation in the enhanced client.
 * <p>
 * Vector indexes are distinct from global and local secondary indexes; they define vector search configuration including
 * embedding dimensions, distance function, and search schema elements.
 */
@SdkPublicApi
@ThreadSafe
public final class EnhancedVectorIndex {
    private final String indexName;
    private final String vectorAttributeName;
    private final int dimensions;
    private final DistanceFunction distanceFunction;
    private final Projection projection;
    private final List<SearchSchemaElement> searchSchemaElements;

    private EnhancedVectorIndex(Builder builder) {
        this.indexName = builder.indexName;
        this.vectorAttributeName = builder.vectorAttributeName;
        this.dimensions = builder.dimensions;
        this.distanceFunction = builder.distanceFunction;
        this.projection = builder.projection;
        if (builder.searchSchemaElements == null) {
            this.searchSchemaElements = Collections.emptyList();
        } else {
            this.searchSchemaElements = Collections.unmodifiableList(new ArrayList<>(builder.searchSchemaElements));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().indexName(indexName)
                        .vectorAttributeName(vectorAttributeName)
                        .dimensions(dimensions)
                        .distanceFunction(distanceFunction)
                        .projection(projection)
                        .searchSchemaElements(searchSchemaElements);
    }

    /**
     * The name of the vector index.
     */
    public String indexName() {
        return indexName;
    }

    /**
     * The name of the table attribute that stores the vector embedding for this index.
     */
    public String vectorAttributeName() {
        return vectorAttributeName;
    }

    /**
     * The number of dimensions in the vector embedding.
     */
    public int dimensions() {
        return dimensions;
    }

    /**
     * The distance function used for similarity search on this index.
     */
    public DistanceFunction distanceFunction() {
        return distanceFunction;
    }

    /**
     * The attribute projection setting for this vector index.
     */
    public Projection projection() {
        return projection;
    }

    /**
     * The search schema elements for this vector index, including the hash attribute and optional inline filters.
     */
    public List<SearchSchemaElement> searchSchemaElements() {
        return searchSchemaElements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EnhancedVectorIndex that = (EnhancedVectorIndex) o;
        return dimensions == that.dimensions
               && Objects.equals(indexName, that.indexName)
               && Objects.equals(vectorAttributeName, that.vectorAttributeName)
               && distanceFunction == that.distanceFunction
               && Objects.equals(projection, that.projection)
               && Objects.equals(searchSchemaElements, that.searchSchemaElements);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(indexName);
        result = 31 * result + Objects.hashCode(vectorAttributeName);
        result = 31 * result + Objects.hashCode(dimensions);
        result = 31 * result + Objects.hashCode(distanceFunction);
        result = 31 * result + Objects.hashCode(projection);
        result = 31 * result + Objects.hashCode(searchSchemaElements);
        return result;
    }

    /**
     * A builder for {@link EnhancedVectorIndex}.
     */
    @NotThreadSafe
    public static final class Builder {
        private String indexName;
        private String vectorAttributeName;
        private int dimensions;
        private DistanceFunction distanceFunction;
        private Projection projection;
        private List<SearchSchemaElement> searchSchemaElements;

        private Builder() {
        }

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder vectorAttributeName(String vectorAttributeName) {
            this.vectorAttributeName = vectorAttributeName;
            return this;
        }

        public Builder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder distanceFunction(DistanceFunction distanceFunction) {
            this.distanceFunction = distanceFunction;
            return this;
        }

        public Builder projection(Projection projection) {
            this.projection = projection;
            return this;
        }

        public Builder projection(Consumer<Projection.Builder> projection) {
            Projection.Builder builder = Projection.builder();
            projection.accept(builder);
            return projection(builder.build());
        }

        public Builder searchSchemaElements(List<SearchSchemaElement> searchSchemaElements) {
            this.searchSchemaElements = searchSchemaElements;
            return this;
        }

        public Builder searchSchemaElements(SearchSchemaElement... searchSchemaElements) {
            this.searchSchemaElements = new ArrayList<>();
            Collections.addAll(this.searchSchemaElements, searchSchemaElements);
            return this;
        }

        public Builder addSearchSchemaElement(SearchSchemaElement searchSchemaElement) {
            if (this.searchSchemaElements == null) {
                this.searchSchemaElements = new ArrayList<>();
            }
            this.searchSchemaElements.add(searchSchemaElement);
            return this;
        }

        public Builder addSearchSchemaElement(Consumer<SearchSchemaElement.Builder> searchSchemaElement) {
            SearchSchemaElement.Builder builder = SearchSchemaElement.builder();
            searchSchemaElement.accept(builder);
            return addSearchSchemaElement(builder.build());
        }

        public EnhancedVectorIndex build() {
            return new EnhancedVectorIndex(this);
        }
    }
}
