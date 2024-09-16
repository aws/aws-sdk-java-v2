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

public class ChangeSdkTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeSdkType()).parser(Java8Parser.builder().classpath("aws-java-sdk-sqs", "sqs", "aws-java-sdk-s3",
                                                                                "aws-java-sdk-dynamodb", "aws-java-sdk-lambda"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeVariables() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n" +
                "import com.amazonaws.services.sqs.AmazonSQSClient;\n" +
                "import com.amazonaws.services.sqs.model.InvalidAttributeNameException;\n" +
                "import com.amazonaws.services.sqs.model.AmazonSQSException;\n" +
                "import com.amazonaws.services.sqs.model.ListQueuesResult;\n" +
               "import com.amazonaws.services.sqs.model.ListQueuesRequest;\n" +
                "class Test {\n" +
                "    static void method() {\n" +
                "        AmazonSQS sqs = null;\n" +
                "        ListQueuesRequest request = null;\n" +
                "        ListQueuesResult result = null;\n" +
                "        InvalidAttributeNameException exception = null;\n" +
                "        AmazonSQSException baseException = null;\n" +
                "    }\n" +
                "}\n",
               "import software.amazon.awssdk.services.sqs.SqsClient;\n"
               // TODO: duplicate import for some reason, fix this
               + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
               + "import software.amazon.awssdk.services.sqs.model.InvalidAttributeNameException;\n"
               + "import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;\n"
               + "import software.amazon.awssdk.services.sqs.model.SqsException;\n"
               + "import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;\n"
               + "\n"
               + "class Test {\n"
               + "    static void method() {\n"
               + "        SqsClient sqs = null;\n"
               + "        ListQueuesRequest request = null;\n"
               + "        ListQueuesResponse result = null;\n"
               + "        InvalidAttributeNameException exception = null;\n"
               + "        SqsException baseException = null;\n"
               + "    }\n"
               + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void wildCardImport_shouldRewrite() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.model.*;\n" +
                "import com.amazonaws.services.sqs.*;\n" +
                "class Test {\n" +
                "    private DeleteQueueResult deleteQueResult;\n" +
                "    static void method(CreateQueueResult createQueueResult) {\n" +
                "        AmazonSQS sqs = null;\n" +
                "        ListQueuesRequest request = null;\n" +
                "        ListQueuesResult result = null;\n" +
                "        InvalidAttributeNameException exception = null;\n" +
                "        AmazonSQSException baseException = null;\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.services.sqs.*;\n"
                + "import software.amazon.awssdk.services.sqs.model.*;\n"
                + "\n"
                + "class Test {\n"
                + "    private DeleteQueueResponse deleteQueResult;\n"
                + "    static void method(CreateQueueResponse createQueueResult) {\n"
                + "        SqsClient sqs = null;\n"
                + "        ListQueuesRequest request = null;\n"
                + "        ListQueuesResponse result = null;\n"
                + "        InvalidAttributeNameException exception = null;\n"
                + "        SqsException baseException = null;\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeFields() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.model.DeleteQueueRequest;\n" +
                "import com.amazonaws.services.sqs.model.DeleteQueueResult;\n" +
                "class Test {\n" +
                "    private DeleteQueueRequest deleteQueueRequest;\n" +
                "    private static DeleteQueueResult resultField;\n" +
                "}\n",
                "import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;\n" +
                "import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;\n" +
                "\n" +
                "class Test {\n" +
                "    private DeleteQueueRequest deleteQueueRequest;\n" +
                "    private static DeleteQueueResponse resultField;\n" +
                "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeFieldsInAList() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.model.DeleteQueueResult;\n" +
                "import java.util.List;\n" +
                "class Test {\n" +
                "    private List<DeleteQueueResult> results;\n" +
                "}\n",
                "import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;\n\n" +
                "import java.util.List;\n" +
                "\n" +
                "class Test {\n" +
                "    private List<DeleteQueueResponse> results;\n" +
                "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeMethodParameters() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.model.CreateQueueRequest;\n" +
                "class Test {\n" +
                "    static void method(CreateQueueRequest request) {\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;\n" +
                "\n" +
                "class Test {\n" +
                "    static void method(CreateQueueRequest request) {\n" +
                "    }\n" +
                "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldNotChangeExistingV2Types() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.model.CreateQueueRequest;\n" +
                "import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;\n" +
                "class Test {\n" +
                "    private CreateQueueRequest createQueue;\n" +
                "    private DeleteQueueRequest deleteQueue;\n" +
                "    static void method() {\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;\n" +
                "import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;\n" +
                "\n" +
                "class Test {\n" +
                "    private CreateQueueRequest createQueue;\n" +
                "    private DeleteQueueRequest deleteQueue;\n" +
                "    static void method() {\n" +
                "    }\n" +
                "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void shouldChangeFieldsInInnerClass() {
        rewriteRun(
            java(
                "import com.amazonaws.services.sqs.model.CreateQueueResult;\n" +
                "class Test {\n" +
                "    public class Inner{\n" +
                "       private CreateQueueResult createQueue;\n" +
                "    }\n" +
                "}\n",
                "import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;\n" +
                "\n" +
                "class Test {\n" +
                "    public class Inner{\n" +
                "       private CreateQueueResponse createQueue;\n" +
                "    }\n" +
                "}\n"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void hasUnsupportedFeature_shouldSkip() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.transfer.TransferManager;\n" +
                "import com.amazonaws.services.sqs.model.DeleteQueueRequest;\n" +
                "import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;\n" +
                "import com.amazonaws.services.lambda.invoke.LambdaFunction;\n" +
                "class Test {\n" +
                "    private TransferManager transferManager;\n" +
                "    private DeleteQueueRequest deleteQueue;\n" +
                "    private DynamoDBMapper ddbMapper;\n" +
                "    private LambdaFunction lambdaFunction;\n" +
                "}\n",
                "import com.amazonaws.services.s3.transfer.TransferManager;\n"
                + "import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;\n"
                + "import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;\n"
                + "import com.amazonaws.services.lambda.invoke.LambdaFunction;\n"
                + "\n"
                + "class Test {\n"
                + "    private TransferManager transferManager;\n"
                + "    private DeleteQueueRequest deleteQueue;\n"
                + "    private DynamoDBMapper ddbMapper;\n"
                + "    private LambdaFunction lambdaFunction;\n"
                + "}"
            )
        );
    }
}
