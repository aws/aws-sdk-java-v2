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

package software.amazon.awssdk.awscore.client.utils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.SdkMutableClientConfiguration;
import software.amazon.awssdk.core.config.defaults.GlobalClientConfigurationDefaults;
import software.amazon.awssdk.core.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.http.loader.DefaultSdkHttpClientFactory;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;

public class HttpTestUtils {
    public static SdkHttpClient testSdkHttpClient() {
        return new DefaultSdkHttpClientFactory().createHttpClientWithDefaults(
                AttributeMap.empty().merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    public static AmazonSyncHttpClient testAmazonHttpClient() {
        return testClientBuilder().httpClient(testSdkHttpClient()).build();
    }

    public static TestClientBuilder testClientBuilder() {
        return new TestClientBuilder();
    }

    public static class TestClientBuilder {
        private RetryPolicy retryPolicy;
        private SdkHttpClient httpClient;
        private Map<String, String> additionalHeaders = new HashMap<>();
        private Duration clientExecutionTimeout = Duration.ofSeconds(5);

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

        public TestClientBuilder clientExecutionTimeout(Duration clientExecutionTimeout) {
            this.clientExecutionTimeout = clientExecutionTimeout;
            return this;
        }

        public AmazonSyncHttpClient build() {
            SdkHttpClient sdkHttpClient = this.httpClient != null ? this.httpClient : testSdkHttpClient();
            ClientOverrideConfiguration overrideConfiguration =
                    ClientOverrideConfiguration.builder()
                                               .totalExecutionTimeout(clientExecutionTimeout)
                                               .apply(this::configureRetryPolicy)
                                               .apply(this::configureAdditionalHeaders)
                                               .build();

            SdkMutableClientConfiguration clientConfig = new SdkMutableClientConfiguration()
                    .httpClient(sdkHttpClient)
                    .overrideConfiguration(overrideConfiguration);

            new GlobalClientConfigurationDefaults().applySyncDefaults(clientConfig);

            return new AmazonSyncHttpClient(clientConfig);
        }

        private ClientOverrideConfiguration.Builder configureAdditionalHeaders(ClientOverrideConfiguration.Builder builder) {
            this.additionalHeaders.forEach(builder::addAdditionalHttpHeader);
            return builder;
        }

        private ClientOverrideConfiguration.Builder configureRetryPolicy(ClientOverrideConfiguration.Builder builder) {
            if (retryPolicy != null) {
                builder.retryPolicy(retryPolicy);
            }
            return builder;
        }
    }
}
