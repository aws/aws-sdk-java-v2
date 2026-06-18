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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object for performing download operations using a presigned URL.
 *
 * <p>There are two ways to construct this request:</p>
 * <ul>
 *   <li>{@link Builder#presignedGetObjectRequest(PresignedGetObjectRequest)} — Recommended when the URL was generated
 *       using the SDK presigner, especially if headers like Range or If-Match were set at presign time. The SDK
 *       automatically extracts the URL and signed header values.</li>
 *   <li>{@link Builder#presignedUrl(URL)} — For URLs where no additional headers (such as Range or If-Match)
 *       were signed at presign time, i.e., only {@code host} is in {@code X-Amz-SignedHeaders}.</li>
 * </ul>
 */
@SdkPublicApi
public final class PresignedUrlDownloadRequest implements ToCopyableBuilder<PresignedUrlDownloadRequest.Builder,
    PresignedUrlDownloadRequest> {

    private final PresignedGetObjectRequest presignedGetObjectRequest;
    private final URL presignedUrl;
    private final String range;
    private final String ifMatch;
    private final String ifNoneMatch;
    private final String ifModifiedSince;
    private final String ifUnmodifiedSince;

    private PresignedUrlDownloadRequest(BuilderImpl builder) {
        this.presignedGetObjectRequest = builder.presignedGetObjectRequest;
        this.presignedUrl = builder.presignedUrl;
        this.range = builder.range;
        this.ifMatch = builder.ifMatch;
        this.ifNoneMatch = builder.ifNoneMatch;
        this.ifModifiedSince = builder.ifModifiedSince;
        this.ifUnmodifiedSince = builder.ifUnmodifiedSince;
    }

    /**
     * <p>
     * The presigned URL for the S3 object. This URL contains all necessary authentication information and can be used to download
     * the object without additional credentials.
     * </p>
     * <b>Note:</b> Presigned URLs have a limited lifetime and will expire after the
     * specified duration. Ensure the URL is used before expiration.
     *
     * @return The presigned URL for the S3 object
     */
    public URL presignedUrl() {
        return presignedGetObjectRequest != null ? presignedGetObjectRequest.url() : presignedUrl;
    }

    /**
     * The presigned request object from the SDK presigner, or null if a raw URL was provided.
     */
    public PresignedGetObjectRequest presignedGetObjectRequest() {
        return presignedGetObjectRequest;
    }

    /**
     * <p>
     * Specifies the byte range of an object. For more information about the HTTP Range header, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-range">
     * https://www.rfc-editor.org/rfc/rfc9110.html#name-range</a>.
     * </p>
     * <b>Note:</b>  Amazon S3 doesn't support retrieving multiple ranges of data per <code>GET</code> request.
     *
     * @return The HTTP Range header value, or null if not specified.
     */
    public String range() {
        return range;
    }

    /**
     * <p>
     * Return the object only if its entity tag (ETag) is the same as the one specified in this header,
     * otherwise return a 412 (precondition failed) error.
     * </p>
     *
     * @return The If-Match header value, or null if not specified.
     */
    public String ifMatch() {
        return ifMatch;
    }

    /**
     * <p>
     * Return the object only if its entity tag (ETag) is different from the one specified in this header,
     * otherwise return a 304 (not modified) response.
     * </p>
     *
     * @return The If-None-Match header value, or null if not specified.
     */
    public String ifNoneMatch() {
        return ifNoneMatch;
    }

    /**
     * <p>
     * Return the object only if it has been modified since the specified time,
     * otherwise return a 304 (not modified) response.
     * </p>
     *
     * @return The If-Modified-Since header value in RFC 7231 format, or null if not specified.
     */
    public String ifModifiedSince() {
        return ifModifiedSince;
    }

    /**
     * <p>
     * Return the object only if it has not been modified since the specified time,
     * otherwise return a 412 (precondition failed) error.
     * </p>
     *
     * @return The If-Unmodified-Since header value in RFC 7231 format, or null if not specified.
     */
    public String ifUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    /**
     * The signed headers map from the presigned request, or null if a raw URL was provided.
     * Used internally by the SDK to send correct header values at download time.
     */
    public Map<String, List<String>> signedHeaders() {
        return presignedGetObjectRequest != null ? presignedGetObjectRequest.signedHeaders() : null;
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
        hashCode = 31 * hashCode + Objects.hashCode(presignedGetObjectRequest);
        hashCode = 31 * hashCode + Objects.hashCode(presignedUrl);
        hashCode = 31 * hashCode + Objects.hashCode(range());
        hashCode = 31 * hashCode + Objects.hashCode(ifMatch());
        hashCode = 31 * hashCode + Objects.hashCode(ifNoneMatch());
        hashCode = 31 * hashCode + Objects.hashCode(ifModifiedSince());
        hashCode = 31 * hashCode + Objects.hashCode(ifUnmodifiedSince());
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
        return Objects.equals(presignedGetObjectRequest, other.presignedGetObjectRequest)
               && Objects.equals(presignedUrl, other.presignedUrl)
               && Objects.equals(range(), other.range())
               && Objects.equals(ifMatch(), other.ifMatch())
               && Objects.equals(ifNoneMatch(), other.ifNoneMatch())
               && Objects.equals(ifModifiedSince(), other.ifModifiedSince())
               && Objects.equals(ifUnmodifiedSince(), other.ifUnmodifiedSince());
    }

    @Override
    public String toString() {
        return ToString.builder("PresignedUrlDownloadRequest")
                       .add("PresignedUrl", presignedUrl())
                       .add("Range", range())
                       .add("IfMatch", ifMatch())
                       .add("IfNoneMatch", ifNoneMatch())
                       .add("IfModifiedSince", ifModifiedSince())
                       .add("IfUnmodifiedSince", ifUnmodifiedSince())
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, PresignedUrlDownloadRequest> {
        /**
         * Sets the presigned URL for the S3 object. Use this for raw URLs from external sources.
         *
         * <p>Mutually exclusive with {@link #presignedGetObjectRequest(PresignedGetObjectRequest)}.
         * If the URL was generated using the SDK presigner with headers like Range or If-Match,
         * use {@link #presignedGetObjectRequest(PresignedGetObjectRequest)} instead.</p>
         */
        Builder presignedUrl(URL presignedUrl);

        /**
         * Sets the presigned request object from the SDK presigner. The SDK extracts the URL and signed
         * header values automatically.
         *
         * <p>Recommended when the URL was generated using the SDK presigner, especially if headers like
         * Range or If-Match were set at presign time.</p>
         *
         * <p>Mutually exclusive with {@link #presignedUrl(URL)}.</p>
         */
        Builder presignedGetObjectRequest(PresignedGetObjectRequest presignedGetObjectRequest);

        /**
         * Specifies the byte range of an object (e.g., "bytes=0-1023").
         */
        Builder range(String range);

        /**
         * Return the object only if its ETag matches this value.
         */
        Builder ifMatch(String ifMatch);

        /**
         * Return the object only if its ETag does NOT match this value.
         */
        Builder ifNoneMatch(String ifNoneMatch);

        /**
         * Return the object only if it has been modified since this date.
         * Value should be in RFC 7231 format (e.g., "Wed, 17 Jun 2026 17:46:28 GMT").
         */
        Builder ifModifiedSince(String ifModifiedSince);

        /**
         * Return the object only if it has NOT been modified since this date.
         * Value should be in RFC 7231 format (e.g., "Wed, 17 Jun 2026 17:46:28 GMT").
         */
        Builder ifUnmodifiedSince(String ifUnmodifiedSince);
    }

    static final class BuilderImpl implements Builder {
        private PresignedGetObjectRequest presignedGetObjectRequest;
        private URL presignedUrl;
        private String range;
        private String ifMatch;
        private String ifNoneMatch;
        private String ifModifiedSince;
        private String ifUnmodifiedSince;

        private BuilderImpl() {
        }

        private BuilderImpl(PresignedUrlDownloadRequest request) {
            this.presignedGetObjectRequest = request.presignedGetObjectRequest;
            this.presignedUrl = request.presignedUrl;
            this.range = request.range();
            this.ifMatch = request.ifMatch();
            this.ifNoneMatch = request.ifNoneMatch();
            this.ifModifiedSince = request.ifModifiedSince();
            this.ifUnmodifiedSince = request.ifUnmodifiedSince();
        }

        @Override
        public Builder presignedUrl(URL presignedUrl) {
            this.presignedUrl = presignedUrl;
            return this;
        }

        @Override
        public Builder presignedGetObjectRequest(PresignedGetObjectRequest presignedGetObjectRequest) {
            Validate.paramNotNull(presignedGetObjectRequest, "presignedGetObjectRequest");
            this.presignedGetObjectRequest = presignedGetObjectRequest;
            return this;
        }

        @Override
        public Builder range(String range) {
            this.range = range;
            return this;
        }

        @Override
        public Builder ifMatch(String ifMatch) {
            this.ifMatch = ifMatch;
            return this;
        }

        @Override
        public Builder ifNoneMatch(String ifNoneMatch) {
            this.ifNoneMatch = ifNoneMatch;
            return this;
        }

        @Override
        public Builder ifModifiedSince(String ifModifiedSince) {
            this.ifModifiedSince = ifModifiedSince;
            return this;
        }

        @Override
        public Builder ifUnmodifiedSince(String ifUnmodifiedSince) {
            this.ifUnmodifiedSince = ifUnmodifiedSince;
            return this;
        }

        @Override
        public PresignedUrlDownloadRequest build() {
            validateMutualExclusion();
            Validate.paramNotNull(resolveUrl(), "presignedUrl or presignedGetObjectRequest");
            validateSignedHeadersAvailable();
            validateNoConflicts();
            return new PresignedUrlDownloadRequest(this);
        }

        private URL resolveUrl() {
            return presignedGetObjectRequest != null ? presignedGetObjectRequest.url() : presignedUrl;
        }

        private Map<String, List<String>> resolveSignedHeaders() {
            return presignedGetObjectRequest != null ? presignedGetObjectRequest.signedHeaders() : null;
        }

        private void validateMutualExclusion() {
            if (presignedUrl != null && presignedGetObjectRequest != null) {
                throw new IllegalArgumentException(
                    "Cannot set both presignedUrl() and presignedGetObjectRequest(). Use one or the other.");
            }
        }

        private void validateSignedHeadersAvailable() {
            if (presignedGetObjectRequest != null || presignedUrl == null) {
                return;
            }
            String query = presignedUrl.getQuery();
            if (query == null) {
                return;
            }
            for (String param : query.split("&")) {
                if (param.startsWith("X-Amz-SignedHeaders=")) {
                    String signed = param.substring("X-Amz-SignedHeaders=".length());
                    StringBuilder conflicting = new StringBuilder();
                    for (String header : new String[]{"range", "if-match", "if-none-match",
                                                      "if-modified-since", "if-unmodified-since"}) {
                        if (signed.contains(header)) {
                            if (conflicting.length() > 0) {
                                conflicting.append(", ");
                            }
                            conflicting.append(header);
                        }
                    }
                    if (conflicting.length() > 0) {
                        throw new IllegalArgumentException(
                            "The presigned URL has [" + conflicting + "] in its SignedHeaders, but the "
                            + "values cannot be recovered from the URL alone. Use "
                            + "presignedGetObjectRequest() instead of presignedUrl() to ensure "
                            + "correct header values are sent at download time.");
                    }
                    break;
                }
            }
        }

        private void validateNoConflicts() {
            Map<String, List<String>> signedHeaders = resolveSignedHeaders();
            if (signedHeaders == null) {
                return;
            }
            validateHeaderNotConflicting(signedHeaders, "range", range);
            validateHeaderNotConflicting(signedHeaders, "if-match", ifMatch);
            validateHeaderNotConflicting(signedHeaders, "if-none-match", ifNoneMatch);
            validateHeaderNotConflicting(signedHeaders, "if-modified-since", ifModifiedSince);
            validateHeaderNotConflicting(signedHeaders, "if-unmodified-since", ifUnmodifiedSince);
        }

        private static void validateHeaderNotConflicting(Map<String, List<String>> signedHeaders,
                                                         String headerName, String explicitValue) {
            if (explicitValue == null) {
                return;
            }
            List<String> signedValues = signedHeaders.get(headerName);
            if (signedValues != null && !signedValues.isEmpty()
                && !explicitValue.equals(signedValues.get(0))) {
                throw new IllegalArgumentException(
                    headerName + " value '" + explicitValue + "' conflicts with signed value '"
                    + signedValues.get(0) + "'. Do not set a different value when signedHeaders contains it.");
            }
        }
    }
}
