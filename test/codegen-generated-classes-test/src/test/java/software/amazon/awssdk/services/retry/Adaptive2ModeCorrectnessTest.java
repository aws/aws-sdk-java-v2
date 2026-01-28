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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClientBuilder;

/**
 * Tests that the ADAPTIVE2 mode behaves as designed. The setup is an API that is rate limited, and a single client is used by
 * multiple threads to make calls to this API. The ADAPTIVE2 mode should "adapt" its calling rate to closely match the expected
 * rate of the API with little overhead (wasted calls).
 *
 * This test might be brittle depending on the hardware is run-on. If proven so we should
 * remove it or tweak the expected assertions.
 */
public class Adaptive2ModeCorrectnessTest {
    private WireMockServer wireMock;
    private AtomicInteger successful;
    private AtomicInteger failed;

    @Test
    public void adaptive2RetryModeBehavesCorrectly() throws InterruptedException {
        stubResponse();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CapturingInterceptor interceptor = new CapturingInterceptor();
        ProtocolRestJsonClient client = clientBuilder()
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor)
                                         .retryStrategy(RetryMode.ADAPTIVE_V2))
            .build();

        int totalRequests = 250;
        for (int i = 0; i < totalRequests; ++i) {
            executor.execute(callAllTypes(client));
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(120, TimeUnit.SECONDS)).isTrue();
        double perceivedAvailability = ((double) successful.get() / totalRequests) * 100;
        double overhead = ((double) interceptor.attemptsCount.get() / totalRequests) * 100 - 100;
        assertThat(perceivedAvailability).isCloseTo(100.0, withinPercentage(20.0));
        assertThat(overhead).isCloseTo(10.0, withinPercentage(100.0));
    }

    private Runnable callAllTypes(ProtocolRestJsonClient client) {
        return () -> {
            try {
                client.allTypes();
                successful.incrementAndGet();
            } catch (SdkException e) {
                failed.incrementAndGet();
            }
        };
    }

    private ProtocolRestJsonClientBuilder clientBuilder() {
        return ProtocolRestJsonClient
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                             "skid")))
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()));
    }

    @Before
    public void setup() {
        successful = new AtomicInteger(0);
        failed = new AtomicInteger(0);
        wireMock = new WireMockServer(wireMockConfig()
                                          .extensions(RateLimiterResponseTransformer.class));
        wireMock.start();
    }

    @After
    public void tearDown() {
        wireMock.stop();
    }

    private void stubResponse() {
        wireMock.stubFor(post(anyUrl())
                             .willReturn(aResponse()
                                             .withTransformers("rate-limiter-transformer")));
    }

    public static class RateLimiterResponseTransformer extends ResponseTransformer {
        private final RateLimiter rateLimiter = new RateLimiter();

        @Override
        public String getName() {
            return "rate-limiter-transformer";
        }

        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            if (rateLimiter.allowRequest()) {
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
    }

    static class RateLimiter {
        private final long capacity;
        private final long refillRate;
        private final AtomicLong tokens;
        private long lastRefillTimestamp;

        public RateLimiter() {
            this.capacity = 50;
            this.refillRate = 50;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean allowRequest() {
            int tokensRequested = 1;
            refillTokens();
            long currentTokens = tokens.get();
            if (currentTokens >= tokensRequested) {
                tokens.getAndAdd(-tokensRequested);
                return true;
            }
            return false;
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTimestamp;
            long refillAmount = elapsed * refillRate / 1000;
            tokens.set(Math.min(capacity, tokens.get() + refillAmount));
            lastRefillTimestamp = now;
        }
    }

    static class CapturingInterceptor implements ExecutionInterceptor {
        private AtomicInteger attemptsCount = new AtomicInteger();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            attemptsCount.incrementAndGet();
        }
    }
}
