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

/**
 * Contains common scenarios to test timeout feature.
 */
public abstract class BaseApiCallTimeoutTest extends BaseTimeoutTest {

    protected static final int TIMEOUT = 1000;
    protected static final int DELAY_BEFORE_TIMEOUT = 100;
    protected static final int DELAY_AFTER_TIMEOUT = 1200;

    @Test
    public void nonstreamingOperation_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_BEFORE_TIMEOUT)));
        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation500_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withFixedDelay(DELAY_AFTER_TIMEOUT)));
        verifyTimedOut();
    }

    @Test
    public void nonstreamingOperation500_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withFixedDelay(DELAY_BEFORE_TIMEOUT)));
        verifyFailedResponseNotTimedOut();
    }

    @Test
    public void nonstreamingOperation_retrySucceeded_FinishedWithinTime_shouldNotTimeout() throws Exception {

        stubFor(post(anyUrl())
                    .inScenario("retry at 500")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(500).withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("retry at 500")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}").withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        assertThat(retryableCallable().call()).isNotNull();
    }

    @Test
    public void nonstreamingOperation_retryWouldSucceed_notFinishedWithinTime_shouldTimeout() {

        stubFor(post(anyUrl())
                    .inScenario("retry at 500")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(500).withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("retry at 500")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));


        verifyRetryableTimeout();
        verifyRequestCount(2, wireMock());
    }

    @Test
    public void streamingOperation_finishedWithinTime_shouldNotTimeout() throws Exception {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        verifySuccessResponseNotTimedOut();
    }

    @Test
    public void streamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));

        verifyTimedOut();
    }
}
