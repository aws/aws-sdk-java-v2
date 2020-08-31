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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryFailedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestClient;
import software.amazon.awssdk.services.endpointdiscoverytest.model.EndpointDiscoveryTestException;

public class EndpointDiscoveryTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private EndpointDiscoveryTestClient client;

    private EndpointDiscoveryTestAsyncClient asyncClient;

    @Before
    public void setupClient() {
        client = EndpointDiscoveryTestClient.builder()
                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .endpointDiscoveryEnabled(true)
                                            .overrideConfiguration(c -> c.putAdvancedOption(
                                                SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE, false))
                                            .build();

        asyncClient = EndpointDiscoveryTestAsyncClient.builder()
                                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                      .region(Region.US_EAST_1)
                                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                      .endpointDiscoveryEnabled(true)
                                                      .overrideConfiguration(c -> c.putAdvancedOption(
                                                          SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE, false))
                                                      .build();
    }

    @Test
    public void syncRequiredOperation_EmptyEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubEmptyResponse();
        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {}))
            .isInstanceOf(EndpointDiscoveryFailedException.class);
    }

    @Test
    public void asyncRequiredOperation_EmptyEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubEmptyResponse();
        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void syncRequiredOperation_NonRetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubDescribeEndpointsResponse(404);
        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {}))
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(EndpointDiscoveryTestException.class);
    }

    @Test
    public void asyncRequiredOperation_NonRetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubDescribeEndpointsResponse(404);
        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(EndpointDiscoveryFailedException.class);
    }

    @Test
    public void syncRequiredOperation_RetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubDescribeEndpointsResponse(500);
        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {}))
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(EndpointDiscoveryTestException.class);
    }

    @Test
    public void asyncRequiredOperation_RetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubDescribeEndpointsResponse(500);
        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(EndpointDiscoveryTestException.class);
    }

    @Test
    public void syncRequiredOperation_InvalidEndpointEndpointDiscoveryResponse_CausesSdkException() {
        stubDescribeEndpointsResponse(200, "invalid", 15);
        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {}))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void asyncRequiredOperation_InvalidEndpointEndpointDiscoveryResponse_CausesSdkException() {
        stubDescribeEndpointsResponse(200, "invalid", 15);
        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(SdkClientException.class);
    }

    private void stubEmptyResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withBody("{}")));
    }

    private void stubDescribeEndpointsResponse(int status) {
        stubDescribeEndpointsResponse(status, "localhost", 60);
    }

    private void stubDescribeEndpointsResponse(int status, String address, long cachePeriodInMinutes) {
        stubFor(post(urlPathEqualTo("/DescribeEndpoints"))
                    .willReturn(aResponse().withStatus(status)
                                           .withBody("{" +
                                                     "  \"Endpoints\": [{" +
                                                     "    \"Address\": \"" + address + "\"," +
                                                     "    \"CachePeriodInMinutes\": " + cachePeriodInMinutes +
                                                     "  }]" +
                                                     "}")));
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertAsyncRequiredOperationCallThrowable() {
        try {
            asyncClient.testDiscoveryRequired(r -> {}).get();
            throw new AssertionError();
        } catch (InterruptedException e) {
            return assertThat(e);
        } catch (ExecutionException e) {
            return assertThat(e.getCause());
        }
    }
}
