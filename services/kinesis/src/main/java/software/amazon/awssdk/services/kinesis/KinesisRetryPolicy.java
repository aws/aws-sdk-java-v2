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

package software.amazon.awssdk.services.kinesis;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.retry.AwsRetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;

/**
 * Default retry policy for the Kinesis Client.
 */
@SdkInternalApi
public class KinesisRetryPolicy {

    /**
     * Default retry policy for Kinesis. Turns off retries for SubscribeToShard
     */
    private static final RetryPolicy DEFAULT =
        AwsRetryPolicy.defaultRetryPolicy().toBuilder()
                      .retryCondition(AndRetryCondition.create(
                          c -> !(c.originalRequest() instanceof SubscribeToShardRequest),
                          AwsRetryPolicy.defaultRetryCondition()))
                      .build();

    private KinesisRetryPolicy() {

    }

    /**
     * @return Default retry policy used by Kinesis
     */
    public static RetryPolicy defaultPolicy() {
        return DEFAULT;
    }
}

