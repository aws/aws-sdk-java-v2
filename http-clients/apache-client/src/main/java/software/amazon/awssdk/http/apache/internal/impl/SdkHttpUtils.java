/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class SdkHttpUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Regex which matches any of the sequences that we need to fix up after
     * URLEncoder.encode().
     */
    private static final Pattern ENCODED_CHARACTERS_PATTERN;

    static {
        StringBuilder pattern = new StringBuilder();

        pattern
                .append(Pattern.quote("+"))
                .append("|")
                .append(Pattern.quote("*"))
                .append("|")
                .append(Pattern.quote("%7E"))
                .append("|")
                .append(Pattern.quote("%2F"));

        ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern.toString());
    }

    /**
     * Returns true if the specified URI is using a non-standard port (i.e. any
     * port other than 80 for HTTP URIs or any port other than 443 for HTTPS
     * URIs).
     *
     * @return True if the specified URI is using a non-standard port, otherwise false.
     */
    public static boolean isUsingNonDefaultPort(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
        int port = uri.getPort();

        if (port <= 0) {
            return false;
        }
        if (scheme.equals("http") && port == 80) {
            return false;
        }
        if (scheme.equals("https") && port == 443) {
            return false;
        }

        return true;
    }

    /**
     * Creates an encoded query string from all the parameters in the specified
     * request.
     *
     * @param request The request containing the parameters to encode.
     * @return Null if no parameters were present, otherwise the encoded query string for the parameters present in the specified
     *         request.
     */
    public static String encodeParameters(SdkHttpFullRequest request) {

        final Map<String, List<String>> requestParams = request.getParameters();

        if (requestParams.isEmpty()) {
            return null;
        }

        final List<NameValuePair> nameValuePairs = requestParams
            .entrySet()
            .stream()
            .flatMap(e -> e.getValue()
                           .stream()
                           .map(v -> new BasicNameValuePair(e.getKey(), v)))
            .collect(Collectors.toList());

        return URLEncodedUtils.format(nameValuePairs, DEFAULT_ENCODING);
    }

    /**
     * Append the given path to the given baseUri.
     *
     * @param baseUri           The URI to append to (required, may be relative)
     * @param path              The path to append (may be null or empty).  Path should be pre-encoded.
     * @param escapeDoubleSlash Whether double-slash in the path should be escaped to "/%2F"
     * @return The baseUri with the path appended
     */
    public static String appendUri(final String baseUri, String path, final boolean escapeDoubleSlash) {
        String resultUri = baseUri;
        if (path != null && path.length() > 0) {
            if (path.startsWith("/")) {
                // trim the trailing slash in baseUri, since the path already starts with a slash
                if (resultUri.endsWith("/")) {
                    resultUri = resultUri.substring(0, resultUri.length() - 1);
                }
            } else if (!resultUri.endsWith("/")) {
                resultUri += "/";
            }
            if (escapeDoubleSlash) {
                resultUri += path.replace("//", "/%2F");
            } else {
                resultUri += path;
            }
        } else if (!resultUri.endsWith("/")) {
            resultUri += "/";
        }

        return resultUri;
    }
}
