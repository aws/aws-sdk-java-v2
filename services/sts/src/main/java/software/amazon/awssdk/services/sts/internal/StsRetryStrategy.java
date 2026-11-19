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

package software.amazon.awssdk.services.sts.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.sts.model.IdpCommunicationErrorException;

/**
 * Specialized retry strategy resolution for STS to enable retrying for {@link IdpCommunicationErrorException}.
 */
@SdkInternalApi
public final class StsRetryStrategy {

    private StsRetryStrategy() {
    }

    public static RetryStrategy resolveRetryStrategy(SdkClientConfiguration config) {
        RetryStrategy configuredRetryStrategy = config.option(SdkClientOption.RETRY_STRATEGY);
        if (configuredRetryStrategy != null) {
            return configuredRetryStrategy;
        }

        // A configured retry policy takes precedence over a retry strategy, so don't build one
        if (config.option(SdkClientOption.RETRY_POLICY) != null) {
            return null;
        }

        RetryMode retryMode = resolveRetryMode(config);

        // Just return null and let the normal retry strategy resolution occur
        if (retryMode == RetryMode.LEGACY || retryMode == RetryMode.ADAPTIVE) {
            return null;
        }

        RetryStrategy defaultStrategy = AwsRetryStrategy.forRetryMode(retryMode,
                                                                      config.option(SdkClientOption.PROFILE_FILE_SUPPLIER),
                                                                      config.option(SdkClientOption.PROFILE_NAME));
        return defaultStrategy.toBuilder()
                              .retryOnException(IdpCommunicationErrorException.class)
                              .build();
    }

    private static RetryMode resolveRetryMode(SdkClientConfiguration config) {
        return RetryMode.resolver()
                        .profileFile(config.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
                        .profileName(config.option(SdkClientOption.PROFILE_NAME))
                        .defaultRetryMode(config.option(SdkClientOption.DEFAULT_RETRY_MODE))
                        .resolve();
    }
}