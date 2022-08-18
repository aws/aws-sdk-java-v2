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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public final class EnvironmentProxyUtils {

    private EnvironmentProxyUtils() {
    }

    public static Optional<String> parseHost(String proxyEnvValue) {
        try {
            URL proxyUrl = new URL(proxyEnvValue);
            return Optional.of(proxyUrl.getHost());
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parsePort(String proxyEnvValue) {
        try {
            URL proxyUrl = new URL(proxyEnvValue);
            return Optional.of(proxyUrl.getPort());
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> parseUsername(String proxyEnvValue) {
        String[] proxyUserParts = parseProxyUserInfoParts(proxyEnvValue);
        return proxyUserParts != null ? Optional.of(proxyUserParts[0]) : Optional.empty();
    }

    public static Optional<String> parsePassword(String proxyEnvValue) {
        String[] proxyUserParts = parseProxyUserInfoParts(proxyEnvValue);
        return proxyUserParts != null ? Optional.of(proxyUserParts[1]) : Optional.empty();
    }

    public static Set<String> parseNonProxyHosts(String noProxyEnvValue) {
        return Stream.of(noProxyEnvValue.split(",")).collect(Collectors.toSet());
    }

    private static String[] parseProxyUserInfoParts(String proxyEnvValue) {
        try {
            URL proxyUrl = new URL(proxyEnvValue);
            if (proxyUrl.getUserInfo() != null) {
                return proxyUrl.getUserInfo().split(":", 2);
            }
        } catch (MalformedURLException e) {
            return null;
        }
        return null;
    }

}
