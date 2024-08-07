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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class ChangeExceptionTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-exception-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.v2migration.ChangeExceptionTypes"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs", "sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void exceptionGetter_shouldRewrite() {
        rewriteRun(
            java(
                "import com.amazonaws.AmazonServiceException;\n"
                + "import java.util.Map;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        AmazonServiceException serviceException = null;\n"
                + "        String requestId = serviceException.getRequestId();\n"
                + "        String errorCode = serviceException.getErrorCode();\n"
                + "        String errorMessage = serviceException.getErrorMessage();\n"
                + "        String serviceName = serviceException.getServiceName();\n"
                + "        int statusCode = serviceException.getStatusCode();\n"
                + "        Map<String, String> httpHeaders = serviceException.getHttpHeaders();\n"
                + "        byte[] rawResponse = serviceException.getRawResponse();\n"
                + "        String rawResponseContent = serviceException.getRawResponseContent();\n"
                + "\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.awscore.exception.AwsServiceException;\n"
                + "\n"
                + "import java.util.Map;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        AwsServiceException serviceException = null;\n"
                + "        String requestId = serviceException.requestId();\n"
                + "        String errorCode = serviceException.awsErrorDetails().errorCode();\n"
                + "        String errorMessage = serviceException.awsErrorDetails().errorMessage();\n"
                + "        String serviceName = serviceException.awsErrorDetails().serviceName();\n"
                + "        int statusCode = serviceException.awsErrorDetails().sdkHttpResponse().statusCode();\n"
                + "        Map<String, String> httpHeaders = serviceException.awsErrorDetails().sdkHttpResponse().headers();\n"
                + "        byte[] rawResponse = serviceException.awsErrorDetails().rawResponse().asByteArray();\n"
                + "        String rawResponseContent = serviceException.awsErrorDetails().rawResponse().asUtf8String();\n"
                + "\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void exceptionGetter_NotSupported_shouldAddComment() {
        rewriteRun(
            java(
                "import com.amazonaws.AmazonServiceException;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        AmazonServiceException serviceException = null;\n"
                + "        String proxyHost = serviceException.getProxyHost();\n"
                + "        AmazonServiceException.ErrorType errorCode = serviceException.getErrorType();\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.awscore.exception.AwsServiceException;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        AwsServiceException serviceException = null;\n"
                + "        String proxyHost = serviceException/*AWS SDK for Java v2 migration: getProxyHost is not supported in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.getProxyHost();\n"
                + "        AwsServiceException.ErrorType errorCode = serviceException/*AWS SDK for Java v2 migration: getErrorType is not supported in v2. AwsServiceException is a service error in v2. Consider removing it.*/.getErrorType();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
