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
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;

@SdkInternalApi
public final class AwsCrtConfigurationUtils {
    private static final Logger log = Logger.loggerFor(AwsCrtAsyncHttpClient.class);

    private AwsCrtConfigurationUtils() {
    }

    public static SocketOptions buildSocketOptions(TcpKeepAliveConfiguration tcpKeepAliveConfiguration,
                                                   Duration connectionTimeout) {
        SocketOptions clientSocketOptions = new SocketOptions();

        if (connectionTimeout != null) {
            clientSocketOptions.connectTimeoutMs = NumericUtils.saturatedCast(connectionTimeout.toMillis());
        }

        if (tcpKeepAliveConfiguration != null) {
            clientSocketOptions.keepAliveIntervalSecs =
                NumericUtils.saturatedCast(tcpKeepAliveConfiguration.keepAliveInterval().getSeconds());
            clientSocketOptions.keepAliveTimeoutSecs =
                NumericUtils.saturatedCast(tcpKeepAliveConfiguration.keepAliveTimeout().getSeconds());

        }

        return clientSocketOptions;
    }

    public static TlsCipherPreference resolveCipherPreference(Boolean postQuantumTlsEnabled) {
        TlsCipherPreference defaultTls = TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
        if (postQuantumTlsEnabled == null || !postQuantumTlsEnabled) {
            return defaultTls;
        }

        // TODO: change this to the new PQ TLS Policy that stays up to date when it's ready
        TlsCipherPreference pqTls = TlsCipherPreference.TLS_CIPHER_PREF_PQ_TLSv1_0_2021_05;
        if (!pqTls.isSupported()) {
            log.warn(() -> "Hybrid post-quantum cipher suites are not supported on this platform. The SDK will use the system "
                           + "default cipher suites instead");
            return defaultTls;
        }

        return pqTls;
    }

}
