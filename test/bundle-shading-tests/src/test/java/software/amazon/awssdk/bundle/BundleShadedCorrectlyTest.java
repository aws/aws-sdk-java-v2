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

package software.amazon.awssdk.bundle;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BundleShadedCorrectlyTest {
    private static final Set<String> ALLOWED_FILES;

    static {
        Set<String> allowedFiles = new HashSet<>();
        allowedFiles.add("mime.types");
        allowedFiles.add("VersionInfo.java");
        allowedFiles.add("mozilla/public-suffix-list.txt");
        ALLOWED_FILES = allowedFiles;
    }
    private static final Pattern ALLOWED_PREFIXES = Pattern.compile("^META-INF/.*|"
                                                                    + "^software/amazon/awssdk/.*|"
                                                                    + "^software/amazon/eventstream/.*|"
                                                                    + "^org/reactivestreams/.*|");
    private static Path bundlePath;

    @BeforeAll
    public static void setup() {
        try {
            Class<?> sdkClientClss = Class.forName("software.amazon.awssdk.core.SdkClient");
            URL jarLocation = sdkClientClss.getProtectionDomain().getCodeSource().getLocation();
            Path sdkClientJar = Paths.get(jarLocation.getFile());
            if (isBundleJar(sdkClientJar.getFileName().toString())) {
                bundlePath = sdkClientJar;
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Test
    public void testBundlePathsAreShaded() throws IOException {
        Assumptions.assumeTrue(bundlePath != null, "SDK classes are not loaded from the bundle.");

        try (ZipInputStream jarZipIs = new ZipInputStream(Files.newInputStream(bundlePath, StandardOpenOption.READ))) {
            while (true) {
                ZipEntry e = jarZipIs.getNextEntry();

                if (e == null) {
                    break;
                }

                if (e.isDirectory()) {
                    continue;
                }

                String fileName = e.getName();

                if (ALLOWED_FILES.contains(fileName)) {
                    continue;
                }

                if (!ALLOWED_PREFIXES.matcher(fileName).matches()) {
                    fail(() -> String.format("%s is not correctly shaded", fileName));
                }
            }
        }
    }

    private static boolean isBundleJar(String fileName) {
        return fileName != null &&
               (fileName.startsWith("bundle-") ||
                fileName.startsWith("aws-sdk-java-bundle-"));
    }
}
