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

package software.amazon.awssdk.services.dynamodb;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;

/**
 * Default retry policy for DynamoDB Client.
 */
@SdkInternalApi
final class DynamoDbRetryPolicy {

    /**
     * Default max retry count for DynamoDB client, when using the LEGACY retry mode.
     **/
    private static final int LEGACY_MAX_ERROR_RETRY = 8;

    /**
     * Default base sleep time for DynamoDB, when using the LEGACY retry mode.
     **/
    private static final Duration LEGACY_BASE_DELAY = Duration.ofMillis(25);

    /**
     * The default back-off strategy for DynamoDB client, which increases
     * exponentially up to a max amount of delay. Compared to the SDK default
     * back-off strategy, it applies a smaller scale factor.
     *
     * This is only used when using the LEGACY retry mode.
     */
    private static final BackoffStrategy LEGACY_BACKOFF_STRATEGY =
        FullJitterBackoffStrategy.builder()
                                 .baseDelay(LEGACY_BASE_DELAY)
                                 .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                                 .build();

    private DynamoDbRetryPolicy() {
    }

    public static RetryPolicy resolveRetryPolicy(SdkClientConfiguration config) {
        RetryPolicy configuredRetryPolicy = config.option(SdkClientOption.RETRY_POLICY);
        if (configuredRetryPolicy != null) {
            return configuredRetryPolicy;
        }

        RetryMode retryMode = RetryMode.resolver()
                                       .profileFile(() -> config.option(SdkClientOption.PROFILE_FILE))
                                       .profileName(config.option(SdkClientOption.PROFILE_NAME))
                                       .resolve();

        if (retryMode != RetryMode.LEGACY) {
            return AwsRetryPolicy.forRetryMode(retryMode)
                                 .toBuilder()
                                 .additionalRetryConditionsAllowed(false)
                                 .build();
        }

        return AwsRetryPolicy.forRetryMode(RetryMode.LEGACY)
                             .toBuilder()
                             .additionalRetryConditionsAllowed(false)
                             .numRetries(LEGACY_MAX_ERROR_RETRY)
                             .backoffStrategy(LEGACY_BACKOFF_STRATEGY)
                             .build();
    }
}
