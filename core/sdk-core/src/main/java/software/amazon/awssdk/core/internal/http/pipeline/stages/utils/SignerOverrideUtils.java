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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncSigningStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.core.signer.Signer;

/**
 * Utility to share across {@link SigningStage} and {@link AsyncSigningStage}.
 */
@SdkInternalApi
public final class SignerOverrideUtils {

    private SignerOverrideUtils() {
    }

    // This is (mostly) same as software.amazon.awssdk.awscore.util.SignerOverrideUtils.isSignerOverridden, but since that
    // is in aws-core, and this is in sdk-core, copied here. It is "mostly" because it changes the SIGNER_OVERRIDDEN check to
    // `true` instead of just isPresent().
    public static boolean isSignerOverridden(RequestExecutionContext context) {
        boolean isClientSignerOverridden =
            Boolean.TRUE.equals(context.executionAttributes().getAttribute(SdkExecutionAttribute.SIGNER_OVERRIDDEN));

        Optional<Signer> requestSigner = context.originalRequest()
                                                .overrideConfiguration()
                                                .flatMap(RequestOverrideConfiguration::signer);
        return isClientSignerOverridden || requestSigner.isPresent();
    }
}
