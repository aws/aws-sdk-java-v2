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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Represents an HTTP response returned by an AWS service in response to a
 * service request.
 */
@SdkInternalApi
@Immutable
class DefaultSdkHttpFullResponse implements SdkHttpFullResponse {
    private final String statusText;
    private final int statusCode;
    private final AbortableInputStream content;
    private final Map<String, List<String>> headers;

    DefaultSdkHttpFullResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.statusText = builder.statusText;
        this.content = builder.content;
        this.headers = CollectionUtils.deepUnmodifiableMap(builder.headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Collection<String> getValuesForHeader(String header) {
        Collection<String> values = headers.get(header);
        return values != null ? values : Collections.emptyList();
    }

    @Override
    public AbortableInputStream getContent() {
        return content;
    }

    @Override
    public String getStatusText() {
        return statusText;
    }

    @Override
    public int getStatusCode() {
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
        private Map<String, List<String>> headers = new HashMap<>();

        Builder() {
        }

        private Builder(DefaultSdkHttpFullResponse defaultSdkHttpFullResponse) {
            this.statusText = defaultSdkHttpFullResponse.statusText;
            this.statusCode = defaultSdkHttpFullResponse.statusCode;
            this.content = defaultSdkHttpFullResponse.content;
            this.headers = CollectionUtils.deepCopyMap(defaultSdkHttpFullResponse.headers);
        }

        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder content(AbortableInputStream content) {
            this.content = content;
            return this;
        }

        @ReviewBeforeRelease("Should we only allow setting the AbortableInputStream?")
        public Builder content(InputStream content) {
            return content(new AbortableInputStream(content, () -> {
            }));
        }

        public Builder headers(Map<String, List<String>> headers) {
            this.headers = CollectionUtils.deepCopyMap(headers);
            return this;
        }

        public Builder addHeader(String headerName, List<String> headerValues) {
            this.headers.put(headerName, headerValues);
            return this;
        }

        /**
         * @return An immutable {@link DefaultSdkHttpFullResponse} object.
         */
        public SdkHttpFullResponse build() {
            return new DefaultSdkHttpFullResponse(this);
        }
    }
}
