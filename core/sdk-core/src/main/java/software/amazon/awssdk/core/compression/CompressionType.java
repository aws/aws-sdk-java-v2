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

package software.amazon.awssdk.core.compression;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.utils.Validate;

/**
 * The supported compression algorithms for operations with the requestCompression trait. Each supported algorithm will have an
 * {@link Compressor} implementation.
 */
@SdkInternalApi
public final class CompressionType {

    public static final CompressionType GZIP = CompressionType.of("gzip");

    private static Map<String, Compressor> compressorMap = new HashMap<String, Compressor>() {{
            put("gzip", new GzipCompressor());
        }};

    private final String id;

    private CompressionType(String id) {
        this.id = id;
    }

    /**
     * Creates a new {@link CompressionType} of the given value.
     */
    public static CompressionType of(String value) {
        Validate.paramNotBlank(value, "compressionType");
        return CompressionTypeCache.put(value);
    }

    /**
     * Returns the {@link Set} of {@link String}s of compression types supported by the SDK.
     */
    public static Set<String> compressionTypes() {
        return compressorMap.keySet();
    }

    /**
     * Whether or not the compression type is supported by the SDK.
     */
    public static boolean isSupported(String compressionType) {
        return compressionTypes().contains(compressionType);
    }

    /**
     * Maps the {@link CompressionType} to its corresponding {@link Compressor}.
     */
    public Compressor newCompressor() {
        Compressor compressor = compressorMap.getOrDefault(this.id, null);
        if (compressor == null) {
            throw new UnsupportedOperationException("The compression type " + id + " does not have an implementation of "
                                                    + "Compressor");
        }
        return compressor;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompressionType that = (CompressionType) o;
        return Objects.equals(id, that.id)
            && Objects.equals(compressorMap, that.compressorMap);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (compressorMap != null ? compressorMap.hashCode() : 0);
        return result;
    }

    private static class CompressionTypeCache {
        private static final ConcurrentHashMap<String, CompressionType> VALUES = new ConcurrentHashMap<>();

        private CompressionTypeCache() {
        }

        private static CompressionType put(String value) {
            return VALUES.computeIfAbsent(value, v -> new CompressionType(value));
        }
    }
}
