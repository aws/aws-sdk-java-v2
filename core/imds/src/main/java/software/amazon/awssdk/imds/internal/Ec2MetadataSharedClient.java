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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataRetryPolicy;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Creates Ec2MetadataClient instances using a shared HTTP client internally.
 * This provides resource efficiency by sharing a single HTTP client across all IMDS-backed providers
 */
@SdkProtectedApi
public final class Ec2MetadataSharedClient {

    private static final Lock LOCK = new ReentrantLock();
    private static volatile SdkHttpClient sharedHttpClient;
    private static int referenceCount = 0;

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

    /**
     * Decrements the reference count and closes the shared HTTP client if no more references exist.
     */
    public static void decrementAndClose() {
        LOCK.lock();
        try {
            referenceCount--;
            if (referenceCount == 0 && sharedHttpClient != null) {
                sharedHttpClient.close();
                sharedHttpClient = null;
            }
        } finally {
            LOCK.unlock();
        }
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
            LOCK.lock();
            try {
                if (sharedHttpClient == null) {
                    sharedHttpClient = createImdsHttpClient();
                }

                referenceCount++;
                
                return DefaultEc2MetadataClientWithFallback.builder()
                                                           .httpClient(sharedHttpClient)
                                                           .retryPolicy(retryPolicy)
                                                           .build();
            } finally {
                LOCK.unlock();
            }
        }
    }
}
