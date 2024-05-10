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

package software.amazon.awssdk.migration.internal.recipe;

import static org.openrewrite.java.Assertions.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.migration.recipe.NewClassToBuilderPattern;

public class HttpSettingsToHttpClientTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-config-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.ChangeConfigTypes"),
                         new NewClassToBuilderPattern(),
                         new HttpSettingsToHttpClient());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs", "aws-sdk-java", "sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void httpSettings_shouldRemove() {
        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    ClientConfiguration clientConfiguration = new ClientConfiguration()\n"
                + "        .withMaxConnections(1000)\n"
                + "        .withConnectionTimeout(1000)\n"
                + "        .withTcpKeepAlive(true)\n"
                + "        .withSocketTimeout(1000)\n"
                + "        .withConnectionTTL(1000)\n"
                + "        .withRequestTimeout(1000)\n"
                + "        .withConnectionMaxIdleMillis(1000);\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "        .apiCallAttemptTimeout(Duration.ofMillis(1000)).build();\n"
                + "}"
            )
        );
    }
}
