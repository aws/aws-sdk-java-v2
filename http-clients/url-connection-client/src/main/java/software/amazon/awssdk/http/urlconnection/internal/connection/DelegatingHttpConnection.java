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

package software.amazon.awssdk.http.urlconnection.internal.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of {@link HttpConnection} that delegates all methods to another {@code HttpConnection}. This is useful to
 * child classes looking to decorate a {@code HttpConnection} without a lot of boilerplate.
 */
@SdkInternalApi
public abstract class DelegatingHttpConnection implements HttpConnection {
    protected final HttpConnection delegate;

    public DelegatingHttpConnection(HttpConnection delegate) {
        this.delegate = delegate;
    }

    @Override
    public void connect() {
        delegate.connect();
    }

    @Override
    public OutputStream getRequestStream() {
        return delegate.getRequestStream();
    }

    @Override
    public int getResponseCode() {
        return delegate.getResponseCode();
    }

    @Override
    public String getResponseMessage() {
        return delegate.getResponseMessage();
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return delegate.getResponseHeaders();
    }

    @Override
    public String getResponseHeader(String header) {
        return delegate.getResponseHeader(header);
    }

    @Override
    public InputStream getResponseStream() {
        return delegate.getResponseStream();
    }

    @Override
    public InputStream getResponseErrorStream() {
        return delegate.getResponseErrorStream();
    }

    @Override
    public void disconnect() {
        delegate.disconnect();
    }
}
