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
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static software.amazon.awssdk.imds.TestConstants.AMI_ID_RESOURCE;
import static software.amazon.awssdk.imds.TestConstants.EC2_METADATA_TOKEN_TTL_HEADER;
import static software.amazon.awssdk.imds.TestConstants.TOKEN_RESOURCE_PATH;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkAsyncHttpClientBuilder;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

@WireMockTest
class Ec2MetadataAsyncClientTest extends BaseEc2MetadataClientTest<Ec2MetadataAsyncClient, Ec2MetadataAsyncClient.Builder> {

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
    protected void successAssertions(String path, Consumer<Ec2MetadataResponse> assertions) {
        CompletableFuture<Ec2MetadataResponse> response = client.get(path);
        try {
            assertions.accept(response.join());
        } catch (Exception e) {
            fail("unexpected error while exeucting tests", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked") // safe because of assertion: assertThat(ex).getCause().isInstanceOf(exceptionType);
    protected <T extends Throwable> void failureAssertions(String path, Class<T> exceptionType, Consumer<T> assertions) {
        CompletableFuture<Ec2MetadataResponse> future = client.get(path);
        Throwable ex = catchThrowable(future::join);
        assertThat(future).isCompletedExceptionally();
        assertThat(ex).getCause().isInstanceOf(exceptionType);
        assertions.accept((T) ex.getCause());
    }

    @Test
    void get_cancelResponseFuture_shouldCancelHttpRequest() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(
            aResponse().withBody("some-token").withFixedDelay(1000)));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(
            aResponse().withBody("some-content").withFixedDelay(1000)));

        CompletableFuture<Ec2MetadataResponse> responseFuture = client.get(AMI_ID_RESOURCE);
        try {
            responseFuture.cancel(true);
            responseFuture.join();
        } catch (CancellationException e) {
            // ignore java.util.concurrent.CancellationException
        }
        verify(0, getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE)));
    }

    @Test
    void builder_httpClientWithDefaultBuilder_shouldBuildProperly() {
        Ec2MetadataAsyncClient buildClient = Ec2MetadataAsyncClient.builder()
                                                                   .httpClient(new DefaultSdkAsyncHttpClientBuilder())
                                                                   .endpoint(URI.create("http://localhost:" + port))
                                                                   .build();
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(
            aResponse().withBody("some-token").withHeader(EC2_METADATA_TOKEN_TTL_HEADER, "21600")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("some-value")));
        CompletableFuture<Ec2MetadataResponse> responseFuture = buildClient.get(AMI_ID_RESOURCE);
        Ec2MetadataResponse response = responseFuture.join();
        assertThat(response.asString()).isEqualTo("some-value");
    }

    @Test
    void builder_httpClientAndHttpBuilder_shouldThrowException() {
        assertThatThrownBy(() -> Ec2MetadataAsyncClient.builder()
                                                       .httpClient(new DefaultSdkAsyncHttpClientBuilder())
                                                       .httpClient(NettyNioAsyncHttpClient.create())
                                                       .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

}
