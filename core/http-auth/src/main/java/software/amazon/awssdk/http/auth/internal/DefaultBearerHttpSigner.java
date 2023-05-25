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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.BearerHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedHttpRequest;
import software.amazon.awssdk.http.auth.spi.HttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncHttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedHttpRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * A default implementation of {@link BearerHttpSigner}.
 */
@SdkInternalApi
public class DefaultBearerHttpSigner implements BearerHttpSigner {

    private static final String AUTHZ_HEADER = "Authorization";
    private static final String BEARER_LABEL = "Bearer";

    @Override
    public SyncSignedHttpRequest sign(SyncHttpSignRequest<? extends TokenIdentity> request) {
        return SyncSignedHttpRequest.builder()
                .request(doSign(request))
                .payload(request.payload().orElse(null))
                .build();
    }

    @Override
    public AsyncSignedHttpRequest signAsync(AsyncHttpSignRequest<? extends TokenIdentity> request) {
        return AsyncSignedHttpRequest.builder()
                .request(doSign(request))
                .payload(request.payload().orElse(null))
                .build();
    }

    /**
     * Using {@link HttpSignRequest}, sign the request with
     * a {@link HttpSignRequest} and re-build it.
     */
    private SdkHttpRequest doSign(HttpSignRequest<?, ? extends TokenIdentity> request) {
        return request.request().toBuilder()
                .putHeader(
                        AUTHZ_HEADER,
                        buildAuthorizationHeader(request.identity()))
                .build();
    }

    /**
     * Use a {@link TokenIdentity} to build an authorization header.
     */
    private String buildAuthorizationHeader(TokenIdentity tokenIdentity) {
        return String.format("%s %s", BEARER_LABEL, tokenIdentity.token());
    }
}
