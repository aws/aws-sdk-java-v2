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

package software.amazon.awssdk.http;

import static java.util.Collections.singletonList;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An immutable HTTP request without access to the request body. {@link SdkHttpFullRequest} should be used when access to a
 * request body stream is required.
 */
@SdkProtectedApi
@Immutable
public interface SdkHttpRequest extends SdkHttpHeaders, ToCopyableBuilder<SdkHttpRequest.Builder, SdkHttpRequest> {

    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullRequest}.
     */
    static Builder builder() {
        return new DefaultSdkHttpFullRequest.Builder();
    }

    /**
     * Returns the protocol that should be used for HTTP communication.
     *
     * <p>This will always be "https" or "http" (lowercase).</p>
     *
     * @return Either "http" or "https" depending on which protocol should be used.
     */
    String protocol();

    /**
     * Returns the host that should be communicated with.
     *
     * <p>This will never be null.</p>
     *
     * @return The host to which the request should be sent.
     */
    String host();

    /**
     * The port that should be used for HTTP communication. If this was not configured when the request was created, it will be
     * derived from the protocol. For "http" it would be 80, and for "https" it would be 443.
     *
     * <p>Important Note: AWS signing DOES NOT include the port when the request is signed if the default port for the protocol is
     * being used. When sending requests via http over port 80 or via https over port 443, the URI or host header MUST NOT include
     * the port or a signature error will be raised from the service for signed requests. HTTP plugin implementers are encouraged
     * to use the {@link #getUri()} method for generating the URI to use for communicating with AWS to ensure the URI used in the
     * request matches the URI used during signing.</p>
     *
     * @return The port that should be used for HTTP communication.
     */
    int port();

    /**
     * Returns the URL-encoded path that should be used in the HTTP request.
     *
     * <p>If a path is configured, the path will always start with '/' and may or may not end with '/', depending on what the
     * service might expect. If a path is not configured, this will always return empty-string (ie. ""). Note that '/' is also a
     * valid path.</p>
     *
     * @return The path to the resource being requested.
     */
    String encodedPath();

    /**
     * Returns a map of all non-URL encoded parameters in this request. HTTP plugins can use
     * {@link SdkHttpUtils#encodeQueryParameters(Map)} to encode parameters into map-form, or
     * {@link SdkHttpUtils#encodeAndFlattenQueryParameters(Map)} to encode the parameters into uri-formatted string form.
     *
     * <p>This will never be null. If there are no parameters an empty map is returned.</p>
     *
     * @return An unmodifiable map of all non-encoded parameters in this request.
     */
    Map<String, List<String>> rawQueryParameters();

    /**
     * Convert this HTTP request's protocol, host, port, path and query string into a properly-encoded URI string that matches the
     * URI string used for AWS request signing.
     *
     * <p>The URI's port will be missing (-1) when the {@link #port()} is the default port for the {@link #protocol()}. (80 for
     * http and 443 for https). This is to reflect the fact that request signature does not include the port.</p>
     *
     * @return The URI for this request, formatted in the same way the AWS HTTP request signer uses the URI in the signature.
     */
    default URI getUri() {
        // We can't create a URI by simply passing the query parameters into the URI constructor that takes a query string,
        // because URI will re-encode them. Because we want to encode them using our encoder, we have to build the URI
        // ourselves and pass it to the single-argument URI constructor that doesn't perform the encoding.

        String encodedQueryString = SdkHttpUtils.encodeAndFlattenQueryParameters(rawQueryParameters())
                                                .map(value -> "?" + value)
                                                .orElse("");

        // Do not include the port in the URI when using the default port for the protocol.
        String portString = SdkHttpUtils.isUsingStandardPort(protocol(), port()) ? "" : ":" + port();

        return URI.create(protocol() + "://" + host() + portString + encodedPath() + encodedQueryString);
    }

    /**
     * Returns the HTTP method (GET, POST, etc) to use when sending this request.
     *
     * <p>This will never be null.</p>
     *
     * @return The HTTP method to use when sending this request.
     */
    SdkHttpMethod method();

    /**
     * A mutable builder for {@link SdkHttpFullRequest}. An instance of this can be created using
     * {@link SdkHttpFullRequest#builder()}.
     */
    interface Builder extends CopyableBuilder<Builder, SdkHttpRequest> {
        /**
         * Convenience method to set the {@link #protocol()}, {@link #host()}, {@link #port()},
         * {@link #encodedPath()} and extracts query parameters from a {@link URI} object.
         *
         * @param uri URI containing protocol, host, port and path.
         * @return This builder for method chaining.
         */
        default Builder uri(URI uri) {
            Builder builder = this.protocol(uri.getScheme())
                       .host(uri.getHost())
                       .port(uri.getPort())
                       .encodedPath(SdkHttpUtils.appendUri(uri.getRawPath(), encodedPath()));
            if (uri.getRawQuery() != null) {
                builder.clearQueryParameters();
                SdkHttpUtils.uriParams(uri)
                            .forEach(this::putRawQueryParameter);
            }
            return builder;
        }

        /**
         * The protocol, exactly as it was configured with {@link #protocol(String)}.
         */
        String protocol();

        /**
         * Configure a {@link SdkHttpRequest#protocol()} to be used in the created HTTP request. This is not validated until the
         * http request is created.
         */
        Builder protocol(String protocol);

        /**
         * The host, exactly as it was configured with {@link #host(String)}.
         */
        String host();

        /**
         * Configure a {@link SdkHttpRequest#host()} to be used in the created HTTP request. This is not validated until the
         * http request is created.
         */
        Builder host(String host);

        /**
         * The port, exactly as it was configured with {@link #port(Integer)}.
         */
        Integer port();

        /**
         * Configure a {@link SdkHttpRequest#port()} to be used in the created HTTP request. This is not validated until the
         * http request is created. In order to simplify mapping from a {@link URI}, "-1" will be treated as "null" when the http
         * request is created.
         */
        Builder port(Integer port);

        /**
         * The path, exactly as it was configured with {@link #encodedPath(String)}.
         */
        String encodedPath();

        /**
         * Configure an {@link SdkHttpRequest#encodedPath()} to be used in the created HTTP request. This is not validated
         * until the http request is created. This path MUST be URL encoded.
         *
         * <p>Justification of requirements: The path must be encoded when it is configured, because there is no way for the HTTP
         * implementation to distinguish a "/" that is part of a resource name that should be encoded as "%2F" from a "/" that is
         * part of the actual path.</p>
         */
        Builder encodedPath(String path);

        /**
         * The query parameters, exactly as they were configured with {@link #rawQueryParameters(Map)},
         * {@link #putRawQueryParameter(String, String)} and {@link #putRawQueryParameter(String, List)}.
         */
        Map<String, List<String>> rawQueryParameters();

        /**
         * Add a single un-encoded query parameter to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this parameter name in the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValue The un-encoded value for the query parameter.
         */
        default Builder putRawQueryParameter(String paramName, String paramValue) {
            return putRawQueryParameter(paramName, singletonList(paramValue));
        }

        /**
         * Add a single un-encoded query parameter to be included in the created HTTP request.
         *
         * <p>This will <b>ADD</b> the value to any existing values already configured with this parameter name in
         * the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValue The un-encoded value for the query parameter.
         */
        Builder appendRawQueryParameter(String paramName, String paramValue);

        /**
         * Add a single un-encoded query parameter with multiple values to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this parameter name in the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValues The un-encoded values for the query parameter.
         */
        Builder putRawQueryParameter(String paramName, List<String> paramValues);

        /**
         * Configure an {@link SdkHttpRequest#rawQueryParameters()} to be used in the created HTTP request. This is not validated
         * until the http request is created. This overrides any values currently configured in the builder. The query parameters
         * MUST NOT be URL encoded.
         *
         * <p>Justification of requirements: The query parameters must not be encoded when they are configured because some HTTP
         * implementations perform this encoding automatically.</p>
         */
        Builder rawQueryParameters(Map<String, List<String>> queryParameters);

        /**
         * Remove all values for the requested query parameter from this builder.
         */
        Builder removeQueryParameter(String paramName);

        /**
         * Removes all query parameters from this builder.
         */
        Builder clearQueryParameters();

        /**
         * The path, exactly as it was configured with {@link #method(SdkHttpMethod)}.
         */
        SdkHttpMethod method();

        /**
         * Configure an {@link SdkHttpRequest#method()} to be used in the created HTTP request. This is not validated
         * until the http request is created.
         */
        Builder method(SdkHttpMethod httpMethod);

        /**
         * Perform a case-insensitive search for a particular header in this request, returning the first matching header, if one
         * is found.
         *
         * <p>This is useful for headers like 'Content-Type' or 'Content-Length' of which there is expected to be only one value
         * present.</p>
         *
         * <p>This is equivalent to invoking {@link SdkHttpUtils#firstMatchingHeader(Map, String)}</p>.
         *
         * @param header The header to search for (case insensitively).
         * @return The first header that matched the requested one, or empty if one was not found.
         */
        default Optional<String> firstMatchingHeader(String header) {
            return SdkHttpUtils.firstMatchingHeader(headers(), header);
        }

        /**
         * The query parameters, exactly as they were configured with {@link #headers(Map)},
         * {@link #putHeader(String, String)} and {@link #putHeader(String, List)}.
         */
        Map<String, List<String>> headers();

        /**
         * Add a single header to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this header name in the builder.</p>
         *
         * @param headerName The name of the header to add (eg. "Host")
         * @param headerValue The value for the header
         */
        default Builder putHeader(String headerName, String headerValue) {
            return putHeader(headerName, singletonList(headerValue));
        }

        /**
         * Add a single header with multiple values to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this header name in the builder.</p>
         *
         * @param headerName The name of the header to add
         * @param headerValues The values for the header
         */
        Builder putHeader(String headerName, List<String> headerValues);

        /**
         * Add a single header to be included in the created HTTP request.
         *
         * <p>This will <b>ADD</b> the value to any existing values already configured with this header name in
         * the builder.</p>
         *
         * @param headerName The name of the header to add
         * @param headerValue The value for the header
         */
        Builder appendHeader(String headerName, String headerValue);

        /**
         * Configure an {@link SdkHttpRequest#headers()} to be used in the created HTTP request. This is not validated
         * until the http request is created. This overrides any values currently configured in the builder.
         */
        Builder headers(Map<String, List<String>> headers);

        /**
         * Remove all values for the requested header from this builder.
         */
        Builder removeHeader(String headerName);

        /**
         * Removes all headers from this builder.
         */
        Builder clearHeaders();
    }
}
