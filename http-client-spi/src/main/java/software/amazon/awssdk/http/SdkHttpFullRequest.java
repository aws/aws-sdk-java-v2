/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
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
        extends SdkHttpRequest, ToCopyableBuilder<SdkHttpFullRequest.Builder, SdkHttpFullRequest> {
    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullRequest}.
     */
    static Builder builder() {
        return new DefaultSdkHttpFullRequest.Builder();
    }

    /**
     * Returns the optional stream containing the payload data to include for this request.
     *
     * <p>When the request does not include payload data, this will return {@link Optional#empty()}.
     *
     * @return The optional stream containing the payload data to include for this request, or empty if there is no payload.
     */
    Optional<InputStream> content();

    /**
     * A mutable builder for {@link SdkHttpFullRequest}. An instance of this can be created using
     * {@link SdkHttpFullRequest#builder()}.
     */
    interface Builder extends CopyableBuilder<Builder, SdkHttpFullRequest> {
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
         * {@link #rawQueryParameter(String, String)} and {@link #rawQueryParameter(String, List)}.
         */
        Map<String, List<String>> rawQueryParameters();

        /**
         * Add a single un-encoded query parameter to be included in the created HTTP request.
         *
         * <p>This completely overrides any values already configured with this parameter name in the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValue The un-encoded value for the query parameter.
         */
        default Builder rawQueryParameter(String paramName, String paramValue) {
            return rawQueryParameter(paramName, singletonList(paramValue));
        }

        /**
         * Add a single un-encoded query parameter with multiple values to be included in the created HTTP request.
         *
         * <p>This completely overrides any values already configured with this parameter name in the builder.</p>
         *
         * @param paramName The name of the query parameter to add
         * @param paramValues The un-encoded values for the query parameter.
         */
        Builder rawQueryParameter(String paramName, List<String> paramValues);

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
         * {@link #header(String, String)} and {@link #header(String, List)}.
         */
        Map<String, List<String>> headers();

        /**
         * Add a single header to be included in the created HTTP request.
         *
         * <p>This completely overrides any values already configured with this header name in the builder.</p>
         *
         * @param headerName The name of the header to add (eg. "Host")
         * @param headerValue The value for the header
         */
        default Builder header(String headerName, String headerValue) {
            return header(headerName, singletonList(headerValue));
        }

        /**
         * Add a single header with multiple values to be included in the created HTTP request.
         *
         * <p>This completely overrides any values already configured with this header name in the builder.</p>
         *
         * @param headerName The name of the header to add
         * @param headerValues The values for the header
         */
        Builder header(String headerName, List<String> headerValues);

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

        /**
         * The content, exactly as it was configured with {@link #content(InputStream)}.
         */
        InputStream content();

        /**
         * Configure an {@link SdkHttpFullRequest#content()} to be used in the created HTTP request. This is not validated until
         * the http request is created.
         */
        Builder content(InputStream content);
    }

}
