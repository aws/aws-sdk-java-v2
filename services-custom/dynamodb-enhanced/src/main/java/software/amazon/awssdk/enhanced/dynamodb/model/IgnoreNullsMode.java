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
 * The SCALAR_ONLY mode supports updates to scalar attributes to any level (top level, first nested level, second nested level,
 * etc.) when the user wants to update scalar attributes by providing only the delta of changes to be updated. This mode
 * does not support updates to maps and is expected to throw a 4xx DynamoDB exception if done so.
 * <p>
 * In the MAPS_ONLY mode, creation of new map/bean structures through update statements are supported, i.e. setting
 * null/non-existent maps to non-null values. If users try to update scalar attributes in this mode, it will overwrite
 * existing values in the table.
 * <p>
 * The DEFAULT mode disables any special handling around null values in the update query expression
 */
@SdkPublicApi
public enum IgnoreNullsMode {
    SCALAR_ONLY,
    MAPS_ONLY,
    DEFAULT
}
