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

public class NewV1ClientClassToV2Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new ChangeSdkType(), new NewClassToBuilderPattern());
        spec.parser(Java8Parser.builder().classpath("sqs",
            "aws-java-sdk-sqs",
            "sqs",
            "sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void clientCreatedUsingNew_isRewritten() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonSQS sqs = new AmazonSQSClient();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "public class SqsExample {\n"
                + "    public static void main(String[] args) {\n"
                + "        SqsClient sqs = SqsClient.builder()\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
