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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtHttpRequestConverter.toRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.CrtUtils.sanitizeRequest;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.auth.signing.AwsSigner;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningResult;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.crt.AwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.util.CredentialUtils;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * An implementation which handles signing by calling CRT. Payloads can also be signed by passing a payload-signer for the class
 * to use after signing the request.
 */
@SdkInternalApi
public class CrtV4aHttpSigner implements AwsCrtV4aHttpSigner {

    private final AwsSigningConfig signingConfig;
    private final V4aPayloadSigner payloadSigner;

    public CrtV4aHttpSigner(AwsSigningConfig signingConfig, V4aPayloadSigner payloadSigner) {
        this.signingConfig = signingConfig;
        this.payloadSigner = payloadSigner;
    }

    @Override
    public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
        if (CredentialUtils.isAnonymous(request.identity())) {
            return SyncSignedRequest.builder()
                                    .request(request.request())
                                    .payload(request.payload().orElse(null))
                                    .build();
        }

        SdkHttpRequest sanitizedRequest = sanitizeRequest(request.request());

        HttpRequest crtRequest = toRequest(sanitizedRequest, request.payload().orElse(null));

        V4aContext v4AContext = sign(sanitizedRequest, crtRequest, signingConfig);

        ContentStreamProvider payload = payloadSigner.sign(request.payload().orElse(null), v4AContext);

        return SyncSignedRequest.builder()
                                .request(v4AContext.getSignedRequest())
                                .payload(payload)
                                .build();
    }

    private V4aContext sign(SdkHttpRequest request, HttpRequest crtRequest, AwsSigningConfig signingConfig) {
        AwsSigningResult signingResult = CompletableFutureUtils.joinLikeSync(AwsSigner.sign(crtRequest, signingConfig));
        return new V4aContext(
            toRequest(request, signingResult.getSignedRequest()),
            signingResult.getSignedRequest(),
            signingResult.getSignature(),
            signingConfig
        );
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
        // There isn't currently a concept of async for crt signers
        throw new UnsupportedOperationException();
    }
}
