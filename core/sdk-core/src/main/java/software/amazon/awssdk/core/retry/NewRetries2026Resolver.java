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

package software.amazon.awssdk.core.retry;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Resolver for whether retry behavior 2.1 is used.
 *
 * @deprecated The {@link RetryMode#STANDARD} and {@link RetryMode#ADAPTIVE_V2} retry modes use retry behavior 2.1. This
 * class is no longer used by the SDK.
 */
@SdkProtectedApi
@Deprecated
public final class NewRetries2026Resolver {

    /**
     * This setting is ignored.
     *
     * @return This resolver for method chaining.
     */
    public NewRetries2026Resolver defaultNewRetries2026(Boolean defaultNewRetries2026) {
        return this;
    }

    /**
     * Always returns {@code false}.
     *
     * <p>Service modules released before retry behavior 2.1 became the default call this to decide whether to apply their
     * own retry behavior 2.1 settings. This resolver can't see the retry mode, profile file or profile name configured on
     * their client, so it returns {@code false} to keep those modules on the retry behavior they used by default. This keeps
     * the {@link RetryMode#LEGACY} retry mode unchanged for them.
     *
     * @return {@code false}
     */
    public boolean resolve() {
        return false;
    }
}
