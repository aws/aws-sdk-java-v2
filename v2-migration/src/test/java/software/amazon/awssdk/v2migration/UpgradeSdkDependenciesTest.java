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

package software.amazon.awssdk.v2migration;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import software.amazon.awssdk.testutils.SdkVersionUtils;
import software.amazon.awssdk.utils.IoUtils;

public class UpgradeSdkDependenciesTest implements RewriteTest {

    private static String sdkVersion;

    private final String useClientConfiguration = "    import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                                                  + "    import com.amazonaws.ClientConfiguration;\n"
                                                  + "          public class Test {\n"
                                                  + "              private ClientConfiguration configuration;\n"
                                                  + "              private AmazonSQSClient sqsClient;\n"
                                                  + "          }";

    private final String noClientConfiguration = "    import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                                                  + "          public class Test {\n"
                                                  + "              private AmazonSQSClient sqsClient;\n"
                                                  + "          }";

    @BeforeAll
    static void setUp() throws IOException {
        sdkVersion = getVersion();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/upgrade-sdk-dependencies.yml")) {
            spec.recipe(stream, "software.amazon.awssdk.v2migration.UpgradeSdkDependencies")
                .parser(Java8Parser.builder().classpath(
                "aws-java-sdk-sqs", "aws-java-sdk-core"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getVersion() throws IOException {
        Path root = Paths.get("../").toAbsolutePath();
        Path pomFile = root.resolve("pom.xml");
        Optional<String> versionString =
            Files.readAllLines(pomFile)
                 .stream().filter(l -> l.contains("<awsjavasdk.previous.version>")).findFirst();

        if (!versionString.isPresent()) {
            throw new AssertionError("No version is found");
        }

        String string = versionString.get().trim();
        String substring = string.substring(29, string.indexOf('/') - 1);
        return substring;
    }

    boolean versionAvailable() {
        try {
            return SdkVersionUtils.checkVersionAvailability(sdkVersion,
                                                            "apache-client",
                                                            "netty-nio-client",
                                                            "aws-core",
                                                            "sqs");
        } catch (Exception exception) {
            return false;
        }
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    @EnabledIf("versionAvailable")
    void standardClient_shouldChangeDependencyGroupIdAndArtifactId() throws IOException {
        rewriteRun(
            mavenProject("project", srcMainJava(java(noClientConfiguration)),
            pomXml(
                "                  <project>\n"
                + "                      <groupId>com.test.app</groupId>\n"
                + "                      <artifactId>my-app</artifactId>\n"
                + "                      <version>1</version>\n"
                + "                      <dependencies>\n"
                + "                          <dependency>\n"
                + "                              <groupId>com.amazonaws</groupId>\n"
                + "                              <artifactId>aws-java-sdk-core</artifactId>\n"
                + "                              <version>1.12.100</version>\n"
                + "                          </dependency>\n"
                + "                          <dependency>\n"
                + "                              <groupId>com.amazonaws</groupId>\n"
                + "                              <artifactId>aws-java-sdk-sqs</artifactId>\n"
                + "                              <version>1.12.100</version>\n"
                + "                          </dependency>\n"
                + "                      </dependencies>\n"
                + "                  </project>",
                String.format("<project>\n"
                              + "    <groupId>com.test.app</groupId>\n"
                              + "    <artifactId>my-app</artifactId>\n"
                              + "    <version>1</version>\n"
                              + "    <dependencies>\n"
                              + "        <dependency>\n"
                              + "            <groupId>software.amazon.awssdk</groupId>\n"
                              + "            <artifactId>aws-core</artifactId>\n"
                              + "            <version>%1$s</version>\n"
                              + "        </dependency>\n"
                              + "        <dependency>\n"
                              + "            <groupId>software.amazon.awssdk</groupId>\n"
                              + "            <artifactId>sqs</artifactId>\n"
                              + "            <version>%1$s</version>\n"
                              + "        </dependency>\n"
                              + "    </dependencies>\n"
                              + "</project>", sdkVersion)
            )));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    @EnabledIf("versionAvailable")
    void useClientConfiguration_shouldAddHttpDependencies() throws IOException {
        rewriteRun(
            mavenProject("project", srcMainJava(java(useClientConfiguration)),
                         pomXml(
                             "                  <project>\n"
                             + "                      <groupId>com.test.app</groupId>\n"
                             + "                      <artifactId>my-app</artifactId>\n"
                             + "                      <version>1</version>\n"
                             + "                      <dependencies>\n"
                             + "                          <dependency>\n"
                             + "                              <groupId>com.amazonaws</groupId>\n"
                             + "                              <artifactId>aws-java-sdk-core</artifactId>\n"
                             + "                              <version>1.12.100</version>\n"
                             + "                          </dependency>\n"
                             + "                          <dependency>\n"
                             + "                              <groupId>com.amazonaws</groupId>\n"
                             + "                              <artifactId>aws-java-sdk-sqs</artifactId>\n"
                             + "                              <version>1.12.100</version>\n"
                             + "                          </dependency>\n"
                             + "                      </dependencies>\n"
                             + "                  </project>",
                             String.format("<project>\n"
                                           + "    <groupId>com.test.app</groupId>\n"
                                           + "    <artifactId>my-app</artifactId>\n"
                                           + "    <version>1</version>\n"
                                           + "    <dependencies>\n"
                                           + "        <dependency>\n"
                                           + "            <groupId>software.amazon.awssdk</groupId>\n"
                                           + "            <artifactId>aws-core</artifactId>\n"
                                           + "            <version>%1$s</version>\n"
                                           + "        </dependency>\n"
                                           + "        <dependency>\n"
                                           + "            <groupId>software.amazon.awssdk</groupId>\n"
                                           + "            <artifactId>sqs</artifactId>\n"
                                           + "            <version>%1$s</version>\n"
                                           + "        </dependency>\n"
                                           + "        <dependency>\n"
                                           + "            <groupId>software.amazon.awssdk</groupId>\n"
                                           + "            <artifactId>apache-client</artifactId>\n"
                                           + "            <version>%1$s</version>\n"
                                           + "        </dependency>\n"
                                           + "        <dependency>\n"
                                           + "            <groupId>software.amazon.awssdk</groupId>\n"
                                           + "            <artifactId>netty-nio-client</artifactId>\n"
                                           + "            <version>%1$s</version>\n"
                                           + "        </dependency>\n"
                                           + "    </dependencies>\n"
                                           + "</project>", sdkVersion)

                         )));
    }
}
