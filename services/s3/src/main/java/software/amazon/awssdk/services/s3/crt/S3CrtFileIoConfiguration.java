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

package software.amazon.awssdk.services.s3.crt;

import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options which controls how client performs file I/O operations. Only applies to file-based workloads.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3CrtFileIoConfiguration
    implements ToCopyableBuilder<S3CrtFileIoConfiguration.Builder, S3CrtFileIoConfiguration> {
    private final Boolean shouldStream;
    private final Double diskThroughputGbps;
    private final Boolean directIo;

    private S3CrtFileIoConfiguration(DefaultBuilder builder) {
        this.shouldStream = builder.shouldStream;
        this.diskThroughputGbps = builder.diskThroughputGbps;
        this.directIo = builder.directIo;
    }

    /**
     * Creates a default builder for {@link S3CrtFileIoConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Skip buffering the part in memory before sending the request.
     * If set, set the {@code diskThroughputGbps} to reasonably align with the available disk throughput.
     * Otherwise, the transfer may fail with connection starvation.
     * Defaults to false.
     *
     * @return if client should skip buffering in memory before sending the request.
     */
    public Boolean shouldStream() {
        return shouldStream;
    }

    /**
     * The estimated disk throughput in gigabits per second (Gbps).
     * Only applied when {@code shouldStream} is true.
     *
     * When doing upload with streaming, it's important to set the disk throughput to prevent connection starvation.
     * Note: There are possibilities that cannot reach all available disk throughput:
     * 1. Disk is busy with other applications
     * 2. OS Cache may cap the throughput, use {@code directIo} to get around this.
     *
     * @return disk throughput value in Gpbs.
     */
    public Double diskThroughputGbps() {
        return diskThroughputGbps;
    }

    /**
     * Enable direct I/O to bypass the OS cache. Helpful when the disk I/O outperforms the kernel cache.
     * Notes:
     * - Only supported on Linux for now.
     * - Only supports upload for now.
     * - Uses it as a potentially powerful tool that should be used with caution. Read NOTES for O_DIRECT
     *   for additional info <a href="https://man7.org/linux/man-pages/man2/openat.2.html">open</a>
     *
     * @return directIO value
     */
    public Boolean directIo() {
        return directIo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3CrtFileIoConfiguration that = (S3CrtFileIoConfiguration) o;

        if (!Objects.equals(shouldStream, that.shouldStream)) {
            return false;
        }
        if (!Objects.equals(diskThroughputGbps, that.diskThroughputGbps)) {
            return false;
        }
        return Objects.equals(directIo, that.directIo);
    }

    @Override
    public int hashCode() {
        int result = shouldStream != null ? shouldStream.hashCode() : 0;
        result = 31 * result + (diskThroughputGbps != null ? diskThroughputGbps.hashCode() : 0);
        result = 31 * result + (directIo != null ? directIo.hashCode() : 0);
        return result;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<Builder, S3CrtFileIoConfiguration> {
        /**
         * Skip buffering the part in memory before sending the request.
         * If set, set the {@code diskThroughputGbps} to reasonably align with the available disk throughput.
         * Otherwise, the transfer may fail with connection starvation.
         * Defaults to false.
         *
         * @param shouldStream whether to stream the file
         * @return The builder for method chaining.
         */
        Builder shouldStream(Boolean shouldStream);

        /**
         * The estimated disk throughput in gigabits per second (Gbps).
         * Only applied when {@code shouldStream} is true.
         *
         * When doing upload with streaming, it's important to set the disk throughput to prevent connection starvation.
         * Note: There are possibilities that cannot reach all available disk throughput:
         * 1. Disk is busy with other applications
         * 2. OS Cache may cap the throughput, use {@code directIo} to get around this.
         *
         * @param diskThroughputGbps the disk throughput in Gbps
         * @return The builder for method chaining.
         */
        Builder diskThroughputGbps(Double diskThroughputGbps);

        /**
         * Enable direct I/O to bypass the OS cache. Helpful when the disk I/O outperforms the kernel cache.
         * Notes:
         * - Only supported on Linux for now.
         * - Only supports upload for now.
         * - Uses it as a potentially powerful tool that should be used with caution. Read NOTES for O_DIRECT
         *   for additional info https://man7.org/linux/man-pages/man2/openat.2.html
         *
         * @param directIo whether to enable direct I/O
         * @return The builder for method chaining.
         */
        Builder directIo(Boolean directIo);

        @Override
        S3CrtFileIoConfiguration build();
    }

    private static final class DefaultBuilder implements Builder {
        private Boolean shouldStream;
        private Double diskThroughputGbps;
        private Boolean directIo;

        private DefaultBuilder() {
        }

        private DefaultBuilder(S3CrtFileIoConfiguration fileIoOptions) {
            this.shouldStream = fileIoOptions.shouldStream;
            this.diskThroughputGbps = fileIoOptions.diskThroughputGbps;
            this.directIo = fileIoOptions.directIo;
        }

        @Override
        public Builder shouldStream(Boolean shouldStream) {
            this.shouldStream = shouldStream;
            return this;
        }

        @Override
        public Builder diskThroughputGbps(Double diskThroughputGbps) {
            this.diskThroughputGbps = diskThroughputGbps;
            return this;
        }

        @Override
        public Builder directIo(Boolean directIo) {
            this.directIo = directIo;
            return this;
        }

        @Override
        public S3CrtFileIoConfiguration build() {
            return new S3CrtFileIoConfiguration(this);
        }
    }
}
