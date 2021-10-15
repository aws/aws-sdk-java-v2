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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.Optional;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.TransferProgressSnapshot;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link TransferProgressSnapshot}.
 */
@SdkInternalApi
public final class DefaultTransferProgressSnapshot
    implements ToCopyableBuilder<DefaultTransferProgressSnapshot.Builder, DefaultTransferProgressSnapshot>,
               TransferProgressSnapshot {

    private final long bytesTransferred;
    private final Optional<Long> transferSize;

    private DefaultTransferProgressSnapshot(Builder builder) {
        builder.transferSize.ifPresent(size -> {
            Validate.isNotNegative(size, "transferSize");
            Validate.isTrue(builder.bytesTransferred <= size,
                            "bytesTransferred (%s) must not be greater than transferSize (%s)",
                            builder.bytesTransferred, size);
        });
        this.bytesTransferred = Validate.isNotNegative(builder.bytesTransferred, "bytesTransferred");
        this.transferSize = builder.transferSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public long bytesTransferred() {
        return bytesTransferred;
    }

    @Override
    public Optional<Long> transferSize() {
        return transferSize;
    }

    @Override
    public Optional<Double> ratioTransferred() {
        return transferSize
            .map(Long::doubleValue)
            .map(size -> (size == 0) ? 1.0 : (bytesTransferred / size));
    }

    @Override
    public Optional<Long> bytesRemaining() {
        return transferSize.map(size -> size - bytesTransferred);
    }

    @Override
    public String toString() {
        return ToString.builder("TransferProgressSnapshot")
                       .add("bytesTransferred", bytesTransferred)
                       .add("transferSize", transferSize)
                       .build();
    }

    @NotThreadSafe
    @SdkPublicApi
    public static final class Builder implements CopyableBuilder<Builder, DefaultTransferProgressSnapshot> {
        private long bytesTransferred = 0L;
        private Optional<Long> transferSize = Optional.empty();

        private Builder() {
            super();
        }

        private Builder(DefaultTransferProgressSnapshot snapshot) {
            this.bytesTransferred = snapshot.bytesTransferred;
            this.transferSize = snapshot.transferSize;
        }

        public Builder bytesTransferred(long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
            return this;
        }

        public long bytesTransferred() {
            return bytesTransferred;
        }

        public Builder transferSize(long transferSize) {
            this.transferSize = Optional.of(transferSize);
            return this;
        }

        public Long transferSize() {
            return transferSize.orElse(null);
        }

        @Override
        public DefaultTransferProgressSnapshot build() {
            return new DefaultTransferProgressSnapshot(this);
        }
    }
}
