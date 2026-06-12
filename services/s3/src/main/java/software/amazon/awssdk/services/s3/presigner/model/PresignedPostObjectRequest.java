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

package software.amazon.awssdk.services.s3.presigner.model;

import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * The result of generating a pre-signed POST upload request.
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class PresignedPostObjectRequest
        implements ToCopyableBuilder<PresignedPostObjectRequest.Builder, PresignedPostObjectRequest> {
    private final URL url;
    private final Map<String, String> signedFormFields;
    private final String bucket;
    private final String key;
    private final Instant expiration;

    private PresignedPostObjectRequest(Builder builder) {
        this.url = Validate.paramNotNull(builder.url, "url");
        this.signedFormFields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.signedFormFields));
        this.bucket = Validate.paramNotBlank(builder.bucket, "bucket");
        this.key = Validate.paramNotBlank(builder.key, "key");
        this.expiration = Validate.paramNotNull(builder.expiration, "expiration");
    }

    public static Builder builder() {
        return new Builder();
    }

    public URL url() {
        return url;
    }

    public Map<String, String> signedFormFields() {
        return signedFormFields;
    }

    public String bucket() {
        return bucket;
    }

    public String key() {
        return key;
    }

    public Instant expiration() {
        return expiration;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PresignedPostObjectRequest that = (PresignedPostObjectRequest) o;

        if (!url.equals(that.url)) {
            return false;
        }
        if (!signedFormFields.equals(that.signedFormFields)) {
            return false;
        }
        if (!bucket.equals(that.bucket)) {
            return false;
        }
        if (!key.equals(that.key)) {
            return false;
        }
        return expiration.equals(that.expiration);
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + signedFormFields.hashCode();
        result = 31 * result + bucket.hashCode();
        result = 31 * result + key.hashCode();
        result = 31 * result + expiration.hashCode();
        return result;
    }

    @SdkPublicApi
    @NotThreadSafe
    public static final class Builder implements CopyableBuilder<Builder, PresignedPostObjectRequest> {
        private URL url;
        private Map<String, String> signedFormFields = new LinkedHashMap<>();
        private String bucket;
        private String key;
        private Instant expiration;

        private Builder() {
        }

        private Builder(PresignedPostObjectRequest request) {
            this.url = request.url;
            this.signedFormFields.putAll(request.signedFormFields);
            this.bucket = request.bucket;
            this.key = request.key;
            this.expiration = request.expiration;
        }

        public Builder url(URL url) {
            this.url = url;
            return this;
        }

        public Builder signedFormFields(Map<String, String> signedFormFields) {
            this.signedFormFields.clear();
            if (signedFormFields != null) {
                this.signedFormFields.putAll(signedFormFields);
            }
            return this;
        }

        public Builder putSignedFormField(String name, String value) {
            signedFormFields.put(name, value);
            return this;
        }

        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder expiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public PresignedPostObjectRequest build() {
            return new PresignedPostObjectRequest(this);
        }
    }
}
