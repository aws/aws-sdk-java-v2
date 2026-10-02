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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.time.Duration;
import java.util.stream.Stream;

class BaseRetryableStageTest {
    // note: values are in seconds
    protected static final String RETRY_AFTER_HEADER = "Retry-After";
    // note: values are in ms
    protected static final String X_AMZ_RETRY_AFTER_HEADER = "x-amz-retry-after";


    protected static Stream<AcquireDelayTestCase> acquireDelayTestCases() {
        return Stream.of(
            new AcquireDelayTestCase(true, Duration.ofDays(1), Duration.ZERO),
            new AcquireDelayTestCase(true, Duration.ofDays(1), Duration.ofMillis(100)),

            new AcquireDelayTestCase(false, Duration.ZERO, Duration.ofDays(1)),
            new AcquireDelayTestCase(false, Duration.ofMillis(100), Duration.ofDays(1))
        );
    }

    protected static Stream<RetryAfterTestCase> retryAfterTestCases() {
        return Stream.of(
            // v2.0
            new RetryAfterTestCase()
                .description("Parses Retry-After correctly")
                .retryAfter("1")
                .expectedDelay(Duration.ofSeconds(1)),

            new RetryAfterTestCase()
                .description("Ignores format error")
                .retryAfter("one second")
                .expectedDelay(Duration.ZERO),

            new RetryAfterTestCase()
                .description("Ignores int overflow")
                .retryAfter(Long.toString(Long.MAX_VALUE))
                .expectedDelay(Duration.ZERO),

            new RetryAfterTestCase()
                .description("Ignores x-amz-retry-after")
                .retryAfter("1")
                .xAmzRetryAfter("50")
                .expectedDelay(Duration.ofSeconds(1)),

            new RetryAfterTestCase()
                .description("No header, no delay")
                .expectedDelay(Duration.ZERO),

            // v2.1
            new RetryAfterTestCase()
                .newRetries2026Enabled(true)
                .description("Parses x-amz-retry-after correctly")
                .xAmzRetryAfter("1")
                .expectedDelay(Duration.ofMillis(1)),

            new RetryAfterTestCase()
                .newRetries2026Enabled(true)
                .description("Ignores format error")
                .xAmzRetryAfter("one second")
                .expectedDelay(Duration.ZERO),

            new RetryAfterTestCase()
                .newRetries2026Enabled(true)
                .description("Ignores int overflow")
                .xAmzRetryAfter(Long.toString(Long.MAX_VALUE))
                .expectedDelay(Duration.ZERO),

            new RetryAfterTestCase()
                .newRetries2026Enabled(true)
                .description("Ignores Retry-After")
                .retryAfter("1")
                .xAmzRetryAfter("50")
                .expectedDelay(Duration.ofMillis(50)),

            new RetryAfterTestCase()
                .newRetries2026Enabled(true)
                .description("No header, no delay")
                .expectedDelay(Duration.ZERO)
        );
    }


    protected static class AcquireDelayTestCase {
        private boolean failure;
        private Duration successDelay;
        private Duration failureDelay;

        public AcquireDelayTestCase(boolean failure, Duration successDelay, Duration failureDelay) {
            this.failure = failure;
            this.successDelay = successDelay;
            this.failureDelay = failureDelay;
        }

        public boolean isFailure() {
            return failure;
        }

        public Duration failureDelay() {
            return failureDelay;
        }

        public Duration successDelay() {
            return successDelay;
        }

        public Duration expectedDelay() {
            if (failure) {
                return failureDelay;
            }
            return successDelay;
        }

        @Override
        public String toString() {
            return (failure ? "Failure" : "Success") + " with delay " + expectedDelay();
        }
    }


    protected static class RetryAfterTestCase {
        private String description;
        private String retryAfter;
        private String xAmzRetryAfter;
        private boolean newRetries2026Enabled;
        private Duration expectedDelay;

        public RetryAfterTestCase description(String description) {
            this.description = description;
            return this;
        }

        public RetryAfterTestCase retryAfter(String retryAfter) {
            this.retryAfter = retryAfter;
            return this;
        }

        public String retryAfter() {
            return retryAfter;
        }

        public RetryAfterTestCase xAmzRetryAfter(String xAmzRetryAfter) {
            this.xAmzRetryAfter = xAmzRetryAfter;
            return this;
        }

        public String xAmzRetryAfter() {
            return xAmzRetryAfter;
        }

        public RetryAfterTestCase newRetries2026Enabled(boolean newRetries2026Enabled) {
            this.newRetries2026Enabled = newRetries2026Enabled;
            return this;
        }

        public boolean isNewRetries2026Enabled() {
            return newRetries2026Enabled;
        }

        public RetryAfterTestCase expectedDelay(Duration expectedDelay) {
            this.expectedDelay = expectedDelay;
            return this;
        }

        public Duration expectedDelay() {
            return expectedDelay;
        }

        @Override
        public String toString() {
            if (newRetries2026Enabled) {
                return "[v2.1] " + description;
            }
            return description;
        }
    }
}
