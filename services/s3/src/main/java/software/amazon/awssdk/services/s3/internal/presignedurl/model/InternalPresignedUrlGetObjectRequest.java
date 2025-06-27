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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
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
 * <p>
 * The internal request handles:
 * </p>
 * <ul>
 * <li>Presigned URL processing and validation</li>
 * <li>HTTP Range header marshalling for partial content requests</li>
 * <li>Custom request marshalling bypassing standard S3 request processing</li>
 * <li>Integration with the AWS SDK's internal request/response pipeline</li>
 * </ul>
 * <p>
 * <b>Note:</b> This is an internal implementation class and should not be used
 * directly. Use {@code PresignedUrlGetObjectRequest} for public API interactions.
 * </p>
 */
@SdkInternalApi
public final class InternalPresignedUrlGetObjectRequest extends S3Request {
    private static final SdkField<String> RANGE_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("Range")
        .getter(getter(InternalPresignedUrlGetObjectRequest::range))
        .setter(setter(Builder::range))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("Range")
                             .unmarshallLocationName("Range").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(
        Arrays.asList(RANGE_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final String url;
    private final String range;

    private InternalPresignedUrlGetObjectRequest(Builder builder) {
        super(builder);
        this.url = builder.url;
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
    public String url() {
        return url;
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
    public List<SdkField<?>> sdkFields() {
        return SDK_FIELDS;
    }

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

    private static <T> Function<Object, T> getter(Function<InternalPresignedUrlGetObjectRequest, T> g) {
        return obj -> g.apply((InternalPresignedUrlGetObjectRequest) obj);
    }

    private static <T> BiConsumer<Object, T> setter(BiConsumer<Builder, T> s) {
        return (obj, val) -> s.accept((Builder) obj, val);
    }

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("Range", RANGE_FIELD);
        return Collections.unmodifiableMap(map);
    }

    public static final class Builder extends S3Request.BuilderImpl implements S3Request.Builder {
        private String url;
        private String range;

        private Builder() {
        }

        private Builder(InternalPresignedUrlGetObjectRequest request) {
            super(request);
            this.url = request.url;
            this.range = request.range;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder range(String range) {
            this.range = range;
            return this;
        }

        @Override
        public S3Request build() {
            return new InternalPresignedUrlGetObjectRequest(this);
        }

        public InternalPresignedUrlGetObjectRequest buildInternal() {
            return new InternalPresignedUrlGetObjectRequest(this);
        }
    }
}