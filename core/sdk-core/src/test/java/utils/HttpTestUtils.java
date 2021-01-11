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

package utils;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkAsyncHttpClientBuilder;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

public class HttpTestUtils {
    public static SdkHttpClient testSdkHttpClient() {
        return new DefaultSdkHttpClientBuilder().buildWithDefaults(
                AttributeMap.empty().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    public static SdkAsyncHttpClient testSdkAsyncHttpClient() {
        return new DefaultSdkAsyncHttpClientBuilder().buildWithDefaults(
            AttributeMap.empty().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    public static AmazonSyncHttpClient testAmazonHttpClient() {
        return testClientBuilder().httpClient(testSdkHttpClient()).build();
    }

    public static AmazonAsyncHttpClient testAsyncHttpClient() {
        return new TestAsyncClientBuilder().asyncHttpClient(testSdkAsyncHttpClient()).build();
    }

    public static TestClientBuilder testClientBuilder() {
        return new TestClientBuilder();
    }

    public static TestAsyncClientBuilder testAsyncClientBuilder() {
        return new TestAsyncClientBuilder();
    }

    public static SdkClientConfiguration testClientConfiguration() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.EXECUTION_INTERCEPTORS, new ArrayList<>())
                                     .option(SdkClientOption.ENDPOINT, URI.create("http://localhost:8080"))
                                     .option(SdkClientOption.RETRY_POLICY, RetryPolicy.defaultRetryPolicy())
                                     .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, new HashMap<>())
                                     .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                     .option(SdkAdvancedClientOption.SIGNER, new NoOpSigner())
                                     .option(SdkAdvancedClientOption.USER_AGENT_PREFIX, "")
                                     .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "")
                                     .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, Executors.newScheduledThreadPool(1))
                                     .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                     .build();
    }

    public static class TestClientBuilder {
        private RetryPolicy retryPolicy;
        private SdkHttpClient httpClient;
        private Map<String, String> additionalHeaders = new HashMap<>();
        private Duration apiCallTimeout;
        private Duration apiCallAttemptTimeout;

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

        public TestClientBuilder apiCallTimeout(Duration duration) {
            this.apiCallTimeout = duration;
            return this;
        }

        public TestClientBuilder apiCallAttemptTimeout(Duration timeout) {
            this.apiCallAttemptTimeout = timeout;
            return this;
        }

        public AmazonSyncHttpClient build() {
            SdkHttpClient sdkHttpClient = this.httpClient != null ? this.httpClient : testSdkHttpClient();
            return new AmazonSyncHttpClient(testClientConfiguration().toBuilder()
                                                                     .option(SdkClientOption.SYNC_HTTP_CLIENT, sdkHttpClient)
                                                                     .applyMutation(this::configureRetryPolicy)
                                                                     .applyMutation(this::configureAdditionalHeaders)
                                                                     .option(SdkClientOption.API_CALL_TIMEOUT, apiCallTimeout)
                                                                     .option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, apiCallAttemptTimeout)
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

    public static class TestAsyncClientBuilder {
        private RetryPolicy retryPolicy;
        private SdkAsyncHttpClient asyncHttpClient;
        private Duration apiCallTimeout;
        private Duration apiCallAttemptTimeout;
        private Map<String, String> additionalHeaders = new HashMap<>();

        public TestAsyncClientBuilder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public TestAsyncClientBuilder asyncHttpClient(SdkAsyncHttpClient asyncHttpClient) {
            this.asyncHttpClient = asyncHttpClient;
            return this;
        }

        public TestAsyncClientBuilder additionalHeader(String key, String value) {
            this.additionalHeaders.put(key, value);
            return this;
        }

        public TestAsyncClientBuilder apiCallTimeout(Duration duration) {
            this.apiCallTimeout = duration;
            return this;
        }

        public TestAsyncClientBuilder apiCallAttemptTimeout(Duration timeout) {
            this.apiCallAttemptTimeout = timeout;
            return this;
        }

        public AmazonAsyncHttpClient build() {
            SdkAsyncHttpClient asyncHttpClient = this.asyncHttpClient != null ? this.asyncHttpClient : testSdkAsyncHttpClient();
            return new AmazonAsyncHttpClient(testClientConfiguration().toBuilder()
                                                                      .option(SdkClientOption.ASYNC_HTTP_CLIENT, asyncHttpClient)
                                                                      .option(SdkClientOption.API_CALL_TIMEOUT, apiCallTimeout)
                                                                      .option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, apiCallAttemptTimeout)
                                                                      .applyMutation(this::configureRetryPolicy)
                                                                      .applyMutation(this::configureAdditionalHeaders)
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
