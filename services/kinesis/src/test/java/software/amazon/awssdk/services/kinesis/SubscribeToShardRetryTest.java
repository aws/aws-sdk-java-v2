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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

/**
 * Kinesis has no retry specialization for {@code SubscribeToShard}: it is retried like other operations, using the
 * attempt counts of the resolved retry mode.
 */
public class SubscribeToShardRetryTest {
    private static final String RETRY_MODE_PROPERTY = "aws.retryMode";
    private static final String MAX_ATTEMPTS_PROPERTY = "aws.maxAttempts";

    private final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();
    private final WireMockServer wireMock = new WireMockServer(0);

    @BeforeEach
    void setup() {
        // Keep the host's retry settings and profile files from changing the expected attempt counts
        environmentVariableHelper.remove("AWS_RETRY_MODE");
        environmentVariableHelper.remove("AWS_MAX_ATTEMPTS");
        environmentVariableHelper.set("AWS_CONFIG_FILE", "does-not-exist");
        environmentVariableHelper.set("AWS_SHARED_CREDENTIALS_FILE", "does-not-exist");
        System.clearProperty(RETRY_MODE_PROPERTY);
        System.clearProperty(MAX_ATTEMPTS_PROPERTY);

        wireMock.start();
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(500)));
    }

    @AfterEach
    void cleanup() {
        wireMock.stop();
        environmentVariableHelper.reset();
        System.clearProperty(RETRY_MODE_PROPERTY);
        System.clearProperty(MAX_ATTEMPTS_PROPERTY);
    }

    @Test
    void defaultRetryMode_retriesSubscribeToShard() {
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(clientBuilder().build()));
        // The default STANDARD retry mode makes three attempts
        verifyRequestCount(3);
    }

    @Test
    void legacyRetryMode_retriesSubscribeToShard() {
        System.setProperty(RETRY_MODE_PROPERTY, "legacy");
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(clientBuilder().build()));
        // The LEGACY retry mode makes four attempts
        verifyRequestCount(4);
    }

    @Test
    void retryStrategyWithOneAttempt_doesNotRetrySubscribeToShard() {
        KinesisAsyncClient client = clientBuilder().overrideConfiguration(o -> o.retryStrategy(r -> r.maxAttempts(1)))
                                                   .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        verifyRequestCount(1);
    }

    @Test
    void requestPluginWithOneAttempt_doesNotRetrySubscribeToShard() {
        SubscribeToShardRequest request =
            SubscribeToShardRequest.builder()
                                   .overrideConfiguration(o -> o.addPlugin(
                                       c -> c.overrideConfiguration(b -> b.retryStrategy(r -> r.maxAttempts(1)))))
                                   .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(clientBuilder().build(), request));
        verifyRequestCount(1);
    }

    @Test
    void explicitStandardRetryMode_retriesSubscribeToShard() {
        KinesisAsyncClient client = clientBuilder().overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD))
                                                   .build();
        assertThrows(ExecutionException.class, () -> callSubscribeToShard(client));
        verifyRequestCount(3);
    }

    private KinesisAsyncClientBuilder clientBuilder() {
        return KinesisAsyncClient.builder()
                                 .region(Region.US_EAST_1)
                                 .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                                 .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    private void callSubscribeToShard(KinesisAsyncClient client) throws Exception {
        callSubscribeToShard(client, SubscribeToShardRequest.builder().build());
    }

    private void callSubscribeToShard(KinesisAsyncClient client, SubscribeToShardRequest request) throws Exception {
        client.subscribeToShard(request,
                                SubscribeToShardResponseHandler.builder()
                                                               .subscriber(e -> { })
                                                               .build())
              .get(10, TimeUnit.SECONDS);
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }
}
