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

package software.amazon.awssdk.services.s3.internal.settingproviders;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.S3SystemSetting;

/**
 * {@link DisableMultiRegionProvider} implementation that loads configuration from system properties
 * and environment variables.
 */
@SdkInternalApi
public final class SystemsSettingsDisableMultiRegionProvider implements DisableMultiRegionProvider {

    private SystemsSettingsDisableMultiRegionProvider() {
    }

    public static SystemsSettingsDisableMultiRegionProvider create() {
        return new SystemsSettingsDisableMultiRegionProvider();
    }

    @Override
    public Optional<Boolean> resolve() {
        return S3SystemSetting.AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.getBooleanValue();
    }
}
