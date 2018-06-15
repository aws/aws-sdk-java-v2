/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.RepeatableInputStreamRequestEntity;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Responsible for creating Apache HttpClient 4 request objects.
 */
@SdkInternalApi
public class ApacheHttpRequestFactory {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final List<String> IGNORE_HEADERS = Arrays.asList(HttpHeaders.CONTENT_LENGTH, HttpHeaders.HOST);

    public HttpRequestBase create(final SdkHttpFullRequest request, final ApacheHttpRequestConfig requestConfig) {
        URI uri = request.getUri();
        final HttpRequestBase base = createApacheRequest(request, uri.toString());
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
        if (SdkHttpMethod.PUT == request.method() && requestConfig.expectContinueEnabled()) {
            requestConfigBuilder.setExpectContinueEnabled(true);
        }

        base.setConfig(requestConfigBuilder.build());
    }


    private HttpRequestBase createApacheRequest(SdkHttpFullRequest request, String uri) {
        switch (request.method()) {
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
                throw new RuntimeException("Unknown HTTP method name: " + request.method());
        }
    }

    private HttpRequestBase wrapEntity(SdkHttpFullRequest request,
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
        if (request.content().isPresent()) {
            HttpEntity entity = new RepeatableInputStreamRequestEntity(request);
            if (request.headers().get(HttpHeaders.CONTENT_LENGTH) == null) {
                entity = ApacheUtils.newBufferedHttpEntity(entity);
            }
            entityEnclosingRequest.setEntity(entity);
        }

        return entityEnclosingRequest;
    }

    /**
     * Configures the headers in the specified Apache HTTP request.
     */
    private void addHeadersToRequest(HttpRequestBase httpRequest, SdkHttpFullRequest request) {

        httpRequest.addHeader(HttpHeaders.HOST, getHostHeaderValue(request));


        // Copy over any other headers already in our request
        request.headers().entrySet().stream()
               /*
                * HttpClient4 fills in the Content-Length header and complains if
                * it's already present, so we skip it here. We also skip the Host
                * header to avoid sending it twice, which will interfere with some
                * signing schemes.
                */
               .filter(e -> !IGNORE_HEADERS.contains(e.getKey()))
               .forEach(e -> e.getValue().forEach(h -> httpRequest.addHeader(e.getKey(), h)));

        /* Set content type and encoding */
        if (httpRequest.getHeaders(HttpHeaders.CONTENT_TYPE) == null ||
            httpRequest.getHeaders(HttpHeaders.CONTENT_TYPE).length == 0) {
            httpRequest.addHeader(HttpHeaders.CONTENT_TYPE,
                                  "application/x-www-form-urlencoded; " +
                                  "charset=" + lowerCase(DEFAULT_ENCODING));
        }
    }

    private String getHostHeaderValue(SdkHttpRequest request) {
        // Apache doesn't allow us to include the port in the host header if it's a standard port for that protocol. For that
        // reason, we don't include the port when we sign the message. See {@link SdkHttpRequest#port()}.
        return !SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
                ? request.host() + ":" + request.port()
                : request.host();
    }
}
