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

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RewriteTest;

public class NumberToDurationTest implements RewriteTest {

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void timeUnitNotSpecified_shouldUseMilliSeconds() {
        rewriteRun(
            spec -> spec.recipe(new NumberToDuration("com.amazonaws.ClientConfiguration setRequestTimeout(int)",
                                                     null))
                        .parser(Java8Parser.builder().classpath("sqs", "aws-core", "sdk-core", "aws-java-sdk-sqs")),
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setRequestTimeout(1000);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.ClientConfiguration;\n\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setRequestTimeout(Duration.ofMillis(1000));\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void timeUnitSpecified_shouldHonor() {
        rewriteRun(
            spec -> spec.recipe(new NumberToDuration("com.amazonaws.ClientConfiguration setRequestTimeout(int)",
                                                     TimeUnit.SECONDS))
                        .parser(Java8Parser.builder().classpath("sqs", "aws-core", "sdk-core", "aws-java-sdk-sqs")),
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setRequestTimeout(1000);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.ClientConfiguration;\n\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setRequestTimeout(Duration.ofSeconds(1000));\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void variableProvided_shouldRewrite() {
        rewriteRun(
            spec -> spec.recipe(new NumberToDuration("com.amazonaws.ClientConfiguration setRequestTimeout(int)",
                                                     TimeUnit.SECONDS))
                        .parser(Java8Parser.builder().classpath("sqs", "aws-core", "sdk-core", "aws-java-sdk-sqs")),
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        int timeout = 1000;\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setRequestTimeout(timeout);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.ClientConfiguration;\n\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        int timeout = 1000;\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setRequestTimeout(Duration.ofSeconds(timeout));\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
