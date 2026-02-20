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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Utility class for adding optimistic locking to DynamoDB delete operations.
 * <p>
 * Optimistic locking prevents concurrent modifications by checking that an item's version hasn't changed since it was last read.
 * If the version has changed, the delete operation fails with a {@code ConditionalCheckFailedException}.
 */
@SdkPublicApi
public final class OptimisticLockingHelper {

    private OptimisticLockingHelper() {
    }

    /**
     * Adds optimistic locking to a delete request.
     *
     * @param request              the original delete request
     * @param versionValue         the expected version value
     * @param versionAttributeName the version attribute name
     * @return delete request with optimistic locking condition
     */
    public static DeleteItemEnhancedRequest withOptimisticLocking(
        DeleteItemEnhancedRequest request, AttributeValue versionValue, String versionAttributeName) {

        Expression conditionExpression = createVersionCondition(versionValue, versionAttributeName);
        return request.toBuilder()
                      .conditionExpression(conditionExpression)
                      .build();
    }

    /**
     * Adds optimistic locking to a transactional delete request.
     *
     * @param request              the original transactional delete request
     * @param versionValue         the expected version value
     * @param versionAttributeName the version attribute name
     * @return transactional delete request with optimistic locking condition
     */
    public static TransactDeleteItemEnhancedRequest withOptimisticLocking(
        TransactDeleteItemEnhancedRequest request, AttributeValue versionValue, String versionAttributeName) {

        Expression conditionExpression = createVersionCondition(versionValue, versionAttributeName);
        return request.toBuilder()
                      .conditionExpression(conditionExpression)
                      .build();
    }

    /**
     * Conditionally applies optimistic locking if enabled and version information exists.
     *
     * @param <T>                  the type of the item
     * @param request              the original delete request
     * @param keyItem              the item containing version information
     * @param tableSchema          the table schema
     * @param useOptimisticLocking if true, applies optimistic locking
     * @return delete request with optimistic locking if enabled and version exists, otherwise original request
     */
    public static <T> DeleteItemEnhancedRequest conditionallyApplyOptimisticLocking(
        DeleteItemEnhancedRequest request, T keyItem, TableSchema<T> tableSchema, boolean useOptimisticLocking) {

        if (!useOptimisticLocking) {
            return request;
        }

        return getVersionAttributeName(tableSchema)
            .map(versionAttributeName -> {
                AttributeValue version = tableSchema.attributeValue(keyItem, versionAttributeName);
                return version != null ? withOptimisticLocking(request, version, versionAttributeName) : request;
            })
            .orElse(request);
    }

    /**
     * Conditionally applies optimistic locking if enabled and version information exists.
     *
     * @param <T>                  the type of the item
     * @param request              the original transactional delete request
     * @param keyItem              the item containing version information
     * @param tableSchema          the table schema
     * @param useOptimisticLocking if true, applies optimistic locking
     * @return delete request with optimistic locking if enabled and version exists, otherwise original request
     */
    public static <T> TransactDeleteItemEnhancedRequest conditionallyApplyOptimisticLocking(
        TransactDeleteItemEnhancedRequest request, T keyItem, TableSchema<T> tableSchema, boolean useOptimisticLocking) {

        if (!useOptimisticLocking) {
            return request;
        }

        return getVersionAttributeName(tableSchema)
            .map(versionAttributeName -> {
                AttributeValue version = tableSchema.attributeValue(keyItem, versionAttributeName);
                return version != null ? withOptimisticLocking(request, version, versionAttributeName) : request;
            })
            .orElse(request);
    }


    /**
     * Creates a version condition expression.
     *
     * @param versionValue         the expected version value
     * @param versionAttributeName the version attribute name
     * @return version check condition expression
     */
    public static Expression createVersionCondition(AttributeValue versionValue, String versionAttributeName) {
        return Expression.builder()
                         .expression(versionAttributeName + " = :version_value")
                         .putExpressionValue(":version_value", versionValue)
                         .build();
    }

    /**
     * Gets the version attribute name from table schema.
     *
     * @param <T>         the type of the item
     * @param tableSchema the table schema
     * @return version attribute name if present, empty otherwise
     */
    public static <T> Optional<String> getVersionAttributeName(TableSchema<T> tableSchema) {
        return tableSchema.tableMetadata().customMetadataObject("VersionedRecordExtension:VersionAttribute", String.class);
    }
}