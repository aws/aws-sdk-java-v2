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

package software.amazon.awssdk.s3.mockserver.server.helpers.get;

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.utils.StringUtils;

public final class StoredObject {

    public static final int DEFAULT_SIZE = 18 * 1024 * 1024;
    public static final int DEFAULT_PART_SIZE = 8 * 1024 * 1024;

    private static final String SINGLE_PUT_CONTENT_TYPE = "application/octet-stream";
    private static final String MULTIPART_CONTENT_TYPE = "binary/octet-stream";

    public enum ChecksumType {
        FULL_OBJECT,
        COMPOSITE
    }

    public enum Algorithm {
        CRC32("x-amz-checksum-crc32"),
        CRC64NVME("x-amz-checksum-crc64nvme");

        private final String headerName;

        Algorithm(String headerName) {
            this.headerName = headerName;
        }

        public String headerName() {
            return headerName;
        }
    }

    private final Payload payload;
    private final int partSize;
    private final boolean multipart;
    private final Algorithm algorithm;
    private final ChecksumType checksumType;
    private final String contentType;

    private final String eTag;
    private final String storedChecksum;

    private final String versionId;

    private final Map<String, String> tags;

    private final Map<String, String> userMetadata;

    private StoredObject(Payload payload, int partSize, boolean multipart,
                         Algorithm algorithm, ChecksumType checksumType) {
        if (multipart && partSize <= 0) {
            throw new IllegalArgumentException("a multipart object needs a positive part size");
        }
        this.payload = payload;
        this.partSize = partSize;
        this.multipart = multipart;
        this.algorithm = algorithm;
        this.checksumType = checksumType;
        this.contentType = multipart ? MULTIPART_CONTENT_TYPE : SINGLE_PUT_CONTENT_TYPE;
        this.eTag = computeETag();
        this.storedChecksum = computeStoredChecksum();
        this.versionId = null;
        this.tags = Immutable.mapOf();
        this.userMetadata = Immutable.mapOf();
    }

    private StoredObject(StoredObject source, String versionId, Map<String, String> tags,
                         Map<String, String> userMetadata) {
        this.payload = source.payload;
        this.partSize = source.partSize;
        this.multipart = source.multipart;
        this.algorithm = source.algorithm;
        this.checksumType = source.checksumType;
        this.contentType = source.contentType;
        this.eTag = source.eTag;
        this.storedChecksum = source.storedChecksum;
        this.versionId = versionId;
        this.tags = Collections.unmodifiableMap(new LinkedHashMap<>(tags));
        this.userMetadata = Collections.unmodifiableMap(new LinkedHashMap<>(userMetadata));
    }

    public Payload payload() {
        return payload;
    }

    public int partSize() {
        return partSize;
    }

    public boolean multipart() {
        return multipart;
    }

    public Algorithm algorithm() {
        return algorithm;
    }

    public ChecksumType checksumType() {
        return checksumType;
    }

    public String contentType() {
        return contentType;
    }

    public String eTag() {
        return eTag;
    }

    public String storedChecksum() {
        return storedChecksum;
    }

    public String versionId() {
        return versionId;
    }

    public Map<String, String> tags() {
        return tags;
    }

    public Map<String, String> userMetadata() {
        return userMetadata;
    }

    public StoredObject withVersionId(String versionId) {
        if (StringUtils.isBlank(versionId)) {
            throw new IllegalArgumentException("a version id must be a non-blank value; use the "
                    + "object as-is to model an unversioned bucket");
        }
        return new StoredObject(this, versionId, tags, userMetadata);
    }

    public StoredObject withTags(Map<String, String> tags) {
        return new StoredObject(this, versionId, tags, userMetadata);
    }

    public StoredObject withMetadata(Map<String, String> metadata) {
        return new StoredObject(this, versionId, tags, metadata);
    }

    public static StoredObject singlePutCrc32() {
        return singlePutCrc32(DEFAULT_SIZE);
    }

    public static StoredObject singlePutCrc32(int size) {
        return new StoredObject(Payload.ofSize(size), 0, false,
                Algorithm.CRC32, ChecksumType.FULL_OBJECT);
    }

    public static StoredObject multipartCrc32Composite() {
        return multipartCrc32Composite(DEFAULT_SIZE, DEFAULT_PART_SIZE);
    }

    public static StoredObject multipartCrc32Composite(int size, int partSize) {
        return multipartCrc32Composite(Payload.ofSize(size), partSize);
    }

    public static StoredObject multipartCrc32Composite(Payload payload, int partSize) {
        return new StoredObject(payload, partSize, true, Algorithm.CRC32, ChecksumType.COMPOSITE);
    }

    public static StoredObject multipartCrc64FullObject() {
        return multipartCrc64FullObject(DEFAULT_SIZE, DEFAULT_PART_SIZE);
    }

    public static StoredObject multipartCrc64FullObject(int size, int partSize) {
        return new StoredObject(Payload.ofSize(size), partSize, true,
                Algorithm.CRC64NVME, ChecksumType.FULL_OBJECT);
    }

    public enum Shape {
        SINGLE_PUT_CRC32("singlePutCrc32"),
        MPU_CRC32_COMPOSITE("mpuCrc32Composite"),
        MPU_CRC64_FULL_OBJECT("mpuCrc64FullObject");

        private final String label;

