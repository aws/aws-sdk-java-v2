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

package software.amazon.awssdk.core.config;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

/**
 * An implementation of {@link SdkClientConfiguration}, {@link SdkSyncClientConfiguration} and
 * {@link SdkAsyncClientConfiguration} that
 * provides fluent write and read methods for all configuration properties.
 *
 * <p>This class is mutable and not thread safe.</p>
 */
@SdkInternalApi
@SuppressWarnings("unchecked")
public class SdkMutableClientConfiguration<T extends SdkMutableClientConfiguration<T>>
    implements SdkSyncClientConfiguration, SdkAsyncClientConfiguration, Cloneable {

    // ClientConfiguration
    private ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder().build();
    private URI endpoint;
    private SdkHttpClient httpClient;
    private SdkAsyncHttpClient asyncHttpClient;

    // AsyncClientConfiguration
    private ScheduledExecutorService asyncExecutorService;

    @Override
    public ClientOverrideConfiguration overrideConfiguration() {
        return overrideConfiguration;
    }

    public T overrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        this.overrideConfiguration = overrideConfiguration;
        return (T) this;
    }

    @Override
    public URI endpoint() {
        return endpoint;
    }

    public T endpoint(URI endpoint) {
        this.endpoint = endpoint;
        return (T) this;
    }

    @Override
    public ScheduledExecutorService asyncExecutorService() {
        return asyncExecutorService;
    }

    public T asyncExecutorService(ScheduledExecutorService executorService) {
        this.asyncExecutorService = executorService;
        return (T) this;
    }

    @Override
    public SdkHttpClient httpClient() {
        return httpClient;
    }

    public T httpClient(SdkHttpClient sdkHttpClient) {
        this.httpClient = sdkHttpClient;
        return (T) this;
    }

    @Override
    public SdkAsyncHttpClient asyncHttpClient() {
        return asyncHttpClient;
    }

    public T asyncHttpClient(SdkAsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
        return (T) this;
    }

    @Override
    public SdkMutableClientConfiguration clone() {
        try {
            return (SdkMutableClientConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone not supported on cloneable object.", e);
        }
    }
}
