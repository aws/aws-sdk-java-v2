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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class V1BuilderVariationsToV2BuilderTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ChangeSdkType(),
                     new V1BuilderVariationsToV2Builder());
        spec.parser(Java8Parser.builder().classpath("sqs",
                                                    "aws-java-sdk-sqs",
                                                    "sqs",
                                                    "sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void asyncClient_useAsyncBuilder_shouldRewrite() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.AmazonSQSAsync;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSAsyncClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQSAsync sqs = AmazonSQSAsyncClient.asyncBuilder().build();\n"
                + "    }\n"
                + "}\n",
                 "import software.amazon.awssdk.services.sqs.SqsAsyncClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsAsyncClient sqs = SqsAsyncClient.builder().build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void syncClientBuilder_useStandardBuilderWithBuild_shouldRewrite() {
        rewriteRun(
            recipeSpec -> recipeSpec.expectedCyclesThatMakeChanges(2),
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                 + "import com.amazonaws.services.sqs.AmazonSQSClientBuilder;\n"
                 + "\n"
                 + "public class Example {\n"
                 + "    public static void main(String[] args) {\n"
                 + "        AmazonSQS sqs = AmazonSQSClientBuilder.standard().build();\n"
                 + "    }\n"
                 + "}\n",
                 "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                 + "\n"
                 + "public class Example {\n"
                 + "    public static void main(String[] args) {\n"
                 + "        SqsClient sqs = SqsClient.builder().build();\n"
                 + "    }\n"
                 + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void asyncClientBuilder_useStandardBuilderWithBuild_shouldRewrite() {
        rewriteRun(
            recipeSpec -> recipeSpec.expectedCyclesThatMakeChanges(2),
            java(
                "import com.amazonaws.services.sqs.AmazonSQSAsync;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQSAsync sqs = AmazonSQSAsyncClientBuilder.standard().build();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsAsyncClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsAsyncClient sqs = SqsAsyncClient.builder().build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void syncClientBuilder_useStandardBuilderWithoutBuild_shouldRewrite() {
        rewriteRun(
            recipeSpec -> recipeSpec.expectedCyclesThatMakeChanges(2),
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQSClientBuilder sqs = AmazonSQSClientBuilder.standard();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsClientBuilder sqs = SqsClient.builder();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void asyncClientBuilder_useStandardBuilderWithoutBuild_shouldRewrite() {
        rewriteRun(
            recipeSpec -> recipeSpec.expectedCyclesThatMakeChanges(2),
            java(
                "import com.amazonaws.services.sqs.AmazonSQSAsync;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQSAsyncClientBuilder sqs = AmazonSQSAsyncClientBuilder.standard();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsAsyncClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsAsyncClientBuilder sqs = SqsAsyncClient.builder();\n"
                + "    }\n"
                + "}"
            )
        );
    }


    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void syncClientBuilder_useCreate_shouldRewrite() {
        rewriteRun(
            recipeSpec -> recipeSpec.expectedCyclesThatMakeChanges(2),
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsClient sqs = SqsClient.create();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void asyncClientBuilder_useCreate_shouldRewrite() {
        rewriteRun(
            recipeSpec -> recipeSpec.expectedCyclesThatMakeChanges(2),
            java(
                "import com.amazonaws.services.sqs.AmazonSQSAsync;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQSAsync sqs = AmazonSQSAsyncClientBuilder.defaultClient();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsAsyncClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsAsyncClient sqs = SqsAsyncClient.create();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
