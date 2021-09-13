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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Verifies that request-level overrides work with endpoint discovery.
 */
public class EndpointDiscoveryRequestOverrideConfigTest {
    private EndpointDiscoveryTestClient client;
    private EndpointDiscoveryTestAsyncClient asyncClient;
    private MockSyncHttpClient httpClient;
    private MockAsyncHttpClient asyncHttpClient;

    private static final AwsBasicCredentials CLIENT_CREDENTIALS = AwsBasicCredentials.create("ca", "cs");
    private static final AwsBasicCredentials REQUEST_CREDENTIALS = AwsBasicCredentials.create("ra", "rs");

    @Before
    public void setupClient() {
        httpClient = new MockSyncHttpClient();
        asyncHttpClient = new MockAsyncHttpClient();

        client = EndpointDiscoveryTestClient.builder()
                                            .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                            .region(Region.US_EAST_1)
                                            .endpointDiscoveryEnabled(true)
                                            .httpClient(httpClient)
                                            .build();

        asyncClient = EndpointDiscoveryTestAsyncClient.builder()
                                                      .credentialsProvider(StaticCredentialsProvider.create(CLIENT_CREDENTIALS))
                                                      .region(Region.US_EAST_1)
                                                      .endpointDiscoveryEnabled(true)
                                                      .httpClient(asyncHttpClient)
                                                      .build();
    }

    @After
    public void cleanup() {
        httpClient.reset();
        asyncHttpClient.reset();
    }

    @Test
    public void syncClientCredentialsUsedByDefault() {
        httpClient.stubResponses(successfulEndpointDiscoveryResponse(), successfulResponse());
        client.testDiscoveryRequired(r -> {});
        assertRequestsUsedCredentials(httpClient.getRequests(), CLIENT_CREDENTIALS);
    }

    @Test
    public void asyncClientCredentialsUsedByDefault() throws Exception {
        asyncHttpClient.stubResponses(successfulEndpointDiscoveryResponse(), successfulResponse());
        asyncClient.testDiscoveryRequired(r -> {}).get(10, TimeUnit.SECONDS);
        assertRequestsUsedCredentials(asyncHttpClient.getRequests(), CLIENT_CREDENTIALS);
    }

    @Test
    public void syncClientRequestCredentialsUsedIfOverridden() {
        httpClient.stubResponses(successfulEndpointDiscoveryResponse(), successfulResponse());
        client.testDiscoveryRequired(r -> r.overrideConfiguration(c -> c.credentialsProvider(() -> REQUEST_CREDENTIALS)));
        assertRequestsUsedCredentials(httpClient.getRequests(), REQUEST_CREDENTIALS);
    }

    @Test
    public void asyncClientRequestCredentialsUsedIfOverridden() throws Exception {
        asyncHttpClient.stubResponses(successfulEndpointDiscoveryResponse(), successfulResponse());
        asyncClient.testDiscoveryRequired(r -> r.overrideConfiguration(c -> c.credentialsProvider(() -> REQUEST_CREDENTIALS)))
                   .get(10, TimeUnit.SECONDS);
        assertRequestsUsedCredentials(asyncHttpClient.getRequests(), REQUEST_CREDENTIALS);
    }

    @Test
    public void syncClientRequestHeadersUsed() {
        httpClient.stubResponses(successfulEndpointDiscoveryResponse(), successfulResponse());
        client.testDiscoveryRequired(r -> r.overrideConfiguration(c -> c.putHeader("Foo", "Bar")));
        assertRequestsHadHeader(httpClient.getRequests(), "Foo", "Bar");
    }

    @Test
    public void asyncClientRequestHeadersUsed() throws Exception {
        asyncHttpClient.stubResponses(successfulEndpointDiscoveryResponse(), successfulResponse());
        asyncClient.testDiscoveryRequired(r -> r.overrideConfiguration(c -> c.putHeader("Foo", "Bar")))
                   .get(10, TimeUnit.SECONDS);
        assertRequestsHadHeader(asyncHttpClient.getRequests(), "Foo", "Bar");
    }

    private void assertRequestsHadHeader(List<SdkHttpRequest> requests, String headerName, String headerValue) {
        assertThat(requests).hasSize(2);
        assertThat(requests).allSatisfy(request -> {
            assertThat(request.firstMatchingHeader(headerName)).hasValue(headerValue);
        });
    }

    private void assertRequestsUsedCredentials(List<SdkHttpRequest> requests, AwsBasicCredentials clientCredentials) {
        assertThat(requests).hasSize(2);
        assertRequestUsedCredentials(requests.get(0), clientCredentials);
        assertRequestUsedCredentials(requests.get(1), clientCredentials);
    }

    private void assertRequestUsedCredentials(SdkHttpRequest request, AwsCredentials expectedCredentials) {
        assertThat(request.firstMatchingHeader("Authorization")).isPresent().hasValueSatisfying(authorizationHeader -> {
            assertThat(authorizationHeader).contains(" Credential=" + expectedCredentials.accessKeyId() + "/");
        });
    }

    private HttpExecuteResponse successfulEndpointDiscoveryResponse() {
        String responseData =
            "{" +
            "  \"Endpoints\": [{" +
            "    \"Address\": \"something\"," +
            "    \"CachePeriodInMinutes\": 30" +
            "  }]" +
            "}";

        AbortableInputStream responseStream = AbortableInputStream.create(new StringInputStream(responseData), () -> {});
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .appendHeader("Content-Length",
                                                                         Integer.toString(responseData.length()))
                                                           .build())
                                  .responseBody(responseStream)
                                  .build();
    }

    private HttpExecuteResponse successfulResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .build();
    }
}
