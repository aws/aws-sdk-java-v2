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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.services.s3.model.S3Request;

/**
 * Internal request object for presigned URL GetObject operations.
 * <p>
 * This class is used internally by the AWS SDK to process presigned URL requests for S3 GetObject operations. It contains minimal
 * SdkField definitions needed for custom marshalling and is not intended for direct use by SDK users.
 * </p>
 * <b>Note:</b> This is an internal implementation class and should not be used
 * directly. Use {@code PresignedUrlDownloadRequest} for public API interactions.
 */
@SdkInternalApi
public final class PresignedUrlDownloadRequestWrapper extends S3Request {
    private static final SdkField<String> RANGE_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("Range")
        .getter(getter(PresignedUrlDownloadRequestWrapper::range))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("Range")
                             .unmarshallLocationName("Range").build()).build();

    private static final SdkField<String> IF_MATCH_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("IfMatch")
        .getter(getter(PresignedUrlDownloadRequestWrapper::ifMatch))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("If-Match")
                             .unmarshallLocationName("If-Match").build()).build();

    private static final SdkField<String> IF_NONE_MATCH_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("IfNoneMatch")
        .getter(getter(PresignedUrlDownloadRequestWrapper::ifNoneMatch))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("If-None-Match")
                             .unmarshallLocationName("If-None-Match").build()).build();

    private static final SdkField<String> IF_MODIFIED_SINCE_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("IfModifiedSince")
        .getter(getter(PresignedUrlDownloadRequestWrapper::ifModifiedSince))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("If-Modified-Since")
                             .unmarshallLocationName("If-Modified-Since").build()).build();

    private static final SdkField<String> IF_UNMODIFIED_SINCE_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("IfUnmodifiedSince")
        .getter(getter(PresignedUrlDownloadRequestWrapper::ifUnmodifiedSince))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("If-Unmodified-Since")
                             .unmarshallLocationName("If-Unmodified-Since").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(
        Arrays.asList(RANGE_FIELD, IF_MATCH_FIELD, IF_NONE_MATCH_FIELD, IF_MODIFIED_SINCE_FIELD, IF_UNMODIFIED_SINCE_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final URL url;
    private final String range;
    private final String ifMatch;
    private final String ifNoneMatch;
    private final String ifModifiedSince;
    private final String ifUnmodifiedSince;
    private final Map<String, List<String>> signedHeaders;

    private PresignedUrlDownloadRequestWrapper(Builder builder) {
        super(builder);
        this.url = builder.url;
        this.range = builder.range;
        this.ifMatch = builder.ifMatch;
        this.ifNoneMatch = builder.ifNoneMatch;
        this.ifModifiedSince = builder.ifModifiedSince;
        this.ifUnmodifiedSince = builder.ifUnmodifiedSince;
        this.signedHeaders = builder.signedHeaders;
    }

    public URL url() {
        return url;
    }

    public String range() {
        return range;
    }

    public String ifMatch() {
        return ifMatch;
    }

    public String ifNoneMatch() {
        return ifNoneMatch;
    }

    public String ifModifiedSince() {
        return ifModifiedSince;
    }

    public String ifUnmodifiedSince() {
        return ifUnmodifiedSince;
    }

    public Map<String, List<String>> signedHeaders() {
        return signedHeaders;
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

    @Override
    public Map<String, SdkField<?>> sdkFieldNameToField() {
        return SDK_NAME_TO_FIELD;
    }

    private static <T> Function<Object, T> getter(Function<PresignedUrlDownloadRequestWrapper, T> g) {
        return obj -> g.apply((PresignedUrlDownloadRequestWrapper) obj);
    }

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("Range", RANGE_FIELD);
        map.put("IfMatch", IF_MATCH_FIELD);
        map.put("IfNoneMatch", IF_NONE_MATCH_FIELD);
        map.put("IfModifiedSince", IF_MODIFIED_SINCE_FIELD);
        map.put("IfUnmodifiedSince", IF_UNMODIFIED_SINCE_FIELD);
        return Collections.unmodifiableMap(map);
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
        return Objects.equals(url, that.url)
               && Objects.equals(range, that.range)
               && Objects.equals(ifMatch, that.ifMatch)
               && Objects.equals(ifNoneMatch, that.ifNoneMatch)
               && Objects.equals(ifModifiedSince, that.ifModifiedSince)
               && Objects.equals(ifUnmodifiedSince, that.ifUnmodifiedSince)
               && Objects.equals(signedHeaders, that.signedHeaders);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(super.hashCode());
        result = 31 * result + Objects.hashCode(url);
        result = 31 * result + Objects.hashCode(range);
        result = 31 * result + Objects.hashCode(ifMatch);
        result = 31 * result + Objects.hashCode(ifNoneMatch);
        result = 31 * result + Objects.hashCode(ifModifiedSince);
        result = 31 * result + Objects.hashCode(ifUnmodifiedSince);
        result = 31 * result + Objects.hashCode(signedHeaders);
        return result;
    }

    public static final class Builder extends S3Request.BuilderImpl {
        private URL url;
        private String range;
        private String ifMatch;
        private String ifNoneMatch;
        private String ifModifiedSince;
        private String ifUnmodifiedSince;
        private Map<String, List<String>> signedHeaders;

        public Builder() {
        }

        Builder(PresignedUrlDownloadRequestWrapper request) {
            super(request);
            this.url = request.url();
            this.range = request.range();
            this.ifMatch = request.ifMatch();
            this.ifNoneMatch = request.ifNoneMatch();
            this.ifModifiedSince = request.ifModifiedSince();
            this.ifUnmodifiedSince = request.ifUnmodifiedSince();
            this.signedHeaders = request.signedHeaders();
        }

        public Builder url(URL url) {
            this.url = url;
            return this;
        }

        public Builder range(String range) {
            this.range = range;
            return this;
        }

        public Builder ifMatch(String ifMatch) {
            this.ifMatch = ifMatch;
            return this;
        }

        public Builder ifNoneMatch(String ifNoneMatch) {
            this.ifNoneMatch = ifNoneMatch;
            return this;
        }

        public Builder ifModifiedSince(String ifModifiedSince) {
            this.ifModifiedSince = ifModifiedSince;
            return this;
        }

        public Builder ifUnmodifiedSince(String ifUnmodifiedSince) {
            this.ifUnmodifiedSince = ifUnmodifiedSince;
            return this;
        }

        public Builder signedHeaders(Map<String, List<String>> signedHeaders) {
            this.signedHeaders = signedHeaders;
            return this;
        }

        @Override
        public PresignedUrlDownloadRequestWrapper build() {
            return new PresignedUrlDownloadRequestWrapper(this);
        }
    }
}
