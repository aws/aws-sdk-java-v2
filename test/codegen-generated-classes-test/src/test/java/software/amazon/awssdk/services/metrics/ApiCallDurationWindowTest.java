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

package software.amazon.awssdk.services.metrics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointParams;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointProvider;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.services.testutil.MockIdentityProviderUtil;

/**
 * Verifies which phases of an API call fall inside the {@link CoreMetric#API_CALL_DURATION} window, for both the
 * synchronous and asynchronous clients.
 *
 * <p>Each test injects a delay that is large relative to everything else the call does, into exactly one phase, and then
 * asserts that the delay is visible in the reported {@code ApiCallDuration}. A phase that is outside the measured window
 * contributes nothing to it, so the assertion fails outright rather than by a margin. This is deliberately stronger than
 * checking the additivity formula: the phases at issue normally cost microseconds against a call that costs
 * milliseconds, so an inequality over real timings passes whether or not they are included.
 *
 * <p>The phases covered are work before marshalling, endpoint resolution, the {@code afterExecution} interceptors, and
 * completion of an {@link AsyncResponseTransformer}. The first three are places the window has been wrong, on one or both
 * clients. The last is not a past defect but is the documented end of the asynchronous window for streaming operations.
 */
public class ApiCallDurationWindowTest {

    /**
     * Large enough to dwarf the rest of the call and to survive scheduling jitter, small enough to keep the test quick.
     */
    private static final Duration INJECTED_DELAY = Duration.ofMillis(300);

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private MetricPublisher publisher;
    private ScheduledExecutorService scheduler;

