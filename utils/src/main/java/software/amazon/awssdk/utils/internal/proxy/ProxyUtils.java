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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Splits a pipe-separated {@code nonProxyHosts} string into its trimmed, lowercased tokens WITHOUT the {@code * -> .*?}
 * Java-regex rewrite that {@code SdkHttpUtils#parseNonProxyHostsProperty()} applies. Surrounding whitespace is trimmed so the
 * common comma-space {@code no_proxy=a.com, *.foo.com} form (which arrives as {@code " *.foo.com"} after the {@code , -> |}
 * normalization) is matched correctly. The returned tokens keep their documented forms (exact host, {@code *.suffix}, bare
 * {@code *}, CIDR), which is what a curl-style matcher such as the CRT client's native matcher expects. The regex-rewritten
 * form remains available via {@code SdkHttpUtils} for the Netty/Apache/url-connection clients.
 */
@SdkInternalApi
final class ProxyUtils {

    private ProxyUtils() {
    }

    static Set<String> splitToGlobTokens(String nonProxyHosts) {
        if (nonProxyHosts != null && !StringUtils.isEmpty(nonProxyHosts)) {
            return Arrays.stream(nonProxyHosts.split("\\|"))
                         .map(String::trim)
                         .map(String::toLowerCase)
                         .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
}
