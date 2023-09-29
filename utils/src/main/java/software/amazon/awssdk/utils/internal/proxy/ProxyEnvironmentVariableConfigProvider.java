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

package software.amazon.awssdk.utils.internal.proxy;

import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsEnvironmentVariable;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.parseNonProxyHostsProperty;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ProxyConfigProvider;
import software.amazon.awssdk.utils.ProxyEnvironmentSetting;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * The system properties related to http and https proxies
 */
@SdkInternalApi
public class ProxyEnvironmentVariableConfigProvider implements ProxyConfigProvider {

    private static final Logger log = Logger.loggerFor(ProxyEnvironmentVariableConfigProvider.class);

    private final String scheme;

    private final URL proxyUrl;

    public ProxyEnvironmentVariableConfigProvider(String scheme) {
        this.scheme = scheme == null ? "http" : scheme;
        this.proxyUrl = silentlyGetURL().orElse(null);
    }


    private Optional<URL> silentlyGetURL(){
        String stringURL = Objects.equals(this.scheme, HTTPS) ? ProxyEnvironmentSetting.HTTPS_PROXY.getStringValue().orElse(null)
                                                           : ProxyEnvironmentSetting.HTTP_PROXY.getStringValue().orElse(null);
        if(StringUtils.isNotBlank(stringURL)){
            try {
                return Optional.of(new URL(stringURL));
            } catch (MalformedURLException e) {
                log.error(()->"Malformed environment  url string " + stringURL, e);
            }
        }
        return Optional.empty();
    }

    @Override
    public int port() {
        return Optional.ofNullable(this.proxyUrl)
                       .map(URL::getPort)
                       .orElse(0);
    }

    @Override
    public Optional<String> userName() {
        return Optional.ofNullable(this.proxyUrl)
                       .map(URL::getUserInfo)
                       .flatMap(userInfo -> Optional.ofNullable(userInfo.split(":", 2)[0]));
    }

    @Override
    public Optional<String> password() {
        return Optional.ofNullable(this.proxyUrl)
                       .map(URL::getUserInfo)
                       .filter(userInfo -> userInfo.contains(":"))
                       .map(userInfo -> userInfo.split(":", 2))
                       .filter(parts -> parts.length > 1)
                       .map(parts -> parts[1]);
    }

    @Override
    public String host() {
        return Optional.ofNullable(this.proxyUrl).map(URL::getHost).orElse(null);
    }

    @Override
    public Set<String> nonProxyHosts() {
        return parseNonProxyHostsEnvironmentVariable() ;
    }
}
