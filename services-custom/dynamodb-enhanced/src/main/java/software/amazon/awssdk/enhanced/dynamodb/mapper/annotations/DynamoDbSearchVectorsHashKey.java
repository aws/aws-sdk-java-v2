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
 * Denotes a HASH partition key in the SearchSchema for one or more vector indexes. The annotated attribute will be used as the
 * partition key for scoping vector searches via {@code SearchConditionExpression}.
 *
 * <p>Each vector index requires exactly one HASH element in its SearchSchema. Only the equality ({@code =}) operator is
 * supported for HASH attributes in {@code SearchConditionExpression}.
 *
 * <p>You must specify at least one index name. The index names must match those specified in corresponding
 * {@link DynamoDbVectorAttribute} annotations.
 *
 * <p>Example usage:
 * <pre>{@code
 * @DynamoDbBean
 * public class Product {
 *     @DynamoDbSearchVectorsHashKey(indexNames = {"product-embedding-index"})
 *     public String getCategory() { return category; }
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
public @interface DynamoDbSearchVectorsHashKey {
    /**
     * The names of one or more vector indexes that this HASH key should participate in.
     */
    String[] indexNames();
}
