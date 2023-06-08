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

package software.amazon.awssdk.http.auth.internal.util;

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * A class that represents a canonical request in AWS, as documented
 * here: <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#create-canonical-request">...</a>
 */
@SdkInternalApi
public final class CanonicalRequest {
    private final SdkHttpRequest request;
    private final SdkHttpRequest.Builder requestBuilder;
    private final String contentSha256;
    private final boolean doubleUrlEncode;
    private final boolean normalizePath;
    private final List<String> headersToIgnore;
    private String canonicalRequestString;
    private StringBuilder signedHeaderStringBuilder;
    private List<Pair<String, List<String>>> canonicalHeaders;
    private String signedHeaderString;

    public CanonicalRequest(SdkHttpRequest request,
                            SdkHttpRequest.Builder requestBuilder,
                            String contentSha256,
                            boolean doubleUrlEncode,
                            boolean normalizePath,
                            List<String> headersToIgnore) {
        this.request = request;
        this.requestBuilder = requestBuilder;
        this.contentSha256 = contentSha256;
        this.doubleUrlEncode = doubleUrlEncode;
        this.normalizePath = normalizePath;
        this.headersToIgnore = headersToIgnore;
    }

    private static boolean isWhiteSpace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\u000b' || ch == '\r' || ch == '\f';
    }

    /**
     * Build the canonical-request string
     */
    public String string() {
        if (canonicalRequestString == null) {
            StringBuilder canonicalRequest = new StringBuilder(512);
            canonicalRequest.append(requestBuilder.method().toString())
                .append(SignerConstant.LINE_SEPARATOR);
            addCanonicalizedResourcePath(canonicalRequest,
                request,
                doubleUrlEncode,
                normalizePath);
            canonicalRequest.append(SignerConstant.LINE_SEPARATOR);
            addCanonicalizedQueryString(canonicalRequest, requestBuilder);
            canonicalRequest.append(SignerConstant.LINE_SEPARATOR);
            addCanonicalizedHeaderString(canonicalRequest, canonicalHeaders());
            canonicalRequest.append(SignerConstant.LINE_SEPARATOR)
                .append(signedHeaderStringBuilder())
                .append(SignerConstant.LINE_SEPARATOR)
                .append(contentSha256);
            this.canonicalRequestString = canonicalRequest.toString();
        }
        return canonicalRequestString;
    }

    private void addCanonicalizedResourcePath(StringBuilder result,
                                              SdkHttpRequest request,
                                              boolean urlEncode,
                                              boolean normalizePath) {
        String path = normalizePath ? request.getUri().normalize().getRawPath()
            : request.encodedPath();

        if (StringUtils.isEmpty(path)) {
            result.append("/");
            return;
        }

        if (urlEncode) {
            path = SdkHttpUtils.urlEncodeIgnoreSlashes(path);
        }

        if (!path.startsWith("/")) {
            result.append("/");
        }
        result.append(path);

        // Normalization can leave a trailing slash at the end of the resource path,
        // even if the input path doesn't end with one. Example input: /foo/bar/.
        // Remove the trailing slash if the input path doesn't end with one.
        boolean trimTrailingSlash = normalizePath &&
            path.length() > 1 &&
            !request.encodedPath().endsWith("/") &&
            result.charAt(result.length() - 1) == '/';
        if (trimTrailingSlash) {
            result.setLength(result.length() - 1);
        }
    }

    /**
     * Examines the specified query string parameters and returns a
     * canonicalized form.
     * <p>
     * The canonicalized query string is formed by first sorting all the query
     * string parameters, then URI encoding both the key and value and then
     * joining them, in order, separating key value pairs with an '&amp;'.
     *
     * @return A canonicalized form for the specified query string parameters.
     */
    private void addCanonicalizedQueryString(StringBuilder result, SdkHttpRequest.Builder httpRequest) {

        SortedMap<String, List<String>> sorted = new TreeMap<>();

        // Signing protocol expects the param values also to be sorted after url
        // encoding in addition to sorted parameter names.
        httpRequest.forEachRawQueryParameter((key, values) -> {
            if (StringUtils.isEmpty(key)) {
                // Do not sign empty keys.
                return;
            }

            String encodedParamName = SdkHttpUtils.urlEncode(key);

            List<String> encodedValues = new ArrayList<>(values.size());
            for (String value : values) {
                String encodedValue = SdkHttpUtils.urlEncode(value);

                // Null values should be treated as empty for the purposes of signing, not missing.
                // For example "?foo=" instead of "?foo".
                String signatureFormattedEncodedValue = encodedValue == null ? "" : encodedValue;

                encodedValues.add(signatureFormattedEncodedValue);
            }
            Collections.sort(encodedValues);
            sorted.put(encodedParamName, encodedValues);
        });

        SdkHttpUtils.flattenQueryParameters(result, sorted);
    }

    public StringBuilder signedHeaderStringBuilder() {
        if (signedHeaderStringBuilder == null) {
            signedHeaderStringBuilder = new StringBuilder();
            addSignedHeaders(signedHeaderStringBuilder, canonicalHeaders());
        }
        return signedHeaderStringBuilder;
    }

    public String signedHeaderString() {
        if (signedHeaderString == null) {
            this.signedHeaderString = signedHeaderStringBuilder().toString();
        }
        return signedHeaderString;
    }

    private List<Pair<String, List<String>>> canonicalHeaders() {
        if (canonicalHeaders == null) {
            canonicalHeaders = canonicalizeSigningHeaders(requestBuilder);
        }
        return canonicalHeaders;
    }

    private void addCanonicalizedHeaderString(StringBuilder result, List<Pair<String, List<String>>> canonicalizedHeaders) {
        canonicalizedHeaders.forEach(header -> {
            result.append(header.left());
            result.append(":");
            for (String headerValue : header.right()) {
                addAndTrim(result, headerValue);
                result.append(",");
            }
            result.setLength(result.length() - 1);
            result.append("\n");
        });
    }

    private List<Pair<String, List<String>>> canonicalizeSigningHeaders(SdkHttpRequest.Builder headers) {
        List<Pair<String, List<String>>> result = new ArrayList<>(headers.numHeaders());

        headers.forEachHeader((key, value) -> {
            String lowerCaseHeader = lowerCase(key);
            if (!headersToIgnore.contains(lowerCaseHeader)) {
                result.add(Pair.of(lowerCaseHeader, value));
            }
        });

        result.sort(Comparator.comparing(Pair::left));

        return result;
    }

    /**
     * "The addAndTrim function removes excess white space before and after values,
     * and converts sequential spaces to a single space."
     * <p>
     * https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
     * <p>
     * The collapse-whitespace logic is equivalent to:
     * <pre>
     *     value.replaceAll("\\s+", " ")
     * </pre>
     * but does not create a Pattern object that needs to compile the match
     * string; it also prevents us from having to make a Matcher object as well.
     */
    private void addAndTrim(StringBuilder result, String value) {
        int lengthBefore = result.length();
        boolean isStart = true;
        boolean previousIsWhiteSpace = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (isWhiteSpace(ch)) {
                if (previousIsWhiteSpace || isStart) {
                    continue;
                }
                result.append(' ');
                previousIsWhiteSpace = true;
            } else {
                result.append(ch);
                isStart = false;
                previousIsWhiteSpace = false;
            }
        }

        if (lengthBefore == result.length()) {
            return;
        }

        int lastNonWhitespaceChar = result.length() - 1;
        while (isWhiteSpace(result.charAt(lastNonWhitespaceChar))) {
            --lastNonWhitespaceChar;
        }

        result.setLength(lastNonWhitespaceChar + 1);
    }

    private void addSignedHeaders(StringBuilder result, List<Pair<String, List<String>>> canonicalizedHeaders) {
        for (Pair<String, List<String>> header : canonicalizedHeaders) {
            result.append(header.left()).append(';');
        }

        if (!canonicalizedHeaders.isEmpty()) {
            result.setLength(result.length() - 1);
        }
    }
}
