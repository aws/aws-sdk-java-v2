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

package software.amazon.awssdk.services.dynamodb;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;

/**
 * Default retry policy for DynamoDB Client.
 */
@SdkProtectedApi
public final class DynamoDbRetryPolicy {

    /** Default max retry count for DynamoDB client **/
    private static final int DEFAULT_MAX_ERROR_RETRY = 8;

    /**
     * Default base sleep time for DynamoDB.
     **/
    private static final Duration DEFAULT_BASE_DELAY = Duration.ofMillis(25);

    /**
     * The default back-off strategy for DynamoDB client, which increases
     * exponentially up to a max amount of delay. Compared to the SDK default
     * back-off strategy, it applies a smaller scale factor.
     */
    private static final BackoffStrategy DEFAULT_BACKOFF_STRATEGY =
        FullJitterBackoffStrategy.builder()
                                 .baseDelay(DEFAULT_BASE_DELAY)
                                 .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                                 .build();

    /**
     * Default retry policy for DynamoDB.
     */
    private static final RetryPolicy DEFAULT =
        AwsRetryPolicy.defaultRetryPolicy().toBuilder()
                      .numRetries(DEFAULT_MAX_ERROR_RETRY)
                      .backoffStrategy(DEFAULT_BACKOFF_STRATEGY).build();

    private DynamoDbRetryPolicy() {

    }

    /**
     * @return Default retry policy used by DynamoDbClient
     */
    public static RetryPolicy defaultPolicy() {
        return DEFAULT;
    }
}
