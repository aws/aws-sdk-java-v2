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

package software.amazon.awssdk.imds.internal;

import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Class to parse the parameters to a SdkHttpRequest, make the call to the endpoint and send the HttpExecuteResponse
 * to the DefaultEc2Metadata class for further processing.
 */
@SdkInternalApi
public class RequestMarshaller {

    public static final String TOKEN_RESOURCE_PATH = "/latest/api/token";

    public static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";

    public static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";

    public static final String USER_AGENT = "user_agent";

    public static final String ACCEPT = "Accept";

    public static final String CONNECTION = "connection";

    private final URI basePath;
    private final URI tokenPath;

    public RequestMarshaller(URI basePath) {
        this.basePath = basePath;
        this.tokenPath = URI.create(basePath + TOKEN_RESOURCE_PATH);
    }

    public SdkHttpFullRequest createTokenRequest(Duration tokenTtl) {
        return defaulttHttpBuilder()
            .method(SdkHttpMethod.PUT)
            .uri(tokenPath)
            .putHeader(EC2_METADATA_TOKEN_TTL_HEADER, String.valueOf(tokenTtl.getSeconds()))
            .build();
    }

    public SdkHttpFullRequest createDataRequest(String path, String token, Duration tokenTtl) {
        URI resourcePath = URI.create(basePath + path);
        return defaulttHttpBuilder()
            .method(SdkHttpMethod.GET)
            .uri(resourcePath)
            .putHeader(EC2_METADATA_TOKEN_TTL_HEADER, String.valueOf(tokenTtl.getSeconds()))
            .putHeader(TOKEN_HEADER, token)
            .build();
    }

    private SdkHttpFullRequest.Builder defaulttHttpBuilder() {
        return SdkHttpFullRequest.builder()
                             .putHeader(USER_AGENT, SdkUserAgent.create().userAgent())
                             .putHeader(ACCEPT, "*/*")
                             .putHeader(CONNECTION, "keep-alive");
    }
}
