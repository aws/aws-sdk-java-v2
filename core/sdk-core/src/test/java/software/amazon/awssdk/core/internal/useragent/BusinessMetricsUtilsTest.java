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

package software.amazon.awssdk.core.internal.useragent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;

class BusinessMetricsUtilsTest {
    private SdkHttpFullRequest.Builder testRequest;

    @BeforeEach
    void setup() {
        testRequest = SdkHttpFullRequest.builder();
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("retryModeMetricInput")
    void when_retryModeMetric_isResolvedFromInput_correctMetricIsReturned(String description, RetryPolicy retryPolicy,
                                                                          RetryStrategy retryStrategy, String expected) {
        Optional<String> retryModeMetric = BusinessMetricsUtils.resolveRetryMode(retryPolicy, retryStrategy);

        if (expected != null) {
            assertThat(retryModeMetric).isPresent().hasValue(expected);
        } else {
            assertThat(retryModeMetric).isEmpty();
        }
    }

    @ParameterizedTest(name = "{0} = {1}")
    @MethodSource("checksumFeatureIdInput")
    void when_checksumFeatureId_isResolvedFromHeader_correctMetricIsReturned(BusinessMetricFeatureId id, String header) {
        assertThat(BusinessMetricsUtils.headerToChecksumFeatureId(header)).isEqualTo(id.value());
    }

    @Test
    void when_checksumFeatureId_isResolvedFromHeader_unknownIsMappedToNull() {
        assertThat(BusinessMetricsUtils.headerToChecksumFeatureId("x-amz-checksum-1234567")).isNull();
    }

    @Test
    void when_checksumFeatureIds_areResolvedFromAlgorithmAndHeaders_allAlgorithmFeatureIdsReturned() {
        ChecksumAlgorithm algorithm = DefaultChecksumAlgorithm.XXHASH128;
        testRequest.putHeader("x-amz-checksum-crc32", "my-checksum");

        assertThat(BusinessMetricsUtils.resolveChecksumAlgorithmFeatureIds(algorithm, testRequest))
            .containsExactly(BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH128.value(),
                             BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32.value());
    }

    @Test
    void when_checksumFeatureIds_areResolvedFromAlgorithmAndHeaders_andTheyResoveToTheSameId_idsAreDeduped() {
        ChecksumAlgorithm algorithm = DefaultChecksumAlgorithm.CRC32;
        testRequest.putHeader("x-amz-checksum-crc32", "my-checksum");

        assertThat(BusinessMetricsUtils.resolveChecksumAlgorithmFeatureIds(algorithm, testRequest))
            .containsExactly(BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32.value());
    }

    @Test
    void when_checksumFeatureIds_areResolvedFromAlgorithmAndHeaders_headerIsUnknown_ignored() {
        testRequest.putHeader("x-amz-checksum-foo", "my-checksum");

        assertThat(BusinessMetricsUtils.resolveChecksumAlgorithmFeatureIds(null, testRequest)).isEmpty();
    }

    private static Stream<Arguments> retryModeMetricInput() {
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

    private static Stream<Arguments> checksumFeatureIdInput() {
        return Arrays.stream(BusinessMetricFeatureId.values())
                     .filter(id -> id.name().startsWith("FLEXIBLE_CHECKSUMS_REQ_")
                                   && !id.name().startsWith("FLEXIBLE_CHECKSUMS_REQ_WHEN"))
                     .map(id -> {
                         String name = id.name();
                         String algorithm = name.substring(23).toLowerCase(Locale.US);
                         // CRC64 is special >_<
                         if ("crc64".equals(algorithm)) {
                             algorithm = "crc64nvme";
                         }
                         return Arguments.of(id, "x-amz-checksum-" + algorithm);
                     });
    }
}
