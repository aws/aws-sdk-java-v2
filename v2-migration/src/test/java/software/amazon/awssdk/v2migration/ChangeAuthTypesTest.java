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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class ChangeAuthTypesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-auth-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.v2migration.ChangeAuthTypes"),
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
                + "        DefaultCredentialsProvider credentials = DefaultCredentialsProvider.builder()\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void defaultCredentialsProviderChain_getInstance_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.AWSCredentials;\n"
                + "import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        DefaultAWSCredentialsProviderChain credentials = DefaultAWSCredentialsProviderChain.getInstance();\n"
                + "        AWSCredentials awsCredentials = credentials.getCredentials();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.AwsCredentials;\n"
                + "import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        DefaultCredentialsProvider credentials = DefaultCredentialsProvider.create();\n"
                + "        AwsCredentials awsCredentials = credentials.resolveCredentials();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void instanceCredentialsProvider_getInstance_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.AWSCredentials;\n"
                + "import com.amazonaws.auth.InstanceProfileCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        InstanceProfileCredentialsProvider credentials = InstanceProfileCredentialsProvider.getInstance();\n"
                + "        AWSCredentials awsCredentials = credentials.getCredentials();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.AwsCredentials;\n"
                + "import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        InstanceProfileCredentialsProvider credentials = InstanceProfileCredentialsProvider.create();\n"
                + "        AwsCredentials awsCredentials = credentials.resolveCredentials();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void staticCredentials_getter_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.auth.AWSStaticCredentialsProvider;\n"
                + "import com.amazonaws.auth.AWSCredentials;\n"
                + "import com.amazonaws.auth.BasicAWSCredentials;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AWSCredentials credentials = new BasicAWSCredentials(\"foo\", \"bar\");\n"
                + "        String accessKeyId = credentials.getAWSAccessKeyId();\n"
                + "        String secreteKey = credentials.getAWSSecretKey();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;\n"
                + "import software.amazon.awssdk.auth.credentials.AwsCredentials;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AwsCredentials credentials = new AwsBasicCredentials(\"foo\", \"bar\");\n"
                + "        String accessKeyId = credentials.accessKeyId();\n"
                + "        String secreteKey = credentials.secretAccessKey();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void sessionStaticCredentials_getter_isRewritten() {
        rewriteRun(
            java("import com.amazonaws.auth.AWSCredentials;\n"
                + "import com.amazonaws.auth.BasicSessionCredentials;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        BasicSessionCredentials credentials = new BasicSessionCredentials(\"foo\", \"bar\", \"123\");\n"
                + "        String accessKeyId = credentials.getAWSAccessKeyId();\n"
                + "        String secreteKey = credentials.getAWSSecretKey();\n"
                + "        String sessionToken = credentials.getSessionToken();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AwsSessionCredentials credentials = new AwsSessionCredentials(\"foo\", \"bar\", \"123\");\n"
                + "        String accessKeyId = credentials.accessKeyId();\n"
                + "        String secreteKey = credentials.secretAccessKey();\n"
                + "        String sessionToken = credentials.sessionToken();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
