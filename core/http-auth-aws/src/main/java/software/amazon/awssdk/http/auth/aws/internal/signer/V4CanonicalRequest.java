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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * A class that represents a canonical request in AWS, as documented:
 * <p>
 * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#create-canonical-request
 * </p>
 */
@SdkInternalApi
@Immutable
public final class V4CanonicalRequest {
    private static final List<String> HEADERS_TO_IGNORE_IN_LOWER_CASE =
        Arrays.asList("connection", "x-amzn-trace-id", "user-agent", "expect");

    private final SdkHttpRequest request;
    private final String contentHash;
    private final Options options;

    // Compute these fields lazily when, and, if needed.
    private String canonicalUri;
    private SortedMap<String, List<String>> canonicalParams;
    private List<Pair<String, List<String>>> canonicalHeaders;
    private String canonicalQueryString;
    private String canonicalHeadersString;
    private String signedHeadersString;
    private String canonicalRequestString;

    /**
     * Create a canonical request.
     * <p>
     * Each parameter of a canonical request is set upon creation of this object.
     * <p>
     * To get such a parameter (i.e. the canonical request string), simply call the getter for that parameter (i.e.
     * getCanonicalRequestString())
     */
    public V4CanonicalRequest(SdkHttpRequest request, String contentHash, Options options) {
        this.request = request;
        this.contentHash = contentHash;
        this.options = options;
    }

    /**
     * Get the string representing which headers are part of the signing process. Header names are separated by a semicolon.
     */
    public String getSignedHeadersString() {
        if (signedHeadersString == null) {
            signedHeadersString = getSignedHeadersString(canonicalHeaders());
        }
        return signedHeadersString;
    }

    /**
     * Get the canonical request string.
     */
    public String getCanonicalRequestString() {
        if (canonicalRequestString == null) {
            canonicalRequestString = getCanonicalRequestString(request.method().toString(), canonicalUri(),
                                                               canonicalQueryString(), canonicalHeadersString(),
                                                               getSignedHeadersString(), contentHash);
        }
        return canonicalRequestString;
    }

    private SortedMap<String, List<String>> canonicalQueryParams() {
        if (canonicalParams == null) {
            canonicalParams = getCanonicalQueryParams(request);
        }
        return canonicalParams;
    }

    private List<Pair<String, List<String>>> canonicalHeaders() {
        if (canonicalHeaders == null) {
            canonicalHeaders = getCanonicalHeaders(request);
        }
        return canonicalHeaders;
    }

    private String canonicalUri() {
        if (canonicalUri == null) {
            canonicalUri = getCanonicalUri(request, options);
        }
        return canonicalUri;
    }

    private String canonicalQueryString() {
        if (canonicalQueryString == null) {
            canonicalQueryString = getCanonicalQueryString(canonicalQueryParams());
        }
        return canonicalQueryString;
    }

    private String canonicalHeadersString() {
        if (canonicalHeadersString == null) {
            canonicalHeadersString = getCanonicalHeadersString(canonicalHeaders());
        }
        return canonicalHeadersString;
    }

    /**
     * Get the list of headers that are to be signed.
     * <p>
     * If calling from a site with the request object handy, this method should be used instead of passing the headers themselves,
     * as doing so creates a redundant copy.
     */
    public static List<Pair<String, List<String>>> getCanonicalHeaders(SdkHttpRequest request) {
        List<Pair<String, List<String>>> result = new ArrayList<>(request.numHeaders());

        // headers retrieved from the request are already sorted case-insensitively
        request.forEachHeader((key, value) -> {
            String lowerCaseHeader = lowerCase(key);
            if (!HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCaseHeader)) {
                result.add(Pair.of(lowerCaseHeader, value));
            }
        });

        result.sort(Comparator.comparing(Pair::left));

