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

package software.amazon.awssdk.imds.internal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.imds.TestConstants.EC2_METADATA_TOKEN_TTL_HEADER;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

@WireMockTest
class CachedTokenClientTest {

    private Ec2MetadataClient.Builder clientBuilder;

    @BeforeEach
    void init(WireMockRuntimeInfo wmRuntimeInfo) {
        this.clientBuilder = Ec2MetadataClient.builder()
                                              .endpoint(URI.create("http://localhost:" + wmRuntimeInfo.getHttpPort()));
    }

    @AfterEach
    void tearDown(WireMockRuntimeInfo wmRuntimeInfo) {
        wmRuntimeInfo.getWireMock().resetMappings();
    }

    @Test
    void get_tokenFailsError4xx_shouldNotRetry() {
        stubFor(put(urlPathEqualTo("/latest/api/token")).willReturn(aResponse().withStatus(400).withBody("ERROR 400")));
        stubFor(get(urlPathEqualTo("/latest/meta-data/ami-id")).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> clientBuilder.build().get("/latest/meta-data/ami-id")).isInstanceOf(SdkClientException.class);
        verify(exactly(1), putRequestedFor(urlPathEqualTo("/latest/api/token"))
            .withHeader("x-aws-ec2-metadata-token-ttl-seconds", equalTo("21600")));
    }

    @Test
    void getToken_failsError5xx_shouldRetryUntilMaxRetriesIsReached() {
        stubFor(put(urlPathEqualTo("/latest/api/token")).willReturn(aResponse().withStatus(500).withBody("ERROR 500")));
        stubFor(get(urlPathEqualTo("/latest/meta-data/ami-id")).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> clientBuilder.build().get("/latest/meta-data/ami-id")).isInstanceOf(SdkClientException.class);
        verify(exactly(4), putRequestedFor(urlPathEqualTo("/latest/api/token"))
            .withHeader("x-aws-ec2-metadata-token-ttl-seconds", equalTo("21600")));
    }

    @Test
    void getToken_failsThenSucceeds_doesCacheTokenThatSucceeds() {
        stubFor(put(urlPathEqualTo("/latest/api/token")).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(500).withBody("Error 500"))
                                                        .willSetStateTo("Cause Success"));
        stubFor(put(urlPathEqualTo("/latest/api/token")).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse()
                                                                        .withBody("token-ok")
                                                                        .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, "21600")));
        stubFor(get(urlPathEqualTo("/latest/meta-data/ami-id")).inScenario("Retry Scenario")
                                                               .whenScenarioStateIs("Cause Success")
                                                               .willReturn(aResponse().withBody("Success")));

        // 3 requests
        Ec2MetadataClient client = clientBuilder.build();
        Ec2MetadataResponse response = client.get("/latest/meta-data/ami-id");
        assertThat(response.asString()).isEqualTo("Success");
        response = client.get("/latest/meta-data/ami-id");
        assertThat(response.asString()).isEqualTo("Success");
        response = client.get("/latest/meta-data/ami-id");
        assertThat(response.asString()).isEqualTo("Success");

        verify(exactly(2), putRequestedFor(urlPathEqualTo("/latest/api/token"))
            .withHeader("x-aws-ec2-metadata-token-ttl-seconds", equalTo("21600")));
        verify(exactly(3), getRequestedFor(urlPathEqualTo("/latest/meta-data/ami-id"))
            .withHeader("x-aws-ec2-metadata-token", equalTo("token-ok")));
    }

    @Test
    void get_multipleCallsSuccess_shouldReuseToken() throws Exception {
        stubFor(put(urlPathEqualTo("/latest/api/token")).willReturn(
            aResponse().withBody("some-token").withHeader(EC2_METADATA_TOKEN_TTL_HEADER, "21600")));
        stubFor(get(urlPathEqualTo("/latest/meta-data/ami-id"))
                    .willReturn(aResponse().withBody("{}").withFixedDelay(800)));

        int tokenTTlSeconds = 4;
        Ec2MetadataClient client = clientBuilder.tokenTtl(Duration.ofSeconds(tokenTTlSeconds)).build();

        int totalRequests = 10;
        for (int i = 0; i < totalRequests; i++) {
            Ec2MetadataResponse response = client.get("/latest/meta-data/ami-id");
            assertThat(response.asString()).isEqualTo("{}");
        }
        verify(exactly(2), putRequestedFor(urlPathEqualTo("/latest/api/token"))
            .withHeader("x-aws-ec2-metadata-token-ttl-seconds", equalTo(String.valueOf(tokenTTlSeconds))));
        verify(exactly(totalRequests), getRequestedFor(urlPathEqualTo("/latest/meta-data/ami-id"))
            .withHeader("x-aws-ec2-metadata-token", equalTo("some-token")));
    }

}
