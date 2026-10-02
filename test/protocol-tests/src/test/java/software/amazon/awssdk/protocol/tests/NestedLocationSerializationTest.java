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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.NestedLocationOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.NestedLocationOperationResponse;
import software.amazon.awssdk.services.protocolrestjson.model.NestedShapeWithLocations;

/**
 * Verifies that HTTP binding locations on non-input shapes are ignored per the Smithy spec,
 * and the members are serialized into the request body instead.
 */
public class NestedLocationSerializationTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;

    @Before
    public void setup() {
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();
    }

    @Test
    public void nestedMemberWithLocation_serializedToBodyNotQueryParam() {
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        client.nestedLocationOperation(NestedLocationOperationRequest.builder()
                  .topLevelQueryParam("topValue")
                  .nested(NestedShapeWithLocations.builder()
                              .nestedQueryParam("nestedValue")
                              .stringMember("hello")
                              .build())
                  .build());

        verify(postRequestedFor(anyUrl()).withQueryParam("topLevel", equalTo("topValue")));

        verify(postRequestedFor(anyUrl()).withRequestBody(
            equalToJson("{\"Nested\":{\"NestedQueryParam\":\"nestedValue\",\"StringMember\":\"hello\"}}")));
    }

    @Test
    public void nestedMemberWithLocation_deserializedFromBodyNotHeader() {
        stubFor(post(anyUrl()).willReturn(aResponse()
            .withStatus(200)
            .withHeader("x-amz-top-level", "headerValue")
            .withBody("{\"NestedResult\":{\"NestedHeader\":\"from-body\",\"Value\":\"hello\"}}")));

        NestedLocationOperationResponse response = client.nestedLocationOperation(
            NestedLocationOperationRequest.builder().build());

        assertThat(response.topLevelHeader()).isEqualTo("headerValue");
        assertThat(response.nestedResult().nestedHeader()).isEqualTo("from-body");
        assertThat(response.nestedResult().value()).isEqualTo("hello");
    }
}
