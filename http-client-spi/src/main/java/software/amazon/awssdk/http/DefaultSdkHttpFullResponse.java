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

import static software.amazon.awssdk.utils.CollectionUtils.deepUnmodifiableMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal implementation of {@link SdkHttpFullResponse}, buildable via {@link SdkHttpFullResponse#builder()}. Returned by HTTP
 * implementation to represent a service response.
 */
@SdkInternalApi
@Immutable
class DefaultSdkHttpFullResponse implements SdkHttpFullResponse {
    private final String statusText;
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final AbortableInputStream content;

    private DefaultSdkHttpFullResponse(Builder builder) {
        Validate.isTrue(builder.statusCode > 0, "Status code must be positive.");
        this.statusCode = builder.statusCode;
        this.statusText = builder.statusText;
        this.headers = deepUnmodifiableMap(builder.headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.content = builder.content;
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public Optional<AbortableInputStream> content() {
        return Optional.ofNullable(content);
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
    public SdkHttpFullResponse.Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullResponse}.
     */
    static final class Builder implements SdkHttpFullResponse.Builder {
        private String statusText;
        private int statusCode;
        private AbortableInputStream content;
        private Map<String, List<String>> headers = new LinkedHashMap<>();

        Builder() {
        }

        private Builder(DefaultSdkHttpFullResponse defaultSdkHttpFullResponse) {
            this.statusText = defaultSdkHttpFullResponse.statusText;
            this.statusCode = defaultSdkHttpFullResponse.statusCode;
            this.content = defaultSdkHttpFullResponse.content;
            this.headers = CollectionUtils.deepCopyMap(defaultSdkHttpFullResponse.headers);
        }

        @Override
        public String statusText() {
            return statusText;
        }

        @Override
        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public AbortableInputStream content() {
            return content;
        }

        @Override
        public Builder content(AbortableInputStream content) {
            this.content = content;
            return this;
        }

        @Override
        public Builder putHeader(String headerName, List<String> headerValues) {
            Validate.paramNotNull(headerName, "headerName");
            Validate.paramNotNull(headerValues, "headerValues");
            this.headers.put(headerName, new ArrayList<>(headerValues));
            return this;
        }

        @Override
        public Builder headers(Map<String, List<String>> headers) {
            Validate.paramNotNull(headers, "headers");
            this.headers = CollectionUtils.deepCopyMap(headers);
            return this;
        }

        @Override
        public Builder removeHeader(String headerName) {
            this.headers.remove(headerName);
            return this;
        }

        @Override
        public Builder clearHeaders() {
            this.headers.clear();
            return this;
        }

        @Override
        public Map<String, List<String>> headers() {
            return deepUnmodifiableMap(this.headers);
        }

        /**
         * @return An immutable {@link DefaultSdkHttpFullResponse} object.
         */
        public SdkHttpFullResponse build() {
            return new DefaultSdkHttpFullResponse(this);
        }
    }
}
