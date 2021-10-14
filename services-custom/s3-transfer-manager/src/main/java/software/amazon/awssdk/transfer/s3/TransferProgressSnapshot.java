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

package software.amazon.awssdk.transfer.s3;

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * {@link TransferProgressSnapshot} is an <b>immutable</b>, point-in-time representation of the progress of a given transfer
 * initiated by {@link S3TransferManager}. {@link TransferProgressSnapshot} offers several helpful methods for checking the
 * progress of a transfer, like {@link #totalBytesTransferred()} and {@link #percentageTransferred()}.
 * <p>
 * {@link TransferProgressSnapshot}'s methods that return {@link Optional} are dependent upon the size of a transfer (i.e., the
 * {@code Content-Length}) being known. In the case of file-based {@link Upload}s, transfer sizes are known up front and
 * immediately available. In the case of {@link Download}s, the transfer size is not known until {@link S3TransferManager}
 * receives a {@link GetObjectResponse} from Amazon S3.
 * <p>
 * The recommended way to receive updates of the latest {@link TransferProgressSnapshot} is to implement the {@link
 * TransferListener} interface. See the {@link TransferListener} documentation for usage instructions. A {@link
 * TransferProgressSnapshot} can also be obtained from a stateful {@link TransferProgress}.
 *
 * @see TransferProgress
 * @see TransferListener
 * @see S3TransferManager
 */
@Immutable
@ThreadSafe
@SdkPublicApi
@SdkPreviewApi
public final class TransferProgressSnapshot
    implements ToCopyableBuilder<TransferProgressSnapshot.Builder, TransferProgressSnapshot> {
    
    private final long totalBytesTransferred;
    private final Optional<Long> totalTransferSize;

    private TransferProgressSnapshot(Builder builder) {
        builder.totalTransferSize.ifPresent(size -> {
            Validate.isNotNegative(size, "totalTransferSize");
            Validate.isTrue(builder.totalBytesTransferred <= size,
                            "totalBytesTransferred (%s) must not be greater than totalTransferSize (%s)",
                            builder.totalBytesTransferred, size);
        });
        this.totalBytesTransferred = Validate.isNotNegative(builder.totalBytesTransferred, "totalBytesTransferred");
        this.totalTransferSize = builder.totalTransferSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * The total number of bytes that have been transferred so far.
     */
    public long totalBytesTransferred() {
        return totalBytesTransferred;
    }

    /**
     * The total size of the transfer, or {@link Optional#empty()} if unknown.
     * <p>
     * In the case of file-based {@link Upload}s, transfer sizes are known up front and immediately available. In the case of
     * {@link Download}s, the transfer size is not known until {@link S3TransferManager} receives a {@link GetObjectResponse} from
     * Amazon S3.
     */
    public Optional<Long> totalTransferSize() {
        return totalTransferSize;
    }

    /**
     * The ratio of the {@link #totalTransferSize()} that has been transferred so far, or {@link Optional#empty()} if unknown.
     * This method depends on the {@link #totalTransferSize()} being known in order to return non-empty.
     * <p>
     * Ratio is computed as {@link #totalBytesTransferred()} {@code /} {@link #totalTransferSize()}, where a transfer that is
     * half-complete would return {@code 0.5}.
     *
     * @see #percentageTransferred()
     * @see #totalTransferSize()
     */
    public Optional<Double> ratioTransferred() {
        return totalTransferSize
            .map(Long::doubleValue)
            .map(size -> (size == 0) ? 0 : (totalBytesTransferred / size));
    }

    /**
     * The percentage of the {@link #totalTransferSize()} that has been transferred so far, or {@link Optional#empty()} if
     * unknown. This method depends on the {@link #totalTransferSize()} being known in order to return non-empty.
     * <p>
     * Percentage is computed as the {@link #ratioTransferred()} {@code * 100}, where a transfer that is half-complete would
     * return {@code 50.0}.
     *
     * @see #ratioTransferred()
     * @see #totalTransferSize()
     */
    public Optional<Double> percentageTransferred() {
        return ratioTransferred().map(ratio -> ratio * 100);
    }

    /**
     * The total number of bytes that are remaining to be transferred, or {@link Optional#empty()} if unknown. This method depends
     * on the {@link #totalTransferSize()} being known in order to return non-empty.
     *
     * @see #totalTransferSize()
     */
    public Optional<Long> totalBytesRemaining() {
        return totalTransferSize.map(size -> size - totalBytesTransferred);
    }

    @Override
    public String toString() {
        return ToString.builder("TransferProgressSnapshot")
                       .add("totalBytesTransferred", totalBytesTransferred)
                       .add("totalTransferSize", totalTransferSize)
                       .build();
    }

    @NotThreadSafe
    @SdkPublicApi
    public static final class Builder implements CopyableBuilder<Builder, TransferProgressSnapshot> {
        private long totalBytesTransferred = 0L;
        private Optional<Long> totalTransferSize = Optional.empty();

        private Builder() {
            super();
        }

        private Builder(TransferProgressSnapshot snapshot) {
            this.totalBytesTransferred = snapshot.totalBytesTransferred;
            this.totalTransferSize = snapshot.totalTransferSize;
        }

        public Builder totalBytesTransferred(long totalBytesTransferred) {
            this.totalBytesTransferred = totalBytesTransferred;
            return this;
        }

        public long getTotalBytesTransferred() {
            return totalBytesTransferred;
        }

        public Builder totalTransferSize(long totalTransferSize) {
            this.totalTransferSize = Optional.of(totalTransferSize);
            return this;
        }

        public Optional<Long> getTotalTransferSize() {
            return totalTransferSize;
        }

        @Override
        public TransferProgressSnapshot build() {
            return new TransferProgressSnapshot(this);
        }
    }
}
