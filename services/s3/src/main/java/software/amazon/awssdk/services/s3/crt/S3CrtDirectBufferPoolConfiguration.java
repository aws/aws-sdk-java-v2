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
 * <p>Multipart uploads, and all downloads except {@code GetObject} requests that set {@code partNumber}, use the pool.
 * Single-part uploads don't. Each request that uses the pool holds at least one full part of pool memory while it runs, so a
 * 1 KiB download still uses an 8 MiB part with the default part size.
 *
 * <p>Downloads that use {@link S3CrtResponseTransformerProvider#toBlockingInputStreamFromDirectBufferPool()} hold pool memory
 * until the application reads or closes the stream, so a slow reader can delay other requests on the client. A starting
 * size is the number of concurrent downloads that use the pool times ({@code initialReadBufferSizeInBytes} plus one part
 * size), plus headroom for the other downloads and multipart uploads on the client. Each download can have one requested
 * part beyond its read window. If the pool is too small, some downloads can stop receiving data until others finish. The
 * SDK does not time out a download that stops after its stream is returned. The application can close the stream to end
 * it.
 *
 * <p>The default {@code initialReadBufferSizeInBytes}, 10 times the part size (80 MiB with the default part size), can
 * limit the throughput of each download on high-bandwidth instances. Raising it can increase throughput per download, but
 * each download can then hold more pool memory.
 *
 * <p>Start with {@link #auto()}, which lets CRT size the pool. Use {@link #elastic(long, long)} to set the ceiling
 * yourself. An explicit ceiling, set through {@link #elastic(long, long)} or through {@code maxNativeMemoryLimitInBytes},
 * must fit within 80% of the JVM direct-memory limit ({@code -XX:MaxDirectMemorySize}), or client creation fails. An
 * {@link #elastic(long, long)} ceiling must also not exceed {@code maxNativeMemoryLimitInBytes} when that is set. For
 * {@link #auto()}, a ceiling set through CRT's {@code AWS_CRT_S3_MEMORY_LIMIT_IN_MB} or
 * {@code AWS_CRT_S3_MEMORY_LIMIT_IN_GIB} environment variable is also explicit. An {@link #auto()} pool without an
 * explicit ceiling derives its ceiling from the throughput target. If that ceiling doesn't fit within the direct-memory
 * limit, it's lowered to fit and CRT logs a warning, instead of failing client creation.
 *
 * <p>With a pool, an explicitly set {@code minimumPartSizeInBytes} is used as the exact upload part size and is never
 * raised. In this case, a known-size upload above the multipart threshold fails before sending data if that part size is
 * below S3's 5 MiB minimum for non-final parts or the object needs more than 10,000 parts. Without an explicit part size,
 * a known-size upload that needs larger parts than the pool can provide fails before sending data. This doesn't apply
 * to uploads from a file path, such as {@code putObject(PutObjectRequest, Path)} or Transfer Manager
 * {@code uploadFile}. For those, CRT can raise the part size as it does without a pool.
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
     * <p>Unless a ceiling is set through {@code maxNativeMemoryLimitInBytes} or CRT's
     * {@code AWS_CRT_S3_MEMORY_LIMIT_IN_MB} or {@code AWS_CRT_S3_MEMORY_LIMIT_IN_GIB} environment variable, CRT derives
     * the ceiling from the client's throughput target. The pool allocates 16 parts (128 MiB with the default part size)
     * when the client is created, or the whole pool if the ceiling is smaller, and keeps them while idle. Above that, it
     * grows with demand and releases unused memory when the client is idle. The derived ceiling can be reduced to fit the
     * JVM direct-memory limit. Use {@link #elastic(long, long)} for a known number of concurrent downloads that use the
     * pool and need an explicit ceiling.
     *
     * @return an automatically sized pool configuration
     */
    public static S3CrtDirectBufferPoolConfiguration auto() {
        return new S3CrtDirectBufferPoolConfiguration(null, null);
    }

    /**
     * Creates a configuration for a pool that grows on demand to its ceiling and releases unused capacity above its
     * floor when the client is idle.
     *
     * <p>The pool keeps its floor allocated from client creation, even while idle. The floor is rounded down to blocks of
     * 16 parts (128 MiB with the default part size). A floor below one block keeps no memory allocated unless it reaches
     * the ceiling, in which case the pool keeps its whole capacity allocated. The ceiling is {@code memoryLimitInBytes}
     * rounded down to whole parts.
     *
     * @param minimumSizeInBytes requested pool floor in bytes
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
     * Returns the configured elastic pool ceiling.
     *
     * @return the elastic pool ceiling, or an empty optional when the pool is automatic
     */
    public Optional<Long> memoryLimitInBytes() {
        return Optional.ofNullable(memoryLimitInBytes);
    }

    /**
     * Returns the configured elastic pool floor.
     *
     * @return the elastic pool floor, or an empty optional when the pool is automatic
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