    @Before
    public void setup() {
        publisher = mock(MetricPublisher.class);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200)
                                                     .withHeader("x-amz-request-id", "req-id")
                                                     .withBody("{}")));
    }

    @After
    public void teardown() {
        scheduler.shutdownNow();
    }

    @Test
    public void syncClient_apiCallDuration_includesWorkBeforeMarshalling() {
        callSync(b -> b.addExecutionInterceptor(new DelayingInterceptor(Phase.BEFORE_MARSHALLING)), null);
        assertDelayIsMeasured();
    }

    @Test
    public void asyncClient_apiCallDuration_includesWorkBeforeMarshalling() {
        callAsync(b -> b.addExecutionInterceptor(new DelayingInterceptor(Phase.BEFORE_MARSHALLING)), null);
        assertDelayIsMeasured();
    }

    @Test
    public void syncClient_apiCallDuration_includesEndpointResolution() {
        callSync(b -> {
        }, slowEndpointProvider());
        assertDelayIsMeasured();
    }

    @Test
    public void asyncClient_apiCallDuration_includesEndpointResolution() {
        callAsync(b -> {
        }, slowEndpointProvider());
        assertDelayIsMeasured();
    }

    @Test
    public void syncClient_apiCallDuration_includesAfterExecutionInterceptors() {
        callSync(b -> b.addExecutionInterceptor(new DelayingInterceptor(Phase.AFTER_EXECUTION)), null);
        assertDelayIsMeasured();
    }

    @Test
    public void asyncClient_apiCallDuration_includesAfterExecutionInterceptors() {
        callAsync(b -> b.addExecutionInterceptor(new DelayingInterceptor(Phase.AFTER_EXECUTION)), null);
        assertDelayIsMeasured();
    }

    /**
     * The async window closes when the future returned to the caller completes, which for a streaming operation is when
     * the {@link AsyncResponseTransformer} completes. This delays only that completion — the transformer schedules it
     * rather than blocking, so no pipeline stage and no event loop thread is held up. Anything the delay shows up in must
     * therefore be measuring as far as the transformer.
     */
    @Test
    public void asyncClient_apiCallDuration_includesResponseTransformerCompletion() {
        try (ProtocolRestJsonAsyncClient client = asyncClientBuilder(b -> {
        }, null).build()) {
            client.streamingOutputOperation(r -> {
            }, new DelayedCompletionTransformer()).join();
        }

        assertDelayIsMeasured();
    }

    private void assertDelayIsMeasured() {
        assertThat(capturedApiCallDuration())
            .as("ApiCallDuration must include the delay injected into the phase under test")
            .isGreaterThanOrEqualTo(INJECTED_DELAY);
    }

    private Duration capturedApiCallDuration() {
        ArgumentCaptor<MetricCollection> captor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(publisher).publish(captor.capture());
        MetricCollection apiCall = captor.getValue();
        ApiCallDurationAssertions.assertEnclosesComponents(apiCall);
        return apiCall.metricValues(CoreMetric.API_CALL_DURATION).get(0);
    }

    private void callSync(Consumer<ClientOverrideConfiguration.Builder> overrides,
                          ProtocolRestJsonEndpointProvider endpointProvider) {
        ProtocolRestJsonClientBuilder builder =
            ProtocolRestJsonClient.builder()
                                  .region(Region.US_WEST_2)
                                  .credentialsProvider(MockIdentityProviderUtil.mockIdentityProvider())
                                  .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                  .overrideConfiguration(c -> {
                                      c.addMetricPublisher(publisher).retryStrategy(b -> b.maxAttempts(1));
                                      overrides.accept(c);
                                  });
        if (endpointProvider != null) {
            builder.endpointProvider(endpointProvider);
        }
        try (ProtocolRestJsonClient client = builder.build()) {
            client.allTypes();
        }
    }

    private void callAsync(Consumer<ClientOverrideConfiguration.Builder> overrides,
                           ProtocolRestJsonEndpointProvider endpointProvider) {
        try (ProtocolRestJsonAsyncClient client = asyncClientBuilder(overrides, endpointProvider).build()) {
            client.allTypes().join();
        }
    }

    private ProtocolRestJsonAsyncClientBuilder asyncClientBuilder(
        Consumer<ClientOverrideConfiguration.Builder> overrides,
        ProtocolRestJsonEndpointProvider endpointProvider) {

        ProtocolRestJsonAsyncClientBuilder builder =
            ProtocolRestJsonAsyncClient.builder()
                                       .region(Region.US_WEST_2)
                                       .credentialsProvider(MockIdentityProviderUtil.mockIdentityProvider())
                                       .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                       .overrideConfiguration(c -> {
                                           c.addMetricPublisher(publisher).retryStrategy(b -> b.maxAttempts(1));
                                           overrides.accept(c);
                                       });
        if (endpointProvider != null) {
            builder.endpointProvider(endpointProvider);
        }
        return builder;
    }

    private ProtocolRestJsonEndpointProvider slowEndpointProvider() {
        ProtocolRestJsonEndpointProvider delegate = ProtocolRestJsonEndpointProvider.defaultProvider();
        return new ProtocolRestJsonEndpointProvider() {
            @Override
            public CompletableFuture<Endpoint> resolveEndpoint(ProtocolRestJsonEndpointParams endpointParams) {
                sleep();
                return delegate.resolveEndpoint(endpointParams);
            }
        };
    }

    private static void sleep() {
        try {
            Thread.sleep(INJECTED_DELAY.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private enum Phase {
        BEFORE_MARSHALLING,
        AFTER_EXECUTION
    }

    /**
     * Drains the response body, then completes its future {@link #INJECTED_DELAY} later via the scheduler rather than by
     * sleeping, so the delay is purely in the transformer's completion and not in any thread the SDK owns.
     */
    private final class DelayedCompletionTransformer
        implements AsyncResponseTransformer<StreamingOutputOperationResponse, Void> {

        private volatile CompletableFuture<Void> result;

        @Override
        public CompletableFuture<Void> prepare() {
            result = new CompletableFuture<>();
            return result;
        }

        @Override
        public void onResponse(StreamingOutputOperationResponse response) {
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            publisher.subscribe(new Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ByteBuffer byteBuffer) {
                }

                @Override
                public void onError(Throwable throwable) {
                    result.completeExceptionally(throwable);
                }

                @Override
                public void onComplete() {
                    scheduler.schedule(() -> result.complete(null), INJECTED_DELAY.toMillis(), TimeUnit.MILLISECONDS);
                }
            });
        }

        @Override
        public void exceptionOccurred(Throwable error) {
            result.completeExceptionally(error);
        }
    }

    private static final class DelayingInterceptor implements ExecutionInterceptor {
        private final Phase phase;

        private DelayingInterceptor(Phase phase) {
            this.phase = phase;
        }

        @Override
        public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
            if (phase == Phase.BEFORE_MARSHALLING) {
                sleep();
            }
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            if (phase == Phase.AFTER_EXECUTION) {
                sleep();
            }
        }
    }
}
