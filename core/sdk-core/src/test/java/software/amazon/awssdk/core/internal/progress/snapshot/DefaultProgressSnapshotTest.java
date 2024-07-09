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

package software.amazon.awssdk.core.internal.progress.snapshot;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefaultProgressSnapshotTest {

    private static Stream<Arguments> getArgumentsForInvalidParameterValidationTests() {
        return Stream.of(Arguments.of("transferredBytes (2) must not be greater than totalBytes (1)",
                                      DefaultProgressSnapshot.builder()
                                                             .transferredBytes(2L)
                                                             .totalBytes(1L),
                                      new IllegalArgumentException()),
                         Arguments.of("transferredBytes must not be negative",
                                      DefaultProgressSnapshot.builder()
                                                             .transferredBytes(-2L),
                                      new IllegalArgumentException()),
                         Arguments.of("totalBytes must not be negative",
                                      DefaultProgressSnapshot.builder()
                                                             .transferredBytes(2L)
                                                             .totalBytes(-2L),
                                      new IllegalArgumentException()));
    }

    private static Stream<Arguments> getArgumentsForMissingParameterValidationTests() {

        DefaultProgressSnapshot snapshotNoTotalBytes = DefaultProgressSnapshot.builder()
                                                                              .transferredBytes(2L)
                                                                              .startTime(Instant.now())
                                                                              .build();

        DefaultProgressSnapshot snapshotRatioTransferredWithoutTotalBytesIsEmpty = DefaultProgressSnapshot.builder()
                                                                                                          .transferredBytes(1L)
                                                                                                          .startTime(Instant.now())
                                                                                                          .build();

        DefaultProgressSnapshot snapshotRemainingBytesWithoutTotalBytesIsEmpty = DefaultProgressSnapshot.builder()
                                                                                                        .transferredBytes(1L)
                                                                                                        .startTime(Instant.now())
                                                                                                        .build();

        DefaultProgressSnapshot snapshotEstimatedTimeRemainingWithoutTotalBytesIsEmpty = DefaultProgressSnapshot.builder()
                                                                                                                .transferredBytes(1L)
                                                                                                                .startTime(Instant.now())
                                                                                                                .build();

        return Stream.of(Arguments.of(snapshotNoTotalBytes.totalBytes().isPresent()),
                         Arguments.of(snapshotRatioTransferredWithoutTotalBytesIsEmpty.ratioTransferred().isPresent()),
                         Arguments.of(snapshotRemainingBytesWithoutTotalBytesIsEmpty.remainingBytes().isPresent()),
                         Arguments.of(snapshotEstimatedTimeRemainingWithoutTotalBytesIsEmpty.estimatedTimeRemaining().isPresent()));
    }

    private static Stream<Arguments> getArgumentsForTimeTest() {

        DefaultProgressSnapshot snapshotEstimatedTimeReamining = DefaultProgressSnapshot.builder()
                                                                                        .transferredBytes(100L)
                                                                                        .startTime(Instant.now().minusSeconds(1))
                                                                                        .totalBytes(500L)
                                                                                        .build();

        Instant startTime = Instant.now().minusMillis(100);
        DefaultProgressSnapshot snapshotTimeElapsed = DefaultProgressSnapshot.builder()
                                                                             .transferredBytes(1L)
                                                                             .startTime(startTime)
                                                                             .build();
        Duration expectedDuration = Duration.between(startTime, Instant.now());

        return Stream.of(Arguments.of(4000L, snapshotEstimatedTimeReamining.estimatedTimeRemaining().get().toMillis()
                             , 10L),
                         Arguments.of(snapshotTimeElapsed.elapsedTime().toMillis(), expectedDuration.toMillis(), 1L));
    }

    private static Stream<Arguments> getArgumentsForBytesTest() {

        DefaultProgressSnapshot snapshotBytes = DefaultProgressSnapshot.builder()
                                                                            .transferredBytes(2L)
                                                                            .totalBytes(5L)
                                                                            .startTime(Instant.now())
                                                                            .build();

        return Stream.of(Arguments.of(5L, snapshotBytes.totalBytes().getAsLong()),
                         Arguments.of(3L, snapshotBytes.remainingBytes().getAsLong(), 3L));
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForInvalidParameterValidationTests")
    void test_invalid_arguments_shouldThrow(String expectedErrorMsg, DefaultProgressSnapshot.Builder builder,
                                                   Exception e) {
        assertThatThrownBy(builder::build)
            .isInstanceOf(e.getClass())
            .hasMessage(expectedErrorMsg);
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForMissingParameterValidationTests")
    void test_missing_params_shouldReturnEmpty(boolean condition) {
        Assertions.assertFalse(condition);
    }

    @Test
    void ratioTransferred() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .totalBytes(5L)
                                                                  .startTime(Instant.now())
                                                                  .build();
        assertEquals(0.2, snapshot.ratioTransferred().getAsDouble(), 0.0);
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForBytesTest")
    void test_estimatedBytesRemaining_and_totalBytes(long expectedBytes, long actualBytes) {
        Assertions.assertEquals(expectedBytes, actualBytes);
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForTimeTest")
    void test_elapsedTime_and_estimatedTimeRemaining(long expected, long timeInMillis, long delta) {
        Assertions.assertEquals(expected, timeInMillis, delta);
    }

    @Test
    void averageBytesPer() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(100L)
                                                                  .startTime(Instant.now().minusMillis(100))
                                                                  .build();
        Assertions.assertEquals(1.0, snapshot.averageBytesPer(TimeUnit.MILLISECONDS), 0.2);
    }
}
