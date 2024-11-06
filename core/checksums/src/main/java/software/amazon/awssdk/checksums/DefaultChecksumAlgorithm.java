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

import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;

/**
 * An enumeration of supported checksum algorithms.
 */
@SdkProtectedApi
public final class DefaultChecksumAlgorithm {
    public static final ChecksumAlgorithm CRC32C = of("CRC32C");
    public static final ChecksumAlgorithm CRC32 = of("CRC32");
    public static final ChecksumAlgorithm MD5 = of("MD5");
    public static final ChecksumAlgorithm SHA256 = of("SHA256");
    public static final ChecksumAlgorithm SHA1 = of("SHA1");
    public static final ChecksumAlgorithm CRC64NVME = of("CRC64NVME");

    private DefaultChecksumAlgorithm() {
    }

    private static ChecksumAlgorithm of(String name) {
        return ChecksumAlgorithmsCache.put(name);
    }

    private static final class ChecksumAlgorithmsCache {
        private static final ConcurrentHashMap<String, ChecksumAlgorithm> VALUES = new ConcurrentHashMap<>();

        private static ChecksumAlgorithm put(String value) {
            return VALUES.computeIfAbsent(value, v -> () -> v);
        }
    }
}
