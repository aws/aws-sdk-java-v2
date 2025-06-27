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
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;

/**
 * Internal request object for presigned URL GetObject operations.
 * <p>
 * This class is used internally by the AWS SDK to process presigned URL requests for S3 GetObject operations. It contains minimal
 * SdkField definitions needed for custom marshalling and is not intended for direct use by SDK users.
 * </p>
 * <b>Note:</b> This is an internal implementation class and should not be used
 * directly. Use {@code PresignedUrlGetObjectRequest} for public API interactions.
 * </p>
 */
@SdkInternalApi
public final class PresignedUrlGetObjectRequestWrapper extends S3Request {
    private static final SdkField<String> RANGE_FIELD = SdkField
        .<String>builder(MarshallingType.STRING)
        .memberName("Range")
        .getter(getter(PresignedUrlGetObjectRequestWrapper::range))
        .traits(LocationTrait.builder().location(MarshallLocation.HEADER).locationName("Range")
                             .unmarshallLocationName("Range").build()).build();

    private static final List<SdkField<?>> SDK_FIELDS = Collections.unmodifiableList(
        Arrays.asList(RANGE_FIELD));

    private static final Map<String, SdkField<?>> SDK_NAME_TO_FIELD = memberNameToFieldInitializer();

    private final String url;
    private final String range;

    /**
     * Creates an internal request from a public presigned URL request.
     *
     * @param presignedUrlGetObjectRequest The public presigned URL request
     */
    public PresignedUrlGetObjectRequestWrapper(PresignedUrlGetObjectRequest presignedUrlGetObjectRequest) {
        this.url = presignedUrlGetObjectRequest.presignedUrl();
        this.range = presignedUrlGetObjectRequest.range();
    }

    public String url() {
        return url;
    }

    public String range() {
        return range;
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
        return null;
    }

    private static <T> Function<Object, T> getter(Function<PresignedUrlGetObjectRequestWrapper, T> g) {
        return obj -> g.apply((PresignedUrlGetObjectRequestWrapper) obj);
    }

    private static Map<String, SdkField<?>> memberNameToFieldInitializer() {
        Map<String, SdkField<?>> map = new HashMap<>();
        map.put("Range", RANGE_FIELD);
        return Collections.unmodifiableMap(map);
    }
}