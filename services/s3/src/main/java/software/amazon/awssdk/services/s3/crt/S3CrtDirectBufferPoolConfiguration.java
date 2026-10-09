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
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Configuration for the client-wide direct buffer pool used by the CRT-based S3 client.
 *
 * <p>Downloads that use
 * {@link software.amazon.awssdk.services.s3.S3AsyncResponseTransformer#toBlockingInputStreamWithBorrowedBuffers()} hold
 * pool memory until the application reads it. When several of these downloads share a pool smaller than the number of
 * concurrent downloads multiplied by
 * {@link software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder#initialReadBufferSizeInBytes(Long)}, some downloads
 * can receive no data for a long time while others continue, and the delay grows with object size. To avoid this, size
 * the pool to at least that product. For predictable capacity, use a {@link #fixed(long)} pool at least that large,
 * because an {@link #auto()} pool can use a smaller effective ceiling.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3CrtDirectBufferPoolConfiguration {
    private static final S3CrtDirectBufferPoolConfiguration AUTO =
        new S3CrtDirectBufferPoolConfiguration(null);

    private final Long memoryLimitInBytes;

    private S3CrtDirectBufferPoolConfiguration(Long memoryLimitInBytes) {
        this.memoryLimitInBytes = Validate.isPositiveOrNull(memoryLimitInBytes, "memoryLimitInBytes");
    }

    /**
     * Creates a configuration for a pool whose memory limit is automatically determined by CRT.
     *
     * <p>The pool grows with demand and shrinks when idle. CRT's throughput-based default can be
     * reduced to fit the JVM direct-memory limit. Use {@link #fixed(long)} when a predictable pool
     * capacity is required.</p>
     *
     * @return an automatically sized pool configuration
     */
    public static S3CrtDirectBufferPoolConfiguration auto() {
        return AUTO;
    }

    /**
     * Creates a configuration for a fixed-size pool.
     *
     * @param memoryLimitInBytes maximum number of bytes allocated by the pool
     * @return a fixed-size pool configuration
     * @throws IllegalArgumentException if {@code memoryLimitInBytes} is not positive
     */
    public static S3CrtDirectBufferPoolConfiguration fixed(long memoryLimitInBytes) {
        return new S3CrtDirectBufferPoolConfiguration(memoryLimitInBytes);
    }

    /**
     * Returns the fixed pool memory limit, or {@code null} when automatic sizing is configured.
     *
     * @return the fixed pool memory limit, or {@code null}
     */
    public Long memoryLimitInBytes() {
        return memoryLimitInBytes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        S3CrtDirectBufferPoolConfiguration other = (S3CrtDirectBufferPoolConfiguration) obj;
        return Objects.equals(memoryLimitInBytes, other.memoryLimitInBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(memoryLimitInBytes);
    }

    @Override
    public String toString() {
        return ToString.builder("S3CrtDirectBufferPoolConfiguration")
                       .add("memoryLimitInBytes", memoryLimitInBytes)
                       .build();
    }
}
