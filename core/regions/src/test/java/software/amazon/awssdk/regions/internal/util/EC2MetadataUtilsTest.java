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

package software.amazon.awssdk.regions.internal.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;

public class EC2MetadataUtilsTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";

    private static final String EC2_METADATA_ROOT = "/latest/meta-data";

    private static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());
        EC2MetadataUtils.clearCache();
    }

    @Test
    public void getToken_queriesCorrectPath() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));

        String token = EC2MetadataUtils.getToken();
        assertThat(token).isEqualTo("some-token");

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
    }

    @Test
    public void getAmiId_queriesAndIncludesToken() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        EC2MetadataUtils.getAmiId();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    public void getAmiId_tokenQueryTimeout_fallsBackToInsecure() {

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withFixedDelay(Integer.MAX_VALUE)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        EC2MetadataUtils.getAmiId();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withoutHeader(TOKEN_HEADER));
    }

    @Test
    public void getAmiId_queriesTokenResource_403Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(403).withBody("oops")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        EC2MetadataUtils.getAmiId();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withoutHeader(TOKEN_HEADER));
    }

    @Test
    public void getAmiId_queriesTokenResource_404Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(404).withBody("oops")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        EC2MetadataUtils.getAmiId();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withoutHeader(TOKEN_HEADER));
    }

    @Test
    public void getAmiId_queriesTokenResource_405Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(405).withBody("oops")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        EC2MetadataUtils.getAmiId();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        WireMock.verify(getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)).withoutHeader(TOKEN_HEADER));
    }

    @Test
    public void getAmiId_queriesTokenResource_400Error_throws() {
        thrown.expect(SdkClientException.class);
        thrown.expectMessage("token");

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(400).withBody("oops")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));

        EC2MetadataUtils.getAmiId();
    }
}
