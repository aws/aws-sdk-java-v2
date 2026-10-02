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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSearchVectorsHashKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSearchVectorsInlineFilterKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbVectorAttribute;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;

@DynamoDbImmutable(builder = VectorIndexImmutable.Builder.class)
public class VectorIndexImmutable {
    private final String id;
    private final String category;
    private final String brand;
    private final List<Float> embedding;
    private final AbstractBean innerBean;

    private VectorIndexImmutable(Builder b) {
        this.id = b.id;
        this.category = b.category;
        this.brand = b.brand;
        this.embedding = b.embedding;
        this.innerBean = b.innerBean;
    }

    @DynamoDbPartitionKey
    public String id() {
        return id;
    }

    @DynamoDbSearchVectorsHashKey(indexNames = {"embedding-index"})
    public String category() {
        return category;
    }

    @DynamoDbSearchVectorsInlineFilterKey(indexNames = {"embedding-index"})
    public String brand() {
        return brand;
    }

    @DynamoDbVectorAttribute(indexName = "embedding-index", dimensions = 1536, distanceFunction = DistanceFunction.COSINE)
    public List<Float> embedding() {
        return embedding;
    }

    public AbstractBean innerBean() {
        return innerBean;
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
        VectorIndexImmutable that = (VectorIndexImmutable) o;
        return Objects.equals(id, that.id)
               && Objects.equals(category, that.category)
               && Objects.equals(brand, that.brand)
               && Objects.equals(embedding, that.embedding)
               && Objects.equals(innerBean, that.innerBean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, category, brand, embedding, innerBean);
    }

    public static final class Builder {
        private String id;
        private String category;
        private String brand;
        private List<Float> embedding;
        private AbstractBean innerBean;

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder brand(String brand) {
            this.brand = brand;
            return this;
        }

        public Builder embedding(List<Float> embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder innerBean(AbstractBean innerBean) {
            this.innerBean = innerBean;
            return this;
        }

        public VectorIndexImmutable build() {
            return new VectorIndexImmutable(this);
        }
    }
}
