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

package software.amazon.awssdk.http;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Type safe key for an HTTP related configuration option. These options are used for service specific configuration
 * and are treated as hints for the underlying HTTP implementation for better defaults. If an implementation does not support
 * a particular option, they are free to ignore it.
 *
 * @param <T> Type of option
 * @see AttributeMap
 */
@SdkProtectedApi
public final class SdkHttpConfigurationOption<T> extends AttributeMap.Key<T> {
    /**
     * Timeout for each read to the underlying socket.
     */
    public static final SdkHttpConfigurationOption<Duration> READ_TIMEOUT =
            new SdkHttpConfigurationOption<>("ReadTimeout", Duration.class);

    /**
     * Timeout for each write to the underlying socket.
     */
    public static final SdkHttpConfigurationOption<Duration> WRITE_TIMEOUT =
            new SdkHttpConfigurationOption<>("WriteTimeout", Duration.class);

    /**
     * Timeout for establishing a connection to a remote service.
     */
    public static final SdkHttpConfigurationOption<Duration> CONNECTION_TIMEOUT =
            new SdkHttpConfigurationOption<>("ConnectionTimeout", Duration.class);

    /**
     * Timeout for acquiring an already-established connection from a connection pool to a remote service.
     */
    public static final SdkHttpConfigurationOption<Duration> CONNECTION_ACQUIRE_TIMEOUT =
            new SdkHttpConfigurationOption<>("ConnectionAcquireTimeout", Duration.class);

    /**
     * Timeout after which an idle connection should be closed.
     */
    public static final SdkHttpConfigurationOption<Duration> CONNECTION_MAX_IDLE_TIMEOUT =
            new SdkHttpConfigurationOption<>("ConnectionMaxIdleTimeout", Duration.class);

    /**
     * Timeout after which a connection should be closed, regardless of whether it is idle. Zero indicates an infinite amount
     * of time.
     */
    public static final SdkHttpConfigurationOption<Duration> CONNECTION_TIME_TO_LIVE =
            new SdkHttpConfigurationOption<>("ConnectionTimeToLive", Duration.class);

    /**
     * Maximum number of connections allowed in a connection pool.
     */
    public static final SdkHttpConfigurationOption<Integer> MAX_CONNECTIONS =
            new SdkHttpConfigurationOption<>("MaxConnections", Integer.class);

    /**
     * HTTP protocol to use.
     */
    public static final SdkHttpConfigurationOption<Protocol> PROTOCOL =
        new SdkHttpConfigurationOption<>("Protocol", Protocol.class);

    /**
     * Maximum number of requests allowed to wait for a connection.
     */
    public static final SdkHttpConfigurationOption<Integer> MAX_PENDING_CONNECTION_ACQUIRES =
            new SdkHttpConfigurationOption<>("MaxConnectionAcquires", Integer.class);

    /**
     * Whether idle connection should be removed after the {@link #CONNECTION_MAX_IDLE_TIMEOUT} has passed.
     */
    public static final SdkHttpConfigurationOption<Boolean> REAP_IDLE_CONNECTIONS =
            new SdkHttpConfigurationOption<>("ReapIdleConnections", Boolean.class);

    /**
     * The {@link TlsKeyManagersProvider} that will be used by the HTTP client when authenticating with a
     * TLS host.
     */
    public static final SdkHttpConfigurationOption<TlsKeyManagersProvider> TLS_KEY_MANAGERS_PROVIDER =
            new SdkHttpConfigurationOption<>("TlsKeyManagersProvider", TlsKeyManagersProvider.class);

    /**
     * Option to disable SSL cert validation and SSL host name verification. By default, this option is off.
     * Only enable this option for testing purposes.
     */
    public static final SdkHttpConfigurationOption<Boolean> TRUST_ALL_CERTIFICATES =
        new SdkHttpConfigurationOption<>("TrustAllCertificates", Boolean.class);

    /**
     * The {@link TlsTrustManagersProvider} that will be used by the HTTP client when authenticating with a
     * TLS host.
     */
    public static final SdkHttpConfigurationOption<TlsTrustManagersProvider> TLS_TRUST_MANAGERS_PROVIDER =
        new SdkHttpConfigurationOption<>("TlsTrustManagersProvider", TlsTrustManagersProvider.class);

    private static final Duration DEFAULT_SOCKET_READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_SOCKET_WRITE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_CONNECTION_ACQUIRE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_CONNECTION_MAX_IDLE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration DEFAULT_CONNECTION_TIME_TO_LIVE = Duration.ZERO;
    private static final Boolean DEFAULT_REAP_IDLE_CONNECTIONS = Boolean.TRUE;
    private static final int DEFAULT_MAX_CONNECTIONS = 50;
    private static final int DEFAULT_MAX_CONNECTION_ACQUIRES = 10_000;
    private static final Boolean DEFAULT_TRUST_ALL_CERTIFICATES = Boolean.FALSE;

    private static final Protocol DEFAULT_PROTOCOL = Protocol.HTTP1_1;

    private static final TlsTrustManagersProvider DEFAULT_TLS_TRUST_MANAGERS_PROVIDER = null;
    private static final TlsKeyManagersProvider DEFAULT_TLS_KEY_MANAGERS_PROVIDER = SystemPropertyTlsKeyManagersProvider.create();

    public static final AttributeMap GLOBAL_HTTP_DEFAULTS = AttributeMap
            .builder()
            .put(READ_TIMEOUT, DEFAULT_SOCKET_READ_TIMEOUT)
            .put(WRITE_TIMEOUT, DEFAULT_SOCKET_WRITE_TIMEOUT)
            .put(CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT)
            .put(CONNECTION_ACQUIRE_TIMEOUT, DEFAULT_CONNECTION_ACQUIRE_TIMEOUT)
            .put(CONNECTION_MAX_IDLE_TIMEOUT, DEFAULT_CONNECTION_MAX_IDLE_TIMEOUT)
            .put(CONNECTION_TIME_TO_LIVE, DEFAULT_CONNECTION_TIME_TO_LIVE)
            .put(MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS)
            .put(MAX_PENDING_CONNECTION_ACQUIRES, DEFAULT_MAX_CONNECTION_ACQUIRES)
            .put(PROTOCOL, DEFAULT_PROTOCOL)
            .put(TRUST_ALL_CERTIFICATES, DEFAULT_TRUST_ALL_CERTIFICATES)
            .put(REAP_IDLE_CONNECTIONS, DEFAULT_REAP_IDLE_CONNECTIONS)
            .put(TLS_KEY_MANAGERS_PROVIDER, DEFAULT_TLS_KEY_MANAGERS_PROVIDER)
            .put(TLS_TRUST_MANAGERS_PROVIDER, DEFAULT_TLS_TRUST_MANAGERS_PROVIDER)
            .build();

    private final String name;

    private SdkHttpConfigurationOption(String name, Class<T> clzz) {
        super(clzz);
        this.name = name;
    }

    /**
     * Note that the name is mainly used for debugging purposes. Two option key objects with the same name do not represent
     * the same option. Option keys are compared by reference when obtaining a value from an {@link AttributeMap}.
     *
     * @return Name of this option key.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

