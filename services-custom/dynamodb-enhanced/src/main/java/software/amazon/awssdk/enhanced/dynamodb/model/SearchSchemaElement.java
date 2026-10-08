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
 * Enhanced model representation of one element in a vector index search schema. Used with {@link EnhancedVectorIndex} when
 * creating a table that includes vector indexes.
 */
@SdkPublicApi
@ThreadSafe
public final class SearchSchemaElement {
    private final String attributeName;
    private final SearchSchemaElementType searchSchemaElementType;

    private SearchSchemaElement(Builder builder) {
        this.attributeName = builder.attributeName;
        this.searchSchemaElementType = builder.searchSchemaElementType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().attributeName(attributeName)
                        .searchSchemaElementType(searchSchemaElementType);
    }

    /**
     * The name of the table attribute referenced by this search schema element.
     */
    public String attributeName() {
        return attributeName;
    }

    /**
     * Whether this element is a hash key for vector search or an inline filter attribute.
     */
    public SearchSchemaElementType searchSchemaElementType() {
        return searchSchemaElementType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchSchemaElement that = (SearchSchemaElement) o;
        return Objects.equals(attributeName, that.attributeName)
               && searchSchemaElementType == that.searchSchemaElementType;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(attributeName);
        result = 31 * result + Objects.hashCode(searchSchemaElementType);
        return result;
    }

    /**
     * A builder for {@link SearchSchemaElement}.
     */
    @NotThreadSafe
    public static final class Builder {
        private String attributeName;
        private SearchSchemaElementType searchSchemaElementType;

        private Builder() {
        }

        public Builder attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        public Builder searchSchemaElementType(SearchSchemaElementType searchSchemaElementType) {
            this.searchSchemaElementType = searchSchemaElementType;
            return this;
        }

        public SearchSchemaElement build() {
            return new SearchSchemaElement(this);
        }
    }
}
