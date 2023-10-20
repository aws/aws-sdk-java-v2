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

package software.amazon.awssdk.awscore.util;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;

/**
 * Utility to override a given {@link SdkRequest}'s {@link Signer}. Typically used by {@link ExecutionInterceptor}s that wish to
 * dynamically enable particular signing methods, like SigV4a for multi-region endpoints.
 */
@SdkProtectedApi
public final class SignerOverrideUtils {
    private SignerOverrideUtils() {
    }

    public static SdkRequest overrideSignerIfNotOverridden(SdkRequest request,
                                                           ExecutionAttributes executionAttributes,
                                                           Signer signer) {
        return overrideSignerIfNotOverridden(request, executionAttributes, () -> signer);
    }
    
    public static SdkRequest overrideSignerIfNotOverridden(SdkRequest request,
                                                           ExecutionAttributes executionAttributes,
                                                           Supplier<Signer> signer) {
        if (isSignerOverridden(request, executionAttributes)) {
            return request;
        }
        return overrideSigner(request, signer.get());
    }

    public static boolean isSignerOverridden(SdkRequest request, ExecutionAttributes executionAttributes) {
        Optional<Boolean> isClientSignerOverridden = Optional.ofNullable(
            executionAttributes.getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN));
        Optional<Signer> requestSigner = request.overrideConfiguration()
                                                .flatMap(RequestOverrideConfiguration::signer);
        return isClientSignerOverridden.isPresent() || requestSigner.isPresent();
    }

    private static SdkRequest overrideSigner(SdkRequest request, Signer signer) {
        return request.overrideConfiguration()
                      .flatMap(config -> config.signer()
                                               .map(existingOverrideSigner -> request))
                      .orElseGet(() -> createNewRequest(request, signer));
    }

    private static SdkRequest createNewRequest(SdkRequest request, Signer signer) {
        AwsRequest awsRequest = (AwsRequest) request;

        AwsRequestOverrideConfiguration modifiedOverride =
            awsRequest.overrideConfiguration()
                      .map(AwsRequestOverrideConfiguration::toBuilder)
                      .orElseGet(AwsRequestOverrideConfiguration::builder)
                      .signer(signer)
                      .build();

        return awsRequest.toBuilder()
                         .overrideConfiguration(modifiedOverride)
                         .build();
    }
}
