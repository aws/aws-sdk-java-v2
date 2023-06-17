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

package software.amazon.awssdk.protocol.tests.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocoljsonrpc.ProtocolJsonRpcAsyncClient;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesRequest;
import software.amazon.awssdk.services.protocoljsonrpc.model.AllTypesResponse;
import software.amazon.awssdk.services.protocoljsonrpc.model.ProtocolJsonRpcException;

public class AsyncAwsJsonRetryTest {

    private static final String PATH = "/";
    private static final String JSON_BODY = "{\"StringMember\":\"foo\"}";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolJsonRpcAsyncClient client;

    @Before
    public void setupClient() {
        client = ProtocolJsonRpcAsyncClient.builder()
                                           .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create
                                                                                                                        ("akid", "skid")))
                                           .region(Region.US_EAST_1)
                                           .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                           .build();
    }

    @Test
    public void shouldRetryOn500() {
        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at 500")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(500)));

        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at 500")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(JSON_BODY)));

        AllTypesResponse allTypesResponse = client.allTypes(AllTypesRequest.builder().build()).join();
        assertThat(allTypesResponse).isNotNull();
    }

    @Test
    public void shouldRetryOnRetryableAwsErrorCode() {
        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at PriorRequestNotComplete")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(400)
                                    .withHeader("x-amzn-ErrorType", "PriorRequestNotComplete")
                                    .withBody("\"{\"__type\":\"PriorRequestNotComplete\",\"message\":\"Blah "
                                              + "error\"}\"")));

        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at PriorRequestNotComplete")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(JSON_BODY)));

        AllTypesResponse allTypesResponse = client.allTypes(AllTypesRequest.builder().build()).join();
        assertThat(allTypesResponse).isNotNull();
    }

    @Test
    public void shouldRetryOnAwsThrottlingErrorCode() {
        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at SlowDown")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(400)
                                    .withHeader("x-amzn-ErrorType", "SlowDown")
                                    .withBody("\"{\"__type\":\"SlowDown\",\"message\":\"Blah "
                                              + "error\"}\"")));

        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at SlowDown")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(JSON_BODY)));

        AllTypesResponse allTypesResponse = client.allTypes(AllTypesRequest.builder().build()).join();
        assertThat(allTypesResponse).isNotNull();
    }

    @Test
    public void retryStrategyNone_shouldNotRetry() {
        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at 500")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(500)));

        stubFor(post(urlEqualTo(PATH))
                    .inScenario("retry at 500")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody(JSON_BODY)));

        ProtocolJsonRpcAsyncClient clientWithNoRetry =
            ProtocolJsonRpcAsyncClient.builder()
                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                       "skid")))
                                      .region(Region.US_EAST_1)
                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                      .overrideConfiguration(c -> c.retryStrategy(AwsRetryStrategy.none()))
                                      .build();

        assertThatThrownBy(() -> clientWithNoRetry.allTypes(AllTypesRequest.builder().build()).join())
            .hasCauseInstanceOf(ProtocolJsonRpcException.class);
    }
}