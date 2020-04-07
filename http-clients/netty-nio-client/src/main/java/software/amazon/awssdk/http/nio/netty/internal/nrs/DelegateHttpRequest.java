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

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
@SdkInternalApi
class DelegateHttpRequest extends DelegateHttpMessage implements HttpRequest {

    protected final HttpRequest request;

    DelegateHttpRequest(HttpRequest request) {
        super(request);
        this.request = request;
    }

    @Override
    public HttpRequest setMethod(HttpMethod method) {
        request.setMethod(method);
        return this;
    }

    @Override
    public HttpRequest setUri(String uri) {
        request.setUri(uri);
        return this;
    }

    @Override
    @Deprecated
    public HttpMethod getMethod() {
        return request.method();
    }

    @Override
    public HttpMethod method() {
        return request.method();
    }

    @Override
    @Deprecated
    public String getUri() {
        return request.uri();
    }

    @Override
    public String uri() {
        return request.uri();
    }

    @Override
    public HttpRequest setProtocolVersion(HttpVersion version) {
        super.setProtocolVersion(version);
        return this;
    }
}
