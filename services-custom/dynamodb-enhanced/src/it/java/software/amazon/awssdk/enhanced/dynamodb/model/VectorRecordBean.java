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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSearchVectorsHashKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSearchVectorsInlineFilterKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbVectorAttribute;

/**
 * Annotated bean equivalent of {@link VectorRecord} for testing the bean-annotation-driven vector index path end-to-end. Uses a
 * single cosine vector index with HASH and INLINE_FILTER search schema elements.
 */
@DynamoDbBean
public class VectorRecordBean {
    private String pk;
    private String sk;
    private String category;
    private String description;
    private List<Float> embedding;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    @DynamoDbSearchVectorsHashKey(indexNames = {"cosine-index"})
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @DynamoDbSearchVectorsInlineFilterKey(indexNames = {"cosine-index"})
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDbVectorAttribute(indexName = "cosine-index", dimensions = 4, distanceFunction = DistanceFunction.COSINE)
    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VectorRecordBean that = (VectorRecordBean) o;
        return Objects.equals(pk, that.pk)
               && Objects.equals(sk, that.sk)
               && Objects.equals(category, that.category)
               && Objects.equals(description, that.description)
               && Objects.equals(embedding, that.embedding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, sk, category, description, embedding);
    }
}
