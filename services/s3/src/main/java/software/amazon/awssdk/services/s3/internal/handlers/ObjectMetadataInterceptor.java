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

package software.amazon.awssdk.services.s3.internal.handlers;

import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Interceptor that trims object metadata keys of any leading or trailing whitespace for {@code PutObject} and {@code
 * CreateMultipartUpload}. This behavior is intended to provide the same functionality as in 1.x.
 */
@SdkInternalApi
public final class ObjectMetadataInterceptor implements ExecutionInterceptor {
    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();

        switch (executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME)) {
            case "PutObject":
                return trimMetadataNames((PutObjectRequest) request);
            case "CreateMultipartUpload":
                return trimMetadataNames((CreateMultipartUploadRequest) request);
            default:
                return request;
        }
    }

    private PutObjectRequest trimMetadataNames(PutObjectRequest putObjectRequest) {
        if (!putObjectRequest.hasMetadata()) {
            return putObjectRequest;
        }

        return putObjectRequest.toBuilder()
            .metadata(trimKeys(putObjectRequest.metadata()))
            .build();
    }

    private CreateMultipartUploadRequest trimMetadataNames(CreateMultipartUploadRequest createMultipartUploadRequest) {
        if (!createMultipartUploadRequest.hasMetadata()) {
            return createMultipartUploadRequest;
        }

        return createMultipartUploadRequest.toBuilder()
                               .metadata(trimKeys(createMultipartUploadRequest.metadata()))
                               .build();
    }

    private Map<String, String> trimKeys(Map<String, String> map) {
        return map.entrySet().stream()
            .collect(Collectors.toMap(e -> StringUtils.trim(e.getKey()), Map.Entry::getValue));
    }
}
