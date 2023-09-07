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

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.crt.internal.CrtAsyncRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtSyncRequestContext;

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

    public static HttpRequest toSyncCrtRequest(CrtSyncRequestContext request) {

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

        HttpHeader[] crtHeaderArray = asArray(createSyncHttpHeaderList(sdkRequest.getUri(), sdkExecuteRequest));

        ContentStreamProvider provider = sdkExecuteRequest.contentStreamProvider().isPresent() ?
                                         sdkExecuteRequest.contentStreamProvider().get() : null;

        if (provider != null) {
            return new HttpRequest(method,
                                   encodedPath + encodedQueryString,
                                   crtHeaderArray,
                                   new HttpRequestBodyStream() {
                                       private InputStream providerStream;
                                       private final byte[] readBuffer = new byte[READ_BUFFER_SIZE];
                                       ;
                                       private static final int READ_BUFFER_SIZE = 16 * 1024;


                                       @Override
                                       public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
                                           int read = 0;

                                           try {
                                               if (providerStream == null) {
                                                   createNewStream();
                                               }

                                               int toRead = min(READ_BUFFER_SIZE, bodyBytesOut.remaining());
                                               read = providerStream.read(readBuffer, 0, toRead);

                                               if (read > 0) {
                                                   bodyBytesOut.put(readBuffer, 0, read);
                                               }
                                           } catch (IOException ioe) {
                                               throw new RuntimeException(ioe);
                                           }

                                           return read < 0;
                                       }

                                       @Override
                                       public boolean resetPosition() {
                                           try {
                                               createNewStream();
                                           } catch (IOException ioe) {
                                               throw new RuntimeException(ioe);
                                           }

                                           return true;
                                       }

                                       private void createNewStream() throws IOException {
                                           if (providerStream != null) {
                                               providerStream.close();
                                           }
                                           providerStream = provider.newStream();
                                       }
                                   });
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
        sdkRequest.forEachHeader((key, value) -> {
            value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaderList::add);
        });

        return crtHeaderList;
    }

    private static List<HttpHeader> createSyncHttpHeaderList(URI uri, HttpExecuteRequest sdkExecuteRequest) {
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

        // Set Content-Length if needed
        if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent() && sdkExecuteRequest.contentStreamProvider().isPresent()) {
            try  (InputStream inputStream = sdkExecuteRequest.contentStreamProvider().get().newStream()) {
                byte[] scratch = new byte[1024];
                Long contentLength = 0L;
                int read = 0;
                while ((read = inputStream.read(scratch)) > 0) {
                    contentLength += read;
                }

                crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength)));
            } catch (IOException e) {
                // do nothing intentionally.
            }
        }

        // Add the rest of the Headers
        sdkRequest.forEachHeader((key, value) -> {
            value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaderList::add);
        });

        return crtHeaderList;
    }
}
