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

package software.amazon.awssdk.enhanced.dynamodb.extensions.annotations;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Strategy used to decide when a new value is generated for an annotated attribute.
 */
@SdkPublicApi
public enum DynamoDbAutoGenerateStrategy {
    /**
     * Generate a new value on every write operation.
     */
    ALWAYS,

    /**
     * Generate a value only when the current value is missing.
     * For values present in the write item map, DynamoDB {@code NULL} is missing and an empty string is present. For a
     * non-primary-key value omitted from an update item map, DynamoDB evaluates stored attribute existence using
     * {@code if_not_exists}; a stored DynamoDB {@code NULL} therefore counts as present.
     */
    CREATE
}
