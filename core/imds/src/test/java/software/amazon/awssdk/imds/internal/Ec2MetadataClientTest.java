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
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static software.amazon.awssdk.imds.TestConstants.AMI_ID_RESOURCE;
import static software.amazon.awssdk.imds.TestConstants.EC2_METADATA_TOKEN_TTL_HEADER;
import static software.amazon.awssdk.imds.TestConstants.TOKEN_RESOURCE_PATH;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

@WireMockTest
class Ec2MetadataClientTest extends BaseEc2MetadataClientTest<Ec2MetadataClient, Ec2MetadataClient.Builder> {

    private Ec2MetadataClient client;

    private int port;

    @BeforeEach
    public void init(WireMockRuntimeInfo wiremock) {
        this.port = wiremock.getHttpPort();
        this.client = Ec2MetadataClient.builder()
                                            .endpoint(URI.create("http://localhost:" + wiremock.getHttpPort()))
                                            .build();
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected BaseEc2MetadataClient overrideClient(Consumer<Ec2MetadataClient.Builder> builderConsumer) {
        Ec2MetadataClient.Builder builder = Ec2MetadataClient.builder();
        builderConsumer.accept(builder);
        this.client = builder.build();
        return (BaseEc2MetadataClient) this.client;
    }

    @Override
    protected void successAssertions(String path, Consumer<Ec2MetadataResponse> assertions) {
        Ec2MetadataResponse response = client.get(path);
        assertions.accept(response);
    }

    @Override
    @SuppressWarnings("unchecked") // safe because of assertion: assertThat(ex).isInstanceOf(exceptionType);
    protected <T extends Throwable> void failureAssertions(String path, Class<T> exceptionType, Consumer<T> assertions) {
        Throwable ex = catchThrowable(() -> client.get(path));
        assertThat(ex).isInstanceOf(exceptionType);
        assertions.accept((T) ex);
    }

    @Test
    void builder_httpClientWithDefaultBuilder_shouldBuildProperly() {
        Ec2MetadataClient buildClient = Ec2MetadataClient.builder()
            .httpClient(new DefaultSdkHttpClientBuilder())
            .endpoint(URI.create("http://localhost:" + port))
            .build();
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(
            aResponse().withBody("some-token").withHeader(EC2_METADATA_TOKEN_TTL_HEADER, "21600")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));
        Ec2MetadataResponse response = buildClient.get(AMI_ID_RESOURCE);
        assertThat(response.asString()).isEqualTo("{}");
    }

    @Test
    void builder_httpClientAndHttpBuilder_shouldThrowException() {
        assertThatThrownBy(() -> Ec2MetadataClient.builder()
                                                       .httpClient(new DefaultSdkHttpClientBuilder())
                                                       .httpClient(UrlConnectionHttpClient.create())
                                                       .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

}
