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

package software.amazon.awssdk.services.customresponsemetadata;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonResponse;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class CustomResponseMetadataTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonClient client;

    private ProtocolRestJsonAsyncClient asyncClient;

    @Before
    public void setupClient() {
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();

        asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                 .build();
    }

    @Test
    public void syncNonStreaming_shouldContainResponseMetadata() {
        stubResponseWithHeaders();
        AllTypesResponse allTypesResponse = client.allTypes(SdkBuilder::build);
        verifyResponseMetadata(allTypesResponse);
    }

    @Test
    public void syncStreaming_shouldContainResponseMetadata() {
        stubResponseWithHeaders();
        ResponseBytes<StreamingOutputOperationResponse> streamingOutputOperationResponseResponseBytes =
            client.streamingOutputOperation(SdkBuilder::build, ResponseTransformer.toBytes());
        verifyResponseMetadata(streamingOutputOperationResponseResponseBytes.response());
    }

    @Test
    public void asyncNonStreaming_shouldContainResponseMetadata() {
        stubResponseWithHeaders();
        AllTypesResponse allTypesResponse = asyncClient.allTypes(SdkBuilder::build).join();
        verifyResponseMetadata(allTypesResponse);
    }

    @Test
    public void asyncStreaming_shouldContainResponseMetadata() {
        stubResponseWithHeaders();
        CompletableFuture<ResponseBytes<StreamingOutputOperationResponse>> response =
            asyncClient.streamingOutputOperation(SdkBuilder::build, AsyncResponseTransformer.toBytes());
        verifyResponseMetadata(response.join().response());
    }

    @Test
    public void headerNotAvailable_responseMetadataShouldBeUnknown() {
        stubResponseWithoutHeaders();
        AllTypesResponse allTypesResponse = client.allTypes(SdkBuilder::build);
        verifyUnknownResponseMetadata(allTypesResponse);
    }

    private void verifyResponseMetadata(ProtocolRestJsonResponse allTypesResponse) {
        assertThat(allTypesResponse.responseMetadata()).isNotNull();
        assertThat(allTypesResponse.responseMetadata().barId()).isEqualTo("bar");
        assertThat(allTypesResponse.responseMetadata().fooId()).isEqualTo("foo");
        assertThat(allTypesResponse.responseMetadata().requestId()).isEqualTo("foobar");
    }

    private void verifyUnknownResponseMetadata(ProtocolRestJsonResponse allTypesResponse) {
        assertThat(allTypesResponse.responseMetadata()).isNotNull();
        assertThat(allTypesResponse.responseMetadata().barId()).isEqualTo("UNKNOWN");
        assertThat(allTypesResponse.responseMetadata().fooId()).isEqualTo("UNKNOWN");
        assertThat(allTypesResponse.responseMetadata().requestId()).isEqualTo("UNKNOWN");
    }

    private void stubResponseWithHeaders() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withHeader("x-foo-id", "foo")
                                           .withHeader("x-bar-id", "bar")
                                           .withHeader("x-foobar-id", "foobar")
                                           .withBody("{}")));
    }

    private void stubResponseWithoutHeaders() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withBody("{}")));
    }
}
