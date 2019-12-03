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

package software.amazon.awssdk.http.apache.async;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.reactive.ReactiveEntityProducer;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Responsible for creating Apache HttpClient 5 request objects.
 */
@SdkInternalApi
class ApacheAsyncRequestFactory {
    private static final List<String> IGNORE_HEADERS = Arrays.asList(HttpHeaders.HOST, HttpHeaders.CONTENT_LENGTH);
    private static final SortedSet<String> SENSITIVE_HEADERS = new TreeSet<>(String::compareToIgnoreCase);

    static {
        // On HTTP/2, we will mark these headers as sensitive so that their contents don't waste space in the HPACK table.
        SENSITIVE_HEADERS.addAll(Arrays.asList("authorization", "amz-sdk-invocation-id", "x-amz-date"));
    }

    public AsyncRequestProducer create(AsyncExecuteRequest request, boolean isHttp2) {
        URI uri = request.request().getUri();
        AsyncRequestBuilder base = createApacheRequest(request, sanitizeUri(uri));
        addHeadersToRequest(base, request.request(), isHttp2);

        return base.build();
    }

    /**
     * The Apache HTTP client doesn't allow consecutive slashes in the URI. For S3
     * and other AWS services, this is allowed and required. This methods replaces
     * any occurrence of "//" in the URI path with "/%2F".
     *
     * @param uri The existing URI with double slashes not sanitized for Apache.
     * @return a new String containing the modified URI
     */
    @ReviewBeforeRelease("URI#toString versus #toASCIIString")
    private String sanitizeUri(URI uri) {
        String newPath = uri.getPath().replace("//", "/%2F");
        return uri.toString().replace(uri.getPath(), newPath);
    }

    private AsyncRequestBuilder createApacheRequest(AsyncExecuteRequest request, String uri) {
        switch (request.request().method()) {
            case HEAD:
                return AsyncRequestBuilder.head(uri);
            case GET:
                return AsyncRequestBuilder.get(uri);
            case DELETE:
                return AsyncRequestBuilder.delete(uri);
            case OPTIONS:
                return AsyncRequestBuilder.options(uri);
            case PATCH:
                return AsyncRequestBuilder.patch(uri).setEntity(wrapEntity(request));
            case POST:
                return AsyncRequestBuilder.post(uri).setEntity(wrapEntity(request));
            case PUT:
                return AsyncRequestBuilder.put(uri).setEntity(wrapEntity(request));
            default:
                throw new IllegalArgumentException("Unknown HTTP method name: " + request.request().method());
        }
    }

    @ReviewBeforeRelease("Buffer request if Content-Length is missing? (Apache does this, not sure about Netty)")
    private AsyncEntityProducer wrapEntity(AsyncExecuteRequest request) {
        if (request.requestContentPublisher() != null) {
            long contentLength = -1;
            Optional<String> maybeContentLength = request.request().firstMatchingHeader("Content-Length");
            if (maybeContentLength.isPresent()) {
                contentLength = Long.parseLong(maybeContentLength.get());
            }

            ContentType contentType = null;
            Optional<String> maybeContentType = request.request().firstMatchingHeader("Content-Type");
            if (maybeContentType.isPresent()) {
                contentType = ContentType.parse(maybeContentType.get());
            }

            return new ReactiveEntityProducer(request.requestContentPublisher(), contentLength, contentType, null);
        }

        return null;
    }

    /**
     * Configures the headers in the specified Apache HTTP request.
     */
    private void addHeadersToRequest(AsyncRequestBuilder httpRequest, SdkHttpRequest request, boolean isHttp2) {
        Map<String, List<String>> headers = request.headers();

        /*
         * We skip the Host header on HTTP/2. Adding it here will effectively cause two copies of the header to be sent: Apache
         * will convert the request URI into an `:authority` header, and then the server will perceive two `Host` headers,
         * possibly resulting in signature validation failures.
         *
         * Additionally, we only add the `Content-Length` header on HTTP/2, since on HTTP/1.1 the `RequestContent` insists on
         * adding it, and will throw an exception if it is already present.
         */
        if (isHttp2) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                if ("content-length".equalsIgnoreCase(key)) {
                    httpRequest.addHeader(HttpHeaders.CONTENT_LENGTH, entry.getValue().get(0));
                }
            }
        } else {
            httpRequest.addHeader(HttpHeaders.HOST, getHostHeaderValue(request));
        }

        // Copy over any other headers already in our request
        headers.entrySet().stream()
               .filter(e -> !IGNORE_HEADERS.contains(e.getKey()))
               .forEach(e -> e.getValue().forEach(h -> httpRequest.addHeader(
                               new BasicHeader(e.getKey(), h, SENSITIVE_HEADERS.contains(e.getKey())))));
    }

    private String getHostHeaderValue(SdkHttpRequest request) {
        // Apache doesn't allow us to include the port in the host header if it's a standard port for that protocol. For that
        // reason, we don't include the port when we sign the message. See {@link SdkHttpRequest#port()}.
        return !SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())
               ? request.host() + ":" + request.port()
               : request.host();
    }
}
