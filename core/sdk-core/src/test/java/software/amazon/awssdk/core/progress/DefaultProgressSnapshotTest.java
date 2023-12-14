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

package software.amazon.awssdk.core.progress;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.progress.snapshot.DefaultProgressSnapshot;

public class DefaultProgressSnapshotTest {
    @Test
    public void bytesTransferred_greaterThan_totalBytes_shouldThrow() {
        DefaultProgressSnapshot.Builder builder = DefaultProgressSnapshot.builder()
                                                                         .transferredBytes(2L)
                                                                         .totalBytes(1L);
        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("transferredBytes (2) must not be greater than totalBytes (1)");
    }

    @Test
    public void transferredBytes_negative_shouldThrow() {
        DefaultProgressSnapshot.Builder builder = DefaultProgressSnapshot.builder()
                                                                         .transferredBytes(-2L);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("transferredBytes must not be negative");
    }

    @Test
    public void transferredBytes_null_isZero() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .build();

        Assertions.assertEquals(0, snapshot.transferredBytes());
    }

    @Test
    public void totalBytes_negative_shouldThrow() {
        DefaultProgressSnapshot.Builder builder = DefaultProgressSnapshot.builder()
                                                                         .transferredBytes(2L)
                                                                         .totalBytes(-2L);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("totalBytes must not be negative");
    }

    @Test
    public void totalBytes_empty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(2L)
                                                                  .build();

        assertThat(snapshot.totalBytes()).isNotPresent();
    }

    @Test
    public void totalBytes() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(2L)
                                                                  .totalBytes(5L)
                                                                  .build();

        Assertions.assertEquals(5, snapshot.totalBytes().getAsLong());
    }

    @Test
    public void startTime_after_currentTime_shouldThrow() {
        Instant timeAfterFiveSeconds = Instant.now().plus(5, ChronoUnit.SECONDS);
        DefaultProgressSnapshot.Builder builder = DefaultProgressSnapshot.builder()
                                                                         .transferredBytes(0L)
                                                                         .startTime(timeAfterFiveSeconds);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("currentTime")
            .hasMessageEndingWith(" must not be before startTime (" + timeAfterFiveSeconds + ")");
    }

    @Test
    public void ratioTransferred_withoutTotalBytes_isEmpty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .build();
        assertThat(snapshot.ratioTransferred()).isNotPresent();
    }

    @Test
    public void ratioTransferred() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .totalBytes(5L)
                                                                  .build();
        assertEquals(0.2, snapshot.ratioTransferred().getAsDouble(), 0.0);
    }

    @Test
    public void remainingBytes_withoutTotalBytes_isEmpty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .build();
        assertThat(snapshot.remainingBytes()).isNotPresent();
    }

    @Test
    public void remainingBytes() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .totalBytes(5L)
                                                                  .build();
        Assertions.assertEquals(4.0, snapshot.remainingBytes().getAsLong(), 0.0);
    }

    @Test
    public void elapsedTime_withoutStartTime_isEmpty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .build();
        assertThat(snapshot.elapsedTime()).isNotPresent();
    }

    @Test
    public void elapsedTime() {

        Instant startTime = Instant.now().minusMillis(100);
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .startTime(startTime)
                                                                  .build();
        Duration expectedDuration = Duration.between(startTime, Instant.now());

        Assertions.assertEquals(snapshot.elapsedTime().get().toMillis(), expectedDuration.toMillis(), 0.1);
    }

    @Test
    public void averageBytesPer_withoutStartTime_isEmpty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .totalBytes(5L)
                                                                  .build();
        assertThat(snapshot.averageBytesPer(TimeUnit.MILLISECONDS)).isNotPresent();
    }

    @Test
    public void averageBytesPer() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(100L)
                                                                  .startTime(Instant.now().minusMillis(100))
                                                                  .build();
        Assertions.assertEquals(1.0, snapshot.averageBytesPer(TimeUnit.MILLISECONDS).getAsDouble(), 0.2);
    }

    @Test
    public void estimatedTimeRemaining_withoutStartTime_isEmpty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .totalBytes(5L)
                                                                  .build();
        assertThat(snapshot.estimatedTimeRemaining()).isNotPresent();
    }

    @Test
    public void estimatedTimeRemaining_withoutTotalBytes_isEmpty() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(1L)
                                                                  .startTime(Instant.now().minusMillis(5))
                                                                  .build();
        assertThat(snapshot.estimatedTimeRemaining()).isNotPresent();
    }

    @Test
    public void estimatedTimeRemaining() {
        DefaultProgressSnapshot snapshot = DefaultProgressSnapshot.builder()
                                                                  .transferredBytes(100L)
                                                                  .startTime(Instant.now().minusSeconds(1))
                                                                  .totalBytes(500L)
                                                                  .build();
        Assertions.assertEquals(4000.0, snapshot.estimatedTimeRemaining().get().toMillis(), 10.0);
    }
}
