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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test timeout feature for synchronous operations when both apiCallTimeout and apiCallAttemptTimeout are enabled.
 */
public class SyncTimeoutTests {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private static final int API_CALL_TIMEOUT = 1200;
    private static final int API_CALL_ATTEMPT_TIMEOUT = 1000;

    private static final int DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT = 100;
    private static final int DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT = 1100;
    private ProtocolRestJsonClient client;

    @Before
    public void setup() {
        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_WEST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .overrideConfiguration(
                                           b -> b.apiCallTimeout(Duration.ofMillis(API_CALL_TIMEOUT))
                                                 .apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
                                                 .retryPolicy(RetryPolicy.none()))
                                       .build();
    }

    @Test
    public void attemptsTimeout_shouldThrowApiCallAttemptTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        assertThatThrownBy(() -> client.allTypes(SdkBuilder::build))
            .isInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void attemptFinishWithinTime_shouldSucceed() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        assertThat(client.allTypes(SdkBuilder::build)).isNotNull();
    }

    @Test
    public void bothTimeout_shouldThrowApiCallTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        assertThatThrownBy(() -> client.allTypes(b -> b.overrideConfiguration(
            c -> c.apiCallTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT)))))
            .isInstanceOf(ApiCallTimeoutException.class);
    }
}
