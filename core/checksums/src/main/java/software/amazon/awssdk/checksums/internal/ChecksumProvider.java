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
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.crt.checksums.CRC32C;
import software.amazon.awssdk.crt.checksums.CRC64NVME;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.Lazy;

/**
 * Utility class providing implementations of checksums.
 *
 * <p>Supports the following implementations for CRC32C:</p>
 * <ul>
 *     <li>Java-based CRC32C (Java 9+)</li>
 *     <li>CRT-based CRC32C (using AWS CRT library)</li>
 *     <li>SDK-based CRC32C (fallback)</li>
 * </ul>
 *
 * <p>Supports CRT-based implementations for CRC64NVME and XXHASH algorithms (using AWS CRT library).</p>
 *
 * <p>For internal use only ({@link SdkInternalApi}).</p>
 */
@SdkInternalApi
public final class ChecksumProvider {

    // Class paths for different CRC32C implementations
    private static final String CRT_CRC32C_CLASS_PATH = "software.amazon.awssdk.crt.checksums.CRC32C";
    private static final String JAVA_CRC32C_CLASS_PATH = "java.util.zip.CRC32C";
    private static final ConstructorCache CONSTRUCTOR_CACHE = new ConstructorCache();
    private static final String CRT_CRC64NVME_PATH = "software.amazon.awssdk.crt.checksums.CRC64NVME";
    private static final String CRT_XXHASH_PATH = "software.amazon.awssdk.crt.checksums.XXHash";
    private static final String CRT_MODULE = "software.amazon.awssdk.crt:aws-crt";

    private static Lazy<Boolean> isXxHashAvailable = checkCrtAvailability(CRT_XXHASH_PATH);
    private static Lazy<Boolean> isCrc64NvmeAvailable = checkCrtAvailability(CRT_CRC64NVME_PATH);
    private static Lazy<Boolean> isCrc32CAvailable = checkCrtAvailability(CRT_CRC32C_CLASS_PATH);

    // Private constructor to prevent instantiation
    private ChecksumProvider() {
    }

    private static Lazy<Boolean> checkCrtAvailability(String fqcn) {
        return new Lazy<>(() -> {
            try {
                ClassLoaderHelper.loadClass(fqcn, false);
            } catch (ClassNotFoundException e) {
                return false;
            }
            return true;
        });
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
        if (!isCrc32CAvailable.getValue()) {
            return null;
        }

        Checksum checksumInstance = new CRC32C();
        return new CrcCloneOnMarkChecksum(checksumInstance);
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

        if (!isCrc64NvmeAvailable.getValue()) {
            throw new RuntimeException(
                "Could not load " + CRT_CRC64NVME_PATH + ". Add dependency on '" + CRT_MODULE
                + "' module to enable CRC64NVME feature.");
        }

        return new CrcCloneOnMarkChecksum(new CRC64NVME());
    }

    /**
     * Creates an instance of the CRT-based XXHASH64 checksum using AWS's CRT library.
     *
     * @return An {@link SdkChecksum} instance for XXHASH64.
     * @throws RuntimeException if the CRT implementation is not available.
     */
    public static SdkChecksum xxHash64CrtImplementation() {
        return crtXxHash(DefaultChecksumAlgorithm.XXHASH64);
    }

    /**
     * Creates an instance of the CRT-based XXHASH3 checksum using AWS's CRT library.
     *
     * @return An {@link SdkChecksum} instance for XXHASH3.
     * @throws RuntimeException if the CRT implementation is not available.
     */
    public static SdkChecksum xxHash3CrtImplementation() {
        return crtXxHash(DefaultChecksumAlgorithm.XXHASH3);
    }

    /**
     * Creates an instance of the CRT-based XXHASH128 checksum using AWS's CRT library.
     *
     * @return An {@link SdkChecksum} instance for XXHASH128.
     * @throws RuntimeException if the CRT implementation is not available.
     */
    public static SdkChecksum xxHash128CrtImplementation() {
        return crtXxHash(DefaultChecksumAlgorithm.XXHASH128);
    }

    static SdkChecksum crtXxHash(ChecksumAlgorithm algorithm) {
        if (!isXxHashAvailable.getValue()) {
            throw new RuntimeException(
                String.format("Could not load %s for algorithm: %s. Add dependency on '%s' module.", CRT_XXHASH_PATH,
                              algorithm.algorithmId(), CRT_MODULE));
        }

        return new XxHashChecksum(algorithm);
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
