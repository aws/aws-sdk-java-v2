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

package software.amazon.awssdk.services.customizeduseragent;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjsonwithinternalconfig.ProtocolRestJsonWithInternalConfigAsyncClient;
import software.amazon.awssdk.services.protocolrestjsonwithinternalconfig.ProtocolRestJsonWithInternalConfigClient;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class InternalUserAgentTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonWithInternalConfigClient client;

    private ProtocolRestJsonWithInternalConfigAsyncClient asyncClient;
    private ProtocolRestJsonClient clientWithoutInternalConfig;
    private ProtocolRestJsonAsyncClient asyncClientWithoutInternalConfig;

    @Before
    public void setupClient() {
        client = ProtocolRestJsonWithInternalConfigClient.builder()
                                                         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                         .region(Region.US_EAST_1)
                                                         .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                         .build();

        clientWithoutInternalConfig = ProtocolRestJsonClient.builder()
                                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                            .region(Region.US_EAST_1)
                                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                            .build();

        asyncClient = ProtocolRestJsonWithInternalConfigAsyncClient.builder()
                                                                   .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                   .region(Region.US_EAST_1)
                                                                   .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                                   .build();

        asyncClientWithoutInternalConfig = ProtocolRestJsonAsyncClient.builder()
                                                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                                      .region(Region.US_EAST_1)
                                                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                                      .build();
    }

    @Test
    public void syncWithInternalUserAgent_shouldContainInternalUserAgent() {
        stubResponse();
        client.oneOperation(SdkBuilder::build);
        verifyUserAgent();
    }


    @Test
    public void asyncWithInternalUserAgent_shouldContainInternalUserAgent() {
        stubResponse();
        asyncClient.oneOperation(SdkBuilder::build).join();
        verifyUserAgent();
    }

    @Test
    public void syncWithoutInternalUserAgent_shouldNotContainInternalUserAgent() {
        stubResponse();
        clientWithoutInternalConfig.allTypes(SdkBuilder::build);
       verifyNotContainUserAgent();
    }

    @Test
    public void asyncWithoutInternalUserAgent_shouldNotContainInternalUserAgent() {
        stubResponse();
        asyncClientWithoutInternalConfig.allTypes(SdkBuilder::build).join();
        verifyNotContainUserAgent();
    }

    private void verifyUserAgent() {
        verify(postRequestedFor(anyUrl()).withHeader("user-agent", containing("md/foobar")));
    }

    private void verifyNotContainUserAgent() {
        verify(postRequestedFor(anyUrl()).withHeader("user-agent", notMatching(".*md/foobar.*")));
    }

    private void stubResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withBody("{}")));
    }
}
