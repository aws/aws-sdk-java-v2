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

package software.amazon.awssdk.protocol.tests.timeout;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.utils.Pair;

/**
 * Contains common scenarios to test timeout feature.
 */
public abstract class BaseApiCallTimeoutTest extends BaseTimeoutTest {

    protected static final Duration TIMEOUT = Duration.ofMillis(300);

    protected static final Duration DELAY_BEFORE_TIMEOUT = Duration.ofMillis(10);
    protected static final Duration DELAY_AFTER_TIMEOUT = Duration.ofMillis(500);

    @Test
    public void nonstreamingOperation_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubSuccessResponse(DELAY_BEFORE_TIMEOUT);
        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubSuccessResponse(DELAY_AFTER_TIMEOUT);
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation500_notFinishedWithinTime_shouldTimeout() {
        stubErrorResponse(DELAY_AFTER_TIMEOUT);
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation500_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubErrorResponse(DELAY_BEFORE_TIMEOUT);
        verifyFailedResponseNotTimedOut();
    }

    @Test
    public void streamingOperation_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubSuccessResponse(DELAY_BEFORE_TIMEOUT);
        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void streamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubSuccessResponse(DELAY_AFTER_TIMEOUT);
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation_retrySucceeded_FinishedWithinTime_shouldNotTimeout() throws Exception {
        mockHttpClient().stubResponses(Pair.of(mockResponse(500), DELAY_BEFORE_TIMEOUT),
                                       Pair.of(mockResponse(200), DELAY_BEFORE_TIMEOUT));

        assertThat(retryableCallable().call()).isNotNull();
        verifyRequestCount(2);
    }

    @Test
    public void nonstreamingOperation_retryWouldSucceed_notFinishedWithinTime_shouldTimeout() {
        mockHttpClient().stubResponses(Pair.of(mockResponse(500), DELAY_BEFORE_TIMEOUT),
                                       Pair.of(mockResponse(200), DELAY_AFTER_TIMEOUT));

        verifyRetryableTimeout();
        verifyRequestCount(2);
    }
}
