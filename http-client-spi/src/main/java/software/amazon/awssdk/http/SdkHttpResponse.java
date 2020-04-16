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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An immutable HTTP response without access to the response body. {@link SdkHttpFullResponse} should be used when access to a
 * response body stream is required.
 */
@SdkPublicApi
@Immutable
public interface SdkHttpResponse extends ToCopyableBuilder<SdkHttpResponse.Builder, SdkHttpResponse>,
                                         SdkHttpHeaders, Serializable {

    /**
     * @return Builder instance to construct a {@link DefaultSdkHttpFullResponse}.
     */
    static SdkHttpFullResponse.Builder builder() {
        return new DefaultSdkHttpFullResponse.Builder();
    }

    /**
     * Returns the HTTP status text returned by the service.
     *
     * <p>If this was not provided by the service, empty will be returned.</p>
     */
    Optional<String> statusText();

    /**
     * Returns the HTTP status code (eg. 200, 404, etc.) returned by the service.
     *
     * <p>This will always be positive.</p>
     */
    int statusCode();

    /**
     * If we get back any 2xx status code, then we know we should treat the service call as successful.
     */
    default boolean isSuccessful() {
        return HttpStatusFamily.of(statusCode()) == HttpStatusFamily.SUCCESSFUL;
    }
    
    /**
     * Builder for a {@link DefaultSdkHttpFullResponse}.
     */
    interface Builder extends CopyableBuilder<Builder, SdkHttpResponse> {
        /**
         * The status text, exactly as it was configured with {@link #statusText(String)}.
         */
        String statusText();

        /**
         * Configure an {@link SdkHttpResponse#statusText()} to be used in the created HTTP response. This is not validated
         * until the http response is created.
         */
        Builder statusText(String statusText);

        /**
         * The status text, exactly as it was configured with {@link #statusCode(int)}.
         */
        int statusCode();

        /**
         * Configure an {@link SdkHttpResponse#statusCode()} to be used in the created HTTP response. This is not validated
         * until the http response is created.
         */
        Builder statusCode(int statusCode);

        /**
         * The HTTP headers, exactly as they were configured with {@link #headers(Map)},
         * {@link #putHeader(String, String)} and {@link #putHeader(String, List)}.
         */
        Map<String, List<String>> headers();

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
         * Add a single header to be included in the created HTTP response.
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
         * Add a single header with multiple values to be included in the created HTTP response.
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
         * Configure an {@link SdkHttpResponse#headers()} to be used in the created HTTP response. This is not validated
         * until the http response is created. This overrides any values currently configured in the builder.
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
