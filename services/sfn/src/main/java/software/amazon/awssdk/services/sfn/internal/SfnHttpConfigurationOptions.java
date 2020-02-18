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

package software.amazon.awssdk.services.sfn.internal;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Sfn specific http configurations.
 */
@SdkInternalApi
public final class SfnHttpConfigurationOptions {

    private static final AttributeMap OPTIONS = AttributeMap
        .builder()
        // Sfn has long polling APIs such as getActivityTask that could take up to
        // 60 seconds to respond
        .put(SdkHttpConfigurationOption.READ_TIMEOUT, Duration.ofSeconds(65))
        .build();

    private SfnHttpConfigurationOptions() {
    }

    public static AttributeMap defaultHttpConfig() {
        return OPTIONS;
    }
}
