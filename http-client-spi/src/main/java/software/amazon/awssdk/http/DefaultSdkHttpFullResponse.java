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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static software.amazon.awssdk.utils.CollectionUtils.unmodifiableMapOfLists;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.internal.http.LowCopyListMap;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal implementation of {@link SdkHttpFullResponse}, buildable via {@link SdkHttpFullResponse#builder()}. Returned by HTTP
 * implementation to represent a service response.
 */
@SdkInternalApi
@Immutable
class DefaultSdkHttpFullResponse implements SdkHttpFullResponse {
    private static final long serialVersionUID = 1;
    private final String statusText;
    private final int statusCode;
    private final transient AbortableInputStream content;
    private transient LowCopyListMap.ForBuildable headers;

    private DefaultSdkHttpFullResponse(Builder builder) {
        this.statusCode = Validate.isNotNegative(builder.statusCode, "Status code must not be negative.");
        this.statusText = builder.statusText;
        this.content = builder.content;
        this.headers = builder.headers.forBuildable();
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers.forExternalRead();
    }

    @Override
    public List<String> matchingHeaders(String header) {
        return unmodifiableList(headers.forInternalRead().getOrDefault(header, emptyList()));
    }

    @Override
    public Optional<String> firstMatchingHeader(String headerName) {
        List<String> headers = this.headers.forInternalRead().get(headerName);
        if (headers == null || headers.isEmpty()) {
            return Optional.empty();
        }

        String header = headers.get(0);
        if (StringUtils.isEmpty(header)) {
            return Optional.empty();
        }

        return Optional.of(header);
    }

    @Override
    public Optional<String> firstMatchingHeader(Collection<String> headersToFind) {
        for (String headerName : headersToFind) {
            Optional<String> header = firstMatchingHeader(headerName);
            if (header.isPresent()) {
                return header;
            }
        }

        return Optional.empty();
    }

    @Override
    public void forEachHeader(BiConsumer<? super String, ? super List<String>> consumer) {
        this.headers.forInternalRead().forEach((k, v) -> consumer.accept(k, unmodifiableList(v)));
    }

    @Override
    public int numHeaders() {
        return this.headers.forInternalRead().size();
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

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        headers = LowCopyListMap.emptyHeaders().forBuildable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultSdkHttpFullResponse that = (DefaultSdkHttpFullResponse) o;
        return (statusCode == that.statusCode) &&
               Objects.equals(statusText, that.statusText) &&
               Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        int result = statusText != null ? statusText.hashCode() : 0;
        result = 31 * result + statusCode;
        result = 31 * result + Objects.hashCode(headers);
        return result;
    }

    /**
     * Builder for a {@link DefaultSdkHttpFullResponse}.
     */
    static final class Builder implements SdkHttpFullResponse.Builder {
        private String statusText;
        private int statusCode;
        private AbortableInputStream content;
        private LowCopyListMap.ForBuilder headers;

        Builder() {
            headers = LowCopyListMap.emptyHeaders();
        }

        private Builder(DefaultSdkHttpFullResponse defaultSdkHttpFullResponse) {
            statusText = defaultSdkHttpFullResponse.statusText;
            statusCode = defaultSdkHttpFullResponse.statusCode;
            content = defaultSdkHttpFullResponse.content;
            headers = defaultSdkHttpFullResponse.headers.forBuilder();
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
            this.headers.forInternalWrite().put(headerName, new ArrayList<>(headerValues));
            return this;
        }

        @Override
        public SdkHttpFullResponse.Builder appendHeader(String headerName, String headerValue) {
            Validate.paramNotNull(headerName, "headerName");
            Validate.paramNotNull(headerValue, "headerValue");
            this.headers.forInternalWrite().computeIfAbsent(headerName, k -> new ArrayList<>()).add(headerValue);
            return this;
        }

        @Override
        public Builder headers(Map<String, List<String>> headers) {
            Validate.paramNotNull(headers, "headers");
            this.headers.setFromExternal(headers);
            return this;
        }

        @Override
        public Builder removeHeader(String headerName) {
            this.headers.forInternalWrite().remove(headerName);
            return this;
        }

        @Override
        public Builder clearHeaders() {
            this.headers.clear();
            return this;
        }

        @Override
        public Map<String, List<String>> headers() {
            return unmodifiableMapOfLists(this.headers.forInternalRead());
        }

        @Override
        public List<String> matchingHeaders(String header) {
            return unmodifiableList(headers.forInternalRead().getOrDefault(header, emptyList()));
        }

        @Override
        public Optional<String> firstMatchingHeader(String headerName) {
            List<String> headers = this.headers.forInternalRead().get(headerName);
            if (headers == null || headers.isEmpty()) {
                return Optional.empty();
            }

            String header = headers.get(0);
            if (StringUtils.isEmpty(header)) {
                return Optional.empty();
            }

            return Optional.of(header);
        }

        @Override
        public Optional<String> firstMatchingHeader(Collection<String> headersToFind) {
            for (String headerName : headersToFind) {
                Optional<String> header = firstMatchingHeader(headerName);
                if (header.isPresent()) {
                    return header;
                }
            }

            return Optional.empty();
        }

        @Override
        public void forEachHeader(BiConsumer<? super String, ? super List<String>> consumer) {
            headers.forInternalRead().forEach((k, v) -> consumer.accept(k, unmodifiableList(v)));
        }

        @Override
        public int numHeaders() {
            return headers.forInternalRead().size();
        }

        /**
         * @return An immutable {@link DefaultSdkHttpFullResponse} object.
         */
        @Override
        public SdkHttpFullResponse build() {
            return new DefaultSdkHttpFullResponse(this);
        }
    }
}
