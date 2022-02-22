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

package software.amazon.awssdk.core.internal.checksums.factory;

import java.util.Optional;
import java.util.zip.Checksum;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;


/**
 * Class to load the Crt based checksum from aws-crt-java library if it is present in class path.
 */
@SdkInternalApi
public final class CrtBasedChecksumProvider {

    public static final Logger LOG = Logger.loggerFor(CrtBasedChecksumProvider.class);
    private static final String CRT_CLASSPATH_FOR_CRC32C = "software.amazon.awssdk.crt.checksums.CRC32C";
    private static final String CRT_CLASSPATH_FOR_CRC32 = "software.amazon.awssdk.crt.checksums.CRC32";
    private static final Lazy<Optional<Class<?>>> CRT_CRC32_CLASS_LOADER =
        new Lazy<>(() -> initializeCrtChecksumClass(CRT_CLASSPATH_FOR_CRC32));
    private static final Lazy<Optional<Class<?>>> CRT_CRC32_C_CLASS_LOADER =
        new Lazy<>(() -> initializeCrtChecksumClass(CRT_CLASSPATH_FOR_CRC32C));

    private CrtBasedChecksumProvider() {
    }

    public static Checksum createCrc32() {
        return createCrtBasedChecksum(CRT_CRC32_CLASS_LOADER);
    }

    public static Checksum createCrc32C() {
        return createCrtBasedChecksum(CRT_CRC32_C_CLASS_LOADER);
    }

    private static Checksum createCrtBasedChecksum(Lazy<Optional<Class<?>>> lazyClassLoader) {
        return lazyClassLoader.getValue().map(
            checksumClass -> {
                try {
                    return (Checksum) checksumClass.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    return null;
                }
            }).orElse(null);
    }

    private static Optional<Class<?>> initializeCrtChecksumClass(String classPath) {
        try {
            return Optional.of(ClassLoaderHelper.loadClass(classPath, false));
        } catch (ClassNotFoundException e) {
            LOG.debug(() -> "Cannot find the " + classPath + " class."
                            + " To invoke a request that requires a CRT based checksums.", e);
            return Optional.empty();
        }
    }
}
