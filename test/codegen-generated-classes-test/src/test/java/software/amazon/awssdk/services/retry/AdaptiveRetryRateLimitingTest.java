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
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.services.json.JsonClient;

/**
 * End-to-end testing for testing the correctness of the rate limiting implementation of the adaptive retry strategy.
 */
class AdaptiveRetryRateLimitingTest {
    private static final AtomicLong eventCount = new AtomicLong(0L);

    private static final int throttleTps = 20;
    private static final double tpsLower = throttleTps * 0.5;
    private static final double tpsUpper = throttleTps * 1.5;

    private static final Duration testDuration = Duration.ofSeconds(30);
    private static final Duration warmupDuration = Duration.ofSeconds(2);
    private static final Duration tailTrim = Duration.ofSeconds(1);
    private static final int numWorkerThreads = Runtime.getRuntime().availableProcessors();

    private static final RollingThrottle throttler = new RollingThrottle(throttleTps);

    private static String newRetriesPropertySave;

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
                                                   .options(options().dynamicPort()
                                                                     .extensions(new ThrottlingTransformer()))
                                                   .build();

    @BeforeAll
    static void setup() {
        newRetriesPropertySave = System.getProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "true");
    }

    @AfterAll
    static void teardown() {
        if (newRetriesPropertySave != null) {
            System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), newRetriesPropertySave);
        } else {
            System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        }
    }

    /**
     * The server has a static max TPS of 20 before responding with throttling exceptions. The client sending rate should match
     * this within an acceptable margin.
     */
    @Test
    void staticServerThrottling() throws Exception {

        wm.stubFor(any(anyUrl()).willReturn(aResponse().withTransformers("throttling")));

        AdaptiveRetryStrategy adaptiveRetryStrategy = AwsRetryStrategy.adaptiveRetryStrategy(true);

        JsonClient client = JsonClient.builder()
                                      .endpointOverride(URI.create(wm.baseUrl()))
                                      .region(Region.US_WEST_2)
                                      .credentialsProvider(StaticCredentialsProvider.create(
                                          AwsBasicCredentials.create("akid", "secret")))
                                      .overrideConfiguration(o -> o.retryStrategy(adaptiveRetryStrategy))
                                      .build();

        Instant startT = Instant.now();
        Instant endAt = startT.plus(testDuration);
        AtomicBoolean stop = new AtomicBoolean(false);

        ExecutorService pool = Executors.newFixedThreadPool(numWorkerThreads);
        try {
            List<Future<?>> futures = IntStream.range(0, numWorkerThreads)
                                               .mapToObj(i -> pool.submit(() -> {
                                                   while (!stop.get() && Instant.now().isBefore(endAt)) {
                                                       try {
                                                           client.allType(r -> {
                                                           });
                                                       } catch (Exception e) {
                                                           // We measure wire-level TPS, not call success.
                                                           // Throttling exceptions surface here once retries are exhausted.
                                                       }
                                                   }
                                               }))
                                               .collect(Collectors.toList());

            for (Future<?> f : futures) {
                f.get(testDuration.getSeconds() + 30, TimeUnit.SECONDS);
            }
        } finally {
            stop.set(true);
        }
        Instant endT = Instant.now();

        double measuredTps = tpsInWindow(
            wm.getAllServeEvents(),
            startT.plus(warmupDuration),
            endT.minus(tailTrim));

        assertThat(measuredTps)
            .as("Expected send rate >= %.1f TPS, but got %.1f TPS. "
                + "Is the SDK not converging to the server throttle rate?",
                tpsLower, measuredTps)
            .isGreaterThanOrEqualTo(tpsLower);
        assertThat(measuredTps)
            .as("Expected send rate <= %.1f TPS, but got %.1f TPS. "
                + "Is the SDK not respecting the server throttle rate limit?",
                tpsUpper, measuredTps)
            .isLessThanOrEqualTo(tpsUpper);
    }

    private static double tpsInWindow(List<ServeEvent> events, Instant from, Instant to) {
        long count = events.stream()
                           .map(e -> e.getRequest().getLoggedDate().toInstant())
                           .filter(t -> !t.isBefore(from) && t.isBefore(to))
                           .count();
        double seconds = Duration.between(from, to).toMillis() / 1000.0;
        return seconds <= 0 ? 0 : count / seconds;
    }

    /**
     * Sliding 1-second window admission control. Once {@code TPS} requests have been admitted within the past second, returns
     * false until the window slides.
     */
    static final class RollingThrottle {
        private final int tps;
        private final ConcurrentLinkedDeque<Long> hits = new ConcurrentLinkedDeque<>();

        RollingThrottle(int tps) {
            this.tps = tps;
        }

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            long cutoff = now - TimeUnit.SECONDS.toNanos(1);
            while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
                hits.pollFirst();
            }
            if (hits.size() >= tps) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }

    /**
     * WireMock transformer that delegates the 200-vs-throttle decision to {@link #throttler}.
     */
    public static final class ThrottlingTransformer extends ResponseDefinitionTransformer {

        private static final String successBody = "{}";
        private static final String throttleBody =
            "{\"__type\":\"ThrottlingException\",\"message\":\"Rate exceeded\"}";

        @Override
        public String getName() {
            return "throttling";
        }

        @Override
        public boolean applyGlobally() {
            return false;
        }

        @Override
        public ResponseDefinition transform(Request request,
                                            ResponseDefinition responseDefinition,
                                            FileSource files,
                                            Parameters parameters) {
            eventCount.incrementAndGet();

            if (throttler.tryAcquire()) {
                return aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/x-amz-json-1.0")
                    .withBody(successBody)
                    .build();
            }
            return aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/x-amz-json-1.0")
                .withHeader("x-amzn-ErrorType", "ThrottlingException")
                .withBody(throttleBody)
                .build();
        }
    }
}
