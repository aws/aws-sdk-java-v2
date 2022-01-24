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
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Concrete class that implements the Signer to sign a request with Bearer token authorization.
 * TODO : Actual implementation will be done with separate PR. This is added to unbloc codegen task implementation
 */
@SdkPublicApi
public final class BearerTokenSigner implements Signer {

    @Override
    public CredentialType credentialType() {
        return CredentialType.BEARER_TOKEN;
    }

    /**
     * TODO : Separate PR will implement the logic for updating the authorization header using
     *
     * @param request             The request to sign
     * @param executionAttributes Contains the attributes required for signing the request
     * @return
     */
    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        return doSign(request, executionAttributes);
    }

    public SdkHttpFullRequest doSign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
        //TODO: Will be implemented in separate PR
        return null;
    }

    public static BearerTokenSigner create() {
        return new BearerTokenSigner();
    }
}
