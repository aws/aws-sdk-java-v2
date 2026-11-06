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

package software.amazon.awssdk.s3.wiretap.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.internal.Crc32Checksum;

public final class Payload {

    private static final Logger LOG = LogManager.getLogger(Payload.class);

    private static final long SPLITMIX_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long SPLITMIX_MIX1 = 0xBF58476D1CE4E5B9L;
    private static final long SPLITMIX_MIX2 = 0x94D049BB133111EBL;

    private static final int PRINTABLE_LOW = 0x20;
    private static final int PRINTABLE_COUNT = 95;

    private static final long DEFAULT_SEED = 0x50415933_44415441L;

    private static final int STREAM_CHUNK = 64 * 1024;

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final byte[] explicitBytes;
    private final long seed;
    private final int size;

    private Payload(byte[] explicitBytes, long seed, int size) {
        this.explicitBytes = explicitBytes;
        this.seed = seed;
        this.size = size;
    }

    public static Payload of(long seed, int size) {
        requireNonNegative(size);
        return new Payload(null, seed, size);
    }

    public static Payload ofSize(int size) {
        return of(DEFAULT_SEED, size);
    }

    public static Payload of(byte[] bytes) {
        return new Payload(bytes.clone(), 0L, bytes.length);
    }

    public static Payload ofString(String s) {
        return of(s.getBytes(StandardCharsets.UTF_8));
    }

    public int size() {
        return size;
    }

    public byte[] toByteArray() {
        if (explicitBytes != null) {
            return explicitBytes.clone();
        }
        byte[] out = new byte[size];
        fill(out, 0, size, 0);
        return out;
    }

    public byte[] toByteArray(int off, int len) {
        checkRange(off, len);
        byte[] out = new byte[len];
        fill(out, 0, len, off);
        return out;
    }

    public String asString() {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }

    public ByteBuffer toByteBuffer() {
        return ByteBuffer.wrap(toByteArray());
    }

    public InputStream newInputStream() {
        if (explicitBytes != null) {
            return new ByteArrayInputStream(explicitBytes);
        }
        return new GeneratorInputStream();
    }

    public Path writeToTempFile(String prefix) {
        try {
            Path file = Files.createTempFile(prefix, ".dat");
            try (InputStream in = newInputStream()) {
                Files.copy(in, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("could not write payload temp file", e);
        }
    }

    public static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.debug("best-effort cleanup of {} failed", file, e);
        }
    }

    public String crc32Base64() {
        return crc32Base64(0, size);
    }

    public byte[] crc32Bytes() {
        return crc32Bytes(0, size);
    }

    public byte[] sha256() {
        return sha256(0, size);
    }

    public String crc32Base64(int off, int len) {
        return Base64.getEncoder().encodeToString(crc32Bytes(off, len));
    }

    public byte[] crc32Bytes(int off, int len) {
        checkRange(off, len);
        Crc32Checksum checksum = new Crc32Checksum();
        streamRange(off, len, (buf, n) -> checksum.update(buf, 0, n));
        return checksum.getChecksumBytes();
    }

    public byte[] sha256(int off, int len) {
        return digest("SHA-256", off, len);
    }

    public byte[] crc64NvmeBytes() {
        return crc64NvmeBytes(0, size);
    }

    public String crc64NvmeBase64() {
        return crc64NvmeBase64(0, size);
    }

    public byte[] crc64NvmeBytes(int off, int len) {
        checkRange(off, len);
        SdkChecksum checksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC64NVME);
        streamRange(off, len, (buf, n) -> checksum.update(buf, 0, n));
        return checksum.getChecksumBytes();
    }

    public String crc64NvmeBase64(int off, int len) {
        return Base64.getEncoder().encodeToString(crc64NvmeBytes(off, len));
    }

    public byte[] md5Bytes() {
        return md5Bytes(0, size);
    }

    public byte[] md5Bytes(int off, int len) {
        return digest("MD5", off, len);
    }

    public String md5Hex(int off, int len) {
        return hex(md5Bytes(off, len));
    }

    public static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_DIGITS[(b >> 4) & 0xF]).append(HEX_DIGITS[b & 0xF]);
        }
        return sb.toString();
    }

    private byte[] digest(String algorithm, int off, int len) {
        checkRange(off, len);
        MessageDigest digest = newDigest(algorithm);
        streamRange(off, len, (buf, n) -> digest.update(buf, 0, n));
        return digest.digest();
    }

    private byte byteAt(long i) {
        long word = splitmix64(seed + (i >>> 3));
        int raw = (int) ((word >>> ((i & 7) << 3)) & 0xFF);
        return (byte) (PRINTABLE_LOW + Math.floorMod(raw, PRINTABLE_COUNT));
    }

    private void fill(byte[] dst, int dstOff, int len, long srcOff) {
        if (explicitBytes != null) {
            System.arraycopy(explicitBytes, (int) srcOff, dst, dstOff, len);
            return;
        }
        for (int k = 0; k < len; k++) {
            dst[dstOff + k] = byteAt(srcOff + k);
        }
    }

    private void streamRange(int off, int len, ChunkSink sink) {
        byte[] buf = new byte[Math.min(len, STREAM_CHUNK)];
        int remaining = len;
        long pos = off;
        while (remaining > 0) {
            int n = Math.min(remaining, buf.length);
            fill(buf, 0, n, pos);
            sink.accept(buf, n);
            pos += n;
            remaining -= n;
        }
    }

    private void checkRange(int off, int len) {
        if (off < 0 || len < 0 || (long) off + len > size) {
            throw new IndexOutOfBoundsException(
                    "range [" + off + ", " + (off + len) + ") outside payload of size " + size);
        }
    }

    private static long splitmix64(long x) {
        long z = x + SPLITMIX_GAMMA;
        z = (z ^ (z >>> 30)) * SPLITMIX_MIX1;
        z = (z ^ (z >>> 27)) * SPLITMIX_MIX2;
        return z ^ (z >>> 31);
    }

    private static MessageDigest newDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " unavailable", e);
        }
    }

    private static void requireNonNegative(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("payload size must be >= 0, was " + size);
        }
    }

    @FunctionalInterface
    private interface ChunkSink {
        void accept(byte[] buf, int len);
    }

    private final class GeneratorInputStream extends InputStream {
        private long pos;
        private long mark;

        @Override
        public int read() {
            if (pos >= size) {
                return -1;
            }
            return byteAt(pos++) & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= size) {
                return -1;
            }
            int n = (int) Math.min(len, size - pos);
            fill(b, off, n, pos);
            pos += n;
            return n;
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark(int readLimit) {
            mark = pos;
        }

        @Override
        public void reset() {
            pos = mark;
        }
    }
}
