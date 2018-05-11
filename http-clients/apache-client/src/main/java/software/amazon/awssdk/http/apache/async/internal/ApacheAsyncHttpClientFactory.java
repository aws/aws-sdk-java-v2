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

package software.amazon.awssdk.http.apache.async.internal;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;

import software.amazon.awssdk.http.apache.internal.ApacheHttpClientFactory;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClientConfig;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClientFactory;
import software.amazon.awssdk.utils.AttributeMap;

public class ApacheAsyncHttpClientFactory implements SdkAsyncHttpClientFactory {
    private final ApacheHttpClientFactory httpClientFactory;

    private ApacheAsyncHttpClientFactory(BuilderImpl builder) {
        this.httpClientFactory = builder.apacheHttpClientFactory;
    }

    @Override
    public SdkAsyncHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults) {
        AttributeMap resolvedOptions = serviceDefaults.merge(GLOBAL_HTTP_DEFAULTS);

        ApacheSdkHttpClientConfig configuration = ApacheSdkHttpClientConfig.builder(resolvedOptions)
                .build();

        ConnectionManagerAwareHttpClient httpClient = httpClientFactory.create(configuration);

        return new ApacheAsyncHttpClient(httpClient, createRequestConfig(configuration), configuration);
    }

    private ApacheHttpRequestConfig createRequestConfig(ApacheSdkHttpClientConfig configuration) {
        ApacheHttpRequestConfig.Builder builder = ApacheHttpRequestConfig.builder();
        configuration.socketTimeout().ifPresent(builder::socketTimeout);
        configuration.connectionTimeout().ifPresent(builder::connectionTimeout);
        configuration.localAddress().ifPresent(builder::localAddress);
        builder.proxyConfiguration(configuration.proxyConfiguration());
        builder.expectContinueEnabled(configuration.expectContinueEnabled());
        return builder.build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder apacheHttpClientFactory(ApacheHttpClientFactory apacheHttpClientFactory);

        ApacheAsyncHttpClientFactory build();
    }

    private static class BuilderImpl implements Builder {
        private ApacheHttpClientFactory apacheHttpClientFactory;

        @Override
        public Builder apacheHttpClientFactory(ApacheHttpClientFactory apacheHttpClientFactory) {
            this.apacheHttpClientFactory = apacheHttpClientFactory;
            return this;
        }

        @Override
        public ApacheAsyncHttpClientFactory build() {
            return new ApacheAsyncHttpClientFactory(this);
        }
    }
}
