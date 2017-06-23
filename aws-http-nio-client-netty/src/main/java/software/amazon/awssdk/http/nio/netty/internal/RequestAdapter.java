/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.StringUtils.isNotBlank;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

public final class RequestAdapter {

    public HttpRequest adapt(SdkHttpRequest sdkRequest) {
        String uri = uriFrom(sdkRequest);
        HttpMethod method = toNettyHttpMethod(sdkRequest.getHttpMethod());
        HttpHeaders headers = new DefaultHttpHeaders();
        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, uri, headers);
        sdkRequest.getHeaders().forEach(request.headers()::add);
        return request;
    }

    private String uriFrom(SdkHttpRequest sdkRequest) {
        StringBuilder uriBuilder = new StringBuilder(sdkRequest.getEndpoint().toString());
        if (isNotBlank(sdkRequest.getResourcePath())) {
            uriBuilder.append(sdkRequest.getResourcePath());
        }

        QueryStringEncoder encoder = new QueryStringEncoder(uriBuilder.toString());
        sdkRequest.getParameters().forEach((k, values) -> values.forEach(v -> encoder.addParam(k, v)));
        return encoder.toString();
    }

    private static HttpMethod toNettyHttpMethod(SdkHttpMethod method) {
        return HttpMethod.valueOf(method.name());
    }
}
