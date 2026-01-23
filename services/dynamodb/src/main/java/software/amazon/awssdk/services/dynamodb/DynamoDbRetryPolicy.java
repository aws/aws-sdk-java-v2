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

import static software.amazon.awssdk.retries.api.BackoffStrategy.exponentialDelay;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.retry.RetryPolicyAdapter;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;


/**
 * Default retry policy for DynamoDB Client.
 */
@SdkInternalApi
final class DynamoDbRetryPolicy {

    /**
     * Default max retry count for DynamoDB client, regardless of retry mode.
     **/
    private static final int MAX_ERROR_RETRY = 8;

    /**
     * Default attempts count for DynamoDB client, regardless of retry mode.
     **/
    private static final int MAX_ATTEMPTS = MAX_ERROR_RETRY + 1;

    /**
     * Default base sleep time for DynamoDB, regardless of retry mode.
     **/
    private static final Duration BASE_DELAY = Duration.ofMillis(25);

    /**
     * The default back-off strategy for DynamoDB client, which increases
     * exponentially up to a max amount of delay. Compared to the SDK default
     * back-off strategy, it applies a smaller scale factor.
     */
    private static final BackoffStrategy BACKOFF_STRATEGY =
        FullJitterBackoffStrategy.builder()
                                 .baseDelay(BASE_DELAY)
                                 .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                                 .build();

    private DynamoDbRetryPolicy() {
    }

    /**
     * @deprecated Use instead {@link #resolveRetryStrategy}.
     */
    @Deprecated
    public static RetryPolicy resolveRetryPolicy(SdkClientConfiguration config) {
        RetryPolicy configuredRetryPolicy = config.option(SdkClientOption.RETRY_POLICY);
        if (configuredRetryPolicy != null) {
            return configuredRetryPolicy;
        }

        RetryMode retryMode = resolveRetryMode(config);
        return retryPolicyFor(retryMode);
    }

    public static RetryStrategy resolveRetryStrategy(SdkClientConfiguration config) {
        RetryStrategy configuredRetryStrategy = config.option(SdkClientOption.RETRY_STRATEGY);
        if (configuredRetryStrategy != null) {
            return configuredRetryStrategy;
        }

        RetryMode retryMode = resolveRetryMode(config);

        if (retryMode == RetryMode.ADAPTIVE) {
            return RetryPolicyAdapter.builder()
                                     .retryPolicy(retryPolicyFor(retryMode))
                                     .build();
        }

        return AwsRetryStrategy.forRetryMode(retryMode)
            .toBuilder()
            .maxAttempts(MAX_ATTEMPTS)
            .backoffStrategy(exponentialDelay(BASE_DELAY, SdkDefaultRetrySetting.MAX_BACKOFF))
            .build();
    }

    private static RetryPolicy retryPolicyFor(RetryMode retryMode) {
        return AwsRetryPolicy.forRetryMode(retryMode)
                             .toBuilder()
                             .additionalRetryConditionsAllowed(false)
                             .numRetries(MAX_ERROR_RETRY)
                             .backoffStrategy(BACKOFF_STRATEGY)
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
