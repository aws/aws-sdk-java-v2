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
import software.amazon.awssdk.utils.Logger;

public class MavenTransferManagerTest {
    private static final Logger log = Logger.loggerFor(MavenTransferManagerTest.class);
    private static String sdkVersion;
    private static Path mavenTmBefore;
    private static Path mavenTmAfter;
    private static Path target;
    private static Path mavenTmActual;
    private static Path mavenTmExpected;

    @BeforeAll
    static void setUp() throws IOException {
        sdkVersion = getVersion();
        mavenTmBefore = new File(MavenTransferManagerTest.class.getResource("maven-tm/before").getFile()).toPath();
        mavenTmAfter = new File(MavenTransferManagerTest.class.getResource("maven-tm/after").getFile()).toPath();
        target = new File(MavenTransferManagerTest.class.getResource("/").getFile()).toPath().getParent();

        mavenTmActual = target.resolve("maven-tm/actual");
        mavenTmExpected = target.resolve("maven-tm/expected");

        deleteTempDirectories();

        FileUtils.copyDirectory(mavenTmBefore.toFile(), mavenTmActual.toFile());
        FileUtils.copyDirectory(mavenTmAfter.toFile(), mavenTmExpected.toFile());

        replaceVersion(mavenTmExpected.resolve("pom.xml"), sdkVersion);
        replaceVersion(mavenTmActual.resolve("pom.xml"), sdkVersion);
    }

    private static void deleteTempDirectories() throws IOException {
        FileUtils.deleteDirectory(mavenTmActual.toFile());
        FileUtils.deleteDirectory(mavenTmExpected.toFile());
    }

    @Test
    @EnabledIf("versionAvailable")
    void mavenProject_shouldConvert() throws IOException {
        verifyTransformation();
        verifyCompilation();
    }

    private static void verifyTransformation() throws IOException {
        List<String> rewriteArgs = new ArrayList<>();
        // pin version since updates have broken tests
        String rewriteMavenPluginVersion = "5.46.0";
        addAll(rewriteArgs, "mvn", "org.openrewrite.maven:rewrite-maven-plugin:" + rewriteMavenPluginVersion + ":run",
               "-Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:v2-migration:"+ getMigrationToolVersion() + "-PREVIEW",
               "-Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2WithTransferManager");

        run(mavenTmActual, rewriteArgs.toArray(new String[0]));
        FileUtils.deleteDirectory(mavenTmActual.resolve("target").toFile());
        assertTwoDirectoriesHaveSameStructure(mavenTmActual, mavenTmExpected);
    }

    private static void verifyCompilation() {
        List<String> packageArgs = new ArrayList<>();
        addAll(packageArgs, "mvn", "package");
        run(mavenTmActual, packageArgs.toArray(new String[0]));
    }

    boolean versionAvailable() {
        return TestUtils.versionAvailable(sdkVersion);
    }
}
