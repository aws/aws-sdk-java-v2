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

package software.amazon.awssdk.core.internal.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Internal implementation of {@link SdkHttpResponse}.
 */
@Immutable
@SdkInternalApi
public final class DefaultSdkHttpResponse implements SdkHttpResponse {

    private final Map<String, List<String>> headers;
    private final int statusCode;
    private final String statusText;

    private DefaultSdkHttpResponse(Map<String, List<String>> headers, int statusCode, String statusText) {
        this.headers = CollectionUtils.deepUnmodifiableMap(headers);
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    /**
     * Static factory to create an {@link DefaultSdkHttpResponse} from the details in a {@link
     * HttpResponse}.
     */
    public static DefaultSdkHttpResponse from(HttpResponse httpResponse) {

        // Legacy HttpResponse only supports a single value for a header
        Map<String, List<String>> headers =
            httpResponse.getHeaders().entrySet().stream()
                        .collect(HashMap::new, (m, e) -> m.put(e.getKey(), Collections.singletonList(e.getValue())),
                                 Map::putAll);

        return new DefaultSdkHttpResponse(headers, httpResponse.getStatusCode(), httpResponse.getStatusText());
    }

    @Override
    public Optional<String> statusText() {
        return Optional.ofNullable(statusText);
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }
}
