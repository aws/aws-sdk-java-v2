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

public class S3StreamingRequestToV2Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new S3StreamingRequestToV2());
        spec.parser(Java8Parser.builder().classpath(
            "aws-java-sdk-s3",
            "aws-java-sdk-core",
            "s3",
            "sdk-core",
            "aws-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void testS3PutObjectOverrideRewrite() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.AmazonS3Client;\n"
                + "\n"
                + "import java.io.File;\n"
                + "\n"
                + "public class S3PutObjectExample {\n"
                + "    private static final String BUCKET = \"my-bucket\";\n"
                + "    private static final String KEY = \"key\";\n"
                + "\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3Client s3 = null;\n"
                + "\n"
                + "        File myFile = new File(\"test.txt\");\n"
                + "\n"
                + "        s3.putObject(BUCKET, KEY, myFile);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.services.s3.AmazonS3Client;\n"
                + "import com.amazonaws.services.s3.model.PutObjectRequest;\n"
                + "import software.amazon.awssdk.core.sync.RequestBody;\n"
                + "\n"
                + "import java.io.File;\n"
                + "\n"
                + "public class S3PutObjectExample {\n"
                + "    private static final String BUCKET = \"my-bucket\";\n"
                + "    private static final String KEY = \"key\";\n"
                + "\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3Client s3 = null;\n"
                + "\n"
                + "        File myFile = new File(\"test.txt\");\n"
                + "\n"
                + "        s3.putObject(new PutObjectRequest(BUCKET, KEY), RequestBody.fromFile(myFile));\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
