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

package software.amazon.awssdk.enhanced.dynamodb.extensions;

import java.util.Collection;
import java.util.Collections;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.enhanced.dynamodb.TableMetadata;

/**
 * Provides shared schema validation utilities for DynamoDB enhanced client extensions.
 */
@SdkProtectedApi
public final class ExtensionsValidationUtils {

    private ExtensionsValidationUtils() {
    }

    /**
     * Validates that there are no attributes that have both annotations. These annotations have conflicting behaviors and cannot
     * be used together on the same attribute. If an attribute is found with both annotations, an IllegalArgumentException is
     * thrown with a message indicating the attribute and the conflicting annotations.
     *
     * @param tableMetadata        The metadata of the table to validate.
     * @param firstAnnotationMetadataKey     The metadata key for the first annotation to check for.
     * @param secondAnnotationMetadataKey    The metadata key for the second annotation to check for.
     * @param firstAnnotationName  The name of the first annotation to use in the error message if a conflict is found.
     * @param secondAnnotationName The name of the second annotation to use in the error message if a conflict is found.
     */
    public static void validateNoAnnotationConflict(TableMetadata tableMetadata,
                                                    String firstAnnotationMetadataKey,
                                                    String secondAnnotationMetadataKey,
                                                    String firstAnnotationName,
                                                    String secondAnnotationName) {

        Collection<?> attributesHavingFirstAnnotation =
            tableMetadata.customMetadataObject(firstAnnotationMetadataKey, Collection.class).orElse(Collections.emptyList());

        if (attributesHavingFirstAnnotation.isEmpty()) {
            return;
        }

        Collection<?> attributesHavingSecondAnnotation =
            tableMetadata.customMetadataObject(secondAnnotationMetadataKey, Collection.class).orElse(Collections.emptyList());

        if (attributesHavingSecondAnnotation.isEmpty()) {
            return;
        }

        attributesHavingFirstAnnotation
            .stream()
            .filter(attributesHavingSecondAnnotation::contains)
            .findFirst()
            .ifPresent(attribute -> {
                throw new IllegalArgumentException(
                    "Attribute '" + attribute + "' cannot have both " + firstAnnotationName
                    + " and " + secondAnnotationName + " annotations. "
                    + "These annotations have conflicting behaviors and cannot be used together on the same attribute.");
            });
    }
}