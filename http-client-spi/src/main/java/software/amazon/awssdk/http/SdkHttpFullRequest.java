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
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An immutable HTTP request with a possible HTTP body.
 *
 * <p>All implementations of this interface MUST be immutable. Instead of implementing this interface, consider using
 * {@link #builder()} to create an instance.</p>
 */
@SdkPublicApi
@Immutable
public interface SdkHttpFullRequest
    extends SdkHttpRequest {
    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullRequest}.
     */
    static SdkHttpFullRequest.Builder builder() {
        return new DefaultSdkHttpFullRequest.Builder();
    }

    @Override
    SdkHttpFullRequest.Builder toBuilder();

    /**
     * @return The optional {@link ContentStreamProvider} for this request.
     */
    Optional<ContentStreamProvider> contentStreamProvider();

    /**
     * A mutable builder for {@link SdkHttpFullRequest}. An instance of this can be created using
     * {@link SdkHttpFullRequest#builder()}.
     */
    interface Builder extends SdkHttpRequest.Builder {
        /**
         * Convenience method to set the {@link #protocol()}, {@link #host()}, {@link #port()},
         * {@link #encodedPath()} and extracts query parameters from a {@link URI} object.
         *
         * @param uri URI containing protocol, host, port and path.
         * @return This builder for method chaining.
         */
        @Override
        default Builder uri(URI uri) {
            Builder builder =  this.protocol(uri.getScheme())
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
        @Override
        String protocol();

        /**
         * Configure a {@link SdkHttpRequest#protocol()} to be used in the created HTTP request. This is not validated until the
         * http request is created.
         */
        @Override
        Builder protocol(String protocol);

        /**
         * The host, exactly as it was configured with {@link #host(String)}.
         */
        @Override
        String host();

        /**
         * Configure a {@link SdkHttpRequest#host()} to be used in the created HTTP request. This is not validated until the
         * http request is created.
         */
        @Override
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
        @Override
        Builder port(Integer port);

        /**
         * The path, exactly as it was configured with {@link #encodedPath(String)}.
         */
        @Override
        String encodedPath();

        /**
         * Configure an {@link SdkHttpRequest#encodedPath()} to be used in the created HTTP request. This is not validated
         * until the http request is created. This path MUST be URL encoded.
         *
         * <p>Justification of requirements: The path must be encoded when it is configured, because there is no way for the HTTP
         * implementation to distinguish a "/" that is part of a resource name that should be encoded as "%2F" from a "/" that is
         * part of the actual path.</p>
         */
        @Override
        Builder encodedPath(String path);

        /**
         * The query parameters, exactly as they were configured with {@link #rawQueryParameters(Map)},
         * {@link #putRawQueryParameter(String, String)} and {@link #putRawQueryParameter(String, List)}.
         */
        @Override
        Map<String, List<String>> rawQueryParameters();

        /**
         * Add a single un-encoded query parameter to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this parameter name in the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValue The un-encoded value for the query parameter.
         */
        @Override
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
        @Override
        Builder appendRawQueryParameter(String paramName, String paramValue);

        /**
         * Add a single un-encoded query parameter with multiple values to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this parameter name in the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValues The un-encoded values for the query parameter.
         */
        @Override
        Builder putRawQueryParameter(String paramName, List<String> paramValues);

        /**
         * Configure an {@link SdkHttpRequest#rawQueryParameters()} to be used in the created HTTP request. This is not validated
         * until the http request is created. This overrides any values currently configured in the builder. The query parameters
         * MUST NOT be URL encoded.
         *
         * <p>Justification of requirements: The query parameters must not be encoded when they are configured because some HTTP
         * implementations perform this encoding automatically.</p>
         */
        @Override
        Builder rawQueryParameters(Map<String, List<String>> queryParameters);

        /**
         * Remove all values for the requested query parameter from this builder.
         */
        @Override
        Builder removeQueryParameter(String paramName);

        /**
         * Removes all query parameters from this builder.
         */
        @Override
        Builder clearQueryParameters();

        /**
         * The path, exactly as it was configured with {@link #method(SdkHttpMethod)}.
         */
        @Override
        SdkHttpMethod method();

        /**
         * Configure an {@link SdkHttpRequest#method()} to be used in the created HTTP request. This is not validated
         * until the http request is created.
         */
        @Override
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
        @Override
        default Optional<String> firstMatchingHeader(String header) {
            return SdkHttpUtils.firstMatchingHeader(headers(), header);
        }

        /**
         * The query parameters, exactly as they were configured with {@link #headers(Map)},
         * {@link #putHeader(String, String)} and {@link #putHeader(String, List)}.
         */
        @Override
        Map<String, List<String>> headers();

        /**
         * Add a single header to be included in the created HTTP request.
         *
         * <p>This completely <b>OVERRIDES</b> any values already configured with this header name in the builder.</p>
         *
         * @param headerName The name of the header to add (eg. "Host")
         * @param headerValue The value for the header
         */
        @Override
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
        @Override
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
        @Override
        Builder appendHeader(String headerName, String headerValue);

        /**
         * Configure an {@link SdkHttpRequest#headers()} to be used in the created HTTP request. This is not validated
         * until the http request is created. This overrides any values currently configured in the builder.
         */
        @Override
        Builder headers(Map<String, List<String>> headers);

        /**
         * Remove all values for the requested header from this builder.
         */
        @Override
        Builder removeHeader(String headerName);

        /**
         * Removes all headers from this builder.
         */
        @Override
        Builder clearHeaders();

        /**
         * Set the {@link ContentStreamProvider} for this request.
         *
         * @param contentStreamProvider The ContentStreamProvider.
         * @return This object for method chaining.
         */
        Builder contentStreamProvider(ContentStreamProvider contentStreamProvider);

        /**
         * @return The {@link ContentStreamProvider} for this request.
         */
        ContentStreamProvider contentStreamProvider();

        @Override
        SdkHttpFullRequest.Builder copy();

        @Override
        SdkHttpFullRequest.Builder applyMutation(Consumer<SdkHttpRequest.Builder> mutator);

        @Override
        SdkHttpFullRequest build();
    }

}
