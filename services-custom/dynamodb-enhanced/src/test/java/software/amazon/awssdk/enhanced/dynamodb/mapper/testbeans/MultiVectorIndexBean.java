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
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSearchVectorsHashKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbVectorAttribute;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;

@DynamoDbBean
public class MultiVectorIndexBean {
    private String id;
    private String category;
    private List<Float> embeddingCosine;
    private List<Float> embeddingDot;
    private List<Float> embeddingEuclidean;
    private List<Float> embeddingCosine2;
    private List<Float> embeddingDot2;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbSearchVectorsHashKey(indexNames = {"cosine-idx", "dot-idx", "euclidean-idx", "cosine-idx-2", "dot-idx-2"})
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @DynamoDbVectorAttribute(indexName = "cosine-idx", dimensions = 128, distanceFunction = DistanceFunction.COSINE)
    public List<Float> getEmbeddingCosine() {
        return embeddingCosine;
    }

    public void setEmbeddingCosine(List<Float> embeddingCosine) {
        this.embeddingCosine = embeddingCosine;
    }

    @DynamoDbVectorAttribute(indexName = "dot-idx", dimensions = 256, distanceFunction = DistanceFunction.DOT_PRODUCT)
    public List<Float> getEmbeddingDot() {
        return embeddingDot;
    }

    public void setEmbeddingDot(List<Float> embeddingDot) {
        this.embeddingDot = embeddingDot;
    }

    @DynamoDbVectorAttribute(indexName = "euclidean-idx", dimensions = 512, distanceFunction = DistanceFunction.EUCLIDEAN)
    public List<Float> getEmbeddingEuclidean() {
        return embeddingEuclidean;
    }

    public void setEmbeddingEuclidean(List<Float> embeddingEuclidean) {
        this.embeddingEuclidean = embeddingEuclidean;
    }

    @DynamoDbVectorAttribute(indexName = "cosine-idx-2", dimensions = 1024, distanceFunction = DistanceFunction.COSINE)
    public List<Float> getEmbeddingCosine2() {
        return embeddingCosine2;
    }

    public void setEmbeddingCosine2(List<Float> embeddingCosine2) {
        this.embeddingCosine2 = embeddingCosine2;
    }

    @DynamoDbVectorAttribute(indexName = "dot-idx-2", dimensions = 1536, distanceFunction = DistanceFunction.DOT_PRODUCT)
    public List<Float> getEmbeddingDot2() {
        return embeddingDot2;
    }

    public void setEmbeddingDot2(List<Float> embeddingDot2) {
        this.embeddingDot2 = embeddingDot2;
    }
}
