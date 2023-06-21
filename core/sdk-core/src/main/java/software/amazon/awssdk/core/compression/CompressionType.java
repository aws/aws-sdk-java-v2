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

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.compression.GzipCompressor;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * The supported compression algorithms for operations with the requestCompression trait. Each supported algorithm will have an
 * {@link Compressor} implementation.
 */
@SdkInternalApi
public enum CompressionType {

    GZIP("gzip"),

    UNKNOWN_TO_SDK_VERSION(null);

    private static final Map<String, CompressionType> VALUE_MAP = EnumUtils.uniqueIndex(
        CompressionType.class, CompressionType::toString);

    private final String value;

    CompressionType(String value) {
        this.value = value;
    }

    /**
     * Maps the {@link CompressionType} to its corresponding {@link Compressor}.
     * TODO: Update mappings here when additional compressors are supported in the future
     */
    public Compressor compressor() {
        if (value == null) {
            return null;
        }
        if (value.equals("gzip")) {
            return new GzipCompressor();
        }
        return null;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Use this in place of valueOf to convert the raw string into the enum value.
     *
     * @param value
     *        real value
     * @return SupportedEncodings corresponding to the value
     */
    public static CompressionType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return VALUE_MAP.getOrDefault(value, UNKNOWN_TO_SDK_VERSION);
    }

    /**
     * Use this in place of {@link #values()} to return a {@link Set} of all values known to the SDK. This will return
     * all known enum values except {@link #UNKNOWN_TO_SDK_VERSION}.
     *
     * @return a {@link Set} of known {@link CompressionType}s
     */
    public static Set<CompressionType> knownValues() {
        Set<CompressionType> knownValues = EnumSet.allOf(CompressionType.class);
        knownValues.remove(UNKNOWN_TO_SDK_VERSION);
        return knownValues;
    }
}
