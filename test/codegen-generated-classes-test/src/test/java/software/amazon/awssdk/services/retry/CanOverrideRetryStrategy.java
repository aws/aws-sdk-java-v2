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
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;

public abstract class CanOverrideRetryStrategy<ClientT, BuilderT extends AwsClientBuilder<BuilderT, ClientT>> {

    protected WireMockServer wireMock = new WireMockServer(0);

    protected abstract BuilderT newClientBuilder();

    protected abstract AllTypesResponse callAllTypes(ClientT client);

    private BuilderT clientBuilder(RetryStrategy retryStrategy) {
        StaticCredentialsProvider credentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
        return newClientBuilder()
            .credentialsProvider(credentialsProvider)
            .overrideConfiguration(o -> o.retryStrategy(retryStrategy))
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    @Test
    public void overrideConfiguration_RetryStrategyIsOverridden_itGetsUsed() {
        WrappingRetryStrategy wrappingRetryStrategy = wrappingRetryStrategy();
        ClientT client = clientBuilder(wrappingRetryStrategy).build();
        assertThrows(Exception.class, () -> callAllTypes(client));
        assertEquals(3, wrappingRetryStrategy.failures.size());
    }

    @BeforeEach
    private void beforeEach() {
        wireMock.start();
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse().withStatus(429)));
    }

    @AfterEach
    private void afterEach() {
        wireMock.stop();
    }


    public WrappingRetryStrategy wrappingRetryStrategy() {
        RetryStrategy wrapped = AwsRetryStrategy.standardRetryStrategy();
        return new WrappingRetryStrategy(wrapped);
    }

    static class WrappingRetryStrategy implements RetryStrategy {
        private final List<Throwable> failures = new ArrayList<>();
        private final RetryStrategy wrapped;

        WrappingRetryStrategy(RetryStrategy wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            return wrapped.acquireInitialToken(request);
        }

        @Override
        public RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request) {
            failures.add(request.failure());
            return wrapped.refreshRetryToken(request);
        }

        @Override
        public RecordSuccessResponse recordSuccess(RecordSuccessRequest request) {
            return wrapped.recordSuccess(request);
        }

        @Override
        public int maxAttempts() {
            return wrapped.maxAttempts();
        }

        @Override
        public Builder<?, ?> toBuilder() {
            return wrapped.toBuilder();
        }
    }

    static class SyncCanOverrideStrategy extends CanOverrideRetryStrategy<ProtocolRestJsonClient,
        ProtocolRestJsonClientBuilder> {
        @Override
        protected ProtocolRestJsonClientBuilder newClientBuilder() {
            return ProtocolRestJsonClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonClient client) {
            return client.allTypes();
        }
    }

    static class AsyncCanOverrideStrategy extends CanOverrideRetryStrategy<ProtocolRestJsonAsyncClient,
        ProtocolRestJsonAsyncClientBuilder> {
        @Override
        protected ProtocolRestJsonAsyncClientBuilder newClientBuilder() {
            return ProtocolRestJsonAsyncClient.builder();
        }

        @Override
        protected AllTypesResponse callAllTypes(ProtocolRestJsonAsyncClient client) {
            try {
                return client.allTypes().join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }

                throw e;
            }
        }
    }
}
