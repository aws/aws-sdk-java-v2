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

    private final long bytesTransferred;
    private final Long transferSizeInBytes;
    private final SdkResponse sdkResponse;

    private DefaultTransferProgressSnapshot(Builder builder) {
        if (builder.transferSizeInBytes != null) {
            Validate.isNotNegative(builder.transferSizeInBytes, "transferSizeInBytes");
            Validate.isTrue(builder.bytesTransferred <= builder.transferSizeInBytes,
                            "bytesTransferred (%s) must not be greater than transferSizeInBytes (%s)",
                            builder.bytesTransferred, builder.transferSizeInBytes);
        }
        Validate.paramNotNull(builder.bytesTransferred, "byteTransferred");
        this.bytesTransferred = Validate.isNotNegative(builder.bytesTransferred, "bytesTransferred");
        this.transferSizeInBytes = builder.transferSizeInBytes;
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
    public long bytesTransferred() {
        return bytesTransferred;
    }

    @Override
    public Optional<Long> transferSizeInBytes() {
        return Optional.ofNullable(transferSizeInBytes);
    }

    @Override
    public Optional<SdkResponse> sdkResponse() {
        return Optional.ofNullable(sdkResponse);
    }

    @Override
    public Optional<Double> ratioTransferred() {
        return transferSizeInBytes()
            .map(Long::doubleValue)
            .map(size -> (size == 0) ? 1.0 : (bytesTransferred / size));
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

        if (bytesTransferred != that.bytesTransferred) {
            return false;
        }
        if (!Objects.equals(transferSizeInBytes, that.transferSizeInBytes)) {
            return false;
        }
        return Objects.equals(sdkResponse, that.sdkResponse);
    }

    @Override
    public int hashCode() {
        int result = (int) (bytesTransferred ^ (bytesTransferred >>> 32));
        result = 31 * result + (transferSizeInBytes != null ? transferSizeInBytes.hashCode() : 0);
        result = 31 * result + (sdkResponse != null ? sdkResponse.hashCode() : 0);
        return result;
    }

    @Override
    public Optional<Long> bytesRemaining() {
        return transferSizeInBytes().map(size -> size - bytesTransferred);
    }

    @Override
    public String toString() {
        return ToString.builder("TransferProgressSnapshot")
                       .add("bytesTransferred", bytesTransferred)
                       .add("transferSizeInBytes", transferSizeInBytes)
                       .add("sdkResponse", sdkResponse)
                       .build();
    }



    public static final class Builder implements CopyableBuilder<Builder, DefaultTransferProgressSnapshot> {
        private Long bytesTransferred;
        private Long transferSizeInBytes;
        private SdkResponse sdkResponse;

        private Builder() {
        }

        private Builder(DefaultTransferProgressSnapshot snapshot) {
            this.bytesTransferred = snapshot.bytesTransferred;
            this.transferSizeInBytes = snapshot.transferSizeInBytes;
            this.sdkResponse = snapshot.sdkResponse;
        }

        public Builder bytesTransferred(Long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
            return this;
        }

        public long getBytesTransferred() {
            return bytesTransferred;
        }

        public Builder transferSizeInBytes(Long transferSizeInBytes) {
            this.transferSizeInBytes = transferSizeInBytes;
            return this;
        }

        public Long getTransferSizeInBytes() {
            return transferSizeInBytes;
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
