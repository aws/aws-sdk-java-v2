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

package software.amazon.awssdk.utils.http;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A set of utilities that assist with HTTP message-related interactions.
 */
@SdkProtectedApi
public final class SdkHttpUtils {
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Characters that we need to fix up after URLEncoder.encode().
     */
    private static final String[] ENCODED_CHARACTERS_WITH_SLASHES = new String[] {"+", "*", "%7E", "%2F"};
    private static final String[] ENCODED_CHARACTERS_WITH_SLASHES_REPLACEMENTS = new String[] {"%20", "%2A", "~", "/"};

    private static final String[] ENCODED_CHARACTERS_WITHOUT_SLASHES = new String[] {"+", "*", "%7E"};
    private static final String[] ENCODED_CHARACTERS_WITHOUT_SLASHES_REPLACEMENTS = new String[] {"%20", "%2A", "~"};

    private static final String QUERY_PARAM_DELIMITER_REGEX = "\\s*&\\s*";
    private static final Pattern QUERY_PARAM_DELIMITER_PATTERN = Pattern.compile(QUERY_PARAM_DELIMITER_REGEX);

    // List of headers that may appear only once in a request; i.e. is not a list of values.
    // Taken from https://github.com/apache/httpcomponents-client/blob/81c1bc4dc3ca5a3134c5c60e8beff08be2fd8792/httpclient5-cache/src/test/java/org/apache/hc/client5/http/impl/cache/HttpTestUtils.java#L69-L85 with modifications:
    // removed: accept-ranges, if-match, if-none-match, vary since it looks like they're defined as lists
    private static final Set<String> SINGLE_HEADERS = Stream.of("age", "authorization",
            "content-length", "content-location", "content-md5", "content-range", "content-type",
            "date", "etag", "expires", "from", "host", "if-modified-since", "if-range",
            "if-unmodified-since", "last-modified", "location", "max-forwards",
            "proxy-authorization", "range", "referer", "retry-after", "server", "user-agent")
            .collect(Collectors.toSet());


    private SdkHttpUtils() {
    }

    /**
     * Encode a string according to RFC 3986: encoding for URI paths, query strings, etc.
     */
    public static String urlEncode(String value) {
        return urlEncode(value, false);
    }

    /**
     * Encode a string according to RFC 3986, but ignore "/" characters. This is useful for encoding the components of a path,
     * without encoding the path separators.
     */
    public static String urlEncodeIgnoreSlashes(String value) {
        return urlEncode(value, true);
    }

    /**
     * Encode a string according to RFC 1630: encoding for form data.
     */
    public static String formDataEncode(String value) {
        return value == null ? null : invokeSafely(() -> URLEncoder.encode(value, DEFAULT_ENCODING));
    }

