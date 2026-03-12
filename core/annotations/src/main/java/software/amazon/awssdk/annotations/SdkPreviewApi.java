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

package software.amazon.awssdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks APIs as preview or experimental features that may change or be removed.
 * 
 * <p><b>WARNING:</b> Elements annotated with {@code @SdkPreviewApi} are not stable and may
 * introduce breaking changes in any release, including minor and patch versions. Do not use
 * preview APIs in production environments.
 * 
 * <p><b>Use with caution:</b>
 * <ul>
 *   <li>Preview APIs are suitable for testing and providing feedback</li>
 *   <li>They may change significantly based on user feedback</li>
 *   <li>They may be promoted to public APIs or removed entirely</li>
 *   <li>No backward compatibility is guaranteed</li>
 * </ul>
 * 
 * <p><b>Intended for:</b> Early adopters and developers who want to experiment with new features
 * and provide feedback before they become stable public APIs.
 * 
 * @see SdkPublicApi
 * @see SdkProtectedApi
 * @see SdkInternalApi
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@SdkProtectedApi
public @interface SdkPreviewApi {
}
