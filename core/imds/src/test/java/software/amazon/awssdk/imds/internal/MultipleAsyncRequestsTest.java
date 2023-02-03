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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.imds.TestConstants.AMI_ID_RESOURCE;
import static software.amazon.awssdk.imds.TestConstants.EC2_METADATA_TOKEN_TTL_HEADER;
import static software.amazon.awssdk.imds.TestConstants.TOKEN_HEADER;
import static software.amazon.awssdk.imds.TestConstants.TOKEN_RESOURCE_PATH;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

@WireMockTest
class MultipleAsyncRequestsTest {

    private int port;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) {
        this.port = wiremock.getHttpPort();
    }

    @Test
    void multipleRequests() {
        int totalRequests = 128;
        String tokenValue = "some-token";

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(
            aResponse().withBody(tokenValue).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, "21600")));
        for (int i = 0; i < totalRequests; i++) {
            ResponseDefinitionBuilder responseStub = aResponse().withStatus(200).withBody("response::" + i);
            stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE + "/" + i)).willReturn(responseStub));
        }
        Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.builder()
                                                              .endpoint(URI.create("http://localhost:" + this.port))
                                                              .build();
        List<CompletableFuture<Ec2MetadataResponse>> requests = Stream.iterate(0, x -> x + 1)
                                                                      .map(i -> client.get(AMI_ID_RESOURCE + "/" + i))
                                                                      .limit(totalRequests)
                                                                      .collect(Collectors.toList());
        CompletableFuture<List<Ec2MetadataResponse>> responses = CompletableFuture
            .allOf(requests.toArray(new CompletableFuture[0]))
            .thenApply(unusedVoid -> requests.stream()
                                             .map(CompletableFuture::join)
                                             .collect(Collectors.toList()));

        List<Ec2MetadataResponse> resolvedResponses = responses.join();
        for (int i = 0; i < totalRequests; i++) {
            Ec2MetadataResponse response = resolvedResponses.get(i);
            assertThat(response.asString()).isEqualTo("response::" + i);
        }
        verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        verify(exactly(totalRequests), getRequestedFor(urlPathMatching(AMI_ID_RESOURCE + "/" + "\\d+"))
            .withHeader(TOKEN_HEADER, equalTo(tokenValue)));

    }
}
