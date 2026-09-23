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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.model.DistanceFunction;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedVectorIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElement;
import software.amazon.awssdk.enhanced.dynamodb.model.SearchSchemaElementType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.VectorDistanceFunction;
import software.amazon.awssdk.services.dynamodb.model.VectorIndex;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Maps enhanced vector index models to low-level DynamoDB create-table types.
 */
@SdkInternalApi
public final class VectorIndexUtils {
    private VectorIndexUtils() {
    }

    public static VectorIndex toVectorIndex(EnhancedVectorIndex enhancedVectorIndex) {
        VectorIndex.Builder builder =
            VectorIndex.builder()
                       .indexName(enhancedVectorIndex.indexName())
                       .vectorAttribute(b -> b.attributeName(enhancedVectorIndex.vectorAttributeName()))
                       .dimensions((long) enhancedVectorIndex.dimensions())
                       .distanceFunction(toVectorDistanceFunction(enhancedVectorIndex.distanceFunction()))
                       .projection(resolveProjection(enhancedVectorIndex.projection()));

        List<software.amazon.awssdk.services.dynamodb.model.SearchSchemaElement> searchSchema =
            toSearchSchema(enhancedVectorIndex);
        if (!searchSchema.isEmpty()) {
            builder.searchSchema(searchSchema);
        }

        return builder.build();
    }

    private static Projection resolveProjection(Projection projection) {
        if (projection != null) {
            return projection;
        }
        return Projection.builder().projectionType(ProjectionType.ALL).build();
    }

    private static VectorDistanceFunction toVectorDistanceFunction(DistanceFunction distanceFunction) {
        if (distanceFunction == null) {
            return null;
        }
        return VectorDistanceFunction.fromValue(distanceFunction.name());
    }

    private static List<software.amazon.awssdk.services.dynamodb.model.SearchSchemaElement> toSearchSchema(
        EnhancedVectorIndex enhancedVectorIndex) {
        if (CollectionUtils.isNullOrEmpty(enhancedVectorIndex.searchSchemaElements())) {
            return Collections.emptyList();
        }

        return enhancedVectorIndex.searchSchemaElements().stream()
                                  .map(VectorIndexUtils::toSdkSearchSchemaElement)
                                  .collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.dynamodb.model.SearchSchemaElement toSdkSearchSchemaElement(
        SearchSchemaElement element) {
        return software.amazon.awssdk.services.dynamodb.model.SearchSchemaElement.builder()
                                  .attributeName(element.attributeName())
                                  .searchSchemaElementType(
                                      toSdkSearchSchemaElementType(element.searchSchemaElementType()))
                                  .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.SearchSchemaElementType toSdkSearchSchemaElementType(
        SearchSchemaElementType searchSchemaElementType) {
        if (searchSchemaElementType == null) {
            return null;
        }
        return software.amazon.awssdk.services.dynamodb.model.SearchSchemaElementType.fromValue(
            searchSchemaElementType.name());
    }
}
