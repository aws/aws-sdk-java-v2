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

package software.amazon.awssdk.core.internal.useragent.businessmetrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.internal.useragent.BusinessMetricsUtils;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.retries.api.RetryStrategy;

class BusinessMetricsUtilsTest {

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void when_retryModeMetric_isResolvedFromInput_correctMetricIsReturned(String description, RetryPolicy retryPolicy,
                                          RetryStrategy retryStrategy, String expected) {
        Optional<String> retryModeMetric = BusinessMetricsUtils.resolveRetryMode(retryPolicy, retryStrategy);

        if (expected != null) {
            assertThat(retryModeMetric).isPresent().hasValue(expected);
        } else {
            assertThat(retryModeMetric).isEmpty();
        }
    }

    private static Stream<Arguments> inputValues() {
        return Stream.of(
            Arguments.of("No retry input returns empty", null, null, null),
            Arguments.of("Retry policy for legacy mode returns legacy",
                         RetryPolicy.forRetryMode(RetryMode.LEGACY), null,
                         BusinessMetricFeatureId.RETRY_MODE_LEGACY.value()),
            Arguments.of("Retry policy for standard mode returns standard",
                         RetryPolicy.forRetryMode(RetryMode.STANDARD), null,
                         BusinessMetricFeatureId.RETRY_MODE_STANDARD.value()),
            Arguments.of("Retry policy for adaptive mode returns adaptive",
                         RetryPolicy.forRetryMode(RetryMode.ADAPTIVE), null,
                         BusinessMetricFeatureId.RETRY_MODE_ADAPTIVE.value()),
            Arguments.of("Retry strategy for legacy mode returns legacy", null,
                         SdkDefaultRetryStrategy.forRetryMode(RetryMode.LEGACY),
                         BusinessMetricFeatureId.RETRY_MODE_LEGACY.value()),
            Arguments.of("Retry strategy for standard mode returns standard", null,
                         SdkDefaultRetryStrategy.forRetryMode(RetryMode.STANDARD),
                         BusinessMetricFeatureId.RETRY_MODE_STANDARD.value()),
            Arguments.of("Retry strategy for adaptive 2 mode returns adaptive", null,
                         SdkDefaultRetryStrategy.forRetryMode(RetryMode.ADAPTIVE_V2),
                         BusinessMetricFeatureId.RETRY_MODE_ADAPTIVE.value()),
            Arguments.of("Retry policy overrides retry strategy",
                         RetryPolicy.forRetryMode(RetryMode.LEGACY),
                         SdkDefaultRetryStrategy.forRetryMode(RetryMode.ADAPTIVE_V2),
                         BusinessMetricFeatureId.RETRY_MODE_LEGACY.value())
        );
    }
}
