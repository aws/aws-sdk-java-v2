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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

@SuppressWarnings("deprecation")
public abstract class TokenBucketRecoversTest<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {

    protected WireMockServer wireMock;

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
    public void retryPolicyTokenBucketRecovers() {
        RetryPolicy retryPolicy = RetryPolicy.forRetryMode(RetryMode.STANDARD)
                                             .toBuilder()
                                             .throttlingBackoffStrategy(BackoffStrategy.none())
                                             .backoffStrategy(BackoffStrategy.none())
                                             .build();
        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryPolicy(retryPolicy))
            .build();
        int exceptions = 0;
        for (int x = 0; x < 60; x++) {
            try {
                callAllTypes(client);
            } catch (Exception e) {
                exceptions += 1;
            }
        }
        verifyRequestCount(161);
    }

    @Test
    public void retryStrategyTokenBucketRecovers() {
        RetryStrategy retryStrategy = SdkDefaultRetryStrategy
            .forRetryMode(RetryMode.STANDARD)
            .toBuilder()
            .backoffStrategy(software.amazon.awssdk.retries.api.BackoffStrategy.retryImmediately())
            .build();
        ClientT client = clientBuilder()
            .overrideConfiguration(o -> o.retryStrategy(retryStrategy))
            .build();
        int exceptions = 0;
        for (int x = 0; x < 60; x++) {
            try {
                callAllTypes(client);
            } catch (Exception e) {
                exceptions += 1;
            }
        }
        verifyRequestCount(161);
    }

    @BeforeEach
    private void beforeEach() {
        wireMock = new WireMockServer(wireMockConfig()
                                          .extensions(ErrorSimulationResponseTransformer.class));
        wireMock.start();
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse()
                                             .withTransformers("error-simulation-transformer")));
    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
    }

    private void verifyRequestCount(int count) {
        wireMock.verify(count, anyRequestedFor(anyUrl()));
    }

    static class SyncCanOverrideStrategy extends TokenBucketRecoversTest<ProtocolRestJsonClient,
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

    static class AsyncCanOverrideStrategy extends TokenBucketRecoversTest<ProtocolRestJsonAsyncClient,
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

    public static class ErrorSimulationResponseTransformer extends ResponseTransformer {
        private AtomicInteger requestCount = new AtomicInteger(0);

        @Override
        public String getName() {
            return "error-simulation-transformer";
        }

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            if (shouldSucceed()) {
                return Response.Builder.like(response)
                                       .but().body("{}")
                                       .status(200)
                                       .build();
            }
            return Response.Builder.like(response)
                                   .but().body("{}")
                                   .status(429)
                                   .build();
        }

        private boolean shouldSucceed() {
            int currentCount = requestCount.getAndIncrement();
            if (currentCount < 150 || currentCount >= 155) {
                // in between 5 successful calls will allow an additional retry as 5 tokens will be deposited to the store.
                return false;
            }
            return true;
        }
    }
}
