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

package software.amazon.awssdk.services.customsdkshape;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestxml.ProtocolRestXmlClient;

public class CustomSdkShapeTest {

    private ProtocolRestXmlClient xmlClient;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Mock
    private SdkHttpClient mockHttpClient;

    @Before
    public void setUp() {
        xmlClient = ProtocolRestXmlClient.builder()
                                         .region(Region.US_WEST_2)
                                         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                          "skid")))
                                         .httpClient(mockHttpClient)
                                         .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                         .build();

    }

    @Test
    public void requestPayloadDoesNotContainInjectedCustomShape() {
        stubFor(any(urlMatching(".*"))
                    .willReturn(aResponse().withStatus(200).withBody("<xml></xml>")));
        xmlClient.allTypes(c -> c.sdkPartType("DEFAULT").build());
        verify(anyRequestedFor(anyUrl()).withRequestBody(notMatching("^.*SdkPartType.*$")));
    }

}
