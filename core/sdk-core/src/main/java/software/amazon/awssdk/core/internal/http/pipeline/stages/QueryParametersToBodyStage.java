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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.SdkProtocolMetadata;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Modifies an HTTP request by moving query parameters to the body under the following conditions:
 * - It is a POST request
 * - There is no content stream provider
 * - There are query parameters to transfer
 */
@SdkInternalApi
public class QueryParametersToBodyStage implements MutableRequestToRequestPipeline {

    private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=" +
                                                       lowerCase(StandardCharsets.UTF_8.toString());

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {

        if (shouldPutParamsInBody(request.build(), context)) {
            return changeQueryParametersToFormData(request.build()).toBuilder();
        }
        return request;
    }

    private boolean shouldPutParamsInBody(SdkHttpFullRequest request, RequestExecutionContext context) {
        SdkProtocolMetadata protocolMetadata =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.PROTOCOL_METADATA);
        if (protocolMetadata == null) {
            return false;
        }
        String protocol = protocolMetadata.serviceProtocol();
        boolean isQueryProtocol = "query".equalsIgnoreCase(protocol) || "ec2".equalsIgnoreCase(protocol);

        return isQueryProtocol &&
               request.method() == SdkHttpMethod.POST &&
               !request.contentStreamProvider().isPresent() &&
               request.numRawQueryParameters() > 0;
    }

    private SdkHttpFullRequest changeQueryParametersToFormData(SdkHttpFullRequest request) {
        byte[] params = request.encodedQueryParametersAsFormData().orElse("")
                             .getBytes(StandardCharsets.UTF_8);

        return request.toBuilder().clearQueryParameters()
                    .contentStreamProvider(() -> new ByteArrayInputStream(params))
                    .putHeader("Content-Length", singletonList(String.valueOf(params.length)))
                    .putHeader("Content-Type", singletonList(DEFAULT_CONTENT_TYPE))
                    .build();
    }
}