        return result;
    }

    /**
     * Get the list of headers that are to be signed. The supplied map of headers is expected to be sorted case-insensitively.
     */
    public static List<Pair<String, List<String>>> getCanonicalHeaders(Map<String, List<String>> headers) {
        List<Pair<String, List<String>>> result = new ArrayList<>(headers.size());

        headers.forEach((key, value) -> {
            String lowerCaseHeader = lowerCase(key);
            if (!HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCaseHeader)) {
                result.add(Pair.of(lowerCaseHeader, value));
            }
        });

        result.sort(Comparator.comparing(Pair::left));

        return result;
    }

    /**
     * Get the string representing the headers that will be signed and their values. The input list is expected to be sorted
     * case-insensitively.
     * <p>
     * The output string will have header names as lower-case, sorted in alphabetical order, and followed by a colon.
     * <p>
     * Values are trimmed of any leading/trailing spaces, sequential spaces are converted to single space, and multiple values are
     * comma separated.
     * <p>
     * Each header-value pair is separated by a newline.
     */
    public static String getCanonicalHeadersString(List<Pair<String, List<String>>> canonicalHeaders) {
        // 2048 chosen experimentally to avoid always needing to resize the string builder's internal byte array.
        // The minimal DynamoDB get-item request at the time of testing used ~1100 bytes. 2048 was chosen as the
        // next-highest power-of-two.
        StringBuilder result = new StringBuilder(2048);
        canonicalHeaders.forEach(header -> {
            result.append(header.left());
            result.append(":");
            for (String headerValue : header.right()) {
                addAndTrim(result, headerValue);
                result.append(",");
            }
            result.setLength(result.length() - 1);
            result.append("\n");
        });
        return result.toString();
    }

    /**
     * Get the string representing which headers are part of the signing process. Header names are separated by a semicolon.
     */
    public static String getSignedHeadersString(List<Pair<String, List<String>>> canonicalHeaders) {
        String signedHeadersString;
        StringBuilder headersString = new StringBuilder(512);
        for (Pair<String, List<String>> header : canonicalHeaders) {
            headersString.append(header.left()).append(";");
        }
        // get rid of trailing semicolon
        signedHeadersString = headersString.toString();

        boolean trimTrailingSemicolon = signedHeadersString.length() > 1 &&
                                        signedHeadersString.endsWith(";");

        if (trimTrailingSemicolon) {
            signedHeadersString = signedHeadersString.substring(0, signedHeadersString.length() - 1);
        }
        return signedHeadersString;
    }

    /**
     * Get the canonical request string.
     * <p>
     * Each {@link String} parameter is separated by a newline character.
     */
    private static String getCanonicalRequestString(String httpMethod, String canonicalUri, String canonicalParamsString,
                                                    String canonicalHeadersString, String signedHeadersString,
                                                    String contentHash) {
        return httpMethod + SignerConstant.LINE_SEPARATOR +
               canonicalUri + SignerConstant.LINE_SEPARATOR +
               canonicalParamsString + SignerConstant.LINE_SEPARATOR +
               canonicalHeadersString + SignerConstant.LINE_SEPARATOR +
               signedHeadersString + SignerConstant.LINE_SEPARATOR +
               contentHash;
    }

    /**
     * "The addAndTrim function removes excess white space before and after values, and converts sequential spaces to a single
     * space."
     * <p>
     * https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
     * <p>
     * The collapse-whitespace logic is equivalent to:
     * <pre>
     *     value.replaceAll("\\s+", " ")
     * </pre>
     * but does not create a Pattern object that needs to compile the match string; it also prevents us from having to make a
     * Matcher object as well.
     */
    private static void addAndTrim(StringBuilder result, String value) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        int valueLength = value.length();

        int start = 0;
        // Find first non-whitespace
        while (isWhiteSpace(value.charAt(start))) {
            ++start;
            if (start >= valueLength) {
                return;
            }
        }

        // Add things word-by-word
        int lastWordStart = start;
        boolean lastWasWhitespace = false;
        for (int i = start; i < valueLength; i++) {
            char c = value.charAt(i);

            if (isWhiteSpace(c)) {
                if (!lastWasWhitespace) {
                    // End of word, add word
                    result.append(value, lastWordStart, i);
                    lastWasWhitespace = true;
                }
            } else {
                if (lastWasWhitespace) {
                    // Start of new word, add space
                    result.append(' ');
                    lastWordStart = i;
                    lastWasWhitespace = false;
                }
            }
        }

        if (!lastWasWhitespace) {
            result.append(value, lastWordStart, valueLength);
        }
    }

    /**
     * Get the uri-encoded version of the absolute path component URL.
     * <p>
     * If the path is empty, a single-forward slash ('/') is returned.
     */
    private static String getCanonicalUri(SdkHttpRequest request, Options options) {
        String path = options.normalizePath ? request.getUri().normalize().getRawPath()
                                            : request.encodedPath();

        if (StringUtils.isEmpty(path)) {
            return "/";
        }

        if (options.doubleUrlEncode) {
            path = SdkHttpUtils.urlEncodeIgnoreSlashes(path);
        }

        if (!path.startsWith("/")) {
            path += "/";
        }

        // Normalization can leave a trailing slash at the end of the resource path,
        // even if the input path doesn't end with one. Example input: /foo/bar/.
        // Remove the trailing slash if the input path doesn't end with one.
        boolean trimTrailingSlash = options.normalizePath &&
                                    path.length() > 1 &&
                                    !request.getUri().getPath().endsWith("/") &&
                                    path.charAt(path.length() - 1) == '/';

        if (trimTrailingSlash) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Get the sorted map of query parameters that are to be signed.
     */
    private static SortedMap<String, List<String>> getCanonicalQueryParams(SdkHttpRequest request) {
        SortedMap<String, List<String>> sorted = new TreeMap<>();

        // Signing protocol expects the param values also to be sorted after url
        // encoding in addition to sorted parameter names.
        request.forEachRawQueryParameter((key, values) -> {
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
        return sorted;
    }

    /**
     * Get the string representing query string parameters. Parameters are URL-encoded and separated by an ampersand.
     * <p>
     * Reserved characters are percent-encoded, names and values are encoded separately and empty parameters have an equals-sign
     * appended before encoding.
     * <p>
     * After encoding, parameters are sorted alphabetically by key name.
     * <p>
     * If no query string is given, an empty string ("") is returned.
     */
    private static String getCanonicalQueryString(SortedMap<String, List<String>> canonicalParams) {
        if (canonicalParams.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(512);
        SdkHttpUtils.flattenQueryParameters(stringBuilder, canonicalParams);
        return stringBuilder.toString();
    }

    private static boolean isWhiteSpace(char ch) {
        switch (ch) {
            case ' ':
            case '\t':
            case '\n':
            case '\u000b':
            case '\r':
            case '\f':
                return true;
            default:
                return false;
        }
    }

    /**
     * A class for representing options used when creating a {@link V4CanonicalRequest}
     */
    public static class Options {
        final boolean doubleUrlEncode;
        final boolean normalizePath;

        public Options(boolean doubleUrlEncode, boolean normalizePath) {
            this.doubleUrlEncode = doubleUrlEncode;
            this.normalizePath = normalizePath;
        }
    }
}
