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

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link ProgressSnapshot}.
 */
@SdkInternalApi
public class DefaultProgressSnapshot
    implements ToCopyableBuilder<DefaultProgressSnapshot.Builder, DefaultProgressSnapshot>,
               ProgressSnapshot  {

    private final long transferredBytes;
    private final Long totalBytes;
    private final Instant startTime;

    public DefaultProgressSnapshot(Builder builder) {
        if (builder.totalBytes != null) {
            Validate.isNotNegative(builder.totalBytes, "totalBytes");
            Validate.isTrue(builder.transferredBytes <= builder.totalBytes,
                            "transferredBytes (%s) must not be greater than totalBytes (%s)",
                            builder.transferredBytes, builder.totalBytes);
        }
        Validate.paramNotNull(builder.transferredBytes, "transferredBytes");
        this.transferredBytes = Validate.isNotNegative(builder.transferredBytes, "transferredBytes");
        this.totalBytes = builder.totalBytes;

        this.startTime = builder.startTime;
    }

    @Override
    public long transferredBytes() {
        return this.transferredBytes;
    }

    @Override
    public Instant startTime() {
        return this.startTime;
    }

    @Override
    public Duration elapsedTime() {
        return Duration.between(this.startTime, Instant.now());
    }

    @Override
    public Optional<Duration> estimatedTimeRemaining() {
        if (!remainingBytes().isPresent()) {
            return Optional.empty();
        }

        long remainingTime = remainingBytes().getAsLong() * elapsedTime().toMillis() / transferredBytes;
        return Optional.of(Duration.ofMillis(remainingTime));

    }

    @Override
    public double averageBytesPer(TimeUnit timeUnit) {
        return this.elapsedTime().equals(Duration.ZERO) ? 1.0 :
               (double) this.transferredBytes / timeUnit.convert(elapsedTime().toMillis(), timeUnit);
    }

    @Override
    public OptionalLong totalBytes() {
        return totalBytes == null ? OptionalLong.empty() : OptionalLong.of(totalBytes);
    }

    @Override
    public OptionalDouble ratioTransferred() {
        if (totalBytes == null) {
            return OptionalDouble.empty();
        }
        return totalBytes == 0 ? OptionalDouble.of(1.0) : OptionalDouble.of(transferredBytes / totalBytes.doubleValue());
    }

    @Override
    public OptionalLong remainingBytes() {
        if (totalBytes == null) {
            return OptionalLong.empty();
        }
        return totalBytes == 0 ? OptionalLong.of(0) : OptionalLong.of(totalBytes - transferredBytes);
    }

    @Override
    public String toString() {
        return ToString.builder("ProgressSnapshot")
                       .add("transferredBytes", transferredBytes)
                       .add("totalBytes", totalBytes)
                       .add("elapsedTime", this.elapsedTime())
                       .build();
    }

    @Override
    public int hashCode() {
        int result = (int) (transferredBytes ^ (transferredBytes >>> 32));
        result = 31 * result + (totalBytes != null ? totalBytes.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultProgressSnapshot that = (DefaultProgressSnapshot) o;

        if (this.transferredBytes != that.transferredBytes) {
            return false;
        }
        if (!Objects.equals(this.totalBytes, that.totalBytes)) {
            return false;
        }
        return Objects.equals(this.startTime, that.startTime);
    }

    @Override
    public DefaultProgressSnapshot.Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, DefaultProgressSnapshot> {
        private long transferredBytes;
        private Long totalBytes;
        private Instant startTime;

        private Builder() {
        }

        private Builder(DefaultProgressSnapshot progressSnapshot) {
            this.transferredBytes = progressSnapshot.transferredBytes;
            this.totalBytes = progressSnapshot.totalBytes;
            this.startTime = progressSnapshot.startTime;
        }

        public Builder transferredBytes(Long transferredBytes) {
            this.transferredBytes = transferredBytes;
            return this;
        }

        public Builder totalBytes(Long totalBytes) {
            this.totalBytes = totalBytes;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        @Override
        public DefaultProgressSnapshot build() {
            return new DefaultProgressSnapshot(this);
        }
    }
}