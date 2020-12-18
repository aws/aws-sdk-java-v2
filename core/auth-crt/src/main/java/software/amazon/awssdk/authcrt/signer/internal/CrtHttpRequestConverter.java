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

package software.amazon.awssdk.authcrt.signer.internal;

import static java.lang.Math.min;
import static software.amazon.awssdk.utils.CollectionUtils.isNullOrEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class CrtHttpRequestConverter {

    private static final String SLASH = "/";
    private static final String HOST_HEADER = "Host";
    private static final int READ_BUFFER_SIZE = 4096;

    public CrtHttpRequestConverter() {
    }

    public HttpRequest requestToCrt(SdkHttpFullRequest inputRequest) {
        String method = inputRequest.method().name();
        String encodedPath = encodedPathToCrtFormat(inputRequest.encodedPath());

        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(inputRequest.rawQueryParameters())
                .map(value -> "?" + value)
                .orElse("");

        HttpHeader[] crtHeaderArray = createHttpHeaderArray(inputRequest);

        Optional<ContentStreamProvider> contentProvider = inputRequest.contentStreamProvider();
        HttpRequestBodyStream crtInputStream = null;
        if (contentProvider.isPresent()) {
            crtInputStream = new CrtHttpRequestConverter.CrtInputStream(contentProvider.get());
        }

        return new HttpRequest(method, encodedPath + encodedQueryString, crtHeaderArray, crtInputStream);
    }

    public SdkHttpFullRequest crtRequestToHttp(SdkHttpFullRequest inputRequest, HttpRequest signedCrtRequest) {
        SdkHttpFullRequest.Builder builder = inputRequest.toBuilder();

        builder.clearHeaders();
        for (HttpHeader header : signedCrtRequest.getHeaders()) {
            builder.appendHeader(header.getName(), header.getValue());
        }

        URI fullUri = null;
        try {
            String portString = SdkHttpUtils.isUsingStandardPort(builder.protocol(), builder.port()) ? "" : ":" + builder.port();
            String encodedPath = encodedPathFromCrtFormat(inputRequest.encodedPath(), signedCrtRequest.getEncodedPath());
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

    public SdkSigningResult crtResultToAws(SdkHttpFullRequest originalRequest, AwsSigningResult signingResult) {
        SdkHttpFullRequest sdkHttpFullRequest = crtRequestToHttp(originalRequest, signingResult.getSignedRequest());
        return new SdkSigningResult(signingResult.getSignature(), sdkHttpFullRequest);
    }

    public HttpRequestBodyStream toCrtStream(byte[] data) {
        return new CrtByteArrayInputStream(data);
    }

    private HttpHeader[] createHttpHeaderArray(SdkHttpFullRequest request) {
        List<HttpHeader> crtHeaderList = new ArrayList<>(request.headers().size() + 2);

        // Set Host Header if needed
        if (isNullOrEmpty(request.headers().get(HOST_HEADER))) {
            crtHeaderList.add(new HttpHeader(HOST_HEADER, request.host()));
        }

        // Add the rest of the Headers
        for (Map.Entry<String, List<String>> headerList: request.headers().entrySet()) {
            for (String val: headerList.getValue()) {
                HttpHeader h = new HttpHeader(headerList.getKey(), val);
                crtHeaderList.add(h);
            }
        }

        return crtHeaderList.toArray(new HttpHeader[0]);
    }

    //TODO When CRT can work with SDK format empty paths, this method can be removed
    private static String encodedPathToCrtFormat(String sdkEncodedPath) {
        if (StringUtils.isEmpty(sdkEncodedPath)) {
            return "/";
        }
        return sdkEncodedPath;
    }

    //TODO When CRT can work with SDK empty paths, this method can be removed
    private static String encodedPathFromCrtFormat(String sdkEncodedPath, String crtEncodedPath) {
        if (SLASH.equals(crtEncodedPath) && StringUtils.isEmpty(sdkEncodedPath)) {
            return "";
        }
        return crtEncodedPath;
    }

    private static class CrtByteArrayInputStream implements HttpRequestBodyStream {
        private byte[] data;
        private byte[] readBuffer;
        private ByteArrayInputStream providerStream;

        CrtByteArrayInputStream(byte[] data) {
            this.data = data;
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
            providerStream = new ByteArrayInputStream(data);
        }
    }

    private static class CrtInputStream implements HttpRequestBodyStream {
        private ContentStreamProvider provider;
        private InputStream providerStream;
        private byte[] readBuffer;

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
