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

package software.amazon.awssdk.http.apache.internal.impl;

import static software.amazon.awssdk.utils.HttpUtils.createUrl;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.RepeatableInputStreamRequestEntity;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;

/**
 * Responsible for creating Apache HttpClient 4 request objects.
 */
public class ApacheHttpRequestFactory {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final List<String> IGNORE_HEADERS = Arrays.asList(HttpHeaders.CONTENT_LENGTH, HttpHeaders.HOST);

    public HttpRequestBase create(final SdkHttpFullRequest request, final ApacheHttpRequestConfig requestConfig) {
        String uri = createUrl(request.getEndpoint(), request.getResourcePath(), request.getParameters(), true).toString();

        final HttpRequestBase base = createApacheRequest(request, uri);
        addHeadersToRequest(base, request);
        addRequestConfig(base, request, requestConfig);

        return base;
    }

    private void addRequestConfig(final HttpRequestBase base,
                                  final SdkHttpFullRequest request,
                                  final ApacheHttpRequestConfig requestConfig) {
        final int connectTimeout = saturatedCast(requestConfig.connectionTimeout().toMillis());
        final RequestConfig.Builder requestConfigBuilder = RequestConfig
                .custom()
                .setConnectionRequestTimeout(connectTimeout)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(saturatedCast(requestConfig.socketTimeout().toMillis()))
                .setLocalAddress(requestConfig.localAddress());

        /*
         * Enable 100-continue support for PUT operations, since this is
         * where we're potentially uploading large amounts of data and want
         * to find out as early as possible if an operation will fail. We
         * don't want to do this for all operations since it will cause
         * extra latency in the network interaction.
         */
        if (SdkHttpMethod.PUT == request.getHttpMethod() && requestConfig.expectContinueEnabled()) {
            requestConfigBuilder.setExpectContinueEnabled(true);
        }

        base.setConfig(requestConfigBuilder.build());
    }


    private static HttpRequestBase createApacheRequest(SdkHttpFullRequest request, String uri) {
        switch (request.getHttpMethod()) {
            case HEAD:
                return new HttpHead(uri);
            case GET:
                return new HttpGet(uri);
            case DELETE:
                return new HttpDelete(uri);
            case OPTIONS:
                return new HttpOptions(uri);
            case PATCH:
                return wrapEntity(request, new HttpPatch(uri));
            case POST:
                return wrapEntity(request, new HttpPost(uri));
            case PUT:
                return wrapEntity(request, new HttpPut(uri));
            default:
                throw new RuntimeException("Unknown HTTP method name: " + request.getHttpMethod());
        }
    }

    private static HttpRequestBase wrapEntity(SdkHttpFullRequest request,
                                       HttpEntityEnclosingRequestBase entityEnclosingRequest) {

        /*
         * We should never reuse the entity of the previous request, since
         * reading from the buffered entity will bypass reading from the
         * original request content. And if the content contains InputStream
         * wrappers that were added for validation-purpose (e.g.
         * Md5DigestCalculationInputStream), these wrappers would never be
         * read and updated again after AmazonHttpClient resets it in
         * preparation for the retry. Eventually, these wrappers would
         * return incorrect validation result.
         */
        if (request.getContent() != null) {
            HttpEntity entity = new RepeatableInputStreamRequestEntity(request);
            if (request.getHeaders().get(HttpHeaders.CONTENT_LENGTH) == null) {
                entity = ApacheUtils.newBufferedHttpEntity(entity);
            }
            entityEnclosingRequest.setEntity(entity);
        }

        return entityEnclosingRequest;
    }

    /**
     * Configures the headers in the specified Apache HTTP request.
     */
    private static void addHeadersToRequest(HttpRequestBase httpRequest, SdkHttpFullRequest request) {

        httpRequest.addHeader(HttpHeaders.HOST, getHostHeaderValue(request.getEndpoint()));


        // Copy over any other headers already in our request
        request.getHeaders().entrySet().stream()
                /*
                 * HttpClient4 fills in the Content-Length header and complains if
                 * it's already present, so we skip it here. We also skip the Host
                 * header to avoid sending it twice, which will interfere with some
                 * signing schemes.
                 */
                .filter(e -> !IGNORE_HEADERS.contains(e.getKey()))
                .forEach(e -> e.getValue().stream()
                        .forEach(h -> httpRequest.addHeader(e.getKey(), h)));

        /* Set content type and encoding */
        if (httpRequest.getHeaders(HttpHeaders.CONTENT_TYPE) == null ||
            httpRequest.getHeaders(HttpHeaders.CONTENT_TYPE).length == 0) {
            httpRequest.addHeader(HttpHeaders.CONTENT_TYPE,
                                  "application/x-www-form-urlencoded; " +
                                  "charset=" + lowerCase(DEFAULT_ENCODING));
        }
    }

    private static String getHostHeaderValue(final URI endpoint) {
        /*
         * Apache HttpClient omits the port number in the Host header (even if
         * we explicitly specify it) if it's the default port for the protocol
         * in use. To ensure that we use the same Host header in the request and
         * in the calculated string to sign (even if Apache HttpClient changed
         * and started honoring our explicit host with endpoint), we follow this
         * same behavior here and in the QueryString signer.
         */
        return isUsingNonDefaultPort(endpoint)
                ? endpoint.getHost() + ":" + endpoint.getPort()
                : endpoint.getHost();
    }

    private static boolean isUsingNonDefaultPort(URI uri) {
        int port = uri.getPort();
        if (port <= 0) {
            return false;
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);

        if (scheme.equals("http") && port == 80) {
            return false;
        }
        if (scheme.equals("https") && port == 443) {
            return false;
        }

        return true;
    }
}
