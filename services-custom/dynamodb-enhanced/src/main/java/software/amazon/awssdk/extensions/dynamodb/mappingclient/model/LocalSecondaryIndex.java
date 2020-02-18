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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.model;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.Projection;

@SdkPublicApi
public final class LocalSecondaryIndex {
    private final String indexName;
    private final Projection projection;

    private LocalSecondaryIndex(Builder builder) {
        this.indexName = builder.indexName;
        this.projection = builder.projection;
    }

    public static LocalSecondaryIndex create(String indexName,
                                             Projection projection) {

        return builder().indexName(indexName).projection(projection).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().indexName(indexName).projection(projection);
    }

    public String indexName() {
        return indexName;
    }

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

        LocalSecondaryIndex that = (LocalSecondaryIndex) o;

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

    public static final class Builder {
        private String indexName;
        private Projection projection;

        private Builder() {
        }

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder projection(Projection projection) {
            this.projection = projection;
            return this;
        }

        public LocalSecondaryIndex build() {
            return new LocalSecondaryIndex(this);
        }
    }
}
