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
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;
import software.amazon.awssdk.utils.NumericUtils;

@SdkInternalApi
public final class AwsCrtConfigurationUtils {

    private AwsCrtConfigurationUtils() {
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

    public static TlsCipherPreference resolveCipherPreference(Boolean postQuantumTlsEnabled) {
        // As of of v0.39.3, aws-crt-java prefers PQ by default, so only return the pre-PQ-default policy
        // below if the caller explicitly disables PQ by passing in false.
        if (Boolean.FALSE.equals(postQuantumTlsEnabled)
                && TlsCipherPreference.TLS_CIPHER_PREF_TLSv1_0_2023.isSupported()) {
            return TlsCipherPreference.TLS_CIPHER_PREF_TLSv1_0_2023;
        }
        return TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
    }

}
