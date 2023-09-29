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

package software.amazon.awssdk.utils;

import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.internal.proxy.ProxyEnvironmentVariableConfigProvider;
import software.amazon.awssdk.utils.internal.proxy.ProxySystemPropertyConfigProvider;


@SdkProtectedApi
public interface ProxyConfigProvider {

    String HTTPS = "https";

    int port();

    Optional<String> userName();

    Optional<String> password();

    String host();


    Set<String> nonProxyHosts();

    static ProxyConfigProvider fromSystemPropertySettings(String scheme){
        return new ProxySystemPropertyConfigProvider(scheme);
    }

    static ProxyConfigProvider fromEnvironmentSettings(String scheme){
        return new ProxyEnvironmentVariableConfigProvider(scheme);
    }


    static ProxyConfigProvider getProxyConfig(
        Boolean useSystemPropertyValues, Boolean useEnvironmentVariableValues
        , String scheme) {
        ProxyConfigProvider resultProxyConfig = null;

        if (useSystemPropertyValues) {
            resultProxyConfig = fromSystemPropertySettings(scheme);
        } else if (useEnvironmentVariableValues) {
            return fromEnvironmentSettings(scheme);
        }
        if (isDefaultProxyConfig(resultProxyConfig) && useEnvironmentVariableValues) {
            return fromEnvironmentSettings(scheme);

        }
        return resultProxyConfig;
    }

    static boolean isDefaultProxyConfig(ProxyConfigProvider proxyConfig) {
        return proxyConfig != null && proxyConfig.host() == null
               && proxyConfig.port() == 0
               && !proxyConfig.password().isPresent()
               && !proxyConfig.userName().isPresent()
               && CollectionUtils.isNullOrEmpty(proxyConfig.nonProxyHosts());
    }


}
