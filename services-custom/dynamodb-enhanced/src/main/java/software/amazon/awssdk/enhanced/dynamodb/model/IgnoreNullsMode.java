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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Determines how null-valued attributes in a Java object are handled during an update operation.
 * This replaces the deprecated 'ignoreNulls' boolean parameter.
 *
 * <p>{@code ignoreNulls(false)} is equivalent to {@link #DEFAULT} and {@code ignoreNulls(true)} is equivalent to
 * {@link #MAPS_ONLY}.
 */
@SdkPublicApi
public enum IgnoreNullsMode {

    /**
     * Ignores all null-valued properties and updates only the non-null scalar attributes at any nesting level.
     * The SDK constructs update expressions that target individual nested attributes, allowing partial updates
     * of nested beans or maps without overwriting sibling attributes.
     *
     * <p>The target bean or map must already exist in DynamoDB. If it does not, DynamoDB returns a validation
     * exception because the document path does not exist. Use {@link #MAPS_ONLY} first to create the nested
     * structure, then use {@code SCALAR_ONLY} for subsequent partial updates.
     */
    SCALAR_ONLY,

    /**
     * Ignores null-valued top-level properties and replaces or adds entire beans and maps as whole values.
     * Null-valued scalar attributes within a provided bean or map are retained (saved as null in DynamoDB).
     *
     * <p>Use this mode to add a new nested bean or map that does not yet exist in DynamoDB, or to fully
     * replace an existing one. This is equivalent to the deprecated {@code ignoreNulls(true)}.
     */
    MAPS_ONLY,

    /**
     * Does not ignore any null values. Null-valued attributes in the Java object generate REMOVE actions in the
     * update expression, deleting those attributes from the item in DynamoDB.
     *
     * <p>This is the default mode and is equivalent to the deprecated {@code ignoreNulls(false)}. When using this
     * mode, you should typically retrieve the item first to avoid unintentionally removing attributes.
     */
    DEFAULT
}
