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

package software.amazon.awssdk.services.awsquerycompatible;

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
import software.amazon.awssdk.services.querycompatiblejson.QueryCompatibleJsonAsyncClient;
import software.amazon.awssdk.services.querycompatiblejson.QueryCompatibleJsonClient;
import software.amazon.awssdk.services.querycompatiblejson.model.AllTypeResponse;
import software.amazon.awssdk.services.querycompatiblejson.model.QueryCompatibleJsonResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class AwsQueryCompatibleErrorTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private QueryCompatibleJsonClient client;

    private QueryCompatibleJsonAsyncClient asyncClient;

    private static final String queryHeaderValue = "CustomException;Sender";

    @Before
    public void setupClient() {
        client = QueryCompatibleJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                       .region(Region.US_EAST_1)
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .build();

        asyncClient = QueryCompatibleJsonAsyncClient.builder()
                                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                 .region(Region.US_EAST_1)
                                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                 .build();
    }

    @Test
    public void stubWithHeader_shouldContainResponseMetadata() {
        stubResponseWithHeaders(queryHeaderValue);
        AllTypeResponse allTypeResponse = client.allType(SdkBuilder::build);
        AllTypeResponse allTypeAsyncResponse = asyncClient.allType(SdkBuilder::build).join();
        verifyResponseMetadata(allTypeResponse, queryHeaderValue);
        verifyResponseMetadata(allTypeAsyncResponse, queryHeaderValue);
    }

    @Test
    public void stubWithNoHeader_responseMetadataShouldBeUnknown() {
        stubResponseWithoutHeaders();
        AllTypeResponse allTypesResponse = client.allType(SdkBuilder::build);
        verifyUnknownResponseMetadata(allTypesResponse);
    }

    @Test
    public void asyncSubWithHeader_shouldContainResponseMetadata() {
        stubResponseWithHeaders(queryHeaderValue);
        AllTypeResponse allTypeAsyncResponse = asyncClient.allType(SdkBuilder::build).join();
        verifyResponseMetadata(allTypeAsyncResponse, queryHeaderValue);
    }

    @Test
    public void asyncStubWithNoHeader_responseMetadataShouldBeUnknown() {
        stubResponseWithoutHeaders();
        AllTypeResponse allTypeAsyncResponse = asyncClient.allType(SdkBuilder::build).join();
        verifyUnknownResponseMetadata(allTypeAsyncResponse);
    }

    private void verifyResponseMetadata(QueryCompatibleJsonResponse allTypesResponse, String queryHeaderValue) {
        assertThat(allTypesResponse.responseMetadata()).isNotNull();
        assertThat(allTypesResponse.responseMetadata().xAmznQueryError()).isEqualTo(queryHeaderValue);
    }

    private void verifyUnknownResponseMetadata(QueryCompatibleJsonResponse allTypesResponse) {
        assertThat(allTypesResponse.responseMetadata()).isNotNull();
        assertThat(allTypesResponse.responseMetadata().xAmznQueryError()).isEqualTo("UNKNOWN");
    }

    private void stubResponseWithHeaders(String headerValue) {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withHeader("x-amzn-query-error", headerValue)
                                           .withBody("{\\\"__type\\\": \\\"ServiceModeledException\\\", \\\"Message\\\": "
                                                     + "\\\"This is the \"\n"
                                                     + "                                                    + \"service "
                                                     + "message\\\"}")));
    }

    private void stubResponseWithoutHeaders() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withBody("{\\\"__type\\\": \\\"ServiceModeledException\\\", \\\"Message\\\": "
                                                     + "\\\"This is the \"\n"
                                                     + "                                                    + \"service "
                                                     + "message\\\"}")));
    }
}
