/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Class containing a URL representing an object in Amazon S3.
 *
 * If the object identified by the given bucket and key has public read permissions,
 * then this URL can be directly accessed to retrieve the object's data.
 */
@SdkPublicApi
public final class GetUrlResponse implements ToCopyableBuilder<GetUrlResponse.Builder, GetUrlResponse> {

    private final URL url;

    private GetUrlResponse(BuilderImpl builder) {
        this.url = builder.url;
    }

    public URL getUrl() {
        return url;
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
        if (!(obj instanceof GetUrlResponse)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     */
    @Override
    public String toString() {
        return ToString.builder("GetUrlResponse").build();
    }

    public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz) {
        return Optional.empty();
    }

    public interface Builder extends CopyableBuilder<Builder, GetUrlResponse> {
        Builder url(URL url);
    }

    private static final class BuilderImpl implements Builder {
        private URL url;

        private BuilderImpl() {
        }

        private BuilderImpl(GetUrlResponse response) {
            url(response.url);
        }

        @Override
        public GetUrlResponse build() {
            return new GetUrlResponse(this);
        }

        @Override
        public Builder url(URL url) {
            this.url = url;
            return this;
        }

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }
    }
}
