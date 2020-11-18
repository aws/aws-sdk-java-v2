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

package software.amazon.awssdk.services.s3control.internal.interceptors;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Execution interceptor which modifies the HTTP request to S3 Control to
 * add a signer attribute that will instruct the signer to not double-url-encode path elements.
 * S3 Control expects path elements to be encoded only once in the canonical URI.
 * Similar functionality exists for S3.
 */
@SdkInternalApi
public final class DisableDoubleUrlEncodingForSigningInterceptor implements ExecutionInterceptor {

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE, Boolean.FALSE);
    }
}
