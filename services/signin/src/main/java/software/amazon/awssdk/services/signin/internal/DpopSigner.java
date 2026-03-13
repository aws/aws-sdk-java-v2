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

package software.amazon.awssdk.services.signin.internal;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Signs request with a DPoP header using the requests endpoint and http method and the
 * key from the resolved Identity.
 */
@SdkInternalApi
public class DpopSigner implements HttpSigner<DpopIdentity> {

    @Override
    public SignedRequest sign(SignRequest<? extends DpopIdentity> request) {
        return SignedRequest.builder()
                            .request(doSign(request))
                            .payload(request.payload().orElse(null))
                            .build();
    }

    @Override
    public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends DpopIdentity> request) {
        return CompletableFuture.completedFuture(
            AsyncSignedRequest.builder()
                              .request(doSign(request))
                              .payload(request.payload().orElse(null))
                              .build());
    }

    /**
     * Using {@link BaseSignRequest}, sign the request with a {@link BaseSignRequest} and re-build it.
     */
    private SdkHttpRequest doSign(BaseSignRequest<?, ? extends DpopIdentity> request) {
        return request.request().toBuilder()
                      .putHeader(
                          "DPoP",
                          buildDpopHeader(request))
                      .build();
    }

    private String buildDpopHeader(BaseSignRequest<?, ? extends DpopIdentity> request) {
        SdkHttpRequest httpRequest = request.request();
        String endpoint = extractRequestEndpoint(httpRequest);
        return DpopHeaderGenerator.generateDPoPProofHeader(
            request.identity(), endpoint, httpRequest.method().name(),
            Instant.now().getEpochSecond(), UUID.randomUUID().toString());
    }

    private static String extractRequestEndpoint(SdkHttpRequest httpRequest) {
        // using SdkHttpRequest.getUri() results in creating a URI which is slow and we don't need the query components
        // construct only the endpoint that we require for DPoP.
        String portString =
            SdkHttpUtils.isUsingStandardPort(httpRequest.protocol(), httpRequest.port()) ? "" : ":" + httpRequest.port();
        return httpRequest.protocol() + "://" + httpRequest.host() + portString + httpRequest.encodedPath();
    }
}
