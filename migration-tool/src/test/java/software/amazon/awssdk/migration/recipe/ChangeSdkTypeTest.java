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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import software.amazon.awssdk.migration.recipe.ChangeSdkType;

public class ChangeSdkTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeSdkType());
    }

    @Test
    void shouldChangeVariables() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs")),
            java(
                "import com.amazonaws.services.sqs.AmazonSQS;\n" +
                "import com.amazonaws.services.sqs.AmazonSQSClient;\n" +
                "import com.amazonaws.services.sqs.model.ListQueuesResult;\n" +
               "import com.amazonaws.services.sqs.model.ListQueuesRequest;\n" +
                "class Test {\n" +
                "    static void method() {\n" +
                "        AmazonSQS sqs = null;\n" +
                "        ListQueuesRequest request = null;\n" +
                "        ListQueuesResult result = null;\n" +
                "    }\n" +
                "}\n",
               "import software.amazon.awssdk.services.sqs.SqsClient;\n" +
               "import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;\n" +
               // TODO: duplicate import for some reason, fix this
               "import software.amazon.awssdk.services.sqs.SqsClient;\n" +
                "import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;\n" +
                "\n" +
                "class Test {\n" +
               "    static void method() {\n" +
                "        SqsClient sqs = null;\n" +
                "        ListQueuesRequest request = null;\n" +
                "        ListQueuesResponse result = null;\n" +
                "    }\n" +
                "}\n"
            )
        );
    }

    @Test
    void shouldChangeFields() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs")),
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
    void shouldChangeFieldsInAList() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs")),
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
    void shouldChangeMethodParameters() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs")),
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
    void shouldNotChangeExistingV2Types() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs", "sqs")),
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
    void shouldChangeFieldsInInnerClass() {
        rewriteRun(
            spec -> spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs")),
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
}
