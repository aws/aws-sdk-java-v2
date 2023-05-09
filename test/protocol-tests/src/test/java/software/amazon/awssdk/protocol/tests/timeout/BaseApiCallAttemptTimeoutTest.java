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

public abstract class BaseApiCallAttemptTimeoutTest extends BaseTimeoutTest {

    protected static final Duration API_CALL_ATTEMPT_TIMEOUT = Duration.ofMillis(100);
    protected static final Duration DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT = Duration.ofMillis(50);
    protected static final Duration DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT = Duration.ofMillis(150);

    @Test
    public void nonstreamingOperation200_finishedWithinTime_shouldSucceed() throws Exception {
        stubSuccessResponse(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT);
        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation200_notFinishedWithinTime_shouldTimeout() {
        stubSuccessResponse(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT);
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation500_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubErrorResponse(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT);
        verifyFailedResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation500_notFinishedWithinTime_shouldTimeout() {
        stubErrorResponse(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT);
        verifyTimedOut();
    }

    @Test
    public void streamingOperation_finishedWithinTime_shouldSucceed() throws Exception {
        stubSuccessResponse(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT);
        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void streamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubSuccessResponse(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT);
        verifyTimedOut();
    }

    @Test
    public void firstAttemptTimeout_retryFinishWithInTime_shouldNotTimeout() throws Exception {
        mockHttpClient().stubResponses(Pair.of(mockResponse(200), DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT),
                                       Pair.of(mockResponse(200), DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT));

        assertThat(retryableCallable().call()).isNotNull();
        verifyRequestCount(2);
    }

    @Test
    public void firstAttemptTimeout_retryFinishWithInTime500_shouldNotTimeout() {
        mockHttpClient().stubResponses(Pair.of(mockResponse(200), DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT),
                                       Pair.of(mockResponse(500), DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT));
        verifyRetraybleFailedResponseNotTimedOut();
        verifyRequestCount(2);
    }

    @Test
    public void allAttemptsNotFinishedWithinTime_shouldTimeout() {
        stubSuccessResponse(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT);
        verifyRetryableTimeout();
    }
}
