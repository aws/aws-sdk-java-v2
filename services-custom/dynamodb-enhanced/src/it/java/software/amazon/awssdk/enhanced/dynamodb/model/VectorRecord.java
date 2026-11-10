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

public class VectorRecord {
    private String pk;
    private String sk;
    private String category;
    private String description;
    private List<Float> embeddingCosine;
    private List<Float> embeddingDot;
    private List<Float> embeddingEuclidean;

    public String getPk() {
        return pk;
    }

    public VectorRecord setPk(String pk) {
        this.pk = pk;
        return this;
    }

    public String getSk() {
        return sk;
    }

    public VectorRecord setSk(String sk) {
        this.sk = sk;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public VectorRecord setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VectorRecord setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<Float> getEmbeddingCosine() {
        return embeddingCosine;
    }

    public VectorRecord setEmbeddingCosine(List<Float> embeddingCosine) {
        this.embeddingCosine = embeddingCosine;
        return this;
    }

    public List<Float> getEmbeddingDot() {
        return embeddingDot;
    }

    public VectorRecord setEmbeddingDot(List<Float> embeddingDot) {
        this.embeddingDot = embeddingDot;
        return this;
    }

    public List<Float> getEmbeddingEuclidean() {
        return embeddingEuclidean;
    }

    public VectorRecord setEmbeddingEuclidean(List<Float> embeddingEuclidean) {
        this.embeddingEuclidean = embeddingEuclidean;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VectorRecord that = (VectorRecord) o;
        return Objects.equals(pk, that.pk)
               && Objects.equals(sk, that.sk)
               && Objects.equals(category, that.category)
               && Objects.equals(description, that.description)
               && Objects.equals(embeddingCosine, that.embeddingCosine)
               && Objects.equals(embeddingDot, that.embeddingDot)
               && Objects.equals(embeddingEuclidean, that.embeddingEuclidean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pk, sk, category, description, embeddingCosine, embeddingDot, embeddingEuclidean);
    }
}
