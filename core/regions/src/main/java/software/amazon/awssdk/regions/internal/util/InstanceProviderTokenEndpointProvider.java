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

package software.amazon.awssdk.regions.internal.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;
import software.amazon.awssdk.regions.util.ResourcesEndpointProvider;

@SdkInternalApi
public final class InstanceProviderTokenEndpointProvider implements ResourcesEndpointProvider {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    private static final String DEFAULT_TOKEN_TTL = "21600";

    @Override
    public URI endpoint() {
        String host = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getStringValueOrThrow();
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return URI.create(host + TOKEN_RESOURCE_PATH);
    }

    @Override
    public Map<String, String> headers() {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("User-Agent", UserAgentUtils.getUserAgent());
        requestHeaders.put("Accept", "*/*");
        requestHeaders.put("Connection", "keep-alive");
        requestHeaders.put(EC2_METADATA_TOKEN_TTL_HEADER, DEFAULT_TOKEN_TTL);

        return requestHeaders;
    }
}