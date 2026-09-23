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

package software.amazon.awssdk.enhanced.dynamodb.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.BeanTableSchemaAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;

/**
 * Marks an attribute as the vector embedding for a named vector index. The annotated getter should return a {@code List<Float>}
 * representing the vector embedding.
 *
 * <p>Each vector index requires exactly one attribute annotated with {@code @DynamoDbVectorAttribute}. Multiple
 * vector indexes on the same bean require separate annotated attributes, each with a distinct {@code indexName}.
 *
 * <p>Example usage:
 * <pre>{@code
 * @DynamoDbBean
 * public class Product {
 *     @DynamoDbVectorAttribute(
 *         indexName = "product-embedding-index",
 *         dimensions = 1536,
 *         distanceFunction = DistanceFunction.COSINE)
 *     public List<Float> productEmbedding() { return productEmbedding; }
 * }
 * }</pre>
 *
 * <p>Vector indexes defined via annotations default to {@code ProjectionType.ALL}. For {@code KEYS_ONLY} or
 * {@code INCLUDE} projections, use the programmatic {@link CreateTableEnhancedRequest#vectorIndexes()} API with explicit
 * {@link EnhancedVectorIndex} definitions.
 */
@SdkPublicApi
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(BeanTableSchemaAttributeTags.class)
public @interface DynamoDbVectorAttribute {
    /**
     * The name of the vector index this embedding attribute belongs to.
     */
    String indexName();

    /**
     * The number of dimensions in the vector embedding.
     */
    int dimensions();

    /**
     * The distance function used for similarity search on this index.
     */
    DistanceFunction distanceFunction();
}
