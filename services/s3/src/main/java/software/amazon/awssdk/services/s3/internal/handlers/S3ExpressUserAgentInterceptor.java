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

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.s3.internal.s3express.S3ExpressUtils;
import software.amazon.awssdk.services.s3.model.S3Request;

/**
 * Interceptor that adds business metrics for S3 Express bucket operations.
 * This interceptor detects when an operation is performed on an S3 Express bucket
 * using S3 Express credentials and adds the appropriate business metric to track usage.
 */

@SdkInternalApi
public final class S3ExpressUserAgentInterceptor implements ExecutionInterceptor {

    private static final ApiName S3_EXPRESS_API_NAME = ApiName.builder()
                                                              .name("sdk-metrics")
                                                              .version(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value())
                                                              .build();

    private static final Consumer<AwsRequestOverrideConfiguration.Builder> S3_EXPRESS_USER_AGENT_APPLIER =
        b -> b.addApiName(S3_EXPRESS_API_NAME);

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();

        if (request instanceof S3Request) {
            S3Request s3Request = (S3Request) request;

            if (S3ExpressUtils.useS3Express(executionAttributes) &&
                S3ExpressUtils.useS3ExpressAuthScheme(executionAttributes)) {

                AwsRequestOverrideConfiguration overrideConfiguration =
                    s3Request.overrideConfiguration()
                             .map(c -> c.toBuilder()
                                        .applyMutation(S3_EXPRESS_USER_AGENT_APPLIER)
                                        .build())
                             .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                             .applyMutation(S3_EXPRESS_USER_AGENT_APPLIER)
                                                                             .build());

                return s3Request.toBuilder().overrideConfiguration(overrideConfiguration).build();
            }
        }

        return request;
    }
}
