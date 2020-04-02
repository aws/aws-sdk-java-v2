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

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * The system properties related to http proxy
 */
@SdkProtectedApi
public enum ProxySystemSetting implements SystemSetting  {

    PROXY_HOST("http.proxyHost"),
    PROXY_PORT("http.proxyPort"),
    NON_PROXY_HOSTS("http.nonProxyHosts"),
    PROXY_USERNAME("http.proxyUser"),
    PROXY_PASSWORD("http.proxyPassword")
    ;

    private final String systemProperty;

    ProxySystemSetting(String systemProperty) {
        this.systemProperty = systemProperty;
    }

    @Override
    public String property() {
        return systemProperty;
    }

    @Override
    public String environmentVariable() {
        return null;
    }

    @Override
    public String defaultValue() {
        return null;
    }
}
