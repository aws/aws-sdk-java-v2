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

package software.amazon.awssdk.imds;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER;
import static com.github.tomakehurst.wiremock.http.Fault.EMPTY_RESPONSE;
import static com.github.tomakehurst.wiremock.http.Fault.MALFORMED_RESPONSE_CHUNK;
import static com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * Unit Tests to test the Ec2Metadata Client functionality
 */
@RunWith(MockitoJUnitRunner.class)
public class Ec2MetadataClientTest {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";

    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";

    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";

    private static final String EC2_METADATA_ROOT = "/latest/meta-data";

    private static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    public Ec2MetadataClient ec2MetadataClient;

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());
        this.ec2MetadataClient = Ec2MetadataClient.builder()
                                                  .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                                  .build();
    }

    @Test
    public void get_succeedOnFirstAttempt() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        MetadataResponse metadataResponse = ec2MetadataClient.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(1, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_doesNotRetryOn4XX() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}").withStatus(404)));

        assertThatThrownBy(() -> ec2MetadataClient.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("metadata")
            .isInstanceOf(SdkClientException.class);
        WireMock.verify(1, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void getToken_doesNotRetryOn4XX() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(403)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> ec2MetadataClient.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("token")
            .isInstanceOf(SdkClientException.class);
        WireMock.verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(0, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_failsThriceWithFixedDelay() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFixedDelay(2_000)));

        SdkHttpClient client = UrlConnectionHttpClient.builder().socketTimeout(Duration.ofMillis(500)).build();
        Ec2MetadataClient ec2MetadataRequest = Ec2MetadataClient.builder()
                                                                .httpClient(client)
                                                                .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                                                .build();
        assertThatThrownBy(() -> ec2MetadataRequest.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("Exceeded maximum number of retries.")
            .isInstanceOf(SdkClientException.class);

        WireMock.verify(4, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(4, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }


    @Test
    public void getToken_failsThriceWithFixedDelay() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                    .willReturn(aResponse().withBody("some-token").withFixedDelay(2_000)));

        SdkHttpClient client = UrlConnectionHttpClient.builder().socketTimeout(Duration.ofMillis(500)).build();
        Ec2MetadataClient ec2MetadataRequest = Ec2MetadataClient.builder()
                                                                .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                                                .httpClient(client)
                                                                .build();
        assertThatThrownBy(() -> ec2MetadataRequest.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("Exceeded maximum number of retries.")
            .isInstanceOf(SdkClientException.class);

        WireMock.verify(4, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(0, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void getToken_failsOnceThenSucceedOnSecondAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withFault(CONNECTION_RESET_BY_PEER))
                                                        .willSetStateTo("Cause Success"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withStatus(200).withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        MetadataResponse metadataResponse = ec2MetadataClient.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(2, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_failsOnceThenSucceedOnSecondAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE))
                                                        .willSetStateTo("Cause Success"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("{}")));


        MetadataResponse metadataResponse = ec2MetadataClient.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(2, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(2, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));

    }

    @Test
    public void get_whenGetTokenFailsOnceThenOk_shouldSucceedOnSecondAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withFault(EMPTY_RESPONSE))
                                                        .willSetStateTo("Cause Success"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Cause Success")
                                                    .willReturn(aResponse().withBody("{}")));


        MetadataResponse metadataResponse = ec2MetadataClient.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(2, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void getToken_failedTwice_shouldSucceedOnThirdAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withFault(MALFORMED_RESPONSE_CHUNK))
                                                        .willSetStateTo("Try-2"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-2")
                                                        .willReturn(aResponse().withFault(CONNECTION_RESET_BY_PEER))
                                                        .willSetStateTo("Try-3"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-3")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE))
                    .withHeader(TOKEN_HEADER, equalTo("some-token"))
                    .willReturn(aResponse().withBody("{}")));

        MetadataResponse metadataResponse = ec2MetadataClient.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(3, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void getToken_failsTwiceWithIOExceptionThenOK_shouldSucceedOnThirdAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withFault(MALFORMED_RESPONSE_CHUNK))
                                                        .willSetStateTo("Try-2"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-2")
                                                        .willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE))
                                                        .willSetStateTo("Try-3"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-3")
                                                        .willReturn(aResponse().withBody("valid-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE))
                    .withHeader(TOKEN_HEADER, equalTo("valid-token"))
                    .willReturn(aResponse().withBody("{}")));

        MetadataResponse metadataResponse = ec2MetadataClient.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(3, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(1, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("valid-token")));
    }

    @Test
    public void get_failsEveryAttempt_shouldThrowOnFourthAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFault(MALFORMED_RESPONSE_CHUNK)));

        assertThatThrownBy(() -> ec2MetadataClient.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("Exceeded maximum number of retries.")
            .isInstanceOf(SdkClientException.class);

        WireMock.verify(4, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(4, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_customRetryAmount() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE)));

        int numRetries = 7;
        BackoffStrategy noWait = FixedDelayBackoffStrategy.create(Duration.ofMillis(10));
        Ec2MetadataClient ec2MetadataRequest = Ec2MetadataClient.builder()
                                                                .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                                                .retryPolicy(Ec2MetadataRetryPolicy.builder()
                                                                                       .backoffStrategy(noWait)
                                                                                       .numRetries(numRetries)
                                                                                       .build())
                                                                .build();
        assertThatThrownBy(() -> ec2MetadataRequest.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("Exceeded maximum number of retries.")
            .isInstanceOf(SdkClientException.class);

        WireMock.verify(8, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(8, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_withOneMaxRetries_shouldNotRetry() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFault(RANDOM_DATA_THEN_CLOSE)));

        Ec2MetadataClient ec2MetadataRequest = Ec2MetadataClient.builder()
                                                                .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
                                                                .retryPolicy(Ec2MetadataRetryPolicy.none())
                                                                .build();
        assertThatThrownBy(() -> ec2MetadataRequest.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("Exceeded maximum number of retries.")
            .isInstanceOf(SdkClientException.class);

        WireMock.verify(1, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(1, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }
}
