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

/**
 * Utility class providing implementations of CRC checksums, specifically CRC32C and CRC64NVME.
 *
 * <p>Supports the following implementations for CRC32C:</p>
 * <ul>
 *     <li>Java-based CRC32C (Java 9+)</li>
 *     <li>CRT-based CRC32C (using AWS CRT library)</li>
 *     <li>SDK-based CRC32C (fallback)</li>
 * </ul>
 *
 * <p>Only supports CRT-based implementation for CRC64NVME (using AWS CRT library).</p>
 *
 * <p>For internal use only ({@link SdkInternalApi}).</p>
 */
@SdkInternalApi
public final class CrcChecksumProvider {


    // Class paths for different CRC32C implementations
    private static final String CRT_CRC32C_CLASS_PATH = "software.amazon.awssdk.crt.checksums.CRC32C";
    private static final String JAVA_CRC32C_CLASS_PATH = "java.util.zip.CRC32C";
    private static final ConstructorCache CONSTRUCTOR_CACHE = new ConstructorCache();
    private static final String CRT_CRC64NVME_PATH = "software.amazon.awssdk.crt.checksums.CRC64NVME";
    private static final String CRT_MODULE = "software.amazon.awssdk.crt:aws-crt";

    // Private constructor to prevent instantiation
    private CrcChecksumProvider() {
    }

    /**
     * Creates an instance of the SDK-based CRC32C checksum as a fallback.
     *
     * @return An SdkChecksum instance.
     */
    static SdkChecksum createSdkBasedCrc32C() {
        SdkCrc32CChecksum sdkChecksum = SdkCrc32CChecksum.create();
        return new CrcCloneOnMarkChecksum(sdkChecksum);
    }

    /**
     * Tries to create a Java 9-based CRC32C checksum.
     * If it's not available, it tries to create a CRT-based checksum.
     * If both are not available, it falls back to an SDK-based CRC32C checksum.
     *
     * @return An instance of {@link SdkChecksum}, based on the first available option.
     */
    public static SdkChecksum crc32cImplementation() {
        SdkChecksum checksum = createJavaCrc32C();
        if (checksum == null) {
            checksum = createCrtCrc32C();
        }
        return checksum != null ? checksum : createSdkBasedCrc32C();
    }

    static SdkChecksum createCrtCrc32C() {
        return CONSTRUCTOR_CACHE.getConstructor(CRT_CRC32C_CLASS_PATH).map(constructor -> {
            try {
                Checksum checksumInstance = (Checksum) constructor.newInstance();
                return new CrcCloneOnMarkChecksum(checksumInstance);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate " + CRT_CRC32C_CLASS_PATH, e);
            }
        }).orElse(null);
    }

    /**
     * Creates an instance of the CRT-based CRC64NVME checksum using AWS's CRT library.
     * <p>
     * Attempts to load the `CRC64NVME` implementation specified by `CRT_CRC64NVME_PATH` and, if successful,
     * wraps it in {@link CrcCloneOnMarkChecksum}. Throws a {@link RuntimeException} if `CRC64NVME` is unavailable.
     * </p>
     *
     * @return An {@link SdkChecksum} instance for CRC64NVME.
     * @throws IllegalStateException if instantiation fails.
     * @throws RuntimeException if the `CRC64NVME` implementation is not available.
     */
    static SdkChecksum crc64NvmeCrtImplementation() {
        return CONSTRUCTOR_CACHE.getConstructor(CRT_CRC64NVME_PATH).map(constructor -> {
            try {
                Checksum checksumInstance = (Checksum) constructor.newInstance();
                return new CrcCloneOnMarkChecksum(checksumInstance);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to instantiate " + CRT_CRC32C_CLASS_PATH, e);
            }
        }).orElseThrow(() -> new RuntimeException(
            "Could not load " + CRT_CRC64NVME_PATH + ". Add dependency on '" + CRT_MODULE
            + "' module to enable CRC64NVME feature."));
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