        Shape(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public StoredObject create(int size, int partSize) {
            switch (this) {
                case SINGLE_PUT_CRC32:
                    return singlePutCrc32(size);
                case MPU_CRC32_COMPOSITE:
                    return multipartCrc32Composite(size, partSize);
                case MPU_CRC64_FULL_OBJECT:
                    return multipartCrc64FullObject(size, partSize);
                default:
                    throw new IllegalStateException("unhandled shape: " + this);
            }
        }
    }

    public Shape shape() {
        if (!multipart) {
            return Shape.SINGLE_PUT_CRC32;
        }
        return checksumType == ChecksumType.COMPOSITE
                ? Shape.MPU_CRC32_COMPOSITE
                : Shape.MPU_CRC64_FULL_OBJECT;
    }

    public int size() {
        return payload.size();
    }

    public int partCount() {
        return multipart ? (size() + partSize - 1) / partSize : 1;
    }

    public int partOffset(int partNumber) {
        checkPartNumber(partNumber);
        return multipart ? (partNumber - 1) * partSize : 0;
    }

    public int partLength(int partNumber) {
        checkPartNumber(partNumber);
        return multipart ? Math.min(partSize, size() - partOffset(partNumber)) : size();
    }

    private String computeETag() {
        if (!multipart) {
            return '"' + payload.md5Hex(0, size()) + '"';
        }
        int parts = partCount();
        byte[] concatenated = new byte[parts * 16];
        for (int n = 1; n <= parts; n++) {
            byte[] partMd5 = payload.md5Bytes(partOffset(n), partLength(n));
            System.arraycopy(partMd5, 0, concatenated, (n - 1) * 16, partMd5.length);
        }
        return '"' + Payload.hex(Payload.of(concatenated).md5Bytes()) + "-" + parts + '"';
    }

    private String computeStoredChecksum() {
        if (checksumType == ChecksumType.FULL_OBJECT) {
            return wholeObjectChecksum();
        }
        int parts = partCount();
        byte[] concatenated = new byte[parts * 4];
        for (int n = 1; n <= parts; n++) {
            byte[] partCrc = payload.crc32Bytes(partOffset(n), partLength(n));
            System.arraycopy(partCrc, 0, concatenated, (n - 1) * 4, partCrc.length);
        }
        SdkChecksum checksum = SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32);
        checksum.update(concatenated, 0, concatenated.length);
        return Base64.getEncoder().encodeToString(checksum.getChecksumBytes()) + "-" + parts;
    }

    public String wholeObjectChecksum() {
        return checksumOf(0, size());
    }

    public String partChecksum(int partNumber) {
        checkPartNumber(partNumber);
        if (!multipart) {
            return wholeObjectChecksum();
        }
        if (checksumType == ChecksumType.FULL_OBJECT) {
            return null;
        }
        return checksumOf(partOffset(partNumber), partLength(partNumber));
    }

    public Integer partCoveringExactly(int off, int len) {
        if (off % partSizeOrWhole() != 0) {
            return null;
        }
        int candidate = off / partSizeOrWhole() + 1;
        if (candidate < 1 || candidate > partCount()) {
            return null;
        }
        return partLength(candidate) == len ? candidate : null;
    }

    private int partSizeOrWhole() {
        return multipart ? partSize : Math.max(size(), 1);
    }

    public long[] windowForRange(String header) {
        String spec = header.trim();
        if (!spec.startsWith("bytes=")) {
            throw new IllegalArgumentException("unsupported Range unit: " + header);
        }
        spec = spec.substring("bytes=".length()).trim();
        if (spec.contains(",")) {
            throw new IllegalArgumentException(
                    "multi-range requests are not modeled (never captured from S3): " + header);
        }
        int dash = spec.indexOf('-');
        if (dash < 0) {
            throw new IllegalArgumentException("malformed Range: " + header);
        }
        String from = spec.substring(0, dash).trim();
        String to = spec.substring(dash + 1).trim();
        int size = size();
        long off;
        long last;
        if (from.isEmpty()) {
            long n = Math.min(Long.parseLong(to), size);
            off = size - n;
            last = size - 1L;
        } else {
            off = Long.parseLong(from);
            last = to.isEmpty() ? size - 1L : Math.min(Long.parseLong(to), size - 1L);
        }
        if (off < 0 || off >= size || last < off) {
            throw new IllegalArgumentException("unsatisfiable Range " + header
                    + " for a " + size + "-byte object; a 416 response is not modeled");
        }
        return new long[]{off, last - off + 1};
    }

    public byte[] appendedMd5(int off, int len) {
        return payload.md5Bytes(off, len);
    }

    private String checksumOf(int off, int len) {
        return algorithm == Algorithm.CRC32
                ? payload.crc32Base64(off, len)
                : payload.crc64NvmeBase64(off, len);
    }

    private void checkPartNumber(int partNumber) {
        if (partNumber < 1 || partNumber > partCount()) {
            throw new IllegalArgumentException("partNumber " + partNumber
                    + " outside 1.." + partCount());
        }
    }

    public String describe() {
        return (multipart ? "MPU" : "simplePut") + "/" + checksumType + " " + algorithm
                + " size=" + size() + (multipart ? " partSize=" + partSize : "")
                + " parts=" + partCount()
                + (versionId == null ? "" : " versionId=" + versionId);
    }
}
