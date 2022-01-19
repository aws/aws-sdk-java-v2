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

package software.amazon.awssdk.protocol.tests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlAsyncClient;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;
import software.amazon.awssdk.services.protocolrestxml.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestxml.model.ProtocolRestXmlResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class XmlMetadataTest {

    private static final String REQUEST_ID = "abcd";

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestXmlClient client;

    private ProtocolRestXmlAsyncClient asyncClient;

    @Before
    public void setupClient() {
        client = ProtocolRestXmlClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                    .build();

        asyncClient = ProtocolRestXmlAsyncClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                              .region(Region.US_EAST_1)
                                              .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                              .build();
    }

    @Test
    public void requestIdInHeaderButNotXml_ShouldContainsResponseMetadata() {
        stubResponseWithHeaders();
        AllTypesResponse allTypesResponse = asyncClient.allTypes(SdkBuilder::build).join();
        verifyResponseMetadata(allTypesResponse);
    }

    @Test
    public void requestIdNotInXmlOrHeader_responseMetadataShouldBeUnknown() {
        stubResponseWithoutHeaders();
        AllTypesResponse allTypesResponse = client.allTypes(SdkBuilder::build);
        verifyUnknownResponseMetadata(allTypesResponse);
    }

    private void verifyResponseMetadata(ProtocolRestXmlResponse xmlResponse) {
        assertThat(xmlResponse.responseMetadata()).isNotNull();
        assertThat(xmlResponse.responseMetadata().requestId()).isEqualTo(REQUEST_ID);
    }

    private void verifyUnknownResponseMetadata(ProtocolRestXmlResponse xmlResponse) {
        assertThat(xmlResponse.responseMetadata()).isNotNull();
        assertThat(xmlResponse.responseMetadata().requestId()).isEqualTo("UNKNOWN");
    }

    private void stubResponseWithHeaders() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-amzn-RequestId", REQUEST_ID)
                                           .withBody("<AllTypesResponse/>")));
    }

    private void stubResponseWithoutHeaders() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withBody("<AllTypesResponse/>")));
    }
}
