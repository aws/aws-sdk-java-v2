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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;

/**
 * Resolver for the {@link SdkSystemSetting#AWS_NEW_RETRIES_2026} that supports setting a fallback value if not defined in the
 * environment or system properties.
 */
@SdkProtectedApi
public final class NewRetries2026Resolver {
    private Boolean defaultNewRetries2026;

    /**
     * The default value for {@code AWS_NEW_RETRIES_2026} if not configured via {@link SdkSystemSetting#AWS_NEW_RETRIES_2026}.
     *
     * @return This resolver for method chaining.
     */
    public NewRetries2026Resolver defaultNewRetries2026(Boolean defaultNewRetries2026) {
        this.defaultNewRetries2026 = defaultNewRetries2026;
        return this;
    }

    /**
     * Resolve whether retries v2.1 is used.
     */
    public boolean resolve() {
        Optional<Boolean> envConfig = SdkSystemSetting.AWS_NEW_RETRIES_2026.getBooleanValue();

        if (envConfig.isPresent()) {
            return envConfig.get();
        }

        if (defaultNewRetries2026 != null) {
            return defaultNewRetries2026;
        }

        return false;
    }
}