    /**
     * Decode the string according to RFC 3986: encoding for URI paths, query strings, etc.
     * <p>
     * Assumes the decoded string is UTF-8 encoded.
     *
     * @param value The string to decode.
     * @return The decoded string.
     */
    public static String urlDecode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to decode value", e);
        }
    }

    /**
     * Encode each of the keys and values in the provided query parameters using {@link #urlEncode(String)}.
     */
    public static Map<String, List<String>> encodeQueryParameters(Map<String, List<String>> rawQueryParameters) {
        return encodeMapOfLists(rawQueryParameters, SdkHttpUtils::urlEncode);
    }

    /**
     * Encode each of the keys and values in the provided form data using {@link #formDataEncode(String)}.
     */
    public static Map<String, List<String>> encodeFormData(Map<String, List<String>> rawFormData) {
        return encodeMapOfLists(rawFormData, SdkHttpUtils::formDataEncode);
    }

    private static Map<String, List<String>> encodeMapOfLists(Map<String, List<String>> map, UnaryOperator<String> encoder) {
        Validate.notNull(map, "Map must not be null.");

        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Entry<String, List<String>> queryParameter : map.entrySet()) {
            String key = queryParameter.getKey();
            String encodedKey = encoder.apply(key);

            List<String> value = queryParameter.getValue();
            List<String> encodedValue = value == null
                                        ? null
                                        : queryParameter.getValue().stream().map(encoder).collect(Collectors.toList());

            result.put(encodedKey, encodedValue);
        }

        return result;
    }

    /**
     * Encode a string for use in the path of a URL; uses URLEncoder.encode,
     * (which encodes a string for use in the query portion of a URL), then
     * applies some postfilters to fix things up per the RFC. Can optionally
     * handle strings which are meant to encode a path (ie include '/'es
     * which should NOT be escaped).
     *
     * @param value the value to encode
     * @param ignoreSlashes  true if the value is intended to represent a path
     * @return the encoded value
     */
    private static String urlEncode(String value, boolean ignoreSlashes) {
        if (value == null) {
            return null;
        }

        String encoded = invokeSafely(() -> URLEncoder.encode(value, DEFAULT_ENCODING));

        if (!ignoreSlashes) {
            return StringUtils.replaceEach(encoded,
                                           ENCODED_CHARACTERS_WITHOUT_SLASHES,
                                           ENCODED_CHARACTERS_WITHOUT_SLASHES_REPLACEMENTS);
        }

        return StringUtils.replaceEach(encoded, ENCODED_CHARACTERS_WITH_SLASHES, ENCODED_CHARACTERS_WITH_SLASHES_REPLACEMENTS);
    }

    /**
     * Encode the provided query parameters using {@link #encodeQueryParameters(Map)} and then flatten them into a string that
     * can be used as the query string in a URL. The result is not prepended with "?".
     */
    public static Optional<String> encodeAndFlattenQueryParameters(Map<String, List<String>> rawQueryParameters) {
        return flattenQueryParameters(encodeQueryParameters(rawQueryParameters));
    }

    /**
     * Encode the provided form data using {@link #encodeFormData(Map)} and then flatten them into a string that
     * can be used as the body of a form data request.
     */
    public static Optional<String> encodeAndFlattenFormData(Map<String, List<String>> rawFormData) {
        return flattenQueryParameters(encodeFormData(rawFormData));
    }

    /**
     * Flatten the provided query parameters into a string that can be used as the query string in a URL. The result is not
     * prepended with "?". This is useful when you have already-encoded query parameters you wish to flatten.
     */
    public static Optional<String> flattenQueryParameters(Map<String, List<String>> toFlatten) {
        if (toFlatten.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder result = new StringBuilder();

        for (Entry<String, List<String>> encodedQueryParameter : toFlatten.entrySet()) {
            String key = encodedQueryParameter.getKey();

            List<String> values = Optional.ofNullable(encodedQueryParameter.getValue()).orElseGet(Collections::emptyList);

            for (String value : values) {
                if (result.length() > 0) {
                    result.append('&');
                }
                result.append(key);
                if (value != null) {
                    result.append('=');
                    result.append(value);
                }
            }
        }
        return Optional.of(result.toString());
    }

    /**
     * Returns true if the specified port is the standard port for the given protocol. (i.e. 80 for HTTP or 443 for HTTPS).
     *
     * Null or -1 ports (to simplify interaction with {@link URI}'s default value) are treated as standard ports.
     *
     * @return True if the specified port is standard for the specified protocol, otherwise false.
     */
    public static boolean isUsingStandardPort(String protocol, Integer port) {
        Validate.paramNotNull(protocol, "protocol");
        Validate.isTrue(protocol.equals("http") || protocol.equals("https"),
                        "Protocol must be 'http' or 'https', but was '%s'.", protocol);

        String scheme = StringUtils.lowerCase(protocol);

        return port == null || port == -1 ||
               (scheme.equals("http") && port == 80) ||
               (scheme.equals("https") && port == 443);
    }

    /**
     * Retrieve the standard port for the provided protocol.
     */
    public static int standardPort(String protocol) {
        if (protocol.equalsIgnoreCase("http")) {
            return 80;
        } else if (protocol.equalsIgnoreCase("https")) {
            return 443;
        } else {
            throw new IllegalArgumentException("Unknown protocol: " + protocol);
        }
    }

    /**
     * Append the given path to the given baseUri, separating them with a slash, if required. The result will preserve the
     * trailing slash of the provided path.
     */
    public static String appendUri(String baseUri, String path) {
        Validate.paramNotNull(baseUri, "baseUri");
        StringBuilder resultUri = new StringBuilder(baseUri);

        if (!StringUtils.isEmpty(path)) {
            if (!baseUri.endsWith("/")) {
                resultUri.append("/");
            }

            resultUri.append(path.startsWith("/") ? path.substring(1) : path);
        }

        return resultUri.toString();
    }

    /**
     * Perform a case-insensitive search for a particular header in the provided map of headers.
     *
     * @param headers The headers to search.
     * @param header The header to search for (case insensitively).
     * @return A stream providing the values for the headers that matched the requested header.
     */
    public static Stream<String> allMatchingHeaders(Map<String, List<String>> headers, String header) {
        return headers.entrySet().stream()
                      .filter(e -> e.getKey().equalsIgnoreCase(header))
                      .flatMap(e -> e.getValue() != null ? e.getValue().stream() : Stream.empty());
    }

    /**
     * Perform a case-insensitive search for a particular header in the provided map of headers.
     *
     * @param headersToSearch The headers to search.
     * @param headersToFind The headers to search for (case insensitively).
     * @return A stream providing the values for the headers that matched the requested header.
     */
    public static Stream<String> allMatchingHeadersFromCollection(Map<String, List<String>> headersToSearch,
                                                                  Collection<String> headersToFind) {
        return headersToSearch.entrySet().stream()
                              .filter(e -> headersToFind.stream()
                                                        .anyMatch(headerToFind -> e.getKey().equalsIgnoreCase(headerToFind)))
                              .flatMap(e -> e.getValue() != null ? e.getValue().stream() : Stream.empty());
    }

    /**
     * Perform a case-insensitive search for a particular header in the provided map of headers, returning the first matching
     * header, if one is found.
     * <br>
     * This is useful for headers like 'Content-Type' or 'Content-Length' of which there is expected to be only one value present.
     *
     * @param headers The headers to search.
     * @param header The header to search for (case insensitively).
     * @return The first header that matched the requested one, or empty if one was not found.
     */
    public static Optional<String> firstMatchingHeader(Map<String, List<String>> headers, String header) {
        return allMatchingHeaders(headers, header).findFirst();
    }

    /**
     * Perform a case-insensitive search for a set of headers in the provided map of headers, returning the first matching
     * header, if one is found.
     *
     * @param headersToSearch The headers to search.
     * @param headersToFind The header to search for (case insensitively).
     * @return The first header that matched a requested one, or empty if one was not found.
     */
    public static Optional<String> firstMatchingHeaderFromCollection(Map<String, List<String>> headersToSearch,
                                                                     Collection<String> headersToFind) {
        return allMatchingHeadersFromCollection(headersToSearch, headersToFind).findFirst();
    }

    public static boolean isSingleHeader(String h) {
        return SINGLE_HEADERS.contains(StringUtils.lowerCase(h));
    }

    /**
     * Extracts query parameters from the given URI
     */
    public static Map<String, List<String>> uriParams(URI uri) {
        return QUERY_PARAM_DELIMITER_PATTERN
                      .splitAsStream(uri.getRawQuery().trim())
                      .map(s -> s.contains("=") ? s.split("=", 2) : new String[] {s, null})
                      .collect(groupingBy(a -> urlDecode(a[0]), mapping(a -> urlDecode(a[1]), toList())));
    }

}
