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

package software.amazon.awssdk.http.nio.netty.internal;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal object for configuring netty.
 */
@SdkInternalApi
public final class NettyConfiguration {
    private final AttributeMap configuration;

    public NettyConfiguration(AttributeMap configuration) {
        this.configuration = configuration;
    }

    public <T> T attribute(AttributeMap.Key<T> key) {
        return configuration.get(key);
    }

    public int connectTimeoutMillis() {
        return saturatedCast(configuration.get(CONNECTION_TIMEOUT).toMillis());
    }

    public int connectionAcquireTimeoutMillis() {
        return saturatedCast(configuration.get(CONNECTION_ACQUIRE_TIMEOUT).toMillis());
    }

    public int maxConnections() {
        return configuration.get(MAX_CONNECTIONS);
    }

    public int maxPendingConnectionAcquires() {
        return configuration.get(MAX_PENDING_CONNECTION_ACQUIRES);
    }

    public boolean trustAllCertificates() {
        return configuration.get(TRUST_ALL_CERTIFICATES);
    }

    public int readTimeoutMillis() {
        return saturatedCast(configuration.get(SdkHttpConfigurationOption.READ_TIMEOUT).toMillis());
    }

    public int writeTimeoutMillis() {
        return saturatedCast(configuration.get(SdkHttpConfigurationOption.WRITE_TIMEOUT).toMillis());
    }
}
