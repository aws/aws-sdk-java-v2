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

package software.amazon.awssdk.checksums;

import java.nio.ByteBuffer;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.internal.Crc32Checksum;
import software.amazon.awssdk.checksums.internal.Crc64NvmeChecksum;
import software.amazon.awssdk.checksums.internal.CrcChecksumProvider;
import software.amazon.awssdk.checksums.internal.Md5Checksum;
import software.amazon.awssdk.checksums.internal.Sha1Checksum;
import software.amazon.awssdk.checksums.internal.Sha256Checksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;

/**
 * Extension of {@link Checksum} to support checksums and checksum validations used by the SDK that are not provided by the JDK.
 */
@SdkProtectedApi
public interface SdkChecksum extends Checksum {

    /**
     * Returns an {@link SdkChecksum} based on the {@link ChecksumAlgorithm} provided.
     * UnsupportedOperationException will be thrown for unsupported algorithm.
     */
    static SdkChecksum forAlgorithm(ChecksumAlgorithm algorithm) {
        switch (algorithm.algorithmId()) {
            case "CRC32C":
                return CrcChecksumProvider.crc32cImplementation();
            case "CRC32":
                return new Crc32Checksum();
            case "SHA1":
                return new Sha1Checksum();
            case "SHA256":
                return new Sha256Checksum();
            case "MD5":
                return new Md5Checksum();
            case "CRC64NVME":
                return new Crc64NvmeChecksum();
            default:
                throw new UnsupportedOperationException("Unsupported checksum algorithm: " + algorithm);
        }
    }

    /**
     * Returns the computed checksum in a byte array rather than the long provided by {@link #getValue()}.
     *
     * @return byte[] containing the checksum
     */
    byte[] getChecksumBytes();

    /**
     * Allows marking a checksum for checksums that support the ability to mark and reset.
     *
     * @param readLimit the maximum limit of bytes that can be read before the mark position becomes invalid.
     */
    void mark(int readLimit);

    /**
     * Updates the current checksum with the specified array of bytes.
     *
     * @param b the array of bytes to update the checksum with
     * @throws NullPointerException if {@code b} is {@code null}
     */
    default void update(byte[] b) {
        update(b, 0, b.length);
    }

    /**
     * Updates the current checksum with the bytes from the specified buffer.
     * <p>
     * The checksum is updated with the remaining bytes in the buffer, starting at the buffer's position. Upon return, the
     * buffer's position will be updated to its limit; its limit will not have been changed.
     *
     * @param buffer the ByteBuffer to update the checksum with
     * @throws NullPointerException if {@code buffer} is {@code null}
     * @apiNote For best performance with DirectByteBuffer and other ByteBuffer implementations without a backing array
     * implementers of this interface should override this method.
     * @implSpec The default implementation has the following behavior.<br> For ByteBuffers backed by an accessible byte array.
     * <pre>{@code
     * update(buffer.array(),
     *        buffer.position() + buffer.arrayOffset(),
     *        buffer.remaining());
     * }</pre>
     * For ByteBuffers not backed by an accessible byte array.
     * <pre>{@code
     * byte[] b = new byte[Math.min(buffer.remaining(), 4096)];
     * while (buffer.hasRemaining()) {
     *     int length = Math.min(buffer.remaining(), b.length);
     *     buffer.get(b, 0, length);
     *     update(b, 0, length);
     * }
     * }</pre>
     */
    default void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        int rem = limit - pos;
        if (rem <= 0) {
            return;
        }
        if (buffer.hasArray()) {
            update(buffer.array(), pos + buffer.arrayOffset(), rem);
        } else {
            byte[] b = new byte[Math.min(buffer.remaining(), 4096)];
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), b.length);
                buffer.get(b, 0, length);
                update(b, 0, length);
            }
        }
        buffer.position(limit);
    }
}
