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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.Java8Parser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Disabled("With OpenRewrite version bump, unit tests fail when ExecutionContext#putMessage is used with multiple recipes "
          + "(invoked in HttpSettingsToHttpClient)")
public class ChangeConfigTypesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/rewrite/change-config-types.yml")) {
            spec.recipes(Environment.builder()
                                    .load(new YamlResourceLoader(stream, URI.create("rewrite.yml"), new Properties()))
                                    .build()
                                    .activateRecipes("software.amazon.awssdk.v2migration.ChangeConfigTypes"),
                         new ChangeSdkType(),
                         new NewClassToBuilderPattern(),
                         new HttpSettingsToHttpClient());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        spec.parser(Java8Parser.builder().classpath("aws-java-sdk-sqs", "sdk-core"));
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
                + "        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithSupportedNonConnectionSettings_shouldConvert() {
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
                + "                .retryPolicy(RetryMode.STANDARD)\n"
                + "                .apiCallTimeout(Duration.ofMillis(5000))\n"
                + "                .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "                .putHeader(\"foo\", \"bar\")\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithHttpSettings_localVariable_shouldRemoveAndSetOnSdkClient() {

        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    public void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration()\n"
                + "            .withRequestTimeout(1000)\n"
                + "            .withMaxConnections(1000)\n"
                + "            .withConnectionTimeout(1000)\n"
                + "            .withTcpKeepAlive(true)\n"
                + "            .withSocketTimeout(1000)\n"
                + "            .withConnectionTTL(1000)\n"
                + "            .withConnectionMaxIdleMillis(1000);\n"
                + "\n"
                + "        AmazonSQS sqs = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(clientConfiguration)\n"
                + "                                       .build();\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.apache.ApacheHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    public void test() {\n"
                + "        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "                .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "                .build();\n"
                + "\n"
                + "        SqsClient sqs = SqsClient.builder()\n"
                + "                .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                        .connectionMaxIdleTime(Duration.ofMillis(1000))\n"
                + "                        .tcpKeepAlive(true)\n"
                + "                        .socketTimeout(Duration.ofMillis(1000))\n"
                + "                        .connectionTimeToLive(Duration.ofMillis(1000))\n"
                + "                        .connectionTimeout(Duration.ofMillis(1000))\n"
                + "                        .maxConnections(1000))\n"
                + "                .overrideConfiguration(clientConfiguration)\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithHttpSettings_memberVariable_shouldRemoveAndSetOnSdkClient() {

        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    private ClientConfiguration clientConfiguration = new ClientConfiguration()\n"
                + "            .withRequestTimeout(1000)\n"
                + "            .withMaxConnections(2000)\n"
                + "            .withConnectionTimeout(3000)\n"
                + "            .withTcpKeepAlive(true)\n"
                + "            .withSocketTimeout(4000)\n"
                + "            .withConnectionTTL(5000)\n"
                + "            .withConnectionMaxIdleMillis(6000);\n"
                + "\n"
                + "    public void test() {\n"
                + "        AmazonSQS sqs = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(clientConfiguration)\n"
                + "                                       .build();\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.apache.ApacheHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "    private ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "            .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "            .build();\n"
                + "\n"
                + "    public void test() {\n"
                + "        SqsClient sqs = SqsClient.builder()\n"
                + "                .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                        .connectionMaxIdleTime(Duration.ofMillis(6000))\n"
                + "                        .tcpKeepAlive(true)\n"
                + "                        .socketTimeout(Duration.ofMillis(4000))\n"
                + "                        .connectionTimeToLive(Duration.ofMillis(5000))\n"
                + "                        .connectionTimeout(Duration.ofMillis(3000))\n"
                + "                        .maxConnections(2000))\n"
                + "                .overrideConfiguration(clientConfiguration)\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithHttpSettings_usedByMultipleSdkClients_shouldRemoveAndSetOnSdkClients() {

        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    private static final ClientConfiguration CONFIGURATION = new ClientConfiguration()\n"
                + "            .withRequestTimeout(1000)\n"
                + "            .withMaxConnections(2000)\n"
                + "            .withConnectionTimeout(3000)\n"
                + "            .withTcpKeepAlive(true)\n"
                + "            .withSocketTimeout(4000)\n"
                + "            .withConnectionTTL(5000)\n"
                + "            .withConnectionMaxIdleMillis(6000);\n"
                + "    private final AmazonSQS sqsMemberVariable = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(CONFIGURATION)\n"
                + "                                       .build();\n"
                + "\n"
                + "    public void test() {\n"
                + "        AmazonSQS sqs = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(CONFIGURATION)\n"
                + "                                       .build();\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.apache.ApacheHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "    private static final ClientOverrideConfiguration CONFIGURATION = ClientOverrideConfiguration.builder()\n"
                + "            .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "            .build();\n"
                + "    private final SqsClient sqsMemberVariable = SqsClient.builder()\n"
                + "            .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                    .connectionMaxIdleTime(Duration.ofMillis(6000))\n"
                + "                    .tcpKeepAlive(true)\n"
                + "                    .socketTimeout(Duration.ofMillis(4000))\n"
                + "                    .connectionTimeToLive(Duration.ofMillis(5000))\n"
                + "                    .connectionTimeout(Duration.ofMillis(3000))\n"
                + "                    .maxConnections(2000))\n"
                + "            .overrideConfiguration(CONFIGURATION)\n"
                + "            .build();\n"
                + "\n"
                + "    public void test() {\n"
                + "        SqsClient sqs = SqsClient.builder()\n"
                + "                .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                        .connectionMaxIdleTime(Duration.ofMillis(6000))\n"
                + "                        .tcpKeepAlive(true)\n"
                + "                        .socketTimeout(Duration.ofMillis(4000))\n"
                + "                        .connectionTimeToLive(Duration.ofMillis(5000))\n"
                + "                        .connectionTimeout(Duration.ofMillis(3000))\n"
                + "                        .maxConnections(2000))\n"
                + "                .overrideConfiguration(CONFIGURATION)\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void sdkClientBuilderWithHttpSettings_noBuild_shouldRewrite() {
        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    private void method() {\n"
                + "        ClientConfiguration configuration = new ClientConfiguration().withMaxConnections(2000);\n"
                + "        AmazonSQSClientBuilder clientBuilder = AmazonSQSClient.builder();\n"
                + "        clientBuilder.withClientConfiguration(configuration);\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.apache.ApacheHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClientBuilder;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    private void method() {\n"
                + "        ClientOverrideConfiguration configuration = ClientOverrideConfiguration.builder()\n"
                + "                .build();\n"
                + "        SqsClientBuilder clientBuilder = SqsClient.builder();\n"
                + "        clientBuilder.httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                .maxConnections(2000)).overrideConfiguration(configuration);\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithHttpSettings_methodReference_shouldRemoveAndSetOnSdkClient() {

        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "\n"
                + "public class Example {\n"
                + "    public ClientConfiguration configuration() {\n"
                + "        return new ClientConfiguration()\n"
                + "            .withRequestTimeout(1000)\n"
                + "            .withTcpKeepAlive(true)\n"
                + "            .withMaxConnections(1000);\n"
                + "    }\n"
                + "\n"
                + "    public void test() {\n"
                + "        AmazonSQS sqs = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(configuration())\n"
                + "                                       .build();\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.apache.ApacheHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "    public ClientOverrideConfiguration configuration() {\n"
                + "        return ClientOverrideConfiguration.builder()\n"
                + "                .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "                .build();\n"
                + "    }\n"
                + "\n"
                + "    public void test() {\n"
                + "        SqsClient sqs = SqsClient.builder()\n"
                + "                .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                        .tcpKeepAlive(true)\n"
                + "                        .maxConnections(1000))\n"
                + "                .overrideConfiguration(configuration())\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithHttpSettings_asyncSdkClient_shouldRemoveAndSetOnSdkClient() {

        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSAsync;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSAsyncClient;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    public void test() {\n"
                + "        ClientConfiguration clientConfiguration = new ClientConfiguration()\n"
                + "            .withConnectionTimeout(1000)\n"
                + "            .withTcpKeepAlive(true)\n"
                + "            .withConnectionTTL(1000)\n"
                + "            .withRequestTimeout(1000)\n"
                + "            .withConnectionMaxIdleMillis(1000);\n"
                + "\n"
                + "        AmazonSQSAsync sqs = AmazonSQSAsyncClient.asyncBuilder()\n"
                + "                                                 .withClientConfiguration(clientConfiguration)\n"
                + "                                                 .build();\n"
                + "\n"
                + "\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsAsyncClient;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    public void test() {\n"
                + "        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder()\n"
                + "                .apiCallAttemptTimeout(Duration.ofMillis(1000))\n"
                + "                .build();\n"
                + "\n"
                + "        SqsAsyncClient sqs = SqsAsyncClient.asyncBuilder()\n"
                + "                .httpClientBuilder(NettyNioAsyncHttpClient.builder()\n"
                + "                        .connectionMaxIdleTime(Duration.ofMillis(1000))\n"
                + "                        .tcpKeepAlive(true)\n"
                + "                        .connectionTimeToLive(Duration.ofMillis(1000))\n"
                + "                        .connectionTimeout(Duration.ofMillis(1000)))\n"
                + "                .overrideConfiguration(clientConfiguration)\n"
                + "                .build();\n"
                + "\n"
                + "\n"
                + "    }\n"
                + "}"
            )
        );
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void multipleSdkClients_shouldSetHttpClientCorrectly() {

        rewriteRun(
            java(
                "import com.amazonaws.ClientConfiguration;\n"
                + "import com.amazonaws.services.sqs.AmazonSQS;\n"
                + "import com.amazonaws.services.sqs.AmazonSQSClient;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    public void test() {\n"
                + "        ClientConfiguration clientConfiguration1 = new ClientConfiguration()\n"
                + "            .withSocketTimeout(1000).withMaxConnections(100);\n"
                + "\n"
                + "        ClientConfiguration clientConfiguration2 = new ClientConfiguration()\n"
                + "            .withConnectionTimeout(2000);\n"
                + "\n"
                + "        AmazonSQS sqs1 = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(clientConfiguration1)\n"
                + "                                       .build();\n"
                + "\n"
                + "        AmazonSQS sqs2 = AmazonSQSClient.builder()\n"
                + "                                       .withClientConfiguration(clientConfiguration2)\n"
                + "                                       .build();\n"
                + "    }\n"
                + "}",
                "import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;\n"
                + "import software.amazon.awssdk.http.apache.ApacheHttpClient;\n"
                + "import software.amazon.awssdk.services.sqs.SqsClient;\n"
                + "\n"
                + "import java.time.Duration;\n"
                + "\n"
                + "public class Example {\n"
                + "\n"
                + "    public void test() {\n"
                + "        ClientOverrideConfiguration clientConfiguration1 = ClientOverrideConfiguration.builder()\n"
                + "                .build();\n"
                + "\n"
                + "        ClientOverrideConfiguration clientConfiguration2 = ClientOverrideConfiguration.builder()\n"
                + "                .build();\n"
                + "\n"
                + "        SqsClient sqs1 = SqsClient.builder()\n"
                + "                .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                        .socketTimeout(Duration.ofMillis(1000))\n"
                + "                        .maxConnections(100))\n"
                + "                .overrideConfiguration(clientConfiguration1)\n"
                + "                .build();\n"
                + "\n"
                + "        SqsClient sqs2 = SqsClient.builder()\n"
                + "                .httpClientBuilder(ApacheHttpClient.builder()\n"
                + "                        .connectionTimeout(Duration.ofMillis(2000)))\n"
                + "                .overrideConfiguration(clientConfiguration2)\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }


    @Test
    @EnabledOnJre({JRE.JAVA_8})
    void configurationWithUnsupportedSettings_shouldAddComment() {
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
                + "                .disableSocketProxy(true)\n"
                + "            /*AWS SDK for Java v2 migration: dnsResolver is not supported in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.dnsResolver(null)\n"
                + "            /*AWS SDK for Java v2 migration: gzip is not supported in v2 tracking in https://github.com/aws/aws-sdk-java-v2/issues/866. Consider removing it.*/.gzip(true)\n"
                + "            /*AWS SDK for Java v2 migration: localAddress is not supported in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.localAddress(null)\n"
                + "                .secureRandom(null)\n"
                + "                .throttledRetries(true)\n"
                + "                .protocol(null)\n"
                + "            /*AWS SDK for Java v2 migration: userAgent override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.userAgent(\"test\")\n"
                + "            /*AWS SDK for Java v2 migration: userAgentPrefix override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.userAgentPrefix(\"test\")\n"
                + "            /*AWS SDK for Java v2 migration: userAgentSuffix override is a request-level config in v2. See https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/core/RequestOverrideConfiguration.Builder.html#addApiName(software.amazon.awssdk.core.ApiName).*/.userAgentSuffix(\"test\")\n"
                + "            /*AWS SDK for Java v2 migration: useExpectContinue is removed in v2. Please submit a feature request https://github.com/aws/aws-sdk-java-v2/issues*/.useExpectContinue(false)\n"
                + "                .build();\n"
                + "    }\n"
                + "}"
            )
        );
    }
}
