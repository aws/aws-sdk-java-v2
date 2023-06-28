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

package software.amazon.awssdk.http.auth.aws.crt.internal;

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public class CrtHttpRequestConverter {

    private static final String SLASH = "/";
    private static final String HOST_HEADER = "Host";
    private static final int READ_BUFFER_SIZE = 4096;

    /**
     * Convert an {@link SdkHttpRequest} to an {@link HttpRequest}.
     */
    public static HttpRequest toRequest(SdkHttpRequest request, ContentStreamProvider payload) {
        String method = request.method().name();
        String encodedPath = encodedPathToCrtFormat(request.encodedPath());

        String encodedQueryString = request.encodedQueryParameters().map(value -> "?" + value).orElse("");

        HttpHeader[] crtHeaderArray = createHttpHeaderArray(request);

        HttpRequestBodyStream crtInputStream = null;
        if (payload != null) {
            crtInputStream = new CrtInputStream(payload);
        }

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray, crtInputStream);
    }

    /**
     * Convert an {@link HttpRequest} to an {@link SdkHttpRequest}.
     */
    public static SdkHttpRequest toRequest(SdkHttpRequest request, HttpRequest crtRequest) {
        SdkHttpRequest.Builder builder = request.toBuilder();

        builder.clearHeaders();
        for (HttpHeader header : crtRequest.getHeaders()) {
            builder.appendHeader(header.getName(), header.getValue());
        }

        URI fullUri;
        try {
            String portString = SdkHttpUtils.isUsingStandardPort(builder.protocol(), builder.port()) ? "" : ":" + builder.port();
            String encodedPath = encodedPathFromCrtFormat(request.encodedPath(), crtRequest.getEncodedPath());
            String fullUriString = builder.protocol() + "://" + builder.host() + portString + encodedPath;
            fullUri = new URI(fullUriString);
        } catch (URISyntaxException e) {
            return null;
        }

        builder.encodedPath(fullUri.getRawPath());
        String remainingQuery = fullUri.getQuery();

        builder.clearQueryParameters();
        while (remainingQuery != null && remainingQuery.length() > 0) {
            int nextQuery = remainingQuery.indexOf('&');
            int nextAssign = remainingQuery.indexOf('=');
            if (nextAssign < nextQuery || (nextAssign >= 0 && nextQuery < 0)) {
                String queryName = remainingQuery.substring(0, nextAssign);
                String queryValue = remainingQuery.substring(nextAssign + 1);
                if (nextQuery >= 0) {
                    queryValue = remainingQuery.substring(nextAssign + 1, nextQuery);
                }

                builder.appendRawQueryParameter(queryName, queryValue);
            } else {
                String queryName = remainingQuery;
                if (nextQuery >= 0) {
                    queryName = remainingQuery.substring(0, nextQuery);
                }

                builder.appendRawQueryParameter(queryName, null);
            }

            if (nextQuery >= 0) {
                remainingQuery = remainingQuery.substring(nextQuery + 1);
            } else {
                break;
            }
        }

        return builder.build();
    }

    private static HttpHeader[] createHttpHeaderArray(SdkHttpRequest request) {
        List<HttpHeader> crtHeaderList = new ArrayList<>(request.numHeaders() + 2);

        // Set Host Header if needed
        if (!request.firstMatchingHeader(HOST_HEADER).isPresent()) {
            crtHeaderList.add(new HttpHeader(HOST_HEADER, request.host()));
        }

        // Add the rest of the Headers
        request.forEachHeader((name, values) -> {
            for (String val : values) {
                HttpHeader h = new HttpHeader(name, val);
                crtHeaderList.add(h);
            }
        });

        return crtHeaderList.toArray(new HttpHeader[0]);
    }

    private static String encodedPathToCrtFormat(String sdkEncodedPath) {
        if (StringUtils.isEmpty(sdkEncodedPath)) {
            return "/";
        }
        return sdkEncodedPath;
    }

    private static String encodedPathFromCrtFormat(String sdkEncodedPath, String crtEncodedPath) {
        if (SLASH.equals(crtEncodedPath) && StringUtils.isEmpty(sdkEncodedPath)) {
            return "";
        }
        return crtEncodedPath;
    }

    private static class CrtInputStream implements HttpRequestBodyStream {
        private final ContentStreamProvider provider;
        private final byte[] readBuffer;
        private InputStream providerStream;

        CrtInputStream(ContentStreamProvider provider) {
            this.provider = provider;
            this.readBuffer = new byte[READ_BUFFER_SIZE];
        }

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
            if (provider == null) {
                throw new IllegalStateException("Cannot reset position while provider is null");
            }
            try {
                createNewStream();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            return true;
        }

        private void createNewStream() throws IOException {
            if (provider == null) {
                throw new IllegalStateException("Cannot create a new stream while provider is null");
            }
            if (providerStream != null) {
                providerStream.close();
            }
            providerStream = provider.newStream();
        }
    }
}
