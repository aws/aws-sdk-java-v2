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

package software.amazon.awssdk.authcrt.signer.internal;

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.params.Aws4aPresignerParams;
import software.amazon.awssdk.authcrt.signer.params.Aws4aSignerParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public abstract class AbstractAws4aSigner<T extends Aws4aSignerParams, U extends Aws4aPresignerParams>
        implements Signer, Presigner {

    protected abstract SdkHttpFullRequest sign(SdkHttpFullRequest request, Aws4aSignerParams signingParams);

    protected abstract SdkHttpFullRequest presign(SdkHttpFullRequest request, Aws4aPresignerParams signingParams);

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        Aws4aSignerParams signingParams = extractSignerParams(T.builder(), executionAttributes)
                .build();
        return sign(request, signingParams);
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        Aws4aPresignerParams signingParams = extractPresignerParams(U.builder(), executionAttributes)
                .build();

        return presign(request, signingParams);
    }

    protected <B extends Aws4aSignerParams.Builder> B extractSignerParams(B paramsBuilder,
                                                                          ExecutionAttributes executionAttributes) {
        paramsBuilder.awsCredentials(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))
                .signingName(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME))
                .signingRegionSet(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION)
                        .toString())   // Temporary, real solution TBD with Lemmy integration
                .signingTimestamp(Instant.now());

        if (executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE) != null) {
            paramsBuilder.doubleUrlEncode(executionAttributes
                    .getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
        }

        return paramsBuilder;
    }

    protected <B extends Aws4aPresignerParams.Builder> B extractPresignerParams(
            B paramsBuilder, ExecutionAttributes executionAttributes) {
        paramsBuilder = extractSignerParams(paramsBuilder, executionAttributes);
        paramsBuilder.expirationTime(executionAttributes
                .getAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION));

        return paramsBuilder;
    }
}
