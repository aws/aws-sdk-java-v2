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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getAllServeEvents;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryAsyncClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.model.IdempotentOperationRequest;

import java.net.URI;
import java.util.List;

public class QueryRequestTransformTest extends ProtocolTestBase {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolQueryClient client;
    private ProtocolQueryAsyncClient asyncClient;

    @Before
    public void setupClient() {
        client = ProtocolQueryClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(
                                            AwsBasicCredentials.create("akid", "skid")))
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                    .build();

        asyncClient = ProtocolQueryAsyncClient.builder()
                                              .credentialsProvider(StaticCredentialsProvider.create(
                                                      AwsBasicCredentials.create("akid", "skid")))
                                              .region(Region.US_EAST_1)
                                              .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                              .build();
    }

    @Test
    public void syncRequest_isMovingParamsToBodyStage() {
        stubSimpleResponse();
        IdempotentOperationRequest request = IdempotentOperationRequest.builder().idempotencyToken("test").build();
        client.idempotentOperation(request);
        verifyResponseMetadata();
    }

    @Test
    public void asyncRequest_isMovingParamsToBodyStage() {
        stubSimpleResponse();
        IdempotentOperationRequest request = IdempotentOperationRequest.builder().idempotencyToken("test").build();
        asyncClient.idempotentOperation(request).join();
        verifyResponseMetadata();
    }

    private void verifyResponseMetadata() {
        verify(postRequestedFor(anyUrl())
                       .withHeader(Header.CONTENT_TYPE, equalTo("application/x-www-form-urlencoded; charset=UTF-8"))
                       .withHeader(Header.CONTENT_LENGTH, matching("\\d+"))
                       .withUrl("/")
                       .withRequestBody(containing("Action=IdempotentOperation"))
                       .withRequestBody(containing("Version="))
                       .withRequestBody(containing("IdempotencyToken=test")));
    }

    private void stubSimpleResponse() {
        stubFor(post(anyUrl()).willReturn(aResponse()
                                                  .withStatus(200)
                                                  .withBody("<IdempotentOperationResponse/>")));
    }

}
