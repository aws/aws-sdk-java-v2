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

package software.amazon.awssdk.migration.internal.recipe;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class S3StreamingResponseToV2Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new S3StreamingResponseToV2());
        spec.parser(Java8Parser.builder().classpath(
            "aws-java-sdk-s3",
            "aws-java-sdk-core",
            "s3",
            "sdk-core"));
    }

    @Test
    public void testS3ObjectRewrite() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        S3Object object = s3.getObject(\"bucket\", \"key\");\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "import software.amazon.awssdk.core.ResponseInputStream;\n"
                + "import software.amazon.awssdk.services.s3.model.GetObjectResponse;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        ResponseInputStream<GetObjectResponse> object = s3.getObject(\"bucket\", \"key\");\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    public void getObjectContent() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        S3Object object = s3.getObject(\"bucket\", \"key\");\n"
                + "        object.getObjectContent().close();\n"
                + "    }\n"
                + "}\n",
                ""
            )
        );
    }
}
