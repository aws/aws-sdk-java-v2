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

package software.amazon.awssdk.http.crt.internal;


import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.crt.io.TlsConnectionOptions;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.crt.io.TlsContextOptions;
import software.amazon.awssdk.crtcore.CrtConfigurationUtils;
import software.amazon.awssdk.http.crt.ConnectionHealthConfiguration;
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;
import software.amazon.awssdk.http.crt.TlsVersion;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;

@SdkInternalApi
public final class AwsCrtConfigurationUtils {
    private static final Logger log = Logger.loggerFor(AwsCrtConfigurationUtils.class);

    // CRT rejects a throughput-failure interval below two seconds (HttpMonitoringOptions).
    private static final int MIN_MONITORING_FAILURE_INTERVAL_SECONDS = 2;

    private AwsCrtConfigurationUtils() {
    }

    /**
     * Resolves the throughput monitor that enforces a connection's read/write inactivity timeout, highest precedence first:
     * <ol>
     *   <li>an explicit {@link ConnectionHealthConfiguration} always wins;</li>
     *   <li>otherwise the SDK-resolved {@code fallbackTimeout}: {@link Duration#ZERO} applies nothing, a positive value is
     *   mapped.</li>
     * </ol>
     * The fallback is only ever supplied to an SDK-managed client.
     */
    public static HttpMonitoringOptions resolveMonitoringOptions(ConnectionHealthConfiguration healthConfiguration,
                                                                 Duration fallbackTimeout) {
        if (healthConfiguration != null) {
            return CrtConfigurationUtils.resolveHttpMonitoringOptions(healthConfiguration).orElse(null);
        }

        if (fallbackTimeout == null || fallbackTimeout.isZero()) {
            return null;
        }
        return mapReadWriteTimeout(fallbackTimeout);
    }

    /**
     * Maps a read/write inactivity timeout onto the CRT throughput monitor that enforces it: a minimum throughput of one byte
     * per second measured over a failure interval of {@code readWriteTimeout}. Because the threshold is a single byte, any byte
     * moved while a stream is pending resets the interval, so the connection is shut down only after {@code readWriteTimeout} of
     * continuous zero-byte progress. The interval has whole-second granularity, and CRT rejects an interval below two seconds, so
     * a shorter timeout is raised to that floor.
     */
    public static HttpMonitoringOptions mapReadWriteTimeout(Duration readWriteTimeout) {
        HttpMonitoringOptions httpMonitoringOptions = new HttpMonitoringOptions();
        httpMonitoringOptions.setMinThroughputBytesPerSecond(1);
        int seconds = Math.max(MIN_MONITORING_FAILURE_INTERVAL_SECONDS,
                               NumericUtils.saturatedCast(readWriteTimeout.getSeconds()));
        httpMonitoringOptions.setAllowableThroughputFailureIntervalSeconds(seconds);
        return httpMonitoringOptions;
    }

    public static SocketOptions buildSocketOptions(TcpKeepAliveConfiguration tcpKeepAliveConfiguration,
                                                   Duration connectionTimeout) {
        SocketOptions clientSocketOptions = new SocketOptions();

        if (connectionTimeout != null) {
            clientSocketOptions.connectTimeoutMs = NumericUtils.saturatedCast(connectionTimeout.toMillis());
        }

        if (tcpKeepAliveConfiguration != null) {
            clientSocketOptions.keepAlive = true;
            clientSocketOptions.keepAliveIntervalSecs =
                NumericUtils.saturatedCast(tcpKeepAliveConfiguration.keepAliveInterval().getSeconds());
            clientSocketOptions.keepAliveTimeoutSecs =
                NumericUtils.saturatedCast(tcpKeepAliveConfiguration.keepAliveTimeout().getSeconds());
            if (tcpKeepAliveConfiguration.keepAliveProbes() != null) {
                clientSocketOptions.keepAliveMaxFailedProbes = tcpKeepAliveConfiguration.keepAliveProbes();
            }
        }

        return clientSocketOptions;
    }

    public static TlsConnectionOptions buildTlsConnectionOptions(TlsContext tlsContext, Duration tlsNegotiationTimeout,
                                                                 String serverName) {
        TlsConnectionOptions tlsConnectionOptions = new TlsConnectionOptions(tlsContext);
        if (tlsNegotiationTimeout != null) {
            tlsConnectionOptions.withTimeoutMs(NumericUtils.saturatedCast(tlsNegotiationTimeout.toMillis()));
        }
        if (serverName != null) {
            tlsConnectionOptions.withServerName(serverName);
        }
        return tlsConnectionOptions;
    }

    public static TlsCipherPreference resolveCipherPreference(Boolean postQuantumTlsEnabled) {
        // As of v0.39.3, aws-crt-java prefers PQ by default, so only return the non-PQ-default policy
        // below if the caller explicitly disables PQ by passing in false.
        if (Boolean.FALSE.equals(postQuantumTlsEnabled)) {
            if (TlsCipherPreference.TLS_CIPHER_NON_PQ_DEFAULT.isSupported()) {
                return TlsCipherPreference.TLS_CIPHER_NON_PQ_DEFAULT;
            }
            log.warn(() -> "Post-quantum TLS was explicitly disabled but TLS_CIPHER_NON_PQ_DEFAULT is not supported. "
                           + "Falling back to TLS_CIPHER_SYSTEM_DEFAULT.");
        }
        return TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
    }

    /**
     * Translate the SDK-owned {@link TlsVersion} into the CRT-native {@link TlsContextOptions.TlsVersions}
     */
    public static TlsContextOptions.TlsVersions resolveMinTlsVersion(TlsVersion minTlsVersion) {
        if (minTlsVersion == null) {
            return TlsContextOptions.TlsVersions.TLS_VER_SYS_DEFAULTS;
        }
        switch (minTlsVersion) {
            case TLS_1_3:
                return TlsContextOptions.TlsVersions.TLSv1_3;
            case SYSTEM_DEFAULT:
                return TlsContextOptions.TlsVersions.TLS_VER_SYS_DEFAULTS;
            default:
                throw new IllegalArgumentException("Unsupported minTlsVersion: " + minTlsVersion);
        }
    }

}
