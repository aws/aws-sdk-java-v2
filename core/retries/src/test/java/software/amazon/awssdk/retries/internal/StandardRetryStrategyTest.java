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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;

public class StandardRetryStrategyTest {
    private static final Duration BASE_DELAY_V21_THROTTLING = Duration.ofMillis(1000);
    private static final Duration BASE_DELAY_V21_NON_THROTTLING = Duration.ofMillis(50);
    private static final Duration BASE_DELAY_V20 = Duration.ofMillis(1000);

    @ParameterizedTest
    @MethodSource("retriesV20Tests")
    void refreshRetryToken_v2_0_behavesCorrectly(Scenario scenario) {
        verifyScenario(scenario);
    }

    @ParameterizedTest
    @MethodSource("retriesV21Tests")
    void refreshRetryToken_v2_1_behavesCorrectly(Scenario scenario) {
        verifyScenario(scenario);
    }

    void verifyScenario(Scenario scenario) {
        DefaultStandardRetryStrategy.Builder builder =
            (DefaultStandardRetryStrategy.Builder) DefaultRetryStrategy.standardStrategyBuilder(scenario.newRetries2026);

        Given given = scenario.given;

        if (given.maxAttempts != null) {
            builder.maxAttempts(given.maxAttempts);
        }

        if (given.initialRetryTokens != null) {
            builder.tokenBucketStore(TokenBucketStore.builder()
                                                     .tokenBucketMaxCapacity(given.initialRetryTokens)
                                                     .build());
        }

        Duration maxBackoff;
        if (given.maxBackoff != null) {
            maxBackoff = given.maxBackoff;
        } else {
            maxBackoff = Duration.ofSeconds(20);
        }

        builder.backoffStrategy(BackoffStrategy.exponentialDelayWithoutJitter(
            scenario.newRetries2026 ? BASE_DELAY_V21_NON_THROTTLING : BASE_DELAY_V20,
            maxBackoff));

        builder.throttlingBackoffStrategy(BackoffStrategy.exponentialDelayWithoutJitter(
            scenario.newRetries2026 ? BASE_DELAY_V21_THROTTLING : BASE_DELAY_V20,
            maxBackoff
        ));

        StandardRetryStrategy strategy = builder.retryOnException(e -> true)
                                                .treatAsThrottling(e -> ((ScenarioTestException) e).throttling)
                                                .build();

        AcquireInitialTokenResponse initialToken = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test"));

        AtomicReference<RetryToken> token = new AtomicReference<>(initialToken.token());

        for (Response response : scenario.responses) {
            Expected expected = response.expected;

            Outcome outcome = expected.outcome;
            switch (outcome) {
                case RETRY_REQUEST: {
                    ScenarioTestException scenarioTestException = new ScenarioTestException(response.statusCode,
                                                                                            response.throttling);
                    RefreshRetryTokenRequest.Builder refreshRequest = RefreshRetryTokenRequest.builder();

                    if (response.xAmzRetryAfter != null) {
                        refreshRequest.suggestedDelay(response.xAmzRetryAfter);
                    }

                    refreshRequest.failure(scenarioTestException)
                                  .isLongPolling(given.isLongPolling)
                                  .token(token.get())
                                  .build();
                    RefreshRetryTokenResponse refreshResponse = strategy.refreshRetryToken(refreshRequest.build());
                    DefaultRetryToken refreshedToken = (DefaultRetryToken) refreshResponse.token();
                    token.set(refreshedToken);

                    assertThat(refreshResponse.delay()).isEqualTo(expected.delay);
                    assertThat(refreshedToken.capacityRemaining()).isEqualTo(expected.retryQuota);
                }
                break;
                case RETRY_QUOTA_EXCEEDED: {
                    ScenarioTestException scenarioTestException = new ScenarioTestException(response.statusCode,
                                                                                            response.throttling);
                    RefreshRetryTokenRequest.Builder refreshRequest = RefreshRetryTokenRequest.builder();

                    if (response.xAmzRetryAfter != null) {
                        refreshRequest.suggestedDelay(response.xAmzRetryAfter);
                    }

                    refreshRequest.failure(scenarioTestException)
                                  .isLongPolling(given.isLongPolling)
                                  .token(token.get())
                                  .build();

                    assertThatThrownBy(() -> strategy.refreshRetryToken(refreshRequest.build()))
                        .isInstanceOf(TokenAcquisitionFailedException.class)
                        .matches(e -> {
                                     TokenAcquisitionFailedException acquireException = (TokenAcquisitionFailedException) e;
                                     DefaultRetryToken acquireToken = (DefaultRetryToken) acquireException.token();
                                     token.set(acquireToken);

                                     Duration acquireDelay = acquireException.delay().orElse(Duration.ZERO);
                                     Duration expectedDelay = expected.delay == null ? Duration.ZERO : expected.delay;

                                     return acquireToken.state() == DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED
                                            && acquireToken.capacityRemaining() == expected.retryQuota
                                            && acquireDelay.equals(expectedDelay);
                                 },
                                 "Token has TOKEN_ACQUISITION_FAILED state and capacity of "
                                 + expected.retryQuota
                                 + " and delay of " + expected.delay);
                }
                break;
                case MAX_ATTEMPTS_EXCEEDED: {
                    ScenarioTestException scenarioTestException = new ScenarioTestException(response.statusCode,
                                                                                            response.throttling);
                    RefreshRetryTokenRequest.Builder refreshRequest = RefreshRetryTokenRequest.builder();

                    if (response.xAmzRetryAfter != null) {
                        refreshRequest.suggestedDelay(response.xAmzRetryAfter);
                    }

                    refreshRequest.failure(scenarioTestException)
                                  .isLongPolling(given.isLongPolling)
                                  .token(token.get())
                                  .build();

                    assertThatThrownBy(() -> strategy.refreshRetryToken(refreshRequest.build()))
                        .isInstanceOf(TokenAcquisitionFailedException.class)
                        .matches(e -> {
                            TokenAcquisitionFailedException acquireException = (TokenAcquisitionFailedException) e;
                            DefaultRetryToken acquireToken = (DefaultRetryToken) acquireException.token();
                            token.set(acquireToken);

                            Duration acquireDelay = acquireException.delay().orElse(Duration.ZERO);
                            Duration expectedDelay = expected.delay == null ? Duration.ZERO : expected.delay;

                            return acquireToken.state() == DefaultRetryToken.TokenState.MAX_RETRIES_REACHED
                                   && acquireToken.capacityRemaining() == expected.retryQuota
                                   && acquireDelay.equals(expectedDelay);
                        }, "Token has MAX_RETRIES_REACHED state and has expected retry quota "
                           + expected.retryQuota
                           + " and delay of " + expected.delay);
                }
                break;
                case SUCCESS: {
                    RecordSuccessRequest recordRequest = RecordSuccessRequest.create(token.get());
                    RecordSuccessResponse recordResponse = strategy.recordSuccess(recordRequest);

                    DefaultRetryToken successToken = (DefaultRetryToken) recordResponse.token();
                    token.set(successToken);
                    assertThat(successToken.capacityRemaining()).isEqualTo(expected.retryQuota);
                }
                break;
                default:
                    throw new RuntimeException("unknown outcome");
            }

            // If the last outcome was a terminal state, get a new token so that state is consistent with a new request
            if (outcome == Outcome.SUCCESS
                || outcome == Outcome.MAX_ATTEMPTS_EXCEEDED
                || outcome == Outcome.RETRY_QUOTA_EXCEEDED) {
                AcquireInitialTokenRequest acquireInitialTokenRequest = AcquireInitialTokenRequest.create("test");
                token.set(strategy.acquireInitialToken(acquireInitialTokenRequest).token());
            }
        }
    }

