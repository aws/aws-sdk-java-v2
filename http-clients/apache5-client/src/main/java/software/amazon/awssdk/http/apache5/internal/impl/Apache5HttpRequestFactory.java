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

import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache5.internal.Apache5HttpRequestConfig;
import software.amazon.awssdk.http.apache5.internal.RepeatableInputStreamRequestEntity;
import software.amazon.awssdk.http.apache5.internal.utils.Apache5Utils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Responsible for creating Apache HttpClient 5 request objects.
 */
@SdkInternalApi
public class Apache5HttpRequestFactory {

    private static final List<String> IGNORE_HEADERS = Arrays.asList(HttpHeaders.CONTENT_LENGTH, HttpHeaders.HOST,
                                                                     HttpHeaders.TRANSFER_ENCODING);

    public HttpUriRequestBase create(final HttpExecuteRequest request, final Apache5HttpRequestConfig requestConfig) {
        HttpUriRequestBase base = createApacheRequest(request, sanitizeUri(request.httpRequest()));
        addHeadersToRequest(base, request.httpRequest());
        addRequestConfig(base, request.httpRequest(), requestConfig);
        return base;
    }

    /**
     *
     * The Apache HTTP client doesn't allow consecutive slashes in the URI. For S3
     * and other AWS services, this is allowed and required. This methods replaces
     * any occurrence of "//" in the URI path with "/%2F".
     *
     * @see SdkHttpRequest#getUri()
     * @param request The existing request
     * @return a new String containing the modified URI
     */
    private URI sanitizeUri(SdkHttpRequest request) {
        String path = request.encodedPath();
        if (path.contains("//")) {
            int port = request.port();
            String protocol = request.protocol();
            String newPath = StringUtils.replace(path, "//", "/%2F");
            String encodedQueryString = request.encodedQueryParameters().map(value -> "?" + value).orElse("");

            // Do not include the port in the URI when using the default port for the protocol.
            String portString = SdkHttpUtils.isUsingStandardPort(protocol, port) ?
                                "" : ":" + port;

            return URI.create(protocol + "://" + request.host() + portString + newPath + encodedQueryString);
        }

        return request.getUri();
    }

    private void addRequestConfig(HttpUriRequestBase base,
                                  SdkHttpRequest request,
                                  Apache5HttpRequestConfig requestConfig) {
        int connectTimeout = saturatedCast(requestConfig.connectionTimeout().toMillis());
        int connectAcquireTimeout = saturatedCast(requestConfig.connectionAcquireTimeout().toMillis());
        RequestConfig.Builder requestConfigBuilder = RequestConfig
            .custom()
            .setConnectionRequestTimeout(connectAcquireTimeout, TimeUnit.MILLISECONDS)
            .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            .setResponseTimeout(saturatedCast(requestConfig.socketTimeout().toMillis()), TimeUnit.MILLISECONDS);

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


    private HttpUriRequestBase createApacheRequest(HttpExecuteRequest request, URI uri) {
        switch (request.httpRequest().method()) {
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
                throw new RuntimeException("Unknown HTTP method name: " + request.httpRequest().method());
        }
    }

    private HttpUriRequestBase wrapEntity(HttpExecuteRequest request,
                                          HttpUriRequestBase entityEnclosingRequest) {

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
        if (request.contentStreamProvider().isPresent()) {
            HttpEntity entity = new RepeatableInputStreamRequestEntity(request);
            if (!request.httpRequest().firstMatchingHeader(HttpHeaders.CONTENT_LENGTH).isPresent() && !entity.isChunked()) {
                entity = Apache5Utils.newBufferedHttpEntity(entity);
            }
            entityEnclosingRequest.setEntity(entity);
        }

        return entityEnclosingRequest;
    }

    /**
     * Configures the headers in the specified Apache5 HTTP request.
     */
    private void addHeadersToRequest(HttpUriRequestBase httpRequest, SdkHttpRequest request) {
        httpRequest.addHeader(HttpHeaders.HOST, getHostHeaderValue(request));

        // Copy over any other headers already in our request
        request.forEachHeader((name, value) -> {
            // HttpClient4 fills in the Content-Length header and complains if
            // it's already present, so we skip it here. We also skip the Host
            // header to avoid sending it twice, which will interfere with some
            // signing schemes.
            if (IGNORE_HEADERS.stream().noneMatch(name::equalsIgnoreCase)) {
                for (String headerValue : value) {
                    httpRequest.addHeader(name, headerValue);
                }
            }
        });
    }

    private String getHostHeaderValue(SdkHttpRequest request) {
        // Respect any user-specified Host header when present
        Optional<String> existingHostHeader = request.firstMatchingHeader(HttpHeaders.HOST);
        if (existingHostHeader.isPresent()) {
            return existingHostHeader.get();
        }
        // Apache doesn't allow us to include the port in the host header if it's a standard port for that protocol. For that
        // reason, we don't include the port when we sign the message. See {@link SdkHttpRequest#port()}.
        return !SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
                ? request.host() + ":" + request.port()
                : request.host();
    }
}
