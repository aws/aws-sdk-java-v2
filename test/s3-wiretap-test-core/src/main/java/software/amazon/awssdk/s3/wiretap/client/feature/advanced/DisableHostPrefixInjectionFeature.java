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

package software.amazon.awssdk.s3.wiretap.client.feature.advanced;

import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.s3.wiretap.client.feature.BooleanFeature;

public final class DisableHostPrefixInjectionFeature extends BooleanFeature {

    public DisableHostPrefixInjectionFeature() {
        super("disableHostPrefix");
        onSyncBool((b, v) -> b.overrideConfig().putAdvancedOption(
                SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, v));
        onAsyncBool((b, v) -> b.overrideConfig().putAdvancedOption(
                SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, v));
    }
}
