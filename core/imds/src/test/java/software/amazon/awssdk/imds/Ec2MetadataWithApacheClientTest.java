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
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;

/**
 * Unit Tests to test the Ec2Metadata Client functionality with Apache HttpClient.
 */
@RunWith(MockitoJUnitRunner.class)
public class Ec2MetadataWithApacheClientTest {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";

    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";

    private static final String EC2_METADATA_ROOT = "/latest/meta-data";

    private static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    private Ec2Metadata ec2Metadata;

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());
        this.ec2Metadata = Ec2Metadata.builder().httpClient(ApacheHttpClient.create()).build();
    }

    @Test
    public void get_failedThriceWith401() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}").withStatus(401)));

        assertThatThrownBy(() -> ec2Metadata.get("/latest/meta-data/ami-id"))
            .hasMessageContaining("Exceeded maximum number of retries.")
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void get_failedOnceWith401_shouldSucceedOnSecondAttempt() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(401))
                                                        .willSetStateTo("Cause Success"));

        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("{}")));


        MetadataResponse metadataResponse = ec2Metadata.get("/latest/meta-data/ami-id");
        assertThat(metadataResponse.asString()).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

}
