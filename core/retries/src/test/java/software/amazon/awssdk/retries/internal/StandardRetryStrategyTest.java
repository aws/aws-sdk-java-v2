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

package software.amazon.awssdk.retries.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;

public class StandardRetryStrategyTest {
    @ParameterizedTest
    @MethodSource("longPollingScenarios")
    void refreshRetryToken_v2_1_longPolling_behavesCorrectly(Scenario scenario) {
        DefaultStandardRetryStrategy.Builder builder =
            (DefaultStandardRetryStrategy.Builder) DefaultRetryStrategy.standardStrategyBuilder(true);

        StandardRetryStrategy strategy = builder.tokenBucketStore(TokenBucketStore.builder()
                                                                                  .tokenBucketMaxCapacity(0)
                                                                                  .build())
                                                .retryOnException(e -> true)
                                                .build();

        AcquireInitialTokenResponse initialToken = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test"));

        RetryToken token = initialToken.token();

        Given given = scenario.given;

        for (Response response : scenario.responses) {

            Expected expected = response.expected;

            switch (expected.outcome) {
                case RETRY_QUOTA_EXCEEDED: {
                    ScenarioTestException scenarioTestException = new ScenarioTestException(response.statusCode);
                    RefreshRetryTokenRequest refreshRequest = RefreshRetryTokenRequest.builder()
                                                                                      .failure(scenarioTestException)
                                                                                      .isLongPolling(given.isLongPolling)
                                                                                      .token(token)
                                                                                      .build();


                    assertThatThrownBy(() -> strategy.refreshRetryToken(refreshRequest))
                        .isInstanceOf(TokenAcquisitionFailedException.class)
                        .matches(e -> {
                            TokenAcquisitionFailedException acquireException = (TokenAcquisitionFailedException) e;
                            Optional<Duration> delay = acquireException.delay();
                            return delay.get().compareTo(expected.delay) <= 0;
                        }, String.format("Token acquire exception has a delay between 0 and %s (jittered)", expected.delay));
                }
                break;
                default:
                    throw new RuntimeException("unknown outcome");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("longPollingScenarios")
    void refreshRetryToken_v2_0_longPolling_behavesCorrectly(Scenario scenario) {
        DefaultStandardRetryStrategy.Builder builder =
            (DefaultStandardRetryStrategy.Builder) DefaultRetryStrategy.standardStrategyBuilder(false);

        StandardRetryStrategy strategy = builder.tokenBucketStore(TokenBucketStore.builder()
                                                                                  .tokenBucketMaxCapacity(0)
                                                                                  .build())
                                                .retryOnException(e -> true)
                                                .build();

        AcquireInitialTokenResponse initialToken = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test"));

        RetryToken token = initialToken.token();

        Given given = scenario.given;

        for (Response response : scenario.responses) {

            Expected expected = response.expected;

            switch (expected.outcome) {
                case RETRY_QUOTA_EXCEEDED: {
                    ScenarioTestException scenarioTestException = new ScenarioTestException(response.statusCode);
                    RefreshRetryTokenRequest refreshRequest = RefreshRetryTokenRequest.builder()
                                                                                      .failure(scenarioTestException)
                                                                                      .isLongPolling(given.isLongPolling)
                                                                                      .token(token)
                                                                                      .build();


                    assertThatThrownBy(() -> strategy.refreshRetryToken(refreshRequest))
                        .isInstanceOf(TokenAcquisitionFailedException.class)
                        .matches(e -> {
                            TokenAcquisitionFailedException acquireException = (TokenAcquisitionFailedException) e;
                            Optional<Duration> delay = acquireException.delay();
                            return delay.get().isZero();
                        }, "Token acquire exception has no delay");
                }
                break;
                default:
                    throw new RuntimeException("unknown outcome");
            }
        }
    }

    private static class ScenarioTestException extends RuntimeException {
        private final int statusCode;

        public ScenarioTestException(int statusCode) {
            this.statusCode = statusCode;
        }
    }

    private static Stream<Scenario> longPollingScenarios() {
        return Stream.of(
            aScenario("Long-Polling Backoff When Token Bucket Empty")
                .given(g -> g.isLongPolling(true))
                .addResponse(r -> r.statusCode(500)
                                   .expected(e -> e.retryQuota(0)
                                                   .delay(Duration.ofMillis(50))
                                                   .outcome(Outcome.RETRY_QUOTA_EXCEEDED))),
            aScenario("Not Long-Polling, Does Not Backoff When Token Bucket Empty")
                .given(g -> g.isLongPolling(false))
                .addResponse(r -> r.statusCode(500)
                                   .expected(e -> e.retryQuota(0)
                                                   .delay(Duration.ZERO)
                                                   .outcome(Outcome.RETRY_QUOTA_EXCEEDED)))
        );
    }

    private static Scenario aScenario(String description) {
        return new Scenario(description);
    }

    private static class Given {
        private int maxAttempts;
        private boolean isLongPolling;
        private Duration maxBackoff;

        public Given maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Given isLongPolling(boolean isLongPolling) {
            this.isLongPolling = isLongPolling;
            return this;
        }

        public Given maxBackoff(Duration maxBackoff) {
            this.maxBackoff = maxBackoff;
            return this;
        }
    }

    private static class Response {
        private int statusCode;
        private Expected expected;

        public Response statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }


        public Response expected(Consumer<Expected> acceptor) {
            this.expected = new Expected();
            acceptor.accept(this.expected);
            return this;
        }
    }

    private static class Expected {
        private Outcome outcome;
        private int retryQuota;
        private Duration delay;

        public Expected outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Expected retryQuota(int retryQuota) {
            this.retryQuota = retryQuota;
            return this;
        }

        public Expected delay(Duration delay) {
            this.delay = delay;
            return this;
        }
    }

    private enum Outcome {
        RETRY_REQUEST,
        RETRY_QUOTA_EXCEEDED,
        MAX_ATTEMPTS_EXCEEDED,
        SUCCESS
    }

    private static class Scenario {
        private String description;
        private Given given;
        private List<Response> responses = new ArrayList<>();

        public Scenario(String description) {
            this.description = description;
        }

        public Scenario given(Consumer<Given> acceptor) {
            this.given = new Given();
            acceptor.accept(this.given);
            return this;
        }

        public Scenario addResponse(Consumer<Response> acceptor) {
            Response response = new Response();
            acceptor.accept(response);
            responses.add(response);
            return this;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
