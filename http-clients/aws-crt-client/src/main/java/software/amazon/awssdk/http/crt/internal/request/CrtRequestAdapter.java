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

package software.amazon.awssdk.http.crt.internal.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.crt.internal.CrtAsyncRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;

@SdkInternalApi
public final class CrtRequestAdapter {
    private CrtRequestAdapter() {
    }

    public static HttpRequest toAsyncCrtRequest(CrtAsyncRequestContext request) {
        AsyncExecuteRequest sdkExecuteRequest = request.sdkRequest();
        SdkHttpRequest sdkRequest = sdkExecuteRequest.request();

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.isEmpty()) {
            encodedPath = "/";
        }

        String encodedQueryString = sdkRequest.encodedQueryParameters()
                                              .map(value -> "?" + value)
                                              .orElse("");

        HttpHeader[] crtHeaderArray = asArray(createAsyncHttpHeaderList(sdkRequest.getUri(), sdkExecuteRequest));

        return new HttpRequest(method,
                               encodedPath + encodedQueryString,
                               crtHeaderArray,
                               new CrtRequestBodyAdapter(sdkExecuteRequest.requestContentPublisher(),
                                                         request.readBufferSize()));
    }

    public static HttpRequest toCrtRequest(CrtRequestContext request) {

        HttpExecuteRequest sdkExecuteRequest = request.sdkRequest();
        SdkHttpRequest sdkRequest = sdkExecuteRequest.httpRequest();

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.isEmpty()) {
            encodedPath = "/";
        }

        String encodedQueryString = sdkRequest.encodedQueryParameters()
                                              .map(value -> "?" + value)
                                              .orElse("");

        HttpHeader[] crtHeaderArray = asArray(createHttpHeaderList(sdkRequest.getUri(), sdkExecuteRequest));

        ContentStreamProvider provider = sdkExecuteRequest.contentStreamProvider().isPresent() ?
                                         sdkExecuteRequest.contentStreamProvider().get() : null;

        if (provider != null) {
            return new HttpRequest(method,
                                   encodedPath + encodedQueryString,
                                   crtHeaderArray,
                                   new CrtRequestInputStreamAdapter(provider));
        }

        return new HttpRequest(method,
                        encodedPath + encodedQueryString,
                            crtHeaderArray, null);
    }

    private static HttpHeader[] asArray(List<HttpHeader> crtHeaderList) {
        return crtHeaderList.toArray(new HttpHeader[0]);
    }

    private static List<HttpHeader> createAsyncHttpHeaderList(URI uri, AsyncExecuteRequest sdkExecuteRequest) {
        SdkHttpRequest sdkRequest = sdkExecuteRequest.request();
        // worst case we may add 3 more headers here
        List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.numHeaders() + 3);

        // Set Host Header if needed
        if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
        }

        // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
        if (!sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE));
        }

        // Set Content-Length if needed
        Optional<Long> contentLength = sdkExecuteRequest.requestContentPublisher().contentLength();
        if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent() && contentLength.isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
        }

        // Add the rest of the Headers
        sdkRequest.forEachHeader((key, value) -> value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaderList::add));

        return crtHeaderList;
    }

    private static List<HttpHeader> createHttpHeaderList(URI uri, HttpExecuteRequest sdkExecuteRequest) {
        SdkHttpRequest sdkRequest = sdkExecuteRequest.httpRequest();
        // worst case we may add 3 more headers here
        List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.numHeaders() + 3);

        // Set Host Header if needed
        if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
        }

        // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
        if (!sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            crtHeaderList.add(new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE));
        }

        // We assume content length was set by the caller if a stream was present, so don't set it here.

        // Add the rest of the Headers
        sdkRequest.forEachHeader((key, value) -> value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaderList::add));

        return crtHeaderList;
    }
}
