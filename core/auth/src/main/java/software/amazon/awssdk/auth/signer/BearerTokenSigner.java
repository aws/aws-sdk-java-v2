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

package software.amazon.awssdk.auth.signer;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Validate;

/**
 * A {@link Signer} that will sign a request with Bearer token authorization.
 */
@SdkPublicApi
public final class BearerTokenSigner implements Signer {

    private static final String BEARER_LABEL = "Bearer";
    private static final String SPACE = " ";

    public static BearerTokenSigner create() {
        return new BearerTokenSigner();
    }

    @Override
    public CredentialType credentialType() {
        return CredentialType.BEARER_TOKEN;
    }

    /**
     * Signs the request by adding an 'Authorization' header containing the string value of the token
     * in accordance with RFC 6750, section 2.1.
     *
     * @param request             The request to sign
     * @param executionAttributes Contains the attributes required for signing the request
     *
     * @return The signed request.
     */
    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        return doSign(request, executionAttributes);
    }

    private SdkHttpFullRequest doSign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        AwsToken token = Validate.notNull(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_TOKEN),
                                               "Token must not be null");
        return request.toBuilder()
                      .putHeader(SignerConstant.AUTHORIZATION, buildAuthorizationHeader(token))
                      .build();
    }

    private String buildAuthorizationHeader(AwsToken token) {
        return String.format("%s%s%s", BEARER_LABEL, SPACE, token.token());
    }
}
