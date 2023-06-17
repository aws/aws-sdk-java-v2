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
import java.io.IOException;
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
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;

/**
 * Core metrics test for async streaming API
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncStreamingCoreMetricsTest extends BaseAsyncCoreMetricsTest {

    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    @Mock
    private MetricPublisher mockPublisher;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private ProtocolRestJsonAsyncClient client;

    @Before
    public void setup() throws IOException {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_2)
                                            .credentialsProvider(mockCredentialsProvider)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .overrideConfiguration(c -> c.addMetricPublisher(mockPublisher)
                                                                         .retryStrategy(b -> b.maxAttempts(MAX_ATTEMPTS)))
                                            .build();

        when(mockCredentialsProvider.resolveCredentials()).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return AwsBasicCredentials.create("foo", "bar");
        });
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
        return "StreamingInputOperation";
    }

    @Override
    Supplier<CompletableFuture<?>> callable() {
        return () -> client.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                                                    AsyncRequestBody.fromBytes("helloworld".getBytes()));
    }

    @Override
    MetricPublisher publisher() {
        return mockPublisher;
    }
}
