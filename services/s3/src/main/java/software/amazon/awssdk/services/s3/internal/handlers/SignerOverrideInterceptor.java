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
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.services.s3.internal.signing.S3SigningUtils;

@SdkInternalApi
public final class SignerOverrideInterceptor implements ExecutionInterceptor {

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        return S3SigningUtils.internalSignerOverride(context.request())
                             .map(signer -> setRequestOverrideSignerIfNotExist(context.request(), signer, executionAttributes))
                             .orElseGet(context::request);
    }

    private SdkRequest setRequestOverrideSignerIfNotExist(SdkRequest request, Signer signer,
                                                          ExecutionAttributes executionAttributes) {
        if (existsOverrideSigner(request, executionAttributes)) {
            return request;
        }
        return setOverrideSigner(request, signer);
    }

    private boolean existsOverrideSigner(SdkRequest request, ExecutionAttributes executionAttributes) {
        Optional<Boolean> isClientSignerOverridden = Optional.ofNullable(
            executionAttributes.getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN));
        Optional<Signer> requestSigner = request.overrideConfiguration()
                                                .flatMap(RequestOverrideConfiguration::signer);
        return isClientSignerOverridden.isPresent() || requestSigner.isPresent();
    }

    private SdkRequest setOverrideSigner(SdkRequest request, Signer signer) {
        return request.overrideConfiguration()
                      .flatMap(config -> config.signer()
                                               .map(existingOverrideSigner -> request))
                      .orElseGet(() -> createNewRequest(request, signer));
    }

    private SdkRequest createNewRequest(SdkRequest request, Signer signer) {
        return ((AwsRequest) request).toBuilder()
                                     .overrideConfiguration(c -> c.signer(signer))
                                     .build();
    }
}
