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
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object for performing GetObject operations using a presigned URL.
 */
@SdkPublicApi
public final class PresignedUrlGetObjectRequest implements ToCopyableBuilder<PresignedUrlGetObjectRequest.Builder,
    PresignedUrlGetObjectRequest> {
    private final URL presignedUrl;
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
     * <b>Note:</b> Presigned URLs have a limited lifetime and will expire after the
     * specified duration. Ensure the URL is used before expiration.
     *
     * @return The presigned URL for the S3 object
     */
    public URL presignedUrl() {
        return presignedUrl;
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
        hashCode = 31 * hashCode + Objects.hashCode(range());
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
        PresignedUrlGetObjectRequest other = (PresignedUrlGetObjectRequest) obj;
        return Objects.equals(presignedUrl(), other.presignedUrl()) &&
               Objects.equals(range(), other.range());
    }

    @Override
    public String toString() {
        return ToString.builder("PresignedUrlGetObjectRequest")
                       .add("PresignedUrl", presignedUrl())
                       .add("Range", range())
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, PresignedUrlGetObjectRequest> {
        Builder presignedUrl(URL presignedUrl);

        Builder range(String range);
    }

    static final class BuilderImpl implements Builder {
        private URL presignedUrl;
        private String range;

        private BuilderImpl() {
        }

        private BuilderImpl(PresignedUrlGetObjectRequest presignedUrlGetObjectRequest) {
            presignedUrl(presignedUrlGetObjectRequest.presignedUrl());
            range(presignedUrlGetObjectRequest.range());
        }

        @Override
        public Builder presignedUrl(URL presignedUrl) {
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
            if (presignedUrl == null) {
                throw new IllegalArgumentException("presignedUrl is required");
            }
            return new PresignedUrlGetObjectRequest(this);
        }
    }
}
