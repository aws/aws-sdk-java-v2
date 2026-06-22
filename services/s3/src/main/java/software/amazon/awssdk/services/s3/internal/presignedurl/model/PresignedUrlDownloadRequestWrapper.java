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

package software.amazon.awssdk.services.s3.internal.presignedurl.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.services.s3.model.S3Request;

/**
 * Internal request object for presigned URL GetObject operations.
 * <p>
 * This class is used internally by the AWS SDK to process presigned URL requests for S3 GetObject operations. It carries the
 * presigned URL and the headers that must be replayed at download time (the values that were signed when the URL was
 * generated). It is not intended for direct use by SDK users.
 * </p>
 * <b>Note:</b> This is an internal implementation class and should not be used
 * directly. Use {@code PresignedUrlDownloadRequest} for public API interactions.
 */
@SdkInternalApi
public final class PresignedUrlDownloadRequestWrapper extends S3Request {

    private static final List<SdkField<?>> SDK_FIELDS = Collections.emptyList();

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = Collections.emptyMap();

    private final URL url;
    private final Map<String, List<String>> headers;

    private PresignedUrlDownloadRequestWrapper(Builder builder) {
        super(builder);
        this.url = builder.url;
        this.headers = Collections.unmodifiableMap(builder.headers);
    }

    public URL url() {
        return url;
    }

    /**
     * Returns the headers to be sent with the download request, using case-insensitive header-name comparison.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        PresignedUrlDownloadRequestWrapper that = (PresignedUrlDownloadRequestWrapper) obj;
        return Objects.equals(url, that.url) && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(super.hashCode());
        result = 31 * result + Objects.hashCode(url);
        result = 31 * result + Objects.hashCode(headers);
        return result;
    }

    public static final class Builder extends S3Request.BuilderImpl {
        private URL url;
        private Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        public Builder() {
        }

        Builder(PresignedUrlDownloadRequestWrapper request) {
            super(request);
            this.url = request.url();
            if (request.headers() != null) {
                headers(request.headers());
            }
        }

        public Builder url(URL url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, List<String>> headers) {
            Map<String, List<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
            this.headers = copy;
            return this;
        }

        @Override
        public PresignedUrlDownloadRequestWrapper build() {
            return new PresignedUrlDownloadRequestWrapper(this);
        }
    }
}
