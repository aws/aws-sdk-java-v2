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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.CredentialUtils;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsS3V4aSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public class DefaultAwsS3V4ASigner implements AwsS3V4aSigner {

    private final Aws4aSigningAdapter signer;
    private final SigningConfigProvider configProvider;

    private DefaultAwsS3V4ASigner() {
        signer = new Aws4aSigningAdapter();
        configProvider = new SigningConfigProvider();
    }

    public static AwsS3V4aSigner create() {
        return new DefaultAwsS3V4ASigner();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (CredentialUtils.isAnonymous(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))) {
            return request;
        }
        return signer.signWithCrt(request, configProvider.createS3CrtSigningConfig(request, executionAttributes));
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        return signer.signWithCrt(request, configProvider.createS3CrtPresigningConfig(request, executionAttributes));
    }
}
