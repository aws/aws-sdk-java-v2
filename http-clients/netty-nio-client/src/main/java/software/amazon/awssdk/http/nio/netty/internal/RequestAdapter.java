/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.nio.netty.internal.utils.UriUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class RequestAdapter {

    private static final String HOST = "Host"; // can't use netty's because of case
    private static final List<String> IGNORE_HEADERS = Arrays.asList(HOST);

    private final Protocol protocol;

    public RequestAdapter(Protocol protocol) {
        Objects.requireNonNull(protocol, "protocol");
        this.protocol = protocol;
    }

    public HttpRequest adapt(SdkHttpRequest sdkRequest) {
        HttpMethod method = toNettyHttpMethod(sdkRequest.method());
        HttpHeaders headers = new DefaultHttpHeaders();
        URI originalUri = sdkRequest.getUri();
        String uri = UriUtils.relativize(originalUri).toString();
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, uri, headers);
        addHeadersToRequest(request, sdkRequest, originalUri);
        return request;
    }

    private static HttpMethod toNettyHttpMethod(SdkHttpMethod method) {
        return HttpMethod.valueOf(method.name());
    }

    /**
     * Configures the headers in the specified Netty HTTP request.
     */
    private void addHeadersToRequest(DefaultHttpRequest httpRequest, SdkHttpRequest request, URI originalUri) {

        httpRequest.headers().add(HOST, getHostHeaderValue(request));
        if (protocol == Protocol.HTTP2 && !StringUtils.isBlank(originalUri.getScheme())) {
            httpRequest.headers().add(ExtensionHeaderNames.SCHEME.text(), originalUri.getScheme());
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
        return !SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
                ? request.host() + ":" + request.port()
                : request.host();
    }
}
