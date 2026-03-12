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

public class NewV1ModelClassToV2Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ChangeSdkType(), new NewClassToBuilderPattern());
        spec.parser(Java8Parser.builder().classpath(
            "aws-java-sdk-sqs",
            "sqs",
            "sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void request_assignedToVariable_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQS sqs = null;\n"
                + "\n"
                + "        SendMessageRequest sendMessage = new SendMessageRequest()\n"
                + "                .withQueueUrl(\"url\")\n"
                + "                .withMessageBody(\"hello world\")\n"
                + "                .withMessageGroupId(\"my-group\");\n"
                + "\n"
                + "        sqs.sendMessage(sendMessage);\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "import software.amazon.awssdk.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsClient sqs = null;\n"
                + "\n"
                + "        SendMessageRequest sendMessage = SendMessageRequest.builder()\n"
                + "                .queueUrl(\"url\")\n"
                + "                .messageBody(\"hello world\")\n"
                + "                .messageGroupId(\"my-group\")\n"
                + "                .build();\n"
                + "\n"
                + "        sqs.sendMessage(sendMessage);\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void request_createdInline_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQS sqs = null;\n"
                + "\n"
                + "        sqs.sendMessage(new SendMessageRequest()\n"
                + "                .withQueueUrl(\"url\")\n"
                + "                .withMessageBody(\"hello world\")\n"
                + "                .withMessageGroupId(\"my-group\"));\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "import software.amazon.awssdk.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsClient sqs = null;\n"
                + "\n"
                + "        sqs.sendMessage(SendMessageRequest.builder()\n"
                + "                .queueUrl(\"url\")\n"
                + "                .messageBody(\"hello world\")\n"
                + "                .messageGroupId(\"my-group\")\n"
                + "                .build());\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void request_returnedFromMethod_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQS sqs = null;\n"
                + "\n"
                + "        sqs.sendMessage(createRequest());\n"
                + "    }\n"
                + "\n"
                + "    private static SendMessageRequest createRequest() {\n"
                + "        return new SendMessageRequest()\n"
                + "                .withQueueUrl(\"url\")\n"
                + "                .withMessageBody(\"hello world\")\n"
                + "                .withMessageGroupId(\"my-group\");\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "import software.amazon.awssdk.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsClient sqs = null;\n"
                + "\n"
                + "        sqs.sendMessage(createRequest());\n"
                + "    }\n"
                + "\n"
                + "    private static SendMessageRequest createRequest() {\n"
                + "        return SendMessageRequest.builder()\n"
                + "                .queueUrl(\"url\")\n"
                + "                .messageBody(\"hello world\")\n"
                + "                .messageGroupId(\"my-group\")\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void request_assignedToVariable_newOnly_isRewritten() {
        rewriteRun(
            java("import com.amazonaws.services.sqs.model.SendMessageRequest;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        SendMessageRequest sendMessage = new SendMessageRequest();\n"
                + "    }\n"
                + "}\n",
                 "import software.amazon.awssdk.services.sqs.model.SendMessageRequest;\n"
                 + "\n"
                 + "public class SqsExample {\n"
                 + "    public static void main(String[] args) {\n"
                 + "        SendMessageRequest sendMessage = SendMessageRequest.builder()\n"
                 + "                .build();\n"
                 + "    }\n"
                 + "}"
            )
        );
    }
}
