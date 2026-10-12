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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;

/**
 * Interceptor to add an 'Expect: 100-continue' header to the HTTP Request if it represents a PUT Object or Upload Part
 * request. This behavior can be disabled via {@link S3Configuration#expectContinueEnabled()}.
 */
@SdkInternalApi
//TODO: This should be generalized for all streaming requests
public final class StreamingRequestInterceptor implements ExecutionInterceptor {

    private static final Logger log = Logger.loggerFor(StreamingRequestInterceptor.class);

    private static final String DECODED_CONTENT_LENGTH_HEADER = "x-amz-decoded-content-length";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        if (shouldAddExpectContinueHeader(context, executionAttributes)) {
            return context.httpRequest().toBuilder().putHeader("Expect", "100-continue").build();
        }
        return context.httpRequest();
    }

    private boolean shouldAddExpectContinueHeader(Context.ModifyHttpRequest context,
                                                  ExecutionAttributes executionAttributes) {
        // Only applies to streaming operations
        if (!(context.request() instanceof PutObjectRequest
              || context.request() instanceof UploadPartRequest)) {
            return false;
        }

        S3Configuration s3Config = getS3Configuration(executionAttributes);
        boolean expectContinueEnabled = s3Config == null || s3Config.expectContinueEnabled();
        boolean crossRegionAccessEnabled = isCrossRegionAccessEnabled(executionAttributes);

        // For cross region PUT, the header defaults to on: sending the body unconditionally to the wrong region where S3
        // responds with a 3xx and closes the connection can surface as an I/O error before the client can retry based on the
        // region in the 3xx response. Users who explicitly disable expectContinueEnabled opt out of this.
        if (crossRegionAccessEnabled && expectContinueEnabled) {
            return true;
        }

        if (!expectContinueEnabled) {
            if (crossRegionAccessEnabled) {
                log.debug(() -> "Expect: 100-continue is explicitly disabled while cross region access is enabled. The "
                                + "header will not be added, so the first call to a bucket whose region "
                                + "has not yet been resolved may fail with an I/O error instead of being transparently "
                                + "redirected.");
            }
            return false;
        }

        long threshold = s3Config != null ? s3Config.expectContinueThresholdInBytes()
                                          : 0L;

        return getContentLengthHeader(context.httpRequest())
            .map(Long::parseLong)
            .map(length -> length >= threshold && length != 0L)
            .orElse(true);
    }

    private S3Configuration getS3Configuration(ExecutionAttributes executionAttributes) {
        ServiceConfiguration serviceConfig = executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_CONFIG);
        return serviceConfig instanceof S3Configuration ? (S3Configuration) serviceConfig : null;
    }

    /**
     * Retrieves content length header value.
     * Checks x-amz-decoded-content-length first, then falls back to Content-Length.
     *
     * @param httpRequest the HTTP request
     * @return Optional containing the content length header value, or empty if not present
     */
    private Optional<String> getContentLengthHeader(SdkHttpRequest httpRequest) {
        Optional<String> decodedLength = httpRequest.firstMatchingHeader(DECODED_CONTENT_LENGTH_HEADER);
        return decodedLength.isPresent()
               ? decodedLength
               : httpRequest.firstMatchingHeader(CONTENT_LENGTH_HEADER);
    }

    private boolean isCrossRegionAccessEnabled(ExecutionAttributes executionAttributes) {
        Optional<AttributeMap> ctxParams = executionAttributes.getOptionalAttribute(
            SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS);

        return ctxParams.map(p -> Boolean.TRUE.equals(p.get(S3ClientContextParams.CROSS_REGION_ACCESS_ENABLED)))
                        .orElse(false);
    }
}
