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

package software.amazon.awssdk.http;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * An immutable set of HTTP headers. {@link SdkHttpRequest} should be used for requests, and {@link SdkHttpResponse} should be
 * used for responses.
 */
@SdkPublicApi
@Immutable
public interface SdkHttpHeaders {
    /**
     * Returns a map of all HTTP headers in this message, sorted in case-insensitive order by their header name.
     *
     * <p>This will never be null. If there are no headers an empty map is returned.</p>
     *
     * @return An unmodifiable map of all headers in this message.
     */
    Map<String, List<String>> headers();

    /**
     * Perform a case-insensitive search for a particular header in this request, returning the first matching header, if one is
     * found.
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
}
