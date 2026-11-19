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
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SystemUserAgent;
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
        URI resourcePath = resolveDataUri(path);
        return defaulttHttpBuilder()
            .method(SdkHttpMethod.GET)
            .uri(resourcePath)
            .putHeader(EC2_METADATA_TOKEN_TTL_HEADER, String.valueOf(tokenTtl.getSeconds()))
            .putHeader(TOKEN_HEADER, token)
            .build();
    }

    /**
     * Resolves the given metadata path against the configured endpoint. Throws an {@link SdkClientException} when the
     * path is null, does not form a valid URI, or changes the scheme, host, or port of the configured endpoint.
     */
    private URI resolveDataUri(String path) {
        if (path == null) {
            throw invalidPathException(null);
        }
        URI resourcePath;
        try {
            resourcePath = URI.create(basePath + path);
        } catch (IllegalArgumentException e) {
            throw invalidPathException(e);
        }
        if (!Objects.equals(basePath.getScheme(), resourcePath.getScheme())
            || !Objects.equals(basePath.getRawAuthority(), resourcePath.getRawAuthority())
            || !Objects.equals(basePath.getHost(), resourcePath.getHost())
            || basePath.getPort() != resourcePath.getPort()) {
            throw invalidPathException(null);
        }
        return resourcePath;
    }

    private SdkClientException invalidPathException(Throwable cause) {
        return SdkClientException.create("Invalid metadata path: the path must not change the scheme, host, or port "
                                         + "of the configured IMDS endpoint " + basePath, cause);
    }

    private SdkHttpFullRequest.Builder defaulttHttpBuilder() {
        return SdkHttpFullRequest.builder()
                             .putHeader(USER_AGENT, SystemUserAgent.getOrCreate().userAgentString())
                             .putHeader(ACCEPT, "*/*")
                             .putHeader(CONNECTION, "keep-alive");
    }
}
