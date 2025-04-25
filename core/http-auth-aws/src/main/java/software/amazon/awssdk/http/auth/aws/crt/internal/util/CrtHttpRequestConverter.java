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

package software.amazon.awssdk.http.auth.aws.crt.internal.util;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.HOST;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.crt.internal.io.CrtInputStream;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
import software.amazon.awssdk.utils.uri.SdkURI;

@SdkInternalApi
public final class CrtHttpRequestConverter {

    private CrtHttpRequestConverter() {
    }

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
            fullUri = SdkURI.getInstance().newURI(fullUriString);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Full URI could not be formed.", e);
        }

        builder.encodedPath(fullUri.getRawPath());
        String remainingQuery = fullUri.getRawQuery();

        builder.clearQueryParameters();
        while (remainingQuery != null && !remainingQuery.isEmpty()) {
            int nextQuery = remainingQuery.indexOf('&');
            int nextAssign = remainingQuery.indexOf('=');
            if (nextAssign < nextQuery || (nextAssign >= 0 && nextQuery < 0)) {
                String queryName = remainingQuery.substring(0, nextAssign);
                String queryValue = remainingQuery.substring(nextAssign + 1);
                if (nextQuery >= 0) {
                    queryValue = remainingQuery.substring(nextAssign + 1, nextQuery);
                }

                String decodedQueryValue = SdkHttpUtils.urlDecode(queryValue);
                String decodedQueryName = SdkHttpUtils.urlDecode(queryName);
                builder.appendRawQueryParameter(decodedQueryName, decodedQueryValue);
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
        if (!request.firstMatchingHeader(HOST).isPresent()) {
            crtHeaderList.add(new HttpHeader(HOST, request.host()));
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
        if ("/".equals(crtEncodedPath) && StringUtils.isEmpty(sdkEncodedPath)) {
            return "";
        }
        return crtEncodedPath;
    }

    public static HttpRequestBodyStream toCrtStream(byte[] data) {
        return new CrtInputStream(() -> new ByteArrayInputStream(data));
    }
}
