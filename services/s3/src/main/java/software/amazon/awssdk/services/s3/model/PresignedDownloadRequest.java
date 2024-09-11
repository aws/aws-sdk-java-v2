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

package software.amazon.awssdk.services.s3.model;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request to download an object from S3 using a pre-signed URL.
 */
@SdkPublicApi
public final class PresignedDownloadRequest
    extends S3Request implements ToCopyableBuilder<PresignedDownloadRequest.Builder, PresignedDownloadRequest> {

    private final URL presignedUrl;
    private final Long startByte;
    private final Long endByte;
    private final Map<String, List<String>> customHeaders;

    private PresignedDownloadRequest(BuilderImpl builder) {
        super(builder);
        this.presignedUrl = builder.presignedUrl;
        this.startByte = builder.startByte;
        this.endByte = builder.endByte;
        this.customHeaders = builder.customHeaders != null ? builder.customHeaders  : Collections.emptyMap();
    }

    /**
     * Returns the pre-signed URL to download the object from S3.
     */
    public URL presignedUrl() {
        return presignedUrl;
    }

    /**
     * Returns the start byte of the object to download.
     */
    public Long startByte() {
        return startByte;
    }

    /**
     * Returns the end byte of the object to download.
     */
    public Long endByte() {
        return endByte;
    }

    /**
     * Returns the custom headers to include in the request.
     */
    public Map<String, List<String>> customHeaders() {
        return Collections.unmodifiableMap(customHeaders);
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
        hashCode = 31 * hashCode + Objects.hashCode(startByte());
        hashCode = 31 * hashCode + Objects.hashCode(endByte());
        hashCode = 31 * hashCode + Objects.hashCode(customHeaders());
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PresignedDownloadRequest)) {
            return false;
        }
        PresignedDownloadRequest other = (PresignedDownloadRequest) obj;
        return Objects.equals(presignedUrl(), other.presignedUrl()) &&
               Objects.equals(startByte(), other.startByte()) &&
               Objects.equals(endByte(), other.endByte()) &&
               Objects.equals(customHeaders(), other.customHeaders());
    }

    @Override
    public String toString() {
        return ToString.builder("PresignedDownloadRequest")
            .add("PresignedUrl", presignedUrl())
            .add("StartByte", startByte())
            .add("EndByte", endByte())
            .add("CustomHeaders", customHeaders())
            .build();
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        // Not used, custom marshaller for request
        return Collections.emptyList();
    }

    public interface Builder extends S3Request.Builder, CopyableBuilder<Builder, PresignedDownloadRequest> {

        /**
         * The pre-signed URL to download the object from S3.
         */
        Builder presignedUrl(URL presignedUrl);

        /**
         * The start byte of the object to download. Defaults to 0.
         */
        Builder startByte(Long startByte);

        /**
         * The end byte of the object to download.
         */
        Builder endByte(Long endByte);

        /**
         * Custom headers to include in the request.
         */
        Builder customHeaders(Map<String, List<String>> customHeaders);
    }

    static final class BuilderImpl extends S3Request.BuilderImpl implements Builder {
        private URL presignedUrl;
        private Long startByte;
        private Long endByte;
        private Map<String, List<String>> customHeaders;

        private BuilderImpl() {
        }

        private BuilderImpl(PresignedDownloadRequest model) {
            super(model);
            this.presignedUrl = model.presignedUrl;
            this.startByte = model.startByte;
            this.endByte = model.endByte;
            this.customHeaders = model.customHeaders;
        }

        @Override
        public Builder presignedUrl(URL presignedUrl) {
            this.presignedUrl = presignedUrl;
            return this;
        }

        @Override
        public Builder startByte(Long startByte) {
            this.startByte = startByte;
            return this;
        }

        @Override
        public Builder endByte(Long endByte) {
            this.endByte = endByte;
            return this;
        }

        @Override
        public Builder customHeaders(Map<String, List<String>> customHeaders) {
            this.customHeaders = customHeaders;
            return this;
        }

        @Override
        public PresignedDownloadRequest build() {
            return new PresignedDownloadRequest(this);
        }
    }
}
