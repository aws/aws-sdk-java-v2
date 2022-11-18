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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.MetadataResponse;

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
    protected void successAssertions(String path, Consumer<MetadataResponse> assertions) {
        MetadataResponse response = client.get(path);
        assertions.accept(response);
    }

    @Override
    @SuppressWarnings("unchecked") // safe because of assertion: assertThat(ex).isInstanceOf(exceptionType);
    protected <T extends Throwable> void failureAssertions(String path, Class<T> exceptionType, Consumer<T> assertions) {
        Throwable ex = catchThrowable(() -> client.get(path));
        assertThat(ex).isInstanceOf(exceptionType);
        assertions.accept((T) ex);
    }
}
