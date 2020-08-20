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

package software.amazon.awssdk.http.apache.async;

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.net.InetAddress;
import java.util.Set;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * SdkProxyRoutePlanner delegates a Proxy Route Planner from the settings instead of the
 * system properties. It will use the proxy created from proxyHost and proxyPort and
 * filter the hosts who matches nonProxyHosts pattern.
 */
@SdkInternalApi
class SdkProxyRoutePlanner extends DefaultRoutePlanner {
    private final HttpHost proxy;
    private final Set<String> hostPatterns;
    private final InetAddress localAddress;

    SdkProxyRoutePlanner(
        String proxyHost,
        int proxyPort,
        String proxyScheme,
        Set<String> nonProxyHosts,
        InetAddress localAddress
    ) {
        super(DefaultSchemePortResolver.INSTANCE);
        this.proxy = new HttpHost(proxyScheme, proxyHost, proxyPort);
        this.hostPatterns = nonProxyHosts;
        this.localAddress = localAddress;
    }

    private boolean doesTargetMatchNonProxyHosts(HttpHost target) {
        if (hostPatterns == null) {
            return false;
        }
        String targetHost = lowerCase(target.getHostName());
        for (String pattern : hostPatterns) {
            if (targetHost.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpContext context) {
        return doesTargetMatchNonProxyHosts(target) ? null : proxy;
    }

    @Override
    protected InetAddress determineLocalAddress(HttpHost firstHop, HttpContext context) {
        return localAddress;
    }
}
