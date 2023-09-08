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

package software.amazon.awssdk.http.auth.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.BearerHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignedRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * A default implementation of {@link BearerHttpSigner}.
 */
@SdkInternalApi
public final class DefaultBearerHttpSigner implements BearerHttpSigner {

    @Override
    public SignedRequest sign(SignRequest<? extends TokenIdentity> request) {
        return SignedRequest.builder()
                            .request(doSign(request))
                            .payload(request.payload().orElse(null))
                            .build();
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends TokenIdentity> request) {
        return CompletableFuture.completedFuture(
            AsyncSignedRequest.builder()
                              .request(doSign(request))
                              .payload(request.payload().orElse(null))
                              .build()
        );
    }

    /**
     * Using {@link BaseSignRequest}, sign the request with a {@link BaseSignRequest} and re-build it.
     */
    private SdkHttpRequest doSign(BaseSignRequest<?, ? extends TokenIdentity> request) {
        return request.request().toBuilder()
                      .putHeader(
                          "Authorization",
                          buildAuthorizationHeader(request.identity()))
                      .build();
    }

    /**
     * Use a {@link TokenIdentity} to build an authorization header.
     */
    private String buildAuthorizationHeader(TokenIdentity tokenIdentity) {
        return "Bearer " + tokenIdentity.token();
    }
}
