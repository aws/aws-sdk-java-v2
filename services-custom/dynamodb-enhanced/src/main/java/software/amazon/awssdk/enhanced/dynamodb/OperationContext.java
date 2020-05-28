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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A context object that is associated with a specific operation and identifies the resources that the operation is
 * meant to operate on.
 * <p>
 * This context is passed to and can be read by extension hooks (see {@link DynamoDbEnhancedClientExtension}).
 */
@SdkPublicApi
public interface OperationContext {
    /**
     * The name of the table being operated on
     */
    String tableName();

    /**
     * The name of the index within the table being operated on. If it is the primary index, then this value will be
     * set to the constant {@link TableMetadata#primaryIndexName()}.
     */
    String indexName();
}
