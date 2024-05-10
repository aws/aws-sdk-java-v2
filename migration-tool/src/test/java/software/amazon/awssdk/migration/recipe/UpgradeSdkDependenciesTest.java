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

package software.amazon.awssdk.migration.recipe;

import static org.openrewrite.maven.Assertions.pomXml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class UpgradeSdkDependenciesTest implements RewriteTest {

    private static String sdkVersion;

    @BeforeAll
    static void setUp() throws IOException {
        sdkVersion = getVersion();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/upgrade-sdk-dependencies.yml")) {
            spec.recipe(stream, "software.amazon.awssdk.UpgradeSdkDependencies");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getVersion() throws IOException {
        Path root = Paths.get(".").normalize().toAbsolutePath();
        Path pomFile = root.resolve("pom.xml");
        Optional<String> versionString =
            Files.readAllLines(pomFile)
                 .stream().filter(l -> l.contains("<version>")).findFirst();

        if (!versionString.isPresent()) {
            throw new AssertionError("No version is found");
        }

        String string = versionString.get().trim();
        String substring = string.substring(9, string.indexOf('/') - 1);
        return substring;
    }

    @Test
    void standardClient_shouldChangeDependencyGroupIdAndArtifactId() throws IOException {
        getVersion();
        String currentVersion = getVersion();
        rewriteRun(
            pomXml(
                "                  <project>\n"
                + "                      <groupId>com.test.app</groupId>\n"
                + "                      <artifactId>my-app</artifactId>\n"
                + "                      <version>1</version>\n"
                + "                      <dependencies>\n"
                + "                          <dependency>\n"
                + "                              <groupId>com.amazonaws</groupId>\n"
                + "                              <artifactId>aws-java-sdk-sqs</artifactId>\n"
                + "                              <version>1.12.100</version>\n"
                + "                          </dependency>\n"
                + "                      </dependencies>\n"
                + "                  </project>",
                String.format("                  <project>\n"
                              + "                      <groupId>com.test.app</groupId>\n"
                              + "                      <artifactId>my-app</artifactId>\n"
                              + "                      <version>1</version>\n"
                              + "                      <dependencies>\n"
                              + "                          <dependency>\n"
                              + "                              <groupId>software.amazon.awssdk</groupId>\n"
                              + "                              <artifactId>sqs</artifactId>\n"
                              + "                              <version>%1$s</version>\n"
                              + "                          </dependency>\n"
                              + "                          <dependency>\n"
                              + "                              <groupId>software.amazon.awssdk</groupId>\n"
                              + "                              <artifactId>apache-client</artifactId>\n"
                              + "                              <version>%1$s</version>\n"
                              + "                          </dependency>\n"
                              + "                          <dependency>\n"
                              + "                              <groupId>software.amazon.awssdk</groupId>\n"
                              + "                              <artifactId>netty-nio-client</artifactId>\n"
                              + "                              <version>%1$s</version>\n"
                              + "                          </dependency>\n"
                              + "                      </dependencies>\n"
                              + "                  </project>", currentVersion)

            )
        );
    }
}
