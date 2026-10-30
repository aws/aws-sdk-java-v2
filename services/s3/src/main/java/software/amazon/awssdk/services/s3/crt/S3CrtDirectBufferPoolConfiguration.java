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
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Configuration for the client-wide direct buffer pool used by the CRT-based S3 client.
 *
 * <p>Multipart uploads, and all downloads except {@code GetObject} requests that set a part number, use the pool.
 * Single-part uploads don't. Each request that uses the pool holds at least one full part of pool memory while it runs, so a
 * 1 KiB download still uses an 8 MiB part with the default part size.
 *
 * <p>Downloads that use {@link S3CrtResponseTransformerFactory#toBlockingInputStreamFromDirectBufferPool()} hold pool memory
 * until the application reads or closes the stream, so a slow reader can delay other requests on the client. A starting
 * size is the number of concurrent downloads that use the pool times ({@code initialReadBufferSizeInBytes} plus one part
 * size), plus headroom for the other downloads and multipart uploads on the client. Each download can have one requested
 * part beyond its read window. If the pool is too small, some downloads can stop receiving data until others finish. The
 * SDK does not time out a download that stops after its stream is returned. The application can close the stream to end
 * it.
 *
 * <p>The default {@code initialReadBufferSizeInBytes} of 80 MiB can limit the throughput of each download on
 * high-bandwidth instances. Raising it can increase throughput per download, but each download can then hold more pool
 * memory.
 *
 * <p>Use {@link #fixed(long)} or {@link #elastic(long, long)} for a predictable ceiling. An explicit ceiling, set through
 * either of those or through {@code maxNativeMemoryLimitInBytes}, must fit within 80% of the JVM direct-memory limit
 * ({@code -XX:MaxDirectMemorySize}), or client creation fails. A {@link #fixed(long)} or {@link #elastic(long, long)}
 * ceiling must also not exceed {@code maxNativeMemoryLimitInBytes} when that is set. An {@link #auto()} pool without
 * {@code maxNativeMemoryLimitInBytes} derives its ceiling from the throughput target. If that ceiling doesn't fit within
 * the direct-memory limit, it's lowered to fit instead of failing client creation.
 *
 * <p>With a pool, an explicitly set {@code minimumPartSizeInBytes} is used as the exact upload part size and is never
 * raised. In this case, a known-size upload above the multipart threshold fails before sending data if that part size is
 * below S3's 5 MiB minimum for non-final parts or the object needs more than 10,000 parts.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3CrtDirectBufferPoolConfiguration {
    private final Long minimumSizeInBytes;
    private final Long memoryLimitInBytes;

    private S3CrtDirectBufferPoolConfiguration(Long minimumSizeInBytes, Long memoryLimitInBytes) {
        this.minimumSizeInBytes = minimumSizeInBytes;
        this.memoryLimitInBytes = memoryLimitInBytes;
    }

    /**
     * Creates a configuration for a pool whose memory limit is automatically determined by CRT.
     *
     * <p>CRT sizes the pool from system information and the client's throughput target. The pool grows with demand and
     * releases memory it is not using. The derived ceiling can be reduced to fit the JVM direct-memory limit. Use
     * {@link #elastic(long, long)} for a known number of concurrent downloads that use the pool and need an explicit
     * ceiling.
     *
     * @return an automatically sized pool configuration
     */
    public static S3CrtDirectBufferPoolConfiguration auto() {
        return new S3CrtDirectBufferPoolConfiguration(null, null);
    }

    /**
     * Creates a configuration for a fixed-size pool.
     *
     * <p>CRT rounds {@code memoryLimitInBytes} down to whole parts, allocates the resulting capacity when the client is
     * created, and never grows or shrinks it. When
     * {@link software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder#minimumPartSizeInBytes(Long)} is not set, a
     * fixed pool limits a known-size multipart upload to 312.5 GiB, or less for a pool smaller than 32 MiB. A larger
     * upload fails before sending data. Use {@link #elastic(long, long)} for larger uploads.
     *
     * @param memoryLimitInBytes requested pool ceiling in bytes, rounded down to whole parts by CRT
     * @return a fixed-size pool configuration
     * @throws IllegalArgumentException if {@code memoryLimitInBytes} is not positive
     */
    public static S3CrtDirectBufferPoolConfiguration fixed(long memoryLimitInBytes) {
        Validate.isPositive(memoryLimitInBytes, "memoryLimitInBytes");
        return new S3CrtDirectBufferPoolConfiguration(null, memoryLimitInBytes);
    }

    /**
     * Creates a configuration for a pool that grows on demand to its ceiling and releases unused capacity above its
     * floor.
     *
     * <p>The pool allocates its floor when the client is created and keeps it while idle. The pool grows in blocks of 16
     * parts, so the floor is {@code minimumSizeInBytes} rounded down to a multiple of 16 parts, or the whole pool if it
     * reaches the ceiling. The ceiling is {@code memoryLimitInBytes} rounded down to whole parts.
     *
     * @param minimumSizeInBytes requested elastic floor in bytes
     * @param memoryLimitInBytes requested pool ceiling in bytes
     * @return an elastic pool configuration
     * @throws IllegalArgumentException if {@code minimumSizeInBytes} is negative, {@code memoryLimitInBytes} is not
     *                                  positive, or {@code minimumSizeInBytes} exceeds {@code memoryLimitInBytes}
     */
    public static S3CrtDirectBufferPoolConfiguration elastic(long minimumSizeInBytes, long memoryLimitInBytes) {
        Validate.isNotNegative(minimumSizeInBytes, "minimumSizeInBytes");
        Validate.isPositive(memoryLimitInBytes, "memoryLimitInBytes");
        Validate.isTrue(minimumSizeInBytes <= memoryLimitInBytes,
                        "minimumSizeInBytes must not exceed memoryLimitInBytes");
        return new S3CrtDirectBufferPoolConfiguration(minimumSizeInBytes, memoryLimitInBytes);
    }

    /**
     * Returns the configured pool ceiling.
     *
     * @return the pool ceiling, or an empty optional when automatic sizing is configured
     */
    public Optional<Long> memoryLimitInBytes() {
        return Optional.ofNullable(memoryLimitInBytes);
    }

    /**
     * Returns the configured elastic pool floor.
     *
     * @return the elastic pool floor, or an empty optional when the pool is automatic or fixed
     */
    public Optional<Long> minimumSizeInBytes() {
        return Optional.ofNullable(minimumSizeInBytes);
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
        return Objects.equals(minimumSizeInBytes, other.minimumSizeInBytes)
               && Objects.equals(memoryLimitInBytes, other.memoryLimitInBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(minimumSizeInBytes);
        return 31 * result + Objects.hashCode(memoryLimitInBytes);
    }

    @Override
    public String toString() {
        return ToString.builder("S3CrtDirectBufferPoolConfiguration")
                       .add("minimumSizeInBytes", minimumSizeInBytes)
                       .add("memoryLimitInBytes", memoryLimitInBytes)
                       .build();
    }
}
