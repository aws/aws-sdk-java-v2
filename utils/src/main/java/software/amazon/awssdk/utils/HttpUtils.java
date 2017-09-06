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

package software.amazon.awssdk.utils;

import static java.util.Objects.requireNonNull;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.StringUtils.isNotBlank;
import static software.amazon.awssdk.utils.StringUtils.removeLastCharacter;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class HttpUtils {

    private static final String PATH_SEPARATOR = "/";

    private HttpUtils() {
    }

    /**
     * Create a URI from a base URI, resource path and map of query parameters.
     *
     * @param baseUri the base URI of the request including the protocol (e.g. https://amazonaws.com/)
     * @param resourcePath a path to a resource (e.g. /some/resource)
     * @param params a map of parameters (keys may have multiple values, null value or an empty list)
     * @return a URI with path and parameters added
     * @see #createUrl(URI, String, Map, boolean)
     */
    public static URI createUrl(URI baseUri, String resourcePath, Map<String, List<String>> queryParameters) {
        return createUrl(baseUri, resourcePath, queryParameters, false);
    }

    /**
     * Create a URL from a base URI, resource path and map of query parameters.
     *
     * <p/>
     * Takes care of encoding key/values in query parameters and multiple slashes between base URI and resource path.
     *
     * @param baseUri the base URI of the request including the protocol (e.g. https://amazonaws.com/)
     * @param resourcePath a path to a resource (e.g. /some/resource)
     * @param params a map of parameters (keys may have multiple values, null value or an empty list)
     * @param escapeDoubleSlash escapes "//" to "/%2F" in the resource path (required for some http client implementations)
     * @return a URI with path and parameters added
     */
    public static URI createUrl(URI baseUri, String resourcePath, Map<String, List<String>> params, boolean escapeDoubleSlash) {
        requireNonNull(baseUri, () -> "baseUri must be supplied");
        String base = baseUri.toString();
        StringBuilder uriBuilder = new StringBuilder(bothHaveSlash(base, resourcePath) ? removeLastCharacter(base) : base);

        if (isNotBlank(resourcePath)) {
            if (neitherHaveSlash(base, resourcePath)) {
                uriBuilder.append(PATH_SEPARATOR);
            }
            uriBuilder.append(escapeDoubleSlash ? resourcePath.replace("//", "/%2F") : resourcePath);
        }

        String flattenedParams = params != null ? params.entrySet().stream()
                                                        .flatMap(HttpUtils::flattenParams)
                                                        .collect(Collectors.joining("&")) : "";

        if (isNotBlank(flattenedParams)) {
            uriBuilder.append("?").append(flattenedParams);
        }

        return URI.create(uriBuilder.toString());
    }

    private static boolean neitherHaveSlash(String base, String path) {
        return !path.startsWith(PATH_SEPARATOR) && !base.endsWith(PATH_SEPARATOR);
    }

    private static boolean bothHaveSlash(String base, String path) {
        return base.endsWith(PATH_SEPARATOR) && path != null && path.startsWith(PATH_SEPARATOR);
    }

    private static Stream<String> flattenParams(Map.Entry<String, List<String>> e) {
        if (e.getValue() == null || e.getValue().size() == 0 || e.getValue().get(0) == null) {
            return Stream.of(encode(e.getKey()));
        }

        return e.getValue().stream().map(v -> encode(e.getKey()) + "=" + encode(v));
    }

    private static String encode(String string) {
        return invokeSafely(() -> URLEncoder.encode(string, StandardCharsets.UTF_8.name()));
    }
}
