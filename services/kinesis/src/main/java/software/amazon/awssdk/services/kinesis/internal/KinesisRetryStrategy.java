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

package software.amazon.awssdk.services.kinesis.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;

/**
 * Default retry strategy resolution for Kinesis. In the {@link RetryMode#STANDARD} and {@link RetryMode#ADAPTIVE_V2} retry
 * modes, Kinesis uses the same retry strategy as other services, except that it never retries {@code SubscribeToShard}. In the
 * {@link RetryMode#LEGACY} and {@link RetryMode#ADAPTIVE} retry modes, and when a retry policy is configured, this returns
 * {@code null} so that the retry policy from {@code KinesisRetryPolicy} is used. A retry strategy configured on the client is
 * used as is.
 */
@SdkInternalApi
public final class KinesisRetryStrategy {
    private static final String SUBSCRIBE_TO_SHARD = "SubscribeToShard";

    private KinesisRetryStrategy() {
    }

    public static RetryStrategy resolveRetryStrategy(SdkClientConfiguration config) {
        RetryStrategy configuredRetryStrategy = config.option(SdkClientOption.RETRY_STRATEGY);
        if (configuredRetryStrategy != null) {
            return configuredRetryStrategy;
        }

        if (config.option(SdkClientOption.RETRY_POLICY) != null) {
            return null;
        }

        RetryMode retryMode = resolveRetryMode(config);
        if (retryMode == RetryMode.LEGACY || retryMode == RetryMode.ADAPTIVE) {
            return null;
        }

        RetryStrategy defaultStrategy = AwsRetryStrategy.forRetryMode(retryMode,
                                                                      config.option(SdkClientOption.PROFILE_FILE_SUPPLIER),
                                                                      config.option(SdkClientOption.PROFILE_NAME));
        return defaultStrategy.toBuilder()
                              .doNotRetryWhen(KinesisRetryStrategy::isSubscribeToShard)
                              .build();
    }

    private static boolean isSubscribeToShard(RefreshRetryTokenRequest request) {
        return request.operationName().filter(SUBSCRIBE_TO_SHARD::equals).isPresent();
    }

    private static RetryMode resolveRetryMode(SdkClientConfiguration config) {
        return RetryMode.resolver()
                        .profileFile(config.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
                        .profileName(config.option(SdkClientOption.PROFILE_NAME))
                        .defaultRetryMode(config.option(SdkClientOption.DEFAULT_RETRY_MODE))
                        .resolve();
    }
}
