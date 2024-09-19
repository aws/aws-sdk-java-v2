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

package software.amazon.awssdk.v2migrationtests;

import static java.util.Collections.addAll;
import static software.amazon.awssdk.v2migrationtests.TestUtils.assertTwoDirectoriesHaveSameStructure;
import static software.amazon.awssdk.v2migrationtests.TestUtils.getMigrationToolVersion;
import static software.amazon.awssdk.v2migrationtests.TestUtils.getVersion;
import static software.amazon.awssdk.v2migrationtests.TestUtils.replaceVersion;
import static software.amazon.awssdk.v2migrationtests.TestUtils.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import software.amazon.awssdk.testutils.SdkVersionUtils;
import software.amazon.awssdk.utils.Logger;

public class GradleProjectTest {
    private static final Logger log = Logger.loggerFor(GradleProjectTest.class);
    private static String sdkVersion;
    private static Path gradleBefore;
    private static Path gradleAfter;
    private static Path target;
    private static Path gradleActual;
    private static Path gradleExpected;

    @BeforeAll
    static void setUp() throws IOException {
        sdkVersion = getVersion();
        gradleBefore = new File(GradleProjectTest.class.getResource("gradle/before").getFile()).toPath();
        gradleAfter = new File(GradleProjectTest.class.getResource("gradle/after").getFile()).toPath();
        target = new File(GradleProjectTest.class.getResource("/").getFile()).toPath().getParent();

        gradleActual = target.resolve("gradle/actual");
        gradleExpected = target.resolve("gradle/expected");

        deleteTempDirectories();

        FileUtils.copyDirectory(gradleBefore.toFile(), gradleActual.toFile());
        FileUtils.copyDirectory(gradleAfter.toFile(), gradleExpected.toFile());

        Path gradlew = gradleActual.resolve("gradlew");

        Set<PosixFilePermission> perms = new HashSet<>();
        perms.addAll(Arrays.asList(PosixFilePermission.OWNER_READ,
                                   PosixFilePermission.OWNER_EXECUTE,
                                   PosixFilePermission.GROUP_EXECUTE,
                                   PosixFilePermission.GROUP_READ,
                                   PosixFilePermission.OTHERS_READ,
                                   PosixFilePermission.OTHERS_EXECUTE));

        Files.setPosixFilePermissions(gradlew, perms);

        replaceVersion(gradleActual.resolve("init.gradle"), getMigrationToolVersion() + "-PREVIEW");
    }

    private static void deleteTempDirectories() throws IOException {
        FileUtils.deleteDirectory(gradleActual.toFile());
        FileUtils.deleteDirectory(gradleExpected.toFile());
    }

    @Test
    @EnabledIf("versionAvailable")
    void gradleProject_shouldConvert() throws IOException {
        verifyTransformation();
        verifyCompilation();
    }

    boolean versionAvailable() {
        return TestUtils.versionAvailable(sdkVersion);
    }

    private static void verifyTransformation() throws IOException {
        List<String> rewriteArgs = new ArrayList<>();
        addAll(rewriteArgs, "./gradlew", "rewriteRun", "--init-script", "init.gradle",
               "-Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2");

        run(gradleActual, rewriteArgs.toArray(new String[0]));
        // only compares source directory and build.gradle and skip non-code directories such as gradle wrapper
        assertTwoDirectoriesHaveSameStructure(gradleActual.resolve("src"), gradleExpected.resolve("src"));
    }

    private static void verifyCompilation() {
        List<String> packageArgs = new ArrayList<>();
        addAll(packageArgs, "./gradlew", "build");
        run(gradleActual, packageArgs.toArray(new String[0]));
    }
}
