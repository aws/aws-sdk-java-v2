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

import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.keyRef;
import static software.amazon.awssdk.enhanced.dynamodb.internal.EnhancedClientUtils.valueRef;

import java.util.Collections;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Utility class for adding optimistic locking to DynamoDB delete operations.
 * <p>
 * Optimistic locking prevents concurrent modifications by checking that an item's version hasn't changed since it was last read.
 * If the version has changed, the delete operation fails with a {@code ConditionalCheckFailedException}.
 */
@SdkInternalApi
public final class OptimisticLockingHelper {

    private static final String CUSTOM_VERSION_METADATA_KEY = "VersionedRecordExtension:VersionAttribute";
    private static final String USE_VERSION_ON_DELETE_METADATA_KEY = "VersionedRecordExtension:UseVersionOnDelete";

    private OptimisticLockingHelper() {
    }

    /**
     * Adds optimistic locking to a delete request.
     * <p>
     * If a condition expression is already set on the request builder, this method merges it with the optimistic locking
     * condition using {@code AND}.
     *
     * @param requestBuilder       the original delete request builder
     * @param versionValue         the expected version value
     * @param versionAttributeName the version attribute name
     * @return delete request with optimistic locking condition
     */
    public static DeleteItemEnhancedRequest optimisticLocking(DeleteItemEnhancedRequest.Builder requestBuilder,
                                                              AttributeValue versionValue, String versionAttributeName) {

        Expression mergedCondition = mergeConditions(
            requestBuilder.build().conditionExpression(),
            createVersionCondition(versionValue, versionAttributeName));

        return requestBuilder
            .conditionExpression(mergedCondition)
            .build();
    }

    /**
     * Adds optimistic locking to a transactional delete request.
     * <p>
     * If a condition expression is already set on the request builder, this method merges it with the optimistic locking
     * condition using {@code AND}.
     *
     * @param requestBuilder       the original delete request builder
     * @param versionValue         the expected version value
     * @param versionAttributeName the version attribute name
     * @return transactional delete request with optimistic locking condition
     */
    public static TransactDeleteItemEnhancedRequest optimisticLocking(TransactDeleteItemEnhancedRequest.Builder requestBuilder,
                                                                      AttributeValue versionValue, String versionAttributeName) {

        Expression mergedCondition = mergeConditions(
            requestBuilder.build().conditionExpression(),
            createVersionCondition(versionValue, versionAttributeName));

        return requestBuilder
            .conditionExpression(mergedCondition)
            .build();
    }

    /**
     * Conditionally applies optimistic locking based on annotation setting.
     *
     * @param <T>            the type of the item
     * @param requestBuilder the delete request builder
     * @param keyItem        the item containing version information
     * @param tableSchema    the table schema
     * @return delete request with optimistic locking if annotation enables it and version exists, otherwise original request
     * @throws IllegalStateException if optimistic locking is enabled but the version attribute is null
     */
    public static <T> DeleteItemEnhancedRequest conditionallyApplyOptimisticLocking(
        DeleteItemEnhancedRequest.Builder requestBuilder, T keyItem, TableSchema<T> tableSchema) {

        return getVersionAttributeName(tableSchema)
            .map(versionAttributeName -> {
                Boolean useVersionOnDelete = tableSchema.tableMetadata()
                                                        .customMetadataObject(USE_VERSION_ON_DELETE_METADATA_KEY, Boolean.class)
                                                        .orElse(false);

                if (!useVersionOnDelete) {
                    return requestBuilder.build();
                }

                AttributeValue version = tableSchema.attributeValue(keyItem, versionAttributeName);
                if (version == null) {
                    throw new IllegalStateException(
                        "Optimistic locking is enabled for delete, but version attribute is null: " + versionAttributeName);
                }
                return optimisticLocking(requestBuilder, version, versionAttributeName);

            }).orElseGet(requestBuilder::build);
    }

    /**
     * Creates a version condition expression.
     *
     * @param versionValue         the expected version value
     * @param versionAttributeName the version attribute name
     * @return version check condition expression
     * @throws IllegalArgumentException if {@code versionAttributeName} or {@code versionValue} are null or empty
     */
    public static Expression createVersionCondition(AttributeValue versionValue, String versionAttributeName) {
        if (versionAttributeName == null || versionAttributeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Version attribute name must not be null or empty.");
        }

        if (versionValue == null || versionValue.n() == null || versionValue.n().trim().isEmpty()) {
            throw new IllegalArgumentException("Version value must not be null or empty.");
        }

        String attributeKeyRef = keyRef(versionAttributeName);
        String attributeValueRef = valueRef(versionAttributeName);

        return Expression.builder()
                         .expression(String.format("%s = %s", attributeKeyRef, attributeValueRef))
                         .expressionNames(Collections.singletonMap(attributeKeyRef, versionAttributeName))
                         .expressionValues(Collections.singletonMap(attributeValueRef, versionValue))
                         .build();
    }

    public static Expression mergeConditions(Expression initialCondition, Expression optimisticLockingCondition) {
        return Expression.join(initialCondition, optimisticLockingCondition, " AND ");
    }

    /**
     * Gets the version attribute name from table schema.
     *
     * @param <T>         the type of the item
     * @param tableSchema the table schema
     * @return version attribute name if present, empty otherwise
     */
    public static <T> Optional<String> getVersionAttributeName(TableSchema<T> tableSchema) {
        return tableSchema.tableMetadata().customMetadataObject(CUSTOM_VERSION_METADATA_KEY, String.class);
    }
}