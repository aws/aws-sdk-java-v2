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

package software.amazon.awssdk.imds.internal;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Lazy;

/**
 * Creates Ec2MetadataClient instances using a shared HTTP client internally.
 * This provides resource efficiency by sharing a single HTTP client across all IMDS-backed providers
 */
@SdkInternalApi
public final class Ec2MetadataSharedClient {
    // Singleton HTTP client shared across all Ec2MetadataClient instances
    private static final Lazy<SdkHttpClient> SHARED_HTTP_CLIENT = new Lazy<>(() -> createImdsHttpClient());
    
    private Ec2MetadataSharedClient() {
        // Prevent instantiation
    }
    
    /**
     * Creates a builder for configuring Ec2MetadataClient with shared HTTP client.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a new Ec2MetadataClient instance using the shared HTTP client
     * with default configuration.
     */
    public static Ec2MetadataClient create() {
        return builder().build();
    }
    
    private static SdkHttpClient createImdsHttpClient() {
        Duration metadataServiceTimeout = Ec2MetadataConfigProvider.instance().resolveServiceTimeout();
        AttributeMap imdsHttpDefaults = AttributeMap.builder()
                .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, metadataServiceTimeout)
                .put(SdkHttpConfigurationOption.READ_TIMEOUT, metadataServiceTimeout)
                .build();

        return new DefaultSdkHttpClientBuilder().buildWithDefaults(imdsHttpDefaults);
    }
    
    public static final class Builder {
        private Ec2MetadataRetryPolicy retryPolicy;
        
        private Builder() {
        }
        
        public Builder retryPolicy(Ec2MetadataRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }
        
        public Ec2MetadataClient build() {
            return DefaultEc2MetadataClientWithFallback.builder()
                .httpClient(SHARED_HTTP_CLIENT.getValue())
                .retryPolicy(retryPolicy)
                .build();
        }
    }
}
