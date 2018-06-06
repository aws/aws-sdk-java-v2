/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.config.SdkClientConfiguration;
import software.amazon.awssdk.core.config.options.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.config.options.SdkAdvancedClientOption;
import software.amazon.awssdk.core.config.options.SdkClientOption;
import software.amazon.awssdk.core.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;

public class HttpTestUtils {
    public static SdkHttpClient testSdkHttpClient() {
        return new DefaultSdkHttpClientBuilder().buildWithDefaults(
                AttributeMap.empty().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    public static AmazonSyncHttpClient testAmazonHttpClient() {
        return testClientBuilder().httpClient(testSdkHttpClient()).build();
    }

    public static TestClientBuilder testClientBuilder() {
        return new TestClientBuilder();
    }

    public static SdkClientConfiguration testClientConfiguration() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, new ArrayList<>())
                                     .option(SdkClientOption.ENDPOINT, URI.create("http://localhost:8080"))
                                     .option(SdkClientOption.RETRY_POLICY, RetryPolicy.DEFAULT)
                                     .option(SdkClientOption.GZIP_ENABLED, false)
                                     .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, new HashMap<>())
                                     .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                     .option(SdkAdvancedClientOption.SIGNER, new NoOpSigner())
                                     .option(SdkAdvancedClientOption.USER_AGENT_PREFIX, "")
                                     .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "")
                                     .option(SdkClientOption.ASYNC_RETRY_EXECUTOR_SERVICE, Executors.newScheduledThreadPool(1))
                                     .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR,
                                             Runnable::run)
                                     .build();
    }

    public static class TestClientBuilder {
        private RetryPolicy retryPolicy;
        private SdkHttpClient httpClient;
        private Map<String, String> additionalHeaders = new HashMap<>();

        public TestClientBuilder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public TestClientBuilder httpClient(SdkHttpClient sdkHttpClient) {
            this.httpClient = sdkHttpClient;
            return this;
        }

        public TestClientBuilder additionalHeader(String key, String value) {
            this.additionalHeaders.put(key, value);
            return this;
        }

        public AmazonSyncHttpClient build() {
            SdkHttpClient sdkHttpClient = this.httpClient != null ? this.httpClient : testSdkHttpClient();
            return new AmazonSyncHttpClient(testClientConfiguration().toBuilder()
                                                                     .option(SdkClientOption.SYNC_HTTP_CLIENT, sdkHttpClient)
                                                                     .apply(this::configureRetryPolicy)
                                                                     .apply(this::configureAdditionalHeaders)
                                                                     .build());
        }

        private void configureAdditionalHeaders(SdkClientConfiguration.Builder builder) {
            Map<String, List<String>> headers =
                    this.additionalHeaders.entrySet().stream()
                                          .collect(Collectors.toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));

            builder.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, headers);
        }

        private void configureRetryPolicy(SdkClientConfiguration.Builder builder) {
            if (retryPolicy != null) {
                builder.option(SdkClientOption.RETRY_POLICY, retryPolicy);
            }
        }
    }
}
