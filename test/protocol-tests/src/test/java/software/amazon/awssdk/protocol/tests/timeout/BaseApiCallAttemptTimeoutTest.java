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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.protocol.wiremock.WireMockUtils.verifyRequestCount;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.Test;

public abstract class BaseApiCallAttemptTimeoutTest extends BaseTimeoutTest {

    protected static final int API_CALL_ATTEMPT_TIMEOUT = 800;
    protected static final int DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT = 100;
    protected static final int DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT = 1000;

    @Test
    public void nonstreamingOperation200_finishedWithinTime_shouldSucceed() throws Exception {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation200_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation500_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500)
                                           .withHeader("x-amzn-ErrorType", "EmptyModeledException")
                                           .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        verifyFailedResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation500_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        verifyTimedOut();
    }

    @Test
    public void streamingOperation_finishedWithinTime_shouldSucceed() throws Exception {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));

        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void streamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        verifyTimedOut();
    }

    @Test
    public void firstAttemptTimeout_retryFinishWithInTime_shouldNotTimeout() throws Exception {
        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));

        assertThat(retryableCallable().call()).isNotNull();
        verifyRequestCount(2, wireMock());
    }

    @Test
    public void firstAttemptTimeout_retryFinishWithInTime500_shouldNotTimeout() throws Exception {
        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        verifyRetraybleFailedResponseNotTimedOut();
    }

    @Test
    public void allAttemtsNotFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .inScenario("timed out in both attempts")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("timed out in both attempts")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        verifyRetryableTimeout();
    }
}
