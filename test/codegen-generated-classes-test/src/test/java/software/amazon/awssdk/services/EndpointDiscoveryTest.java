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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.endpointdiscovery.EndpointDiscoveryFailedException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestAsyncClient;
import software.amazon.awssdk.services.endpointdiscoverytest.EndpointDiscoveryTestClient;
import software.amazon.awssdk.services.endpointdiscoverytest.model.EndpointDiscoveryTestException;

public class EndpointDiscoveryTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private SdkHttpClient mockSyncClient;

    private SdkAsyncHttpClient mockAsyncClient;

    private EndpointDiscoveryTestClient client;

    private EndpointDiscoveryTestAsyncClient asyncClient;

    @Before
    public void setupClient() {
        mockSyncClient = mock(SdkHttpClient.class);

        client = EndpointDiscoveryTestClient.builder()
                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                "akid", "skid")))
                                            .region(Region.US_EAST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .endpointDiscoveryEnabled(true)
                                            .overrideConfiguration(c -> c.putAdvancedOption(
                                                SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE, false))
                                            .httpClient(mockSyncClient)
                                            .build();

        mockAsyncClient = mock(SdkAsyncHttpClient.class);
        asyncClient = EndpointDiscoveryTestAsyncClient.builder()
                                                      .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                                      .region(Region.US_EAST_1)
                                                      .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                      .endpointDiscoveryEnabled(true)
                                                      .overrideConfiguration(c -> c.putAdvancedOption(
                                                          SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE,
                                                          false))
                                                      .httpClient(mockAsyncClient)
                                                      .build();
    }

    @Test
    public void syncRequiredOperation_EmptyEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubResponse(mockSyncClient, 200, "{}");
        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {
        }))
            .isInstanceOf(EndpointDiscoveryFailedException.class);
    }

    @Test
    public void asyncRequiredOperation_EmptyEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubResponse(mockAsyncClient, 200, "{}");
        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void syncRequiredOperation_NonRetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubResponse(mockSyncClient, 404, "localhost", 60);

        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {
        }))
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(EndpointDiscoveryTestException.class);
    }

    @Test
    public void asyncRequiredOperation_NonRetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubResponse(mockAsyncClient, 404, "localhost", 60);

        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(EndpointDiscoveryFailedException.class);
    }

    @Test
    public void syncRequiredOperation_RetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubResponse(mockSyncClient, 500, "localhost", 60);

        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {
        }))
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(EndpointDiscoveryTestException.class);
    }

    @Test
    public void asyncRequiredOperation_RetryableEndpointDiscoveryResponse_CausesEndpointDiscoveryFailedException() {
        stubResponse(mockAsyncClient, 500, "localhost", 60);

        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(EndpointDiscoveryFailedException.class)
            .hasCauseInstanceOf(EndpointDiscoveryTestException.class);
    }

    @Test
    public void syncRequiredOperation_InvalidEndpointEndpointDiscoveryResponse_CausesSdkException() {
        stubResponse(mockSyncClient, 500, "invalid", 15);

        assertThatThrownBy(() -> client.testDiscoveryRequired(r -> {
        }))
            .isInstanceOf(SdkClientException.class);
    }

    @Test
    public void asyncRequiredOperation_InvalidEndpointEndpointDiscoveryResponse_CausesSdkException() {
        stubResponse(mockAsyncClient, 500, "invalid", 15);

        assertAsyncRequiredOperationCallThrowable()
            .isInstanceOf(SdkClientException.class);
    }

    private AbstractThrowableAssert<?, ? extends Throwable> assertAsyncRequiredOperationCallThrowable() {
        try {
            asyncClient.testDiscoveryRequired(r -> {
            }).get();
            throw new AssertionError();
        } catch (InterruptedException e) {
            return assertThat(e);
        } catch (ExecutionException e) {
            return assertThat(e.getCause());
        }
    }

    private static class TestExecutableHttpRequest implements ExecutableHttpRequest {
        private final HttpExecuteResponse response;

        TestExecutableHttpRequest(HttpExecuteResponse response) {
            this.response = response;
        }

        @Override
        public void abort() {
        }

        @Override
        public HttpExecuteResponse call() throws IOException {
            return response;
        }
    }

    private void stubResponse(SdkAsyncHttpClient mockClient, int statusCode, String address, long cachePeriod) {
        String responseBody = "{" +
                              "  \"Endpoints\": [{" +
                              "    \"Address\": \"" + address + "\"," +
                              "    \"CachePeriodInMinutes\": " + cachePeriod +
                              "  }]" +
                              "}";

        stubResponse(mockClient, statusCode, responseBody);
    }

    private void stubResponse(SdkAsyncHttpClient mockClient, int statusCode, String responseBody) {
        when(mockClient.execute(any())).thenAnswer(
            stubAsyncResponse(SdkHttpResponse.builder().statusCode(statusCode).build(),
                              ByteBuffer.wrap(responseBody.getBytes(StandardCharsets.UTF_8))));
    }

    private void stubResponse(SdkHttpClient mockClient, int statusCode, String address, long cachePeriod) {
        String responseBody = "{" +
                              "  \"Endpoints\": [{" +
                              "    \"Address\": \"" + address + "\"," +
                              "    \"CachePeriodInMinutes\": " + cachePeriod +
                              "  }]" +
                              "}";

        stubResponse(mockClient, statusCode, responseBody);
    }

    private void stubResponse(SdkHttpClient mockClient, int statusCode, String responseBody) {
        when(mockClient.prepareRequest(any())).thenReturn(new TestExecutableHttpRequest(
            HttpExecuteResponse.builder()
                               .response(SdkHttpResponse.builder()
                                                        .statusCode(statusCode)
                                                        .build())
                               .responseBody(AbortableInputStream.create(
                                   new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8))))
                               .build()
        ));
    }

    private Answer<CompletableFuture<Void>> stubAsyncResponse(SdkHttpResponse response, ByteBuffer content) {
        return (i) -> {
            AsyncExecuteRequest request = i.getArgument(0, AsyncExecuteRequest.class);
            request.responseHandler().onHeaders(response);
            request.responseHandler().onStream(Flowable.just(content));

            CompletableFuture<Void> cf = new CompletableFuture<>();
            cf.complete(null);

            return cf;
        };
    }
}
