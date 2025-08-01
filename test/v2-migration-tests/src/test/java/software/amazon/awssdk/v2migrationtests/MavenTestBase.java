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
import static software.amazon.awssdk.v2migrationtests.TestUtils.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;

public class MavenTestBase {

    protected static String sdkVersion;
    protected static Path mavenBefore;
    protected static Path mavenAfter;
    protected static Path target;
    protected static Path mavenActual;
    protected static Path mavenExpected;

    @BeforeAll
    static void init() throws IOException {
        sdkVersion = getVersion();
    }

    protected static void deleteTempDirectories() throws IOException {
        FileUtils.deleteDirectory(mavenActual.toFile());
        FileUtils.deleteDirectory(mavenExpected.toFile());
    }

    protected static void verifyTransformation(boolean experimental) throws IOException {
        String recipeCmd = "-Drewrite.activeRecipes=software.amazon.awssdk.v2migration.AwsSdkJavaV1ToV2";
        if (experimental) {
            recipeCmd += "Experimental";
        }

        List<String> rewriteArgs = new ArrayList<>();
        // pin version since updates have broken tests
        String rewriteMavenPluginVersion = "6.6.0";
        addAll(rewriteArgs, "mvn", "org.openrewrite.maven:rewrite-maven-plugin:" + rewriteMavenPluginVersion + ":run",
               "-Drewrite.recipeArtifactCoordinates=software.amazon.awssdk:v2-migration:"+ getMigrationToolVersion() + "-PREVIEW",
               recipeCmd);

        run(mavenActual, rewriteArgs.toArray(new String[0]));
        FileUtils.deleteDirectory(mavenActual.resolve("target").toFile());
        assertTwoDirectoriesHaveSameStructure(mavenActual, mavenExpected);
    }

    protected static void verifyCompilation() {
        List<String> packageArgs = new ArrayList<>();
        addAll(packageArgs, "mvn", "package");
        run(mavenActual, packageArgs.toArray(new String[0]));
    }

    boolean versionAvailable() {
        return TestUtils.versionAvailable(sdkVersion);
    }
}
