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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.S3Request;

/**
 * Apply TM specific user agent to the request
 */
@SdkInternalApi
public final class ApplyUserAgentInterceptor implements ExecutionInterceptor {
    private static final ApiName API_NAME =
        ApiName.builder().name("ft").version("s3-transfer").build();
    private static final Consumer<AwsRequestOverrideConfiguration.Builder> USER_AGENT_APPLIER =
        b -> b.addApiName(API_NAME);

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        assert context.request() instanceof S3Request;

        S3Request request = (S3Request) context.request();
        AwsRequestOverrideConfiguration overrideConfiguration =
            request.overrideConfiguration()
                   .map(c -> c.toBuilder()
                              .applyMutation(USER_AGENT_APPLIER)
                              .build())
                   .orElseGet(() -> AwsRequestOverrideConfiguration.builder()
                                                                   .applyMutation(USER_AGENT_APPLIER)
                                                                   .build());

        return request.toBuilder().overrideConfiguration(overrideConfiguration).build();
    }
}
