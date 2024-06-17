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

package software.amazon.awssdk.services.retry;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;

public class RetryStrategySetupUsingRetryMode {

    private WireMockServer wireMock = new WireMockServer(0);

    @Test
    public void sdkRetryStrategyDoesNotRetryOnAWSRetryableErrors() {
        // Configuring the client to use an non-AWS aware retry strategy won't retry on AWS retryable conditions.
        ProtocolRestJsonClient client =
            client(b -> b.overrideConfiguration(o -> o.retryStrategy(SdkDefaultRetryStrategy.standardRetryStrategy())));
        assertThrows(ProtocolRestJsonException.class, () -> callAllTypes(client));
        // One request, i.e., no retries.
        verifyRequestCount(1);
    }

    @Test
    public void clientBuilder_settingRetryModeInOverrideConfigurationConsumer() {
        // Configuring the client using RetryMode should support AWS retryable conditions.
        ProtocolRestJsonClient client = client(b -> b.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)));
        assertThrows(ProtocolRestJsonException.class, () -> callAllTypes(client));
        // Three requests, i.e., there were retries.
        verifyRequestCount(3);
    }

    @Test
    public void request_settingRetryModeInOverrideConfigurationConsumer() {
        // Configuring the client using RetryMode should support AWS retryable conditions.
        assertThrows(ProtocolRestJsonException.class, () -> callAllTypesWithPlugin(o -> o.retryStrategy(RetryMode.STANDARD)));
        // Three requests, i.e., there were retries.
        verifyRequestCount(3);
    }

    @Test
    public void request_settingRetryModeInOverrideConfigurationConsumerRunTwice() {
        ProtocolRestJsonClient client = client(b -> {
        });

        SdkPlugin retryStrategyPlugin = config -> config.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD));

        // Configuring the client using RetryMode should support AWS retryable conditions.
        assertThrows(ProtocolRestJsonException.class, () -> callAllTypes(client, Collections.singletonList(retryStrategyPlugin)));
        // Three requests, i.e., there were retries.
        verifyRequestCount(3);

        SdkPlugin unrelatedPlugin = config -> config.overrideConfiguration(o -> o.apiCallTimeout(Duration.ofSeconds(10)));

        // Configuring the client using an unrelated plugin should not remember the previous settings.
        assertThrows(ProtocolRestJsonException.class, () -> callAllTypes(client, Collections.singletonList(unrelatedPlugin)));
        // Four retries, the LEGACY retry strategy is back in.
        verifyRequestCount(3 + 4);
    }

    @Test
    public void request_settingRetryStrategyOverrideConfigurationConsumer() {
        // Configuring the client using RetryStrategy should work as expected.
        assertThrows(ProtocolRestJsonException.class,
                     () -> callAllTypesWithPlugin(o -> o.retryStrategy(AwsRetryStrategy.standardRetryStrategy()
                                                                                       .toBuilder()
                                                                                       .maxAttempts(2)
                                                                                       .build())));
        // Two requests, the configured per request is being used.
        verifyRequestCount(2);
    }

    @Test
    public void request_configuringRetryStrategyOverrideConfigurationConsumer() {
        // Configuring the client using Consumer<RetryStrategy.Builder> should work as expected.
        assertThrows(ProtocolRestJsonException.class,
                     () -> callAllTypesWithPlugin(o -> o.retryStrategy(b -> b.maxAttempts(2))));
        // Two requests, the configured per request is being used.
        verifyRequestCount(2);
    }

    @Test
    public void request_configuringRetryStrategyOverrideConfigurationMode() {
        // Configuring the client using RetryMode should work as expected.
        assertThrows(ProtocolRestJsonException.class,
                     () -> callAllTypesWithPlugin(o -> o.retryStrategy(RetryMode.STANDARD)));
        // Three requests, the configured per request is being used.
        verifyRequestCount(3);
    }

    @Test
    public void clientBuilder_settingRetryModeInOverrideConfigurationAndUsingIt() {
        // It does not matter if the ClientOverrideConfiguration.Builder is created by the customer or inside the
        // overrideConfiguration method in the client, using RetryMode should support AWS retryable conditions.
        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();
        builder.retryStrategy(RetryMode.STANDARD);
        ProtocolRestJsonClient client = client(b -> b.overrideConfiguration(builder.build()));
        assertThrows(ProtocolRestJsonException.class, () -> callAllTypes(client));
        // Three requests, i.e., there were retries.
        verifyRequestCount(3);
    }


    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    AllTypesResponse callAllTypesWithPlugin(Consumer<ClientOverrideConfiguration.Builder> configure) {
        ProtocolRestJsonClient client = client(b -> {
        });

        SdkPlugin plugin = config -> config.overrideConfiguration(configure);
        return callAllTypes(client, Collections.singletonList(plugin));
    }

    AllTypesResponse callAllTypes(ProtocolRestJsonClient client) {
        return callAllTypes(client, Collections.emptyList());
    }

    AllTypesResponse callAllTypes(ProtocolRestJsonClient client, List<SdkPlugin> requestPlugins) {
        return client.allTypes(r -> r.overrideConfiguration(c -> {
            for (SdkPlugin plugin : requestPlugins) {
                c.addPlugin(plugin);
            }
        }));
    }

    @BeforeEach
    private void beforeEach() {
        wireMock.start();
        wireMock.stubFor(post(anyUrl())
                             .willReturn(
                                 aResponse()
                                     .withStatus(400)
                                     // This is an AWS retryable error typ but 4xx are not retryable status codes. A normal SDK
                                     // retry strategy will not retry this response whereas an AWS retry strategy will.
                                     .withHeader("x-amzn-ErrorType", "PriorRequestNotComplete")
                                     .withBody("\"{\"__type\":\"PriorRequestNotComplete\",\"message\":\"Blah "
                                               + "error\"}\"")));

    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
    }


    private ProtocolRestJsonClient client(Consumer<ProtocolRestJsonClientBuilder> configure) {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        ProtocolRestJsonClientBuilder builder = ProtocolRestJsonClient
            .builder()
            .credentialsProvider(credentialsProvider)
            .region(Region.US_EAST_1)
            .endpointOverride(endpointOverride);
        configure.accept(builder);
        return builder.build();
    }
}
