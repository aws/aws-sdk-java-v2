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

package software.amazon.awssdk.core.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Lives in sdk-core because the retries module has no logging test dependencies.
 */
class DoNotRetryWhenPredicateFailureLoggingTest {

    @Test
    void throwingPredicate_logsWarningOncePerStrategy() {
        RetryStrategy strategy = StandardRetryStrategy.builder()
                                                      .retryOnException(t -> true)
                                                      .doNotRetryWhen(r -> r.operationName().get().isEmpty())
                                                      .build();

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            for (int i = 0; i < 3; i++) {
                RefreshRetryTokenRequest request = refreshRequest(strategy);
                assertThatThrownBy(() -> strategy.refreshRetryToken(request))
                    .isInstanceOf(TokenAcquisitionFailedException.class)
                    .hasCause(request.failure());
            }

            assertThat(logCaptor.loggedEvents()).filteredOn(e -> isPredicateFailure(e, Level.WARN)).hasSize(1);
            assertThat(logCaptor.loggedEvents()).filteredOn(e -> isPredicateFailure(e, Level.DEBUG)).hasSize(2);
        }
    }

    private static boolean isPredicateFailure(LogEvent event, Level level) {
        return event.getLevel() == level
               && event.getMessage().getFormattedMessage().contains("doNotRetryWhen predicate threw an exception");
    }

    private static RefreshRetryTokenRequest refreshRequest(RetryStrategy strategy) {
        return RefreshRetryTokenRequest.builder()
                                       .token(strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token())
                                       .failure(new RuntimeException("failed"))
                                       .build();
    }
}
