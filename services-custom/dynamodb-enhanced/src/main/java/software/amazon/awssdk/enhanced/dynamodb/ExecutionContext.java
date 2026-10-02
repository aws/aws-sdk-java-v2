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

package software.amazon.awssdk.enhanced.dynamodb;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Execution context used internally during table schema creation to track whether
 * a schema is being created for a root entity or a flattened nested object.
 */
@SdkProtectedApi
public enum ExecutionContext {
    /**
     * Indicates schema creation for a top-level entity that will be directly used
     * with DynamoDB operations. Enables schema caching for performance optimization.
     */
    ROOT,
    /**
     * Indicates schema creation for a nested object marked with {@code @DynamoDbFlatten}.
     * Bypasses caching to prevent conflicts and infinite recursion during flattened
     * object processing.
     */
    FLATTENED
}
