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

package software.amazon.awssdk.auth.credentials.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;

/**
 * Interceptor that adds the CREDENTIALS_IMDS business metric when IMDS credentials are being used.
 */
@SdkInternalApi
public final class ImdsCredentialsBusinessMetricInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        AwsCredentials credentials = executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS);
        
        if (credentials != null && isImdsCredentials(credentials)) {
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS)
                               .addMetric(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
        }
        
        return context.request();
    }

    private boolean isImdsCredentials(AwsCredentials credentials) {
        return credentials.providerName()
                         .map(name -> name.contains("InstanceProfile"))
                         .orElse(false);
    }
}
