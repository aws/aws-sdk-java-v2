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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

public abstract class OverridesAreForgotten<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {

    protected WireMockServer wireMock = new WireMockServer(0);

    protected abstract BuilderT newClientBuilder();

    protected abstract AllTypesResponse callAllTypes(ClientT client, SdkPlugin... plugins);

    private BuilderT clientBuilder() {
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        return newClientBuilder()
            .credentialsProvider(credentialsProvider)
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    @Test
    public void pluginOverridesAreForgottenForSubsequentPlugins() {
        stubThrottlingResponse();
        ClientT client = clientBuilder()
            .addPlugin(c -> c.overrideConfiguration(o -> o.retryStrategy(RetryMode.STANDARD)))
            .build();

        // 50 times 2 retries 5 tokens withdrawn for each for a grand total of 500.
        // Tokens are exhausted, no more retries will be allowed by this strategy.
        int expected = 0;
        for (int x = 1; x <= 50; x++) {
            assertThatThrownBy(() -> callAllTypes(client))
                .isInstanceOf(SdkException.class);
            expected += 3;
            verifyRequestCount(expected);
        }

        // Override for the request, new strategy should allow the
        // default 3 attempts (first attempt plus two retries).
        assertThatThrownBy(() -> callAllTypes(client,
                                              config -> config.overrideConfiguration(
                                                  oc -> oc.retryStrategy(RetryMode.STANDARD))))
            .isInstanceOf(SdkException.class);
        expected += 3;
        verifyRequestCount(expected);

        // Call again using a no-op plugin.
        assertThatThrownBy(() -> callAllTypes(client, config -> {
        }))
            .isInstanceOf(SdkException.class);
        // only one attempt, the original strategy is back in place and has exhausted its tokens.
        expected += 1;
        verifyRequestCount(expected);
    }

    @BeforeEach
    private void beforeEach() {
        wireMock.start();
    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
    }

    private void stubThrottlingResponse() {
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse().withStatus(429)));
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    static class SyncOverridesAreForgotten extends OverridesAreForgotten<ProtocolRestJsonClient,
        ProtocolRestJsonClientBuilder> {
        @Override
        protected ProtocolRestJsonClientBuilder newClientBuilder() {
            return ProtocolRestJsonClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonClient client, SdkPlugin... plugins) {
            AllTypesRequest.Builder requestBuilder = AllTypesRequest.builder();
            for (SdkPlugin plugin : plugins) {
                requestBuilder.overrideConfiguration(o -> o.addPlugin(plugin));
            }
            return client.allTypes(requestBuilder.build());
        }
    }

    static class AsyncOverridesAreForgotten extends OverridesAreForgotten<ProtocolRestJsonAsyncClient,
        ProtocolRestJsonAsyncClientBuilder> {
        @Override
        protected ProtocolRestJsonAsyncClientBuilder newClientBuilder() {
            return ProtocolRestJsonAsyncClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonAsyncClient client, SdkPlugin... plugins) {
            try {
                AllTypesRequest.Builder requestBuilder = AllTypesRequest.builder();
                for (SdkPlugin plugin : plugins) {
                    requestBuilder.overrideConfiguration(o -> o.addPlugin(plugin));
                }
                return client.allTypes(requestBuilder.build()).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw e;
            }
        }
    }
}
