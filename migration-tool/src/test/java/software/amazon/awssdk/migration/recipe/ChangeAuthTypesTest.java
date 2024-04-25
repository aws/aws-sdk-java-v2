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

import static org.openrewrite.java.Assertions.java;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import software.amazon.awssdk.migration.internal.recipe.NewClassToBuilder;

public class ChangeAuthTypesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-auth-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.ChangeAuthTypes"),
                         new NewClassToBuilder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().classpath("sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void defaultCredentialsProviderChain_usingNew_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        DefaultAWSCredentialsProviderChain credentials = new DefaultAWSCredentialsProviderChain();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        DefaultCredentialsProvider credentials = DefaultCredentialsProvider.builder().build();\n"
                + "    }\n"
                + "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void defaultCredentialsProviderChain_getInstance_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        DefaultAWSCredentialsProviderChain credentials = DefaultAWSCredentialsProviderChain.getInstance();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        DefaultCredentialsProvider credentials = DefaultCredentialsProvider.create();\n"
                + "    }\n"
                + "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void instanceCredentialsProvider_getInstance_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.InstanceProfileCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        InstanceProfileCredentialsProvider credentials = InstanceProfileCredentialsProvider.getInstance();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        InstanceProfileCredentialsProvider credentials = InstanceProfileCredentialsProvider.create();\n"
                + "    }\n"
                + "}\n"
            )
        );
    }
}
