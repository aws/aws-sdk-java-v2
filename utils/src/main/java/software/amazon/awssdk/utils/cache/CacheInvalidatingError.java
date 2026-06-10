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

package software.amazon.awssdk.utils.cache;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Marker interface for exceptions that indicate a non-recoverable refresh failure.
 * When thrown during a cache refresh, the caching layer will propagate the exception
 * immediately without applying backoff or extending expiration.
 *
 * <p>Exceptions implementing this interface bypass cache static stability behavior,
 * ensuring that actionable errors (such as expired tokens or changed credentials)
 * are never suppressed by the caching layer.</p>
 */
@SdkProtectedApi
public interface CacheInvalidatingError {
}