    private static Stream<Scenario> retriesV20Tests() {
        return Stream.of(
            aScenario("Retry eventually succeeds.")
                .given(g ->
                           g.maxAttempts(3).initialRetryTokens(500).maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(495)
                                                 .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(490)
                                                 .delay(Duration.ofSeconds(2))))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(495))),

            aScenario("Fail due to max attempts reached.")
                .given(g ->
                           g.maxAttempts(3)
                            .initialRetryTokens(500)
                            .maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(
                                      e ->
                                          e.outcome(Outcome.RETRY_REQUEST)
                                           .retryQuota(495)
                                           .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(
                                      e ->
                                          e.outcome(Outcome.RETRY_REQUEST)
                                           .retryQuota(490)
                                           .delay(Duration.ofSeconds(2))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(
                                      e ->
                                          e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                           .retryQuota(490))),

            aScenario("Retry Quota reached after a single retry.")
                .given(g ->
                           g.maxAttempts(3)
                            .initialRetryTokens(5)
                            .maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofSeconds(1))
                                                 .retryQuota(0)))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .retryQuota(0))),

            aScenario("No retries at all if retry quota is 0.")
                .given(g ->
                           g.maxAttempts(3)
                            .initialRetryTokens(0)
                            .maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .retryQuota(0))),

            aScenario("Verifying exponential backoff timing.")
                .given(g ->
                           g.maxAttempts(5)
                            .initialRetryTokens(500)
                            .maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(495)
                                                 .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(490)
                                                 .delay(Duration.ofSeconds(2))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(485)
                                                 .delay(Duration.ofSeconds(4))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(480)
                                                 .delay(Duration.ofSeconds(8))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .retryQuota(480))),

            aScenario("Verify max backoff time.")
                .given(g ->
                           g.maxAttempts(5)
                            .initialRetryTokens(500)
                            .maxBackoff(Duration.ofSeconds(3)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(495)
                                                 .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(490)
                                                 .delay(Duration.ofSeconds(2))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(485)
                                                 .delay(Duration.ofSeconds(3))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(480)
                                                 .delay(Duration.ofSeconds(3))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .retryQuota(480))),

            aScenario("Retry Stops After Retry Quota Exhaustion")
                .given(g ->
                           g.maxAttempts(5)
                            .initialRetryTokens(10)
                            .maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(5)
                                                 .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(0)
                                                 .delay(Duration.ofSeconds(2))))
                .addResponse(r ->
                                 r.statusCode(503)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .retryQuota(0))),

            aScenario("Retry quota Recovery After Successful Responses")
                .given(g ->
                           g.maxAttempts(5)
                            .initialRetryTokens(15)
                            .maxBackoff(Duration.ofSeconds(20)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(10)
                                                 .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(5)
                                                 .delay(Duration.ofSeconds(2))))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(10)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(5)
                                                 .delay(Duration.ofSeconds(1))))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(10)
                                                 .delay(Duration.ofSeconds(1)))),

            // taken from v2.1 tests, adjusted with all 0 delay for acquire failures
            aScenario("Long-Polling Backoff After Transient Error When Token Bucket Empty")
                .given(g -> g.isLongPolling(true)
                             .initialRetryTokens(0))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .delay(Duration.ZERO)
                                                 .retryQuota(0))),

            aScenario("Long-Polling Backoff After Throttling Error When Token Bucket Empty")
                .given(g -> g.isLongPolling(true)
                             .initialRetryTokens(0))
                .addResponse(r ->
                                 r.statusCode(400)
                                  .isThrottling(true)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .delay(Duration.ZERO)
                                                 .retryQuota(0))),

            aScenario("Long-Polling Max Attempts Exceeded Must NOT Delay")
                .given(g ->
                           g.isLongPolling(true)
                            .maxAttempts(2))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofSeconds(1))
                                                 .retryQuota(495)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .delay(Duration.ZERO)
                                                 .retryQuota(495)))
        );
    }

    private static Stream<Scenario> retriesV21Tests() {
        return Stream.of(
            aScenario("Retry eventually succeeds.")
                .given(g -> {
                })
                .newRetries2026(true)
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(486)
                                                 .delay(Duration.ofMillis(50))))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(472)
                                                 .delay(Duration.ofMillis(100))))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(486))),

            aScenario("Fail due to max attempts reached.")
                .newRetries2026(true)
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(486)
                                                 .delay(Duration.ofMillis(50))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(472)
                                                 .delay(Duration.ofMillis(100))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .retryQuota(472))),
            aScenario("Retry Quota reached after a single retry.")
                .newRetries2026(true)
                .given(g -> g.initialRetryTokens(14))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .retryQuota(0)
                                                 .delay(Duration.ofMillis(50))))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .retryQuota(0))),
            aScenario("No retries at all if retry quota is 0.")
                .newRetries2026(true)
                .given(g -> g.initialRetryTokens(0))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .retryQuota(0))),
            aScenario("Verifying exponential backoff timing.")
                .newRetries2026(true)
                .given(g -> g.maxAttempts(5))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(486)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(100))
                                                 .retryQuota(472)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(200))
                                                 .retryQuota(458)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(400))
                                                 .retryQuota(444)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .retryQuota(444))),

            aScenario("Verify max backoff time.")
                .newRetries2026(true)
                .given(g -> g.maxAttempts(5).maxBackoff(Duration.ofMillis(200)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(486)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(100))
                                                 .retryQuota(472)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(200))
                                                 .retryQuota(458)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(200))
                                                 .retryQuota(444)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .retryQuota(444))),

            aScenario("Retry Stops After Retry Quota Exhaustion")
                .newRetries2026(true)
                .given(g -> g.maxAttempts(5).initialRetryTokens(20))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(6)))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .retryQuota(6))),

            aScenario("Retry quota Recovery After Successful Responses")
                .newRetries2026(true)
                .given(g -> g.maxAttempts(5).initialRetryTokens(30))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(16)))
                .addResponse(r ->
                                 r.statusCode(502)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(100))
                                                 .retryQuota(2)))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(16)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(2)))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(16))),

            aScenario("Throttling Error Token Bucket Drain (5 tokens) and Backoff Duration (1000ms)")
                .newRetries2026(true)
                .addResponse(r ->
                                 r.statusCode(400)
                                  .isThrottling(true)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(1000))
                                                 .retryQuota(495)))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(500))),

            aScenario("Long-Polling Backoff After Transient Error When Token Bucket Empty")
                .newRetries2026(true)
                .given(g -> g.isLongPolling(true)
                             .initialRetryTokens(0))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(0))),

            aScenario("Long-Polling Backoff After Throttling Error When Token Bucket Empty")
                .newRetries2026(true)
                .given(g -> g.isLongPolling(true)
                             .initialRetryTokens(0))
                .addResponse(r ->
                                 r.statusCode(400)
                                  .isThrottling(true)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_QUOTA_EXCEEDED)
                                                 .delay(Duration.ofMillis(1000))
                                                 .retryQuota(0))),

            aScenario("Long-Polling Max Attempts Exceeded Must NOT Delay")
                .newRetries2026(true)
                .given(g ->
                           g.isLongPolling(true)
                            .maxAttempts(2))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(486)))
                .addResponse(r ->
                                 r.statusCode(500)
                                  .expected(e ->
                                                e.outcome(Outcome.MAX_ATTEMPTS_EXCEEDED)
                                                 .delay(Duration.ZERO)
                                                 .retryQuota(486))),

            aScenario("Honor x-amz-retry-after Header")
                .newRetries2026(true)
                .addResponse(r ->
                                 r.statusCode(500)
                                  .xAmzRetryAfter(Duration.ofMillis(1500))
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(1500))
                                                 .retryQuota(486)))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(500))),

            aScenario("x-amz-retry-after minimum is exponential backoff duration")
                .newRetries2026(true)
                .addResponse(r ->
                                 r.statusCode(500)
                                  .xAmzRetryAfter(Duration.ofMillis(0))
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(50))
                                                 .retryQuota(486)))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(500))),

            aScenario("x-amz-retry-after maximum is 5+exponential backoff duration")
                .newRetries2026(true)
                .addResponse(r ->
                                 r.statusCode(500)
                                  .xAmzRetryAfter(Duration.ofMillis(10000))
                                  .expected(e ->
                                                e.outcome(Outcome.RETRY_REQUEST)
                                                 .delay(Duration.ofMillis(5050))
                                                 .retryQuota(486)))
                .addResponse(r ->
                                 r.statusCode(200)
                                  .expected(e ->
                                                e.outcome(Outcome.SUCCESS)
                                                 .retryQuota(500)))
        );
    }

    private static Scenario aScenario(String description) {
        return new Scenario(description);
    }

    private static class ScenarioTestException extends RuntimeException {
        private final int statusCode;
        private final boolean throttling;

        public ScenarioTestException(int statusCode, boolean throttling) {
            this.statusCode = statusCode;
            this.throttling = throttling;
        }
    }

    private static class Given {
        private Integer maxAttempts;
        private Integer initialRetryTokens;
        private boolean isLongPolling;
        private Duration maxBackoff;

        public Given maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Given initialRetryTokens(int initialRetryTokens) {
            this.initialRetryTokens = initialRetryTokens;
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
        private boolean throttling;
        private Duration xAmzRetryAfter;
        private Expected expected;

        public Response statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Response isThrottling(boolean throttling) {
            this.throttling = throttling;
            return this;
        }

        public Response xAmzRetryAfter(Duration xAmzRetryAfter) {
            this.xAmzRetryAfter = xAmzRetryAfter;
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
        private boolean newRetries2026;
        private Given given = new Given();
        private List<Response> responses = new ArrayList<>();

        public Scenario(String description) {
            this.description = description;
        }

        public Scenario newRetries2026(boolean newRetries2026) {
            this.newRetries2026 = newRetries2026;
            return this;
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
