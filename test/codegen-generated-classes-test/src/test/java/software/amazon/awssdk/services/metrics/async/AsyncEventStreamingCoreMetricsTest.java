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

package software.amazon.awssdk.services.metrics.async;

import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.EventStreamOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.EventStreamOperationResponseHandler;
import software.amazon.awssdk.services.testutil.MockIdentityProviderUtil;

/**
 * Core metrics test for async streaming API
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncEventStreamingCoreMetricsTest extends BaseAsyncCoreMetricsTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);
    @Mock
    private MetricPublisher mockPublisher;


    private ProtocolRestJsonAsyncClient client;

    @Before
    public void setup() {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_2)
                                            .credentialsProvider(MockIdentityProviderUtil.mockIdentityProvider())
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .overrideConfiguration(c -> c.addMetricPublisher(mockPublisher)
                                                                         .retryStrategy(b -> b.maxAttempts(MAX_ATTEMPTS)))
                                            .build();
    }

    @After
    public void teardown() {
        wireMock.resetAll();
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Override
    String operationName() {
        return "EventStreamOperation";
    }

    @Override
    Supplier<CompletableFuture<?>> callable() {
        return () -> client.eventStreamOperation(EventStreamOperationRequest.builder().overrideConfiguration(b -> b.signer(new NoOpSigner())).build(),
                                                 new EmptyPublisher<>(),
                                                 EventStreamOperationResponseHandler.builder()
                                                                                    .subscriber(b -> {})
                                                                                    .build());
    }

    @Override
    MetricPublisher publisher() {
        return mockPublisher;
    }
}
