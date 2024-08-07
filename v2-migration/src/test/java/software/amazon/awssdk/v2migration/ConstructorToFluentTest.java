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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.java.Assertions.java;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

/**
 * Recipe that remaps invocations of model constructors that take some members as constructor parameters, so that the
 * parameters are specified using the fluent setter for the member instead.
 */
public class ConstructorToFluentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(Java8Parser.builder().classpath(
            "aws-java-sdk-s3",
            "aws-java-sdk-core",
            "s3",
            "sdk-core"));
        spec.recipe(new ConstructorToFluent("com.amazonaws.services.s3.model.GetObjectRequest",
                                            Arrays.asList("java.lang.String", "java.lang.String"),
                                            Arrays.asList("withBucketName", "withKey")));

    }

    @Test
    public void newRecipe_listsNotMatch_throws() {
        assertThatThrownBy(() -> new ConstructorToFluent("foo",
                                                         Collections.emptyList(),
                                                         Collections.singletonList("bar")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ConstructorToFluent("foo",
                                                         Collections.singletonList("bar"),
                                                         Collections.emptyList()))
            .isInstanceOf(IllegalArgumentException.class);
    }



    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void getObjectRequest_matchingCtorNotFound_doesNotRewrite() {
        rewriteRun(
            java("import com.amazonaws.services.s3.AmazonS3;\n"
                 + "import com.amazonaws.services.s3.model.GetObjectRequest;\n"
                 + "import com.amazonaws.services.s3.model.S3Object;\n"
                 + "import com.amazonaws.services.s3.model.S3ObjectInputStream;\n"
                 + "\n"
                 + "public class S3Example {\n"
                 + "    public static void main(String[] args) {\n"
                 + "        AmazonS3 s3 = null;\n"
                 + "\n"
                 + "        GetObjectRequest getObject = new GetObjectRequest(\"bucket\", \"key\", \"version\");\n"
                 + "\n"
                 + "        S3Object object = s3.getObject(getObject);\n"
                 + "\n"
                 + "        S3ObjectInputStream objectContent = object.getObjectContent();\n"
                 + "    }\n"
                 + "}\n",
                 sourceSpecs -> {}
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void getObjectRequest_bucketKeyCtor_convertedToFluent() {
        rewriteRun(
            java(
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.GetObjectRequest;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        GetObjectRequest getObject = new GetObjectRequest(\"bucket\", \"key\");\n"
                + "\n"
                + "        S3Object object = s3.getObject(getObject);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.GetObjectRequest;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        GetObjectRequest getObject = new GetObjectRequest().withBucketName(\"bucket\").withKey(\"key\");\n"
                + "\n"
                + "        S3Object object = s3.getObject(getObject);\n"
                + "    }\n"
                + "}"
            )
        );
    }

    // RequesterPays is a boolean primitive type, test to ensure the type matching can handle this
    @Test
    @EnabledOnJre({JRE.JAVA_8})
    public void getObjectRequest_bucketKeyRequesterPays_convertedToFluent() {
        rewriteRun(
            spec -> spec.recipe(new ConstructorToFluent("com.amazonaws.services.s3.model.GetObjectRequest",
                                                        Arrays.asList("java.lang.String", "java.lang.String", "boolean"),
                                                        Arrays.asList("withBucketName", "withKey", "withRequesterPays"))),
            java(
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.GetObjectRequest;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        GetObjectRequest getObject = new GetObjectRequest(\"bucket\", \"key\", false);\n"
                + "\n"
                + "        S3Object object = s3.getObject(getObject);\n"
                + "    }\n"
                + "}\n",
                "import com.amazonaws.services.s3.AmazonS3;\n"
                + "import com.amazonaws.services.s3.model.GetObjectRequest;\n"
                + "import com.amazonaws.services.s3.model.S3Object;\n"
                + "\n"
                + "public class S3Example {\n"
                + "    public static void main(String[] args) {\n"
                + "        AmazonS3 s3 = null;\n"
                + "\n"
                + "        GetObjectRequest getObject = new GetObjectRequest().withBucketName(\"bucket\").withKey(\"key\").withRequesterPays(false);\n"
                + "\n"
                + "        S3Object object = s3.getObject(getObject);\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
