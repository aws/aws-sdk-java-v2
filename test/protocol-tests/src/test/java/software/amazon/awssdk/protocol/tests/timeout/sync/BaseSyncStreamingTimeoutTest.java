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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.protocol.tests.timeout.BaseTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;

public abstract class BaseSyncStreamingTimeoutTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;
    protected static final int TIMEOUT = 1000;
    protected static final int DELAY_BEFORE_TIMEOUT = 100;

    @Before
    public void setup() {
        client = ProtocolRestJsonClient.builder()
                                       .region(Region.US_WEST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                       .overrideConfiguration(clientOverrideConfiguration())
                                       .build();
    }

    @Test
    public void slowFileTransformer_shouldThrowTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowFileResponseTransformer<>()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    @Test
    public void slowBytesTransformer_shouldThrowTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowBytesResponseTransformer<>()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    @Test
    public void slowInputTransformer_shouldThrowTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowInputStreamResponseTransformer<>()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    @Test
    public void slowCustomResponseTransformer_shouldThrowTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        assertThatThrownBy(() -> client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new BaseTimeoutTest.SlowCustomResponseTransformer()))
            .isInstanceOf(expectedException());
        verifyInterruptStatusClear();
    }

    abstract Class<? extends Exception> expectedException();

    abstract ClientOverrideConfiguration clientOverrideConfiguration();

    private void verifyInterruptStatusClear() {
        assertThat(Thread.currentThread().isInterrupted()).isFalse();
    }

}
