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
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;

@SdkInternalApi
public final class DefaultAwsCrtV4aSigner implements AwsCrtV4aSigner {

    private final AwsCrt4aSigningAdapter signer;
    private final SigningConfigProvider configProvider;

    private DefaultAwsCrtV4aSigner() {
        signer = new AwsCrt4aSigningAdapter();
        configProvider = new SigningConfigProvider();
    }

    public static AwsCrtV4aSigner create() {
        return new DefaultAwsCrtV4aSigner();
    }

    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        if (CredentialUtils.isAnonymous(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))) {
            return request;
        }
        return signer.signRequest(request, configProvider.createCrtSigningConfig(executionAttributes));
    }

    @Override
    public SdkHttpFullRequest presign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        return signer.signRequest(request, configProvider.createCrtPresigningConfig(executionAttributes));
    }
}
