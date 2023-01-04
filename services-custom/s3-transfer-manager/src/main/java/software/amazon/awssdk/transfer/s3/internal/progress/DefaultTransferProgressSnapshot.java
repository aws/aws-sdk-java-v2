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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;
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

    private final long transferredBytes;
    private final Long totalBytes;
    private final SdkResponse sdkResponse;

    private DefaultTransferProgressSnapshot(Builder builder) {
        if (builder.totalBytes != null) {
            Validate.isNotNegative(builder.totalBytes, "totalBytes");
            Validate.isTrue(builder.transferredBytes <= builder.totalBytes,
                            "transferredBytes (%s) must not be greater than totalBytes (%s)",
                            builder.transferredBytes, builder.totalBytes);
        }
        Validate.paramNotNull(builder.transferredBytes, "byteTransferred");
        this.transferredBytes = Validate.isNotNegative(builder.transferredBytes, "transferredBytes");
        this.totalBytes = builder.totalBytes;
        this.sdkResponse = builder.sdkResponse;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public long transferredBytes() {
        return transferredBytes;
    }

    @Override
    public OptionalLong totalBytes() {
        return totalBytes == null ? OptionalLong.empty() : OptionalLong.of(totalBytes);
    }

    @Override
    public Optional<SdkResponse> sdkResponse() {
        return Optional.ofNullable(sdkResponse);
    }

    @Override
    public OptionalDouble ratioTransferred() {
        if (totalBytes == null) {
            return OptionalDouble.empty();
        }
        return totalBytes == 0 ? OptionalDouble.of(1.0) : OptionalDouble.of(transferredBytes / totalBytes.doubleValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultTransferProgressSnapshot that = (DefaultTransferProgressSnapshot) o;

        if (transferredBytes != that.transferredBytes) {
            return false;
        }
        if (!Objects.equals(totalBytes, that.totalBytes)) {
            return false;
        }
        return Objects.equals(sdkResponse, that.sdkResponse);
    }

    @Override
    public int hashCode() {
        int result = (int) (transferredBytes ^ (transferredBytes >>> 32));
        result = 31 * result + (totalBytes != null ? totalBytes.hashCode() : 0);
        result = 31 * result + (sdkResponse != null ? sdkResponse.hashCode() : 0);
        return result;
    }

    @Override
    public OptionalLong remainingBytes() {
        if (totalBytes == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(totalBytes - transferredBytes);
    }

    @Override
    public String toString() {
        return ToString.builder("TransferProgressSnapshot")
                       .add("transferredBytes", transferredBytes)
                       .add("totalBytes", totalBytes)
                       .add("sdkResponse", sdkResponse)
                       .build();
    }



    public static final class Builder implements CopyableBuilder<Builder, DefaultTransferProgressSnapshot> {
        private Long transferredBytes;
        private Long totalBytes;
        private SdkResponse sdkResponse;

        private Builder() {
        }

        private Builder(DefaultTransferProgressSnapshot snapshot) {
            this.transferredBytes = snapshot.transferredBytes;
            this.totalBytes = snapshot.totalBytes;
            this.sdkResponse = snapshot.sdkResponse;
        }

        public Builder transferredBytes(Long transferredBytes) {
            this.transferredBytes = transferredBytes;
            return this;
        }

        public long getTransferredBytes() {
            return transferredBytes;
        }

        public Builder totalBytes(Long totalBytes) {
            this.totalBytes = totalBytes;
            return this;
        }

        public Long getTotalBytes() {
            return totalBytes;
        }

        public Builder sdkResponse(SdkResponse sdkResponse) {
            this.sdkResponse = sdkResponse;
            return this;
        }

        @Override
        public DefaultTransferProgressSnapshot build() {
            return new DefaultTransferProgressSnapshot(this);
        }
    }
}
