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

package software.amazon.awssdk.http.apache5.internal.impl;

import java.io.IOException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An instance of {@link ConnectionManagerAwareHttpClient} that delegates all the requests to the given http client.
 */
@SdkInternalApi
public class Apache5SdkHttpClient implements ConnectionManagerAwareHttpClient {

    private final HttpClient delegate;

    private final HttpClientConnectionManager cm;

    public Apache5SdkHttpClient(final HttpClient delegate,
                                final HttpClientConnectionManager cm) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate " +
                                               "cannot be null");
        }
        if (cm == null) {
            throw new IllegalArgumentException("connection manager " +
                                               "cannot be null");
        }
        this.delegate = delegate;
        this.cm = cm;
    }

    @Override
    public HttpResponse execute(ClassicHttpRequest request) throws IOException {
        return delegate.execute(request);
    }

    @Override
    public HttpResponse execute(ClassicHttpRequest request, HttpContext context) throws IOException {
        return delegate.execute(request, context);
    }

    @Override
    public ClassicHttpResponse execute(HttpHost target, ClassicHttpRequest request) throws IOException {
        return delegate.execute(target, request);
    }

    @Override
    public HttpResponse execute(HttpHost target, ClassicHttpRequest request, HttpContext context) throws IOException {
        return delegate.execute(target, request, context);
    }

    @Override
    public <T> T execute(ClassicHttpRequest request, HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        return delegate.execute(request, responseHandler);
    }

    @Override
    public <T> T execute(ClassicHttpRequest request, HttpContext context,
                         HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        return delegate.execute(request, context, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, ClassicHttpRequest request,
                         HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        return delegate.execute(target, request, responseHandler);
    }

    @Override
    public <T> T execute(HttpHost target, ClassicHttpRequest request,
                         HttpContext context,
                         HttpClientResponseHandler<? extends T> responseHandler) throws IOException {
        return delegate.execute(target, request, context, responseHandler);
    }

    @Override
    public HttpClientConnectionManager getHttpClientConnectionManager() {
        return cm;
    }
}
