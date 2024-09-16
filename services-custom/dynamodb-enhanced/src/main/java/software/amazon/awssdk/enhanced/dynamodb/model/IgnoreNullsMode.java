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
 * In the SCALAR_ONLY mode, updates to nested scalar attributes are supported in the ignoreNulls = true mode, i.e. when the user
 * wants to update nested scalar attributes by providing only the delta of changes to be updated. This mode does not support
 * updates to maps and is expected to throw a 400x DynamoDB exception if done so.
 * <p>
 * In the MAPS_ONLY mode, updates to nested map structures are supported, i.e. setting null/non-existent maps to non-null values
 * are supported in the ignoreNulls = true mode, i.e. when user only provides the delta of changes to be updated.
 * <p>
 * The DEFAULT mode operates by setting ignoreNulls to false, and requires the user to fetch existing DDB item, make modifications
 * to it and then update the item
 */
@SdkPublicApi
public enum IgnoreNullsMode {
    SCALAR_ONLY,
    MAPS_ONLY,
    DEFAULT
}
