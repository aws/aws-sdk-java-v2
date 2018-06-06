/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.config.options;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration.Builder;
import software.amazon.awssdk.core.config.options.SdkAdvancedClientOption;
import software.amazon.awssdk.regions.Region;

/**
 * A collection of advanced options that can be configured on an AWS client via
 * {@link Builder#advancedOption(SdkAdvancedClientOption, Object)}.
 *
 * <p>These options are usually not required outside of testing or advanced libraries, so most users should not need to configure
 * them.</p>
 *
 * @param <T> The type of value associated with the option.
 */
@SdkPublicApi
public final class AwsAdvancedClientOption<T> extends SdkAdvancedClientOption<T> {

    /**
     * Whether region detection should be enabled. Region detection is used when the {@link AwsClientBuilder#region(Region)} is
     * not specified. This is enabled by default.
     */
    public static final AwsAdvancedClientOption<Boolean> ENABLE_DEFAULT_REGION_DETECTION =
        new AwsAdvancedClientOption<>(Boolean.class);

    private AwsAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
