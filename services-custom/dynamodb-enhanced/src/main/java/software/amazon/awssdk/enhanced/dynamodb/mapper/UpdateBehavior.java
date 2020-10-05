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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Update behaviors that can be applied to individual attributes. This behavior will only apply to 'update' operations
 * such as UpdateItem, and not 'put' operations such as PutItem.
 * <p>
 * If an update behavior is not specified for an attribute, the default behavior of {@link #WRITE_ALWAYS} will be
 * applied.
 */
@SdkPublicApi
public enum UpdateBehavior {
    /**
     * Always overwrite with the new value if one is provided, or remove any existing value if a null value is
     * provided and 'ignoreNulls' is set to false.
     * <p>
     * This is the default behavior applied to all attributes unless otherwise specified.
     */
    WRITE_ALWAYS,

    /**
     * Write the new value if there is no existing value in the persisted record or a new record is being written,
     * otherwise leave the existing value.
     * <p>
     * IMPORTANT: If a null value is provided and 'ignoreNulls' is set to false, the attribute
     * will always be removed from the persisted record as DynamoDb does not support conditional removal with this
     * method.
     */
    WRITE_IF_NOT_EXISTS
}
