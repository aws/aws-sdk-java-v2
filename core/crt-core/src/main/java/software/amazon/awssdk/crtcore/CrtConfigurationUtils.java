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

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.crt.http.HttpMonitoringOptions;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.NumericUtils;

@SdkProtectedApi
public final class CrtConfigurationUtils {

    private static final Logger log = Logger.loggerFor(CrtConfigurationUtils.class);

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
        if (!proxyConfiguration.nonProxyHosts().isEmpty()) {
            proxyConfiguration.nonProxyHosts().stream()
                              .filter(Objects::nonNull)
                              .filter(CrtConfigurationUtils::isUnsupportedWildcard)
                              .forEach(CrtConfigurationUtils::warnUnsupportedWildcard);
            String noProxyHosts = proxyConfiguration.nonProxyHosts().stream()
                                                    .filter(Objects::nonNull)
                                                    .map(CrtConfigurationUtils::toCurlNoProxyHost)
                                                    .collect(Collectors.joining(","));
            clientProxyOptions.setNoProxyHosts(noProxyHosts);
        }

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

    /**
     * Translates a {@code nonProxyHosts} token to the form expected by the curl-style native matcher: a host name with a
     * leading {@code *.} wildcard ({@code *.example.com}) becomes a dot-anchored suffix ({@code .example.com}), while a bare
     * {@code *}, exact host names, and CIDR ranges are passed through unchanged. A wildcard in any other position is
     * unsupported (see {@link #isUnsupportedWildcard}) and passed through unchanged, so the native matcher will not match it.
     */
    private static String toCurlNoProxyHost(String nonProxyHost) {
        if (nonProxyHost.startsWith("*.")) {
            return nonProxyHost.substring(1);
        }
        return nonProxyHost;
    }

    private static boolean isUnsupportedWildcard(String nonProxyHost) {
        return nonProxyHost.contains("*") && !nonProxyHost.equals("*") && !nonProxyHost.startsWith("*.");
    }

    private static void warnUnsupportedWildcard(String nonProxyHost) {
        log.warn(() -> "Unsupported wildcard in nonProxyHosts entry '" + nonProxyHost + "' for the CRT-based HTTP client: only "
                       + "an exact host, a leading '*.' suffix wildcard (e.g. *.example.com), a single '*', or a CIDR range "
                       + "are supported. This entry is ignored, so requests to matching hosts will go through the proxy.");
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
