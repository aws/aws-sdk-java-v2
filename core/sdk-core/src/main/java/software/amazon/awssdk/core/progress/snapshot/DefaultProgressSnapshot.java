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

package software.amazon.awssdk.core.progress.snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
    private final Optional<Instant> startTime;

    public DefaultProgressSnapshot(Builder builder) {
        if (builder.totalBytes != null) {
            Validate.isNotNegative(builder.totalBytes, "totalBytes");
            Validate.isTrue(builder.transferredBytes <= builder.totalBytes,
                            "transferredBytes (%s) must not be greater than totalBytes (%s)",
                            builder.transferredBytes, builder.totalBytes);
        }
        Validate.paramNotNull(builder.transferredBytes, "byteTransferred");
        this.transferredBytes = Validate.isNotNegative(builder.transferredBytes, "transferredBytes");
        this.totalBytes = builder.totalBytes;

        if (builder.startTime.isPresent()) {
            Instant currentTime = Instant.now();
            Validate.isTrue(currentTime.isAfter(builder.startTime.get()),
                            "currentTime (%s) must not be before startTime (%s)",
                            currentTime, builder.startTime.get());
        }

        this.startTime = builder.startTime;
    }

    @Override
    public long transferredBytes() {
        return this.transferredBytes;
    }

    @Override
    public Optional<Instant> startTime() {
        return this.startTime;
    }

    @Override
    public Optional<Duration> elapsedTime() {
        return this.startTime.isPresent() ? Optional.of(Duration.between(startTime.get(), Instant.now())) : Optional.empty();
    }

    @Override
    public Optional<Duration> estimatedTimeRemaining() {
        if (!elapsedTime().isPresent() || !remainingBytes().isPresent()) {
            return Optional.empty();
        }

        long remainingTime = remainingBytes().getAsLong() * elapsedTime().get().toMillis() / transferredBytes;
        return Optional.of(Duration.ofMillis(remainingTime));

    }

    @Override
    public OptionalDouble averageBytesPer(TimeUnit timeUnit) {
        if (!this.elapsedTime().isPresent()) {
            return OptionalDouble.empty();
        }

        return this.elapsedTime().get().equals(Duration.ZERO) ? OptionalDouble.of(1.0) :
               OptionalDouble.of((double) this.transferredBytes / timeUnit.convert(elapsedTime().get().toMillis(), timeUnit));
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
    public DefaultProgressSnapshot.Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, DefaultProgressSnapshot> {
        private long transferredBytes;
        private Long totalBytes;
        private Optional<Instant> startTime = Optional.empty();

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

        public Long getTransferredBytes() {
            return this.transferredBytes;
        }

        public Builder totalBytes(Long totalBytes) {
            this.totalBytes = totalBytes;
            return this;
        }

        public Long getTotalBytes() {
            return this.totalBytes;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = Optional.of(startTime);
            return this;
        }

        public Optional<Instant> startTime() {
            return this.startTime;
        }

        @Override
        public DefaultProgressSnapshot build() {
            return new DefaultProgressSnapshot(this);
        }
    }
}