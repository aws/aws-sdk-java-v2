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

public class ChangeConfigTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-config-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.ChangeConfigTypes"),
                         new NewClassToBuilderPattern());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs","aws-sdk-java", "sdk-core"));
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void defaultConfiguration_shouldConvert() {
        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration();\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "    \n"
                + "    void test() {\n"
                + "        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder().build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void defaultConfigurationWithSupportedNonConnectionSettings_shouldConvert() {
        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.retry.RetryMode;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration()\n"
                + "            .withRetryMode(RetryMode.STANDARD)\n"
                + "            .withClientExecutionTimeout(5000)\n"
                + "            .withRequestTimeout(1000)\n"
                + "            .withHeader(\"foo\", \"bar\");\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.core.retry.RetryMode;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "            .retryPolicy(RetryMode.STANDARD)\n"
                + "            .apiCallTimeout(Duration.ofMillis(5000))\n"
                + "            .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "            .putHeader(\"foo\", \"bar\").build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void defaultConfigurationWithUnsupportedSettings_shouldAddComment() {
        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration()\n"
                + "            .withCacheResponseMetadata(false)\n"
                + "            .withDisableHostPrefixInjection(true)\n"
                + "            .withDisableSocketProxy(true)\n"
                + "            .withDnsResolver(null)\n"
                + "            .withGzip(true)\n"
                + "            .withLocalAddress(null)\n"
                + "            .withSecureRandom(null)\n"
                + "            .withThrottledRetries(true)\n"
                + "            .withProtocol(null)\n"
                + "            .withUserAgent(\"test\")\n"
                + "            .withUserAgentPrefix(\"test\")\n"
                + "            .withUserAgentSuffix(\"test\")\n"
                + "            .withUseExpectContinue(false);\n"
                + "    }\n"
                + "}\n",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    void test() {\n"
                + "        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "            /*AWS SDK for Java v2 migration: cacheResponseMetadata is deprecated and not supported in v2. Consider removing it.*/.cacheResponseMetadata(false)\n"
                + "            /*AWS SDK for Java v2 migration: disableHostPrefixInjection is deprecated and not supported removed in v2. Consider removing it.*/.disableHostPrefixInjection(true)\n"
                + "            .disableSocketProxy(true)\n"
                + "            /*AWS SDK for Java v2 migration: dnsResolver is not supported in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.dnsResolver(null)\n"
                + "            /*AWS SDK for Java v2 migration: gzip is not supported in v2 tracking in https://github.com/aws/aws-sdk-java-v2/issues/866. Consider removing it.*/.gzip(true)\n"
                + "            /*AWS SDK for Java v2 migration: localAddress is not supported in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.localAddress(null)\n"
                + "            .secureRandom(null)\n"
                + "            .throttledRetries(true)\n"
                + "            .protocol(null)\n"
                + "            /*AWS SDK for Java v2 migration: userAgent override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.userAgent(\"test\")\n"
                + "            /*AWS SDK for Java v2 migration: userAgentPrefix override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.userAgentPrefix(\"test\")\n"
                + "            /*AWS SDK for Java v2 migration: userAgentSuffix override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.userAgentSuffix(\"test\")\n"
                + "            /*AWS SDK for Java v2 migration: useExpectContinue is removed in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.useExpectContinue(false).build();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
