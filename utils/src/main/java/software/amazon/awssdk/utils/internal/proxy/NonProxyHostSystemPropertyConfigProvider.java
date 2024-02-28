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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.NonProxyHostConfigProvider;

/**
 * An implementation of the {@link NonProxyHostConfigProvider} interface that retrieves non-proxy host configuration settings from
 * environment variables.
 *
 * @see NonProxyHostConfigProvider
 */
@SdkInternalApi
public class NonProxyHostSystemPropertyConfigProvider implements NonProxyHostConfigProvider {

    /**
     * Retrieves the set of non-proxy hosts from environment variables.
     *
     * @return The set of non-proxy hosts.
     */
    @Override
    public Set<String> nonProxyHosts() {
        return parseNonProxyHostsProperty();
    }
}