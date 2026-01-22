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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Disabled("OpenRewrite can't recognize types from the same project. https://github.com/openrewrite/rewrite/issues/2927")
public class NewV1ClassToStaticFactoryTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-auth-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.v2migration.ChangeAuthTypes"),
                         new NewClassToStaticFactory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().logCompilationWarningsAndErrors(true).classpath(
            "aws-java-sdk-core",
            "sdk-core", "auth", "aws-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void envVarCredsProvider_assignedToVariable_isRewritten() {
        rewriteRun(
            java("import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        EnvironmentVariableCredentialsProvider provider = new EnvironmentVariableCredentialsProvider();\n"
                + "    }\n"
                + "}\n",
                 "import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        EnvironmentVariableCredentialsProvider provider = EnvironmentVariableCredentialsProvider.create()"
                 + ";\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void imdsCredsProvider_assignedToVariable_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.InstanceProfileCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "        InstanceProfileCredentialsProvider credentials = new InstanceProfileCredentialsProvider();\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "        InstanceProfileCredentialsProvider credentials = InstanceProfileCredentialsProvider.create();\n"
                + "}\n"
            )
        );
    }


    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void staticCredentialsProvider_getInstance_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.AWSStaticCredentialsProvider;\n"
                + "import com.amazonaws.auth.AWSCredentials;\n"
                + "import com.amazonaws.auth.BasicAWSCredentials;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AWSCredentials credentials = new BasicAWSCredentials(\"foo\", \"bar\");\n"
                + "        AWSStaticCredentialsProvider provider = new AWSStaticCredentialsProvider(credentials);\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;\n"
                + "import software.amazon.awssdk.auth.credentials.AwsCredentials;\n"
                + "import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AwsCredentials credentials = AwsBasicCredentials.create(\"foo\", \"bar\");\n"
                + "        StaticCredentialsProvider provider = StaticCredentialsProvider.create(credentials);\n"
                + "    }\n"
                + "}\n"
            )
        );
    }
}
