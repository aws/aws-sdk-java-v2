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

/**
 * Denotes an INLINE_FILTER attribute in the SearchSchema for one or more vector indexes. Inline filter attributes are applied as
 * pre-filters before vector search and can be used in {@code SearchConditionExpression}.
 *
 * <p>Only the equality ({@code =}) operator is supported for inline filter attributes. Up to 18 inline filters
 * can be defined per vector index.
 *
 * <p>You must specify at least one index name. The index names must match those specified in corresponding
 * {@link DynamoDbVectorAttribute} annotations.
 *
 * <p>Example usage:
 * <pre>{@code
 * @DynamoDbBean
 * public class Product {
 *     @DynamoDbSearchVectorsInlineFilterKey(indexNames = {"product-embedding-index"})
 *     public String getColor() { return color; }
 *
 *     @DynamoDbSearchVectorsInlineFilterKey(indexNames = {"product-embedding-index"})
 *     public String getSize() { return size; }
 *
 *     @DynamoDbVectorAttribute(
 *         indexName = "product-embedding-index",
 *         dimensions = 1536,
 *         distanceFunction = DistanceFunction.COSINE)
 *     public List<Float> getProductEmbedding() { return productEmbedding; }
 * }
 * }</pre>
 */
@SdkPublicApi
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@BeanTableSchemaAttributeTag(BeanTableSchemaAttributeTags.class)
public @interface DynamoDbSearchVectorsInlineFilterKey {
    /**
     * The names of one or more vector indexes that this inline filter key should participate in.
     */
    String[] indexNames();
}
