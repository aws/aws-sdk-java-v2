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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.ExecutableHttpRequest;

/**
 * Unit Tests to test the Ec2Metadata Client functionality
 */
@RunWith(MockitoJUnitRunner.class)
public class Ec2MetadataTest {

    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";

    private static final String EC2_METADATA_ROOT = "/latest/meta-data";

    private static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";

    @Mock
    private Ec2Metadata ec2Metadata;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());

    }

    @Test
    public void when_dummy_string_is_returned(){

        when(ec2Metadata.get("/ami-id")).thenReturn("IMDS");
        assertThat(ec2Metadata.get("/ami-id")).isEqualTo("IMDS");

    }

    @Test
    public void verify_equals_hashcode(){

        EqualsVerifier.forClass(Ec2Metadata.class)
            .usingGetClass()
            .verify();
    }

    @Test
    public void get_AmiId_onMetadataResource_200_Success() throws IOException {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");

        assertThat(data).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_AmiId_onMetadataResource_404Error_throws() throws IOException {

        thrown.expect(SdkServiceException.class);
        thrown.expectMessage("metadata");

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}").withStatus(404)));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");
    }

    @Test
    public void get_AmiId_onMetadataResource_401Error_throws() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}").withStatus(401)));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");

        assertThat(data).isNull();
    }

    @Test
    public void get_AmiId_onMetadataResource_IOException_throws() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withFixedDelay(Integer.MAX_VALUE)));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");

        assertThat(data).isNull();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void get_AmiId_onTokenResource_403Error_throws() throws IOException {

        thrown.expect(SdkServiceException.class);
        thrown.expectMessage("token");

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(403)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");
    }

    @Test
    public void get_AmiId_onTokenResource_401Error_throws() throws IOException {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(401)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");

        assertThat(data).isNull();
        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

    }

    @Test
    public void getAmiId_onTokenResource_IOError_throws() throws IOException {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withFixedDelay(Integer.MAX_VALUE)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");

        assertThat(data).isNull();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
       // WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }
    @Test
    public void getAmiId_onTokenResource_200() throws IOException {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        Ec2Metadata ec2Metadata = Ec2Metadata.builder().endpoint(URI.create("http://localhost:8080")).build();
        String data = ec2Metadata.get("/latest/meta-data/ami-id");

        assertThat(data).isEqualTo("{}");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

}
