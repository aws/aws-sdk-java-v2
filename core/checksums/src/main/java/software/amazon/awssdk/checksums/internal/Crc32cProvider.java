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

package software.amazon.awssdk.checksums.internal;

import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.crt.checksums.CRC32C;

/**
 * Utility class to provide different implementations of CRC32C checksum. This class supports the use of: 1. Java-based CRC32C
 * (Java 9+ when available) 2. CRT-based CRC32C (when available) 3. SDK-based CRC32C (as fallback)
 */
@SdkInternalApi
public final class Crc32cProvider {


    // Class paths for different CRC32C implementations
    private static final String CRT_CRC32C_CLASS_PATH = "software.amazon.awssdk.crt.checksums.CRC32C";
    private static final String JAVA_CRC32C_CLASS_PATH = "java.util.zip.CRC32C";
    private static final ConstructorCache CONSTRUCTOR_CACHE = new ConstructorCache();

    // Private constructor to prevent instantiation
    private Crc32cProvider() {
    }

    /**
     * Creates an instance of the SDK-based CRC32C checksum as a fallback.
     *
     * @return An SdkChecksum instance.
     */
    static SdkChecksum createSdkBasedCrc32C() {
        SdkCrc32CChecksum sdkChecksum = SdkCrc32CChecksum.create();
        return new CrcCloneOnMarkChecksum(sdkChecksum, checksumToClone -> ((SdkCrc32CChecksum) checksumToClone).clone());
    }

    /**
     * Tries to create a Java 9-based CRC32C checksum.
     * If it's not available, it tries to create a CRT-based checksum.
     * If both are not available, it falls back to an SDK-based CRC32C checksum.
     *
     * @return An instance of {@link SdkChecksum}, based on the first available option.
     */
    public static SdkChecksum create() {
        SdkChecksum checksum = createJavaCrc32C();
        if (checksum == null) {
            checksum = createCrtCrc32C();
        }
        return checksum != null ? checksum : createSdkBasedCrc32C();
    }

    static SdkChecksum createCrtCrc32C() {
        return CONSTRUCTOR_CACHE.getConstructor(CRT_CRC32C_CLASS_PATH).map(constructor -> {
            try {
                return new CrcCloneOnMarkChecksum((Checksum) constructor.newInstance(), checksumToClone ->
                    (Checksum) ((CRC32C) checksumToClone).clone());
            } catch (ClassCastException | ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate " + JAVA_CRC32C_CLASS_PATH, e);
            }
        }).orElse(null);
    }

    static SdkChecksum createJavaCrc32C() {
        return CONSTRUCTOR_CACHE.getConstructor(JAVA_CRC32C_CLASS_PATH).map(constructor -> {
            try {
                return new CrcCombineOnMarkChecksum((Checksum) constructor.newInstance(), SdkCrc32CChecksum::combine);
            } catch (ClassCastException | ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate " + JAVA_CRC32C_CLASS_PATH, e);
            }
        }).orElse(null);
    }

}
