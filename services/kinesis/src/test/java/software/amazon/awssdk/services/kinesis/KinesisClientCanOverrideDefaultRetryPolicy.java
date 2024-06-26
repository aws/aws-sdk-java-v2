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

package software.amazon.awssdk.services.kinesis;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;

public class KinesisClientCanOverrideDefaultRetryPolicy {
    private WireMockServer wireMock = new WireMockServer(0);

    @Test
    public void kinesisRetryPolicyIsUsedWhenNotOverridden() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .credentialsProvider(credentialsProvider)
                                                      .endpointOverride(endpointOverride)
                                                      .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        // Standard retry strategy does not retry on subscribeToShard
        verifyRequestCount(1);
    }

    @Test
    public void retryStrategyClientBuilderOverrideIsUsed() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .credentialsProvider(credentialsProvider)
                                                      .overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                                                      .endpointOverride(endpointOverride)
                                                      .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        // Standard retry strategy does retry on subscribeToShard
        verifyRequestCount(3);
    }

    @Test
    public void retryStrategyPluginOverrideIsUsed() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .credentialsProvider(credentialsProvider)
                                                      .endpointOverride(endpointOverride)
                                                      .addPlugin(c -> c.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)))
                                                      .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        // Standard retry strategy does retry on subscribeToShard
        verifyRequestCount(3);
    }

    @Test
    public void retryStrategyRequestPluginOverrideIsUsed() {
        URI endpointOverride = URI.create("http://localhost:" + wireMock.port());
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

        KinesisAsyncClient client = KinesisAsyncClient.builder()
                                                      .credentialsProvider(credentialsProvider)
                                                      .endpointOverride(endpointOverride)
                                                      .build();
        SdkPlugin plugin = c -> c.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD));
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client, plugin));
        // Standard retry strategy does retry on subscribeToShard
        verifyRequestCount(3);
    }

    private void callSubscribeToShard(KinesisAsyncClient client, SdkPlugin... plugins) throws Exception {
        List<SubscribeToShardEventStream> events = new ArrayList<>();
        SubscribeToShardRequest.Builder builder = SubscribeToShardRequest.builder();
        for (SdkPlugin plugin : plugins) {
            builder.overrideConfiguration(o -> o.addPlugin(plugin));
        }
        client.subscribeToShard(builder.build(),
                                SubscribeToShardResponseHandler.builder()
                                                               .subscriber(events::add)
                                                               .build())
              .get(10, TimeUnit.SECONDS);
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    @BeforeEach
    private void beforeEach() {
        wireMock.start();
        wireMock.stubFor(post(anyUrl())
                             .willReturn(
                                 aResponse()
                                     .withStatus(500)));

    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
    }

}
