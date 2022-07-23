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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

/**
 * Unit Tests to test the Ec2Metadata Client functionality configured with ApacheHttpClient.
 */
@RunWith(MockitoJUnitRunner.class)
public class Ec2MetadataTestWithApacheClient {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";

    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";

    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";

    private static final String EC2_METADATA_ROOT = "/latest/meta-data";

    private static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";

    @Mock
    private Ec2Metadata ec2Metadata;

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());

    }

    @Test
    public void get_whenDummyResponseIsReturned(){

        MetadataResponse metadataResponse = new MetadataResponse("IMDS");
        when(ec2Metadata.get("/ami-id")).thenReturn(metadataResponse);
        assertThat(ec2Metadata.get("/ami-id").asString()).isEqualTo("IMDS");

    }

    @Test
    public void verifyEc2Metadata_equalsAndHashcode(){

        EqualsVerifier.forClass(Ec2Metadata.class)
                      .usingGetClass()
                      .verify();
    }

    @Test
    public void get_shouldSucceedOnFirstAttempt() throws IOException {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        SdkHttpClient httpClient = ApacheHttpClient.create();
        Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();

        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_failedThriceWith404() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}").withStatus(404)));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("metadata")
          .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void get_failedThriceWith401() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}").withStatus(401)));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("Exceeded maximum number of retries.")
          .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void get_failedThriceWithFixedDelay() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFixedDelay(Integer.MAX_VALUE)));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("Exceeded maximum number of retries.")
          .isInstanceOf(SdkClientException.class);

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void getToken_failedThriceWith403() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(403)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("token")
          .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void getToken_failedThriceWith401() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(401)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("Exceeded maximum number of retries.")
          .isInstanceOf(SdkClientException.class);
        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void getToken_failedThriceWithFixedDelay() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withFixedDelay(Integer.MAX_VALUE)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("Exceeded maximum number of retries.")
          .isInstanceOf(SdkClientException.class);

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
    }

    @Test
    public void getToken_failedOnceWith401_shouldSucceedOnSecondAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(401))
                                                        .willSetStateTo("Cause Success"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        SdkHttpClient httpClient = ApacheHttpClient.create();
        Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void get_failedOnceWith401_shouldSucceedOnSecondAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs(STARTED)
                                                    .willReturn(aResponse().withStatus(401))
                                                    .willSetStateTo("Cause Success"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Cause Success")
                                                    .willReturn(aResponse().withBody("{}")));


        SdkHttpClient httpClient = ApacheHttpClient.create();
        Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void getAndGetToken_failedOnceWith401_shouldSucceedOnSecondAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(401))
                                                        .willSetStateTo("Cause Success"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs(STARTED)
                                                    .willReturn(aResponse().withStatus(401))
                                                    .willSetStateTo("Cause Success"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Cause Success")
                                                    .willReturn(aResponse().withBody("{}")));


        SdkHttpClient httpClient = ApacheHttpClient.create();
        Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void getAndGetToken_failedOnceWith403_shouldSucceedOnSecondAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(403))
                                                        .willSetStateTo("Cause Success"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs(STARTED)
                                                    .willReturn(aResponse().withStatus(403))
                                                    .willSetStateTo("Cause Success"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Cause Success")
                                                    .willReturn(aResponse().withBody("{}")));

        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("Could not retrieve token ");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void getToken_failedTwiceWith200_shouldSucceedOnThirdAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(200))
                                                        .willSetStateTo("Try-2"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-2")
                                                        .willReturn(aResponse().withStatus(200))
                                                        .willSetStateTo("Try-3"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-3")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        SdkHttpClient httpClient = ApacheHttpClient.create();
        Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void getToken_failedTwiceWithIOExceptionAnd200_shouldSucceedOnThirdAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK))
                                                        .willSetStateTo("Try-2"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-2")
                                                        .willReturn(aResponse().withStatus(200))
                                                        .willSetStateTo("Try-3"));

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Try-3")
                                                        .willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        SdkHttpClient httpClient = ApacheHttpClient.create();
        Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }
    @Test
    public void get_failedTwiceWith401_shouldFailOnThirdAttempt() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs(STARTED)
                                                    .willReturn(aResponse().withStatus(401))
                                                    .willSetStateTo("Try-2"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Try-2")
                                                    .willReturn(aResponse().withStatus(401))
                                                    .willSetStateTo("Try-3"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Try-3")
                                                    .willReturn(aResponse().withStatus(401)));


        assertThatThrownBy(() -> {
            SdkHttpClient httpClient = ApacheHttpClient.create();
            Ec2Metadata ec2Metadata = Ec2Metadata.builder().httpClient(httpClient).build();
            MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        }).hasMessageContaining("Exceeded maximum number of retries.")
          .isInstanceOf(SdkClientException.class);

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void verifyEc2MetadataRetryPolicy_equalsAndHashcode(){

        EqualsVerifier.forClass(Ec2MetadataRetryPolicy.class).usingGetClass().verify();
    }

}