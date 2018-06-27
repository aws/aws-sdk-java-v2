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

package software.amazon.awssdk.http.apache.internal;

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.util.Set;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * SdkProxyRoutePlanner delegates a Proxy Route Planner from the settings instead of the
 * system properties. It will use the proxy created from proxyHost and proxyPort and
 * filter the hosts who matches nonProxyHosts pattern.
 */
@SdkInternalApi
public class SdkProxyRoutePlanner extends DefaultRoutePlanner {

    private HttpHost proxy;
    private Set<String> hostPatterns;

    public SdkProxyRoutePlanner(String proxyHost, int proxyPort, Set<String> nonProxyHosts) {
        super(DefaultSchemePortResolver.INSTANCE);
        proxy = new HttpHost(proxyHost, proxyPort);
        this.hostPatterns = nonProxyHosts;
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
    protected HttpHost determineProxy(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws HttpException {

        return doesTargetMatchNonProxyHosts(target) ? null : proxy;
    }
}
