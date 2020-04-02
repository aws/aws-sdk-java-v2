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

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Turns on payload signing and prevents moving query params to body during a POST which S3 doesn't like.
 */
@SdkInternalApi
public class PayloadSigningInterceptor implements ExecutionInterceptor {

    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {
        executionAttributes.putAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING, true);
        if (!context.requestBody().isPresent() && context.httpRequest().method().equals(SdkHttpMethod.POST)) {
            return Optional.of(RequestBody.fromBytes(new byte[0]));
        }

        return context.requestBody();
    }
}
