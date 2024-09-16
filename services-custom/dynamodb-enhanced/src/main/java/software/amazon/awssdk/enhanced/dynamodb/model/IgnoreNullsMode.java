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
 * <p>
 * SCALAR_ONLY mode is used for updates to nested scalar attributes when the
 * user wants to provide ONLY the delta to be updated
 * This mode does NOT support updates to nested map structures, i.e.
 * setting null valued / non-existent maps to non-null values in this mode
 * will throw a 400x DynamoDB error
 * <p>
 * The DEFAULT mode denotes that the entire item is provided. This supports updates
 * to both scalar and non-scalar attributes.
 */
@SdkPublicApi
public enum IgnoreNullsMode {
    SCALAR_ONLY,
    DEFAULT
}
