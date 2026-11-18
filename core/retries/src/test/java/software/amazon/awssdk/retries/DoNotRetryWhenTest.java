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

package software.amazon.awssdk.retries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.internal.DefaultRetryToken;

class DoNotRetryWhenTest {
    private static final String EXCLUDED = "SubscribeToShard";
    private static final Predicate<RefreshRetryTokenRequest> IS_EXCLUDED =
        r -> r.operationName().filter(EXCLUDED::equals).isPresent();

    private static Stream<Arguments> builders() {
        return Stream.of(
            Arguments.of("standard", (Supplier<RetryStrategy.Builder<?, ?>>) StandardRetryStrategy::builder),
            Arguments.of("adaptive", (Supplier<RetryStrategy.Builder<?, ?>>) AdaptiveRetryStrategy::builder),
            Arguments.of("legacy", (Supplier<RetryStrategy.Builder<?, ?>>) LegacyRetryStrategy::builder));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builders")
    void matchingRequest_isNotRetried(String name, Supplier<RetryStrategy.Builder<?, ?>> builder) {
        RetryStrategy strategy = builder.get().retryOnException(t -> true).doNotRetryWhen(IS_EXCLUDED).build();
        assertNotRetried(strategy, EXCLUDED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builders")
    void otherRequests_areRetried(String name, Supplier<RetryStrategy.Builder<?, ?>> builder) {
        RetryStrategy strategy = builder.get().retryOnException(t -> true).doNotRetryWhen(IS_EXCLUDED).build();
        assertRetried(strategy, "ListShards");
        assertRetried(strategy, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builders")
    void toBuilder_keepsPredicate(String name, Supplier<RetryStrategy.Builder<?, ?>> builder) {
        RetryStrategy strategy = builder.get().retryOnException(t -> true).doNotRetryWhen(IS_EXCLUDED).build()
                                        .toBuilder()
                                        .maxAttempts(5)
                                        .build();
        assertNotRetried(strategy, EXCLUDED);
        assertRetried(strategy, "ListShards");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("builders")
    void throwingPredicate_isNotRetriedAndKeepsOriginalFailure(String name, Supplier<RetryStrategy.Builder<?, ?>> builder) {
        RetryStrategy strategy = builder.get()
                                        .retryOnException(t -> true)
                                        .doNotRetryWhen(r -> r.operationName().get().isEmpty())
                                        .build();
        RefreshRetryTokenRequest request = refreshRequest(strategy, null);

        assertThatThrownBy(() -> strategy.refreshRetryToken(request))
            .isInstanceOf(TokenAcquisitionFailedException.class)
            .hasCause(request.failure())
            .matches(e -> ((DefaultRetryToken) ((TokenAcquisitionFailedException) e).token()).state()
                          == DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION);
    }

    @Test
    void withoutPredicate_matchingRequestIsRetried() {
        assertRetried(StandardRetryStrategy.builder().retryOnException(t -> true).build(), EXCLUDED);
    }

    @Test
    void refreshRequest_operationNameDefaultsToEmpty() {
        RefreshRetryTokenRequest request = RefreshRetryTokenRequest.builder()
                                                                   .token(initialToken(StandardRetryStrategy.builder().build()))
                                                                   .failure(new RuntimeException())
                                                                   .build();
        assertThat(request.operationName()).isEmpty();
        assertThat(request.toBuilder().operationName("GetObject").build().operationName()).hasValue("GetObject");
    }

    private static void assertRetried(RetryStrategy strategy, String operationName) {
        assertThat(strategy.refreshRetryToken(refreshRequest(strategy, operationName)).token()).isNotNull();
    }

    private static void assertNotRetried(RetryStrategy strategy, String operationName) {
        assertThatThrownBy(() -> strategy.refreshRetryToken(refreshRequest(strategy, operationName)))
            .isInstanceOf(TokenAcquisitionFailedException.class)
            .matches(e -> ((DefaultRetryToken) ((TokenAcquisitionFailedException) e).token()).state()
                          == DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION);
    }

    private static RefreshRetryTokenRequest refreshRequest(RetryStrategy strategy, String operationName) {
        return RefreshRetryTokenRequest.builder()
                                       .token(initialToken(strategy))
                                       .failure(new RuntimeException("failed"))
                                       .operationName(operationName)
                                       .build();
    }

    private static RetryToken initialToken(RetryStrategy strategy) {
        return strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
    }
}
