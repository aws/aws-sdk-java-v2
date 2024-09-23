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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import software.amazon.awssdk.testutils.SdkVersionUtils;
import software.amazon.awssdk.utils.Logger;

public class MavenProjectTest {
    private static final Logger log = Logger.loggerFor(MavenProjectTest.class);
    private static String sdkVersion;
    private static Path mavenBefore;
    private static Path mavenAfter;
    private static Path target;
    private static Path mavenActual;
    private static Path mavenExpected;

    @BeforeAll
    static void setUp() throws IOException {
        sdkVersion = getVersion();
        mavenBefore = new File(MavenProjectTest.class.getResource("maven/before").getFile()).toPath();
        mavenAfter = new File(MavenProjectTest.class.getResource("maven/after").getFile()).toPath();
        target = new File(MavenProjectTest.class.getResource("/").getFile()).toPath().getParent();

        mavenActual = target.resolve("maven/actual");
        mavenExpected = target.resolve("maven/expected");

        deleteTempDirectories();

        FileUtils.copyDirectory(mavenBefore.toFile(), mavenActual.toFile());
        FileUtils.copyDirectory(mavenAfter.toFile(), mavenExpected.toFile());

        replaceVersion(mavenExpected.resolve("pom.xml"), sdkVersion);
        replaceVersion(mavenActual.resolve("pom.xml"), sdkVersion);
    }

    private static void deleteTempDirectories() throws IOException {
        FileUtils.deleteDirectory(mavenActual.toFile());
        FileUtils.deleteDirectory(mavenExpected.toFile());
    }

    @Test
    @EnabledIf("versionAvailable")
    void mavenProject_shouldConvert() throws IOException {
        verifyTransformation();
        verifyCompilation();
    }

    private static void verifyTransformation() throws IOException {
        List<String> rewriteArgs = new ArrayList<>();
        addAll(rewriteArgs, "mvn", "org.openrewrite.maven:rewrite-maven-plugin:run",
               "-Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:v2-migration:"+ getMigrationToolVersion() + "-PREVIEW",
               "-Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2");

        run(mavenActual, rewriteArgs.toArray(new String[0]));
        FileUtils.deleteDirectory(mavenActual.resolve("target").toFile());
        assertTwoDirectoriesHaveSameStructure(mavenActual, mavenExpected);
    }

    private static void verifyCompilation() {
        List<String> packageArgs = new ArrayList<>();
        addAll(packageArgs, "mvn", "package");
        run(mavenActual, packageArgs.toArray(new String[0]));
    }

    boolean versionAvailable() {
        return TestUtils.versionAvailable(sdkVersion);
    }
}
