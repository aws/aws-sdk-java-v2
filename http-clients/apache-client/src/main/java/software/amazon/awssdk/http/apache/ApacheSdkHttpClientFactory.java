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

package software.amazon.awssdk.http.apache;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS;

import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.apache.internal.ApacheHttpClientFactory;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClientConfig;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Factory for creating an instance of {@link SdkHttpClient}. The factory can be configured through the builder {@link
 * #builder()}, once built it  can create a {@link SdkHttpClient} via {@link #createHttpClient()} or can be passed to the SDK
 * client builders directly to have the SDK create and manage the HTTP client. See documentation on the service's respective
 * client builder for more information on configuring the HTTP layer.
 *
 * <pre class="brush: java">
 * SdkHttpClient httpClient = ApacheSdkHttpClientFactory.builder()
 * .socketTimeout(Duration.ofSeconds(10))
 * .build()
 * .createHttpClient();
 * </pre>
 */
public final class ApacheSdkHttpClientFactory implements SdkHttpClientFactory {
    private final ApacheHttpClientFactory apacheHttpClientFactory;
    private final ApacheSdkHttpClientConfig defaultConfig;

    private ApacheSdkHttpClientFactory(BuilderImpl builder) {
        apacheHttpClientFactory = builder.apacheHttpClientFactory;
        this.defaultConfig = builder.defaultConfig;
    }

    @Override
    public SdkHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults) {
        AttributeMap resolvedOptions = serviceDefaults.merge(GLOBAL_HTTP_DEFAULTS);
        ApacheSdkHttpClientConfig configuration = ApacheSdkHttpClientConfig.builder(resolvedOptions).build();
        return createHttpClient(configuration);
    }

    public SdkHttpClient createHttpClient() {
        return createHttpClient(defaultConfig);
    }

    public SdkHttpClient createHttpClient(ApacheSdkHttpClientConfig configuration) {
        ConnectionManagerAwareHttpClient httpClient = apacheHttpClientFactory.create(configuration);
        return new ApacheHttpClient(httpClient, configuration);
    }

    /**
     * @return Builder instance to construct a {@link ApacheSdkHttpClientFactory}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder apacheHttpClientFactory(ApacheHttpClientFactory apacheHttpClientFactory);

        Builder defaultConfig(ApacheSdkHttpClientConfig apacheSdkHttpClientConfig);

        ApacheSdkHttpClientFactory build();
    }

    private static class BuilderImpl implements Builder {
        private ApacheHttpClientFactory apacheHttpClientFactory;
        private ApacheSdkHttpClientConfig defaultConfig;

        @Override
        public Builder apacheHttpClientFactory(ApacheHttpClientFactory apacheHttpClientFactory) {
            this.apacheHttpClientFactory = apacheHttpClientFactory;
            return this;
        }

        @Override
        public Builder defaultConfig(ApacheSdkHttpClientConfig defaultConfig) {
            this.defaultConfig = defaultConfig;
            return this;
        }

        @Override
        public ApacheSdkHttpClientFactory build() {
            return new ApacheSdkHttpClientFactory(this);
        }
    }
}

