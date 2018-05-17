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

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.USE_STRICT_HOSTNAME_VERIFICATION;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Helper class to unwrap and convert service defaults.
 */
@Immutable
public final class NettyConfiguration {
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(60);
    private static final Integer MAX_PENDING_ACQUIRE = 10_000;
    private final AttributeMap serviceDefaults;
    private final NettySdkHttpClientFactory factory;

    NettyConfiguration(AttributeMap serviceDefaults, NettySdkHttpClientFactory factory) {
        this.serviceDefaults = serviceDefaults;
        this.factory = factory;
    }

    /**
     * @return the timeout in milliseconds
     * @see NettySdkHttpClientFactory.Builder#connectionTimeout(Duration)
     */
    public int connectionTimeout() {
        return saturatedCast(serviceDefaults.get(CONNECTION_TIMEOUT).toMillis());
    }

    /**
     * @see NettySdkHttpClientFactory.Builder#maxConnectionsPerEndpoint(Integer)
     */
    public int maxConnectionsPerEndpoint() {
        return serviceDefaults.get(MAX_CONNECTIONS);
    }

    /**
     * @see NettySdkHttpClientFactory.Builder#maxPendingAcquires(Integer)
     */
    public int maxPendingAcquires() {
        return factory.maxPendingAcquires().orElse(MAX_PENDING_ACQUIRE);
    }

    @ReviewBeforeRelease("Support disabling strict hostname verification")
    public <T> Optional<T> getConfigurationValue(AttributeMap.Key<T> key) {
        return key == USE_STRICT_HOSTNAME_VERIFICATION ? Optional.empty() : Optional.ofNullable(serviceDefaults.get(key));
    }

    /**
     * @return the timeout in seconds
     * @see NettySdkHttpClientFactory.Builder#readTimeout(Duration)
     */
    public int readTimeout() {
        return saturatedCast(factory.readTimeout().orElse(DEFAULT_READ_TIMEOUT).getSeconds());
    }

    /**
     * @return the timeout in seconds
     * @see NettySdkHttpClientFactory.Builder#writeTimeout(Duration)
     */
    public int writeTimeout() {
        return saturatedCast(factory.writeTimeout().orElse(DEFAULT_WRITE_TIMEOUT).getSeconds());
    }

    /**
     * @return the timeout in milliseconds
     * @see NettySdkHttpClientFactory.Builder#connectionAcquisitionTimeout(Duration)
     */
    public int connectionAcquisitionTimeout() {
        return factory.connectionAcquisitionTimeout().map(d -> saturatedCast(d.toMillis())).orElseGet(this::connectionTimeout);
    }

    /**
     * @see NettySdkHttpClientFactory.Builder#trustAllCertificates(Boolean)
     */
    public boolean trustAllCertificates() {
        return factory.trustAllCertificates().orElse(Boolean.FALSE);
    }
}
