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

package software.amazon.awssdk.protocol.tests.timeout;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.protocol.wiremock.WireMockUtils.verifyRequestCount;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test apiCallTimeout feature for asynchronous operations.
 */
public class AsyncApiCallTimeoutTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private static final String STREAMING_OUTPUT_PATH = "/2016-03-11/streamingOutputOperation";
    private static final int TIMEOUT = 1000;
    private static final int DELAY_BEFORE_TIMEOUT = 100;
    private static final int DELAY_AFTER_TIMEOUT = 1200;
    private ProtocolRestJsonAsyncClient client;
    private ProtocolRestJsonAsyncClient clientWithRetry;

    @Before
    public void setup() {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                            .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(TIMEOUT))
                                                                         .retryPolicy(RetryPolicy.none()))
                                            .build();

        clientWithRetry = ProtocolRestJsonAsyncClient.builder()
                                                     .region(Region.US_WEST_1)
                                                     .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                     .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                     .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMillis(TIMEOUT))
                                                                                  .retryPolicy(RetryPolicy.builder().numRetries(1).build()))
                                                     .build();
    }

    @Test
    public void nonstreamingOperation_finishedWithinTime_shouldNotTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_BEFORE_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
    }

    @Test
    public void nonstreamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void nonstreamingOperation500_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withFixedDelay(DELAY_AFTER_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void nonstreamingOperation500_finishedWithinTime_shouldNotTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withFixedDelay(DELAY_BEFORE_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ProtocolRestJsonException.class);
    }

    @Test
    public void nonstreamingOperation_retrySucceeded_FinishedWithinTime_shouldNotTimeout() {

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


        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = clientWithRetry.allTypes(SdkBuilder::build);

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
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


        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = clientWithRetry.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
        verifyRequestCount(2, wireMock);
    }

    @Test
    public void streamingOperation_finishedWithinTime_shouldNotTimeout() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH))
                    .willReturn(aResponse().withStatus(200).withBody("test").withFixedDelay(DELAY_BEFORE_TIMEOUT)));

        ResponseBytes<StreamingOutputOperationResponse> response =
                client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                AsyncResponseTransformer.toBytes()).join();

        byte[] arrayCopy = response.asByteArray();
        assertThat(arrayCopy).containsExactly('t', 'e', 's', 't');
    }

    @Test
    public void streamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH))
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));

        assertThatThrownBy(() ->
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBytes()).join())
            .hasRootCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void increaseTimeoutInRequestOverrideConfig_shouldTakePrecedence() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture =
            client.allTypes(b -> b.overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMillis(DELAY_AFTER_TIMEOUT + 1000))));

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
    }

}
