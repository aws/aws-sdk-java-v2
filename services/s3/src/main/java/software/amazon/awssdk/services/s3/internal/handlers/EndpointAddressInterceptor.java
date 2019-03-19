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

package software.amazon.awssdk.services.s3.internal.handlers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.S3EndpointUtils;

@SdkInternalApi
public final class EndpointAddressInterceptor implements ExecutionInterceptor {

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {

        SdkRequest sdkRequest = context.request();

        return S3EndpointUtils.applyEndpointConfiguration(context.httpRequest(),
                                                          sdkRequest,
                                                          executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION),
                                                          (S3Configuration) executionAttributes
                                                              .getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG),
                                                          sdkRequest.getValueForField("Bucket", String.class)
                                                                    .orElse(null));
    }
}
