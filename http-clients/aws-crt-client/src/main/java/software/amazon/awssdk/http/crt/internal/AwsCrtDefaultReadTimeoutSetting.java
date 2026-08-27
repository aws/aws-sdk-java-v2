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

package software.amazon.awssdk.http.crt.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * The rollout gate the standalone (non-service-client) CRT HTTP Client reads to decide whether to apply the default read/write
 * inactivity timeout. It mirrors {@code SdkSystemSetting.AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026} in sdk-core, using the same
 * environment variable and system property; the CRT client reads them directly because it cannot depend on sdk-core. This is
 * needed to ensure a timeout is configured when a standalone HTTP client is created.
 */
@SdkInternalApi
public enum AwsCrtDefaultReadTimeoutSetting implements SystemSetting {
    ENABLE_DEFAULT_READ_TIMEOUT_2026;

    @Override
    public String property() {
        return "aws.enableDefaultReadTimeout2026";
    }

    @Override
    public String environmentVariable() {
        return "AWS_ENABLE_DEFAULT_READ_TIMEOUT_2026";
    }

    @Override
    public String defaultValue() {
        return null;
    }
}
