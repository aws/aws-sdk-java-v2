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

package software.amazon.awssdk.protocol.tests.timeout.sync;

import java.time.Duration;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;

/**
 * A set of tests to test ApiCallTimeout for synchronous streaming operations because they are tricky.
 */
public class SyncStreamingOperationApiCallTimeoutTest extends BaseSyncStreamingTimeoutTest {

    @Override
    Class<? extends Exception> expectedException() {
        return ApiCallTimeoutException.class;
    }

    @Override
    ClientOverrideConfiguration clientOverrideConfiguration() {
        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofMillis(TIMEOUT))
                                          .retryStrategy(AwsRetryStrategy.none())
                                          .build();
    }
}
