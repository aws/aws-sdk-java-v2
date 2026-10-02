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
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSearchVectorsInlineFilterKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbVectorAttribute;

/**
 * Immutable equivalent of {@link InlineFilterOnlyVectorRecordBean} for end-to-end annotation testing.
 */
@DynamoDbImmutable(builder = InlineFilterOnlyVectorRecordImmutable.Builder.class)
public class InlineFilterOnlyVectorRecordImmutable {
    private final String pk;
    private final String sk;
    private final String category;
    private final List<Float> embedding;

    private InlineFilterOnlyVectorRecordImmutable(Builder b) {
        this.pk = b.pk;
        this.sk = b.sk;
        this.category = b.category;
        this.embedding = b.embedding;
    }

    @DynamoDbPartitionKey
    public String pk() {
        return pk;
    }

    @DynamoDbSortKey
    public String sk() {
        return sk;
    }

    @DynamoDbSearchVectorsInlineFilterKey(indexNames = {"filter-only-index"})
    public String category() {
        return category;
    }

    @DynamoDbVectorAttribute(indexName = "filter-only-index", dimensions = 4, distanceFunction = DistanceFunction.COSINE)
    public List<Float> embedding() {
        return embedding;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InlineFilterOnlyVectorRecordImmutable that = (InlineFilterOnlyVectorRecordImmutable) o;
        return Objects.equals(pk, that.pk)
               && Objects.equals(sk, that.sk)
               && Objects.equals(category, that.category)
               && Objects.equals(embedding, that.embedding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, sk, category, embedding);
    }

    public static final class Builder {
        private String pk;
        private String sk;
        private String category;
        private List<Float> embedding;

        private Builder() {
        }

        public Builder pk(String pk) {
            this.pk = pk;
            return this;
        }

        public Builder sk(String sk) {
            this.sk = sk;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder embedding(List<Float> embedding) {
            this.embedding = embedding;
            return this;
        }

        public InlineFilterOnlyVectorRecordImmutable build() {
            return new InlineFilterOnlyVectorRecordImmutable(this);
        }
    }
}
