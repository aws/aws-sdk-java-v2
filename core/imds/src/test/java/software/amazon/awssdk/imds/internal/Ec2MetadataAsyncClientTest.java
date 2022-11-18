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
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.MetadataResponse;

@WireMockTest
class Ec2MetadataAsyncClientTest extends BaseEc2MetadataClientTest<Ec2MetadataAsyncClient,
    Ec2MetadataAsyncClient.Builder> {

    private Ec2MetadataAsyncClient client;

    private int port;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) {
        this.port = wiremock.getHttpPort();
        this.client = Ec2MetadataAsyncClient.builder()
                                            .endpoint(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                            .build();
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected BaseEc2MetadataClient overrideClient(Consumer<Ec2MetadataAsyncClient.Builder> builderConsumer) {
        Ec2MetadataAsyncClient.Builder builder = Ec2MetadataAsyncClient.builder();
        builderConsumer.accept(builder);
        this.client = builder.build();
        return (BaseEc2MetadataClient) this.client;
    }

    @Override
    protected void successAssertions(String path, Consumer<MetadataResponse> assertions) {
        CompletableFuture<MetadataResponse> response = client.get(path);
        try {
            assertions.accept(response.join());
        } catch (Exception e) {
            fail("unexpected error while exeucting tests", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked") // safe because of assertion: assertThat(ex).getCause().isInstanceOf(exceptionType);
    protected <T extends Throwable> void failureAssertions(String path, Class<T> exceptionType, Consumer<T> assertions) {
        CompletableFuture<MetadataResponse> future = client.get(path);
        Throwable ex = catchThrowable(future::join);
        assertThat(future).isCompletedExceptionally();
        assertThat(ex).getCause().isInstanceOf(exceptionType);
        assertions.accept((T) ex.getCause());
    }

    @Test
    void get_multipleAsyncRequest_shouldCompleteSuccessfully() {
        int totalRequests = 128;
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                    .willReturn(aResponse().withFixedDelay(200).withBody("some-token")));
        for (int i = 0; i < totalRequests; i++) {
            ResponseDefinitionBuilder responseStub = aResponse()
                .withFixedDelay(300).withStatus(200).withBody("response::" + i);
            stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE + "/" + i)).willReturn(responseStub));
        }
        overrideClient(b -> b.httpClient(
                                 NettyNioAsyncHttpClient.builder()
                                                        .connectionTimeout(Duration.ofSeconds(10))
                                                        .readTimeout(Duration.ofSeconds(10))
                                                        .build())
                             .endpoint(URI.create("http://localhost:" + port)));
        List<CompletableFuture<MetadataResponse>> requests = Stream.iterate(0, x -> x + 1)
                                                                   .map(i -> client.get(AMI_ID_RESOURCE + "/" + i))
                                                                   .limit(totalRequests)
                                                                   .collect(Collectors.toList());
        CompletableFuture<List<MetadataResponse>> responses = CompletableFuture
            .allOf(requests.toArray(new CompletableFuture[0]))
            .thenApply(unusedVoid -> requests.stream()
                                             .map(CompletableFuture::join)
                                             .collect(Collectors.toList()));

        List<MetadataResponse> resolvedResponses = responses.join();
        for (int i = 0; i < totalRequests; i++) {
            MetadataResponse response = resolvedResponses.get(i);
            assertThat(response.asString()).isEqualTo("response::" + i);
        }
        verify(exactly(totalRequests),
               putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                   .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        verify(exactly(totalRequests), getRequestedFor(urlPathMatching(AMI_ID_RESOURCE + "/" + "\\d+"))
            .withHeader(TOKEN_HEADER, equalTo("some-token")));
    }

    @Test
    void get_largeResponse_shouldSucceed() throws Exception {
        int size = 10 * 1024 * 1024; // 10MB
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (i % 128);
        }
        String ec2MetadataContent = new String(bytes, StandardCharsets.US_ASCII);
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody(ec2MetadataContent)));

        try (Ec2MetadataAsyncClient client =
                 Ec2MetadataAsyncClient.builder().endpoint(URI.create("http://localhost:" + port)).build()) {
            CompletableFuture<MetadataResponse> res = client.get(AMI_ID_RESOURCE);
            MetadataResponse response = res.get();
            assertThat(response.asString()).hasSize(size);
            assertThat(response.asString()).isEqualTo(ec2MetadataContent);
            verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        }
    }

    @Test
    void get_cancelResponseFuture_shouldPropagate() {
        SdkAsyncHttpClient mockClient = Mockito.mock(SdkAsyncHttpClient.class);
        CompletableFuture<Void> responseFuture = new CompletableFuture<>();
        when(mockClient.execute(any(AsyncExecuteRequest.class))).thenReturn(responseFuture);
        overrideClient(builder -> builder.httpClient(mockClient));

        Ec2MetadataAsyncClient mockedClient = Ec2MetadataAsyncClient.builder().httpClient(mockClient).build();
        CompletableFuture<MetadataResponse> future = mockedClient.get(AMI_ID_RESOURCE);
        future.cancel(true);

        assertThat(responseFuture).isCancelled();
    }
}
