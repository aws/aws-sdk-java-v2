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

package software.amazon.awssdk.crtcore;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.utils.NumericUtils;

@SdkProtectedApi
public final class CrtConfigurationUtils {

    private CrtConfigurationUtils() {
    }

    public static Optional<HttpProxyOptions> resolveProxy(CrtProxyConfiguration proxyConfiguration,
                                                          TlsContext tlsContext) {
        if (proxyConfiguration == null) {
            return Optional.empty();
        }

        HttpProxyOptions clientProxyOptions = new HttpProxyOptions();

        clientProxyOptions.setHost(proxyConfiguration.host());
        clientProxyOptions.setPort(proxyConfiguration.port());

        if ("https".equalsIgnoreCase(proxyConfiguration.scheme())) {
            clientProxyOptions.setTlsContext(tlsContext);
        }

        if (proxyConfiguration.username() != null && proxyConfiguration.password() != null) {
            clientProxyOptions.setAuthorizationUsername(proxyConfiguration.username());
            clientProxyOptions.setAuthorizationPassword(proxyConfiguration.password());
            clientProxyOptions.setAuthorizationType(HttpProxyOptions.HttpProxyAuthorizationType.Basic);
        } else {
            clientProxyOptions.setAuthorizationType(HttpProxyOptions.HttpProxyAuthorizationType.None);
        }

        return Optional.of(clientProxyOptions);
    }

    public static Optional<HttpMonitoringOptions> resolveHttpMonitoringOptions(CrtConnectionHealthConfiguration config) {
        if (config == null) {
            return Optional.empty();
        }

        HttpMonitoringOptions httpMonitoringOptions = new HttpMonitoringOptions();
        httpMonitoringOptions.setMinThroughputBytesPerSecond(config.minimumThroughputInBps());
        int seconds = NumericUtils.saturatedCast(config.minimumThroughputTimeout().getSeconds());
        httpMonitoringOptions.setAllowableThroughputFailureIntervalSeconds(seconds);
        return Optional.of(httpMonitoringOptions);
    }

}
