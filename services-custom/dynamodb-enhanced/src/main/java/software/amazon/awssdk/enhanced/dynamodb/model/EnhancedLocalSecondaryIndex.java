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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.utils.Validate;

/**
 * Enhanced model representation of a 'local secondary index' of a DynamoDb table. This is optionally used with the
 * 'createTable' operation in the enhanced client.
 */
@SdkPublicApi
public final class EnhancedLocalSecondaryIndex {
    private final String indexName;
    private final Projection projection;

    private EnhancedLocalSecondaryIndex(Builder builder) {
        this.indexName = Validate.paramNotBlank(builder.indexName, "indexName");
        this.projection = builder.projection;
    }

    public static EnhancedLocalSecondaryIndex create(String indexName,
                                                     Projection projection) {

        return builder().indexName(indexName).projection(projection).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().indexName(indexName).projection(projection);
    }

    /**
     * The name of this local secondary index
     */
    public String indexName() {
        return indexName;
    }

    /**
     * The attribute projection setting for this local secondary index.
     */
    public Projection projection() {
        return projection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnhancedLocalSecondaryIndex that = (EnhancedLocalSecondaryIndex) o;

        if (indexName != null ? ! indexName.equals(that.indexName) : that.indexName != null) {
            return false;
        }
        return projection != null ? projection.equals(that.projection) : that.projection == null;
    }

    @Override
    public int hashCode() {
        int result = indexName != null ? indexName.hashCode() : 0;
        result = 31 * result + (projection != null ? projection.hashCode() : 0);
        return result;
    }

    /**
     * A builder for {@link EnhancedLocalSecondaryIndex}
     */
    public static final class Builder {
        private String indexName;
        private Projection projection;

        private Builder() {
        }

        /**
         * The name of this local secondary index
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The attribute projection setting for this local secondary index.
         */
        public Builder projection(Projection projection) {
            this.projection = projection;
            return this;
        }

        /**
         * The attribute projection setting for this local secondary index.
         */
        public Builder projection(Consumer<Projection.Builder> projection) {
            Projection.Builder builder = Projection.builder();
            projection.accept(builder);
            return projection(builder.build());
        }

        public EnhancedLocalSecondaryIndex build() {
            return new EnhancedLocalSecondaryIndex(this);
        }
    }
}
