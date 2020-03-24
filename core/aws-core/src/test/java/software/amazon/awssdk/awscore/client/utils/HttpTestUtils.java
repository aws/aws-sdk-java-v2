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

package software.amazon.awssdk.awscore.client.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
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
                                     .option(SdkClientOption.RETRY_POLICY, RetryPolicy.defaultRetryPolicy())
                                     .option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, new HashMap<>())
                                     .option(SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED, false)
                                     .option(SdkAdvancedClientOption.SIGNER, new NoOpSigner())
                                     .option(SdkAdvancedClientOption.USER_AGENT_PREFIX, "")
                                     .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "")
                                     .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                     .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE, Executors.newScheduledThreadPool(1))
                                     .build();
    }

    public static class TestClientBuilder {
        private RetryPolicy retryPolicy;
        private SdkHttpClient httpClient;

        public TestClientBuilder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public TestClientBuilder httpClient(SdkHttpClient sdkHttpClient) {
            this.httpClient = sdkHttpClient;
            return this;
        }

        public AmazonSyncHttpClient build() {
            SdkHttpClient sdkHttpClient = this.httpClient != null ? this.httpClient : testSdkHttpClient();
            return new AmazonSyncHttpClient(testClientConfiguration().toBuilder()
                                                                     .option(SdkClientOption.SYNC_HTTP_CLIENT, sdkHttpClient)
                                                                     .applyMutation(this::configureRetryPolicy)
                                                                     .build());
        }

        private void configureRetryPolicy(SdkClientConfiguration.Builder builder) {
            if (retryPolicy != null) {
                builder.option(SdkClientOption.RETRY_POLICY, retryPolicy);
            }
        }
    }
}
