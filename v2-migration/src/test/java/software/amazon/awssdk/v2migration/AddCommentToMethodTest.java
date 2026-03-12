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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RewriteTest;

public class AddCommentToMethodTest implements RewriteTest {

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldAddCommentToMethod() {
        rewriteRun(
            spec -> spec.recipe(new AddCommentToMethod("com.amazonaws.ClientConfiguration setCacheResponseMetadata(boolean)",
                                                       "a comment"))
                        .parser(Java8Parser.builder().classpath("sqs", "aws-core", "sdk-core", "aws-java-sdk-sqs")),
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration.setCacheResponseMetadata(false);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        clientConfiguration/*AWS SDK for Java v2 migration: a comment*/.setCacheResponseMetadata(false);\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void hasExistingComments_shouldNotClobber() {
        rewriteRun(
            spec -> spec.recipe(new AddCommentToMethod("com.amazonaws.ClientConfiguration setCacheResponseMetadata(boolean)",
                                                       "a comment"))
                        .parser(Java8Parser.builder().classpath("sqs", "aws-core", "sdk-core", "aws-java-sdk-sqs")),
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        // Existing comment \n"
                + "        /*existing comment*/ clientConfiguration.setCacheResponseMetadata(false);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "        // Existing comment \n"
                + "        /*existing comment*/ clientConfiguration/*AWS SDK for Java v2 migration: a comment*/.setCacheResponseMetadata(false);\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
