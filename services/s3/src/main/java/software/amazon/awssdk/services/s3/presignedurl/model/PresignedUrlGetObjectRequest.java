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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object for performing GetObject operations using a presigned URL.
 * <p>
 * Request model used for downloading S3 objects using a pre-signed URL, without requiring AWS credentials at request time. The
 * pre-signed URL contains all necessary authentication information and enables direct object retrieval from S3.
 * </p>
 */
@SdkPublicApi
public final class PresignedUrlGetObjectRequest implements ToCopyableBuilder<PresignedUrlGetObjectRequest.Builder,
    PresignedUrlGetObjectRequest> {
    private final String presignedUrl;
    private final String range;

    private PresignedUrlGetObjectRequest(BuilderImpl builder) {
        this.presignedUrl = builder.presignedUrl;
        this.range = builder.range;
    }

    /**
     * <p>
     * The presigned URL for the S3 object. This URL contains all necessary authentication information and can be used to download
     * the object without additional credentials.
     * </p>
     * <p>
     * The presigned URL is generated using AWS credentials and includes the following components:
     * </p>
     * <ul>
     * <li><code>X-Amz-Algorithm</code> - The signing algorithm used (e.g., AWS4-HMAC-SHA256)</li>
     * <li><code>X-Amz-Credential</code> - The credential scope for the request</li>
     * <li><code>X-Amz-Date</code> - The date and time the URL was signed</li>
     * <li><code>X-Amz-Expires</code> - The duration for which the URL is valid</li>
     * <li><code>X-Amz-SignedHeaders</code> - The headers that were signed</li>
     * <li><code>X-Amz-Signature</code> - The calculated signature for authentication</li>
     * <li><code>X-Amz-Security-Token</code> - The session token (if using temporary credentials)</li>
     * </ul>
     * <p>
     * <b>Note:</b> Presigned URLs have a limited lifetime and will expire after the
     * specified duration. Ensure the URL is used before expiration.
     * </p>
     *
     * @return The presigned URL for the S3 object
     */
    public String presignedUrl() {
        return presignedUrl;
    }

    /**
     * <p>
     * Specifies the byte range of an object. For more information about the HTTP Range header, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-range">
     * https://www.rfc-editor.org/rfc/rfc9110.html#name-range</a>.
     * </p>
     * <note>
     * <p>
     * Note: Amazon S3 doesn't support retrieving multiple ranges of data per <code>GET</code> request.
     * </p>
     * </note>
     *
     * @return The HTTP Range header value, or null if not specified.
     */
    public String range() {
        return range;
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
    public final int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + Objects.hashCode(presignedUrl());
        hashCode = 31 * hashCode + Objects.hashCode(range());
        return hashCode;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PresignedUrlGetObjectRequest other = (PresignedUrlGetObjectRequest) obj;
        return Objects.equals(presignedUrl(), other.presignedUrl()) &&
               Objects.equals(range(), other.range());
    }

    @Override
    public final String toString() {
        return ToString.builder("PresignedUrlGetObjectRequest")
                       .add("PresignedUrl", presignedUrl())
                       .add("Range", range())
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, PresignedUrlGetObjectRequest> {
        Builder presignedUrl(String presignedUrl);

        Builder range(String range);
    }

    static final class BuilderImpl implements Builder {
        private String presignedUrl;
        private String range;

        private BuilderImpl() {
        }

        private BuilderImpl(PresignedUrlGetObjectRequest model) {
            presignedUrl(model.presignedUrl);
            range(model.range);
        }

        @Override
        public Builder presignedUrl(String presignedUrl) {
            this.presignedUrl = presignedUrl;
            return this;
        }

        @Override
        public Builder range(String range) {
            this.range = range;
            return this;
        }

        @Override
        public PresignedUrlGetObjectRequest build() {
            return new PresignedUrlGetObjectRequest(this);
        }
    }
}