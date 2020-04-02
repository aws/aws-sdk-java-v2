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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil.ExtensionHeaderNames;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class RequestAdapter {

    private static final String HOST = "Host";
    private static final List<String> IGNORE_HEADERS = Collections.singletonList(HOST);

    private final Protocol protocol;

    public RequestAdapter(Protocol protocol) {
        this.protocol = Validate.paramNotNull(protocol, "protocol");
    }

    public HttpRequest adapt(SdkHttpRequest sdkRequest) {
        HttpMethod method = toNettyHttpMethod(sdkRequest.method());
        HttpHeaders headers = new DefaultHttpHeaders();
        String uri = encodedPathAndQueryParams(sdkRequest);
        // All requests start out as HTTP/1.1 objects, even if they will
        // ultimately be sent over HTTP2. Conversion to H2 is handled at a
        // later stage if necessary; see HttpToHttp2OutboundAdapter.
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, uri, headers);
        addHeadersToRequest(request, sdkRequest);
        return request;
    }

    private static HttpMethod toNettyHttpMethod(SdkHttpMethod method) {
        return HttpMethod.valueOf(method.name());
    }

    private static String encodedPathAndQueryParams(SdkHttpRequest sdkRequest) {
        String encodedPath = sdkRequest.encodedPath();
        if (StringUtils.isBlank(encodedPath)) {
            encodedPath = "/";
        }

        String encodedQueryParams = SdkHttpUtils.encodeAndFlattenQueryParameters(sdkRequest.rawQueryParameters())
                .map(queryParams -> "?" + queryParams)
                .orElse("");

        return encodedPath + encodedQueryParams;
    }

    /**
     * Configures the headers in the specified Netty HTTP request.
     */
    private void addHeadersToRequest(DefaultHttpRequest httpRequest, SdkHttpRequest request) {
        httpRequest.headers().add(HOST, getHostHeaderValue(request));

        String scheme = request.getUri().getScheme();
        if (Protocol.HTTP2 == protocol && !StringUtils.isBlank(scheme)) {
            httpRequest.headers().add(ExtensionHeaderNames.SCHEME.text(), scheme);
        }

        // Copy over any other headers already in our request
        request.headers().entrySet().stream()
                /*
                 * Skip the Host header to avoid sending it twice, which will
                 * interfere with some signing schemes.
                 */
                .filter(e -> !IGNORE_HEADERS.contains(e.getKey()))
                .forEach(e -> e.getValue().forEach(h -> httpRequest.headers().add(e.getKey(), h)));
    }

    private String getHostHeaderValue(SdkHttpRequest request) {
        return SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
                ? request.host()
                : request.host() + ":" + request.port();
    }
}
