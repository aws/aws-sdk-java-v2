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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.BearerHttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSignRequest;
import software.amazon.awssdk.http.auth.spi.SignedHttpRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;


/**
 * A default implementation of {@link BearerHttpSigner}
 */
@SdkInternalApi
public class DefaultBearerHttpSigner implements BearerHttpSigner {

    private static final String AUTHZ_HEADER = "Authorization";
    private static final String BEARER_LABEL = "Bearer";

    private final TokenIdentity tokenIdentity;

    public DefaultBearerHttpSigner(TokenIdentity tokenIdentity) {
        this.tokenIdentity = tokenIdentity;
    }

    @Override
    public SignedHttpRequest<? extends ContentStreamProvider> sign(HttpSignRequest<? extends ContentStreamProvider> request) {
        return doSign(request);
    }

    @Override
    public SignedHttpRequest<? extends Publisher<? extends ByteBuffer>>
        signAsync(HttpSignRequest<? extends Publisher<? extends ByteBuffer>> request) {
        return doSign(request);
    }

    private <T> SignedHttpRequest<T> doSign(HttpSignRequest<T> request) {
        SdkHttpRequest signedRequest = request.request().toBuilder()
                .putHeader(
                        AUTHZ_HEADER,
                        buildAuthorizationHeader(tokenIdentity))
                .build();

        return SignedHttpRequest.builder(request.payloadType())
                .request(signedRequest)
                .payload(request.payload().orElse(null))
                .build();
    }

    private String buildAuthorizationHeader(TokenIdentity tokenIdentity) {
        return String.format("%s %s", BEARER_LABEL, tokenIdentity.token());
    }

}
