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

package software.amazon.awssdk.services.s3.presignedurl.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object for performing download operations using a presigned URL.
 *
 * <p>If any fields from the {@code GetObjectRequest} that are marshalled as HTTP headers were signed when
 * generating the presigned URL using {@link software.amazon.awssdk.services.s3.presigner.S3Presigner#presignGetObject},
 * their values are not stored in the URL — only their names appear in {@code X-Amz-SignedHeaders}. If any
 * signed headers are missing from the download request, S3 will return a signature mismatch error.
 *
 * <p>Use {@link Builder#putHeader(String, String)} (or {@link Builder#headers(Map)}) to supply the signed
 * header values at download time, for example:</p>
 * <pre>{@code
 * PresignedUrlDownloadRequest.builder()
 *     .presignedUrl(url)
 *     .putHeader("Range", "bytes=0-1023")
 *     .putHeader("If-Match", eTag)
 *     .build();
 * }</pre>
 *
 */
@SdkPublicApi
public final class PresignedUrlDownloadRequest implements ToCopyableBuilder<PresignedUrlDownloadRequest.Builder,
    PresignedUrlDownloadRequest> {

    private final URL presignedUrl;
    private final Map<String, List<String>> headers;

    private PresignedUrlDownloadRequest(BuilderImpl builder) {
        this.presignedUrl = builder.presignedUrl;
        this.headers = CollectionUtils.deepUnmodifiableMap(builder.headers,
                                                           () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * <p>
     * The presigned URL for the S3 object. This URL contains all necessary authentication information and can be used
     * to download the object without additional credentials.
     * </p>
     * <b>Note:</b> Presigned URLs have a limited lifetime and will expire after the
     * specified duration. Ensure the URL is used before expiration.
     *
     * @return The presigned URL for the S3 object
     */
    public URL presignedUrl() {
        return presignedUrl;
    }

    /**
     * Returns the headers to be sent with the download request. These are the headers that were signed when
     * generating the presigned URL and must be included at download time for the signature to match (for example
     * {@code Range}, {@code If-Match}, {@code If-None-Match}, or the SSE-C headers).
     *
     * <p>The returned map uses case-insensitive header-name comparison.</p>
     *
     * @return An unmodifiable map of header name to values, or an empty map if none set.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(presignedUrl());
        hashCode = 31 * hashCode + Objects.hashCode(headers());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PresignedUrlDownloadRequest other = (PresignedUrlDownloadRequest) obj;
        return Objects.equals(presignedUrl(), other.presignedUrl()) &&
               Objects.equals(headers(), other.headers());
    }

    @Override
    public String toString() {
        return ToString.builder("PresignedUrlDownloadRequest")
                       .add("PresignedUrl", presignedUrl())
                       .add("Headers", headers())
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, PresignedUrlDownloadRequest> {
        /**
         * Sets the presigned URL for the S3 object.
         * @param presignedUrl the presigned URL
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder presignedUrl(URL presignedUrl);

        /**
         * Adds a single header to be sent with the download request. Use this to supply any header value that was
         * signed when generating the presigned URL (for example {@code Range}, {@code If-Match},
         * {@code If-None-Match}, or the SSE-C headers) and therefore must be present at download time.
         *
         * <p>Header names are treated case-insensitively. This overrides any value already configured for the same
         * header name.</p>
         *
         * @param name The header name (e.g., {@code "Range"}, {@code "If-Match"})
         * @param value The header value (e.g., {@code "bytes=0-1023"})
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        default Builder putHeader(String name, String value) {
            return putHeader(name, Collections.singletonList(value));
        }

        /**
         * Adds a single header with multiple values to be sent with the download request.
         *
         * <p>Header names are treated case-insensitively. This overrides any values already configured for the same
         * header name.</p>
         *
         * @param name The header name
         * @param values The header values
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder putHeader(String name, List<String> values);

        /**
         * Sets all headers to be sent with the download request, replacing any previously configured headers.
         *
         * @param headers A map of header name to values
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder headers(Map<String, List<String>> headers);
    }

    static final class BuilderImpl implements Builder {
        private URL presignedUrl;
        private Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private BuilderImpl() {
        }

        private BuilderImpl(PresignedUrlDownloadRequest request) {
            presignedUrl(request.presignedUrl());
            headers(request.headers());
        }

        @Override
        public Builder presignedUrl(URL presignedUrl) {
            this.presignedUrl = presignedUrl;
            return this;
        }

        @Override
        public Builder putHeader(String name, List<String> values) {
            Validate.paramNotNull(name, "name");
            Validate.paramNotNull(values, "values");
            this.headers.put(name, new ArrayList<>(values));
            return this;
        }

        @Override
        public Builder headers(Map<String, List<String>> headers) {
            Validate.paramNotNull(headers, "headers");
            Map<String, List<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            copy.putAll(CollectionUtils.deepCopyMap(headers));
            this.headers = copy;
            return this;
        }

        @Override
        public PresignedUrlDownloadRequest build() {
            Validate.paramNotNull(presignedUrl, "presignedUrl");
            return new PresignedUrlDownloadRequest(this);
        }
    }
}
