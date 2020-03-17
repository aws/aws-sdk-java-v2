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

package software.amazon.awssdk.http.nio.netty.internal;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_ACQUIRE_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_PENDING_CONNECTION_ACQUIRES;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Internal object for configuring netty.
 */
@SdkInternalApi
public final class NettyConfiguration {

    public static final int CHANNEL_POOL_CLOSE_TIMEOUT_SECONDS = 5;
    public static final int EVENTLOOP_SHUTDOWN_QUIET_PERIOD_SECONDS = 2;
    public static final int EVENTLOOP_SHUTDOWN_TIMEOUT_SECONDS = 15;
    public static final int EVENTLOOP_SHUTDOWN_FUTURE_TIMEOUT_SECONDS = 16;
    public static final int HTTP2_CONNECTION_PING_TIMEOUT_SECONDS = 5;

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

    public int readTimeoutMillis() {
        return saturatedCast(configuration.get(SdkHttpConfigurationOption.READ_TIMEOUT).toMillis());
    }

    public int writeTimeoutMillis() {
        return saturatedCast(configuration.get(SdkHttpConfigurationOption.WRITE_TIMEOUT).toMillis());
    }

    public int idleTimeoutMillis() {
        return saturatedCast(configuration.get(SdkHttpConfigurationOption.CONNECTION_MAX_IDLE_TIMEOUT).toMillis());
    }

    public int connectionTtlMillis() {
        return saturatedCast(configuration.get(SdkHttpConfigurationOption.CONNECTION_TIME_TO_LIVE).toMillis());
    }

    public boolean reapIdleConnections() {
        return configuration.get(SdkHttpConfigurationOption.REAP_IDLE_CONNECTIONS);
    }

    public TlsKeyManagersProvider tlsKeyManagersProvider() {
        return configuration.get(SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER);
    }

    public TlsTrustManagersProvider tlsTrustManagersProvider() {
        return configuration.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER);
    }

    public boolean trustAllCertificates() {
        return configuration.get(TRUST_ALL_CERTIFICATES);
    }
}
