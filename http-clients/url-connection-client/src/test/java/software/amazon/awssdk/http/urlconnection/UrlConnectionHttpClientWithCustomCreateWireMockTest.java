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
package software.amazon.awssdk.http.urlconnection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.FunctionalUtils.safeFunction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientTestSuite;

public final class UrlConnectionHttpClientWithCustomCreateWireMockTest extends SdkHttpClientTestSuite {

    private Function<HttpURLConnection, HttpURLConnection> connectionInterceptor = Function.identity();

    @Override
    protected SdkHttpClient createSdkHttpClient(SdkHttpClientOptions options) {
        return UrlConnectionHttpClient.create(uri -> invokeSafely(() -> {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            return connectionInterceptor.apply(connection);
        }));
    }

    // Empty test; behavior not supported when using custom factory
    @Override
    public void testCustomTlsTrustManager() {
    }

    // Empty test; behavior not supported when using custom factory
    @Override
    public void testTrustAllWorks() {
    }

    // Empty test; behavior not supported when using custom factory
    @Override
    public void testCustomTlsTrustManagerAndTrustAllFails() {
    }

    // Empty test; behavior not supported because the URL connection client does not allow disabling connection reuse
    @Override
    public void connectionsAreNotReusedOn5xxErrors() throws Exception {
    }

    @Test
    public void testGetResponseCodeNpeIsWrappedAsIo() throws Exception {
        connectionInterceptor = safeFunction(connection -> new DelegateHttpURLConnection(connection) {
            @Override
            public int getResponseCode() {
                throw new NullPointerException();
            }
        });

        assertThatThrownBy(() -> testForResponseCode(HttpURLConnection.HTTP_OK))
            .isInstanceOf(IOException.class)
            .hasMessage("Unexpected NullPointerException when trying to read response from HttpURLConnection")
            .hasCauseInstanceOf(NullPointerException.class);
    }

    private class DelegateHttpURLConnection extends HttpURLConnection {
        private final HttpURLConnection delegate;

        private DelegateHttpURLConnection(HttpURLConnection delegate) {
            super(delegate.getURL());
            this.delegate = delegate;
        }

        @Override
        public String getHeaderFieldKey(int n) {
            return delegate.getHeaderFieldKey(n);
        }

        @Override
        public void setFixedLengthStreamingMode(int contentLength) {
            delegate.setFixedLengthStreamingMode(contentLength);
        }

        @Override
        public void setFixedLengthStreamingMode(long contentLength) {
            delegate.setFixedLengthStreamingMode(contentLength);
        }

        @Override
        public void setChunkedStreamingMode(int chunklen) {
            delegate.setChunkedStreamingMode(chunklen);
        }

        @Override
        public String getHeaderField(int n) {
            return delegate.getHeaderField(n);
        }

        @Override
        public void setInstanceFollowRedirects(boolean followRedirects) {
            delegate.setInstanceFollowRedirects(followRedirects);
        }

        @Override
        public boolean getInstanceFollowRedirects() {
            return delegate.getInstanceFollowRedirects();
        }

        @Override
        public void setRequestMethod(String method) throws ProtocolException {
            delegate.setRequestMethod(method);
        }

        @Override
        public String getRequestMethod() {
            return delegate.getRequestMethod();
        }

        @Override
        public int getResponseCode() throws IOException {
            return delegate.getResponseCode();
        }

        @Override
        public String getResponseMessage() throws IOException {
            return delegate.getResponseMessage();
        }

        @Override
        public long getHeaderFieldDate(String name, long Default) {
            return delegate.getHeaderFieldDate(name, Default);
        }

        @Override
        public void disconnect() {
            delegate.disconnect();
        }

        @Override
        public boolean usingProxy() {
            return delegate.usingProxy();
        }

        @Override
        public Permission getPermission() throws IOException {
            return delegate.getPermission();
        }

        @Override
        public InputStream getErrorStream() {
            return delegate.getErrorStream();
        }

        @Override
        public void connect() throws IOException {
            delegate.connect();
        }

        @Override
        public void setConnectTimeout(int timeout) {
            delegate.setConnectTimeout(timeout);
        }

        @Override
        public int getConnectTimeout() {
            return delegate.getConnectTimeout();
        }

        @Override
        public void setReadTimeout(int timeout) {
            delegate.setReadTimeout(timeout);
        }

        @Override
        public int getReadTimeout() {
            return delegate.getReadTimeout();
        }

        @Override
        public URL getURL() {
            return delegate.getURL();
        }

        @Override
        public int getContentLength() {
            return delegate.getContentLength();
        }

        @Override
        public long getContentLengthLong() {
            return delegate.getContentLengthLong();
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public String getContentEncoding() {
            return delegate.getContentEncoding();
        }

        @Override
        public long getExpiration() {
            return delegate.getExpiration();
        }

        @Override
        public long getDate() {
            return delegate.getDate();
        }

        @Override
        public long getLastModified() {
            return delegate.getLastModified();
        }

        @Override
        public String getHeaderField(String name) {
            return delegate.getHeaderField(name);
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return delegate.getHeaderFields();
        }

        @Override
        public int getHeaderFieldInt(String name, int Default) {
            return delegate.getHeaderFieldInt(name, Default);
        }

        @Override
        public long getHeaderFieldLong(String name, long Default) {
            return delegate.getHeaderFieldLong(name, Default);
        }

        @Override
        public Object getContent() throws IOException {
            return delegate.getContent();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return delegate.getOutputStream();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public void setDoInput(boolean doinput) {
            delegate.setDoInput(doinput);
        }

        @Override
        public boolean getDoInput() {
            return delegate.getDoInput();
        }

        @Override
        public void setDoOutput(boolean dooutput) {
            delegate.setDoOutput(dooutput);
        }

        @Override
        public boolean getDoOutput() {
            return delegate.getDoOutput();
        }

        @Override
        public void setAllowUserInteraction(boolean allowuserinteraction) {
            delegate.setAllowUserInteraction(allowuserinteraction);
        }

        @Override
        public boolean getAllowUserInteraction() {
            return delegate.getAllowUserInteraction();
        }

        @Override
        public void setUseCaches(boolean usecaches) {
            delegate.setUseCaches(usecaches);
        }

        @Override
        public boolean getUseCaches() {
            return delegate.getUseCaches();
        }

        @Override
        public void setIfModifiedSince(long ifmodifiedsince) {
            delegate.setIfModifiedSince(ifmodifiedsince);
        }

        @Override
        public long getIfModifiedSince() {
            return delegate.getIfModifiedSince();
        }

        @Override
        public boolean getDefaultUseCaches() {
            return delegate.getDefaultUseCaches();
        }

        @Override
        public void setDefaultUseCaches(boolean defaultusecaches) {
            delegate.setDefaultUseCaches(defaultusecaches);
        }

        @Override
        public void setRequestProperty(String key, String value) {
            delegate.setRequestProperty(key, value);
        }

        @Override
        public void addRequestProperty(String key, String value) {
            delegate.addRequestProperty(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return delegate.getRequestProperty(key);
        }

        @Override
        public Map<String, List<String>> getRequestProperties() {
            return delegate.getRequestProperties();
        }
    }
}
