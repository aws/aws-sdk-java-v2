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
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static software.amazon.awssdk.imds.EndpointMode.IPV4;
import static software.amazon.awssdk.imds.EndpointMode.IPV6;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.imds.Ec2MetadataClientBuilder;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.imds.EndpointMode;
import software.amazon.awssdk.imds.MetadataResponse;

public abstract class BaseEc2MetadataClientTest<T, B extends Ec2MetadataClientBuilder<B, T>> {

    protected static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    protected static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    protected static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    protected static final String EC2_METADATA_ROOT = "/latest/meta-data";
    protected static final String AMI_ID_RESOURCE = EC2_METADATA_ROOT + "/ami-id";
    protected static final int DEFAULT_TOTAL_ATTEMPTS = 4;

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    protected abstract BaseEc2MetadataClient overrideClient(Consumer<B> builderConsumer);

    protected abstract void successAssertions(String path, Consumer<MetadataResponse> assertions);

    protected abstract <T extends Throwable> void failureAssertions(String path, Class<T> exceptionType,
                                                                    Consumer<T> assertions);

    @After
    public void reset() {
        mockMetadataEndpoint.resetAll();
    }

    @Test
    public void get_successOnFirstTry_shouldNotRetryAndSucceed() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withBody("{}")));
        successAssertions(AMI_ID_RESOURCE, response -> {
            assertThat(response.asString()).isEqualTo("{}");
            verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void get_failsEverytime_shouldRetryAndFails() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withStatus(500).withBody("Error 500")));
        failureAssertions(AMI_ID_RESOURCE, SdkClientException.class, ex -> {
            verify(exactly(DEFAULT_TOTAL_ATTEMPTS), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(DEFAULT_TOTAL_ATTEMPTS), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void get_returnsStatus4XX_shouldFailsAndNotRetry() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withStatus(400).withBody("error")));
        failureAssertions(AMI_ID_RESOURCE, SdkClientException.class, ex -> {
            verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void get_failsOnceThenSucceed_withCustomClient_shouldSucceed() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(500).withBody("Error 500"))
                    .willSetStateTo("Cause Success"));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Cause Success")
                    .willReturn(aResponse().withBody("{}")));

        overrideClient(builder -> builder
            .retryPolicy(Ec2MetadataRetryPolicy.builder()
                                               .numRetries(5)
                                               .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(300)))
                                               .build())
            .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
            .tokenTtl(Duration.ofSeconds(1024)));

        successAssertions(AMI_ID_RESOURCE, response -> {
            assertThat(response.asString()).isEqualTo("{}");
            verify(exactly(2), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("1024")));
            verify(exactly(2), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void getToken_failsEverytime_shouldRetryAndFailsAndNotCallService() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(500).withBody("Error 500")));
        failureAssertions(AMI_ID_RESOURCE, SdkClientException.class, ex -> {
            verify(exactly(DEFAULT_TOTAL_ATTEMPTS), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(0), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void getToken_returnsStatus4XX_shouldFailsAndNotRetry() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(400).withBody("ERROR 400")));
        failureAssertions(AMI_ID_RESOURCE, SdkClientException.class, ex -> {
            verify(exactly(1), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(0), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void getToken_failsOnceThenSucceed_withCustomClient_shouldSucceed() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs(STARTED)
                                                        .willReturn(aResponse().withStatus(500).withBody("Error 500"))
                                                        .willSetStateTo("Cause Success"));
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).inScenario("Retry Scenario")
                                                        .whenScenarioStateIs("Cause Success")
                                                        .willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).inScenario("Retry Scenario")
                                                    .whenScenarioStateIs("Cause Success")
                                                    .willReturn(aResponse().withBody("Success")));

        overrideClient(builder -> builder
            .retryPolicy(Ec2MetadataRetryPolicy.builder()
                                               .numRetries(5)
                                               .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofMillis(300)))
                                               .build())
            .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
            .build());

        successAssertions(AMI_ID_RESOURCE, response -> {
            assertThat(response.asString()).isEqualTo("Success");
            verify(exactly(2), putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(exactly(1), getRequestedFor(urlPathEqualTo(AMI_ID_RESOURCE))
                .withHeader(TOKEN_HEADER, equalTo("some-token")));
        });
    }

    @Test
    public void get_noRetries_shouldNotRetry() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(AMI_ID_RESOURCE)).willReturn(aResponse().withStatus(500).withBody("Error 500")));

        overrideClient(builder -> builder
            .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
            .retryPolicy(Ec2MetadataRetryPolicy.none()).build());

        failureAssertions(AMI_ID_RESOURCE, SdkClientException.class, ex -> {
            verify(1, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
            verify(1, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
        });
    }

    @Test
    public void builder_endpointAndEndpointModeSpecified_shouldThrowIllegalArgException() {
        assertThatThrownBy(() -> overrideClient(builder -> builder
            .endpoint(URI.create("http://localhost:" + mockMetadataEndpoint.port()))
            .endpointMode(IPV6)))
        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void builder_defaultValue_clientShouldUseIPV4Endpoint() {
        BaseEc2MetadataClient client = overrideClient(builder -> {});
        assertThat(client.endpoint).hasToString("http://169.254.169.254");
    }

    @Test
    public void builder_setEndpoint_shouldUseEndpoint() {
        String customEndpoint = "http://123.456.123.456";
        BaseEc2MetadataClient client = overrideClient(builder -> builder.endpoint(URI.create(customEndpoint)));
        assertThat(client.endpoint).hasToString(customEndpoint);
    }

    @ParameterizedTest
    @MethodSource("endpointArgumentSource")
    public void builder_setEndPointMode_shouldUseEndpointModeValue(EndpointMode endpointMode, String value) {
        BaseEc2MetadataClient client = overrideClient(builder -> builder.endpointMode(endpointMode));
        assertThat(client.endpoint).hasToString(value);
    }

    private static Stream<Arguments> endpointArgumentSource() {
        return Stream.of(
            arguments(IPV4, "http://169.254.169.254"),
            arguments(IPV6, "http://[fd00:ec2::254]"));
    }
}
